# 博客端

> 状态：当前有效
> 适用范围：V2 公开读者端
> 最后校准：2026-09-06
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
- 文章评论与留言板共用三语界面，覆盖列表、两层回复、分页、提交状态、成功/失败提示和错误重试；切换语言会重新映射评论时间。
- 作者资料、站点配置、建站日期、公开统计和路由访问打点。
- 公开静态页按当前界面语言打点；文章 canonical slug 规范化不重复增加 PV。
- loading、empty、404、locked、error 与 retry 状态。
- PASSWORD 文章密码解锁；令牌仅保存在当前标签页的 sessionStorage，并用于正文与文章评论请求。
- 空状态、解锁状态、阅读时长、代码复制、Mermaid 和复制署名等用户可见文案统一由 `vue-i18n` 管理，不在业务组件中按 locale 手写文案分支。
- 顶部导航、Logo、语言/搜索/主题/菜单控制、搜索结果、文章卡片和 404 恢复入口使用 `RouterLink`、真实 `a` 或原生 `button`，并提供可识别名称和统一的键盘焦点样式。

## Spotify 基础播放器（第一版）

- 复用公开站点配置的 `spotifyPlaylistId`，使用 Spotify 官方 iframe 默认配色与标准高度，不新增后端接口、依赖、OAuth 或自定义播放控制。
- 播放器挂载在路由视图外的常驻顶部导航中，音乐图标位于语言切换左侧；未配置歌单时不显示入口，读者主动展开后才加载 Spotify。折叠保留 iframe，不主动暂停；播放和暂停由 Spotify 官方控件提供。
- 路由与语言切换不主动重建播放器；配置请求失败保留上次成功配置，成功清空或更换歌单会关闭旧播放器。不以 iframe 加载事件宣称音频播放成功；没有自定义单独重载或停止关闭按钮，异常时可刷新页面重新加载。
- 曲目与顺序在 Spotify 管理同一歌单；完整播放能力取决于 Spotify 账户、浏览器和服务限制。
- 第一版采用已确认的顶部入口：桌面端由音乐图标下方靠右直接展开标准播放器；原有右下角入口、说明、浅色操作栏及专用按钮已移除。点击面板外部或在音乐入口按 Escape 仅收起；Escape 收起将焦点返回音乐入口。Spotify iframe 内的键盘事件由 Spotify 自身处理。
- 小屏幕使用底部面板布局，真实设备验收仍单独跟踪。首次展开需要等待第三方资源加载，本版未加入预加载或加载状态提示。后续验收见 [ISSUE-010](../../start-here/open-issues.md)，验证与发布记录见[当前状态](../../start-here/current-status.md)。

## 当前边界

- 标题层级存在 ISSUE-019：首页文章卡片重复使用 `h1`，文章详情存在两个 `h1`，留言板没有页面级 `h1`；当前按 P3 暂缓，除非产品决策重新提出，否则不主动实施。
- SEO、RSS/Atom、sitemap、Open Graph 和结构化数据尚未形成完整发布能力。
- 文章上一篇/下一篇导航尚未补齐，见[开放问题](../../start-here/open-issues.md)的 ISSUE-008。
- Gitalk、Valine、Twikoo、Waline 和旧静态 JSON 已从当前文章主链路清理；后续留言板继续使用 V2 自研评论接口。

本地 API base 为 `/api`，Vite 代理目标默认 `http://localhost:8080`。运行与验证命令见 `../../ops/local-development.md`。

文章写作语法与历史文章恢复步骤见 [`../../content/markdown-authoring.md`](../../content/markdown-authoring.md)。
