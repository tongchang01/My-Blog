# 分类与标签接口契约

## 1. 权限矩阵

| Method | Path | 匿名 | DEMO | ADMIN |
|---|---|---:|---:|---:|
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

## 2. 公开读取

- `lang` 必须为 `zh`、`ja`、`en`，缺少或不支持时返回 `400 + 90001`。
- `zh` 返回中文；日文或英文为空时 fallback 到中文。
- 公开响应只包含 `id/name/slug`，不返回原始三语字段、排序、文章数或审计字段。
- 分类按 `sortOrder ASC, id ASC` 排序。
- 标签按 `nameZh ASC, id ASC` 排序。

## 3. 后台读取

- ADMIN、DEMO 均可读取 active 列表和详情。
- 分类、标签数据量较小，列表不分页。
- 响应包含全部三语字段和 `createdAt/createdBy/updatedAt/updatedBy`。
- 不返回 `deleted/deletedAt/deletedBy`。
- 本切片不返回文章数；文章数随文章查询切片统一实现。

## 4. 新增与完整编辑

分类 POST、PUT 要求五个字段全部出现：

```json
{
  "nameZh": "后端",
  "nameJa": null,
  "nameEn": "Backend",
  "slug": "backend",
  "sortOrder": 10
}
```

标签 POST、PUT 要求四个字段全部出现：

```json
{
  "nameZh": "Java",
  "nameJa": null,
  "nameEn": "Java",
  "slug": "java"
}
```

- `nameZh` 必填且最长 64；`nameJa/nameEn` 可显式为 `null`。
- 空白可选名称规范化为 `null`。
- 分类 `sortOrder` 范围为 0..1,000,000。
- 未知字段、字段缺失和非法值返回 `400 + 90001`。
- PUT 是完整覆盖，不提供通用 PATCH。

## 5. slug

- trim 后使用 `Locale.ROOT` 转小写。
- 长度为 1..64。
- 只允许小写字母、数字和单个连字符。
- 连字符不得位于首尾，不允许连续连字符。
- 分类和标签分别在自身表内永久唯一。
- 软删除后 slug 仍不可复用。
- 并发唯一键冲突返回 `409 + 90004`，不暴露索引名或 SQL。

## 6. 分类排序

`PUT /api/admin/categories/sort-orders`

```json
{
  "items": [
    {"id": 101, "sortOrder": 0},
    {"id": 102, "sortOrder": 10}
  ]
}
```

- 每次 1..100 项，ID 必须为正数且不得重复。
- 允许相同 `sortOrder`，最终由 ID 保证稳定顺序。
- 全部目标必须为 active；任一缺失或更新异常时整批回滚。
- 标签不提供排序接口。

## 7. 引用保护与软删除

- active 文章引用分类或标签时，DELETE 返回 `409 + 90004`。
- 已软删除文章不阻止分类或标签删除。
- 不自动修改文章分类，不解绑标签，不级联删除。
- DELETE 显式写入 `deleted/deletedAt/deletedBy/updatedAt/updatedBy`。
- 删除后公开和后台查询均不可见。

## 8. 错误语义

| HTTP | code | 场景 |
|---:|---|---|
| 400 | `90001` | lang、字段、slug、排序请求非法 |
| 401 | `10002` | token 缺失或失效 |
| 403 | `10003` | DEMO 或非 ADMIN 执行写操作 |
| 404 | `90003` | 分类或标签不存在、已删除 |
| 409 | `90004` | slug 已占用、目标仍被 active 文章引用 |
| 500 | `99999` | 更新行数异常或未知持久化状态 |
