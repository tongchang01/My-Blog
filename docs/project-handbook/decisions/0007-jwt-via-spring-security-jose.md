# ADR-0007: JWT 改用 spring-security-oauth2-jose

- 状态：accepted
- 日期：2026-04
- 决策者：项目负责人

## 背景

V1 使用 `jjwt 0.9.0`：
- 已多年无维护
- 与 Spring Security 集成弱，需要大量手写代码
- V1 中 JWT 设计存在问题：硬编码 issuer、没有 exp claim（依赖 Redis 判过期）

V2 需要重新设计认证体系。

## 备选方案

- 方案 A：继续 jjwt（新版 0.12.x）
- 方案 B：spring-security-oauth2-jose（Spring 官方）
- 方案 C：自实现 JWT 签发解析

## 决定

选 B：使用 `spring-security-oauth2-jose`，由 Spring Security 标准方式签发与解析。

## 理由

- 官方推荐方案，长期支持有保障
- 与 Spring Security 6 无缝集成
- 标准 JWT claims，自包含（exp、jti、roles），不依赖外部存储判过期
- 撤销可走独立 store，逻辑清晰

## 后果

正面：
- Token 自包含，可独立验证
- 与 Spring Security 风格一致，减少自实现代码
- 标准 claims 易于和第三方集成

负面：
- API 比 jjwt 复杂，学习曲线略陡
- 当前撤销 store 是内存实现，多实例部署需后续迁移到 Redis

后续需关注：
- 多实例部署前必须把 `TokenRevocationStore` 迁到 Redis

## 相关

- 相关 rules：`rules/security-baseline.md`
- 相关 pitfalls：`pitfalls.md` 撤销存储项
