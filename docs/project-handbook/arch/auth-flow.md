# 认证流程

> 本文档回答："登录怎么走？JWT 怎么签发与校验？审计在哪？PASSWORD 文章怎么解锁？"
> 适用范围：V2 identity 模块 + common-infra 安全组件。
> 相关：`product/decisions-draft.md` R1 / R6 C1 / R7 D6、`../decisions/0007-jwt-via-spring-security-jose.md`、`../rules/security-baseline.md`

## 1. 总览（双 Token 机制）

V2 采用 **access token（无状态 JWT）+ refresh token（DB 存储）** 双 token：

```
[客户端] ──POST /api/auth/login──► [AuthController]
                                        │
                                        ▼
                            [AuthApplicationService]
                                        │
                  ┌─────────────────────┼─────────────────────┐
                  ▼                     ▼                     ▼
        [UserAuthRepository]   [PasswordEncoder]    [JwtTokenService]
        (校验用户存在 + 密码)   (BCrypt 校验)         (签发 access)
                                        │                     │
                                        ▼                     ▼
                            [LoginAuditService]   [RefreshTokenService]
                            (更新 last_login_at)     (写 t_refresh_token)
                                        │
                                        ▼
                       返回 ApiResponse<TokenResponse>
                       { accessToken, refreshToken, expiresIn }
```

## 2. 登录流程详解

1. **入口**：`POST /api/auth/login`，Body：`{ username, password }`
2. **限流前置**：同 IP + 同 username 在 10 分钟窗口内失败 5 次 → 429 `90002`
3. **校验账号**：从 `t_user_auth` 查用户，不存在 → `10001` 用户名或密码错误（401）
4. **校验状态**：账号被禁用 / `locked_until > now` → 403
5. **校验密码**：BCrypt 比对，不匹配 → `login_fail_count += 1`，返 `10001`（401）
6. **签发 access token**：含 `sub / exp / iat / iss / ver / typ="access"`
7. **签发 refresh token**：随机字符串，SHA-256 哈希后写 `t_refresh_token`（user_id / token_hash / expires_at）
8. **写审计**：更新 `last_login_at`、`last_login_ip`，重置 `login_fail_count = 0`
9. **审计失败 → 不签发**：若审计 SQL 失败，登录返回 500 (`99999`)，**不**返回 token（防止系统状态不一致）
10. **返回**：`{ accessToken, refreshToken, accessExpiresIn: 900, refreshExpiresIn: 604800 }`

登录失败（不存在/密码错/禁用）**不**更新 `last_login_at` / `last_login_ip` 审计字段。

## 3. Token 结构

### Access Token（JWT）

```
header  : { alg: HS256, typ: JWT }
payload : {
  sub:  "<user_id>",
  iss:  "myblog-v2",
  exp:  <epoch seconds>,
  iat:  <epoch seconds>,
  ver:  <int>,             // 对应 t_user_auth.token_version
  typ:  "access"           // 与 article_access 强隔离
}
```

- TTL：15 分钟（`myblog.security.jwt.access-token-ttl`）
- 完全自包含 + `ver` 校验，**不依赖外部撤销存储**
- 签名密钥：`MYBLOG_JWT_SECRET` 环境变量，启动时由 `JwtSecretStartupValidator` 校验

### Refresh Token（不是 JWT）

- 类型：随机字符串（≥32 字节熵）
- TTL：7 天（`myblog.security.jwt.refresh-token-ttl`）
- 存储：`t_refresh_token` 表

```sql
t_refresh_token:
  id           BIGINT PK                       -- ASSIGN_ID（MyBatis-Plus 雪花），不带 AUTO_INCREMENT
  user_id      BIGINT NOT NULL
  token_hash   VARCHAR(64) NOT NULL UNIQUE  -- SHA-256(token)，不存明文
  expires_at   DATETIME NOT NULL
  revoked      TINYINT NOT NULL DEFAULT 0
  created_at / created_by / ...             -- 审计
  KEY idx_user (user_id)
```

🔴 **明文 refresh token 只返给客户端一次**，DB 只存 hash。

## 4. Access Token 校验流程（每个受保护请求）

```
[请求] ──► [JwtAuthenticationFilter]
              │
              ├─ 1. 从 Authorization: Bearer <token> 提取
              ├─ 2. JwtTokenService.parse(token)
              │     - 签名错 / 过期 → 401 (10002)
              │     - typ != "access" → 401 (10002)
              ├─ 3. 查 t_user_auth.token_version
              │     - token.ver != user.token_version → 401 (10002)
              ├─ 4. 构建 Authentication（含 ROLE_ADMIN / ROLE_DEMO），注入 SecurityContext
              ▼
       [继续 Spring Security 链] ──► [Controller]
```

**关键点**：每次校验需查一次 `t_user_auth.token_version`（轻量单行查询，未来加 Caffeine 短 TTL 缓存）。

## 5. Refresh 流程

```
POST /api/auth/refresh   Body: { refreshToken }
   │
   ├─ 1. 计算 SHA-256(refreshToken) → 查 t_refresh_token
   │     - 不存在 / revoked=1 / expires_at < now → 401 (10002)
   ├─ 2. 查 t_user_auth，校验状态正常
   ├─ 3. 签发新 access token（带最新 ver）
   ├─ 4.（可选）轮换：标记旧 refresh token revoked=1，签发新 refresh token
   ▼
   返回 { accessToken, refreshToken?, accessExpiresIn }
```

> 是否轮换 refresh token：DDL 阶段决定（默认轮换 = 更安全；不轮换 = 实现简单）。

## 6. 登出流程

```
POST /api/auth/logout   Header: Authorization: Bearer <access>
   │
   ├─ 1. 解析 access token，取 sub (user_id)
   ├─ 2. UPDATE t_user_auth SET token_version = token_version + 1 WHERE id = ?
   │     → 所有未过期 access token 立即失效
   ├─ 3. UPDATE t_refresh_token SET revoked = 1 WHERE user_id = ? AND revoked = 0
   │     → 所有 refresh token 失效
   ▼
   返回 200
```

**强制下线 / 改密** 走同一机制：递增 `token_version` + 撤销 refresh token。

## 7. PASSWORD 文章解锁流程

```
POST /api/public/articles/{id}/unlock   Body: { password }
   │
   ├─ 1. 限流：同 IP + 同 article 5 次/10 分钟冷却 → 429 (90002)
   ├─ 2. 查 t_article.access_password (BCrypt hash)
   ├─ 3. BCrypt.matches(password, hash)
   │     - 不匹配 → 401 (20002 文章密码错误)
   ├─ 4. 签发 Article Access Token（JWT）
   │     payload: { typ: "article_access", aid: <article_id>, exp: now+30min, iat, iss }
   │     （不含 sub，不能用作身份凭证）
   ▼
   返回 { articleToken, expiresIn: 1800 }
```

### Article Access Token 校验

后续访问该文章正文 / 评论列表 / 提交评论时：

```
[请求] Header: X-Article-Token: <token>
   │
   ├─ 1. JwtTokenService.parse(token)
   ├─ 2. typ 必须为 "article_access"，否则 401
   ├─ 3. aid 必须等于当前 URL 中的 article_id，否则 403
   ├─ 4. exp 校验
   ▼
   放行
```

🔴 **两类 token 在过滤器层按 `typ` 字段分发到不同处理链**，互不互通：
- access token 不能用作文章解锁凭证
- article access token 不能用作身份凭证（不含 `sub`）

## 8. 安全白名单

配置：`application.yml` → `myblog.security.public-endpoints`，必须以 `HTTP method + path` 双维度声明（详见 `../rules/security-baseline.md` §6）。

## 9. 客户端 IP 提取

登录、评论限流和审计必须共用 `ClientIpResolver`：

1. 直连请求只使用 `request.getRemoteAddr()`，忽略客户端提供的代理头
2. 仅当远端地址匹配 `myblog.web.trusted-proxies` 时读取 `X-Forwarded-For` / `X-Real-IP`
3. `X-Forwarded-For` 取首个非空段；无有效代理头时回退到远端地址

空白返回 `null`，不写入伪造值。`ip_source`（归属地）**V2 不解析**（R2 #16）。

## 10. 关键代码路径（V2 目标）

- `AccessTokenIssuer` — identity 应用层使用的访问令牌签发端口
- `AccessTokenVerifier` — Security 过滤器使用的访问令牌验证端口
- `JwtTokenService` — common 内部 JWT 编解码实现，实现上述端口（含 typ 区分）
- `RefreshTokenService` — refresh token 签发 / 校验 / 撤销 / SHA-256 哈希
- `ArticleAccessTokenService` — PASSWORD 文章 token 签发 / 校验
- `JwtAuthenticationFilter` — Authorization header 处理（typ=access）
- `ArticleAccessTokenFilter` — X-Article-Token 处理（typ=article_access）
- `JwtSecretStartupValidator` — 启动校验密钥

依赖方向固定为：

```text
identity.application -> common.auth.token.AccessTokenIssuer
common.security.JwtAuthenticationFilter -> common.auth.token.AccessTokenVerifier
```

identity 不调用过滤器或 Spring Security 具体实现；过滤器后续通过验证端口完成
`token_version` 校验，不直接依赖 identity infrastructure。
- `AuthApplicationService` — 登录 / 刷新 / 登出用例编排
- `LoginRateLimiter` — 登录限流（Caffeine）
- `LoginAuditService` / `t_user_auth` — 审计

## 11. 测试覆盖

详见 `../rules/security-baseline.md` §14。

## 12. 历史对照

V1 / 早期 V2 的实现差异：

| 项 | V1 / 早期 V2 | V2（当前） |
|---|---|---|
| 撤销机制 | 内存 `TokenRevocationStore`（不跨重启 / 不跨实例） | `token_version` + DB `t_refresh_token` |
| Refresh token | 无 | 有，TTL 7d |
| Access token TTL | 长（小时级或更久） | 15 分钟 |
| `ver` claim | 无 | 有 |
| `typ` 区分 | 无 | `"access"` / `"article_access"` |
| Redis 依赖 | 教训表中（P-001 计划迁 Redis） | **不引 Redis**（DB 方案） |

详见 `../pitfalls.md` P-001。
