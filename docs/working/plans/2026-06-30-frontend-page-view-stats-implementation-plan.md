# 前台访问统计接入实施思路

> 状态：方案已定 / 实现待设计。本文记录后续实现方向，避免前台接入时继续沿用第三方统计口径。

## 背景

Aurora/Hexo 原前台的统计展示分为两类：

- `PostStats.vue` 原始核心展示是阅读时长和字数。
- `PostStats.vue` 使用 Waline / Twikoo / Valine / LeanCloud 展示文章或页面浏览量。
- `PostStats.vue` 使用 Waline / Twikoo 展示文章评论数。
- `FooterContainer.vue` 使用 Waline / Busuanzi 展示全站 PV / UV。
- `FooterContainer.vue` 使用 `themeConfig.site.started_date` 在前台计算建站天数。

V2 后端已经有自研访问统计打点和后台 dashboard，但当前没有公开读取单篇文章浏览量或全站 PV / UV 的前台接口。
后台 dashboard 已经展示 PV / UV、趋势、TOP 文章和语言分布；后台文章列表已经展示文章评论数。

## 已定原则

1. 第一版前台接入先打通 V2 访问打点。
2. 文章详情页访问时调用 `POST /api/public/stats/page-views`，传 `articleId` 和 `lang`。
3. 首页、分类、标签、归档、关于、友链、搜索等非文章页访问时调用同一打点接口，`articleId` 固定传 `0`，沿用现有 `0=首页/非文章页汇总` 约定。
4. 不再让前台同时展示第三方统计值和 V2 后台统计值，避免读者端与后台 dashboard 数据不一致。
5. `PostStats.vue` 第一版只保留阅读时长和字数。
6. `PostStats.vue` 的第三方浏览量和第三方评论数展示注释或删除，代码注释中写明后期可分别对接 V2 `viewCount` 和文章 `commentCount`。
7. `FooterContainer.vue` 的 Waline / Busuanzi PV / UV 展示绑定注释或删除，但页脚继续展示自研统计数据：今日访客、访问量、建站天数。
8. 页脚 `今日访客` 口径为全站、全语言、所有公开页面今日日 UV 合计。
9. 页脚 `访问量` 口径为全站、全语言、所有公开页面累计 PV。
10. 页脚 `建站天数` 由公开站点配置 `startedDate` 在前台本地计算；为空时不展示。

## 展示口径

### 文章浏览量

`PostStats.vue` 第一版不展示文章浏览量。如果后期需要在文章详情展示“本文浏览量”，优先采用低成本方案：

- 后端在 `PublicArticleDetailVO` 增加 `viewCount`。
- 前台文章详情直接读取 `viewCount`，不额外请求统计接口。
- 统计口径优先使用已聚合 PV；如要求近实时，再评估是否叠加当日未聚合明细。

该方案实现成本不高，因为后端已经有文章访问打点和日聚合表，主要缺公开读取字段。

### 页脚统计

`FooterContainer.vue` 第一版展示：

- `今日访客：123`
- `访问量：12345`
- `建站天数：365 天`

后端需要新增公开站点统计摘要接口：

- `GET /api/public/stats/site-summary`
- 返回 `todayUv`：全站、全语言、所有公开页面今日日 UV 合计。
- 返回 `totalPv`：全站、全语言、所有公开页面累计 PV。

当前后台 dashboard 已有 PV / UV 数据，但它是后台接口，不直接作为读者端页脚数据源。公开摘要接口可直接基于 `t_page_view_daily` 聚合查询，接受定时聚合延迟，不要求刷新后立即变化。

### 建站天数

建站天数不是访问统计结果。原前台从 `themeConfig.site.started_date` 读取起始日期并在浏览器本地计算天数。

建站天数保留在页脚展示。`startedDate` 纳入 V2 站点配置，由后台维护日期，公开站点配置返回该字段，前台继续本地计算天数。为空时前台不展示建站天数。

建议 DB 字段：

- `t_site_config.started_date DATE NULL`

## 三端变更范围

### 后端

- 已有：公开访问打点接口、日 PV / UV 聚合、后台 dashboard。
- 新增公开站点统计摘要接口：`GET /api/public/stats/site-summary`。
- 站点摘要返回 `todayUv` 和 `totalPv`。
- `todayUv` 从 `t_page_view_daily` 查询当天 `SUM(uv)`，不按语言或文章过滤。
- `totalPv` 从 `t_page_view_daily` 查询全部日期 `SUM(pv)`，不按语言或文章过滤。
- 允许聚合延迟，不叠加当日未聚合明细。
- 站点配置表新增 `started_date DATE NULL`。
- 站点配置 Entity / Domain / Admin VO / Update Request / Public VO 补 `startedDate`。
- 后期展示文章浏览量时，给公开文章详情补 `viewCount`。
- 已有：公开文章列表和详情返回 `commentCount`，后台文章列表也展示 `commentCount`。

### 后台

- 第一版无强制改动，继续使用现有 dashboard。
- 已有：后台 dashboard 展示 PV / UV、趋势、TOP 文章和语言分布。
- 已有：后台文章列表展示评论数。
- 站点配置页增加建站日期维护项。
- `startedDate` 可为空；为空时前台不展示建站天数。

### 前台

- 新增公开路由访问打点：文章页携带真实 `articleId`，非文章页携带 `articleId=0`。
- `PostStats` 第一版只展示阅读时长和字数。
- `PostStats` 注释或删除 Waline / Twikoo / Valine / LeanCloud 的浏览量展示绑定，并在代码注释中说明后期可通过后端文章详情 `viewCount` 恢复。
- `PostStats` 注释或删除 Waline / Twikoo 的评论数展示绑定，并在代码注释中说明后期可通过文章接口 `commentCount` 恢复。
- `FooterContainer` 注释或删除 Waline / Busuanzi 的 PV / UV 展示绑定。
- `FooterContainer` 调用公开站点统计摘要接口，展示 `今日访客：todayUv` 和 `访问量：totalPv`。
- `FooterContainer` 从公开站点配置读取 `startedDate`，本地计算并展示 `建站天数`；为空时不展示。
- 后期如后端返回 `viewCount`，再恢复文章详情浏览量展示。

## 验证建议

- 前台路由切换测试：文章页会调用 page-view 打点并传 `articleId`。
- 前台路由切换测试：非文章页会调用 page-view 打点并传 `articleId=0`。
- 组件测试：`PostStats` 不渲染第三方浏览量和第三方评论数 DOM。
- 组件测试：`FooterContainer` 不渲染 Waline / Busuanzi PV / UV DOM。
- 组件测试：`FooterContainer` 使用 `site-summary.todayUv` 和 `site-summary.totalPv` 展示今日访客和访问量。
- 组件测试：`FooterContainer` 使用公开站点配置 `startedDate` 计算建站天数；`startedDate` 为空时不展示。
- 后端 API 测试：公开站点统计摘要返回全站、全语言、所有公开页面的 `todayUv` 和 `totalPv`。
- 后端站点配置测试：后台可保存 `startedDate`，公开站点配置可读取 `startedDate`。
- 后期恢复评论数展示时，测试评论数来自文章详情 `commentCount`。
- 后期新增展示字段时，补后端 API 测试和前台组件测试。
