# 博客前台现状

> 代码目录：`frontend/apps/blog/`
>
> 当前定位：从 Aurora/Hexo 前台逐步迁移到 MyBlog V2 后端；保留现有视觉主题，不再把 Hexo JSON 作为新功能的数据契约。

## 运行环境

- Node.js：`^20.19.0 || >=22.12.0`，当前已在 Node 24 验证。
- pnpm：`9.15.9`，以 `packageManager` 和 lockfile 为准。
- 后端：默认通过 Vite 将 `/api` 代理到 `http://localhost:8080`。

```powershell
cd frontend/apps/blog
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

质量检查：

```powershell
corepack pnpm lint
corepack pnpm typecheck
corepack pnpm exec vitest run
corepack pnpm build
```

## 已完成的首批联调

- `/` 优先使用已保存语言；首次访问按浏览器语言映射到 `zh`、`ja` 或 `en`。
- 建立 `/:lang` 首页和 `/:lang/posts/:id/:slug?` ID 主导文章路由。
- 接入 `GET /api/public/site-config`，后端请求失败时使用 typed defaults 降级渲染。
- 接入公开文章列表、分页、loading、empty、error 和 retry 状态。
- 接入公开文章详情；支持 canonical slug、PASSWORD 锁定、404、网络错误和 retry 状态。
- 文章、分类和标签 ID 在公开 JSON 契约中均按 string 使用，避免 JavaScript 精度丢失。
- 正文通过 `markdown-it` 渲染，禁用原始 HTML，并为外链补充安全属性。
- 首页已停止请求 `site.json`、`posts/1.json`、`features.json`、旧搜索索引和旧友情链接页面数据。

## 仍然保留在前端 defaults 的配置

后端表当前未覆盖以下内容，因此暂由 `features/site-settings/defaults.ts` 提供：

- 主题颜色、暗黑模式和布局开关；
- 导航菜单；
- 作者与社交链接；
- Aurora 兼容外观配置。

这些字段后续由后台管理系统和扩展后的站点配置契约接管，不在首批联调中扩大后端 schema。

## 后续批次

- 后台管理前端工程；
- 分类、标签、归档、友链、关于页与搜索的 V2 API 迁移；
- 自研评论、留言和访问统计前台接入；
- PASSWORD 文章完整解锁流程；
- Spotify Embed；
- Markdown chunk 分包和 Sass 旧 API/`@import` 清理。

## 本次验收边界

2026-06-20 使用 H2 test profile 完成真实 Controller/SQL/Vite 代理联调：站点配置、空文章列表、语言保存、404、后端离线错误态和重试恢复均通过。测试库没有文章，因此非 PASSWORD 详情、canonical 替换和 PASSWORD 页面状态由 mapper/store/controller 自动测试覆盖，尚未用本机 MySQL 数据手工验收。

`local` profile 启动前必须设置：

- `MYBLOG_DATASOURCE_USERNAME`
- `MYBLOG_DATASOURCE_PASSWORD`
- `MYBLOG_JWT_SECRET`
- `MYBLOG_STATS_HASH_SECRET`
