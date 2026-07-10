# 博客端

> 状态：当前有效
> 适用范围：V2 公开读者端
> 最后校准：2026-07-10
> 对应代码：`frontend/apps/blog/`
> 权威程度：前端实现摘要

博客端基于 Vue 3、Pinia、Vue Router、vue-i18n、Axios、markdown-it、Vite 和 Vitest。界面源自 Aurora，但数据源、路由和业务交互已经迁移到 V2 API。

## 当前能力

- `/:lang` 三语首页，包含 PINNED、FEATURED 和普通文章区域。
- `/:lang/posts/:id/:slug?` 文章详情；ID 是查询依据，加载后规范化 slug。
- 分类、标签、归档、标题摘要搜索、关于页和友链页。
- 后端 Markdown 正文渲染、目录、阅读时长和字数。
- 文章评论列表、两层回复、分页、提交状态和错误重试。
- 作者资料、站点配置、建站日期、公开统计和路由访问打点。
- loading、empty、404、locked、error 与 retry 状态。

## 当前边界

- PASSWORD 文章只显示锁定元数据，详情与评论没有解锁流程。
- 留言板后端接口已存在，博客端没有留言板页面。
- SEO、RSS/Atom、sitemap、Open Graph 和结构化数据尚未形成完整发布能力。
- 作者卡总字数、部分社交入口和文章上一篇/下一篇导航尚未补齐，见[开放问题](../../start-here/open-issues.md)的 ISSUE-008。
- Gitalk、Valine、Twikoo、Waline 和旧静态 JSON 已从当前文章主链路清理；后续留言板继续使用 V2 自研评论接口。

本地 API base 为 `/api`，Vite 代理目标默认 `http://localhost:8080`。运行与验证命令见 `../../ops/local-development.md`。
