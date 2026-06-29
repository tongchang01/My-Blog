# 分类与标签接口契约

> 状态：当前有效
> 适用范围：V2 后端 content 模块、前台 blog、后台 admin
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/*Category*`、`*Tag*`
> 权威程度：API 契约

## 本文档回答什么问题

本文档记录分类与标签的公开读取、后台读取、新增、编辑、排序和删除接口契约。

## 1. 接口清单

| Method | Path | 匿名 | DEMO | ADMIN |
|--------|------|------|------|-------|
| GET | `/api/public/categories?lang=zh` | 允许 | 允许 | 允许 |
| GET | `/api/public/tags?lang=zh` | 允许 | 允许 | 允许 |
| GET | `/api/admin/categories` | 401 | 允许 | 允许 |
| GET | `/api/admin/categories/{id}` | 401 | 允许 | 允许 |
| POST | `/api/admin/categories` | 401 | 403 | 允许 |
| PUT | `/api/admin/categories/{id}` | 401 | 403 | 允许 |
| PUT | `/api/admin/categories/sort-orders` | 401 | 403 | 允许 |
| DELETE | `/api/admin/categories/{id}` | 401 | 403 | 允许 |
| GET | `/api/admin/tags` | 401 | 允许 | 允许 |
| GET | `/api/admin/tags/{id}` | 401 | 允许 | 允许 |
| POST | `/api/admin/tags` | 401 | 403 | 允许 |
| PUT | `/api/admin/tags/{id}` | 401 | 403 | 允许 |
| DELETE | `/api/admin/tags/{id}` | 401 | 403 | 允许 |

## 2. 公开读取分类

```http
GET /api/public/categories?lang=zh
```

鉴权：匿名。

Query：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `lang` | string | 是 | 只接受 `zh`、`ja`、`en` |

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": [
    {
      "id": 123,
      "name": "后端",
      "slug": "backend"
    }
  ]
}
```

当前代码中的 `PublicCategoryVO.id` 是 number。它与“前端可见 Snowflake ID 统一 string”的规则存在不一致，已登记到 O-010；修正前本文档按当前代码事实记录。

规则：

- 只返回 active 分类。
- `lang=ja/en` 字段为空时 fallback 到中文。
- 公开响应只包含 `id/name/slug`。
- 不返回三语原始字段、排序、文章数或审计字段。
- 分类按 `sortOrder ASC, id ASC` 排序。

## 3. 公开读取标签

```http
GET /api/public/tags?lang=zh
```

鉴权：匿名。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": [
    {
      "id": 123,
      "name": "Java",
      "slug": "java"
    }
  ]
}
```

当前代码中的 `PublicTagVO.id` 是 number。它与“前端可见 Snowflake ID 统一 string”的规则存在不一致，已登记到 O-010。

规则：

- 只返回 active 标签。
- `lang=ja/en` 字段为空时 fallback 到中文。
- 公开响应只包含 `id/name/slug`。
- 不返回三语原始字段、文章数或审计字段。
- 标签按名称和 ID 的服务端规则排序。

## 4. 后台读取分类

```http
GET /api/admin/categories
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": [
    {
      "id": "123",
      "nameZh": "后端",
      "nameJa": null,
      "nameEn": "Backend",
      "slug": "backend",
      "sortOrder": 10,
      "createdAt": "2026-06-14T12:00:00",
      "createdBy": "1001",
      "updatedAt": "2026-06-14T12:00:00",
      "updatedBy": "1001"
    }
  ]
}
```

后台分类列表不分页，只返回 active 记录。`id`、`createdBy`、`updatedBy` 为 string 或 `null`。

详情：

```http
GET /api/admin/categories/{id}
```

不存在或已删除返回 `404 + 90003`。

## 5. 后台读取标签

```http
GET /api/admin/tags
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": [
    {
      "id": "123",
      "nameZh": "Java",
      "nameJa": null,
      "nameEn": "Java",
      "slug": "java",
      "createdAt": "2026-06-14T12:00:00",
      "createdBy": "1001",
      "updatedAt": "2026-06-14T12:00:00",
      "updatedBy": "1001"
    }
  ]
}
```

后台标签列表不分页，只返回 active 记录。`id`、`createdBy`、`updatedBy` 为 string 或 `null`。

详情：

```http
GET /api/admin/tags/{id}
```

不存在或已删除返回 `404 + 90003`。

## 6. 新增和完整编辑分类

```http
POST /api/admin/categories
PUT /api/admin/categories/{id}
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体五个业务字段必须全部出现：

```json
{
  "nameZh": "后端",
  "nameJa": null,
  "nameEn": "Backend",
  "slug": "backend",
  "sortOrder": 10
}
```

字段规则：

| 字段 | 规则 |
|------|------|
| `nameZh` | 必填，最长 64 |
| `nameJa` / `nameEn` | 可显式为 `null`，空白规范化为 `null` |
| `slug` | 必填，见 slug 规则 |
| `sortOrder` | 必填，0 到 1000000 |

成功响应：HTTP 200，`data` 为 `AdminCategoryVO`。

## 7. 新增和完整编辑标签

```http
POST /api/admin/tags
PUT /api/admin/tags/{id}
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体四个业务字段必须全部出现：

```json
{
  "nameZh": "Java",
  "nameJa": null,
  "nameEn": "Java",
  "slug": "java"
}
```

字段规则：

| 字段 | 规则 |
|------|------|
| `nameZh` | 必填，最长 64 |
| `nameJa` / `nameEn` | 可显式为 `null`，空白规范化为 `null` |
| `slug` | 必填，见 slug 规则 |

成功响应：HTTP 200，`data` 为 `AdminTagVO`。

## 8. slug 规则

- trim 后按 `Locale.ROOT` 转小写。
- 长度 1 到 64。
- 只允许小写字母、数字和单个连字符。
- 连字符不得位于首尾。
- 不允许连续连字符。
- 分类和标签分别在自身表内永久唯一。
- 软删除后 slug 仍不可复用。
- 并发唯一键冲突返回 `409 + 90004`，不暴露索引名或 SQL。

## 9. 分类排序

```http
PUT /api/admin/categories/sort-orders
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体：

```json
{
  "items": [
    { "id": 101, "sortOrder": 0 },
    { "id": 102, "sortOrder": 10 }
  ]
}
```

规则：

- 每次 1 到 100 项。
- ID 必须为正数且不得重复。
- 允许相同 `sortOrder`，最终由 ID 保证稳定顺序。
- 全部目标必须为 active。
- 任一目标缺失或更新异常时整批回滚。

成功响应：HTTP 200，`data` 为 `null`。

标签当前不提供排序接口。

## 10. 删除分类或标签

```http
DELETE /api/admin/categories/{id}
DELETE /api/admin/tags/{id}
Authorization: Bearer <access-token>
```

鉴权：仅 ADMIN。

规则：

- active 文章引用分类或标签时，返回 `409 + 90004`。
- 已软删除文章不阻止分类或标签删除。
- 不自动修改文章分类。
- 不自动解绑标签。
- 不级联删除文章。
- 删除为软删除，公开和后台查询均不可见。

成功响应：HTTP 200，`data` 为 `null`。

## 11. 错误码

| 场景 | HTTP | code |
|------|------|------|
| lang、字段、slug、排序请求非法 | 400 | `90001` |
| access token 缺失或失效 | 401 | `10002` |
| DEMO 或非 ADMIN 执行写操作 | 403 | `10003` |
| 分类或标签不存在、已删除 | 404 | `90003` |
| slug 已占用、目标仍被 active 文章引用 | 409 | `90004` |
| 更新行数异常或未知持久化状态 | 500 | `99999` |
