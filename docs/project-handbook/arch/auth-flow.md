# 认证流程

> 本文档回答："登录怎么走？JWT 怎么签发与校验？审计在哪？"
> 适用范围：V2 identity 模块 + common 安全组件。
> 相关 ADR：ADR-0007（JWT 改用 spring-security-oauth2-jose）
> 相关规则：`../rules/security-baseline.md`

## 1. 总览

```
[客户端] ──POST /api/auth/login──► [AuthController]
                                        │
                                        ▼
                            [AuthApplicationService]
                                        │
                  ┌─────────────────────┼─────────────────────┐
                  ▼                     ▼                     ▼
        [UserAuthRepository]   [PasswordEncoder]    [JwtTokenService]
        (校验用户存在 + 密码)   (BCrypt 校验)         (签发 JWT)
                                        │
                                        ▼
                            [LoginAuditService] (更新 last_login_time / ip)
                                        │
                                        ▼
                                  返回 ApiResponse<TokenResponse>
```

## 2. 登录流程详解

1. **入口**：`POST /api/auth/login`，Body：`{ username, password }`
2. **校验账号**：从 `t_user_auth` 查用户，不存在 → `AUTH_INVALID_CREDENTIALS`（401）
3. **校验状态**：账号被禁用 → `AUTH_USER_DISABLED`（403）
4. **校验密码**：BCrypt 比对，不匹配 → `AUTH_INVALID_CREDENTIALS`（401）
5. **签发 JWT**：调用 `JwtTokenService.issue(...)`，含 `subject / username / roles / jti / exp`
6. **写审计**：更新 `last_login_time`、`ip_address`（客户端 IP 提取顺序见 `security-baseline.md` §7）
7. **审计失败 → 不签发**：若审计 SQL 失败，登录返回 500，**不**返回 token（防止系统状态不一致）
8. **返回**：`{ token, expiresAt, ... }`

登录失败（不存在/密码错/禁用）**不**更新审计字段。

## 3. JWT 结构

```
header  : { alg: HS256, typ: JWT }
payload : {
  sub:      "<userId>",
  username: "<username>",
  roles:    ["USER" | "ADMIN" | ...],
  jti:      "<uuid>",
  exp:      <epoch seconds>,
  iat:      <epoch seconds>
}
```

- 完全自包含，校验时不依赖外部存储（撤销除外）
- 签名密钥：`MYBLOG_JWT_SECRET` 环境变量，启动时由 `JwtSecretStartupValidator` 校验

## 4. 校验流程（每个受保护请求）

```
[请求] ──► [JwtAuthenticationFilter]
              │
              ├─ 1. 从 Authorization: Bearer <token> 提取
              ├─ 2. JwtTokenService.parse(token)
              │     - 签名错 / 过期 → 401
              ├─ 3. 检查 TokenRevocationStore.isRevoked(jti)
              │     - 已撤销 → 401
              ├─ 4. 构建 Authentication，注入 SecurityContext
              ▼
       [继续 Spring Security 链] ──► [Controller]
```

## 5. 登出流程

```
POST /api/auth/logout
   │
   ▼
解析当前 token 的 jti
   │
   ▼
JwtTokenService.revoke(jti) ──► TokenRevocationStore (当前为内存实现)
```

⚠️ 当前撤销 store 是内存实现：
- 服务重启 → 已撤销的 token 重新生效
- 多实例部署 → 撤销不会跨实例同步

后续必须迁到 Redis（见 `pitfalls.md`）。

## 6. 安全白名单

配置：`application.yml` → `myblog.security.public-endpoints`，必须以 `HTTP method + path` 双维度声明。

例：
```yaml
myblog:
  security:
    public-endpoints:
      - method: POST
        path: /api/auth/login
      - method: GET
        path: /api/comments/**
```

## 7. 客户端 IP 提取

按优先级：
1. `X-Forwarded-For`（首个非空段）
2. `X-Real-IP`
3. `request.getRemoteAddr()`

空白返回 `null`，不写入伪造值。`ip_source`（归属地）当前暂不解析。

## 8. 关键代码路径

- `JwtTokenService` — Token 签发/解析/撤销
- `JwtAuthenticationFilter` — 请求过滤
- `JwtSecretStartupValidator` — 启动校验密钥
- `TokenRevocationStore` — 撤销存储（内存）
- `AuthApplicationService` — 登录用例编排
- `LoginAuditService` / `t_user_auth` — 审计

## 9. 测试覆盖

- `JwtSecretStartupValidatorTest` — 空密钥启动失败
- `JwtTokenServiceTest` — 签发/解析/撤销全路径
- `JwtAuthenticationFilterTest` — 合法/过期/撤销/缺失
- `SecurityConfigTest` — 白名单匿名、保护接口必鉴权
- `AuthControllerTest` — 登录成功审计字段更新；登录失败不动

## 10. 未实现项

详见 `../rules/security-baseline.md` §10。
