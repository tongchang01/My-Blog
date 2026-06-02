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

## V1 → V2 的待定事项

- Vue 3 + Element Plus + Pinia 大概率保留（V1 前台已是 Vue 3）
- 国际化 vue-i18n：看 `product/feature-inventory.md` 标注结果
- 音乐播放器 aplayer：跟随"音乐功能"去留
- Markdown 渲染：保留 markdown-it 全家桶，但精简插件
- 相册组件：跟随"相册功能"去留

## 写作约定

- 每个页面写清：路径、所需登录状态、调用接口（引用 api-contract）、关键交互、边界场景（空数据、错误态、loading）
- 不重复 api-contract 的字段定义，只引用
