# 认证接口契约

> 本文档是前后端对接认证接口的事实源。
> 当前仅开放后台登录；refresh、logout 和当前用户接口尚未开放。

## 1. 后台登录

### 请求

```http
POST /api/auth/login
Content-Type: application/json
```

无需 access token。安全白名单按 `POST + /api/auth/login` 精确匹配，其他 `/api/auth/**` 方法和路径不会自动公开。

```json
{
  "username": "admin",
  "password": "correct-password"
}
```

| 字段 | 类型 | 必填 | 限制 | 说明 |
|---|---|---|---|---|
| `username` | string | 是 | 非空，最长 64 字符 | 后端执行 trim，并使用 `Locale.ROOT` 转为小写 |
| `password` | string | 是 | 非空，最长 128 字符 | 原样参与 BCrypt，不 trim、不改变大小写 |

### 成功响应

HTTP 200：

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

| 字段 | 类型 | 说明 |
|---|---|---|
| `accessToken` | string | JWT access token，包含 user id、username、role 和 token version |
| `refreshToken` | string | refresh token 明文，仅本次响应返回；数据库只保存 SHA-256 |
| `accessExpiresIn` | number | access token TTL，单位秒 |
| `refreshExpiresIn` | number | refresh token TTL，单位秒 |

ADMIN 和 DEMO 均可登录，access token 的角色分别为 `ADMIN`、`DEMO`。GUEST 不允许登录后台。

### 失败响应

| 场景 | HTTP | code | 对外语义 |
|---|---:|---|---|
| 请求字段为空、超长或 JSON 非法 | 400 | `90001` | 参数校验失败 |
| 未知账号、GUEST、密码错误、账号仍在锁定期 | 401 | `10001` | 用户名或密码错误 |
| 同一 IP + 规范化用户名命中冷却 | 429 | `90002` | 请求过于频繁 |
| 未预期的审计、持久化或签发异常 | 500 | `99999` | 系统内部错误 |

错误响应固定使用：

```json
{
  "code": "10001",
  "msg": "用户名或密码错误",
  "data": null
}
```

### 限流边界

- 限流键为可信客户端 IP + 规范化用户名。
- 连续第 1 至第 5 次坏凭据返回 `401 + 10001`。
- 第 6 次请求在账号查询和 BCrypt 前返回 `429 + 90002`。
- 凭据验证成功后清除当前限流键，下一次失败从新周期开始。
- 账号锁定状态不对外暴露，不能根据响应判断账号是否存在。

## 2. 尚未开放的接口

以下能力有部分应用层或持久化基础，但没有 Controller，不属于当前 HTTP 契约：

- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- 当前用户资料查询

前端不得提前调用或假设其请求、响应字段。接口实施时先更新本文档，再编写代码。
