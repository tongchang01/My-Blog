# 博客端

> 状态：当前有效
> 适用范围：V2 公开读者端
> 最后校准：2026-07-19
> 对应代码：`frontend/apps/blog/`
> 权威程度：前端实现摘要

博客端基于 Vue 3、Pinia、Vue Router、vue-i18n、Axios、markdown-it、Vite 和 Vitest。界面源自 Aurora，但数据源、路由和业务交互已经迁移到 V2 API。

## 当前能力

- 所有公开页面统一使用 `/:lang` 语言前缀；旧的无语言静态地址按已保存语言、浏览器语言、中文默认值依次重定向。
- `/:lang` 三语首页，包含 PINNED、FEATURED 和普通文章区域。
- `/:lang/about`、`/:lang/archives`、`/:lang/tags`、`/:lang/categories`、`/:lang/links`、`/:lang/message-board` 静态入口。
- `/:lang/posts/:id/:slug?` 文章详情；ID 是查询依据，加载后规范化 slug。
- 分类、标签、归档、标题摘要搜索、关于页、友链页和留言板。
- 分类与标签复用页面会随完整 URL 刷新查询，并忽略已取消请求的迟到结果。
- 前台安全解析 Markdown 正文、目录、阅读时长和字数；支持 GFM 表格、任务列表、脚注、KaTeX、代码高亮和 Mermaid。
- 文章评论列表、两层回复、分页、提交状态和错误重试。
- 作者资料、站点配置、建站日期、公开统计和路由访问打点。
- 公开静态页按当前界面语言打点；文章 canonical slug 规范化不重复增加 PV。
- loading、empty、404、locked、error 与 retry 状态。
- PASSWORD 文章密码解锁；令牌仅保存在当前标签页的 sessionStorage，并用于正文与文章评论请求。

## 当前边界

- 顶部导航、文章卡片、搜索结果等核心入口尚未完整使用原生链接/按钮语义，键盘可达性与标题层级待按 ISSUE-019 收口。
- 评论与留言板的数据请求会携带当前 locale，但共用组件的用户可见文案仍为中文，待按 ISSUE-020 补齐三语。
- SEO、RSS/Atom、sitemap、Open Graph 和结构化数据尚未形成完整发布能力。
- 文章上一篇/下一篇导航尚未补齐，见[开放问题](../../start-here/open-issues.md)的 ISSUE-008。
- Gitalk、Valine、Twikoo、Waline 和旧静态 JSON 已从当前文章主链路清理；后续留言板继续使用 V2 自研评论接口。

本地 API base 为 `/api`，Vite 代理目标默认 `http://localhost:8080`。运行与验证命令见 `../../ops/local-development.md`。

文章写作语法与历史文章恢复步骤见 [`../../content/markdown-authoring.md`](../../content/markdown-authoring.md)。
