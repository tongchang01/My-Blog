# 站点配置接口契约

> 状态：当前有效
> 适用范围：V2 后端 system 模块、前台 blog、后台 admin
> 最后校准：2026-07-05
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/*SiteConfig*`
> 权威程度：API 契约

## 本文档回答什么问题

本文档记录公开站点配置读取、后台完整站点配置读取和后台全量更新接口的契约。

## 1. 接口清单

| Method | Path | 权限 | 用途 |
|--------|------|------|------|
| GET | `/api/public/site-config?lang=zh` | 匿名 | 查询当前语言公开站点配置 |
| GET | `/api/admin/site-config` | ADMIN / DEMO | 查询完整三语站点配置 |
| PUT | `/api/admin/site-config` | ADMIN | 全量更新完整站点配置 |

DEMO 只能读取，更新返回 `403 + 10003`。

## 2. 查询公开站点配置

```http
GET /api/public/site-config?lang=zh
```

鉴权：匿名。

Query：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `lang` | string | 是 | 只接受 `zh`、`ja`、`en` |

当前 Controller 允许缺省进入服务层，但服务层会把缺失 `lang` 视为参数错误。因此客户端应始终显式传入 `lang`。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "siteTitle": "MyBlog",
    "siteSubtitle": null,
    "aboutMd": null,
    "logoUrl": null,
    "faviconUrl": null,
    "icpNo": null,
    "spotifyPlaylistId": null,
    "startedDate": null
  }
}
```

公开响应不包含三语副本、ID、审计列、删除列或后台内部配置。

语言回退：

- 查询 `ja` 或 `en` 时，缺失翻译按字段独立回退中文。
- 中文字段也为空时返回 `null`。
- `aboutMd` 返回 Markdown 原文，不返回 HTML。

错误：

| 场景 | HTTP | code |
|------|------|------|
| `lang` 缺失或非法 | 400 | `90001` |
| 固定配置行缺失或已删除 | 500 | `99999` |

## 3. 查询后台完整站点配置

```http
GET /api/admin/site-config
Authorization: Bearer <access-token>
```

鉴权：ADMIN / DEMO。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
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
    "startedDate": null,
    "updatedAt": "2026-06-14T12:00:00",
    "updatedBy": 1001
  }
}
```

注意：当前 `updatedBy` 在 `AdminSiteConfigVO` 中是 number 或 `null`，不是 string。

错误：

| 场景 | HTTP | code |
|------|------|------|
| access token 缺失或失效 | 401 | `10002` |
| 固定配置行缺失或已删除 | 500 | `99999` |

## 4. 全量更新后台站点配置

```http
PUT /api/admin/site-config
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。

请求体必须提交 14 个业务字段：

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
  "startedDate": null
}
```

字段规则：

| 字段 | 规则 |
|------|------|
| `siteTitleZh` | trim 后必填，最长 128 |
| `siteTitleJa` / `siteTitleEn` | trim，可空，最长 128 |
| `siteSubtitleZh` / `siteSubtitleJa` / `siteSubtitleEn` | trim，可空，最长 255 |
| `aboutMdZh` / `aboutMdJa` / `aboutMdEn` | 可空，最长 50000，保留 Markdown 原文 |
| `logoUrl` / `faviconUrl` | trim，可空，最长 255，只接受 HTTP/HTTPS 绝对 URL |
| `icpNo` | trim，可空，最长 64 |
| `spotifyPlaylistId` | trim，可空，最长 64，只接受字母、数字、下划线、连字符 |
| `startedDate` | 可空，格式 `yyyy-MM-dd`，用于前台本地计算建站天数 |

成功响应：HTTP 200，`data` 为更新后的完整后台站点配置。

错误：

| 场景 | HTTP | code |
|------|------|------|
| access token 缺失或失效 | 401 | `10002` |
| DEMO 更新 | 403 | `10003` |
| 字段缺失、未知字段、长度非法、URL 非法、JSON 非法 | 400 | `90001` |
| 固定配置行缺失、已删除或更新异常 | 500 | `99999` |

## 5. 安全与渲染边界

- 后端只保存和返回 Markdown 原文。
- 前台和后台预览必须使用安全 Markdown 渲染管线。
- 不允许把 `aboutMd` 原文当作可信 HTML 直接插入页面。
- Logo、Favicon 当前保存 URL 字符串；后台可通过附件选择器把附件 `publicUrl` 写入这些字段。
