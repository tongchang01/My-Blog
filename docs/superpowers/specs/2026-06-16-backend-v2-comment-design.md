# Backend V2 评论与留言纵向切片设计

## 1. 目标

在当前 `com.tyb.myblog.v2` 架构下重建 `comment` 模块，覆盖：

- `t_comment` 的领域模型、MyBatis-Plus Entity、Mapper XML 和 Repository。
- 评论 Markdown 原文保存、HTML 清洗后保存，公开端只返回 `contentHtml`。
- 文章评论和留言板统一使用 `targetType + targetId`。
- 公开评论读取、提交、回复、留言板读取和留言提交。
- 后台评论分页、审核、隐藏、恢复、软删除。
- 评论数缓存：公开通过的文章评论变化时，同事务维护 `t_article.comment_count`。
- 回复邮件通知：只做可关闭的 Resend 适配与失败日志，模板多语言不进入本轮。

## 2. 不做

- 不修改冻结的 `V1__init.sql`。
- 不做 PASSWORD 文章解锁和 `X-Article-Token` 校验；PASSWORD 文章评论列表与提交暂时返回 `403`。
- 不做评论图片上传、表情包、富编辑器或实时通知。
- 不做复杂反垃圾策略；本轮只做本地 Caffeine 限流、重复内容拒绝和黑名单关键词审核。
- 不做邮件模板多语言。后续需要时按收件人语言新增模板系统。
- 不新增 Redis、RabbitMQ、Elasticsearch 或外键。

## 3. 关键决策

### 3.1 Markdown 安全

评论是匿名写入口，必须严格执行 `pitfalls.md` R-013：

1. 保存用户原文到 `content_md`。
2. 使用 CommonMark 解析 Markdown。
3. 解析阶段不允许原始 HTML 成为可信输出。
4. 使用 OWASP Java HTML Sanitizer 白名单清洗。
5. 公开响应只返回 `contentHtml`，不返回 `contentMd`、邮箱、IP、UA。

计划引入依赖：

- `org.commonmark:commonmark`
- `com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer`

### 3.2 评论目标

`CommentTarget` 是值对象：

- `ARTICLE`：`targetId` 必须是未删除文章，且当前允许评论。
- `GUESTBOOK`：`targetId` 固定为 `0`。

当前文章模块未实现 PASSWORD 解锁，所以：

- `PUBLISHED` 文章允许公开读取和提交评论。
- `PASSWORD` 文章评论读取与提交返回 `403`。
- `DRAFT`、`PRIVATE`、`SCHEDULED`、已删除文章按公开语义返回 `404`。

### 3.3 回复结构

评论最多两层：

- 顶层评论：`parentId = null`，`replyToCommentId = null`。
- 回复顶层评论：`parentId = 顶层评论 id`，`replyToCommentId = 顶层评论 id`。
- 回复子评论：`parentId = 同一顶层评论 id`，`replyToCommentId = 实际被回复评论 id`。

回复必须满足：

- 被回复评论存在、未删除、审核通过、未隐藏。
- 被回复评论的 `targetType + targetId` 等于当前提交目标。
- `replyToNickname` 保存被回复者昵称快照。
- 被回复者是系统用户时写入 `replyToUserId`，否则为空。

### 3.4 审核状态

`auditStatus` 使用枚举：

| 状态 | DB 值 | 公开展示 | 后台展示 | 进入方式 |
|------|------|----------|----------|----------|
| `PASS` | 1 | 是 | 是 | 未命中黑名单或后台审核通过 |
| `PENDING` | 2 | 否 | 是 | 命中黑名单 |
| `HIDDEN` | 3 | 否 | 是 | 后台手动隐藏 |

公开计数只统计：`targetType=ARTICLE`、未删除、`auditStatus=PASS`。

### 3.5 评论计数

`content` 模块需要暴露 application 端口，不让 `comment` 直接访问 content repository：

- `ArticleCommentPolicyService`：公开评论前校验文章是否允许评论。
- `ArticleCommentCountService`：在同一事务中按 `delta` 调整 `comment_count`。

`comment` 不依赖 `content.domain`、`content.infrastructure` 或 `content.web`，只依赖 `content.application`。

计数变化规则：

- 新评论入库为 `PASS` 且目标是文章：`+1`。
- `PENDING -> PASS`：`+1`。
- `PASS -> PENDING/HIDDEN`：`-1`。
- `HIDDEN/PENDING -> PASS`：`+1`。
- `PASS` 评论软删除：`-1`。
- 已删除评论恢复为 `PASS`：`+1`。

### 3.6 限流与重复内容

本轮使用本机 Caffeine，避免提前引入 Redis：

- 评论提交：同 IP 每分钟最多 5 条。
- 同 IP + 同 target + 同 `SHA-256(contentMd)` 5 分钟内重复提交直接拒绝。
- PASSWORD 解锁限流仍属于文章解锁后续计划。

限流失败返回 `429 + ApiErrorCode.RATE_LIMITED`。

### 3.7 邮件

邮件只在回复评论且入库为 `PASS` 后触发：

- 提交评论的事务先完成评论与计数。
- 事务提交后异步发送邮件，避免邮件失败回滚评论。
- 发送失败写入 `t_mail_log`，成功不入库。
- 邮件功能由 `myblog.mail.resend.enabled` 控制，默认关闭。
- 没有 API 返回邮件失败细节。

`t_mail_log` 属于 `common-infra`，代码放在 `common.mail` 或 `common.infrastructure.mail` 下，不放进 `comment`。

## 4. 接口草案

### 4.1 公开文章评论

```text
GET  /api/public/articles/{articleId}/comments?page=1&size=20
POST /api/public/articles/{articleId}/comments
```

提交请求：

```json
{
  "nickname": "TYB",
  "email": "tyb@example.com",
  "site": "https://example.com",
  "contentMd": "支持 **Markdown**",
  "replyToCommentId": null
}
```

公开响应项：

```json
{
  "id": "COMMENT_ID",
  "parentId": null,
  "replyToCommentId": null,
  "replyToNickname": null,
  "authorNickname": "TYB",
  "authorSite": "https://example.com",
  "contentHtml": "<p>支持 <strong>Markdown</strong></p>",
  "createdAt": "2026-06-16T12:00:00",
  "replies": []
}
```

### 4.2 公开留言板

```text
GET  /api/public/guestbook/comments?page=1&size=20
POST /api/public/guestbook/comments
```

请求体与文章评论一致，target 固定为 `GUESTBOOK + 0`。

### 4.3 后台评论管理

```text
GET    /api/admin/comments
POST   /api/admin/comments/{id}/approve
POST   /api/admin/comments/{id}/hide
POST   /api/admin/comments/{id}/restore
DELETE /api/admin/comments/{id}
```

后台查询参数：

| 字段 | 说明 |
|------|------|
| `targetType` | `ARTICLE` / `GUESTBOOK`，可选 |
| `targetId` | 目标 ID，可选 |
| `auditStatus` | `PASS` / `PENDING` / `HIDDEN`，可选 |
| `keyword` | 按昵称、邮箱、正文模糊匹配，可选 |
| `includeDeleted` | 是否包含软删除，默认 `false` |

后台响应允许返回邮箱、IP、UA、`contentMd`、`contentHtml` 和删除审计字段。

## 5. 文件结构

```text
comment/
├── application/
│   ├── CommentCreateService.java
│   ├── CommentQueryService.java
│   ├── AdminCommentQueryService.java
│   ├── AdminCommentModerationService.java
│   ├── CommentRateLimitService.java
│   └── CommentNotificationService.java
├── domain/
│   ├── Comment.java
│   ├── CommentAuthor.java
│   ├── CommentContent.java
│   ├── CommentRepository.java
│   ├── CommentTarget.java
│   ├── CommentTargetType.java
│   ├── CommentAuditStatus.java
│   └── CommentMarkdownRenderer.java
├── infrastructure/
│   ├── markdown/
│   ├── ratelimit/
│   └── persistence/
│       ├── entity/
│       ├── mapper/
│       ├── mapping/
│       └── repository/
└── web/
    ├── PublicArticleCommentController.java
    ├── PublicGuestbookCommentController.java
    └── AdminCommentController.java
```

content 需要新增 application 端口：

```text
content/application/article/
├── ArticleCommentPolicyService.java
└── ArticleCommentCountService.java
```

common-infra 需要新增邮件失败日志能力：

```text
common/mail/
├── MailFailureLog.java
├── MailFailureLogRepository.java
├── MailSender.java
└── MailSendCommand.java
```

## 6. 验收标准

- 评论 Markdown 清洗测试覆盖脚本、事件属性、危险协议和原始 HTML。
- 公开接口不返回 `contentMd`、`authorEmail`、`authorIp`、`authorUserAgent`。
- 后台接口可查看审核、隐藏、删除和恢复所需字段。
- 文章评论提交和审核状态变化正确维护 `comment_count`。
- GUESTBOOK 不触碰文章计数。
- 跨目标回复、回复隐藏评论、回复已删除评论全部拒绝。
- PASSWORD 文章评论在解锁能力落地前返回 `403`。
- 所有生产 SQL 位于 Mapper XML，并保留必要中文业务注释。
- `comment` 只依赖 `content.application`，不跨模块访问 content 持久化。
- Resend 默认关闭；开启后失败写 `t_mail_log`，成功不入库。
- `mvn clean test`、ArchUnit、OpenAPI 守护和静态 SQL 审计通过。
