# 包结构与分层依赖规则

> 本文档回答："V2 代码该放在哪个包？层与层之间能不能直接调？"
> 适用范围：`MyBlog-springboot-v2/` 所有 Java 代码。
> 相关 ADR：`../decisions/0002-package-base-com-tyb-myblog-v2.md`、`0003-four-layer-architecture.md`、`0004-six-business-modules.md`（**R5 修订**）、`0012-archunit-guards.md`

## 1. 基础包名

固定为 `com.tyb.myblog.v2`，不再使用旧的 `com.aurora.myblog.v2`。

## 2. 顶层包结构（六大模块，R5 修订）

| 包 | 类型 | 职责 |
|----|------|------|
| `common` | 非业务 | `common-infra` 一层：响应封装、异常体系、Security 链路、Knife4j、Clock、i18n、ArchUnit 规则；其下 `infrastructure/` 子包放 MyBatis-Plus / Flyway / DataSource 等数据库基础配置 |
| `identity` | 业务 | 用户、角色、登录、JWT 签发与刷新（access 15min + refresh 7d） |
| `content` | 业务 | 文章、分类、标签 |
| `comment` | 业务 | 评论、留言板（复用 t_comment）、审核 |
| `system` | 业务 | 站点配置、附件、友链申请（**不含** V1 的字典 / 后台菜单 / 操作日志） |
| `stats` | 业务 | 访客统计（自研日聚合：t_page_view 明细 + t_page_view_daily 聚合） |

> **命名说明**：文档里写的 `common-infra` 指代 `com.tyb.myblog.v2.common`（含子目录 `infrastructure`）这一整层，**不是两个顶层包**。详见 ADR-0004 R5 修订。

错误码段映射：identity=10 / content=20 / comment=30 / system=40 / stats=50 / common-infra=90 / 兜底=99。

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
- **禁止**：依赖 Spring Web / MyBatis-Plus / JdbcTemplate / 数据库 Entity；读 HTTP Header；返回 HTTP Response；调外部 SDK；直接 `LocalDateTime.now()` / `new Date()`（必须用注入的 `Clock`，ADR-0018 / R-011）
- **依赖方向**：`domain` 不依赖 `web/application/infrastructure`

### 4.4 infrastructure 层

- **允许**：MyBatis-Plus Entity、Mapper、XML、Repository 实现、Reader/Writer/Gateway 数据库实现、外部服务适配
- **禁止**：写 Controller、定义业务规则、让其它模块直接访问本模块 Mapper、把 Entity 泄漏到 web 层
- **禁止**：Flyway 脚本里出现 `FOREIGN KEY`（ADR-0017 / R-012）

### 4.5 common-infra（`com.tyb.myblog.v2.common`）

- **允许**：全局异常处理、统一响应、分页、Web 支撑、Security 链路、Clock Bean、i18n、Knife4j / springdoc 配置、跨模块基础工具；`common/infrastructure/` 子包放 MyBatis-Plus 配置、数据源、事务、Flyway 配置
- **禁止**：任何业务模块的私有逻辑、只被一个模块使用的 DTO、数据库 Entity、业务 Mapper、具体外部服务实现
- **禁止反向依赖**：`common` 不依赖任何业务模块（ArchUnit 规则 #4）

## 5. 跨模块协作规则

**唯一允许的方式**：通过对方模块 `application/` 暴露的服务接口调用。

```
✅ comment.application → content.application.ArticleVisibilityService
✅ stats.application → content.application.ArticleQueryService（聚合时反查文章标题）
❌ comment.infrastructure → content.infrastructure.persistence.mapper.ArticleMapper
```

**禁止**：
- 一个模块的 `web` 直接调另一个模块的 `infrastructure`
- 直接使用另一个模块的 MyBatis-Plus Mapper
- `common` 反向依赖任一业务模块
- 业务模块之间共享 Entity

常见跨模块关系（见 `arch/module-map.md` §4 完整表）：

| 调用方 | 被调方 | 典型场景 |
|--------|--------|----------|
| `comment` | `identity` | 评论展示用户名/头像 |
| `comment` | `content` | 校验文章存在 |
| `content` | `system` | 校验封面 attachment 存在 |
| `stats` | `content` / `identity` | 聚合时按文章 id 反查标题 |

## 6. ArchUnit 守护（自动校验）

测试文件：`src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`（DDL 冻结后按 R5 模块清单重写）。

| # | 规则 |
|---|------|
| 1 | `..domain..` 不依赖 `..web..` / `..infrastructure..` |
| 2 | `..web..` 不访问 `..infrastructure.persistence.mapper..` |
| 3 | `..application..` 不直接访问 MyBatis-Plus Mapper |
| 4 | `..common..` 不依赖业务模块 |
| 5 | 业务模块不互相访问对方 `infrastructure.persistence` |
| 6 | `..domain..` 不直接 `LocalDateTime.now()` / `new Date()`（必须用注入的 Clock，ADR-0018 / R-011） |
| 7 | Flyway 脚本不出现 `FOREIGN KEY`（ADR-0017 / R-012，由 Flyway review 守护，非 ArchUnit）|

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
- `common-infra` 可被任何业务模块依赖，但不能反向依赖业务模块
