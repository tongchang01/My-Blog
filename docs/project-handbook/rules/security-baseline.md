# 安全基线

> 本文档回答："写涉及认证、密码、密钥、白名单的代码时，必须做什么？不能做什么？"
> 适用范围：V2 所有安全相关代码与配置。
> 相关：`product/decisions-draft.md` R1 / R6 C1 / R7 D6 / D7、`../decisions/0007-jwt-via-spring-security-jose.md`

## 1. 认证框架

- 使用 **Spring Security + JWT**
- JWT 通过 `spring-security-oauth2-jose` 生成与解析
- 不再使用旧版 `jjwt 0.9.0`

## 2. JWT 密钥管理（红线）

- 🔴 **不设代码默认值**。配置项 `myblog.security.jwt.secret` 必须通过环境变量 `MYBLOG_JWT_SECRET` 注入
- 🔴 启动时若环境变量缺失，**必须启动失败**（由 `JwtSecretStartupValidator` 守护）
- 🔴 生产环境密钥**至少 32 字节**
- 配置类必须注明："生产环境必须通过环境变量覆盖默认密钥，不能使用代码中的开发默认值"

```java
// ✅ 正例
String secret = jwtProperties.secret();   // 来自 @ConfigurationProperties，环境变量注入

// ❌ 反例
String secret = "mydefaultsecret";        // 硬编码
```

## 3. 双 Token 机制（R6 C1）

V2 采用 **access token（无状态 JWT）+ refresh token（DB 存储）** 双 token：

### Access Token

- 类型：JWT（`spring-security-oauth2-jose` 签发）
- TTL：**15 分钟**（`myblog.security.jwt.access-ttl`）
- 校验：完全无状态，过滤器层 verify 签名 + exp + ver claim
- Claim：
  - 标准：`sub`（user_id）/ `exp` / `iat` / `iss`
  - 自定义：
    - **`ver`**（int）：对应 `t_user_auth.token_version`
    - **`typ`**（固定 `"access"`）：与 PASSWORD 文章 token 强隔离

### Refresh Token

- 类型：随机字符串（**不是** JWT）
- TTL：**7 天**（`myblog.security.jwt.refresh-ttl`）
- 存储：`t_refresh_token` 表，列含 `token_hash VARCHAR(64) UNIQUE`（SHA-256，**不存明文**）/ `user_id` / `expires_at` / `revoked`
- 用法：`/api/auth/refresh` 拿 refresh token 换新 access token，同时再次校验 `user.token_version`

### token_version 机制

- `t_user_auth.token_version INT NOT NULL DEFAULT 0`
- 触发 +1 的场景：
  - 用户改密
  - 用户主动登出
  - 管理员强制下线
- access token 校验时除标准校验外还校验 `token.ver == user.token_version`，不一致 → 401 `10002` token 已失效
- 撤销跨重启 & 跨实例均生效（**不依赖 Redis**）

🔴 **禁止**：内存态 `TokenRevocationStore`（V1/早期 V2 实现方式，已废弃，详见 `pitfalls.md` P-001）。

## 4. PASSWORD 文章访问 Token（独立 Token 体系）

| 项 | Access Token | Article Access Token |
|---|---|---|
| `typ` claim | `"access"` | `"article_access"` |
| `sub` claim | user_id | **不含** |
| 自定义 claim | `ver` | `aid`（article_id） |
| TTL | 15 分钟 | 30 分钟 |
| 用途 | 登录身份 | 单篇 PASSWORD 文章解锁 |
| 携带 header | `Authorization: Bearer ...` | `X-Article-Token: ...` |

- 过滤器层按 `typ` 字段分发到不同处理链，**两类 token 互不互通**
- Article access token 不含 `sub`，不能用作身份凭证；登录 access token 不能用作文章解锁凭证

## 5. 密码处理

- 使用 **BCrypt**（Spring Security 标配，强度配 `myblog.security.password.bcrypt-strength`）
- 🔴 禁止明文存储、禁止可逆加密
- 🔴 禁止在日志、异常、响应中输出密码字段
- PASSWORD 文章密码：前端 HTTPS 传明文 → 后端 `BCrypt.matches(rawPassword, storedHash)` 校验

## 6. 安全白名单

- 配置位置：`application.yml` → `myblog.security.public-endpoints`
- 必须以 **HTTP method + path** 双维度声明（不能只声明 path，避免 P-006）
- 例：`GET /api/public/comments` 可匿名，`POST /api/public/comments` 也可匿名（评论是游客接口）；但 `POST /api/admin/...` 必须 ADMIN
- 新增公开接口时，必须在白名单中同步声明

路径前缀分级（详见 `api-response.md` §6）：

| 前缀 | 鉴权 |
|---|---|
| `/api/public/**` | permitAll |
| `/api/auth/**` | permitAll |
| `/api/admin/** GET` | hasAnyRole('ADMIN','DEMO')；敏感读单独标 hasRole('ADMIN') |
| `/api/admin/** POST/PUT/DELETE/PATCH` | hasRole('ADMIN')（DEMO 写必返 403） |

🔴 DEMO 写权限：DEMO 视为"普通访客 + 后台只读"，写操作必须靠 `@PreAuthorize("hasRole('ADMIN')")` 后端强制拒绝，**不能只靠前端隐藏按钮**。

🔴 敏感读：凡涉及 ADMIN 专属内容的 GET（PRIVATE/草稿正文、评论审计字段等）必须单独 `@PreAuthorize("hasRole('ADMIN')")`。

## 7. 登录流程与审计

登录成功后必须：
- 更新 `t_user_auth.last_login_time`
- 更新 `t_user_auth.ip_address`
- 重置 `login_fail_count = 0`

登录失败、账号不存在、密码错误、禁用用户**都不更新**审计字段（但 `login_fail_count` 按需 +1）。

客户端 IP 提取顺序：
1. `X-Forwarded-For`（取第一个）
2. `X-Real-IP`
3. `request.getRemoteAddr()`

空白值返回 `null`，不写入伪造值。**`ip_source`（归属地）V2 不解析**（R2 #16 决定）。

## 8. 审计失败处理

- 审计 SQL 失败时，登录接口返回**系统错误**（`99999` / HTTP 500），**不签发 token**
- 原因：保持系统状态一致，避免掩盖审计链路问题
- 例外：后续如需提高可用性，可设计异步审计或失败降级，但需独立 ADR

## 9. 限流策略（R7 D6）

| 接口 | 限制 | 超限响应 |
|---|---|---|
| `POST /api/auth/login` | 同 IP + 同 username 5 次/10 分钟冷却 | 429 + `90002` |
| `POST /api/public/articles/{id}/unlock` | 同 IP + 同 article 5 次/10 分钟冷却 | 429 + `90002` |
| `POST /api/public/articles/{id}/comments` | 同 IP 1 分钟 5 条 + 5 分钟内同 IP+同文章重复 content 拒绝 | 429 + `30002` |
| 附件上传 | 仅 ADMIN，不限流 | — |

- 实现：进程内 `Caffeine` 计数器（与 R7 D1 单实例一致）
- 多实例水平扩展时需换 Redis 实现（后置 V3）

## 10. CORS（R7 D7）

- 配置位置：`ApiCorsProperties`（`application.yml` → `myblog.cors`）
- 🔴 禁止通配符 `*`
- 必须显式列出允许的源
- `allow-credentials: true` 时 `allowed-origins` 必须为具体域名
- 默认 exposed-headers：`Authorization` / `X-Article-Token`

## 11. 评论安全基线（R4 #12-P0）

匿名写入口必须实现：

| 能力 | 要求 |
|---|---|
| Markdown 清洗 | 解析 Markdown 子集 → 禁用原始 HTML → Sanitizer（Jsoup / OWASP HTML Sanitizer）白名单清洗 |
| 双字段存储 | `content_md`（用户原文，≤5000）+ `content_html`（清洗后）；🔴 **前台只渲染 `content_html`**（R-013） |
| author_site 协议白名单 | 只允许 `http://` / `https://`；前端锚标签必须 `rel="nofollow noopener noreferrer"` |
| 频率限制 | 见 §9 |

## 12. 富文本与上传

| 项 | 状态 |
|----|------|
| 文章正文 XSS 清洗 | ⚠️ 实现时必做（U-002，迁移文章模块前接入 Sanitizer） |
| 上传文件 MIME / 大小校验 | ⚠️ 实现 `/api/admin/attachments POST` 时落实白名单（U-003） |

## 13. 环境变量清单（启动前必设）

| 变量 | 用途 | 必填 |
|------|------|------|
| `MYBLOG_JWT_SECRET` | JWT 签名密钥（≥32 字节） | ✅ 是 |
| `MYBLOG_DATASOURCE_URL` | 数据库连接 URL | ✅ local / prod |
| `MYBLOG_DATASOURCE_USERNAME` | 数据库账号 | ✅ local / prod |
| `MYBLOG_DATASOURCE_PASSWORD` | 数据库密码 | ✅ local / prod |
| `MYBLOG_CORS_ALLOWED_ORIGINS` | 生产环境允许的前端来源 | 视部署环境 |
| `MYBLOG_MAIL_API_KEY` | Resend API key | ✅ 是（mail 启用时） |

启动时 `MyBlogConfigStartupValidator` 一次性校验所有必填项。

## 14. 测试要求

- `JwtSecretStartupValidatorTest`：验证空密钥时启动失败
- `JwtTokenServiceTest`：签发 / 解析 / `ver` 不匹配失败 / typ 隔离
- `RefreshTokenServiceTest`：refresh 流转 / SHA-256 哈希存储 / revoked 校验
- `JwtAuthenticationFilterTest`：合法 / 过期 / `ver` 不匹配 / 缺失 token 行为
- `ArticleAccessTokenFilterTest`：`typ=article_access` token 不能用作登录凭证
- `SecurityConfigTest`：白名单接口可匿名、受保护接口必鉴权
- `LoginRateLimiterTest`：5 次失败后冷却 10 分钟
- `AuthControllerTest`：登录成功审计字段更新；登录失败审计字段不动
