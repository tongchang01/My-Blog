# 认证接口契约

> 本文档是前后端对接认证接口的事实源。
> 当前开放后台登录、refresh token 轮换、全端退出、当前用户资料查询 / 编辑和修改本人密码。

## 1. 通用约定

- 响应统一使用 `code/msg/data`。
- 前端业务逻辑只判断 `code`，不得依赖中文 `msg`。
- access token 使用 `Authorization: Bearer <token>`。
- refresh token 只通过 JSON 请求体传输，不使用 Cookie。
- token 原文不得写入日志。

## 2. 后台登录

```http
POST /api/auth/login
Content-Type: application/json
```

该接口无需 access token，匿名白名单按 `POST + /api/auth/login` 精确匹配。

```json
{
  "username": "admin",
  "password": "correct-password"
}
```

| 字段 | 类型 | 必填 | 限制 | 说明 |
|---|---|---|---|---|
| `username` | string | 是 | 非空，最长 64 字符 | trim 后使用 `Locale.ROOT` 转为小写 |
| `password` | string | 是 | 非空，最长 128 字符 | 原样参与 BCrypt |

成功时返回 HTTP 200：

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

ADMIN 和 DEMO 均可登录，GUEST 不允许登录后台。

| 场景 | HTTP | code |
|---|---:|---|
| 请求字段为空、超长或 JSON 非法 | 400 | `90001` |
| 未知账号、GUEST、密码错误、账号仍在锁定期 | 401 | `10001` |
| 同一 IP + 规范化用户名命中冷却 | 429 | `90002` |
| 未预期的审计、持久化或签发异常 | 500 | `99999` |

## 3. 刷新认证会话

```http
POST /api/auth/refresh
Content-Type: application/json
```

该接口无需 access token，匿名白名单只开放 POST。

```json
{
  "refreshToken": "<opaque-random-token>"
}
```

成功响应与登录完全一致，返回一对新的 access token 和 refresh token。旧 refresh token 在同一事务内撤销，不能再次使用。

以下场景统一返回 HTTP 401、业务码 `10002`：

- refresh token 不存在、过期或已撤销
- 同一旧 refresh token 被串行或并发重放
- 所属账号已删除或仍在锁定期
- 所属账号类型为 GUEST

```json
{
  "code": "10002",
  "msg": "登录状态已失效",
  "data": null
}
```

账号 username、type 和 `token_version` 会在刷新时重新读取，新的 access token 不沿用旧 JWT 中的账号快照。

## 4. 全端退出

```http
POST /api/auth/logout
Authorization: Bearer <access-token>
```

该接口不接收请求体中的 `userId` 或 refresh token。后端只使用当前认证主体 ID。

成功时返回 HTTP 200：

```json
{
  "code": "00000",
  "msg": "success",
  "data": null
}
```

退出成功后：

- 当前账号 `token_version + 1`，此前签发的全部 access token 立即失效
- 当前账号全部未撤销 refresh token 被撤销
- 其他账号的认证会话不受影响

缺少、无效或已失效的 access token 返回 HTTP 401、业务码 `10002`。

## 5. 查询当前用户资料

```http
GET /api/auth/me
Authorization: Bearer <access-token>
```

ADMIN 和 DEMO 均可查询自己的账号与资料。接口不接受客户端传入的用户 ID。

成功时返回 HTTP 200：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "id": 1001,
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

响应不会返回密码摘要、`tokenVersion`、登录失败次数、锁定时间或 refresh token。

## 6. 编辑当前用户资料

```http
PATCH /api/auth/me/profile
Authorization: Bearer <access-token>
Content-Type: application/json
```

仅 ADMIN 可调用。DEMO 调用返回 `403 + 10003`。

请求示例：

```json
{
  "nickname": "TYB",
  "bioZh": "新的中文简介",
  "twitterUrl": null
}
```

PATCH 字段语义：

| 请求状态 | 行为 |
|---|---|
| 字段未出现 | 保持原值 |
| 字段出现且有值 | 规范化并更新 |
| 可选字段为 `null` 或空白字符串 | 清空为数据库 `NULL` |

`nickname` 未出现时保持原值；出现时 trim 后必须非空。空请求体对象和未知字段均返回参数错误。

| 字段 | 限制 |
|---|---|
| `nickname` | 必填，最长 64 字符 |
| `avatarUrl` / `website` / 各社交链接 | 可空，最长 255 字符，仅 HTTP / HTTPS |
| `bioZh` / `bioJa` / `bioEn` | 可空，各最长 5000 字符 |
| `location` | 可空，最长 64 字符 |
| `emailPublic` | 可空，最长 128 字符，必须为邮箱格式 |

成功后返回更新后的完整 profile 对象。

| 场景 | HTTP | code |
|---|---:|---|
| access token 缺失或失效 | 401 | `10002` |
| DEMO 尝试编辑 | 403 | `10003` |
| 字段非法、空 PATCH、未知字段或 JSON 非法 | 400 | `90001` |
| 账号 / 资料数据不完整或更新异常 | 500 | `99999` |

## 7. 修改当前用户密码

```http
PUT /api/auth/me/password
Authorization: Bearer <access-token>
Content-Type: application/json
```

仅 ADMIN 可调用。DEMO 调用返回 `403 + 10003`。接口不接受客户端传入的用户 ID。

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

当前密码和新密码均按原样参与 BCrypt，不做 trim。当前密码必填且最长 128 字符；新密码长度为 8 至 128 字符，并且不能与当前密码相同。

成功时返回 HTTP 200：

```json
{
  "code": "00000",
  "msg": "success",
  "data": null
}
```

成功后当前账号的 `token_version` 递增，全部 refresh token 被撤销，历史 access token 和 refresh token 均失效。接口不签发新 token；客户端应清理本地 token 并重新登录。

| 场景 | HTTP | code |
|---|---:|---|
| 请求体为空、字段缺失、长度非法、新旧密码相同或 JSON 非法 | 400 | `90001` |
| 当前密码错误 | 401 | `10001` |
| access token 缺失、失效或主体 ID 非法 | 401 | `10002` |
| DEMO 尝试修改密码 | 403 | `10003` |
| 账号并发删除、更新行数异常或持久化失败 | 500 | `99999` |

## 8. 权限矩阵

| 操作 | ADMIN | DEMO |
|---|---|---|
| 登录、refresh、全端退出 | 允许 | 允许 |
| 查询本人账号与资料 | 允许 | 允许 |
| 编辑本人资料 | 允许 | 禁止 |
| 修改本人密码 | 允许 | 禁止 |

## 9. 限流边界

- 登录限流键为可信客户端 IP + 规范化用户名。
- 连续第 1 至第 5 次坏凭据返回 `401 + 10001`。
- 第 6 次请求在账号查询和 BCrypt 前返回 `429 + 90002`。
- 凭据验证成功后清除当前限流键。
- refresh 和 logout 本轮不新增独立限流器。

## 10. 尚未开放

- 单设备会话管理
- Cookie 模式 token
