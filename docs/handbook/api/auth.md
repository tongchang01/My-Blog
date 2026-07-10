# 认证接口契约

> 状态：当前有效
> 适用范围：V2 后端认证接口、后台 admin 会话接入
> 最后校准：2026-07-09
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/`
> 权威程度：API 契约

## 本文档回答什么问题

本文档记录后台登录、refresh token 轮换、全端退出、当前用户查询、公开作者资料、当前用户资料更新和修改密码接口的前后端契约。

## 1. 通用约定

- 响应统一使用 `ApiResponse<T>`：`code/msg/data`。
- access token 使用 `Authorization: Bearer <token>`。
- refresh token 只通过 JSON 请求体传输，不使用 Cookie。
- token 原文不得写入日志、URL 或第三方统计。
- 后台当前会话存储在前端 localStorage，安全升级争议见 O-008。
- ADMIN 和 DEMO 均可登录后台；GUEST 不允许登录后台。

## 2. 后台登录

```http
POST /api/auth/login
Content-Type: application/json
```

鉴权：匿名。白名单按 `POST + /api/auth/login` 精确匹配。

请求体：

```json
{
  "username": "admin",
  "password": "correct-password"
}
```

| 字段 | 类型 | 必填 | 限制 | 说明 |
|------|------|------|------|------|
| `username` | string | 是 | 非空，最长 64 字符 | 后端 trim 后按 `Locale.ROOT` 小写 |
| `password` | string | 是 | 非空，最长 128 字符 | 原样参与 BCrypt 校验 |

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<opaque-random-token>",
    "accessExpiresIn": 900,
    "refreshExpiresIn": 604800
  }
}
```

错误：

| 场景 | HTTP | code |
|------|------|------|
| 字段为空、超长或 JSON 非法 | 400 | `90001` |
| 未知账号、GUEST、密码错误、账号锁定 | 401 | `10001` |
| 同一 IP + username 登录失败次数触发冷却 | 429 | `90002` |
| 审计、持久化或 token 签发未预期异常 | 500 | `99999` |

## 3. 刷新认证会话

```http
POST /api/auth/refresh
Content-Type: application/json
```

鉴权：匿名。白名单按 `POST + /api/auth/refresh` 精确匹配。

请求体：

```json
{
  "refreshToken": "<opaque-random-token>"
}
```

| 字段 | 类型 | 必填 | 限制 | 说明 |
|------|------|------|------|------|
| `refreshToken` | string | 是 | 非空 | refresh token 明文，仅客户端持有 |

成功响应与登录一致，返回一对新的 access token 和 refresh token。旧 refresh token 会在同一事务内撤销，不能再次使用。

以下场景统一返回 HTTP 401、`10002`：

- refresh token 不存在。
- refresh token 过期。
- refresh token 已撤销。
- 同一旧 refresh token 被重放。
- 所属账号已删除、锁定或不可用。
- 所属账号类型不允许后台登录。

## 4. 全端退出

```http
POST /api/auth/logout
Authorization: Bearer <access-token>
```

鉴权：需要有效 access token。

请求体：无。接口不接收客户端传入的 `userId` 或 refresh token。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": null
}
```

成功后：

- 当前账号 `token_version + 1`。
- 当前账号全部未撤销 refresh token 被撤销。
- 此前签发的 access token 立即失效。
- 其它账号会话不受影响。

错误：

| 场景 | HTTP | code |
|------|------|------|
| access token 缺失、无效或已失效 | 401 | `10002` |

## 5. 查询当前用户

```http
GET /api/auth/me
Authorization: Bearer <access-token>
```

鉴权：ADMIN、DEMO。

请求参数：无。接口不接受客户端传入用户 ID。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "id": "9007199254740993",
    "username": "admin",
    "type": "ADMIN",
    "profile": {
      "nickname": "TYB",
      "avatarUrl": null,
      "bioZh": "中文简介",
      "bioJa": null,
      "bioEn": null,
      "location": "Tokyo",
      "website": "https://example.com",
      "emailPublic": null,
      "githubUrl": null,
      "twitterUrl": null,
      "linkedinUrl": null,
      "zhihuUrl": null,
      "qiitaUrl": null,
      "juejinUrl": null
    }
  }
}
```

字段规则：

- `id` 固定为十进制字符串，避免浏览器端 Snowflake ID 精度损失。
- `type` 当前为 `ADMIN` 或 `DEMO`。
- 响应不返回密码摘要、`tokenVersion`、登录失败次数、锁定时间或 refresh token。

错误：

| 场景 | HTTP | code |
|------|------|------|
| access token 缺失、无效或已失效 | 401 | `10002` |
| 当前账号或资料数据异常 | 500 | `99999` |

## 6. 查询公开作者资料

```http
GET /api/public/author-profile
```

鉴权：匿名。白名单按 `GET + /api/public/author-profile` 精确匹配。

请求参数：无。

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "nickname": "TYB",
    "avatarUrl": null,
    "bioZh": "中文简介",
    "bioJa": null,
    "bioEn": null,
    "location": "Tokyo",
    "website": "https://example.com",
    "emailPublic": null,
    "githubUrl": "https://github.com/tyb",
    "twitterUrl": null,
    "linkedinUrl": null,
    "zhihuUrl": null,
    "qiitaUrl": null,
    "juejinUrl": null
  }
}
```

公开响应不返回 `userId`、账号名、账号类型、密码摘要、token 版本、登录安全字段、审计列或删除列。

主作者选择规则：

- 后端从已有公开文章中选择作者，不接受客户端传入用户 ID。
- 只统计 `status = PUBLISHED`、未删除、且 `publishAt <= 当前时间` 的文章。
- 优先选择公开文章数最多的作者；文章数相同则选择最近发布时间更晚者；仍相同则按用户 ID 升序。

错误：

| 场景 | HTTP | code |
|------|------|------|
| 当前没有可见公开作者资料 | 404 | `90003` |

## 7. 更新当前用户资料

```http
PATCH /api/auth/me/profile
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。DEMO 返回 `403 + 10003`。

请求体为部分更新对象。字段未出现表示保持原值；字段出现且为 `null` 或空白字符串时，可选字段清空为 `null`。

请求示例：

```json
{
  "nickname": "TYB",
  "bioZh": "新的中文简介",
  "twitterUrl": null
}
```

可提交字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `nickname` | string/null | 昵称。出现时 trim 后必须非空 |
| `avatarUrl` | string/null | 头像 URL |
| `bioZh` | string/null | 中文简介 |
| `bioJa` | string/null | 日文简介 |
| `bioEn` | string/null | 英文简介 |
| `location` | string/null | 所在地 |
| `website` | string/null | 个人主页 |
| `emailPublic` | string/null | 公开邮箱 |
| `githubUrl` | string/null | GitHub URL |
| `twitterUrl` | string/null | Twitter URL |
| `linkedinUrl` | string/null | LinkedIn URL |
| `zhihuUrl` | string/null | 知乎 URL |
| `qiitaUrl` | string/null | Qiita URL |
| `juejinUrl` | string/null | 掘金 URL |

成功响应：HTTP 200，`data` 为更新后的完整 `profile` 对象。

错误：

| 场景 | HTTP | code |
|------|------|------|
| access token 缺失、无效或已失效 | 401 | `10002` |
| DEMO 尝试更新 | 403 | `10003` |
| 空 PATCH、字段非法、未知字段或 JSON 非法 | 400 | `90001` |
| 账号资料缺失或持久化异常 | 500 | `99999` |

## 8. 修改当前用户密码

```http
PUT /api/auth/me/password
Authorization: Bearer <access-token>
Content-Type: application/json
```

鉴权：仅 ADMIN。DEMO 返回 `403 + 10003`。

请求体：

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

| 字段 | 类型 | 必填 | 限制 | 说明 |
|------|------|------|------|------|
| `currentPassword` | string | 是 | 非空，最长 128 字符 | 原样参与 BCrypt，不 trim |
| `newPassword` | string | 是 | 8 至 128 字符 | 原样参与 BCrypt，不 trim，不能与旧密码相同 |

成功响应：HTTP 200

```json
{
  "code": "00000",
  "msg": "success",
  "data": null
}
```

成功后：

- 当前账号密码摘要更新。
- 当前账号 `token_version + 1`。
- 当前账号全部 refresh token 被撤销。
- 历史 access token 和 refresh token 均失效。
- 接口不签发新 token，客户端应清理本地会话并重新登录。

错误：

| 场景 | HTTP | code |
|------|------|------|
| 请求体为空、字段缺失、长度非法、JSON 非法 | 400 | `90001` |
| 新旧密码相同 | 400 | `90001` |
| 当前密码错误 | 401 | `10001` |
| access token 缺失、无效或主体 ID 非法 | 401 | `10002` |
| DEMO 尝试修改密码 | 403 | `10003` |
| 账号并发删除、更新行数异常或持久化失败 | 500 | `99999` |

## 9. 权限矩阵

| 操作 | 匿名 | ADMIN | DEMO |
|------|------|-------|------|
| 登录 | 允许 | 允许 | 允许 |
| refresh | 允许 | 允许 | 允许 |
| logout | 禁止 | 允许 | 允许 |
| 查询当前用户 | 禁止 | 允许 | 允许 |
| 查询公开作者资料 | 允许 | 允许 | 允许 |
| 更新当前用户资料 | 禁止 | 允许 | 禁止 |
| 修改当前用户密码 | 禁止 | 允许 | 禁止 |

## 10. 限流边界

登录限流：

- key：可信客户端 IP + 规范化 username。
- 第 1 至第 5 次坏凭据返回 `401 + 10001`。
- 第 6 次命中冷却返回 `429 + 90002`。
- 凭据验证成功后清除当前限流 key。

refresh、logout、me、公开作者资料、profile、password 当前不单独增加接口级限流。

## 11. 尚未开放

- Cookie 模式 token。
- 单设备会话管理。
- 管理员强制下线其它账号。

相关未完成或争议事项见 O-008。
