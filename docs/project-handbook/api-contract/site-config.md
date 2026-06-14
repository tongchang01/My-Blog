# 站点配置接口契约

> 适用模块：`system / t_site_config`
> 实施日期：2026-06-14

## 1. 接口

| Method | Path | 权限 | 用途 |
|---|---|---|---|
| GET | `/api/public/site-config?lang=zh\|ja\|en` | 匿名 | 查询当前语言公开配置 |
| GET | `/api/admin/site-config` | ADMIN、DEMO | 查询完整三语配置 |
| PUT | `/api/admin/site-config` | ADMIN | 全量更新完整配置 |

DEMO 只能读取，更新返回 `403 + 10003`。匿名访问后台接口返回
`401 + 10002`。

## 2. 公开读取

`lang` 必填且区分大小写，只接受 `zh`、`ja`、`en`。日语和英语字段为空时，
按字段独立回退中文；中文可选字段也为空时返回 `null`。

公开响应只包含：

```json
{
  "siteTitle": "MyBlog",
  "siteSubtitle": null,
  "aboutMd": null,
  "logoUrl": null,
  "faviconUrl": null,
  "icpNo": null,
  "spotifyPlaylistId": null
}
```

不返回三语副本、ID、审计列和删除列。

## 3. 后台读取与更新

后台成功响应包含 13 个业务字段，以及 `updatedAt`、`updatedBy`：

```json
{
  "siteTitleZh": "MyBlog",
  "siteTitleJa": null,
  "siteTitleEn": null,
  "siteSubtitleZh": null,
  "siteSubtitleJa": null,
  "siteSubtitleEn": null,
  "aboutMdZh": null,
  "aboutMdJa": null,
  "aboutMdEn": null,
  "logoUrl": null,
  "faviconUrl": null,
  "icpNo": null,
  "spotifyPlaylistId": null,
  "updatedAt": "2026-06-14T12:00:00",
  "updatedBy": 1001
}
```

PUT 请求中的 13 个业务字段必须全部出现。除 `siteTitleZh` 外，其余字段允许
显式 `null` 或空白字符串，二者都会清空为数据库 `NULL`。字段缺失或未知字段
返回 `400 + 90001`。

## 4. 字段规则

| 字段 | 规则 |
|---|---|
| `siteTitleZh` | trim 后必填，最长 128 |
| `siteTitleJa`、`siteTitleEn` | trim，可空，最长 128 |
| 三语 `siteSubtitle` | trim，可空，最长 255 |
| 三语 `aboutMd` | 可空，最长 50,000；保留 Markdown 原文，纯空白清空 |
| `logoUrl`、`faviconUrl` | trim，可空，最长 255；只接受绝对 HTTP/HTTPS URL |
| `icpNo` | trim，可空，最长 64 |
| `spotifyPlaylistId` | trim，可空，最长 64；只接受字母、数字、下划线、连字符 |

后端返回 Markdown 原文，不渲染 HTML。前端必须使用禁用原始 HTML 的安全
Markdown 渲染管线。

## 5. 错误

| 场景 | HTTP | code |
|---|---:|---|
| lang 缺失或非法 | 400 | `90001` |
| PUT 字段、长度、URL、Markdown 或 Spotify ID 非法 | 400 | `90001` |
| access token 缺失或失效 | 401 | `10002` |
| DEMO 更新 | 403 | `10003` |
| 固定配置行缺失、已删除或更新异常 | 500 | `99999` |

`t_site_config.id=1` 是迁移建立的固定数据。运行时不会自动补建，缺失时按内部
数据损坏处理。

## 6. 验收

2026-06-14 全量 `mvn clean test`：329 tests，0 failures，0 errors，
4 skipped。跳过项均为 Docker 不可用时的 Testcontainers MySQL 条件测试；
站点配置 H2 事务、行锁并发、真实 JWT HTTP 流程和 OpenAPI 契约测试均通过。
