# V1 与 V2 关系说明

> 本文档回答："V1 和 V2 是什么关系？V2 是否兼容 V1 数据库？V1 在重构中承担什么作用？"
> 适用范围：MyBlog 全量 V2 重构期间的产品、数据库、后端和前端设计讨论。

## 0. 结论

- V1 继续线上运行，作为业务参考和数据来源。
- V2 独立重构，不兼容 V1 数据库结构。
- V2 schema 重新设计，V1 表只用于业务考古和一次性数据导入映射。
- 当前已写的 V2 后端业务代码属于过渡实现，基盘能力保留，最终业务实现以后续产品规格、领域模型和新 schema 为准。

## 1. 技术栈对照

| 维度          | V1                              | V2                            |
| ----------- | ------------------------------- | ----------------------------- |
| Spring Boot | 2.3.7（EOL）                      | 3.5.14                        |
| Java        | 1.8                             | 17 LTS                        |
| ORM         | JdbcTemplate + MyBatis 混用       | MyBatis-Plus 3.5.12 为主        |
| JWT         | `jjwt 0.9.0`（停维护）               | `spring-security-oauth2-jose` |
| API 文档      | knife4j 2.x                     | springdoc-openapi             |
| JSON        | fastjson 1.2.76（CVE）            | Jackson（Spring Boot 默认）       |
| 数据库迁移       | 无（手工 SQL）                       | Flyway                        |
| 安全          | Spring Security + 手写 JWT        | Spring Security 6 + 标准 JWT    |
| 包结构         | `com.aurora.myblog`             | `com.tyb.myblog.v2`           |
| 架构          | 技术分层（controller/service/mapper） | 模块化单体 + 四层 DDD-lite           |
| 架构守护        | 无                               | ArchUnit                      |

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

- 三端、后端、数据库和业务规则按 V2 重新设计。
- V1 仅作为业务语义参考，不作为新 schema 兼容目标。
- 重新设计代码组织：模块化单体 + 四层
- 升级核心依赖到当前长期支持版本
- 引入架构守护（ArchUnit）防止退化
- 引入迁移工具（Flyway）规范化 DB 变更
- 文档先行，规则可执行

### 3.2 模块化决策

划分为 6 个模块（见 `arch/module-map.md`）：

- `common`、`infrastructure`、`identity`、`content`、`comment`、`system`（system 尚未创建）

每个业务模块四层：`web / application / domain / infrastructure`。

### 3.3 安全设计

- 强制环境变量注入 JWT secret，启动校验缺失即失败
- Token 自包含（exp + jti），撤销走独立 store
- 白名单 method + path 双维度
- 登录审计：成功才更新 `last_login_time` / `ip_address`，审计失败不签发 token

### 3.4 错误处理

- 业务异常统一走 `ApiException` + `ApiErrorCode`
- `GlobalExceptionHandler` 统一兜底
- HTTP 状态码语义明确（401 / 403 / 404 / 409 / 500）

### 3.5 持久化

- MyBatis-Plus 为主
- SQL 写法分层：BaseMapper / @Select / XML（详见 `rules/sql-placement.md`）
- JdbcTemplate 渐进迁移
- Flyway + H2 测试

### 3.6 测试

- JUnit 5 + Spring Boot Test + Spring Security Test
- ArchUnit 守护架构规则
- 必测场景明确列出（见 `rules/testing-policy.md`）

## 4. 兼容策略

V2 与 V1 并存期间的原则：

- V1 线上服务保持不动，除非出现必须修复的生产事故。
- V2 不共享 V1 schema，不以兼容旧表字段为目标。
- V1 数据在 V2 上线前通过一次性导入迁移，具体映射写入 `migration/`。
- V1 旧字段、旧状态值、旧表名只在业务考古和导入映射中说明，不散落到 V2 新业务代码规范里。
- V2 接口契约以后续 `api-contract/` 为准，不继续沿用 V1 接口惯性。

## 5. 当前 V2 代码的定位

当前后端 V2 已经完成一批基础工程能力：

- 包名和模块结构：`com.tyb.myblog.v2`
- 模块化单体与四层结构
- Spring Boot 3、Java 17、MyBatis-Plus、Flyway、ArchUnit
- JWT、安全白名单、统一异常、统一响应、中文注释规则

这些属于 V2 基盘，原则上保留。

identity、content、comment 中按旧表写出的业务实现，需要降级理解为"过渡实现"：

- 可以作为测试、分层和 MyBatis-Plus 迁移样板参考。
- 不代表最终业务功能、接口字段或表结构已经定稿。
- 后续会根据 `product/feature-inventory.md`、`product/use-cases.md`、`product/data-model.md`、`arch/schema-design.md` 调整或重写。

## 6. 已完成 vs 待完成

详见 `status.md` 与 `roadmap.md`。

## 7. V1 不会再做的事

- 不做 V1 大规模重构
- 不升级 V1 依赖
- 不补 V1 测试
- 不把 V1 schema 当作 V2 兼容目标
- V2 上线并完成数据导入后，V1 整体下线

## 8. 相关文档

- 架构：`arch/module-map.md`、`arch/auth-flow.md`、`arch/persistence-strategy.md`
- 决策：`decisions/0013-no-v1-compatibility.md`、`decisions/0014-schema-redesign-principles.md`
- 规则：`rules/`
- 数据迁移：`migration/`
- 已识别问题：`pitfalls.md`
