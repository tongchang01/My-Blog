# 友链接口契约

## 1. 权限矩阵

| Method | Path | 匿名 | DEMO | ADMIN |
|---|---|---:|---:|---:|
| GET | `/api/public/friend-links` | 允许 | 允许 | 允许 |
| GET | `/api/admin/friend-links?page=1&size=20` | 401 | 允许 | 允许 |
| GET | `/api/admin/friend-links/{id}` | 401 | 允许 | 允许 |
| POST | `/api/admin/friend-links` | 401 | 403 | 允许 |
| PUT | `/api/admin/friend-links/{id}` | 401 | 403 | 允许 |
| PATCH | `/api/admin/friend-links/{id}/status` | 401 | 403 | 允许 |
| PUT | `/api/admin/friend-links/sort-orders` | 401 | 403 | 允许 |
| DELETE | `/api/admin/friend-links/{id}` | 401 | 403 | 允许 |

## 2. 公开读取

`GET /api/public/friend-links`

- 只返回 `deleted=0` 且状态为 `VISIBLE` 的记录。
- 按 `sortOrder ASC, id ASC` 排序，不分页。
- 字段为 `id/name/url/avatarUrl/description`。
- 不返回状态、排序值和任何审计字段。

## 3. 后台读取

- 列表和详情同时包含 `VISIBLE`、`HIDDEN`，不包含软删除记录。
- 分页默认 `page=1&size=20`，最大 100。
- 分页结构固定为 `records/total/page/size`。
- 响应包含完整业务字段及 `createdAt/createdBy/updatedAt/updatedBy`。
- 不返回 `deleted/deletedAt/deletedBy`。

## 4. 新增与完整编辑

POST 和 PUT 请求均要求以下六个字段全部出现：

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

- `name` 最大 64。
- `url` 必填，`avatarUrl` 可空，均只接受绝对 HTTP/HTTPS 地址。
- URL 不要求唯一，允许多个友链指向同一站点。
- `description` 最大 255。
- `sortOrder` 范围为 0..1,000,000。
- `status` 只接受 `VISIBLE`、`HIDDEN`。
- 未知字段、字段缺失和非法值返回 `400 + 90001`。

## 5. 状态与排序

状态请求：

```json
{"status":"HIDDEN"}
```

批量排序请求：

```json
{
  "items": [
    {"id": 101, "sortOrder": 10},
    {"id": 102, "sortOrder": 20}
  ]
}
```

- 批量排序允许部分列表，每次 1..100 项。
- ID 不得重复，全部目标必须为 active。
- 任一目标缺失或更新异常时整批回滚。
- 更新、状态、排序和删除都先锁定 active 行。

## 6. 软删除

DELETE 执行显式软删除，同时写入：

- `deleted=1`
- `deletedAt`
- `deletedBy`
- `updatedAt`
- `updatedBy`

删除后公开和后台查询均不可见。不提供恢复、回收站和批量删除。

在线友链申请、审核和 URL 可用性探测不属于本接口。未来如需在线申请，使用独立
`t_friend_link_application`，不扩展 `t_friend_link`。
