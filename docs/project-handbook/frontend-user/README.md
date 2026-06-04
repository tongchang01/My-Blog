# frontend-user/ — 前台（博客访客端）规格

> 本目录回答："前台 Vue 应用做什么？页面长什么样？怎么和后端对接？"
> 对应代码目录：`MyBlog-vue/MyBlog-blog/`（V1 现状）/ 重构后的 V2 前台目录
> 当前状态：骨架待填，等 `product/feature-inventory.md` 标注完后再写。

## 范围

只覆盖**访客 + 已登录普通用户**视角的页面：首页、文章、归档、标签、留言、友链、关于、个人中心等。
后台管理界面在 `frontend-admin/`。

## 计划包含的文件

| 文件 | 内容 | 状态 |
|------|------|------|
| `tech-stack.md` | V2 前台技术选型（Vue 版本、UI 库、路由、状态管理、构建工具） | ⏳ |
| `pages.md` | 页面清单：路径、用途、调用的后端接口、关键交互 | ⏳ |
| `components.md` | 通用组件设计（Header / Footer / ArticleCard / CommentTree 等） | ⏳ |
| `state-and-routing.md` | 路由结构、全局状态（用户、主题）、鉴权拦截 | ⏳ |
| `styling.md` | 设计语言、暗黑模式、响应式断点 | ⏳ |

## V1 → V2 已决策事项（详见 `product/feature-inventory.md` + `product/decisions-draft.md`）

- **技术栈**：Vue 3 + Element Plus + Pinia + TypeScript + vue-i18n（前后台统一）
- **国际化**：留，三语 zh / ja / en，路径前缀 `/{lang}/`；UI 文案 vue-i18n 打包，业务内容 DB 三语副本；字体 CSS `:lang()` + Noto Sans SC/JP/Sans（R2 #7-#19）
- **音乐播放器**：删 aplayer，改用 **Spotify 官方 Embed iframe**，读 `t_site_config.spotify_playlist_id`（⑬）
- **相册**：全删（⑫）
- **说说**：全删（⑪）
- **Markdown 渲染**：评论只渲后端 sanitize 后的 `content_html`（R-013 红线）；文章正文走 Vditor 渲染管线
- **暗黑模式**：留，跟随系统 + 手动切换（🟢 候选，待与新功能候选一起最终敲定）
- **活动热图**：从后台仪表盘迁到前台 about 页（⑯）

## 写作约定

- 每个页面写清：路径、所需登录状态、调用接口（引用 api-contract）、关键交互、边界场景（空数据、错误态、loading）
- 不重复 api-contract 的字段定义，只引用
