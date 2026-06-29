# 认证流程

> 本文档回答："登录怎么走？JWT 怎么签发与校验？审计在哪？PASSWORD 文章怎么解锁？"
> 适用范围：V2 identity 模块 + common-infra 安全组件。
> 相关：`../../archive/project-handbook/product/decisions-draft.md` R1 / R6 C1 / R7 D6、`../adr/0007-jwt-via-spring-security-jose.md`、`../rules/security-baseline.md`

## 1. 总览（双 Token 机制）

V2 采用 **access token（无状态 JWT）+ refresh token（DB 存储）** 双 token：

```
[客户端] ──POST /api/auth/login──► [AuthController]
                                        │
                                        ▼
                            [AuthApplicationService]
                                        │
                  ├─ Caffeine 前置限流
                  ├─ LoginCredentialVerifier
                  │    ├─ UserAccountRepository
                  │    ├─ BCrypt
                  │    └─ LoginStateRecorder（失败累计 / 锁定）
                  │
                  ▼
                    [LoginSuccessTransactionService]
                  审计 → refresh token → access token
                  （任一步失败时数据库变更整体回滚）
                                        │
                                        ▼
                       返回 ApiResponse<TokenResponse>
                       { accessToken, refreshToken,
                         accessExpiresIn, refreshExpiresIn }
```

## 2. 登录流程详解

1. **入口**：`POST /api/auth/login`，Body：`{ username, password }`
2. **规范化用户名**：trim 后使用 `Locale.ROOT` 小写；密码保持原样
3. **限流前置**：同 IP + 同规范化 username 连续失败 5 次后，第 6 次请求返回 429 `90002`
4. **校验账号与类型**：未知账号或 GUEST → `401 + 10001`
5. **校验锁定**：`locked_until > now` → `401 + 10001`，不暴露锁定状态
6. **校验密码**：BCrypt 不匹配时累计数据库失败状态和 Caffeine 失败次数，返回 `401 + 10001`
7. **凭据成功**：清除当前 Caffeine 限流键
8. **成功短事务**：按固定顺序写成功审计 → 签发并持久化 refresh token → 签发 access token
9. **事务失败**：任一步抛出运行时异常，成功审计和 refresh token 写入一起回滚，返回 `500 + 99999`
10. **返回**：`{ accessToken, refreshToken, accessExpiresIn: 900, refreshExpiresIn: 604800 }`

登录失败（未知账号、GUEST、密码错误、锁定）**不**更新 `last_login_at` / `last_login_ip`。

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
- JWT 本体不保存服务端会话；认证时通过持久化端口比对 `ver` 与当前 `token_version`，**不依赖独立黑名单**
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
   ├─ 1. 计算 SHA-256(refreshToken)
   ├─ 2. SELECT ... FOR UPDATE 锁定仍有效的 t_refresh_token
   ├─ 3. 重新查询 t_user_auth
   │     - deleted=0
   │     - type IN (ADMIN, DEMO)
   │     - locked_until 为空或已结束
   ├─ 4. 标记旧 refresh token revoked=1
   ├─ 5. 持久化新 refresh token
   ├─ 6. 使用最新 username、role、token_version 签发 access token
   ▼
   返回 { accessToken, refreshToken, accessExpiresIn, refreshExpiresIn }
```

`RefreshSessionTransactionService` 统一控制行锁、旧 token 撤销、新 refresh token 写入和 access token 签发。JWT 签发抛出运行时异常时，数据库事务整体回滚，旧 refresh token 仍可重试。

同一旧 refresh token 串行或并发使用时最多一次成功。不存在、过期、已撤销、重放、账号删除、账号锁定和 GUEST 账号统一返回 `401 + 10002`。

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

`POST /api/auth/logout` 已开放，必须携带仍有效的 Bearer access token。Controller 通过 `@CurrentUser AuthenticatedPrincipal` 读取账号 ID，不接受客户端提供的用户 ID。

## 7. 修改密码流程

```
PUT /api/auth/me/password   Header: Authorization: Bearer <access>
                            Body: { currentPassword, newPassword }
   │
   ├─ 1. 仅允许 ADMIN，用户 ID 只取自当前认证主体
   ├─ 2. SELECT t_user_auth ... FOR UPDATE 锁定当前账号
   ├─ 3. BCrypt 校验当前密码；校验新密码不与当前密码相同
   ├─ 4. BCrypt 生成新 password_hash
   ├─ 5. 单条 UPDATE 原子更新 password_hash 并令 token_version + 1
   ├─ 6. 撤销当前账号全部 refresh token
   ▼
   返回 200，不签发新 token，客户端重新登录
```

账号加锁、密码更新、token version 递增和 refresh token 全撤销处于同一事务；任一步失败时整体回滚。账号行锁还保证两个使用相同旧密码的并发改密请求最多一个成功。

## 8. PASSWORD 文章解锁流程

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

## 9. 安全白名单

配置：`application.yml` → `myblog.security.public-endpoints`，必须以 `HTTP method + path` 双维度声明（详见 `../rules/security-baseline.md` §6）。

## 10. 客户端 IP 提取

登录、评论限流和审计必须共用 `ClientIpResolver`：

1. 直连请求只使用 `request.getRemoteAddr()`，忽略客户端提供的代理头
2. 仅当远端地址匹配 `myblog.web.trusted-proxies` 时读取 `X-Forwarded-For` / `X-Real-IP`
3. `X-Forwarded-For` 取首个非空段；无有效代理头时回退到远端地址

空白返回 `null`，不写入伪造值。`ip_source`（归属地）**V2 不解析**（R2 #16）。

## 11. 关键代码路径（V2 目标）

- `AccessTokenIssuer` — identity 应用层使用的访问令牌签发端口
- `AccessTokenVerifier` — Security 过滤器使用的访问令牌验证端口
- `JwtTokenService` — common 内部 JWT 编解码实现，实现 `AccessTokenIssuer` / `AccessTokenDecoder`（含 typ 区分）
- `PersistentAccessTokenVerifier` — identity 应用层的持久化验证实现，组合 JWT 解码与用户 `token_version` 校验
- `RefreshTokenService` — refresh token 签发 / 校验 / 撤销 / SHA-256 哈希
- `RefreshSessionApplicationService` — refresh 参数校验与统一 `10002` 映射
- `RefreshSessionTransactionService` — refresh 行锁和单事务轮换
- `LogoutApplicationService` — 当前认证主体 ID 校验与全端撤销
- `ChangePasswordApplicationService` — 当前 ADMIN 改密、会话失效与事务编排
- `PasswordHashService` — 登录与改密共用的 BCrypt 校验 / 摘要端口
- `PasswordAccountRepository` — 改密账号行锁读取与密码、token version 原子更新端口
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
- `AuthApplicationService` — 当前负责后台登录外层编排
- `LoginSuccessTransactionService` — 登录成功审计、refresh token 持久化和 access token 签发的短事务
- `LoginRateLimiter` — 登录限流（Caffeine）
- `LoginStateRecorder` / `t_user_auth` — 失败累计、锁定和成功审计

## 12. 测试覆盖

当前认证会话测试覆盖登录、刷新、旧 token 重放、账号删除/锁定/GUEST、JWT 签发失败回滚、全端退出、改密事务回滚、账号隔离、H2 双线程并发轮换和并发改密。2026-06-14 全量结果为 288 tests、0 failures、0 errors、4 skipped；跳过项均为 Docker 不可用时的 Testcontainers MySQL 条件测试。

## 13. 历史对照

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
