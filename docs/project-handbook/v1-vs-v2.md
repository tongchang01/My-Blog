# V1 → V2 对照

> 本文档回答："V1 有什么问题？V2 怎么改的？区别在哪？"
> 性质：阶段性快照，V2 稳定后可归档。

## 1. 技术栈对照

| 维度 | V1 | V2 |
|------|-----|-----|
| Spring Boot | 2.3.7（EOL） | 3.5.14 |
| Java | 1.8 | 17 LTS |
| ORM | JdbcTemplate + MyBatis 混用 | MyBatis-Plus 3.5.12 为主 |
| JWT | `jjwt 0.9.0`（停维护） | `spring-security-oauth2-jose` |
| API 文档 | knife4j 2.x | knife4j 4.x（基于 springdoc-openapi） |
| JSON | fastjson 1.2.76（CVE） | Jackson（Spring Boot 默认） |
| 数据库迁移 | 无（手工 SQL） | Flyway |
| 安全 | Spring Security + 手写 JWT | Spring Security 6 + 标准 JWT |
| 包结构 | `com.aurora.myblog` | `com.tyb.myblog.v2` |
| 架构 | 技术分层（controller/service/mapper）| 模块化单体 + 四层 DDD-lite |
| 架构守护 | 无 | ArchUnit |

## 2. V1 关键问题清单

### 2.1 安全

- 🔴 JWT 硬编码 issuer `"huaweimian"`（见 `pitfalls.md` P-003）
- 🔴 JWT 无 `exp` claim，依赖 Redis 判过期（P-004）
- 🔴 安全白名单只配 path 不配 method（P-006）
- 🔴 fastjson 1.2.76 多 CVE（P-005）
- 🔴 密钥可能硬编码（依环境而异）

### 2.2 架构

- Controller 直接调用 Mapper，业务规则散落
- 无业务模块边界，所有功能堆在同一 package 下
- JdbcTemplate 与 MyBatis 混用无统一规则（P-008）
- 无架构守护，规则全靠人记

### 2.3 错误处理

- Controller 自接异常返 `success:false`（P-007）
- HTTP 状态与业务状态脱节
- 异常消息可能包含内部细节

### 2.4 测试

- 测试覆盖率低
- 无架构测试
- 无迁移脚本测试

### 2.5 依赖与构建

- 依赖较旧，部分有 CVE
- 无 Flyway，数据库变更靠手工 SQL
- 构建工具与配置陈旧

## 3. V2 重构方向

### 3.1 总体思路

- **完整 schema 重设计**（不复用 V1 旧表结构）：审计列规范、软删三件套、i18n 三语副本、状态枚举等全部按 V2 决定重新建表
- 重新设计代码组织：模块化 + 四层 DDD-lite
- 升级核心依赖到当前长期支持版本
- 引入架构守护（ArchUnit）防止退化
- 引入迁移工具（Flyway）规范化 DB 变更
- 文档先行，规则可执行
- V1 冻结只读，V2 与 V1 **不共用数据库**

### 3.2 模块化决策

划分为 6 个模块（见 `arch/module-map.md`、`product/decisions-draft.md` R5 B1）：

| 模块 | 职责 |
|---|---|
| `identity` | t_user_auth / t_user_info / 登录 / JWT |
| `content` | t_article / t_category / t_tag / t_article_tag |
| `comment` | t_comment |
| `system` | t_site_config / t_attachment / 全局配置 |
| `stats` | t_page_view / t_page_view_daily |
| `common-infra` | 基础设施：异常、响应包装、审计 handler、storage 抽象、限流等 |

每个业务模块四层：`web / application / domain / infrastructure`。

### 3.3 安全设计

- 强制环境变量注入 JWT secret，启动校验缺失即失败
- **双 Token 机制**：access token (JWT, 15min) + refresh token (DB, 7d)；`token_version` claim 实现跨重启 / 跨实例撤销（**不引 Redis**）
- access token 携带 `typ` claim 区分登录 token 与 PASSWORD 文章解锁 token
- 白名单 method + path 双维度
- 登录限流：同 IP+username 5 次/10 分钟冷却（Caffeine）
- 登录审计：成功才更新 `last_login_at` / `last_login_ip`，审计失败不签发 token
- 详见 `arch/auth-flow.md`、`rules/security-baseline.md`

### 3.4 错误处理

- 业务异常统一走 `ApiException` + `ApiErrorCode`
- `GlobalExceptionHandler` 统一兜底
- HTTP 状态码语义明确（401 / 403 / 404 / 409 / 429 / 500）
- **错误码空间**：String 5 位 MMSSS（如 `00000` / `10001` / `99999`），详见 `rules/error-handling.md` §3

### 3.5 持久化

- MyBatis-Plus 为主，统一 ORM
- 审计列规范：8 列基线（id + 4 个 created/updated + 3 个 deleted）由 `BaseEntity` + `AuditFieldHandler` 自动填
- **DB 不用 FOREIGN KEY 约束**（详见 `pitfalls.md` R-012），引用完整性由 application 层维护
- SQL 写法分层：BaseMapper / @Select / XML（详见 `rules/sql-placement.md`）
- Flyway 管理 schema 变更

### 3.6 横切

- **时区**：JVM + MySQL + Clock + API + 前端五层全部 Asia/Tokyo（详见 `product/decisions-draft.md` R7 D11、`pitfalls.md` R-011）
- **i18n**：UI 文案前端 JSON + vue-i18n；业务内容 DB 三语副本（文章标题/摘要、分类/标签、站点配置）；fallback 到中文
- **URL 策略**：id 主导 + slug 可读性增强 `/{lang}/posts/{id}-{slug}`；不维护 slug 历史
- **CORS**：白名单显式列出，禁止 `*`
- **日志**：Logback + JSON + MDC traceId + 敏感字段脱敏

### 3.7 测试

- JUnit 5 + Spring Boot Test + Spring Security Test
- ArchUnit 守护架构规则
- 必测场景明确列出（见 `rules/testing-policy.md`）

## 4. V1 / V2 关系

V2 是**独立的新项目**：
- 独立代码库（`MyBlog-springboot-v2/`，包前缀 `com.tyb.myblog.v2`）
- **独立的新数据库**（不复用 V1 schema；Flyway 从 V1__init 起重新建表）
- V1 冻结只读，作为"原功能怎么做"的历史参考
- 路径前缀区分（V2 用 `/api/public/**` / `/api/auth/**` / `/api/admin/**`）
- V2 上线后 V1 整体下线

## 5. 已完成 vs 待完成

详见 `status.md` 与 `roadmap.md`。

## 6. V1 不会再做的事

- 不修复 V1 bug（除非影响 V2 共用数据）
- 不升级 V1 依赖
- 不补 V1 测试
- V2 迁完后 V1 整体下线

## 7. 相关文档

- 设计决定（早期难改）：`product/decisions-draft.md` R1–R7
- 架构：`arch/module-map.md`、`arch/auth-flow.md`、`arch/persistence-strategy.md`
- 决策：`decisions/0001-0014`
- 规则：`rules/`
- 已识别问题：`pitfalls.md`
