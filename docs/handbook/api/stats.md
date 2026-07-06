# 访问统计接口契约

> 状态：当前有效
> 适用范围：V2 后端 stats 模块、前台 blog、后台 admin
> 最后校准：2026-07-05
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats/web/`
> 权威程度：API 契约

## 本文档回答什么问题

本文档记录公开页面访问打点和后台访问统计总览接口契约，并说明 PV、UV、语言分布和 TOP 文章的统计口径。

## 1. 公开页面访问打点

```http
POST /api/public/stats/page-views
Content-Type: application/json
```

鉴权：匿名。

请求体：

```json
{
  "articleId": 123,
  "lang": "zh"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `articleId` | number/null | 否 | 文章页面传文章 ID；非文章页面省略或传 `null` |
| `lang` | string | 是 | 只接受 `zh`、`ja`、`en` |

服务端从请求上下文读取：

- 可信客户端 IP。
- `User-Agent`。
- `Referer`。

客户端不得在 JSON 中提交这些字段。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": null
}
```

规则：

- `PUBLISHED` 和 `PASSWORD` 文章可以打点。
- `PRIVATE`、`DRAFT`、`SCHEDULED`、已删除和不存在的文章不写入明细。
- 非文章页面保存 `articleId = null`，日聚合中统一为非文章口径。
- 同一 IP 超过进程内限流阈值时返回 `90002 + 429`。

错误：

| 场景 | HTTP | code |
|------|------|------|
| `lang` 缺失或非法 | 400 | `90001` |
| 文章不存在、已删除或状态不可打点 | 404 | `90003` |
| 同一 IP 超过限流阈值 | 429 | `90002` |

## 2. 统计隐私口径

- 每次通过校验的打点增加一次 PV。
- 日 UV 在同一 `articleId + lang + JST 日期` 内按 visitor hash 去重。
- visitor hash 使用 JST 日期、客户端 IP、User-Agent 和服务端密钥生成 HMAC-SHA256。
- visitor hash 每日轮换。
- 数据库不保存原始 IP、原始 User-Agent 或 hash secret。
- 明细只保存 hash、可选 referrer 和访问时间等必要字段。
- API 不提供跨日期独立访客数。

## 3. 公开站点统计摘要

```http
GET /api/public/stats/site-summary
```

鉴权：匿名。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "todayUv": 20,
    "totalPv": 1000
  }
}
```

字段说明：

| 字段 | 含义 |
|------|------|
| `todayUv` | JST 今天全站、全语言、所有公开页面的日 UV 合计 |
| `totalPv` | 全站、全语言、所有公开页面累计 PV |

当前接口直接读取 `t_page_view_daily` 聚合表，接受聚合任务延迟，不叠加尚未聚合的访问明细。

## 4. 后台访问统计总览

```http
GET /api/admin/stats/dashboard?from=2026-06-01&to=2026-06-30
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

Query：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `from` | date | 否 | JST 日期，格式 `yyyy-MM-dd` |
| `to` | date | 否 | JST 日期，格式 `yyyy-MM-dd` |

规则：

- `from` 和 `to` 必须同时出现或同时省略。
- 省略时默认查询包含今天在内的最近 30 个 JST 自然日。
- 自定义区间包含首尾。
- 最大闭区间 366 天。
- 开始日期晚于结束日期返回 `400 + 90001`。
- 即使自定义区间不包含今天，`todayPv` 和 `todayUv` 仍单独查询 JST 今天。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "periodPv": 1000,
    "todayPv": 50,
    "todayUv": 20,
    "averageDailyUv": 12.3,
    "trend": [
      {
        "date": "2026-06-01",
        "pv": 10,
        "uv": 5
      }
    ],
    "topArticles": [
      {
        "articleId": 123,
        "title": "文章标题",
        "pv": 100,
        "dailyUvSum": 30
      }
    ],
    "languageDistribution": [
      {
        "language": "zh",
        "pv": 800,
        "ratio": 0.8000
      }
    ]
  }
}
```

字段说明：

| 字段 | 含义 |
|------|------|
| `periodPv` | 查询区间总 PV |
| `todayPv` | JST 今天 PV |
| `todayUv` | JST 今天日 UV |
| `averageDailyUv` | 区间内各日 UV 之和除以完整区间天数 |
| `trend` | 从 `from` 到 `to` 的连续日期 PV/UV，缺失日期补零 |
| `topArticles` | TOP 10 文章访问数据 |
| `languageDistribution` | 各语言 PV 及占区间 PV 比例 |

`topArticles[].dailyUvSum` 是区间内各日 UV 的算术和，不是跨日独立访客数。

`topArticles[].articleId` 在 HTTP JSON 边界输出为 string；统计聚合、查询和 `articleId=0` 的首页/非文章页汇总语义不变。

错误：

| 场景 | HTTP | code |
|------|------|------|
| access token 缺失或失效 | 401 | `10002` |
| 非 ADMIN/DEMO 访问 | 403 | `10003` |
| 日期缺失一端、格式非法、区间非法或跨度过大 | 400 | `90001` |

## 5. 聚合与清理

当前实现口径：

- 日聚合定时任务每 5 分钟幂等重算。
- 应用启动时补算今天和昨天。
- 维护任务清理 90 天前访问明细。
- 聚合数据保留，用于后台趋势和 TOP 文章。

## 6. DEMO 边界

DEMO 可读取 dashboard，统计 dashboard 不做字段裁剪。DEMO 敏感字段裁剪边界已在 O-002 关闭。
