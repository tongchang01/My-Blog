# 前台访问统计闭环实施计划

> 状态：执行中
> 适用范围：O-020 访问统计前台打点和展示口径
> 最后校准：2026-07-05

## 目标

补齐 V2 前台访问统计闭环：

- 前台公开路由访问时写入 V2 page-view 打点。
- 页脚展示 V2 自研统计摘要：今日访客、访问量。
- 页脚恢复建站天数展示，数据来自 V2 站点配置 `startedDate`。
- `PostStats` 第一版只保留阅读时长和字数，不继续显示第三方浏览量/评论数。

## 不做范围

- 不恢复单篇文章浏览量展示。
- 不接文章评论和留言。
- 不实现 PASSWORD 完整解锁。
- 不做 SEO / Sitemap / RSS。
- 不新增第三方统计服务。

## 实施拆分

### 1. 后端公开统计摘要

- 新增 `GET /api/public/stats/site-summary`。
- 返回：
  - `todayUv`：JST 今天全站日 UV 合计。
  - `totalPv`：全部日期全站 PV 合计。
- 数据源复用 `t_page_view_daily`，允许聚合延迟，不叠加未聚合明细。
- 补 web slice 测试和集成测试。

### 2. 站点配置建站日期

- `t_site_config` 新增 `started_date DATE NULL`。
- 后端站点配置 domain / application / web / persistence 补 `startedDate`。
- 后台站点配置表单增加一个日期输入。
- 公开站点配置返回 `startedDate`。
- 补后端和前端 mapper/form 测试。

### 3. 前台 page-view 打点

- 新增 stats API 客户端。
- 在 router `afterEach` 中打点。
- 文章详情路由传真实 `articleId`，其他公开页面传 `articleId=0`。
- 打点失败只吞掉，不阻断页面导航。
- 补路由打点单元测试。

### 4. 前台页脚统计和 PostStats 清理

- `FooterContainer` 拉取 `site-summary` 并展示今日访客、访问量。
- `FooterContainer` 使用公开站点配置 `startedDate` 计算建站天数；为空不展示。
- 删除 Waline / Busuanzi 页脚统计 DOM。
- `PostStats` 删除第三方浏览量/评论数 DOM，只展示阅读时长和字数。
- 补组件测试。

## 验证命令

- `mvn -f MyBlog-springboot-v2/pom.xml -Dtest=PublicStatsControllerTest,StatsIntegrationTest,PublicSiteConfigControllerTest,AdminSiteConfigControllerTest,SiteConfigUpdateServiceTest test`
- `pnpm --dir frontend/apps/admin typecheck`
- `pnpm --dir frontend/apps/admin test`
- `pnpm --dir frontend/apps/blog typecheck`
- `pnpm --dir frontend/apps/blog test`
- 阶段收尾再按风险运行前后端 build。
