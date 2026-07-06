# 前台 Blog 联调状态

> 状态：当前有效
> 适用范围：V2 前台读者端
> 最后校准：2026-07-06
> 对应代码：`frontend/apps/blog/`
> 权威程度：前台完成度记录

## 已完成首批

- 本轮校准以 `E:\My-Blog\frontend\apps\blog/src/` 的前端集成分支源码为准。
- 三语入口和语言保存。
- `/:lang` 首页和 `/:lang/posts/:id/:slug?` ID 主导文章路由。
- `GET /api/public/site-config` 接入，失败时使用 typed defaults 降级。
- 公开文章列表、分页、loading、empty、error、retry。
- 首页已改接 `GET /api/public/articles/home` 聚合接口，使用 `pinnedArticle`、`featuredArticles`、`articles` 渲染首屏。
- 首页不再用普通文章分页的 `records[0]` / `records.slice(...)` 推断置顶或推荐语义。
- 公开文章详情、canonical slug、PASSWORD 锁定态、404、网络错误、retry。
- 分类和标签列表已接入 `GET /api/public/categories`、`GET /api/public/tags`，数量使用公开 `articleCount` 映射。
- 分类和标签公开页已使用 slug 路由 `/:lang/categories/:slug`、`/:lang/tags/:slug`，并通过公开文章列表 `categorySlug/tagSlug` 查询。
- 归档页已接入 `GET /api/public/archives`，按后端返回的年月分组渲染时间线，文章链接使用 `/:lang/posts/:id/:slug?` ID 主导路由。
- 关于页已接入公开站点配置 `aboutMd`，不再读取旧 `/pages/about/index.json`。
- 搜索弹窗已接入公开文章 `keyword` 查询，不再读取旧 `/search.json`；第一版只展示标题和摘要，不搜索正文，不做高亮片段。
- 访问统计已接入 V2：公开路由导航后写入 page-view，页脚展示 V2 站点统计摘要并按 `startedDate` 计算建站天数，`PostStats` 仅保留阅读时长和字数。
- 文章详情页评论已接入 V2 自研公开评论 API，支持评论列表、回复、分页、提交成功/待审核提示和错误重试；文章评论主链路不再初始化 Gitalk / Valine / Twikoo / Waline。
- 首页已停止请求旧 Hexo/Aurora mock 数据。
- Markdown 正文通过 `markdown-it` 渲染，禁用原始 HTML。

## 待补齐

- 友链仍需替换旧 JSON 数据源或旧页面实现。
- 留言板评论前台接入。
- PASSWORD 文章完整解锁流程。
- Spotify Embed。
- Markdown chunk 分包和 Sass 旧 API/`@import` 清理。

## 对应 open issues

- O-001 PASSWORD 文章完整解锁流程。
- O-003 前台读者主链路补齐。
- O-004 前台评论、留言和统计接入。
- O-019 评论和留言迁移到 V2 自研 API。
