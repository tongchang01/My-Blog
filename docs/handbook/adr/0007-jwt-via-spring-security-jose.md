# ADR-0007：使用 Spring Security JOSE 与数据库会话撤销

> 状态：当前有效
> 适用范围：V2 后台认证
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/auth/`、`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/`
> 权威程度：ADR

## 背景

旧 JWT 实现缺少标准过期声明并依赖外部缓存判定状态。V2 需要标准 JWT、短期 access token、可轮换 refresh token 和跨重启撤销能力。

## 决策

- access token 使用 `spring-security-oauth2-jose` 生成 HS256 JWT，有效期 15 分钟。
- JWT 包含 `jti`、`sub`、`iss`、`iat`、`exp`、`typ=access`、`ver`、`username` 和 `roles`。
- 每次认证比对数据库账号状态和 `token_version`。
- refresh token 是随机字符串，有效期 7 天，数据库只保存 SHA-256 hash，并在事务中单次轮换。
- 退出和改密递增 `token_version` 并撤销全部 refresh token。

## 结果

认证不依赖 Redis 或内存黑名单。JWT 只用于后台身份认证；PASSWORD 文章使用独立随机访问令牌，见 `0019-password-article-access.md`。

流程见 `../architecture/auth-flow.md`。
