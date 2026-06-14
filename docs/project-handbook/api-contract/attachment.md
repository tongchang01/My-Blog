# 附件接口契约

> 状态：已实施（2026-06-14）

## 1. 接口与权限

| 方法 | 路径 | ADMIN | DEMO | 匿名 |
|---|---|---:|---:|---:|
| POST | `/api/admin/attachments` | 允许 | `403 + 10003` | `401 + 10002` |
| GET | `/api/admin/attachments?page=1&size=20` | 允许 | 允许 | `401 + 10002` |
| GET | `/api/admin/attachments/{id}` | 允许 | 允许 | `401 + 10002` |

本轮不提供附件删除、回收站或物理清理接口。

## 2. 上传

请求使用 `multipart/form-data`，文件字段固定为 `file`。

只接受实际内容可解码为 JPEG、PNG、WebP、GIF 的图片：

- 最大 10 MiB。
- 单边最大 20,000 像素。
- 总像素最大 40,000,000。
- GIF 最多 500 帧。
- 不信任客户端 MIME 和扩展名，不接受 SVG。

成功响应的 `data`：

```json
{
  "id": 123,
  "storageType": "S3",
  "bucket": "myblog-assets",
  "objectKey": "attachments/2026/06/uuid.webp",
  "publicUrl": "https://static.example.com/attachments/2026/06/uuid.webp",
  "contentType": "image/webp",
  "fileSize": 102400,
  "width": 1600,
  "height": 900,
  "originalFilename": "cover.webp",
  "hashSha256": "64位小写十六进制",
  "createdAt": "2026-06-14T12:00:00",
  "createdBy": 1001
}
```

响应不包含删除和更新审计字段。

## 3. 去重与恢复

- 相同 SHA-256 命中 active 记录时，确认物理对象存在后直接返回原记录。
- 命中 deleted 记录时恢复原记录，保留原 ID、对象键和公开地址。
- 数据库记录存在但物理对象缺失时返回 `500 + 99999`，不静默重建。
- 并发上传通过 `hash_sha256` 唯一键收敛；竞争失败方删除自己的随机对象并返回获胜记录。

## 4. 查询

列表只返回 active 记录，按 `createdAt DESC, id DESC` 排序。

- `page` 默认 1，必须大于 0。
- `size` 默认 20，范围 1 到 100。
- 分页字段固定为 `records/total/page/size`。
- 详情不存在或已删除返回 `404 + 90003`。

## 5. 存储配置

新上传目标由 `myblog.storage.type` 选择 `LOCAL` 或 `S3`，不双写。已有记录按自己的
`storageType` 路由。

- LOCAL 使用 `root`、`bucket-alias`、`public-base-url`。
- S3 使用 `region`、`bucket`、`public-base-url`。
- S3 凭证来自 AWS Default Credentials Provider Chain。
- S3 不设置对象 ACL；公开读取由 Bucket Policy 或 CloudFront 管理。
- 历史 OSS 元数据可读取，但当前版本不提供 OSS 上传 adapter。

## 6. 主要错误

| 场景 | HTTP | code |
|---|---:|---|
| 空文件、超限、非法或损坏图片、非法分页 | 400 | `90001` |
| token 缺失或失效 | 401 | `10002` |
| DEMO 上传 | 403 | `10003` |
| 附件不存在 | 404 | `90003` |
| 存储损坏或内部失败 | 500 | `99999` |
