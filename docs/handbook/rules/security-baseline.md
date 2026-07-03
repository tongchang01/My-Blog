# 安全基线

> 状态：当前有效
> 适用范围：MyBlog V2 后端安全相关代码、配置和后台会话实现
> 最后校准：2026-07-03
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/`、`identity/application/token/`、`frontend/apps/admin/src/features/auth/`
> 权威程度：规则

## 本文档回答什么问题

本文档规定 V2 写认证、授权、密码、JWT、refresh token、CORS、限流、客户端 IP、匿名写入口和上传能力时必须遵守的安全规则。

## 1. 当前安全模型

V2 当前采用：

- Spring Security。
- 无状态 JWT access token。
- 数据库存储 refresh token hash。
- BCrypt 密码摘要。
- `token_version` 服务端撤销机制。
- Caffeine 单实例限流。
- method + path 维度公开白名单。
- ADMIN / DEMO / GUEST 三类访问边界。

当前后台前端将 access token 和 refresh token 保存在 `localStorage` 的后台专用 key 中。该方案对个人后台可接受，但不属于最高安全等级；升级到 HttpOnly Cookie 方案见 O-008。

## 2. JWT 密钥管理

配置项：`myblog.security.jwt.secret`

注入方式：`MYBLOG_JWT_SECRET`

规则：

- 禁止在代码中硬编码 JWT 密钥。
- 禁止提交真实密钥到 Git。
- 启动时必须校验密钥非空。
- 禁止使用默认开发值 `change-me-change-me-change-me-change-me`。
- UTF-8 编码后长度必须至少 32 字节。
- 生产环境必须由环境变量或安全配置系统注入。

当前由 `JwtSecretStartupValidator` 守护。缺失、默认值或长度不足都会启动失败。

## 3. Access Token

Access token 当前为 JWT，使用 HS256 签名。

当前 claim：

| claim | 含义 |
|-------|------|
| `sub` | 用户 ID，字符串 |
| `iss` | issuer |
| `iat` | 签发时间 |
| `exp` | 过期时间 |
| `jti` | token ID |
| `typ` | 固定为 `access` |
| `ver` | 用户当前 `token_version` |
| `username` | 登录用户名 |
| `roles` | 业务角色名列表，不包含 `ROLE_` 前缀 |

规则：

- TTL 当前为 `15m`。
- 解析时必须校验签名、过期时间、issuer、`typ=access` 和 `ver`。
- 认证过滤器只接受 Bearer access token。
- access token 不能作为 PASSWORD 文章访问凭证。
- JWT 中不得放密码、refresh token、密钥或敏感个人信息。

## 4. Refresh Token

Refresh token 当前为随机字符串，不是 JWT。

规则：

- 使用 `SecureRandom` 生成 32 字节随机数。
- 明文只返回给客户端一次。
- 数据库只保存 SHA-256 hash。
- TTL 当前为 `7d`。
- refresh 时必须通过行锁锁定仍有效 token。
- 成功 refresh 必须撤销旧 token，并写入新 token。
- 旧 refresh token 并发使用时最多一次成功。
- 不存在、过期、已撤销、账号不可用等情况统一返回 token 失效语义。

## 5. token_version 撤销机制

`token_version` 位于用户认证状态中，用于让旧 access token 立即失效。

必须递增 `token_version` 的场景：

- 用户主动 logout。
- 用户修改密码。
- 后续管理员强制下线。

access token 校验时必须比较：

```text
token.ver == 当前用户 token_version
```

不一致时返回 `10002` + 401。

## 6. 密码处理

规则：

- 使用 BCrypt，当前强度来自 `myblog.security.password.bcrypt-strength`，默认 10。
- 禁止明文存储密码。
- 禁止可逆加密保存密码。
- 禁止在日志、异常、响应、前端持久化中输出密码。
- 当前用户改密只能由 ADMIN 执行。
- 改密用户 ID 只能取当前认证主体，不能由客户端传入。
- 当前密码和新密码不 trim。
- 新密码必须满足长度规则，并且不能与旧密码相同。
- 改密时必须锁定账号行，避免并发请求使用同一旧密码重复成功。
- 密码摘要更新、`token_version + 1`、refresh token 全撤销必须处于同一事务。
- 改密成功后不签发新 token，客户端必须重新登录。

## 7. 登录安全

登录流程必须满足：

- 用户名 trim 后使用 `Locale.ROOT` 小写规范化。
- 密码保持原样参与 BCrypt 校验。
- 未知账号、GUEST、密码错误和锁定状态不得向客户端暴露差异。
- 登录成功才更新 `last_login_at`、`last_login_ip` 并重置失败状态。
- 登录失败不更新登录成功审计字段。
- 成功审计、refresh token 写入和 access token 签发必须保持事务一致性；失败时不应签发半成功会话。

登录失败限流：

| 维度 | 当前配置 |
|------|----------|
| key | 客户端 IP + 规范化 username |
| 阈值 | 5 次连续失败 |
| 冷却 | 10 分钟 |
| 实现 | Caffeine 单实例缓存 |
| 超限响应 | `90002` + 429 |

多实例部署下该限流不共享，见 O-009。

## 8. 授权规则

当前后端采用 Spring Security request matcher 和 application 层裁剪共同约束。

| 路径/方法 | 当前语义 |
|-----------|----------|
| public endpoints | 配置中的 method + path 匹配后匿名访问 |
| `OPTIONS /**` | 匿名访问，用于 CORS 预检 |
| `GET /api/admin/...` 中列出的后台读接口 | ADMIN + DEMO |
| `PUT /api/auth/me/password` | ADMIN |
| `PATCH /api/auth/me/profile` | ADMIN |
| 其它 `/api/admin/**` | ADMIN |
| 未匹配接口 | 默认要求认证 |

DEMO 规则：

- DEMO 是后台只读演示账号。
- DEMO 写操作必须由后端拒绝，不能只靠前端隐藏按钮。
- DEMO 可读字段是否需要裁剪，由 application 层统一处理，不应散落在 Controller、Mapper、Repository。
- DEMO 敏感字段裁剪边界已按 O-002 关闭：文章非公开正文、评论邮箱/IP/UA、附件内部存储字段由后端裁剪；统计 dashboard 不裁剪。

## 9. 公开白名单

配置位置：`myblog.security.public-endpoints`

规则：

- 必须同时声明 HTTP method 和 path。
- 禁止只按 path 模糊放行。
- 新增公开接口时必须同步配置白名单和测试。
- 公开接口仍然必须做参数校验、限流或内容安全处理。

当前公开接口包括：

- `GET /actuator/health`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- 公开站点配置、友链、分类、标签、文章列表和详情。
- 公开文章评论提交。
- 留言板评论读取与提交。
- 公开访问统计打点。

## 10. CORS 与 CSRF

当前 API 使用 Bearer token，不依赖浏览器 Cookie 会话，因此 Spring Security CSRF 已关闭。

CORS 当前规则：

- 配置位置：`myblog.cors.allowed-origins`。
- 禁止使用通配符 `*`。
- `allowCredentials` 当前为 `true`，因此 origins 必须为明确来源。
- 允许方法：`GET`、`POST`、`PUT`、`PATCH`、`DELETE`、`OPTIONS`。
- 允许请求头：`Authorization`、`Content-Type`、`X-Request-Id`。
- 同源反向代理部署可保持 allowed origins 为空，但必须验证 `/api/**` 路径转发正确。

如果未来改为 HttpOnly Cookie 保存 refresh token，需要重新评估 CSRF 策略。

## 11. 客户端 IP 解析

登录审计、登录限流、评论限流和统计限流必须使用统一的 `ClientIpResolver`。

规则：

1. 直连请求只使用 `request.getRemoteAddr()`。
2. 只有当远端地址匹配 `myblog.web.trusted-proxies` 时，才读取代理头。
3. 可信代理场景优先取 `X-Forwarded-For` 第一个非空段。
4. 没有有效 `X-Forwarded-For` 时取 `X-Real-IP`。
5. 仍没有时回退到远端地址。
6. 空白值返回 `null`，不写入伪造值。

生产使用反向代理时，必须显式配置代理 IP 或 CIDR，不要为了省事信任整个私网。

## 12. 匿名写入口安全

匿名写入口至少包括评论、留言和统计打点。

规则：

- 必须限制请求频率。
- 必须校验目标资源状态。
- 评论和留言必须做内容长度、站点 URL 协议、Markdown 清洗和 HTML Sanitizer。
- 前台只能渲染清洗后的 `contentHtml`，不能渲染用户原始 Markdown 为 HTML。
- 统计 visitor hash 使用每日轮换 HMAC，不保存原始 IP/User-Agent 作为访客标识。

## 13. 附件和上传安全

当前附件上传仅 ADMIN 可执行。

规则：

- 上传大小受 Spring multipart 和业务配置双重限制，当前最大 10 MiB。
- 只接受图片类型：JPEG、PNG、WebP、GIF。
- 必须识别真实格式，不只信任文件扩展名或 Content-Type。
- 必须限制尺寸、总像素和 GIF 帧数。
- 上传对象不允许携带静态云凭证。
- S3 使用默认凭证链，不在配置中保存 access key / secret key。
- 附件公开 URL 不应暴露本地文件系统路径。

## 14. PASSWORD 文章安全边界

当前完整 PASSWORD 解锁流程仍未完成，见 O-001。

当前规则：

- PASSWORD 文章不能把正文直接返回给未解锁访客。
- 登录 access token 不能作为文章解锁凭证。
- 后续若实现 Article Access Token，必须使用独立 `typ=article_access`，不含 `sub`，并通过 `X-Article-Token` 或明确的新方案携带。
- Article Access Token 方案落地前，不得在前台伪造“已解锁”状态。

## 15. 后台前端会话存储

当前后台实现：

- access token 和 refresh token 保存在 localStorage 的 `myblog-admin-session`。
- session 解析失败、字段缺失或 refresh token 过期时会清理本地会话。
- refresh 失败会清理 token 和当前用户。

规则：

- token、密码和登录请求体不得输出到 console 或持久化日志。
- 不得把 token 写入 URL query、localStorage 之外的随意 key 或第三方 SDK。
- 任何 XSS 风险都会放大 localStorage token 风险，后台页面必须严格避免渲染未清洗 HTML。
- 是否升级为 HttpOnly Cookie 方案见 O-008。

## 16. 环境变量

安全相关变量至少包括：

| 变量 | 用途 | 规则 |
|------|------|------|
| `MYBLOG_JWT_SECRET` | JWT 签名密钥 | 必填，至少 32 字节 |
| `MYBLOG_DATASOURCE_USERNAME` | 数据库用户名 | local/prod 必填 |
| `MYBLOG_DATASOURCE_PASSWORD` | 数据库密码 | local/prod 必填 |
| `MYBLOG_DATASOURCE_URL` | 数据库 URL | prod 必填，local 有默认 URL |
| `MYBLOG_STATS_HASH_SECRET` | 统计 HMAC 密钥 | 启用统计时必填 |
| `MYBLOG_CORS_ALLOWED_ORIGINS` | 允许前端来源 | 跨域部署时必须明确配置 |
| `MYBLOG_WEB_TRUSTED_PROXIES` | 可信代理 | 使用反向代理时配置 |
| `MYBLOG_RESEND_API_KEY` | 邮件服务密钥 | Resend 启用时必填 |

完整环境变量后续统一维护在 `../ops/environment.md`。

## 17. 测试要求

安全相关变更至少覆盖：

- JWT 密钥缺失、默认值、长度不足时启动失败。
- JWT 签发、解析、过期、issuer、`typ`、`ver` 校验。
- 缺 token / 无效 token 返回 `10002` + 401。
- 无权限返回 `10003` + 403。
- ADMIN / DEMO 写权限边界。
- 登录失败限流。
- refresh token hash 存储、轮换、重放失败。
- logout 和改密后旧 token 失效。
- CORS 白名单和公开接口白名单。
- ClientIpResolver 的直连、可信代理、伪造头场景。
- 上传类型、大小、真实格式和非法图片。

## 相关文档

- 未完成和争议项：`../start-here/open-issues.md`
- API 响应规则：`api-response.md`
- 异常处理规则：`error-handling.md`
- 包结构规则：`package-layout.md`
- 认证流程：`../architecture/auth-flow.md`
