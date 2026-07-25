# 管理端

> 状态：当前有效
> 适用范围：V2 后台管理应用
> 最后校准：2026-07-25
> 对应代码：`frontend/apps/admin/`
> 权威程度：前端实现摘要

管理端基于 Vue 3、Pinia、Vue Router、Element Plus、ECharts、Vite 和 Vitest，界面框架源自 vue-pure-admin thin i18n。上游来源记录保存在应用目录的 `UPSTREAM.md`。

## 当前页面

- 登录、会话恢复、刷新、退出和 403/404/500 页面。
- PV/UV 趋势、语言分布和热门文章仪表盘。
- 文章列表、新建、完整编辑、Markdown 实时预览、首页槽位、软删除、回收站和恢复。
- 分类、标签、评论、友链和附件管理。
- 站点配置、当前用户资料和密码修改。

## 权限与数据

- 菜单由前端静态路由定义，后端不提供动态菜单。
- ADMIN 可以读写；DEMO 只读，写按钮禁用且后端再次授权。
- DEMO 的非公开文章正文、评论邮箱/IP/User-Agent 和附件内部存储字段由后端裁剪。
- access token 与 refresh token 当前保存在 `myblog-admin-session` localStorage 记录中；刷新失败或结构非法时清理会话。
- 文章编辑草稿按当前用户隔离保存，PASSWORD 口令不进入草稿；退出、会话失效和改密会清理该用户草稿。
- Axios base URL 由 `VITE_API_BASE_URL` 控制；本地留空并通过 `/api` 代理到后端。

本地默认端口为 8848，路由使用 hash 模式。运行与验证命令见 `../../ops/local-development.md`。

## 构建边界

- 业务页面、文章编辑器 Mermaid 和代码高亮按需加载，KaTeX 样式仅随文章编辑器加载。
- Element Plus 只全局注册当前模板实际使用的组件与 `v-loading` 指令；消息和确认框由调用点直接导入。
- 上游假通知和孤立 Welcome 页面已删除；403/404/500 路由保留但不进入业务菜单。仍在使用的布局、主题和菜单搜索能力继续复用上游实现。
- `pnpm check:bundle-budget` 检查 `dist/index.html` 引用的首屏 JS/CSS gzip 体积，CI 在 production build 后执行；预算与完整命令见 [`../../ops/build-and-test.md`](../../ops/build-and-test.md)。

文章预览支持的语法和历史文章恢复步骤见 [`../../content/markdown-authoring.md`](../../content/markdown-authoring.md)。

后端已有能力、管理端消费状态和表单业务语义见已完成的[管理端后端能力消费审查](backend-consumption-audit.md)。
