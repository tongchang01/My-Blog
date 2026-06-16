# Backend V2 评论与留言纵向切片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 分五个小批次完成 V2 评论、留言板、后台审核、Markdown 安全清洗、文章评论计数和可关闭的邮件失败日志。

**Architecture:** 继续使用 `web -> application -> domain <- infrastructure`。`comment` 只通过 `content.application` 校验文章评论策略和维护评论计数；Markdown 渲染与限流作为 comment infrastructure 适配；邮件发送与失败日志作为 common-infra 能力，comment 只依赖端口。

**Tech Stack:** Java 17、Spring Boot 3.5.14、MyBatis-Plus 3.5.12、Mapper XML、MapStruct 1.6.3、Lombok、Caffeine、CommonMark、OWASP Java HTML Sanitizer、Resend HTTP API、H2、JUnit 5、AssertJ、Mockito、MockMvc、springdoc/Knife4j。

---

## 0. 执行约束

- 设计依据：`docs/superpowers/specs/2026-06-16-backend-v2-comment-design.md`。
- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`。
- 不修改冻结的 `MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql`。
- 不使用 `@Select`、`@Insert`、`@Update`、`@Delete` 注解 SQL。
- 不使用 `deleteById`、`removeById` 或物理删除评论。
- DTO、Command、Result、Page、查询条件优先使用 `record`。
- Spring 依赖注入类使用 Lombok `@RequiredArgsConstructor`。
- 生产代码保留必要中文业务注释，尤其是复杂 SQL 和安全清洗策略。
- 所有时间来自注入的 `Clock`。
- 每个 Task 执行 RED -> GREEN -> 定向回归 -> 静态检查 -> 独立中文提交。
- 不使用子代理；按当前会话顺序执行。
- Docker 不作为通过前提，只允许既有 Testcontainers 条件测试在 Docker 不可用时 skipped。

## 1. 提交拆分

1. `建立评论领域模型与安全渲染`
   - 依赖、Markdown 清洗、Comment 聚合、Entity、Mapper XML、Repository。
2. `实现公开评论与留言提交`
   - 文章评论策略端口、公开查询、提交、回复、限流、评论计数。
3. `实现后台评论审核管理`
   - 后台分页、审核通过、隐藏、恢复、软删除。
4. `实现评论邮件通知与失败日志`
   - common mail 端口、Resend 适配、失败日志、事务后通知。
5. `完成评论接口契约与集成验证`
   - OpenAPI、接口文档、真实 HTTP 集成、状态文档和全量验证。

## 2. 文件结构

### 2.1 Task 1 新建/修改

```text
MyBlog-springboot-v2/pom.xml

MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/
├── domain/
│   ├── Comment.java
│   ├── CommentAuditStatus.java
│   ├── CommentAuthor.java
│   ├── CommentContent.java
│   ├── CommentMarkdownRenderer.java
│   ├── CommentRepository.java
│   ├── CommentTarget.java
│   ├── CommentTargetType.java
│   └── NewComment.java
└── infrastructure/
    ├── markdown/
    │   └── CommonMarkCommentMarkdownRenderer.java
    └── persistence/
        ├── entity/CommentEntity.java
        ├── mapper/CommentMapper.java
        ├── mapping/CommentPersistenceMapping.java
        └── repository/MyBatisCommentRepository.java

MyBlog-springboot-v2/src/main/resources/mapper/comment/CommentMapper.xml

MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/
├── domain/CommentDomainTest.java
├── infrastructure/markdown/CommonMarkCommentMarkdownRendererTest.java
└── infrastructure/persistence/DatabaseCommentRepositoryTest.java
```

### 2.2 Task 2 新建/修改

```text
content/application/article/
├── ArticleCommentCountService.java
├── ArticleCommentPolicy.java
└── ArticleCommentPolicyService.java

comment/application/
├── CommentCreateCommand.java
├── CommentCreateResult.java
├── CommentCreateService.java
├── CommentPageQuery.java
├── CommentPageResult.java
├── CommentQueryService.java
├── CommentRateLimitService.java
└── DuplicateCommentGuard.java

comment/domain/
├── CommentPage.java
├── CommentPageItem.java
└── CommentQueryRepository.java

comment/infrastructure/
├── persistence/projection/CommentPageRow.java
├── persistence/repository/MyBatisCommentQueryRepository.java
└── ratelimit/CaffeineCommentRateLimiter.java

comment/web/
├── PublicArticleCommentController.java
├── PublicGuestbookCommentController.java
├── PublicCommentCreateRequest.java
├── PublicCommentVO.java
└── PublicCommentOpenApiRequest.java
```

修改：

```text
content/domain/article/ArticleRepository.java
content/infrastructure/persistence/mapper/ArticleMapper.java
src/main/resources/mapper/content/ArticleMapper.xml
src/main/resources/application.yml
src/main/resources/application-test.yml
```

### 2.3 Task 3 新建/修改

```text
comment/application/
├── AdminCommentCommandService.java
├── AdminCommentPageQuery.java
├── AdminCommentPageResult.java
└── AdminCommentQueryService.java

comment/domain/
├── AdminCommentPage.java
├── AdminCommentPageItem.java
└── AdminCommentQueryRepository.java

comment/infrastructure/persistence/
├── projection/AdminCommentPageRow.java
└── repository/MyBatisAdminCommentQueryRepository.java

comment/web/
├── AdminCommentController.java
├── AdminCommentPageItemVO.java
└── AdminCommentOpenApiRequest.java
```

### 2.4 Task 4 新建/修改

```text
common/mail/
├── MailFailureLog.java
├── MailFailureLogRepository.java
├── MailSendCommand.java
├── MailSendResult.java
└── MailSender.java

common/infrastructure/mail/
├── NoopMailSender.java
├── ResendMailSender.java
├── ResendMailProperties.java
├── MailFailureLogEntity.java
├── MailFailureLogMapper.java
└── MyBatisMailFailureLogRepository.java

src/main/resources/mapper/common/MailFailureLogMapper.xml

comment/application/
├── CommentNotificationEvent.java
└── CommentNotificationService.java
```

### 2.5 Task 5 新建/修改

```text
docs/project-handbook/api-contract/comment.md
docs/project-handbook/api-contract/README.md
docs/project-handbook/status.md
docs/project-handbook/roadmap.md
docs/project-handbook/pitfalls.md
docs/superpowers/specs/2026-06-16-backend-v2-comment-design.md
docs/superpowers/plans/2026-06-16-backend-v2-comment.md

src/test/java/com/tyb/myblog/v2/comment/integration/CommentIntegrationTest.java
src/test/java/com/tyb/myblog/v2/comment/web/CommentOpenApiTest.java
```

## 3. Task 1：评论领域模型与安全渲染

- [ ] **Step 1：写 Markdown 清洗失败测试**

Create `CommonMarkCommentMarkdownRendererTest`，覆盖：

- `**bold**` 输出 `<strong>`。
- 原始 `<script>` 被移除。
- `javascript:` 链接被移除。
- 图片、表格、原始 HTML 不进入输出白名单。

Run:

```powershell
mvn "-Dtest=CommonMarkCommentMarkdownRendererTest" test
```

Expected：编译失败，缺少 renderer。

- [ ] **Step 2：写 Comment 领域失败测试**

Create `CommentDomainTest`，覆盖：

- 顶层评论与回复的 parent/reply 规则。
- GUESTBOOK targetId 必须是 0。
- contentMd 必填且不超过 5000。
- email 格式、site 协议、nickname 长度校验。
- PASS/PENDING/HIDDEN 枚举 DB 值转换。

Run:

```powershell
mvn "-Dtest=CommentDomainTest" test
```

Expected：编译失败，缺少 domain 类型。

- [ ] **Step 3：引入 Markdown 依赖并实现 renderer**

修改 `pom.xml`：

```xml
<commonmark.version>0.24.0</commonmark.version>
<owasp-html-sanitizer.version>20240325.1</owasp-html-sanitizer.version>
```

新增依赖：

```xml
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark</artifactId>
    <version>${commonmark.version}</version>
</dependency>
<dependency>
    <groupId>com.googlecode.owasp-java-html-sanitizer</groupId>
    <artifactId>owasp-java-html-sanitizer</artifactId>
    <version>${owasp-html-sanitizer.version}</version>
</dependency>
```

`CommonMarkCommentMarkdownRenderer` 使用 `Parser.builder().build()` 和 `HtmlRenderer.builder().escapeHtml(true).build()`，再用 `Sanitizers.FORMATTING.and(Sanitizers.LINKS)` 清洗，禁止图片。

- [ ] **Step 4：实现 Comment 领域对象**

核心类型：

```text
CommentTargetType: ARTICLE(1), GUESTBOOK(2)
CommentAuditStatus: PASS(1), PENDING(2), HIDDEN(3)
CommentTarget: targetType + targetId
CommentAuthor: userId + nickname + email + site + ip + userAgent
CommentContent: markdown + safeHtml
Comment: reconstitute + approve/hide/restore/softDelete
NewComment: create
```

所有校验失败抛 `ApiException(ApiErrorCode.VALIDATION_ERROR)`。

- [ ] **Step 5：写 Repository 失败测试**

Create `DatabaseCommentRepositoryTest`，用 SQL 准备文章、评论和回复，覆盖：

- 插入顶层评论和回复。
- 根据 id 查询 active 评论。
- 回复跨 target 时应用层能拿到原 target。
- softDelete/restore/approve/hide 使用显式 SQL。

Run:

```powershell
mvn "-Dtest=DatabaseCommentRepositoryTest" test
```

Expected：编译失败，缺少 repository。

- [ ] **Step 6：实现 Entity、Mapper XML、Repository**

`CommentMapper.xml` 必须包含：

- `commentColumns`
- `selectActiveById`
- `insertComment`
- `updateAuditStatus`
- `softDelete`
- `restore`

复杂 SQL 上方保留中文注释。禁止注解 SQL。

- [ ] **Step 7：运行 Task 1 验证**

```powershell
mvn "-Dtest=CommentDomainTest,CommonMarkCommentMarkdownRendererTest,DatabaseCommentRepositoryTest" test
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/comment
git diff --check
```

Expected：测试通过；`rg` 无结果；diff check 通过。

- [ ] **Step 8：提交 Task 1**

```powershell
git add MyBlog-springboot-v2/pom.xml `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment `
  MyBlog-springboot-v2/src/main/resources/mapper/comment `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment
git diff --cached --check
git commit -m "建立评论领域模型与安全渲染"
```

## 4. Task 2：公开评论与留言提交

- [ ] **Step 1：写 content 评论策略端口失败测试**

Create `ArticleCommentPolicyServiceTest`，覆盖：

- PUBLISHED 允许评论。
- PASSWORD 返回 forbidden。
- DRAFT/PRIVATE/SCHEDULED/已删除返回 not found。
- commentCount delta 不能减到负数。

Run:

```powershell
mvn "-Dtest=ArticleCommentPolicyServiceTest" test
```

Expected：编译失败。

- [ ] **Step 2：实现 content application 端口**

新增：

```text
ArticleCommentPolicyService
ArticleCommentPolicy
ArticleCommentCountService
```

`ArticleMapper.xml` 增加按 id 查询评论策略和 `incrementCommentCount`，禁止 comment 模块直接改 `t_article`。

- [ ] **Step 3：写公开查询与提交失败测试**

Create `CommentCreateServiceTest`、`CommentQueryServiceTest`、`PublicCommentControllerTest`，覆盖：

- 匿名提交 PUBLISHED 文章评论。
- 命中黑名单入 PENDING，不增加 commentCount。
- PASS 评论增加 commentCount。
- GUESTBOOK 提交不增加 commentCount。
- 回复同 target 的评论成功，跨 target 拒绝。
- 限流返回 `429`。
- 公开列表只返回 PASS 且未删除评论，并按顶层 + replies 展示。

Run:

```powershell
mvn "-Dtest=CommentCreateServiceTest,CommentQueryServiceTest,PublicCommentControllerTest" test
```

Expected：编译失败。

- [ ] **Step 4：实现应用服务、限流和公开 Controller**

公开路径：

```text
GET  /api/public/articles/{articleId}/comments
POST /api/public/articles/{articleId}/comments
GET  /api/public/guestbook/comments
POST /api/public/guestbook/comments
```

`PublicCommentVO` 不得包含 `contentMd`、`email`、`ip`、`userAgent`。

- [ ] **Step 5：运行 Task 2 验证**

```powershell
mvn "-Dtest=ArticleCommentPolicyServiceTest,CommentCreateServiceTest,CommentQueryServiceTest,PublicCommentControllerTest" test
mvn "-Dtest=PublicArticleControllerTest,AdminArticleControllerTest" test
rg -n "contentMd|authorEmail|authorIp|authorUserAgent" src/main/java/com/tyb/myblog/v2/comment/web
git diff --check
```

Expected：测试通过；公开 VO 不泄露敏感字段。

- [ ] **Step 6：提交 Task 2**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment `
  MyBlog-springboot-v2/src/main/resources/mapper/content `
  MyBlog-springboot-v2/src/main/resources/mapper/comment `
  MyBlog-springboot-v2/src/main/resources/application.yml `
  MyBlog-springboot-v2/src/main/resources/application-test.yml `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment
git diff --cached --check
git commit -m "实现公开评论与留言提交"
```

## 5. Task 3：后台评论审核管理

- [ ] **Step 1：写后台查询和命令失败测试**

Create `AdminCommentQueryServiceTest`、`AdminCommentModerationServiceTest`、`AdminCommentControllerTest`，覆盖：

- ADMIN/DEMO 可读后台列表。
- 只有 ADMIN 可 approve/hide/restore/delete。
- PENDING -> PASS 增加文章 commentCount。
- PASS -> HIDDEN 减少文章 commentCount。
- PASS softDelete 减少文章 commentCount。
- HIDDEN restore 不增加 commentCount。
- includeDeleted=false 默认不显示已删除。

Run:

```powershell
mvn "-Dtest=AdminCommentQueryServiceTest,AdminCommentModerationServiceTest,AdminCommentControllerTest" test
```

Expected：编译失败。

- [ ] **Step 2：实现后台查询 Repository 和 Service**

后台路径：

```text
GET /api/admin/comments
```

支持 `targetType`、`targetId`、`auditStatus`、`keyword`、`includeDeleted`、`page`、`size`。

- [ ] **Step 3：实现审核、隐藏、恢复、软删除**

后台路径：

```text
POST   /api/admin/comments/{id}/approve
POST   /api/admin/comments/{id}/hide
POST   /api/admin/comments/{id}/restore
DELETE /api/admin/comments/{id}
```

状态变化和 commentCount 更新必须在同一事务内。

- [ ] **Step 4：运行 Task 3 验证**

```powershell
mvn "-Dtest=AdminCommentQueryServiceTest,AdminCommentModerationServiceTest,AdminCommentControllerTest,SecurityConfigTest" test
rg -n "deleteById|removeById" src/main/java/com/tyb/myblog/v2/comment
git diff --check
```

Expected：测试通过；无物理删除 helper。

- [ ] **Step 5：提交 Task 3**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment `
  MyBlog-springboot-v2/src/main/resources/mapper/comment `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment
git diff --cached --check
git commit -m "实现后台评论审核管理"
```

## 6. Task 4：邮件通知与失败日志

- [ ] **Step 1：写 mail 端口和失败日志测试**

Create `MailFailureLogRepositoryTest`、`ResendMailSenderTest`、`CommentNotificationServiceTest`，覆盖：

- Resend disabled 时不发 HTTP。
- Resend 返回非 2xx 时写失败日志。
- 成功发送不写 `t_mail_log`。
- 回复评论且 PASS 才触发通知事件。
- PENDING/HIDDEN/顶层评论不触发通知。

Run:

```powershell
mvn "-Dtest=MailFailureLogRepositoryTest,ResendMailSenderTest,CommentNotificationServiceTest" test
```

Expected：编译失败。

- [ ] **Step 2：实现 common mail 端口和 Mapper XML**

`MailFailureLogMapper.xml` 只写失败日志，字段映射 `t_mail_log`。错误消息需要截断和脱敏。

- [ ] **Step 3：实现 Resend 适配**

使用 JDK `HttpClient` 或 Spring `RestClient`。配置：

```yaml
myblog:
  mail:
    resend:
      enabled: false
      api-key: ${MYBLOG_RESEND_API_KEY:}
      from-email: ${MYBLOG_RESEND_FROM_EMAIL:}
```

生产默认关闭；开启但缺少 api key/from 时启动失败。

- [ ] **Step 4：接入评论事务后通知**

`CommentCreateService` 在评论入库事务提交后发布事件。`CommentNotificationService` 监听后读取被回复评论邮箱并发送。

- [ ] **Step 5：运行 Task 4 验证**

```powershell
mvn "-Dtest=MailFailureLogRepositoryTest,ResendMailSenderTest,CommentNotificationServiceTest,CommentCreateServiceTest" test
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/common src/main/java/com/tyb/myblog/v2/comment
git diff --check
```

Expected：测试通过；无注解 SQL。

- [ ] **Step 6：提交 Task 4**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment `
  MyBlog-springboot-v2/src/main/resources/mapper/common `
  MyBlog-springboot-v2/src/main/resources/application.yml `
  MyBlog-springboot-v2/src/main/resources/application-prod.yml `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment
git diff --cached --check
git commit -m "实现评论邮件通知与失败日志"
```

## 7. Task 5：契约、集成和收尾

- [ ] **Step 1：写真 HTTP 集成失败测试**

Create `CommentIntegrationTest`，覆盖：

- 匿名提交 PUBLISHED 文章评论成功。
- 公开列表返回 `contentHtml`，不返回邮箱/IP/UA/Markdown。
- PASSWORD 文章评论读取和提交返回 403。
- GUESTBOOK 留言和回复成功。
- 后台 approve/hide/delete/restore 后 commentCount 正确。
- DEMO 后台只读、写操作 403。

Run:

```powershell
mvn "-Dtest=CommentIntegrationTest" test
```

Expected：失败或编译失败，随后补齐缺失行为。

- [ ] **Step 2：写 OpenAPI 守护测试**

Create `CommentOpenApiTest`，检查：

- 公开和后台评论路径完整。
- 公开 DTO 不包含 `contentMd`、`authorEmail`、`authorIp`、`authorUserAgent`。
- OpenAPI 不暴露 Entity、Mapper、Repository、Command 内部类型。

- [ ] **Step 3：补接口契约文档**

Create `docs/project-handbook/api-contract/comment.md`，写清：

- 公开文章评论和留言板接口。
- 后台审核接口。
- 权限矩阵。
- 请求/响应字段。
- 审核状态和错误码。
- PASSWORD 评论暂不开放。
- 邮件失败不暴露给前端。

更新 `api-contract/README.md`，加入：

```markdown
| `comment.md` | 公开评论、留言板、后台审核、Markdown 清洗和邮件失败日志 | ✅ 已落地 |
```

- [ ] **Step 4：同步状态文档**

更新：

- `roadmap.md`：标记 comment 已完成。
- `status.md`：记录评论能力和延期项。
- `pitfalls.md`：将 U-006 / U-007 标记为关闭或更新。
- 本计划和设计文档回填提交拆分与最终验证统计。

- [ ] **Step 5：运行最终验证**

```powershell
mvn "-Dtest=CommentIntegrationTest,CommentOpenApiTest,ArticleIntegrationTest,SecurityConfigTest" test
mvn clean test
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/comment src/main/java/com/tyb/myblog/v2/common
rg -n "deleteById|removeById" src/main/java/com/tyb/myblog/v2/comment
rg -n "LocalDateTime\\.now\\(\\)|Instant\\.now\\(\\)" src/main/java/com/tyb/myblog/v2/comment src/main/java/com/tyb/myblog/v2/common
git diff --check
git diff --exit-code HEAD -- MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql
```

Expected：

- 全量测试通过，只有 Docker/Testcontainers 条件测试可 skipped。
- 静态审计无违规。
- V1 DDL 无差异。

- [ ] **Step 6：提交 Task 5**

```powershell
git add MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment `
  docs/project-handbook/api-contract `
  docs/project-handbook/roadmap.md `
  docs/project-handbook/status.md `
  docs/project-handbook/pitfalls.md `
  docs/superpowers/specs/2026-06-16-backend-v2-comment-design.md `
  docs/superpowers/plans/2026-06-16-backend-v2-comment.md
git diff --cached --check
git commit -m "完成评论接口契约与集成验证"
```

## 8. 最终验收清单

- [ ] `comment` 模块不依赖 `content.domain/web/infrastructure`。
- [ ] 所有生产 SQL 位于 XML。
- [ ] 评论 Markdown 安全清洗，公开接口只返回 `contentHtml`。
- [ ] 公开接口不泄露邮箱、IP、UA、`contentMd`。
- [ ] 后台接口可按目标、审核状态、关键字和删除状态筛选。
- [ ] 评论提交、审核、隐藏、删除、恢复正确维护文章 `comment_count`。
- [ ] GUESTBOOK 不影响文章计数。
- [ ] 跨目标回复和非法回复被拒绝。
- [ ] PASSWORD 文章评论在解锁能力落地前返回 403。
- [ ] 邮件默认关闭；开启失败写 `t_mail_log`，成功不入库。
- [ ] V1 DDL 未修改。
- [ ] 五个实施批次分别形成中文本地提交。
- [ ] `mvn clean test`、ArchUnit、OpenAPI 和静态审计全部通过。
