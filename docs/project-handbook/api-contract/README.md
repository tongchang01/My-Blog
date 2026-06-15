# api-contract/ — 三端接口契约

> 本目录回答："后端给前端暴露什么接口？字段名、类型、错误码、分页约定是什么？"
> 性质：**三端共识层**，是 backend / frontend-user / frontend-admin 的共同输入。
> 当前状态：identity、system 与 content 分类标签契约已落地，其余接口随业务模块实现逐步补齐。

## 与其他文档的关系

```
product/use-cases.md        →  我要做什么用例
       ↓
api-contract/endpoints.md   ←  本目录：把用例落到 HTTP 接口
       ↓                       ↓
arch/schema-design.md       frontend-*/pages.md
（后端按契约实现）          （前端按契约对接）
```

api-contract 是后端与前端的**单一事实源**。前后端任何一方想改字段，都先改本目录的文档，再改代码。

## 计划包含的文件

| 文件 | 内容 | 状态 |
|------|------|------|
| `conventions.md` | 通用约定：路径前缀、分页参数、时间格式、命名风格、ApiResponse 结构 | ⏳ |
| `error-codes.md` | 全部 ApiErrorCode 枚举 + HTTP 状态映射 + 触发条件 | ⏳ |
| `auth.md` | 登录 / 登出 / 刷新 / 当前用户接口契约 | ✅ 已落地 |
| `site-config.md` | 公开与后台站点配置查询、ADMIN 全量更新 | ✅ 已落地 |
| `attachment.md` | 图片上传、去重恢复、LOCAL/S3、后台列表与详情 | ✅ 已落地 |
| `friend-link.md` | 公开友链、后台 CRUD、状态、排序和软删除 | ✅ 已落地 |
| `category-tag.md` | 分类标签三语读取、后台管理、排序和引用保护删除 | ✅ 已落地 |
| `endpoints-public.md` | 前台公开接口（文章列表、详情、评论提交等） | ⏳ |
| `endpoints-admin.md` | 后台管理接口（按业务模块分组） | ⏳ |
| `dto-glossary.md` | 公共 DTO 字段含义（ArticleVO / CommentVO 等） | ⏳ |

## 写作约定

- 每个接口写清：method、path、auth 要求、请求体 schema、响应体 schema、可能错误码、典型示例
- 字段名用 camelCase（JSON 约定）；后端 snake_case 字段在序列化时映射
- 时间字段统一 ISO 8601 本地时间字符串，语义固定为 JST（如 `2026-06-01T12:34:56`）
- 分页统一：`?page=1&size=20`，响应含 `records/total/page/size`
- 错误码统一走 `error-codes.md`，禁止接口里临时新造
