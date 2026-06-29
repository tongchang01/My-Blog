# 后端 V2 首版权限边界实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不实现 PASSWORD 解锁的前提下，限制公开 DEMO 账号读取未公开文章正文和评论审计敏感字段，并同步首版产品与接口边界。

**Architecture:** 保留现有 ADMIN/DEMO 后台 GET 鉴权和单一 Repository 查询，在 application 查询服务生成响应结果时按 `AuthenticatedPrincipal.roles()` 裁剪字段。Web 映射、数据库投影和 Security 路由保持不变；PASSWORD 公开详情与评论继续返回现有 `403 + 10003`。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring Security、JUnit 5、Mockito、AssertJ、MockMvc、Maven、H2、MySQL 8、PowerShell

---

## 执行边界

- 执行模式固定为 **Inline Execution**，不使用子代理；实施前读取 `superpowers:executing-plans`，每完成一个任务停下检查结果。
- 每次提交前运行 `git diff --stat` 和 `git status --short`；每次 `git add` 只暂存任务列出的精确路径，禁止 `git add .` 或 `git add -A`。
- 不修改 `SecurityConfig`、Controller、Web VO、Repository、Mapper 或数据库结构。
- 不实现 `/api/public/articles/{id}/unlock`、article access token、PASSWORD 解锁限流或评论授权缓存。
- 不修改历史审查报告 `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`。
- MySQL 验证只连接用户授权的 `myblog_v2_dev`；密码只从当前进程环境变量读取，不写入文件、命令或提交。

### Task 1: 裁剪 DEMO 的非公开文章正文

**Files:**
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/AdminArticleQueryServiceTest.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/AdminArticleDetailResult.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/ArticleQueryService.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/integration/ArticleIntegrationTest.java`

- [ ] **Step 1: 给应用服务写角色与状态矩阵的失败测试**

在 `AdminArticleQueryServiceTest` 增加参数化测试导入：

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
```

把现有 `detail(long id, Long coverId)` 改成接收状态：

```java
private AdminArticleDetail detail(
        long id,
        Long coverId,
        ArticleStatus status) {
    return new AdminArticleDetail(
            id,
            "标题",
            null,
            null,
            "摘要",
            null,
            null,
            "正文",
            10L,
            "分类",
            1001L,
            "article-" + id,
            status,
            NOW.minusDays(1),
            coverId,
            null,
            2,
            List.of(20L, 30L),
            NOW.minusDays(2),
            1001L,
            NOW.minusHours(1),
            1001L);
}
```

更新现有调用为 `detail(10L, 300L, ArticleStatus.PASSWORD)`，并新增：

```java
@Test
void adminCanReadBodyForEveryArticleStatus() {
    for (ArticleStatus status : ArticleStatus.values()) {
        long id = 100L + status.databaseValue();
        when(repository.findActiveDetail(id))
                .thenReturn(Optional.of(detail(id, null, status)));

        assertThat(service.adminDetail(principal("ADMIN"), id).body())
                .as(status.name())
                .isEqualTo("正文");
    }
}

@Test
void demoCanReadPublishedBody() {
    when(repository.findActiveDetail(10L))
            .thenReturn(Optional.of(detail(
                    10L,
                    null,
                    ArticleStatus.PUBLISHED)));

    assertThat(service.adminDetail(principal("DEMO"), 10L).body())
            .isEqualTo("正文");
}

@ParameterizedTest
@EnumSource(
        value = ArticleStatus.class,
        names = {"DRAFT", "PRIVATE", "PASSWORD", "SCHEDULED"})
void demoCannotReadNonPublishedBody(ArticleStatus status) {
    when(repository.findActiveDetail(10L))
            .thenReturn(Optional.of(detail(10L, null, status)));

    assertThat(service.adminDetail(principal("DEMO"), 10L).body())
            .isNull();
}
```

- [ ] **Step 2: 运行单元测试并确认 RED**

在 `MyBlog-springboot-v2/` 运行：

```powershell
mvn '-Dtest=AdminArticleQueryServiceTest' test
```

Expected: `demoCannotReadNonPublishedBody` 失败，实际正文仍为 `正文`；ADMIN 与 PUBLISHED 用例通过。

- [ ] **Step 3: 在 application 结果构造时实现最小裁剪**

把 `AdminArticleDetailResult.from` 改为显式接收是否包含正文：

```java
public static AdminArticleDetailResult from(
        AdminArticleDetail detail,
        String coverUrl,
        boolean includeBody) {
    return new AdminArticleDetailResult(
            detail.id(),
            detail.titleZh(),
            detail.titleJa(),
            detail.titleEn(),
            detail.summaryZh(),
            detail.summaryJa(),
            detail.summaryEn(),
            includeBody ? detail.body() : null,
            detail.categoryId(),
            detail.categoryNameZh(),
            detail.authorId(),
            detail.slug(),
            detail.status(),
            detail.publishAt(),
            detail.coverAttachmentId(),
            coverUrl,
            detail.commentCount(),
            detail.tagIds(),
            detail.createdAt(),
            detail.createdBy(),
            detail.updatedAt(),
            detail.updatedBy());
}
```

在 `ArticleQueryService.adminDetail` 中用 ADMIN 优先、PUBLISHED 次之的规则调用：

```java
boolean includeBody = principal.roles().contains("ADMIN")
        || detail.status() == ArticleStatus.PUBLISHED;
return AdminArticleDetailResult.from(
        detail,
        coverUrl,
        includeBody);
```

同时添加：

```java
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
```

该判断保证同时具有 ADMIN/DEMO 角色时仍按 ADMIN 返回完整正文。

- [ ] **Step 4: 运行单元测试并确认 GREEN**

```powershell
mvn '-Dtest=AdminArticleQueryServiceTest' test
```

Expected: BUILD SUCCESS，全部 `AdminArticleQueryServiceTest` 通过。

- [ ] **Step 5: 增加真实 HTTP 响应回归**

在 `ArticleIntegrationTest` 创建三篇文章之后、修改 PASSWORD 文章之前增加：

```java
mockMvc.perform(get("/api/admin/articles/{id}", publishedId)
                .header("Authorization", bearer(demo)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.body").value("正文"));
mockMvc.perform(get("/api/admin/articles/{id}", passwordId)
                .header("Authorization", bearer(demo)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.body").value((Object) null));
mockMvc.perform(get("/api/admin/articles/{id}", scheduledId)
                .header("Authorization", bearer(demo)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.body").value((Object) null));
mockMvc.perform(get("/api/admin/articles/{id}", passwordId)
                .header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.body").value("正文"));
```

单元测试覆盖全部五种状态，集成测试证明 JSON 字段保留且值为 `null`、ADMIN 行为不变。

- [ ] **Step 6: 运行 content 定向回归**

```powershell
mvn '-Dtest=AdminArticleQueryServiceTest,ArticleIntegrationTest,AdminArticleControllerTest' test
```

Expected: BUILD SUCCESS；DEMO 列表仍可读、写操作仍为 403、公开 PASSWORD 详情仍为 403。

- [ ] **Step 7: 检查范围并提交**

```powershell
git diff --stat
git status --short
git diff --check
git add MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/AdminArticleQueryServiceTest.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/AdminArticleDetailResult.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/ArticleQueryService.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/integration/ArticleIntegrationTest.java
git diff --cached --stat
git commit -m "限制DEMO读取非公开文章正文"
```

Expected: 提交只包含上述四个文件。

### Task 2: 隐藏 DEMO 的评论审计字段

**Files:**
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/application/AdminCommentQueryServiceTest.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/AdminCommentQueryService.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/integration/CommentIntegrationTest.java`

- [ ] **Step 1: 写 ADMIN 完整、DEMO 脱敏的失败测试**

在 `AdminCommentQueryServiceTest` 提取：

```java
private static final AdminCommentPageQuery QUERY =
        new AdminCommentPageQuery(
                CommentTargetType.ARTICLE,
                100L,
                CommentAuditStatus.PENDING,
                "hello",
                false,
                1,
                20);

private static final AdminCommentQueryCriteria CRITERIA =
        new AdminCommentQueryCriteria(
                CommentTargetType.ARTICLE,
                100L,
                CommentAuditStatus.PENDING,
                "hello",
                false,
                1,
                20);

private static AuthenticatedPrincipal principal(String role) {
    return new AuthenticatedPrincipal(
            "1001",
            role.toLowerCase(),
            List.of(role));
}
```

用以下两个测试替换只检查记录数量的 `adminAndDemoCanReadCommentPage`：

```java
@Test
void adminCanReadCommentAuditFields() {
    when(repository.page(CRITERIA))
            .thenReturn(new AdminCommentPage(List.of(item()), 1, 1, 20));

    AdminCommentPageResult.Item result = service.page(
            principal("ADMIN"), QUERY).records().get(0);

    assertThat(result.authorEmail()).isEqualTo("tyb@example.com");
    assertThat(result.authorIp()).isEqualTo("127.0.0.1");
    assertThat(result.authorUserAgent()).isEqualTo("JUnit");
    verify(repository).page(CRITERIA);
}

@Test
void demoCanReadCommentWithoutAuditFields() {
    when(repository.page(CRITERIA))
            .thenReturn(new AdminCommentPage(List.of(item()), 1, 1, 20));

    AdminCommentPageResult.Item result = service.page(
            principal("DEMO"), QUERY).records().get(0);

    assertThat(result.authorEmail()).isNull();
    assertThat(result.authorIp()).isNull();
    assertThat(result.authorUserAgent()).isNull();
    assertThat(result.authorNickname()).isEqualTo("TYB");
    assertThat(result.contentMd()).isEqualTo("hello");
    verify(repository).page(CRITERIA);
}
```

- [ ] **Step 2: 运行单元测试并确认 RED**

```powershell
mvn '-Dtest=AdminCommentQueryServiceTest' test
```

Expected: DEMO 用例失败，三个审计字段仍为原值；ADMIN 用例通过。

- [ ] **Step 3: 在 application 映射时实现最小裁剪**

在 `AdminCommentQueryService.page` 中计算一次 ADMIN 可见性并传入映射：

```java
boolean includeAuditFields = principal.roles().contains("ADMIN");
return new AdminCommentPageResult(
        page.records().stream()
                .map(item -> toItem(item, includeAuditFields))
                .toList(),
        page.total(),
        page.page(),
        page.size());
```

把 `toItem` 改为：

```java
private static AdminCommentPageResult.Item toItem(
        AdminCommentPageItem item,
        boolean includeAuditFields) {
    return new AdminCommentPageResult.Item(
            item.id(),
            item.targetType(),
            item.targetId(),
            item.parentId(),
            item.replyToCommentId(),
            item.replyToNickname(),
            item.authorNickname(),
            includeAuditFields ? item.authorEmail() : null,
            item.authorSite(),
            includeAuditFields ? item.authorIp() : null,
            includeAuditFields ? item.authorUserAgent() : null,
            item.contentMd(),
            item.contentHtml(),
            item.auditStatus(),
            item.createdAt(),
            item.deleted());
}
```

ADMIN 优先规则同样保证混合角色不会被误脱敏。

- [ ] **Step 4: 运行单元测试并确认 GREEN**

```powershell
mvn '-Dtest=AdminCommentQueryServiceTest' test
```

Expected: BUILD SUCCESS，ADMIN 与 DEMO 用例均通过。

- [ ] **Step 5: 增加真实 HTTP 字段级回归**

给 `CommentIntegrationTest` 的第一条公开评论请求增加固定 User-Agent：

```java
long passedId = response(post("/api/public/articles/100/comments")
        .header("User-Agent", "JUnit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(commentBody(
                "Reader",
                "reader@example.com",
                "hello",
                null)))
        .at("/data/id").asLong();
```

把现有 DEMO 后台 GET 断言扩展为：

```java
mockMvc.perform(get("/api/admin/comments")
                .header("Authorization", bearer(demo)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].authorEmail")
                .value((Object) null))
        .andExpect(jsonPath("$.data.records[0].authorIp")
                .value((Object) null))
        .andExpect(jsonPath("$.data.records[0].authorUserAgent")
                .value((Object) null))
        .andExpect(jsonPath("$.data.records[0].authorNickname").exists())
        .andExpect(jsonPath("$.data.records[0].contentMd").exists());
mockMvc.perform(get("/api/admin/comments")
                .header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].authorEmail").exists())
        .andExpect(jsonPath("$.data.records[0].authorIp").exists())
        .andExpect(jsonPath("$.data.records[0].authorUserAgent").exists());
```

这里只断言 ADMIN 字段非空；精确值由应用服务单元测试固定，避免集成测试绑定 MockMvc 的 IP 表示细节。

- [ ] **Step 6: 运行 comment 定向回归**

```powershell
mvn '-Dtest=AdminCommentQueryServiceTest,CommentIntegrationTest,AdminCommentControllerTest' test
```

Expected: BUILD SUCCESS；DEMO GET 为 200 且字段为 JSON `null`，DEMO 审核写操作仍为 403。

- [ ] **Step 7: 检查范围并提交**

```powershell
git diff --stat
git status --short
git diff --check
git add MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/application/AdminCommentQueryServiceTest.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/AdminCommentQueryService.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/integration/CommentIntegrationTest.java
git diff --cached --stat
git commit -m "隐藏DEMO评论审计敏感字段"
```

Expected: 提交只包含上述三个文件。

### Task 3: 同步首版 PASSWORD 与 DEMO 有效文档

**Files:**
- Modify: `docs/project-handbook/product/use-cases.md`
- Modify: `docs/project-handbook/product/business-rules.md`
- Modify: `docs/project-handbook/product/decisions-draft.md`
- Modify: `docs/project-handbook/api-contract/article.md`
- Modify: `docs/project-handbook/api-contract/comment.md`
- Modify: `docs/project-handbook/rules/security-baseline.md`
- Modify: `docs/project-handbook/roadmap.md`

- [ ] **Step 1: 将 PASSWORD 完整解锁标为目标能力、首版固定锁定**

在 `use-cases.md` 的 G-03 与 G-06 中保留长期目标描述，并紧接着增加：

```markdown
> 首版范围：PASSWORD 完整解锁链路暂不提供。公开详情、评论列表和评论提交固定返回 `403 + 10003`；前端只实现锁定占位态，不调用 `/unlock`。
```

在 `business-rules.md` 的 BR-204 后增加：

```markdown
### BR-204A 首版 PASSWORD 例外

- PASSWORD 的完整密码校验与 article access token 是上线后目标能力。
- 首版公开列表只返回标题、摘要、锁标识和公开元数据。
- 首版公开详情、评论读取和评论提交固定返回 `403 + 10003`，不提供解锁接口。
```

把 BR-305 补充为长期规则，并明确首版受 BR-204A 覆盖。

- [ ] **Step 2: 记录 DEMO 字段裁剪裁决并消除旧授权冲突**

在 `decisions-draft.md` 的“DEMO 视角”与 R1 权限段落统一写成：

```markdown
- DEMO 是公开演示账号，后台 GET 端点保持可读，但 application 层必须裁剪敏感字段。
- 后台文章详情仅允许 DEMO 读取 PUBLISHED 正文；DRAFT、PRIVATE、PASSWORD、SCHEDULED 的 `body` 返回 `null`。
- 评论管理列表对 DEMO 保持可读，但 `authorEmail`、`authorIp`、`authorUserAgent` 返回 `null`；ADMIN 保持完整。
- 敏感读采用同一端点的字段级裁剪，不为 DEMO 复制 Controller、Repository 或 SQL。
```

删除或改写“评论管理页仅 ADMIN 可访问”“敏感 GET 必须单独 `hasRole('ADMIN')`”等与裁决冲突的句子。PASSWORD 长期流程段落前增加：

```markdown
> 以下为上线后目标方案；首版不提供 `/unlock`、article access token 或 PASSWORD 评论授权，当前边界以 BR-204A 和 API 契约为准。
```

- [ ] **Step 3: 更新文章与评论 API 契约**

在 `api-contract/article.md` 的后台文章详情加入：

```markdown
角色字段规则：

| 状态 | ADMIN `body` | DEMO `body` |
|---|---|---|
| PUBLISHED | 完整正文 | 完整正文 |
| DRAFT / PRIVATE / PASSWORD / SCHEDULED | 完整正文 | `null` |

`body` 字段始终存在；DEMO 无权读取时返回 JSON `null`，不省略字段。任何角色均不得获得密码或 `passwordHash`。
```

在 `api-contract/comment.md` 的后台评论 GET 中把原“后台响应包含邮箱、IP、UA”改为：

```markdown
字段权限：

- ADMIN：`authorEmail`、`authorIp`、`authorUserAgent` 返回完整审计值。
- DEMO：上述三个字段保留在响应中但固定为 `null`。
- 两种角色均可读取昵称、站点、`contentMd`、`contentHtml`、审核状态和时间字段。
```

- [ ] **Step 4: 更新安全基线与路线图**

把 `security-baseline.md` 第 6 节的后台 GET 规则统一为：

```markdown
| `/api/admin/** GET` | hasAnyRole('ADMIN','DEMO')；application 层按角色裁剪敏感字段 |
```

并将敏感读红线改为：

```markdown
🔴 敏感读：公开 DEMO 账号不得获得未公开文章正文或评论审计字段。允许 DEMO 读取的后台 GET 必须在 application 层返回字段级裁剪结果，Controller、Web mapping 和 Repository 不得各自重复判断角色。
```

在 PASSWORD token 章节顶部增加首版状态说明，避免把目标方案表述为当前已落地能力。

确认 `roadmap.md` 的 `PASSWORD 文章访问 token 完整流程` 仍位于 L2，并补充：

```markdown
- [x] 首版 PASSWORD 锁定态与 DEMO 敏感字段裁剪边界冻结
```

该已完成项放在 M3 收尾或 M4 开始前，不把完整解锁误标为完成。

- [ ] **Step 5: 做文档一致性检查**

在仓库根目录运行：

```powershell
rg -n "PASSWORD|/unlock|article access token|DEMO|评论审计|敏感读|hasRole\('ADMIN'\)" docs/project-handbook/product/use-cases.md docs/project-handbook/product/business-rules.md docs/project-handbook/product/decisions-draft.md docs/project-handbook/api-contract/article.md docs/project-handbook/api-contract/comment.md docs/project-handbook/rules/security-baseline.md docs/project-handbook/roadmap.md
rg -n "评论管理页.*仅 ADMIN|敏感读接口.*hasRole|DEMO.*完整.*authorEmail|DEMO.*PRIVATE.*正文" docs/project-handbook
git diff --check
```

Expected: 第一条搜索中所有长期 PASSWORD 描述均明确标注目标/上线后，首版描述与 API 契约一致；第二条不再命中冲突规则；`git diff --check` 无错误。

- [ ] **Step 6: 检查范围并提交**

```powershell
git diff --stat
git status --short
git add docs/project-handbook/product/use-cases.md docs/project-handbook/product/business-rules.md docs/project-handbook/product/decisions-draft.md docs/project-handbook/api-contract/article.md docs/project-handbook/api-contract/comment.md docs/project-handbook/rules/security-baseline.md docs/project-handbook/roadmap.md
git diff --cached --stat
git commit -m "冻结首版PASSWORD与DEMO权限边界"
```

Expected: 提交只包含上述七个有效文档，不包含设计稿、计划、历史审查报告或实现文件。

### Task 4: 完成安全、H2 与 MySQL 验收

**Files:**
- Verify only: `MyBlog-springboot-v2/**`

- [ ] **Step 1: 运行权限相关定向回归**

在 `MyBlog-springboot-v2/` 运行：

```powershell
mvn '-Dtest=AdminArticleQueryServiceTest,ArticleIntegrationTest,AdminCommentQueryServiceTest,CommentIntegrationTest,AdminArticleControllerTest,AdminCommentControllerTest,SecurityConfigTest,ArchitectureRulesTest' test
```

Expected: BUILD SUCCESS；文章/评论角色矩阵、Security 路由与 ArchUnit 全部通过。

- [ ] **Step 2: 运行 fresh H2 全量测试**

先清除可能残留的 MySQL 覆盖变量：

```powershell
Remove-Item Env:SPRING_DATASOURCE_URL -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_DATASOURCE_USERNAME -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_DATASOURCE_DRIVER_CLASS_NAME -ErrorAction SilentlyContinue
Remove-Item Env:SPRING_FLYWAY_ENABLED -ErrorAction SilentlyContinue
mvn clean test
```

Expected: BUILD SUCCESS，0 failures，0 errors，4 skipped；测试总数应高于上次 fresh H2 的 615，最终以 Surefire 汇总为准。

- [ ] **Step 3: 运行本地 MySQL 广泛回归**

确认当前进程已经安全提供 `SPRING_DATASOURCE_PASSWORD`，再运行：

```powershell
if (-not $env:SPRING_DATASOURCE_PASSWORD) {
    throw '请先在当前进程设置 SPRING_DATASOURCE_PASSWORD'
}
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/myblog_v2_dev?useUnicode=true&characterEncoding=utf8&useSSL=false&connectionTimeZone=Asia/Tokyo&forceConnectionTimeToSession=true&sessionVariables=time_zone='%2B09:00'"
$env:SPRING_DATASOURCE_USERNAME = 'root'
$env:SPRING_DATASOURCE_DRIVER_CLASS_NAME = 'com.mysql.cj.jdbc.Driver'
$env:SPRING_FLYWAY_ENABLED = 'true'
mvn '-Dtest=**/*Test,!FlywayMigrationTest,!RefreshSessionTransactionIntegrationTest,!DatabasePasswordAccountRepositoryTest,!DatabaseUserProfileRepositoryTest' test
```

Expected: BUILD SUCCESS，0 failures，0 errors，4 skipped；日志明确显示连接 `jdbc:mysql://localhost:3306/myblog_v2_dev`。

- [ ] **Step 4: 检查提交边界和最终工作区**

回到仓库根目录运行：

```powershell
git log -4 --oneline
git status --short
git diff --stat
```

Expected: 最近提交依次包含计划、文章裁剪、评论裁剪和文档同步；没有未提交实现差异。如果出现编辑器格式化差异，按内容价值判断：无用则还原，有用则另开单一目的提交，不混入上述提交。

