# 友链接口契约

> 状态：当前有效
> 适用范围：V2 后端 system 模块、前台 blog、后台 admin
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/*FriendLink*`
> 权威程度：API 契约

## 本文档回答什么问题

本文档记录公开友链列表和后台友链管理接口的权限、请求、响应和错误码。

## 1. 接口清单

| Method | Path | 匿名 | DEMO | ADMIN |
|--------|------|------|------|-------|
| GET | `/api/public/friend-links` | 允许 | 允许 | 允许 |
| GET | `/api/admin/friend-links?page=1&size=20` | 401 | 允许 | 允许 |
| GET | `/api/admin/friend-links/{id}` | 401 | 允许 | 允许 |
| POST | `/api/admin/friend-links` | 401 | 403 | 允许 |
| PUT | `/api/admin/friend-links/{id}` | 401 | 403 | 允许 |
| PATCH | `/api/admin/friend-links/{id}/status` | 401 | 403 | 允许 |
| PUT | `/api/admin/friend-links/sort-orders` | 401 | 403 | 允许 |
| DELETE | `/api/admin/friend-links/{id}` | 401 | 403 | 允许 |

## 2. 查询公开友链

```http
GET /api/public/friend-links
```

鉴权：匿名。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": [
    {
      "id": "123",
      "name": "Example",
      "url": "https://example.com",
      "avatarUrl": null,
      "description": null
    }
  ]
}
```

规则：

- 只返回 active 且状态为 `VISIBLE` 的友链。
- 按 `sortOrder ASC, id ASC` 排序。
- 不分页。
- 不返回状态、排序值或审计字段。
- `id` 为 string。

## 3. 分页查询后台友链

```http
GET /api/admin/friend-links?page=1&size=20
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

Query：

| 参数 | 默认 | 规则 |
|------|------|------|
| `page` | 1 | 大于 0 |
| `size` | 20 | 1 到 100 |

成功响应：HTTP 200，`data` 为 `PageResponse<AdminFriendLinkVO>`。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [
      {
        "id": "123",
        "name": "Example",
        "url": "https://example.com",
        "avatarUrl": null,
        "description": null,
        "sortOrder": 10,
        "status": "VISIBLE",
        "createdAt": "2026-06-14T12:00:00",
        "createdBy": "1001",
        "updatedAt": "2026-06-14T12:00:00",
        "updatedBy": "1001"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

后台列表包含 `VISIBLE` 和 `HIDDEN`，不包含软删除记录。

## 4. 查询后台友链详情

```http
GET /api/admin/friend-links/{id}
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

成功响应：HTTP 200，`data` 为 `AdminFriendLinkVO`。

不存在或已删除返回 `404 + 90003`。

## 5. 新增友链

```http
POST /api/admin/friend-links
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体：

```json
{
  "name": "Example",
  "url": "https://example.com",
  "avatarUrl": null,
  "description": null,
  "sortOrder": 10,
  "status": "VISIBLE"
}
```

字段规则：

| 字段 | 规则 |
|------|------|
| `name` | 必填，最长 64 |
| `url` | 必填，只接受 HTTP/HTTPS 绝对 URL |
| `avatarUrl` | 可空，只接受 HTTP/HTTPS 绝对 URL |
| `description` | 可空，最长 255 |
| `sortOrder` | 0 到 1000000 |
| `status` | `VISIBLE` 或 `HIDDEN` |

URL 不要求唯一，允许多个友链指向同一站点。

成功响应：HTTP 200，`data` 为新增后的 `AdminFriendLinkVO`。

## 6. 完整编辑友链

```http
PUT /api/admin/friend-links/{id}
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体与新增一致，六个业务字段必须全部出现。

成功响应：HTTP 200，`data` 为更新后的 `AdminFriendLinkVO`。

## 7. 修改友链状态

```http
PATCH /api/admin/friend-links/{id}/status
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体：

```json
{
  "status": "HIDDEN"
}
```

`status` 只接受 `VISIBLE` 或 `HIDDEN`。

成功响应：HTTP 200，`data` 为更新后的 `AdminFriendLinkVO`。

## 8. 批量调整排序

```http
PUT /api/admin/friend-links/sort-orders
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体：

```json
{
  "items": [
    { "id": 101, "sortOrder": 10 },
    { "id": 102, "sortOrder": 20 }
  ]
}
```

规则：

- `items` 数量为 1 到 100。
- ID 不得重复。
- 全部目标必须为 active。
- 任一目标缺失或更新异常时整批回滚。

成功响应：HTTP 200，`data` 为更新后的 `AdminFriendLinkVO[]`。

注意：当前排序请求中的 `id` 是 JSON number；后台响应中的 `id` 是 string。

## 9. 删除友链

```http
DELETE /api/admin/friend-links/{id}
Authorization: Bearer <access-token>
```

鉴权：仅 ADMIN。

成功响应：HTTP 200，`data` 为 `null`。

删除为软删除：

- 公开列表不可见。
- 后台列表和详情不可见。
- 写入删除和更新审计字段。

当前不提供恢复、回收站、批量删除、在线友链申请或 URL 可用性探测接口。

## 10. 错误码

| 场景 | HTTP | code |
|------|------|------|
| access token 缺失或失效 | 401 | `10002` |
| DEMO 执行写操作 | 403 | `10003` |
| 字段缺失、未知字段、非法 URL、非法状态、非法排序 | 400 | `90001` |
| 友链不存在或已删除 | 404 | `90003` |
| 并发状态冲突或唯一冲突 | 409 | `90004` |
| 持久化或内部异常 | 500 | `99999` |
