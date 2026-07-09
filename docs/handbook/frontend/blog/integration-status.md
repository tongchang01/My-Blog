# 前台 Blog 联调状态

> 状态：当前有效
> 适用范围：V2 前台读者端
> 最后校准：2026-07-08
> 对应代码：`frontend/apps/blog/`
> 权威程度：前台完成度记录

## 已完成

- 三语入口、语言保存和主路由。
- 首页、公开文章列表、文章详情、分类页、标签页、归档页、关于页、搜索、友链页。
- 站点配置、作者卡片、移动菜单、访问统计、文章评论。
- 首页使用 `GET /api/public/articles/home`。
- 文章列表、分类/标签筛选和搜索使用 `GET /api/public/articles`。
- 分类和标签使用 `GET /api/public/categories`、`GET /api/public/tags`。
- 归档使用 `GET /api/public/archives`。
- 关于页使用公开站点配置中的 `aboutMd`。
- 友链页使用 `GET /api/public/friend-links` 简版卡片。
- 访问统计使用 V2 page-view 写入和站点 summary。
- 文章详情评论使用 V2 自研评论 API。
- 旧 `authors/blog-author.json`、`statistic.json`、`pages/links/index.json`、`avatarWall`、通用 `page/[slug].vue`、旧第三方评论工具、旧分类/标签包装 store 和旧 `api/index.ts` 已清理。

## 后置

- 留言板评论前台接入。
- PASSWORD 文章完整解锁流程。
- 完整 SEO / RSS / Sitemap / Open Graph / 结构化数据。
- Spotify Embed。
- Markdown chunk 分包和 Sass 旧 API / `@import` 清理。

## 对应 open issues

- O-001 PASSWORD 文章完整解锁流程。
- O-004 前台评论、留言和统计接入。
