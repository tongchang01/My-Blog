# 访问统计接口契约

> 本文档是公开访问打点与后台统计总览的 HTTP 单一事实源。所有日期均按 Asia/Tokyo（JST）解释。

## 1. 公开页面访问打点

### `POST /api/public/stats/page-views`

- 鉴权：匿名可访问。
- `Content-Type`：`application/json`。
- 请求体只包含：
  - `articleId`：可选正整数。文章页面传文章 ID；非文章页面省略。
  - `lang`：必填，只允许 `zh`、`ja`、`en`。
- 服务端读取 `User-Agent`、`Referer` 和可信客户端 IP；客户端不能在 JSON 中提交这些字段。
- 成功响应：HTTP 200，`{"code":"00000","msg":"success","data":null}`。
- `lang` 非法或缺失：HTTP 400 + `90001`。
- 文章不存在、已删除，或状态不是 `PUBLISHED` / `PASSWORD`：HTTP 404 + `90003`。
- 同一 IP 超过进程内限流阈值：HTTP 429 + `90002`。

`PUBLISHED` 和 `PASSWORD` 文章可以打点；`PRIVATE`、`DRAFT`、`SCHEDULED`、已删除和不存在的文章不写入明细。非文章页面在明细表保存 `article_id=NULL`，日聚合统一为 `articleId=0`。

## 2. 统计口径与隐私

- 每次通过校验的打点增加一次 PV。
- 日 UV 在同一 `articleId + lang + JST 日期` 内按 `visitorHash` 去重。
- `visitorHash` 使用日期、客户端 IP、User-Agent 和服务端密钥生成 HMAC-SHA256；日期每日轮换。
- 数据库不保存原始 IP、原始 User-Agent 或 hash secret；只保存 hash、可选 referrer 和访问时间。
- 日聚合每 5 分钟幂等重算；应用启动时补算今天和昨天；90 天前的访问明细会被清理。
- 每日 hash 会轮换，因此 API 不提供跨日期独立访客数。

## 3. 后台访问统计总览

### `GET /api/admin/stats/dashboard`

- 鉴权：`ADMIN`、`DEMO` 可读；`GUEST` 返回 403；匿名返回 401。其他 HTTP 方法不向 DEMO 开放。
- 查询参数：`from`、`to` 必须同时出现或同时省略，格式为 `yyyy-MM-dd`。
- 默认区间：包含今天在内的最近 30 个 JST 自然日。
- 自定义区间：包含首尾，最多 366 天；开始晚于结束或只传一个参数返回 HTTP 400 + `90001`。
- 即使自定义区间不包含今天，`todayPv`、`todayUv` 仍单独查询 JST 今天。

成功响应 `data`：

| 字段 | 含义 |
|---|---|
| `periodPv` | 查询区间内总 PV |
| `todayPv` | JST 今天 PV |
| `todayUv` | JST 今天日 UV |
| `averageDailyUv` | 区间内各日 UV 之和除以完整区间天数，保留 1 位小数 |
| `trend[]` | 从 `from` 到 `to` 的连续日期、PV、日 UV；缺失日期补零 |
| `topArticles[]` | TOP 10，按 PV 降序、articleId 升序稳定排序 |
| `languageDistribution[]` | 各语言 PV 及其占区间 PV 的比例，保留 4 位小数 |

`topArticles[]` 字段为 `articleId`、`title`、`pv`、`dailyUvSum`。`dailyUvSum` 是区间内各日 UV 的算术和，不是跨日独立访客数；文章不存在时 `title=null`，统计行仍保留。`averageDailyUv` 同样基于日 UV，不表达跨日去重人数。
