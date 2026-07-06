# 评论与留言接口契约

> 状态：当前有效
> 适用范围：V2 后端 comment 模块、前台 blog、后台 admin
> 最后校准：2026-07-06
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/`
> 权威程度：API 契约

## 本文档回答什么问题

本文档记录公开文章评论、留言板、后台评论查询、审核、隐藏、删除、恢复和后台回复接口契约。

## 1. 通用约定

- 响应体统一使用 `ApiResponse<T>`。
- 分页参数统一为 `page`、`size`，默认 `1 / 20`。
- 公开接口只返回清洗后的 `contentHtml`。
- 公开接口不返回 `contentMd`、邮箱、IP、User-Agent。
- 评论 Markdown 原文由后端保存到 `content_md`，清洗后 HTML 保存到 `content_html`。
- 前台只能渲染 `contentHtml`。
- PASSWORD 文章评论在文章解锁能力落地前不可用。

## 2. 评论状态

| 状态 | 公开展示 | 说明 |
|------|----------|------|
| `PASS` | 是 | 已通过审核 |
| `PENDING` | 否 | 等待后台审核 |
| `HIDDEN` | 否 | 后台隐藏 |

## 3. 查询公开文章评论

```http
GET /api/public/articles/{articleId}/comments?page=1&size=20
```

鉴权：匿名。

成功响应：HTTP 200，`data` 为 `PageResponse<PublicCommentVO>`。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [
      {
        "id": "123",
        "parentId": null,
        "replyToCommentId": null,
        "replyToNickname": null,
        "authorNickname": "TYB",
        "authorSite": "https://example.com",
        "contentHtml": "<p>hello</p>",
        "createdAt": "2026-06-20T12:00:00",
        "replies": []
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

分页 `total` 只统计根评论；当前页根评论的 `replies` 完整返回，回复暂不单独分页。

当前公开评论 ID、父评论 ID 和回复目标 ID 是 JSON string 或 `null`，避免 Snowflake ID 精度损失。

错误：

| 场景 | HTTP | code |
|------|------|------|
| 文章不存在、未发布、私密、定时或已删除 | 404 | `90003` |
| PASSWORD 文章评论未开放 | 403 | `10003` |
| page/size 非法 | 400 | `90001` |

## 4. 提交公开文章评论

```http
POST /api/public/articles/{articleId}/comments
Content-Type: application/json
```

鉴权：匿名。

请求体：

```json
{
  "nickname": "TYB",
  "email": "tyb@example.com",
  "site": "https://example.com",
  "contentMd": "hello",
  "replyToCommentId": null
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `nickname` | string | 是 | 评论昵称 |
| `email` | string | 是 | 邮箱，不在公开响应返回 |
| `site` | string/null | 否 | 只允许 HTTP/HTTPS |
| `contentMd` | string | 是 | Markdown 原文 |
| `replyToCommentId` | string/null | 否 | 回复同一文章下已通过评论；公开 ID 按 string 传递，避免 Snowflake ID 精度损失 |

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "id": "123",
    "auditStatus": "PASS"
  }
}
```

错误：

| 场景 | HTTP | code |
|------|------|------|
| 昵称、邮箱、站点、正文或回复关系非法 | 400 | `90001` |
| PASSWORD 文章评论未开放 | 403 | `10003` |
| 文章或被回复评论不存在 | 404 | `90003` |
| 跨目标回复、重复提交等冲突 | 409 | `90004` |
| 提交过于频繁 | 429 | `90002` |

## 5. 查询留言板评论

```http
GET /api/public/guestbook/comments?page=1&size=20
```

鉴权：匿名。

成功响应与公开文章评论列表相同。留言板目标固定为 `GUESTBOOK`，目标 ID 为 `0`。

## 6. 提交留言板评论

```http
POST /api/public/guestbook/comments
Content-Type: application/json
```

鉴权：匿名。

请求体与文章评论提交相同。`replyToCommentId` 可为空，也可指向同一留言板下已通过评论。

留言和回复不影响文章 `commentCount`。

## 7. 后台评论列表

```http
GET /api/admin/comments?page=1&size=20
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

Query：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `targetType` | string | 无 | `ARTICLE` 或 `GUESTBOOK` |
| `targetId` | number | 无 | 目标 ID |
| `auditStatus` | string | 无 | `PASS`、`PENDING`、`HIDDEN` |
| `keyword` | string | 无 | 匹配昵称和 Markdown 原文 |
| `includeDeleted` | boolean | false | 是否包含软删除评论 |
| `page` | number | 1 | 页码 |
| `size` | number | 20 | 每页数量 |

成功响应：HTTP 200，`data` 为 `PageResponse<AdminCommentPageItemVO>`。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [
      {
        "id": "123",
        "targetType": "ARTICLE",
        "targetId": "10",
        "parentId": null,
        "replyToCommentId": null,
        "replyToNickname": null,
        "authorNickname": "TYB",
        "authorEmail": "tyb@example.com",
        "authorSite": "https://example.com",
        "authorIp": "127.0.0.1",
        "authorUserAgent": "Mozilla/5.0",
        "contentMd": "hello",
        "contentHtml": "<p>hello</p>",
        "auditStatus": "PASS",
        "createdAt": "2026-06-20T12:00:00",
        "deleted": false
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

字段权限：

- ADMIN 可读取 `authorEmail`、`authorIp`、`authorUserAgent`。
- DEMO 固定裁剪 `authorEmail`、`authorIp`、`authorUserAgent`，这些字段返回 `null`；评论内容、目标信息、审核状态和删除状态仍返回。

后台评论 ID、目标 ID、父评论 ID 和回复目标 ID 均为 string 或 `null`。

## 8. 后台审核、隐藏、删除、恢复

| Method | Path | 鉴权 | 行为 |
|--------|------|------|------|
| POST | `/api/admin/comments/{id}/approve` | ADMIN | 审核通过评论 |
| POST | `/api/admin/comments/{id}/hide` | ADMIN | 隐藏评论 |
| DELETE | `/api/admin/comments/{id}` | ADMIN | 软删除评论 |
| POST | `/api/admin/comments/{id}/restore` | ADMIN | 恢复已删除评论 |

成功响应均为 HTTP 200，`data` 为 `null`。

文章评论计数与审核、隐藏、删除、恢复处于同一业务事务边界内维护。

DEMO 调用写接口返回 `403 + 10003`。

## 9. 后台回复评论

```http
POST /api/admin/comments/{id}/reply
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体：

```json
{
  "contentMd": "后台回复内容"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `contentMd` | string | 是 | Markdown 原文，不能为空 |

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "id": "456",
    "auditStatus": "PASS"
  }
}
```

后台回复会创建一条回复评论，并按评论通知策略在事务提交后触发邮件通知。

## 10. 邮件通知

- 只有审核通过的回复评论触发通知事件。
- 通知在评论事务提交后执行。
- Resend 默认关闭。
- 开启 Resend 但缺少必要配置时启动失败。
- 发送成功不写 `t_mail_log`。
- 发送失败写 `t_mail_log`，错误消息截断和脱敏。
- 邮件失败不暴露给前端。

## 11. 错误码

| 场景 | HTTP | code |
|------|------|------|
| 参数、正文、邮箱、站点或回复关系非法 | 400 | `90001` |
| access token 缺失或失效 | 401 | `10002` |
| DEMO 写操作、PASSWORD 评论未开放 | 403 | `10003` |
| 文章、评论或目标不存在 | 404 | `90003` |
| 跨目标回复、重复提交、状态冲突 | 409 | `90004` |
| 评论或留言提交过于频繁 | 429 | `90002` |
| 内部异常 | 500 | `99999` |
