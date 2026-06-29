# 前台 Blog 联调状态

> 状态：待校准
> 适用范围：V2 前台读者端
> 最后校准：2026-06-29
> 对应代码：`frontend/apps/blog/`
> 权威程度：前台完成度记录

## 已完成首批

- 三语入口和语言保存。
- `/:lang` 首页和 `/:lang/posts/:id/:slug?` ID 主导文章路由。
- `GET /api/public/site-config` 接入，失败时使用 typed defaults 降级。
- 公开文章列表、分页、loading、empty、error、retry。
- 公开文章详情、canonical slug、PASSWORD 锁定态、404、网络错误、retry。
- 首页已停止请求旧 Hexo/Aurora mock 数据。
- Markdown 正文通过 `markdown-it` 渲染，禁用原始 HTML。

## 待补齐

- 分类、标签、归档、友链、关于、搜索。
- 评论、留言和访问统计前台接入。
- PASSWORD 文章完整解锁流程。
- Spotify Embed。
- Markdown chunk 分包和 Sass 旧 API/`@import` 清理。

## 对应 open issues

- O-001 PASSWORD 文章完整解锁流程。
- O-003 前台读者主链路补齐。
- O-004 前台评论、留言和统计接入。
- O-010/O-011 公开接口 ID 类型校准。

