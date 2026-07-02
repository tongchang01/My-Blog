# 文章接口契约

> 状态：当前有效
> 适用范围：V2 后端 content 模块、前台 blog、后台 admin
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/*Article*`
> 权威程度：API 契约

## 本文档回答什么问题

本文档记录公开文章列表/详情、后台文章列表/详情、文章写入、回收站、软删除和恢复接口契约。

## 1. 接口清单

| Method | Path | 匿名 | DEMO | ADMIN |
|--------|------|------|------|-------|
| GET | `/api/public/articles` | 允许 | 允许 | 允许 |
| GET | `/api/public/articles/home` | 允许 | 允许 | 允许 |
| GET | `/api/public/articles/{id}` | 允许 | 允许 | 允许 |
| GET | `/api/admin/articles` | 401 | 允许 | 允许 |
| GET | `/api/admin/articles/{id}` | 401 | 允许 | 允许 |
| GET | `/api/admin/articles/recycle-bin` | 401 | 允许 | 允许 |
| POST | `/api/admin/articles` | 401 | 403 | 允许 |
| PUT | `/api/admin/articles/{id}` | 401 | 403 | 允许 |
| DELETE | `/api/admin/articles/{id}` | 401 | 403 | 允许 |
| POST | `/api/admin/articles/{id}/restore` | 401 | 403 | 允许 |

## 2. 通用约定

- 后台写接口仅 ADMIN 可用。
- DEMO 可读后台文章，但敏感正文裁剪边界见 O-002。
- 文章密码只在写入接口接收明文。
- 查询响应不得返回密码明文或密码 hash。
- 时间字段为 Asia/Tokyo 语义的本地时间字符串，不带 offset。
- 文章软删除后进入回收站，不做物理删除。

## 3. 后台文章列表

```http
GET /api/admin/articles?page=1&size=20&status=PUBLISHED
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

Query：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | number | 1 | 页码，从 1 开始 |
| `size` | number | 20 | 每页数量 |
| `status` | string | 无 | `DRAFT`、`PUBLISHED`、`PRIVATE`、`PASSWORD`、`SCHEDULED` |
| `categoryId` | number | 无 | 分类 ID |
| `tagId` | number | 无 | 标签 ID |
| `titleKeyword` | string | 无 | 按三语标题模糊匹配 |
| `createdFrom` | string | 无 | 创建时间下界，ISO 本地时间 |
| `createdTo` | string | 无 | 创建时间上界，ISO 本地时间 |
| `publishFrom` | string | 无 | 发布时间下界，ISO 本地时间 |
| `publishTo` | string | 无 | 发布时间上界，ISO 本地时间 |

成功响应：HTTP 200，`data` 为 `PageResponse<AdminArticlePageItemVO>`。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [
      {
        "id": "123",
        "titleZh": "中文标题",
        "titleJa": null,
        "titleEn": "English title",
        "summaryZh": "中文摘要",
        "summaryJa": null,
        "summaryEn": "English summary",
        "categoryId": "10",
        "categoryNameZh": "后端",
        "slug": "hello-world",
        "status": "PUBLISHED",
        "publishAt": "2026-06-16T12:00:00",
        "coverAttachmentId": "100",
        "coverUrl": "https://static.example.com/cover.webp",
        "commentCount": 2,
        "tagIds": ["20"],
        "createdAt": "2026-06-16T10:00:00",
        "createdBy": "1",
        "updatedAt": "2026-06-16T11:00:00",
        "updatedBy": "1"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

后台响应中的文章 ID、分类 ID、封面附件 ID、标签 ID、审计用户 ID 均为 string 或 `null`。

## 4. 后台文章详情

```http
GET /api/admin/articles/{id}
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

成功响应：HTTP 200，`data` 为 `AdminArticleDetailVO`。

详情字段在列表字段基础上增加：

- `body`
- `authorId`

响应不包含密码明文或密码 hash。

错误：

| 场景 | HTTP | code |
|------|------|------|
| 文章不存在或已删除 | 404 | `90003` |
| access token 缺失或失效 | 401 | `10002` |

## 5. 文章回收站

```http
GET /api/admin/articles/recycle-bin?page=1&size=20
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

成功响应：HTTP 200，`data` 为 `PageResponse<DeletedArticlePageItemVO>`。

回收站条目字段：

```json
{
  "id": "123",
  "titleZh": "中文标题",
  "titleJa": null,
  "titleEn": "English title",
  "status": "PUBLISHED",
  "categoryId": "10",
  "deletedAt": "2026-06-20T12:00:00",
  "deletedBy": "1"
}
```

## 6. 新增文章

```http
POST /api/admin/articles
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体必须提交所有字段：

```json
{
  "titleZh": "中文标题",
  "titleJa": null,
  "titleEn": "English title",
  "summaryZh": "中文摘要",
  "summaryJa": null,
  "summaryEn": "English summary",
  "body": "Markdown 正文",
  "categoryId": 10,
  "tagIds": [20],
  "slug": "hello-world",
  "status": "PUBLISHED",
  "password": null,
  "publishAt": "2026-06-16T12:00:00",
  "coverAttachmentId": 100
}
```

字段规则：

| 字段 | 规则 |
|------|------|
| `titleZh` | 必填 |
| `titleJa` / `titleEn` | 可为 `null` |
| `summaryZh` / `summaryJa` / `summaryEn` | 按应用层规则校验，可选翻译可为 `null` |
| `body` | 必填，Markdown 原文 |
| `categoryId` | number 或 `null`，由状态规则决定是否必需 |
| `tagIds` | 数组，最多 20 个，ID 必须为正数且不得重复 |
| `slug` | string 或 `null`，由应用层规范化和唯一性校验 |
| `status` | `DRAFT`、`PUBLISHED`、`PRIVATE`、`PASSWORD`、`SCHEDULED` |
| `password` | string 或 `null`；PASSWORD 状态新建时必须非空 |
| `publishAt` | ISO 本地时间或 `null` |
| `coverAttachmentId` | number 或 `null` |
| `homepageSlot` | `NONE`、`PINNED`、`FEATURED`；仅 `PUBLISHED` 可设置为 `PINNED` / `FEATURED` |

注意：当前写请求中的关联 ID 在 OpenAPI 模型中为 number；后台响应中的 ID 为 string。

首页展示槽位规则：

- `PINNED` 最多 1 篇。
- `FEATURED` 最多 2 篇。
- 只有 `PUBLISHED` 文章可进入首页槽位。
- 保存超限时返回 `409 + 90004`，不自动替换旧槽位文章。
- 文章改为非 `PUBLISHED` 状态时，后端自动清空首页槽位为 `NONE`。

## 7. 完整编辑文章

```http
PUT /api/admin/articles/{id}
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体与新增文章一致，所有字段都必须出现。

PASSWORD 密码规则：

- 更新为 `PASSWORD` 且 `password` 非空：重新生成密码 hash。
- 更新为 `PASSWORD` 且 `password` 为 `null`：保留原密码 hash。
- 更新为非 `PASSWORD` 状态：清空密码 hash。

成功响应：HTTP 200，`data` 为更新后的 `AdminArticleDetailVO`。

## 8. 删除文章

```http
DELETE /api/admin/articles/{id}
Authorization: Bearer <access-token>
```

鉴权：仅 ADMIN。

成功响应：HTTP 200，`data` 为 `null`。

删除为软删除；删除后不出现在公开列表、公开详情和后台普通列表中。

## 9. 恢复文章

```http
POST /api/admin/articles/{id}/restore
Authorization: Bearer <access-token>
```

鉴权：仅 ADMIN。

成功响应：HTTP 200，`data` 为恢复后的 `AdminArticleDetailVO`。

若分类、标签或封面附件引用已不可用，返回 `409 + 90004`。

## 10. 公开文章列表

```http
GET /api/public/articles?page=1&size=20&lang=zh
```

鉴权：匿名。

Query：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | number | 1 | 页码，从 1 开始 |
| `size` | number | 20 | 每页数量，最大 100 |
| `lang` | string | `zh` | 支持 `zh`、`ja`、`en`；缺失或非法时按服务端规则回退 `zh` |
| `categoryId` | number | 无 | 分类 ID |
| `tagId` | number | 无 | 标签 ID |
| `keyword` | string | 无 | 当前语言标题/摘要关键字 |
| `archiveMonth` | string | 无 | 格式 `yyyy-MM` |

成功响应：HTTP 200，`data` 为 `PageResponse<PublicArticlePageItemVO>`。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 123,
        "title": "中文标题",
        "summary": "中文摘要",
        "categoryId": 10,
        "categoryName": "后端",
        "slug": "hello-world",
        "publishAt": "2026-06-16T12:00:00",
        "coverUrl": "https://static.example.com/cover.webp",
        "commentCount": 2,
        "tags": [
          { "id": 20, "name": "Java", "slug": "java" }
        ],
        "createdAt": "2026-06-16T10:00:00",
        "locked": false
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

公开列表只返回 `PUBLISHED` 和 `PASSWORD` 状态文章，不返回正文和密码信息。PASSWORD 文章通过 `locked=true` 表示。

当前公开文章响应中的 `id`、`categoryId` 和 `tags[].id` 是 number；这与前端可见 ID string 规则不一致，统一登记到 O-010。

## 11. 公开首页文章

```http
GET /api/public/articles/home?lang=zh&size=10
```

鉴权：匿名。

Query：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `lang` | string | `zh` | 支持 `zh`、`ja`、`en`；缺失或非法时按服务端规则回退 `zh` |
| `size` | number | 10 | 普通文章数量，最大 50 |

成功响应：HTTP 200。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "pinnedArticle": null,
    "featuredArticles": [],
    "articles": []
  }
}
```

字段语义：

- `pinnedArticle`：`PublicArticlePageItemVO` 或 `null`，最多 1 篇。
- `featuredArticles`：`PublicArticlePageItemVO[]`，最多 2 篇。
- `articles`：`PublicArticlePageItemVO[]`，排除已进入 `PINNED` / `FEATURED` 槽位的文章。

公开首页只返回 `PUBLISHED` 文章。PASSWORD 文章继续只进入普通公开列表，不进入首页置顶或推荐槽位。

## 12. 公开文章详情

```http
GET /api/public/articles/{id}?lang=zh
```

鉴权：匿名。

成功响应：HTTP 200，`data` 为 `PublicArticleDetailVO`。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "id": 123,
    "title": "中文标题",
    "summary": "中文摘要",
    "body": "Markdown 正文",
    "categoryId": 10,
    "categoryName": "后端",
    "slug": "hello-world",
    "publishAt": "2026-06-16T12:00:00",
    "coverUrl": "https://static.example.com/cover.webp",
    "commentCount": 2,
    "tags": [
      { "id": 20, "name": "Java", "slug": "java" }
    ],
    "createdAt": "2026-06-16T10:00:00",
    "updatedAt": "2026-06-16T11:00:00",
    "locked": false
  }
}
```

当前行为：

- `PUBLISHED`：返回正文。
- `PASSWORD`：返回 `403 + 10003`，完整解锁流程见 O-001。
- `DRAFT`、`PRIVATE`、`SCHEDULED`、不存在或已删除：返回 `404 + 90003`。

## 13. 状态规则摘要

| 状态 | 公开列表 | 公开详情 | 后台读 | 后台写 |
|------|----------|----------|--------|--------|
| `DRAFT` | 不可见 | 404 | 可读 | ADMIN |
| `PUBLISHED` | 可见 | 返回正文 | 可读 | ADMIN |
| `PRIVATE` | 不可见 | 404 | 可读 | ADMIN |
| `PASSWORD` | 可见，`locked=true` | 403 | 可读 | ADMIN |
| `SCHEDULED` | 到期前不可见 | 到期前 404 | 可读 | ADMIN |

## 14. 错误码

| 场景 | HTTP | code |
|------|------|------|
| 参数、字段、状态、时间、slug、标签请求非法 | 400 | `90001` |
| access token 缺失或失效 | 401 | `10002` |
| DEMO 执行写操作、PASSWORD 公开详情未解锁 | 403 | `10003` |
| 文章不存在、不可见或已删除 | 404 | `90003` |
| slug 冲突、引用失效、恢复冲突 | 409 | `90004` |
| 持久化或内部异常 | 500 | `99999` |
