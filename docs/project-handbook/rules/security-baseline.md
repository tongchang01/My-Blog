# 安全基线

> 本文档回答："写涉及认证、密码、密钥、白名单的代码时，必须做什么？不能做什么？"
> 适用范围：V2 所有安全相关代码与配置。
> 相关 ADR：`../decisions/0007-jwt-via-spring-security-jose.md`

## 1. 认证框架

- 使用 **Spring Security + JWT**
- JWT 通过 `spring-security-oauth2-jose` 生成与解析
- 不再使用旧版 `jjwt 0.9.0`

## 2. JWT 密钥管理（红线）

- 🔴 **不设代码默认值**。配置项 `myblog.security.jwt.secret` 必须通过环境变量 `MYBLOG_JWT_SECRET` 注入
- 🔴 启动时若环境变量缺失，**必须启动失败**（已由 `JwtSecretStartupValidator` 守护）
- 🔴 生产环境密钥**至少 32 字节**
- 配置类必须注明："生产环境必须通过环境变量覆盖默认密钥，不能使用代码中的开发默认值"

```java
// ✅ 正例
String secret = jwtProperties.secret();   // 来自 @ConfigurationProperties，环境变量注入

// ❌ 反例
String secret = "mydefaultsecret";        // 硬编码
```

## 3. JWT Token 内容

Token 必须自包含（不依赖外部存储），至少含：
- `subject`（用户标识）
- `username`
- `roles`
- `jti`（用于撤销）
- `expiresAt`（过期时间）

## 4. Token 撤销

- 登出时调用 `JwtTokenService.revoke(jti)`，写入 `TokenRevocationStore`
- 解析 Token 时检查是否已撤销
- ⚠️ **当前撤销存储是内存实现**，服务重启或多实例部署时会失效。后续应迁到 Redis（见 `../pitfalls.md` P-001）

## 5. 密码处理

- 使用 **BCrypt**（已通过 Spring Security 标配实现）
- 🔴 禁止明文存储、禁止可逆加密
- 🔴 禁止在日志、异常、响应中输出密码字段

## 6. 安全白名单

- 配置位置：`application.yml` → `myblog.security.public-endpoints`
- 必须以 **HTTP method + path** 双维度声明（不能只声明 path）
- 例：`GET /api/comments` 可匿名，`POST /api/comments` 必须登录
- 新增公开接口时，必须在白名单中同步声明

## 7. 登录流程与审计

登录成功后必须：
- 更新 `t_user_auth.last_login_time`
- 更新 `t_user_auth.ip_address`

登录失败、账号不存在、密码错误、禁用用户**都不更新**审计字段。

客户端 IP 提取顺序：
1. `X-Forwarded-For`
2. `X-Real-IP`
3. `request.getRemoteAddr()`

空白值返回 `null`，不写入伪造值。`ip_source`（归属地）当前暂不解析。

## 8. 审计失败处理

- 审计 SQL 失败时，登录接口返回**系统错误**，**不签发 token**
- 原因：保持系统状态一致，避免掩盖审计链路问题
- 例外：后续如需提高可用性，可设计异步审计或失败降级，但需独立 ADR

## 9. CORS

- 配置位置：`ApiCorsProperties`（`application.yml` → `myblog.cors`）
- 🔴 禁止通配符 `*`
- 必须显式列出允许的源

## 10. 未在本阶段实现的安全特性

| 项 | 状态 | 建议处理 |
|----|------|---------|
| Redis 在线用户管理 / 踢下线 / 设备管理 | ⏳ 未实现 | 后续专项 |
| IP 归属地解析 | ⏳ 未实现 | 引入第三方库时单独评估 |
| 登录限流 / 验证码 | ⏳ 未实现 | 见 pitfalls.md，后续专项 |
| OAuth2 第三方登录 | ⏳ 未实现 | 暂无需求 |
| 富文本 XSS 清洗 | ⏳ 未实现 | 见 pitfalls.md，迁移文章模块前必做 |
| 上传文件安全（MIME 校验、大小限制） | ⏳ 未实现 | 见 pitfalls.md，迁移上传前必做 |

## 11. 环境变量清单（启动前必设）

| 变量 | 用途 | 必填 |
|------|------|------|
| `MYBLOG_JWT_SECRET` | JWT 签名密钥（≥32 字节） | ✅ 是 |
| `MYBLOG_DB_URL` | 数据库连接 URL | 视环境 |
| `MYBLOG_DB_USERNAME` | 数据库账号 | 视环境 |
| `MYBLOG_DB_PASSWORD` | 数据库密码 | 视环境 |

## 12. 测试要求

- `JwtSecretStartupValidatorTest`：验证空密钥时启动失败
- `JwtTokenServiceTest`：签发/解析/撤销路径全覆盖
- `JwtAuthenticationFilterTest`：合法 / 过期 / 撤销 / 缺失 token 行为
- `SecurityConfigTest`：白名单接口可匿名、受保护接口必鉴权
- `AuthControllerTest`：登录成功审计字段更新；登录失败审计字段不动
