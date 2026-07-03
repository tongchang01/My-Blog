# API 契约

> 状态：当前有效
> 适用范围：MyBlog V2 后端、前台 blog、后台 admin
> 最后校准：2026-06-29
> 权威程度：接口契约入口

## 本文档回答什么问题

本目录记录 MyBlog V2 后端暴露给前台和后台的 HTTP API 契约。前后端字段、路径、错误码、分页和权限边界都应以本目录为准。

## 通用约定

- 响应统一使用 `code/msg/data`。
- 成功码固定为 `00000`。
- 前端业务逻辑判断 `code`，不依赖中文 `msg`。
- 失败响应使用对应 HTTP 状态码，不用 HTTP 200 表达失败。
- 分页响应当前使用 `records/total/page/size`，不包含 `pages`。
- 暴露给前端的 Snowflake ID 使用 JSON string。
- 时间字段使用 ISO-8601 本地时间字符串，语义为 Asia/Tokyo。
- API 变更必须同步更新对应文档和自动化测试。

## 契约清单

| 文档 | 主题 | 当前状态 |
|------|------|----------|
| `auth.md` | 登录、refresh、logout、当前用户、资料更新、改密 | 已迁移校准 |
| `article.md` | 后台文章、公开文章、回收站、定时发布 | 已迁移校准；公开 ID 类型见 O-010 |
| `attachment.md` | 图片上传、附件列表、回收站、删除和恢复 | 已迁移校准 |
| `category-tag.md` | 分类标签公开读取和后台管理 | 已迁移校准；公开 ID 类型见 O-010 |
| `comment.md` | 公开评论、留言板、后台审核和后台回复 | 已迁移校准；公开 ID 类型见 O-011 |
| `friend-link.md` | 公开友链和后台友链管理 | 已迁移校准 |
| `site-config.md` | 公开站点配置和后台配置管理 | 已迁移校准 |
| `stats.md` | 公开打点和后台统计看板 | 已迁移校准；TOP 文章 ID 类型已按 O-012 收口 |

## 路径前缀

| 前缀 | 用途 | 鉴权 |
|------|------|------|
| `/api/public/**` | 前台公开接口 | 匿名 |
| `/api/auth/**` | 认证与当前用户 | 按接口单独声明 |
| `/api/admin/**` | 后台管理接口 | ADMIN/DEMO 读，ADMIN 写 |
| `/actuator/health` | 健康检查 | 匿名 |

## 写作规则

每个接口至少写清：

- method 和 path。
- 鉴权要求。
- 请求 query/path/body。
- 成功响应 `data`。
- 常见错误码。
- 权限差异。
- 字段类型和 nullable 语义。

## 相关规则

- API 响应规则：`../rules/api-response.md`
- 异常处理规则：`../rules/error-handling.md`
- 安全基线：`../rules/security-baseline.md`
