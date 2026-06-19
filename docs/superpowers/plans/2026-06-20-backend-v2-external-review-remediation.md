# 后端 V2 外部审查采纳项实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. The user explicitly prohibited subagents. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复外部审查中经当前分支复核后仍成立的问题，同时避免错误方案和无收益范围扩张。

**Architecture:** 保持现有四层结构、通用错误码和乐观评论发布策略。先处理独立的小缺陷，再执行 Web→Domain 边界迁移和公开 DTO 契约收窄；稳定领域枚举使用精确白名单，其他领域对象通过 application contract 隔离。

**Tech Stack:** Java 17、Spring Boot 3、MyBatis XML、MySQL/H2、JUnit 5、Mockito、MockMvc、ArchUnit、Maven。

---

## 0. 执行约束

- 不使用子代理。
- 每个 Task 独立测试、独立提交；提交信息使用中文。
- 每次提交前运行 `git diff --stat` 和 `git status --short`。
- 不导入 `C:\Users\TYB\OneDrive\Desktop\后端\deploy`；该目录只提供复核线索。
- 不实现以下外部建议：删除评论 approve、创建镜像枚举、一次性增加模块错误码、重做既有 Schema 例外。
- 阶段结束前运行 fresh H2 全量；MySQL 广泛回归只使用用户授权的 `myblog_v2_dev`。

## 1. 文件与职责规划

| 单元 | 文件 | 职责 |
|---|---|---|
| HTTP 404 | `common/error/GlobalExceptionHandler.java` | 把未知 API 路由映射为统一 404。 |
| 评论审核 | 新建 `comment/application/CommentAuditPolicy.java`；新建 `comment/infrastructure/config/CommentAuditProperties.java`、`KeywordCommentAuditPolicy.java` | application 只依赖审核端口；infrastructure 从配置构建关键词策略。 |
| 标签批量查询 | `ArticleMapper.java/xml`、`MyBatisAdminArticleQueryRepository.java` | 一次读取当前页所有文章的标签 ID。 |
| PASSWORD 预检 | 新建 `PublicArticleAccessMetadata.java`；修改公开文章 repository/mapper/service | 不读取正文即可判断不存在、不可见和 PASSWORD。 |
| 分层守护 | application result/query 类型、相关 Web mapping、`ArchitectureRulesTest.java` | 迁出 Web 对非枚举 Domain 类型的直接依赖，并守护精确白名单。 |
| DTO 收窄 | 公开文章与附件 VO/result/mapping、OpenAPI/Controller 测试 | 移除无用内部字段并增加公开详情更新时间。 |
| 契约与发布检查 | API contract、error handling、security baseline/workflow | 固化 JST、分页、错误码、代理和 CORS 约定。 |

---

### Task 1: 未知 API 路由返回统一 404

**Files:**
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/error/GlobalExceptionHandler.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/error/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: 写失败测试**

在 `GlobalExceptionHandlerTest` 增加：

```java
@Test
void returnsNotFoundEnvelopeForUnknownApiRoute() throws Exception {
    mockMvc.perform(get("/api/test/errors/missing-route"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("90003"))
            .andExpect(jsonPath("$.msg").value("接口不存在"))
            .andExpect(jsonPath("$.data").isEmpty());
}
```

- [ ] **Step 2: 验证测试先失败**

Run:

```powershell
mvn '-Dtest=GlobalExceptionHandlerTest' test
```

Expected: 新用例得到 `500 + 99999`，测试失败。

- [ ] **Step 3: 实现最小 404 handler**

在 catch-all handler 之前增加：

```java
@ExceptionHandler(NoResourceFoundException.class)
ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
        NoResourceFoundException exception) {
    return ResponseEntity.status(exception.getStatusCode())
            .body(ApiResponse.fail(
                    ApiErrorCode.NOT_FOUND.code(),
                    "接口不存在"));
}
```

并导入 `org.springframework.web.servlet.resource.NoResourceFoundException`。

- [ ] **Step 4: 运行局部验证**

Run: `mvn '-Dtest=GlobalExceptionHandlerTest' test`

Expected: `GlobalExceptionHandlerTest` 全部通过。

- [ ] **Step 5: 检查并提交**

```powershell
git diff --stat
git status --short
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/error/GlobalExceptionHandler.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/error/GlobalExceptionHandlerTest.java'
git commit -m '修复未知接口返回状态'
```

---

### Task 2: 将评论硬编码关键词替换为可配置审核策略

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/CommentAuditPolicy.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/config/CommentAuditProperties.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/config/KeywordCommentAuditPolicy.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/CommentCreateService.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/infrastructure/config/KeywordCommentAuditPolicyTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/application/CommentCreateServiceTest.java`

- [ ] **Step 1: 写策略失败测试**

覆盖空列表默认 PASS、忽略大小写命中 PENDING、空白关键词被丢弃：

```java
@Test
void marksNormalizedKeywordMatchesAsPending() {
    KeywordCommentAuditPolicy policy = new KeywordCommentAuditPolicy(
            new CommentAuditProperties(List.of(" spam ", "广告")));

    assertThat(policy.audit("This is SPAM"))
            .isEqualTo(CommentAuditStatus.PENDING);
    assertThat(policy.audit("普通评论"))
            .isEqualTo(CommentAuditStatus.PASS);
}
```

- [ ] **Step 2: 定义 application 端口**

```java
package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;

public interface CommentAuditPolicy {
    CommentAuditStatus audit(String contentMarkdown);
}
```

- [ ] **Step 3: 实现配置与关键词策略**

```java
@ConfigurationProperties("myblog.comment.audit")
public record CommentAuditProperties(List<String> blockedKeywords) {
    public CommentAuditProperties {
        blockedKeywords = blockedKeywords == null
                ? List.of()
                : blockedKeywords.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(keyword -> !keyword.isEmpty())
                        .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                        .distinct()
                        .toList();
    }
}
```

`KeywordCommentAuditPolicy` 实现 `CommentAuditPolicy`，命中任一关键词返回 `PENDING`，否则返回 `PASS`。使用 `@Component` 并通过 `@EnableConfigurationProperties(CommentAuditProperties.class)` 注册属性。

- [ ] **Step 4: 注入策略并删除硬编码 audit 方法**

在 `CommentCreateService` 构造依赖中加入 `CommentAuditPolicy`，将：

```java
CommentAuditStatus status = audit(command.contentMd());
```

替换为：

```java
CommentAuditStatus status = auditPolicy.audit(command.contentMd());
```

保留 `AdminCommentCommandService.approve()` 和对应端点。

- [ ] **Step 5: 增加配置并更新 service 测试**

```yaml
myblog:
  comment:
    audit:
      blocked-keywords: []
```

测试 profile 使用 `blocked-keywords: [spam]`。`CommentCreateServiceTest` 注入 mock `CommentAuditPolicy`，分别 stub PASS/PENDING，不再让 service 测试依赖关键词实现。

- [ ] **Step 6: 运行局部验证**

Run:

```powershell
mvn '-Dtest=KeywordCommentAuditPolicyTest,CommentCreateServiceTest,CommentIntegrationTest,ApplicationConfigurationTest' test
```

Expected: 所有指定测试通过；PASS 评论递增计数，PENDING 评论不递增，approve 测试仍存在并通过。

- [ ] **Step 7: 检查并提交**

提交前确认没有删除 approve 代码或测试。

```powershell
git diff --stat
git status --short
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment' 'MyBlog-springboot-v2/src/main/resources/application.yml' 'MyBlog-springboot-v2/src/test/resources/application-test.yml' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment'
git commit -m '实现可配置评论关键词审核'
```

---

### Task 3: 批量读取后台文章分页标签

**Files:**
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/ArticleMapper.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/mapper/content/ArticleMapper.xml`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/repository/MyBatisAdminArticleQueryRepository.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/infrastructure/persistence/MyBatisAdminArticleQueryRepositoryUnitTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/infrastructure/persistence/DatabaseAdminArticleQueryRepositoryTest.java`

- [ ] **Step 1: 写失败的 mapper 交互测试**

构造两条 `AdminArticlePageRow`，stub `selectAdminPage` 和批量标签结果，断言：

```java
verify(mapper).selectArticleTags(List.of(101L, 100L));
verify(mapper, never()).selectTagIds(anyLong());
```

同时断言两个 page item 分别得到自己的有序 `tagIds`。

- [ ] **Step 2: 验证测试先失败**

Run: `mvn '-Dtest=MyBatisAdminArticleQueryRepositoryUnitTest' test`

Expected: 当前实现调用 `selectTagIds`，交互断言失败。

- [ ] **Step 3: 复用批量标签查询**

不要新增重复 SQL。将 `ArticleMapper.selectPublicArticleTags(List<Long>)` 改名为用途无关的 `selectArticleTags(List<Long>)`，同步 XML statement id 和公开文章 repository 调用。

在后台 repository 中：

```java
List<AdminArticlePageRow> rows = mapper.selectAdminPage(
        criteria, offset, criteria.size());
Map<Long, List<Long>> tagIdsByArticle = rows.isEmpty()
        ? Map.of()
        : mapper.selectArticleTags(rows.stream()
                        .map(AdminArticlePageRow::getId)
                        .toList())
                .stream()
                .collect(Collectors.groupingBy(
                        ArticleTagRow::getArticleId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                ArticleTagRow::getTagId,
                                Collectors.toList())));
```

`toPageItem` 接受已分组的 `List<Long> tagIds`；`findActiveDetail` 仍使用单条 `selectTagIds`。

- [ ] **Step 4: 运行 repository 验证**

Run:

```powershell
mvn '-Dtest=MyBatisAdminArticleQueryRepositoryUnitTest,DatabaseAdminArticleQueryRepositoryTest,DatabasePublicArticleQueryRepositoryTest' test
```

Expected: 三组测试通过；空页不发标签 SQL，非空页只发一次批量标签 SQL。

- [ ] **Step 5: 检查并提交**

```powershell
git diff --stat
git status --short
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure' 'MyBlog-springboot-v2/src/main/resources/mapper/content/ArticleMapper.xml' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/infrastructure/persistence'
git commit -m '批量读取后台文章标签'
```

---

### Task 4: PASSWORD 详情先检查访问元数据再读取正文

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/article/PublicArticleAccessMetadata.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/article/PublicArticleQueryRepository.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/projection/PublicArticleAccessMetadataRow.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/ArticleMapper.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/mapper/content/ArticleMapper.xml`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/repository/MyBatisPublicArticleQueryRepository.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleQueryService.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/PublicArticleQueryServiceTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/infrastructure/persistence/DatabasePublicArticleQueryRepositoryTest.java`

- [ ] **Step 1: 写 service 失败测试**

新增 PASSWORD 场景并验证正文查询未发生：

```java
when(repository.findPublicAccessMetadata(101L, NOW))
        .thenReturn(Optional.of(new PublicArticleAccessMetadata(
                101L, ArticleStatus.PASSWORD)));

assertError(() -> service.detail(101L, "zh"), ApiErrorCode.FORBIDDEN);
verify(repository, never()).findPublicDetail(101L, NOW);
```

另覆盖 metadata 为空返回 NOT_FOUND，以及 PUBLISHED 才调用 `findPublicDetail`。

- [ ] **Step 2: 定义最小领域投影与 repository 方法**

```java
public record PublicArticleAccessMetadata(
        long id,
        ArticleStatus status) {
}
```

在 `PublicArticleQueryRepository` 增加：

```java
Optional<PublicArticleAccessMetadata> findPublicAccessMetadata(
        long id,
        LocalDateTime now);
```

- [ ] **Step 3: 增加不含 body 的 SQL**

```xml
<select id="selectPublicAccessMetadata"
        resultType="com.tyb.myblog.v2.content.infrastructure.persistence.projection.PublicArticleAccessMetadataRow">
    SELECT id, status
    FROM t_article
    WHERE id = #{id}
      AND deleted = 0
      AND status IN (2, 4)
      AND publish_at IS NOT NULL
      AND publish_at &lt;= #{now}
</select>
```

projection 只包含 `id`、`status`，repository 映射为领域 record。

- [ ] **Step 4: 调整 service 查询顺序**

```java
PublicArticleAccessMetadata access = repository
        .findPublicAccessMetadata(id, now)
        .orElseThrow(() -> new ApiException(
                ApiErrorCode.NOT_FOUND,
                "文章不存在"));
if (access.status() == ArticleStatus.PASSWORD) {
    throw new ApiException(
            ApiErrorCode.FORBIDDEN,
            "密码文章暂不开放正文访问");
}
PublicArticleDetail detail = repository.findPublicDetail(id, now)
        .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
```

- [ ] **Step 5: 运行局部验证**

Run:

```powershell
mvn '-Dtest=PublicArticleQueryServiceTest,DatabasePublicArticleQueryRepositoryTest,PublicArticleControllerTest,ArticleIntegrationTest' test
```

Expected: PASSWORD 返回 403 且不调用正文查询；公开详情行为不变。

- [ ] **Step 6: 检查并提交**

```powershell
git diff --stat
git status --short
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content' 'MyBlog-springboot-v2/src/main/resources/mapper/content/ArticleMapper.xml' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content'
git commit -m '避免预读密码文章正文'
```

---

### Task 5: 守护 Web→Domain 精确边界

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/UserProfileResult.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileResult.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileQueryService.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileUpdateService.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/UserProfileVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/CurrentUserVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/AdminArticlePageResult.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/CategoryWriteRequestSupport.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/TagWriteRequestSupport.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWriteRequestSupport.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/CurrentUserControllerTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/AdminArticleControllerTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/AdminCategoryTagControllerTest.java`

- [ ] **Step 1: 固定当前违例清单**

Run:

```powershell
rg -n '^import com\.tyb\.myblog\.v2\..*\.domain\.' 'MyBlog-springboot-v2/src/main/java' -g '*.java' | Select-String '\\web\\'
```

保存输出到任务记录。只允许最终留下 5 个稳定枚举的 import。

- [ ] **Step 2: 先写 ArchUnit 失败规则和 fixture**

新增规则：Web 不得依赖 Domain，以下具体类型除外：

```java
private static final Set<String> WEB_DOMAIN_ENUM_WHITELIST = Set.of(
        "com.tyb.myblog.v2.identity.domain.account.AccountType",
        "com.tyb.myblog.v2.content.domain.article.ArticleStatus",
        "com.tyb.myblog.v2.comment.domain.CommentAuditStatus",
        "com.tyb.myblog.v2.comment.domain.CommentTargetType",
        "com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus");
```

规则 predicate 必须按完整类名判断，不能放宽为整个 package 或所有 enum。增加 deliberate violation fixture，证明 `UserProfile` 依赖会被拒绝，白名单枚举可通过。

- [ ] **Step 3: 迁出 `UserProfile`**

application 新增扁平 `UserProfileResult`，由 service 在 application 内完成 domain→result 映射；`UserProfileVO` 只接受 result。不要在 application result 中继续嵌套 domain 对象。

- [ ] **Step 4: 迁出 `AdminArticlePageItem`**

`AdminArticlePageResult.Item` 必须持有自己的字段，`ArticleWebMapping` 从 application item 映射，不再 import domain page item。

- [ ] **Step 5: 迁出请求支持中的值对象**

Web 层只保留 JSON presence、空值与未知字段处理。`ContentName`、`ContentSlug`、`ArticleSlug` 的规范化和业务校验移到 application command/service，不在 Web 复制领域规则。

- [ ] **Step 6: 验证只剩精确白名单**

Run:

```powershell
mvn '-Dtest=ArchitectureRulesTest,CurrentUserControllerTest,AdminArticleControllerTest,CategoryControllerTest,TagControllerTest' test
rg -n '^import com\.tyb\.myblog\.v2\..*\.domain\.' 'MyBlog-springboot-v2/src/main/java' -g '*.java' | Select-String '\\web\\'
```

Expected: 测试通过；搜索结果只包含 5 个获准枚举中的实际使用项，不含实体、值对象或 page item。

- [ ] **Step 7: 检查并提交**

该任务涉及多文件但只有一个目的：落实分层边界。若 `git diff --stat` 显示无关模块扩散，先拆出更小迁移提交，不要把 DTO 收窄混进本提交。

```powershell
git diff --stat
git status --short
git add -- 'MyBlog-springboot-v2/src/main/java' 'MyBlog-springboot-v2/src/test/java'
git commit -m '守护Web与领域层依赖边界'
```

---

### Task 6: 收窄公开文章与附件响应契约

**Files:**
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticlePageResult.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleDetailResult.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleQueryService.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArticlePageItemVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArticleDetailVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/AttachmentVO.java`
- Modify: public article and attachment Controller/OpenAPI tests
- Modify: `docs/project-handbook/api-contract/article.md`
- Modify: `docs/project-handbook/api-contract/attachment.md`

- [ ] **Step 1: 先修改契约测试表达目标 schema**

公开文章 page/detail 不再包含 `status`、`coverAttachmentId`；detail 包含 `updatedAt`。附件响应不再包含 `storageType`、`bucket`、`objectKey`、`hashSha256`。

在 JSON 测试中使用：

```java
.andExpect(jsonPath("$.data.status").doesNotExist())
.andExpect(jsonPath("$.data.coverAttachmentId").doesNotExist())
.andExpect(jsonPath("$.data.updatedAt").value("2026-06-16T12:00:00"));
```

附件测试对 4 个字段逐一断言 `doesNotExist()`。

- [ ] **Step 2: 验证契约测试先失败**

Run:

```powershell
mvn '-Dtest=PublicArticleControllerTest,ArticleOpenApiTest,AdminAttachmentControllerTest,AttachmentOpenApiTest' test
```

Expected: 新 schema 断言失败。

- [ ] **Step 3: 收窄公开文章 result/VO**

- page 保留 `locked` 和 `coverUrl`，删除 `status`、`coverAttachmentId`。
- detail 删除 `status`、`coverAttachmentId`，增加 `LocalDateTime updatedAt`。
- domain 查询仍可保留 status/attachment ID 供 application 判定锁定和解析 URL，不能为了 DTO 收窄破坏内部逻辑。
- `PublicArticleDetail` 若当前缺少 `updatedAt`，同步 projection、XML select 与映射读取该列。

- [ ] **Step 4: 收窄 AttachmentVO**

最终字段为：

```java
public record AttachmentVO(
        long id,
        String publicUrl,
        String contentType,
        long fileSize,
        int width,
        int height,
        String originalFilename,
        LocalDateTime createdAt,
        Long createdBy) {
}
```

application `AttachmentResult` 可以继续持有存储操作所需字段；只在 Web contract 做最小化。

- [ ] **Step 5: 同步 API 文档并运行验证**

Run:

```powershell
mvn '-Dtest=PublicArticleQueryServiceTest,PublicArticleControllerTest,ArticleOpenApiTest,AdminAttachmentControllerTest,AttachmentOpenApiTest,AttachmentIntegrationTest' test
```

Expected: 目标响应字段通过，正文、封面 URL、附件上传/查询行为不变。

- [ ] **Step 6: 检查并提交**

```powershell
git diff --stat
git status --short
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/AttachmentVO.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system' 'docs/project-handbook/api-contract'
git commit -m '收窄文章与附件响应字段'
```

---

### Task 7: 同步契约语义与发布检查

**Files:**
- Modify: `docs/project-handbook/api-contract/article.md`
- Modify: `docs/project-handbook/api-contract/comment.md`
- Modify: `docs/project-handbook/rules/error-handling.md`
- Modify: `docs/project-handbook/rules/security-baseline.md`
- Modify: `docs/project-handbook/workflows/build-and-test.md`
- Create: `docs/project-handbook/workflows/release-checklist.md`

- [ ] **Step 1: 固化时间契约**

明确所有 `LocalDateTime` 响应和后台日期参数均为 Asia/Tokyo 本地时间、格式 `yyyy-MM-dd'T'HH:mm:ss`，不携带 offset。前端必须按 JST 语义解析，不能直接依赖浏览器本地时区。注明这是 ADR-0018 的既有决定，不把 `xxx` 追加到 `LocalDateTime` format。

- [ ] **Step 2: 固化评论分页语义**

在 comment contract 明确：

- `total` 只统计根评论；
- 当前页每个根评论的 `replies` 完整返回；
- 当前版本不提供 reply 子分页；
- 评论规模达到可观测响应体/延迟阈值后，再引入独立 replies endpoint。

- [ ] **Step 3: 修正错误码规则漂移**

保留 10/20/30/40/50 段作为预留空间，但删除“每个业务错误必须使用模块码”的强制表述。写明：首版复用稳定语义码；只有前端需要对某一业务原因做稳定分支时，才新增模块码，并同步 API contract 和测试。

- [ ] **Step 4: 增加部署检查**

发布检查必须区分：

1. 同源反代：CORS 空列表允许；验证 `/api/**` 转发后路径前缀没有被剥离。
2. 跨域前端：必须设置 `MYBLOG_CORS_ALLOWED_ORIGINS` 为具体 origin，并验证 OPTIONS 预检。
3. 反向代理：必须设置 `MYBLOG_WEB_TRUSTED_PROXIES` 为实际代理 IP/CIDR；禁止默认信任整个私网；验证两台客户端产生不同的解析 IP/限流键。
4. 直连 Spring Boot：trusted proxies 保持空列表。

- [ ] **Step 5: 文档自检并提交**

Run:

```powershell
rg -n 'total.*根评论|Asia/Tokyo|MYBLOG_WEB_TRUSTED_PROXIES|MYBLOG_CORS_ALLOWED_ORIGINS|预留空间' 'docs/project-handbook'
git diff --check
git diff --stat
git status --short
```

Expected: 五类语义均可定位，无空白错误，只修改相关手册文件。

```powershell
git add -- 'docs/project-handbook'
git commit -m '同步后端契约与发布检查'
```

---

### Task 8: 阶段级全量验证

**Files:**
- No code changes expected.

- [ ] **Step 1: 确认工作树状态**

Run: `git status --short`

Expected: 干净；若不干净，先识别并拆分，不把验证产物提交。

- [ ] **Step 2: fresh H2 全量**

```powershell
Set-Location 'MyBlog-springboot-v2'
mvn clean test
```

Expected: 0 failures、0 errors；记录 tests/skipped 总数。

- [ ] **Step 3: MySQL 广泛回归**

使用环境变量提供凭据，不把密码写入命令、日志或文档。目标数据库只允许 `myblog_v2_dev`；连接参数保留当前已验证的 `allowPublicKeyRetrieval=true` 和 Asia/Tokyo session 配置。

Expected: 0 failures、0 errors；Docker 条件测试若仍因环境缺失跳过，单独记录，不伪装为通过。

- [ ] **Step 4: 最终范围检查**

```powershell
Set-Location 'E:\My-Blog\.worktrees\backend-v2-refactor'
git log --oneline -10
git status --short
```

Expected: 每个 Task 一个明确目的的中文提交，工作树干净，无外部 deploy 文件、密码或测试数据库产物。

## 2. 完成标准

- 未知路由返回 `404 + 90003`。
- 评论关键词由配置驱动，PASS/PENDING/approve 闭环均有测试。
- 后台文章分页标签查询从 N 次降为一次批量查询。
- PASSWORD 请求在 403 前不读取正文。
- Web 层对 Domain 的依赖只剩 5 个精确白名单枚举。
- 公开文章和附件响应不再暴露已裁决的内部/冗余字段，公开详情包含 `updatedAt`。
- 时间、评论分页、错误码、trusted proxy、CORS 契约同步完成。
- H2 与授权 MySQL 回归均为 0 failures、0 errors。
