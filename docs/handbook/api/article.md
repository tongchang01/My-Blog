# 文章接口契约

> 状态：当前有效
> 适用范围：V2 后端 content 模块、前台 blog、后台 admin
> 最后校准：2026-07-18
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/`
> 权威程度：API 契约

## 本文档回答什么问题

本文档记录公开文章列表/详情、后台文章列表/详情、文章写入、回收站、软删除和恢复接口契约。

## 1. 接口清单

| Method | Path | 匿名 | DEMO | ADMIN |
|--------|------|------|------|-------|
| GET | `/api/public/articles` | 允许 | 允许 | 允许 |
| GET | `/api/public/articles/home` | 允许 | 允许 | 允许 |
| GET | `/api/public/archives` | 允许 | 允许 | 允许 |
| GET | `/api/public/articles/{id}` | 允许 | 允许 | 允许 |
| POST | `/api/public/articles/{id}/unlock` | 允许 | 允许 | 允许 |
| GET | `/api/admin/articles` | 401 | 允许 | 允许 |
| GET | `/api/admin/articles/{id}` | 401 | 允许 | 允许 |
| GET | `/api/admin/articles/recycle-bin` | 401 | 允许 | 允许 |
| POST | `/api/admin/articles` | 401 | 403 | 允许 |
| PUT | `/api/admin/articles/{id}` | 401 | 403 | 允许 |
| DELETE | `/api/admin/articles/{id}` | 401 | 403 | 允许 |
| POST | `/api/admin/articles/{id}/restore` | 401 | 403 | 允许 |

## 2. 通用约定

- 后台写接口仅 ADMIN 可用。
- DEMO 可读后台文章，但后台详情正文按文章状态裁剪。
- 文章密码只在写入接口接收明文。
- 查询响应不得返回密码明文或密码 hash。
- PASSWORD 解锁响应只返回一次文章访问令牌；后续请求通过 `X-Article-Access-Token` 发送，不能替代后台登录 token。
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
        "homepageSlot": "NONE",
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

列表与详情都包含 `homepageSlot`，取值为 `NONE`、`PINNED` 或 `FEATURED`。

字段权限：

- ADMIN 可读取所有文章状态的 `body`。
- DEMO 只可读取 `PUBLISHED` 文章的 `body`；`DRAFT`、`PRIVATE`、`PASSWORD`、`SCHEDULED` 文章详情的 `body` 返回 `null`。
- 响应不包含 `password`、`accessPassword` 或密码 hash。

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
  "homepageSlot": "NONE",
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
| `slug` | string 或 `null`，由应用层规范化；文章 slug 不要求唯一 |
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
- 创建和更新会先锁定对应槽位 guard，再检查数量并写入；并发请求不能同时越过上限。
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
- 传入非空新密码、切换离开 `PASSWORD` 状态或软删除文章时，撤销该文章所有已签发的访问令牌；恢复文章不恢复令牌。

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
| `categorySlug` | string | 无 | 分类 slug；前台公开 URL 优先使用该参数 |
| `tagSlug` | string | 无 | 标签 slug；前台公开 URL 优先使用该参数 |
| `keyword` | string | 无 | 当前语言标题/摘要关键字，不搜索正文 |
| `archiveMonth` | string | 无 | 格式 `yyyy-MM` |

成功响应：HTTP 200，`data` 为 `PageResponse<PublicArticlePageItemVO>`。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [
      {
        "id": "123",
        "title": "中文标题",
        "summary": "中文摘要",
        "categoryId": "10",
        "categoryName": "后端",
        "slug": "hello-world",
        "publishAt": "2026-06-16T12:00:00",
        "coverUrl": "https://static.example.com/cover.webp",
        "commentCount": 2,
        "tags": [
          { "id": "20", "name": "Java", "slug": "java" }
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

公开列表只返回 `PUBLISHED` 和 `PASSWORD` 状态文章，不返回正文和密码信息。PASSWORD 文章通过 `locked=true` 表示。筛选支持 `categoryId/tagId` 和 `categorySlug/tagSlug`；博客端分类和标签路由使用 slug。

`keyword` 只匹配三语标题和三语摘要，不搜索正文，不返回高亮片段。搜索结果仍使用公开文章列表响应，不返回正文和密码信息。

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

## 12. 公开归档时间线

```http
GET /api/public/archives?page=1&size=12&lang=zh
```

鉴权：匿名。

Query：

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| `page` | number | 1 | 页码，从 1 开始 |
| `size` | number | 12 | 每页文章数量，最大 100 |
| `lang` | string | `zh` | 支持 `zh`、`ja`、`en`；缺失或非法时按服务端规则回退 `zh` |

成功响应：HTTP 200，`data` 为 `PageResponse<PublicArchivePageVO>`。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [
      {
        "yearMonth": "2026-06",
        "year": 2026,
        "month": 6,
        "articles": [
          {
            "id": "9007199254740993",
            "title": "中文标题",
            "slug": "hello-world",
            "publishedAt": "2026-06-16T12:00:00",
            "summary": "中文摘要"
          }
        ]
      }
    ],
    "total": 1,
    "page": 1,
    "size": 12
  }
}
```

归档分页单位是文章数，`total` 是公开可见文章总数，不是月份分组总数。服务端先按公开文章口径和 `publishAt DESC, id DESC` 分页，再对当前页文章按 `yearMonth` 分组。

归档文章项只返回 `id/title/slug/publishedAt/summary`。不返回正文、分类、标签、封面、评论数、状态、锁定标记、密码字段或存储字段。文章 ID 继续按公开 HTTP 契约输出为 JSON string，供前台 `/:lang/posts/:id/:slug?` ID 主导路由使用。

## 13. 公开文章详情

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
    "id": "123",
    "title": "中文标题",
    "summary": "中文摘要",
    "body": "Markdown 正文",
    "categoryId": "10",
    "categoryName": "后端",
    "slug": "hello-world",
    "publishAt": "2026-06-16T12:00:00",
    "coverUrl": "https://static.example.com/cover.webp",
    "commentCount": 2,
    "tags": [
      { "id": "20", "name": "Java", "slug": "java" }
    ],
    "createdAt": "2026-06-16T10:00:00",
    "updatedAt": "2026-06-16T11:00:00",
    "locked": false
  }
}
```

当前行为：

- `PUBLISHED`：返回正文。
- `PASSWORD`：请求须携带有效的 `X-Article-Access-Token`，否则返回 `403 + 10003`；令牌由下节解锁接口获取。
- `DRAFT`、`PRIVATE`、`SCHEDULED`、不存在或已删除：返回 `404 + 90003`。

## 14. 解锁 PASSWORD 文章

```http
POST /api/public/articles/{id}/unlock
Content-Type: application/json
```

鉴权：匿名，不接受后台 `Authorization` 代替文章密码。

请求体：

```json
{ "password": "article-password" }
```

成功响应：HTTP 200，令牌默认 24 小时有效。

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "token": "base64url-random-token",
    "expiresAt": "2026-07-19T12:00:00"
  }
}
```

同一客户端 IP 与文章组合一分钟最多尝试 5 次。令牌只可通过 `X-Article-Access-Token` 访问对应 PASSWORD 文章的详情和文章评论；过期、撤销、跨文章或缺失令牌均返回 `403 + 10003`。密码改动、状态离开 PASSWORD 或软删除文章会撤销全部令牌。

## 15. 状态规则摘要

| 状态 | 公开列表 | 公开详情 | 后台读 | 后台写 |
|------|----------|----------|--------|--------|
| `DRAFT` | 不可见 | 404 | 可读 | ADMIN |
| `PUBLISHED` | 可见 | 返回正文 | 可读 | ADMIN |
| `PRIVATE` | 不可见 | 404 | 可读 | ADMIN |
| `PASSWORD` | 可见，`locked=true` | 有效文章令牌时返回正文，否则 403 | 可读 | ADMIN |
| `SCHEDULED` | 到期前不可见 | 到期前 404 | 可读 | ADMIN |

## 16. 错误码

| 场景 | HTTP | code |
|------|------|------|
| 参数、字段、状态、时间、slug 格式、标签请求非法 | 400 | `90001` |
| access token 缺失或失效 | 401 | `10002` |
| DEMO 执行写操作、PASSWORD 公开详情未解锁 | 403 | `10003` |
| 文章不存在、不可见或已删除 | 404 | `90003` |
| 首页槽位超限、引用失效、恢复冲突 | 409 | `90004` |
| PASSWORD 解锁尝试过于频繁 | 429 | `90002` |
| 持久化或内部异常 | 500 | `99999` |
