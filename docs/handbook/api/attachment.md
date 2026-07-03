# 附件接口契约

> 状态：当前有效
> 适用范围：V2 后端 system 模块、后台 admin 附件管理
> 最后校准：2026-07-03
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/AdminAttachmentController.java`
> 权威程度：API 契约

## 本文档回答什么问题

本文档记录后台图片附件上传、列表、详情、回收站、软删除和恢复接口的契约。

## 1. 接口清单

| Method | Path | ADMIN | DEMO | 匿名 |
|--------|------|-------|------|------|
| GET | `/api/admin/attachments?page=1&size=20` | 允许 | 允许 | 401 |
| GET | `/api/admin/attachments/deleted?page=1&size=20` | 允许 | 允许 | 401 |
| GET | `/api/admin/attachments/{id}` | 允许 | 允许 | 401 |
| POST | `/api/admin/attachments` | 允许 | 403 | 401 |
| DELETE | `/api/admin/attachments/{id}` | 允许 | 403 | 401 |
| POST | `/api/admin/attachments/{id}/restore` | 允许 | 403 | 401 |

当前不提供永久删除、批量删除或物理清理接口。

## 2. 附件响应结构

`AttachmentVO`：

```json
{
  "id": "123",
  "publicUrl": "https://static.example.com/attachments/2026/06/uuid.webp",
  "contentType": "image/webp",
  "fileSize": 102400,
  "width": 1600,
  "height": 900,
  "originalFilename": "cover.webp",
  "createdAt": "2026-06-14T12:00:00",
  "createdBy": "1001"
}
```

字段规则：

- `id` 为 string，避免 Snowflake ID 精度损失。
- `createdBy` 为 string 或 `null`。
- ADMIN / DEMO 使用同一响应结构，只返回公开管理元数据。
- 不返回 `storageType`、`bucket`、`objectKey`、`hashSha256`、本地磁盘路径、删除审计和更新审计字段。

## 3. 分页查询 active 附件

```http
GET /api/admin/attachments?page=1&size=20
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

Query：

| 参数 | 默认 | 规则 |
|------|------|------|
| `page` | 1 | 大于 0 |
| `size` | 20 | 1 到 100 |

成功响应：HTTP 200，`data` 为 `PageResponse<AttachmentVO>`。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [],
    "total": 0,
    "page": 1,
    "size": 20
  }
}
```

列表只返回 active 附件。

## 4. 分页查询已删除附件

```http
GET /api/admin/attachments/deleted?page=1&size=20
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

成功响应与 active 列表结构一致，但 `records` 为已软删除附件。

## 5. 查询附件详情

```http
GET /api/admin/attachments/{id}
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

成功响应：HTTP 200，`data` 为 `AttachmentVO`。

错误：

| 场景 | HTTP | code |
|------|------|------|
| 附件不存在或不可见 | 404 | `90003` |
| access token 缺失或失效 | 401 | `10002` |

## 6. 上传图片附件

```http
POST /api/admin/attachments
Authorization: Bearer <access-token>
Content-Type: multipart/form-data
```

鉴权：仅 ADMIN。

请求：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `file` | file | 是 | 图片文件 |

图片限制：

- 最大 10 MiB。
- 只接受实际可解码的 JPEG、PNG、WebP、GIF。
- 不信任客户端 MIME 和扩展名。
- 不接受 SVG。
- 单边最大 20000 像素。
- 总像素最大 40000000。
- GIF 最多 500 帧。

成功响应：HTTP 200，`data` 为 `AttachmentVO`。

去重与恢复：

- 相同 SHA-256 命中 active 记录时，确认物理对象存在后直接返回原记录。
- 命中 deleted 记录时恢复原记录，保留原 ID、对象键和公开地址。
- 数据库记录存在但物理对象缺失时返回 `500 + 99999`。
- 并发上传通过唯一键收敛；竞争失败方清理自己写入的随机对象并返回获胜记录。

错误：

| 场景 | HTTP | code |
|------|------|------|
| 空文件、超限、损坏图片、非法格式、缺少 file 字段 | 400 | `90001` |
| access token 缺失或失效 | 401 | `10002` |
| DEMO 上传 | 403 | `10003` |
| 存储损坏或内部失败 | 500 | `99999` |

## 7. 软删除附件

```http
DELETE /api/admin/attachments/{id}
Authorization: Bearer <access-token>
```

鉴权：仅 ADMIN。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": null
}
```

删除后 active 列表和详情不再返回该附件。当前不执行物理对象删除。

## 8. 恢复已删除附件

```http
POST /api/admin/attachments/{id}/restore
Authorization: Bearer <access-token>
```

鉴权：仅 ADMIN。

成功响应：HTTP 200，`data` 为 `null`。

恢复后附件重新进入 active 列表。

## 9. 存储配置

上传目标由 `myblog.storage.type` 决定：

- `LOCAL`：使用本地 root、bucket alias 和 public base URL。
- `S3`：使用 region、bucket 和 public base URL。

规则：

- 新上传不双写。
- 已有记录按自己的存储元数据路由。
- S3 使用 AWS Default Credentials Provider Chain。
- S3 不设置对象 ACL；公开访问由 Bucket Policy 或 CloudFront 管理。
- 公开 URL 不应暴露本地文件系统路径。
