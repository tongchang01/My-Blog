# 包结构与分层依赖规则

> 状态：当前有效
> 适用范围：`MyBlog-springboot-v2/` 所有 Java 代码
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`、`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
> 权威程度：规则

## 本文档回答什么问题

本文档规定 V2 后端代码应该放在哪个包、层与层之间能不能直接调用、跨模块协作如何进行，以及哪些规则由 ArchUnit 自动守护。

## 1. 基础包名

V2 后端基础包固定为：

```text
com.tyb.myblog.v2
```

禁止新增旧包名或其它并列根包承载 V2 业务代码。

## 2. 顶层包结构

| 包 | 类型 | 职责 |
|----|------|------|
| `common` | 公共基础设施 | 响应、异常、认证上下文、token 端口、Security 接入、配置、存储、邮件、时间、MyBatis-Plus、Flyway 等 |
| `identity` | 业务模块 | 用户、登录、JWT access token、refresh token、当前用户资料 |
| `content` | 业务模块 | 文章、分类、标签、定时发布、文章回收站 |
| `comment` | 业务模块 | 评论、留言板、审核、回复通知 |
| `system` | 业务模块 | 站点配置、附件、友链 |
| `stats` | 业务模块 | 访问打点、日聚合、后台统计看板 |

文档中的 `common-infra` 指 `com.tyb.myblog.v2.common` 这一整层，不是独立顶层包。

禁止恢复旧的顶层技术包：

```text
com.tyb.myblog.v2.infrastructure
```

## 3. 业务模块内部四层

业务模块固定采用以下结构：

```text
{module}/
├── web/             HTTP 接入：Controller、Request、Response、Web DTO、参数校验、OpenAPI 注解
├── application/     用例编排：ApplicationService、Command、Query、Result、事务边界、跨模块契约
├── domain/          业务规则：领域对象、领域枚举、领域服务、Repository 端口
└── infrastructure/  技术实现：Entity、Mapper、XML、Repository 实现、外部服务适配
```

推荐细分：

```text
infrastructure/
├── persistence/
│   ├── entity/
│   ├── mapper/
│   └── repository/
└── client/
```

## 4. web 层规则

允许：

- Controller。
- HTTP Request / Response / VO。
- 参数校验注解。
- OpenAPI 注解。
- 调用本模块 application 用例。

禁止：

- 直接调用 Mapper。
- 拼 SQL。
- 访问数据库 Entity。
- 调用其它模块 infrastructure。
- 编写核心业务规则。

web 层只允许依赖经过裁决的稳定领域枚举。当前白名单由 `ArchitectureRulesTest` 维护，例如账号类型、文章状态、评论状态、友链状态等。其它领域对象不得直接暴露到 web 层。

## 5. application 层规则

允许：

- ApplicationService。
- Command / Query / Result。
- 用例编排。
- 事务边界。
- 权限、审计、状态流转组织。
- 调用本模块 domain 对象、领域服务和端口。
- 通过对方模块 application 契约进行跨模块协作。

禁止：

- 拼 SQL。
- 继承或调用 MyBatis-Plus `BaseMapper`。
- 依赖本模块 infrastructure。
- 暴露 HTTP Request / Response。
- 存放数据库 Entity。
- 直接绑定 Servlet API。
- 直接暴露外部 SDK 类型。

## 6. domain 层规则

允许：

- 领域对象。
- 领域枚举。
- 业务规则。
- 状态流转判断。
- 不依赖技术框架的领域服务和策略。
- Repository 端口定义。

禁止：

- 依赖 Spring Web。
- 依赖 Spring Security。
- 依赖 MyBatis、MyBatis-Plus、JdbcTemplate。
- 依赖 Servlet API。
- 读 HTTP Header。
- 返回 HTTP Response。
- 调用外部 SDK。
- 直接 `LocalDateTime.now()` 或 `new Date()`。

当前时间必须由应用层传入，或通过统一 `Clock` 在合适边界获取。

## 7. infrastructure 层规则

允许：

- MyBatis-Plus Entity。
- Mapper。
- XML SQL。
- Repository 实现。
- Reader / Writer / Gateway 实现。
- 外部服务适配。

禁止：

- 写 Controller。
- 定义业务规则。
- 让其它模块直接访问本模块 Mapper。
- 把 Entity 泄漏到 web 层。
- 依赖本模块 web 层。

## 8. common 层规则

允许：

- 全局异常处理。
- 统一响应。
- 分页模型。
- 认证上下文和 MVC 支撑。
- Security 链路和 JWT 实现。
- Clock Bean。
- i18n 与 OpenAPI 配置。
- MyBatis-Plus / Flyway / DataSource 配置。
- 跨模块公共端口，如存储、邮件、token。

禁止：

- 依赖任何业务模块。
- 存放只被一个业务模块使用的 DTO。
- 存放业务数据库 Entity。
- 存放业务 Mapper。
- 让业务模块依赖 `common.security` 具体实现。

`common.auth.token` 是稳定端口层，不得依赖 Spring Security 或任何业务模块。

## 9. 跨模块协作规则

唯一允许方式：通过对方模块 application 层公开契约。

允许示例：

```text
comment.application -> content.application
stats.application -> content.application
content.application -> system.application
```

禁止示例：

```text
comment.infrastructure -> content.infrastructure.persistence.mapper.ArticleMapper
content.web -> system.infrastructure.persistence.entity.AttachmentEntity
stats.application -> content.domain.article.Article
```

特殊认证边界：

```text
identity.application -> common.auth.token.AccessTokenIssuer
common.security.JwtAuthenticationFilter -> common.auth.token.AccessTokenVerifier
identity.application.PersistentAccessTokenVerifier -> implements AccessTokenVerifier
```

含义：identity 拥有用户状态和认证用例，common 拥有无业务状态的 token 端口、JWT 编解码和 Security 接入。

## 10. ArchUnit 守护

测试文件：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`

当前守护规则包括：

| # | 规则 |
|---|------|
| 1 | 业务模块不依赖 `common.security` 具体实现 |
| 2 | `common.auth.token` 不依赖 Spring Security 或业务模块 |
| 3 | `common` 不依赖业务模块 |
| 4 | `domain` 不依赖 web、application、infrastructure |
| 5 | `domain` 不依赖 Spring Security、Spring Web、MyBatis、MyBatis-Plus、Servlet API |
| 6 | `domain` 不直接读取系统时间 |
| 7 | `web` 不依赖 infrastructure |
| 8 | `web` 只允许依赖白名单内的稳定领域枚举 |
| 9 | `application` 不依赖 web、infrastructure 或 Servlet API |
| 10 | `infrastructure` 不依赖 web |
| 11 | 顶层业务模块无循环依赖 |
| 12 | 跨业务模块只允许依赖对方 application |
| 13 | `common.security` 不依赖 identity infrastructure |
| 14 | 不允许旧顶层 `com.tyb.myblog.v2.infrastructure` 包 |

违反架构规则应导致 `mvn test` 失败。

## 11. DTO / Command / Entity 放置速查

| 类型 | 放置位置 |
|------|----------|
| HTTP 入参 | `web` 或 `web/request` |
| HTTP 出参 | `web` 或 `web/response` |
| Web VO | `web` |
| 用例命令 | `application` 或 `application/command` |
| 用例查询 | `application` 或 `application/query` |
| 用例结果 | `application` |
| 领域对象 | `domain` |
| Repository 端口 | `domain` |
| 数据库 Entity | `infrastructure/persistence/entity` |
| Mapper | `infrastructure/persistence/mapper` |
| Repository 实现 | `infrastructure/persistence/repository` |

## 12. 例外

- 如需跨模块直接访问非 application 层，必须先写 ADR 并更新 ArchUnit 规则。
- 如需在 web 层暴露新的领域枚举，必须先确认该枚举是稳定 API 语义，并更新 ArchUnit 白名单。
- 如需新增顶层模块，必须同步更新模块地图、包结构规则和 ArchUnit 测试。

## 相关文档

- 模块地图：`../architecture/module-map.md`
- 文档规则：`documentation.md`
- ADR：`../adr/0002-package-base-com-tyb-myblog-v2.md`、`../adr/0003-four-layer-architecture.md`、`../adr/0004-six-business-modules.md`、`../adr/0012-archunit-guards.md`（待迁移）
