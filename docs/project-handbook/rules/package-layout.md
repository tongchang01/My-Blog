# 包结构与分层依赖规则

> 本文档回答："V2 代码该放在哪个包？层与层之间能不能直接调？"
> 适用范围：`MyBlog-springboot-v2/` 所有 Java 代码。
> 相关 ADR：`../decisions/0002-package-base-com-tyb-myblog-v2.md`、`0003-four-layer-architecture.md`、`0004-six-business-modules.md`、`0012-archunit-guards.md`

## 1. 基础包名

固定为 `com.tyb.myblog.v2`，不再使用旧的 `com.aurora.myblog.v2`。

## 2. 顶层包结构（六大模块）

| 包 | 类型 | 职责 |
|----|------|------|
| `common` | 非业务 | 全局通用：异常处理、响应模型、分页、Web 支撑、安全能力、配置属性、IP/UA 解析等 |
| `infrastructure` | 非业务 | 全局技术基础设施：MyBatis-Plus 基础配置、数据源、事务、Flyway、全局认证适配 |
| `identity` | 业务 | 身份、用户、角色、权限、登录、审计 |
| `content` | 业务 | 文章、分类、标签、归档、访问控制 |
| `comment` | 业务 | 评论、审核、恢复 |
| `system` | 业务（预留） | 系统配置、菜单、站点参数 |

## 3. 业务模块内部统一四层

```
{module}/
├── web/             HTTP 接入：Controller、Request、Response、Web DTO、参数校验注解、OpenAPI 注解
├── application/     用例编排：ApplicationService、Command、Query、用例输入输出对象、事务边界
├── domain/          业务规则：领域对象、领域枚举、领域服务、状态流转（不依赖任何框架）
└── infrastructure/  技术实现：Entity、Mapper、XML、Repository 实现、Reader/Writer/Gateway 实现、外部 SDK 适配
```

`infrastructure/` 推荐子结构：`persistence/{entity,mapper,repository}` 与 `client/`。

## 4. 各层"允许"与"禁止"

### 4.1 web 层

- **允许**：Controller、Request/Response、Web DTO、参数校验、OpenAPI 注解、Web 层错误码映射
- **禁止**：调用 Mapper、拼 SQL、访问 Entity、写业务规则、调其它模块的 infrastructure
- **依赖方向**：`web → application`

### 4.2 application 层

- **允许**：ApplicationService、Command、Query、用例输入输出对象、跨领域编排、事务、权限/审计/状态流转组织
- **禁止**：拼 SQL、继承 MyBatis-Plus `BaseMapper`、暴露 HTTP Request/Response、存放数据库 Entity、直接暴露外部 SDK
- **依赖方向**：`application → domain`、`application → 本模块 infrastructure`

### 4.3 domain 层（最严格）

- **允许**：领域对象、领域枚举、业务规则、状态流转判断、领域服务、不依赖技术框架的策略
- **禁止**：依赖 Spring Web / MyBatis-Plus / JdbcTemplate / 数据库 Entity；读 HTTP Header；返回 HTTP Response；调外部 SDK
- **依赖方向**：`domain` 不依赖 `web/application/infrastructure`

### 4.4 infrastructure 层

- **允许**：MyBatis-Plus Entity、Mapper、XML、Repository 实现、Reader/Writer/Gateway 数据库实现、V1 数据导入映射、外部服务适配
- **禁止**：写 Controller、定义业务规则、让其它模块直接访问本模块 Mapper、把 Entity 泄漏到 web 层

### 4.5 common 与顶层 infrastructure

- **common 允许**：全局异常处理、统一响应、分页、Web 支撑、安全能力、配置属性、跨模块基础工具
- **common 禁止**：任何业务模块的私有逻辑、只被一个模块使用的 DTO、数据库 Entity、MyBatis-Plus Mapper、具体外部服务实现
- **顶层 infrastructure 允许**：全局认证适配、全局数据库配置、MyBatis-Plus 基础配置、springdoc 配置、Flyway、数据源、事务配置
- **顶层 infrastructure 禁止**：业务模块私有 Mapper / Entity、Controller、业务规则

## 5. 跨模块协作规则

**唯一允许的方式**：通过对方模块 `application/` 暴露的服务接口调用。

```
✅ comment.application → content.application.ArticleVisibilityService
❌ comment.infrastructure → content.infrastructure.persistence.mapper.ArticleMapper
```

**禁止**：
- 一个模块的 `web` 直接调另一个模块的 `infrastructure`
- 直接使用另一个模块的 MyBatis-Plus Mapper
- `common` 反向依赖任一业务模块
- 业务模块之间共享 Entity

## 6. ArchUnit 守护（自动校验）

测试文件：`src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`

强制以下规则：
1. `..domain..` 不依赖 `..web..`
2. `..domain..` 不依赖 `..infrastructure..`
3. `..web..` 不访问 `..infrastructure.persistence.mapper..`
4. `..application..` 不直接访问 MyBatis-Plus Mapper
5. `..common..` 不依赖 `..identity..` `..content..` `..comment..` `..system..`
6. 业务模块不能直接访问其它业务模块的 `infrastructure.persistence`
7. 业务模块不依赖 `common.security` 的具体实现
8. `common` 不反向依赖任何业务模块

违反任一规则，`mvn test` 直接失败。**新增模块务必同步更新 ArchUnit 规则**。

## 7. DTO / Command / Query / Entity 放置速查

| 类型 | 放置位置 |
|------|----------|
| HTTP 入参 | `web/request/` |
| HTTP 出参 | `web/response/` |
| 用例命令 | `application/command/` |
| 用例查询 | `application/query/` |
| 领域对象 | `domain/` |
| 数据库 Entity | `infrastructure/persistence/entity/` |

## 8. 例外

- 跨模块协作必须通过 `application` 服务接口；如有特殊场景需要例外，先写 ADR 评估后再实施
- 顶层 `common` 与 `infrastructure` 可被任何业务模块依赖，但不能反向依赖业务模块
