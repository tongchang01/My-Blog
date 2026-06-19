# 评论与留言接口契约

> 当前状态：✅ 已落地。覆盖公开文章评论、留言板、后台审核、Markdown 安全清洗、文章评论计数和邮件失败日志。

## 1. 通用约定

- 响应体统一使用 `ApiResponse<T>`：成功 `code=00000`，失败按 `ApiErrorCode` 返回。
- 分页参数统一为 `page`、`size`，默认 `1 / 20`。
- 公开接口只返回清洗后的 `contentHtml`，不返回 `contentMd`、邮箱、IP、UA。
- 评论 Markdown 原文由后端解析和清洗后双写：`content_md` 保存原文，`content_html` 供前台渲染。
- PASSWORD 文章评论读取和提交在文章解锁能力落地前固定返回 `403 + 10003`。

## 2. 审核状态

| 状态 | DB 值 | 公开展示 | 说明 |
|------|------:|----------|------|
| `PASS` | 1 | 是 | 未命中黑名单或后台审核通过 |
| `PENDING` | 2 | 否 | 命中基础黑名单，等待后台审核 |
| `HIDDEN` | 3 | 否 | 后台手动隐藏 |

## 3. 公开文章评论

### `GET /api/public/articles/{articleId}/comments`

- Auth：匿名
- Query：`page`、`size`
- 成功：`200 + ApiResponse<PageResponse<PublicCommentVO>>`
- 错误：
  - `403 + 10003`：PASSWORD 文章暂不开放评论
  - `404 + 90003`：文章不存在、未发布、私密、定时或已删除

### `POST /api/public/articles/{articleId}/comments`

- Auth：匿名
- Request：

```json
{
  "nickname": "TYB",
  "email": "tyb@example.com",
  "site": "https://example.com",
  "contentMd": "hello",
  "replyToCommentId": null
}
```

- 成功：`200 + ApiResponse<PublicCommentCreateVO>`
- 响应字段：`id`、`auditStatus`
- 错误：
  - `400 + 90001`：昵称、邮箱、站点、正文或回复关系非法
  - `403 + 10003`：PASSWORD 文章暂不开放评论
  - `404 + 90003`：文章或被回复评论不存在
  - `409 + 90004`：跨目标回复、重复提交等冲突
  - `429 + 90002`：同 IP 评论提交过于频繁

## 4. 留言板

### `GET /api/public/guestbook/comments`

- Auth：匿名
- Query：`page`、`size`
- 成功：`200 + ApiResponse<PageResponse<PublicCommentVO>>`
- 留言板目标固定为 `targetType=GUESTBOOK`、`targetId=0`。

### `POST /api/public/guestbook/comments`

- Auth：匿名
- Request：同文章评论提交，`replyToCommentId` 可为空或指向同一留言板下已通过评论。
- 成功：`200 + ApiResponse<PublicCommentCreateVO>`
- 留言和回复不影响文章 `comment_count`。

## 5. 后台评论管理

### `GET /api/admin/comments`

- Auth：`ADMIN` / `DEMO`
- Query：
  - `targetType`：`ARTICLE` / `GUESTBOOK`
  - `targetId`
  - `auditStatus`：`PASS` / `PENDING` / `HIDDEN`
  - `keyword`：匹配作者昵称与 Markdown 原文
  - `includeDeleted`：默认 `false`
  - `page`、`size`
- 成功：`200 + ApiResponse<PageResponse<AdminCommentPageItemVO>>`
- 字段权限：
  - ADMIN：`authorEmail`、`authorIp`、`authorUserAgent` 返回完整审计值。
  - DEMO：上述三个字段保留在响应中，但固定为 `null`。
  - 两种角色均可读取昵称、站点、`contentMd`、`contentHtml`、审核状态和时间字段。

### 写操作

| Method | Path | Auth | 行为 |
|--------|------|------|------|
| `POST` | `/api/admin/comments/{id}/approve` | `ADMIN` | `PENDING/HIDDEN -> PASS`；文章评论公开计数必要时 +1 |
| `POST` | `/api/admin/comments/{id}/hide` | `ADMIN` | `PASS/PENDING -> HIDDEN`；文章评论公开计数必要时 -1 |
| `DELETE` | `/api/admin/comments/{id}` | `ADMIN` | 软删除；已公开文章评论计数 -1 |
| `POST` | `/api/admin/comments/{id}/restore` | `ADMIN` | 恢复软删除；若恢复的是 `PASS` 文章评论，计数 +1 |

`DEMO` 只能读取，写操作返回 `403 + 10003`。

## 6. 邮件通知

- 只有审核通过的回复评论触发通知事件。
- 通知在评论事务提交后执行。
- Resend 默认关闭：`myblog.mail.resend.enabled=false`。
- 开启 Resend 但缺少 `apiKey/fromEmail` 时启动失败。
- 发送成功不写 `t_mail_log`；发送失败写 `t_mail_log`，错误消息会截断和脱敏。
- 邮件失败不暴露给前端。
