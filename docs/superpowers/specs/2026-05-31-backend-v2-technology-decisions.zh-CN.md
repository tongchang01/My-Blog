# 后端 V2 技术选型与架构决策记录

**日期：** 2026-05-31

**适用范围：** `MyBlog-springboot-v2`

**状态：** 已记录当前决策；后端项目结构另开文档继续讨论。

---

## 1. 背景

我正在把旧版 Java 后端重构为后端 V2。当前 V2 已经完成基础工程、安全能力、身份登录、内容领域、评论基础能力、评论治理和本地评论审计表结构迁移。

随着后续业务迁移继续推进，代码改动量会越来越大。如果此时不先敲定技术选型，后面很容易出现两个问题：

- 我在不同模块里使用不同技术栈，导致维护成本升高。
- 我在业务迁移完成后再反向替换基础框架，造成大量返工。

因此，本记录用于冻结后端 V2 的主要技术方向。项目结构仍然保留为下一轮重点讨论，不在本文中强行拍板。

---

## 2. 已确认决策

| 决策项 | 结论 | 说明 |
| --- | --- | --- |
| 项目形态 | 模块化单体 | 项目体量不适合拆微服务。微服务会提前引入部署、链路、事务和运维复杂度。 |
| Java 版本 | Java 17 | 旧版 Java 8 不再作为 V2 兼容目标。 |
| 构建工具 | Maven | 保持当前 Maven，不切换 Gradle。 |
| Spring Boot | Spring Boot 3.5.x | V2 已经使用 Spring Boot 3.5.14，后续继续沿用 3.x 体系。 |
| Web 框架 | Spring MVC | 当前系统是典型后台 API，不需要 WebFlux。 |
| 安全框架 | Spring Security + JWT | V2 已使用 Spring Security 和 OAuth2 JOSE 相关能力生成/解析 JWT，继续沿用。 |
| ORM | MyBatis-Plus | 后续统一切到 MyBatis-Plus，不再扩大 JdbcTemplate 使用范围。已有 JdbcTemplate 代码通过单独任务逐步迁移。 |
| 数据库 | MySQL | 主库继续使用 MySQL。 |
| 数据库迁移 | SQL 迁移规范优先，Flyway 使用范围待收口 | 目前测试环境已用 Flyway，local 真实库仍关闭自动迁移。后续需要明确正式环境迁移策略。 |
| 缓存 | Redis 后续按需引入 | 当前阶段不作为必选基础设施。出现验证码、限流、热点缓存、访问统计等明确场景后再引入。 |
| MQ | RabbitMQ 后续按需引入 | 当前阶段不引入。后续仅用于邮件、订阅通知、异步后处理等非核心链路。 |
| 搜索 | 暂不引入 Elasticsearch | 当前文章搜索先由 MySQL 承担。等数据量、分词、相关性排序需求明确后再评估 ES。 |
| AWS SDK | 后续继续使用 AWS SDK | 旧版已有 S3、SES，后续能力仍可保留，但业务层必须通过接口隔离 AWS 具体实现。 |
| 工具库 | 引入 Hutool | Hutool 作为通用工具库引入，但需要约束使用边界，避免工具方法侵入核心业务规则。 |
| 接口文档 | springdoc-openapi | Spring Boot 3 下优先使用 `springdoc-openapi-starter-webmvc-ui`，不沿用旧 `knife4j-spring-boot-starter 2.0.7`。 |
| JSON | Jackson | 使用 Spring Boot 默认 Jackson，不继续引入旧版 Fastjson。 |
| 健康检查 | Actuator | V2 已使用 Actuator 暴露 health，后续保留。 |
| 测试 | JUnit 5 + Spring Boot Test + Spring Security Test + H2 + ArchUnit | 保持并继续加强测试。ArchUnit 用于约束分层。 |
| 模板引擎 | 不在后端 V2 使用 Thymeleaf | 前台和后台后续走 Vue3，后端只提供 API。 |

---

## 3. 旧版、新版与后续技术对照

| 类别 | 旧版后端已使用 | 当前 V2 已使用 | 后续决策 |
| --- | --- | --- | --- |
| Java | Java 8 | Java 17 | 固定 Java 17 |
| Spring Boot | 2.3.7.RELEASE | 3.5.14 | 继续 3.x |
| Web | Spring MVC | Spring MVC | 继续 Spring MVC |
| Security | Spring Security + 自定义 JWT + `jjwt 0.9.0` | Spring Security + OAuth2 JOSE JWT | 继续新版方案 |
| ORM | MyBatis-Plus 3.4.2 | JdbcTemplate | 全量迁移到 MyBatis-Plus |
| 数据库 | MySQL | MySQL / H2 测试库 | 主库 MySQL，测试继续 H2 |
| Migration | 无系统化迁移机制 | Flyway 测试迁移，local 关闭 | 建立 SQL 迁移规范，再决定正式 Flyway 策略 |
| Redis | Spring Data Redis | 未引入 | 按明确场景引入 |
| MQ | RabbitMQ | 未引入 | 仅异步通知/后处理场景引入 |
| Search | Elasticsearch | 未引入 | 暂不引入 |
| Scheduler | Quartz | 未引入 | 后续有定时任务再引入 |
| Mail | Spring Mail / AWS SES | 未引入 | 后续保留 AWS SES 能力 |
| Storage | AWS S3 SDK | 未引入 | 后续保留 AWS S3 能力 |
| API Doc | Knife4j 2.0.7 | 未引入 | 使用 springdoc-openapi |
| 工具库 | Hutool、commons-lang3、commons-codec | 未引入 | 引入 Hutool，其他按需 |
| JSON | Fastjson、Jackson | Jackson | 使用 Jackson |
| Monitoring | 无明确体系 | Actuator health | 继续 Actuator |
| Test | Spring Boot Test | Spring Boot Test、Spring Security Test、H2、ArchUnit | 继续增强 |

---

## 4. ORM 决策：统一 MyBatis-Plus

### 4.1 决策

后端 V2 后续统一使用 MyBatis-Plus 作为主 ORM，不再继续扩大 JdbcTemplate 使用范围。

### 4.2 原因

我的项目是博客内容系统，业务形态主要包括：

- 用户、角色、菜单、登录审计。
- 文章、分类、标签、归档、置顶、推荐、访问控制。
- 评论、评论审核、评论恢复、评论审计。
- 后台管理列表、分页、筛选、状态变更。

这些场景有大量标准 CRUD、分页查询、条件查询和后台管理查询。MyBatis-Plus 在这些场景下比 JdbcTemplate 更适合长期维护。

### 4.3 约束

- 过渡期允许 JdbcTemplate 和 MyBatis-Plus 共存。
- 新增业务模块默认使用 MyBatis-Plus。
- 已有 JdbcTemplate 模块不在零散任务里顺手替换，必须按模块单独迁移。
- 最终目标是项目内只保留 MyBatis-Plus 作为主数据访问方式。
- 不允许在 Service 层直接拼复杂 SQL。复杂查询应放在 Mapper XML 或明确的 infrastructure 实现里。

### 4.4 预计迁移步骤

1. 新增 MyBatis-Plus 依赖和基础配置。
2. 加入 Mapper 扫描、分页插件和测试基线。
3. 选一个低风险模块试迁移。
4. 按 identity、content、comment 的顺序逐步替换已有 JdbcTemplate。
5. 每替换一个模块，必须保留或补强对应测试。

---

## 5. Hutool 决策：引入但限制边界

### 5.1 决策

后端 V2 可以引入 Hutool 作为通用工具库。

### 5.2 推荐使用场景

- 字符串判空、格式处理。
- 集合判空、简单转换。
- 日期时间格式化。
- 脱敏、摘要、简单编码。
- 文件名、路径、轻量工具处理。

### 5.3 不推荐使用场景

- 核心业务规则判断。
- 安全敏感加密逻辑。
- 复杂时间规则，例如跨时区业务结算。
- 为了少写两行代码而降低可读性的地方。

### 5.4 约束

Hutool 是工具，不是架构。业务语义必须仍然通过清晰的方法名、领域对象和服务边界表达。

---

## 6. 接口文档决策：springdoc-openapi

### 6.1 决策

后端 V2 使用 `springdoc-openapi-starter-webmvc-ui` 作为接口文档方案。

### 6.2 不继续使用旧 Knife4j 2.0.7 的原因

旧版后端使用的是 `knife4j-spring-boot-starter 2.0.7`。这套依赖属于 Spring Boot 2 时代的 Swagger 生态，迁移到 Spring Boot 3 后兼容性和长期维护都不是最佳选择。

### 6.3 说明

Knife4j 可以理解为 Swagger/OpenAPI 文档 UI 和增强能力的一种实现。后端 V2 的基础选择应该先稳定在 Spring Boot 3 生态更通用的 springdoc-openapi。后续如果确实需要更好的中文 UI 或增强体验，再评估 Knife4j 4.x，而不是沿用旧版 2.x。

---

## 7. Redis、MQ、Elasticsearch 决策边界

### 7.1 Redis

当前不作为基础必选组件。后续出现以下明确场景时再引入：

- 验证码。
- 登录失败限制。
- 接口限流。
- 文章访问量缓冲。
- 热点文章或站点统计缓存。

### 7.2 MQ

当前不引入。后续如果出现以下场景，优先考虑 RabbitMQ：

- 评论后邮件通知。
- 用户订阅文章发布通知。
- 文件上传后的异步处理。
- 访问日志或统计异步落库。

MQ 不用于承载核心业务一致性。核心写入流程仍以数据库事务为准。

### 7.3 Elasticsearch

当前不引入。文章搜索先使用 MySQL。后续只有当以下需求明确时再评估：

- 文章数量明显增大。
- 需要中文分词。
- 需要相关性排序。
- 需要复杂全文检索。

---

## 8. AWS SDK 决策

AWS 相关能力后续继续保留，尤其是：

- S3：图片、附件、对象存储。
- SES：邮件发送。

但 V2 代码需要避免业务层直接依赖 AWS SDK。推荐做法是定义存储、邮件等端口接口，由 infrastructure 层提供 AWS 实现。

这样后续即使更换存储或邮件服务，也不需要改动业务层。

---

## 9. 数据库迁移策略待确认

当前状态：

- 测试环境使用 Flyway 自动迁移 H2。
- local 真实 MySQL profile 关闭 Flyway 自动迁移。
- 本地真实库结构改造目前通过手工 SQL 文档执行。

后续需要单独确定：

- 正式环境是否启用 Flyway。
- 生产迁移脚本如何审核。
- 每次表结构变更是否必须提供回滚脚本。
- 旧库兼容期保留多久。
- 表结构重构和业务代码发布如何分批。

在这些问题明确前，不应贸然打开 local 或生产环境的 Flyway 自动迁移。

---

## 10. 后端项目结构暂不冻结

本记录只冻结技术选型，不冻结项目结构。

后端结构需要下一轮单独讨论，包括：

- 是否继续当前 `common / infrastructure / modules`。
- 模块内部是否继续使用 `api / application / domain / infrastructure`。
- DTO、Command、Query、Response、Assembler 如何放置。
- Mapper、Entity、Repository 的边界如何定义。
- 公共能力和业务模块之间如何防止互相污染。
- ArchUnit 规则如何配合目录结构。

这部分对后续可维护性影响很大，应单独写《后端 V2 项目结构决策记录》。

---

## 11. 近期执行建议

1. 先提交本技术选型记录。
2. 下一轮讨论并冻结后端项目结构。
3. 结构冻结后，新增 MyBatis-Plus 基础设施计划。
4. 用一个低风险模块验证 MyBatis-Plus 迁移方式。
5. 再逐步迁移 identity、content、comment 的已有 JdbcTemplate 实现。

---

## 12. 当前最终技术栈

```text
Java 17
Maven
Spring Boot 3.5.x
Spring MVC
Spring Security + JWT
MyBatis-Plus
MySQL
H2 Test Database
SQL Migration Specification / Flyway Test Migration
Hutool
springdoc-openapi
Actuator
JUnit 5
Spring Boot Test
Spring Security Test
ArchUnit
RabbitMQ later, only when asynchronous notification or post-processing is needed
Redis later, only when cache/rate-limit/statistics scenarios are clear
AWS S3 / AWS SES later, behind infrastructure adapters
```
