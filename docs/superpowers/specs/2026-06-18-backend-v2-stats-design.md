# Backend V2 访问统计纵向切片设计

## 1. 目标

在当前 `com.tyb.myblog.v2` 模块化单体架构下重建 `stats` 模块，覆盖：

- 公开页面访问明细写入 `t_page_view`。
- 按文章、语言和 JST 日期聚合到 `t_page_view_daily`。
- 后台查询访问摘要、每日趋势、热门文章 TOP 10 和语言分布。
- 启动补算、定时校准和 90 天访问明细清理。
- 使用每日轮换的访客 hash 计算日 UV，不保存原始 IP 和 User-Agent。

stats 完成后，M3 后端业务模块重建阶段结束，下一阶段进入前台与后台前端骨架。

## 2. 不做

- 不修改冻结的 `V1__init.sql`。
- 不引入 Redis、消息队列、时序数据库或第三方统计服务。
- 不按首页、归档、分类、标签、友链等非文章路由细分统计；这些页面统一聚合为 `articleId=0`。
- 不做设备、浏览器、地域、来源域名或爬虫精确分析。
- 不提供跨日期独立访客数。每日访客 hash 会轮换，跨日 UV 不具备准确去重条件。
- 不实现公开活动热图。该需求的“访问热图”与“写作热图”口径尚未统一，留到前端阶段单独确认。
- 不在 stats 中组合总文章数、总评论数和最新评论；前端复用 content/comment 已有后台接口。

## 3. 统计口径

### 3.1 PV 与 UV

- 每次通过校验的公开页面打点计入一次 PV。
- 同一 `visitorHash + articleId + lang + JST 日期` 在当天只计一次 UV。
- 重复刷新会增加 PV，但不会重复增加当天 UV。
- 后台管理页面、普通 API 请求和健康检查不打点。

### 3.2 页面范围

- 文章详情页携带具体 `articleId`。
- 其他公开页面不携带 `articleId`，明细表保存 `NULL`，聚合表使用 `0`。
- 文章打点前通过 `content.application` 校验文章是否允许公开访问。
- `PUBLISHED` 与 `PASSWORD` 文章允许打点；`DRAFT`、`PRIVATE`、`SCHEDULED` 和已删除文章拒绝打点。
- 非文章页面只形成全站汇总，不区分具体路由。

### 3.3 语言与日期

- `lang` 只接受 `zh`、`ja`、`en`。
- 统计日期统一通过项目注入的 `Clock` 按 `Asia/Tokyo` 计算。
- 聚合主键保持冻结 schema 定义：`article_id + lang + stat_date`。

## 4. 访客隐私

访客标识按以下方式生成：

```text
HMAC-SHA256(statsHashSecret, jstDate + "\n" + trustedClientIp + "\n" + userAgent)
```

- 客户端 IP 必须复用 common-infra 的可信代理解析结果。
- 不把原始 IP 或 User-Agent 写入 stats 表。
- JST 日期参与摘要，使同一访客的 hash 每日自动变化。
- stats 使用独立密钥，不复用 JWT、文章访问 token 或其他安全密钥。
- local/prod 缺少 `MYBLOG_STATS_HASH_SECRET` 时启动失败；test profile 使用固定测试值。

每日轮换会主动放弃跨日访客追踪。因此后台只展示每日 UV、今日 UV 和平均每日 UV，不展示误导性的“区间独立访客数”。

## 5. 架构边界

```text
stats.web -> stats.application -> stats.domain <- stats.infrastructure
                                  |
                                  v
                         content.application
```

### 5.1 web

- 接收公开打点请求和后台统计查询。
- 完成请求格式、语言、日期范围和分页上限校验。
- 从 HTTP 请求读取 User-Agent 与 Referer，不允许请求体覆盖这些值。
- 不访问 Mapper、Entity 或 content infrastructure。

### 5.2 application

- 编排文章可见性校验、访客 hash、访问明细写入。
- 编排单日聚合、启动补算、定时校准和明细清理。
- 查询日趋势、热门文章和语言分布，并通过 content application 批量补齐文章标题。
- 定义事务边界，不把调度注解放入领域层。

### 5.3 domain

- 定义访问事件、支持语言、统计日期范围、每日聚合结果和 Repository 端口。
- 保存 PV/UV 口径和日期范围等业务不变量。
- 不依赖 Spring Web、Servlet API、MyBatis 或具体调度实现。

### 5.4 infrastructure

- 使用 MyBatis-Plus Entity、Mapper 接口和 Mapper XML 实现持久化。
- 使用 HMAC 适配器实现访客 hash。
- 使用 Spring Scheduler 触发聚合和清理。
- 聚合、趋势、TOP 10、语言分布及清理 SQL 全部放在 XML，不使用注解 SQL。

## 6. 接口设计

### 6.1 公开页面打点

```text
POST /api/public/stats/page-views
```

请求：

```json
{
  "articleId": "123",
  "lang": "zh"
}
```

- `articleId` 可选；缺失时表示非文章页面。
- `lang` 必填。
- Referer 从 HTTP `Referer` 头读取，去除首尾空白后最多保留 512 字符。
- User-Agent 缺失时按空字符串参与 hash，不伪造默认浏览器标识。
- 成功响应继续使用项目统一 `ApiResponse<Void>`。

公共打点按可信客户端 IP 限制为每分钟 120 次。超限返回现有 `429 + 90002`，用于限制单一来源灌入明细，不承担精确反爬职责。

### 6.2 后台数据总览

```text
GET /api/admin/stats/dashboard?from=2026-05-20&to=2026-06-18
```

- ADMIN 和 DEMO 可读，GUEST 不可访问。
- 默认查询最近 30 个 JST 自然日，起止日期均包含。
- 查询范围最长 366 天。
- `from`、`to` 必须同时提供或同时省略，且 `from <= to`。

响应数据包含：

```json
{
  "periodPv": 1200,
  "todayPv": 48,
  "todayUv": 31,
  "averageDailyUv": 26.4,
  "trend": [
    {"date": "2026-06-18", "pv": 48, "uv": 31}
  ],
  "topArticles": [
    {"articleId": "123", "title": "文章标题", "pv": 320, "uv": 180}
  ],
  "languageDistribution": [
    {"lang": "zh", "pv": 900, "ratio": 0.75}
  ]
}
```

- `periodPv` 是所选区间日聚合 PV 之和。
- `averageDailyUv` 是区间内每日 UV 的算术平均值，不表示跨日去重访客数。
- 趋势需要补齐无访问日期，以 `pv=0, uv=0` 返回连续日期序列。
- TOP 10 只包含 `articleId > 0` 的文章，按 PV 降序、articleId 升序稳定排序。
- 语言分布按 PV 计算；区间 PV 为 0 时 ratio 返回 0。
- 已删除或不再公开的文章仍可保留历史聚合，但标题批量查询失败时返回 `title=null`，不丢弃统计行。

## 7. 数据写入与聚合

### 7.1 明细写入

公开打点链路只执行：

1. 校验请求和限流。
2. 若有 articleId，通过 content application 校验公开可见性。
3. 解析可信客户端 IP，生成每日访客 hash。
4. 规范化 Referer。
5. 向 `t_page_view` 追加一行。

写入不等待聚合，也不直接更新 `t_page_view_daily`，避免访问请求竞争同一聚合行。

### 7.2 单日重算

单日聚合按以下 SQL 语义计算：

```sql
SELECT COALESCE(article_id, 0), lang,
       COUNT(*) AS pv,
       COUNT(DISTINCT visitor_hash) AS uv
FROM t_page_view
WHERE created_at >= :dayStart
  AND created_at < :nextDayStart
GROUP BY COALESCE(article_id, 0), lang
```

应用层在单个事务中删除目标日期旧聚合并批量写入新结果。重复执行同一日期不会累加或翻倍。聚合过程中刚写入的访问可能暂时遗漏，但下一轮重算会修正。

### 7.3 调度

- 每 5 分钟重算今天和昨天。
- 应用启动后补算最近 90 个 JST 自然日。
- 每天凌晨先校准最近 90 天，再物理删除 90 天以前的 `t_page_view`。
- `t_page_view_daily` 长期保留，不软删除。
- 补算和清理使用数据库约束或本地互斥避免同一实例内重入；V2 单实例部署不引入分布式锁。

## 8. 跨模块依赖

stats 只允许依赖 content application 暴露的接口：

- `PublicArticleStatisticsPolicyService`：校验文章是否可公开打点。
- `ArticleStatisticsSummaryService`：按一组 articleId 批量返回当前可用标题。

stats 不访问 `ArticleRepository`、`ArticleMapper`、`ArticleEntity` 或 content domain。标题补齐必须批量执行，禁止 TOP 10 查询产生 N+1 调用。

## 9. 错误处理

- 请求字段校验失败：使用现有通用参数错误。
- 文章不存在或不可公开访问：复用 content 公开查询的 404 语义。
- 单 IP 每分钟超过 120 次：`429 + 90002`。
- hash 密钥缺失：local/prod 启动失败，不降级为无密钥 SHA-256。
- 明细写入或后台查询数据库失败：由全局异常体系返回系统错误，日志不记录原始 IP、User-Agent 或 hash 密钥。
- 聚合或清理任务失败：记录中文错误日志并等待下一次调度重试，不删除未完成聚合范围的明细。

stats 暂不新增业务错误码；后续只有出现无法由通用或 content 错误表达的业务分支时，才从 `50xxx` 增加。

## 10. 测试策略

### 10.1 领域与应用测试

- 同一日期、IP、User-Agent 生成相同 hash。
- 日期、IP 或 User-Agent 任一变化都会生成不同 hash。
- hash 输入和日志不泄漏密钥或原始客户端信息。
- 文章与非文章打点正确转换 articleId。
- 语言、日期范围和 366 天上限校验。
- 默认最近 30 天按 JST 计算。
- 趋势日期补零、平均每日 UV 和语言占比计算。
- TOP 10 标题批量补齐，缺失标题保留统计行。

### 10.2 持久化与任务测试

- 并发追加访问明细不覆盖数据。
- PV 使用总行数，UV 使用每日不同 visitorHash 数。
- `NULL article_id` 聚合为 `0`。
- 重复执行单日聚合不翻倍。
- JST 午夜边界不会把访问归入错误日期。
- 启动补算覆盖最近 90 天。
- 清理先校准后删除，且只删除 90 天以前明细。
- 聚合、趋势、TOP 10 和语言分布 XML SQL 使用 H2 集成测试验证。

### 10.3 Web 与架构测试

- 打点请求不接受客户端指定 IP、User-Agent 或 Referer 正文值。
- 非法语言、不可见文章、超限和成功响应契约。
- ADMIN/DEMO 可访问 dashboard，GUEST 不可访问。
- OpenAPI 使用独立文档模型，不暴露内部 Command。
- ArchUnit 验证 stats 四层依赖和跨模块仅依赖 content application。
- 最后运行全量 `mvn clean test` 和既有静态规则检查。

## 11. 实施拆分建议

后续实施计划拆成五个独立中文提交：

1. 建立访问统计领域模型与明细持久化。
2. 实现公开页面访问打点与隐私 hash。
3. 实现日聚合、补算与明细清理任务。
4. 实现后台统计查询与文章标题补齐。
5. 完成接口契约、集成验证与项目文档收尾。

每个批次都必须先写失败测试，再实现最小代码，并执行定向回归；复杂 SQL 必须进入 Mapper XML，生产代码保留必要中文业务注释。
