# 文章接口契约

> 状态：已落地。对应后端纵向切片覆盖后台文章管理、公开文章查询、回收站、软删除恢复与定时发布。

## 通用约定

- 后台接口需要 `Authorization: Bearer <accessToken>`。
- 后台只允许 `ADMIN` 写入；`DEMO` 只读。
- 公开接口不需要登录。
- 文章密码只在写入接口接收明文，后端仅持久化 BCrypt hash，任何查询响应不得返回密码或 hash。
- `publishAt`、`createdAt`、`updatedAt`、`deletedAt` 均使用 JST 语义的 ISO-8601 本地时间字符串。
- 文章软删除后进入回收站，不做物理删除。

## 后台文章列表

`GET /api/admin/articles`

查询参数：

| 字段 | 类型 | 说明 |
|------|------|------|
| `page` | number | 页码，从 1 开始 |
| `size` | number | 每页数量 |
| `status` | number | 可选，1 草稿、2 已发布、3 密码、4 定时 |
| `keyword` | string | 可选，按标题或 slug 模糊匹配 |
| `categoryId` | string | 可选，分类 ID |
| `tagId` | string | 可选，标签 ID |

响应 `data`：

```json
{
  "records": [
    {
      "id": "ARTICLE_ID",
      "slug": "hello-world",
      "status": 2,
      "titleZh": "中文标题",
      "titleEn": "English title",
      "titleJa": "日本語タイトル",
      "categoryId": "CATEGORY_ID",
      "categoryNameZh": "分类",
      "coverAttachmentId": "ATTACHMENT_ID",
      "publishAt": "2026-06-16T12:00:00",
      "createdAt": "2026-06-16T10:00:00",
      "updatedAt": "2026-06-16T11:00:00"
    }
  ],
  "total": 1,
  "page": 1,
  "size": 20
}
```

## 后台文章详情

`GET /api/admin/articles/{id}`

响应 `data` 包含文章完整三语标题、摘要、正文、分类、标签、封面附件、状态和发布时间字段，但不包含 `passwordHash`。

角色字段规则：

| 状态 | ADMIN `body` | DEMO `body` |
|---|---|---|
| PUBLISHED | 完整正文 | 完整正文 |
| DRAFT / PRIVATE / PASSWORD / SCHEDULED | 完整正文 | `null` |

`body` 字段始终存在；DEMO 无权读取时返回 JSON `null`，不省略字段。任何角色均不得获得密码或 `passwordHash`。

## 新建文章

`POST /api/admin/articles`

请求体必填字段：

```json
{
  "slug": "hello-world",
  "status": 2,
  "titleZh": "中文标题",
  "titleEn": "English title",
  "titleJa": "日本語タイトル",
  "summaryZh": "中文摘要",
  "summaryEn": "English summary",
  "summaryJa": "日本語概要",
  "contentZh": "中文正文",
  "contentEn": "English content",
  "contentJa": "日本語本文",
  "categoryId": "CATEGORY_ID",
  "tagIds": ["TAG_ID"],
  "coverAttachmentId": "ATTACHMENT_ID",
  "publishAt": "2026-06-16T12:00:00",
  "accessPassword": "optional-password"
}
```

状态规则：

- `PUBLISHED` 需要 `publishAt <= now`。
- `SCHEDULED` 需要 `publishAt > now`。
- `PASSWORD` 需要非空 `accessPassword`。
- `DRAFT` 可以不设置 `publishAt`。

## 更新文章

`PUT /api/admin/articles/{id}`

请求体与新建文章一致。更新为 `PASSWORD` 时：

- 请求包含非空 `accessPassword`：重新生成密码 hash。
- 请求包含 `accessPassword: null`：保留原密码 hash。

更新为非 `PASSWORD` 状态时清空密码 hash。

## 删除、回收站与恢复

`DELETE /api/admin/articles/{id}`

软删除文章。已删除文章不再出现在公开列表和后台普通列表中。

`GET /api/admin/articles/recycle-bin`

分页查询回收站文章，响应项包含 `deletedAt` 与 `deletedBy`，用于后台审计展示。

`POST /api/admin/articles/{id}/restore`

恢复软删除文章。若分类、标签或封面附件已不可用，返回 `409`，前端应提示先修复引用。

## 公开文章列表

`GET /api/public/articles`

查询参数：

| 字段 | 类型 | 说明 |
|------|------|------|
| `page` | number | 页码，从 1 开始 |
| `size` | number | 每页数量 |
| `lang` | string | `zh`、`en`、`ja`，默认 `zh` |
| `keyword` | string | 可选，按当前语言标题或摘要模糊匹配 |
| `categoryId` | string | 可选，分类 ID |
| `tagId` | string | 可选，标签 ID |
| `archiveMonth` | string | 可选，格式 `yyyy-MM` |

只返回 `PUBLISHED` 与 `PASSWORD` 状态文章，列表不返回正文，不返回密码信息。

## 公开文章详情

`GET /api/public/articles/{id}?lang=zh`

- `PUBLISHED`：返回当前语言标题、摘要、正文、分类、标签、封面和发布时间。
- `PASSWORD`：首版不开放解锁接口，详情返回 `403 + 10003`；完整解锁链路位于上线后增量。
- `DRAFT`、`SCHEDULED`、已删除或不存在：返回 `404`。

## OpenAPI 守护

测试必须保证 OpenAPI 文档暴露的是契约 DTO，而不是内部 Entity、Mapper、Command 内部标记或密码 hash 字段。
