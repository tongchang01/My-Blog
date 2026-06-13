# 认证接口契约

> 本文档是前后端对接认证接口的事实源。
> 当前开放后台登录、refresh token 轮换和全端退出；当前用户资料接口尚未开放。

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

账号 username、role 和 `token_version` 会在刷新时重新读取，新的 access token 不沿用旧 JWT 中的账号快照。

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

## 5. 限流边界

- 登录限流键为可信客户端 IP + 规范化用户名。
- 连续第 1 至第 5 次坏凭据返回 `401 + 10001`。
- 第 6 次请求在账号查询和 BCrypt 前返回 `429 + 90002`。
- 凭据验证成功后清除当前限流键。
- refresh 和 logout 本轮不新增独立限流器。

## 6. 尚未开放

- 当前用户资料查询
- 修改密码
- 单设备会话管理
- Cookie 模式 token
