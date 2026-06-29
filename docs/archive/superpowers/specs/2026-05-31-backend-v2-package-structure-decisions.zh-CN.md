# 后端 V2 项目结构决策记录

**日期：** 2026-05-31

**适用范围：** `MyBlog-springboot-v2`

**状态：** 生效。后续 Java 包结构迁移、注释补齐、MyBatis-Plus 引入和业务迁移必须遵守本文。

---

## 1. 背景

我已经确认后端 V2 会继续采用模块化单体，不拆微服务。继续推进业务迁移前，需要先把 Java 包结构固定下来，否则后续 MyBatis-Plus Entity、Mapper、DTO、Controller、Application Service 的位置都会变得不稳定。

当前 V2 的结构类似：

```text
com.aurora.myblog.v2.modules.content.api
```

这个结构存在三个问题：

- `aurora` 是旧项目痕迹，不适合作为 V2 长期基础包名。
- `modules` 语义偏空，业务模块本身已经能表达模块含义。
- `api` 容易混淆 HTTP Controller、接口契约、OpenAPI 文档和 Java 对外接口。

因此，从本决策开始，后端 V2 统一迁移到新的包结构。

---

## 2. 已确认结构

### 2.1 基础包名

后端 V2 基础包名固定为：

```text
com.tyb.myblog.v2
```

不再继续使用：

```text
com.aurora.myblog.v2
```

### 2.2 顶层包结构

目标结构：

```text
com.tyb.myblog.v2
├── common              // 全局通用能力
├── infrastructure      // 全局技术基础设施
├── identity            // 用户、认证、角色、菜单
├── content             // 文章、分类、标签、归档
├── comment             // 评论、审核、恢复、评论审计
└── system              // 后台系统配置、站点配置，后续需要再加
```

### 2.3 业务模块内部结构

业务模块内部统一采用：

```text
content
├── web                 // Controller、Request、Response、Web DTO
├── application         // 用例编排、Command、Query、Application Service
├── domain              // 业务规则、状态、领域模型、领域服务
└── infrastructure      // MyBatis-Plus Entity、Mapper、Repository 实现、外部适配
```

`identity`、`content`、`comment`、未来的 `system` 都遵守同一规则。

---

## 3. 命名决策

| 旧结构 | 新结构 | 决策原因 |
| --- | --- | --- |
| `com.aurora.myblog.v2` | `com.tyb.myblog.v2` | 去掉旧项目品牌痕迹，使用当前个人/组织标识。 |
| `modules.content` | `content` | `content` 本身已经是业务模块，不需要再套一层语义偏空的 `modules`。 |
| `api` | `web` | `web` 明确表示 HTTP 接入层，避免和接口契约、OpenAPI、Java API 混淆。 |

---

## 4. 顶层包职责

### 4.1 `common`

`common` 放全局通用能力，只能包含多个业务模块都会复用的基础代码。

允许放：

- 全局异常处理。
- 通用响应模型。
- 通用分页模型。
- 通用 Web 支撑能力。
- 安全支撑能力。
- 配置属性对象。
- 客户端 IP、User-Agent 等通用解析器。
- 跨模块都需要的基础工具适配。

禁止放：

- `identity`、`content`、`comment` 任一模块的私有业务逻辑。
- 只被一个模块使用的 DTO、Command、Response。
- 具体数据库表 Entity。
- MyBatis-Plus Mapper。
- 外部服务具体实现。

### 4.2 `infrastructure`

顶层 `infrastructure` 放全局技术基础设施，不放具体业务模块的私有持久化代码。

允许放：

- 全局认证适配。
- 全局数据库配置。
- MyBatis-Plus 基础配置。
- springdoc-openapi 基础配置。
- 全局外部服务适配基类或通用端口实现。
- Flyway、数据源、事务等基础设施配置。

禁止放：

- `content` 私有文章 Mapper。
- `comment` 私有评论 Entity。
- `identity` 私有用户查询实现。
- Controller、Request、Response。
- 业务规则和状态流转。

### 4.3 `identity`

`identity` 是身份与权限业务模块。

允许包含：

- 登录认证。
- 当前用户资料。
- 用户凭证读取。
- 角色和菜单读取。
- 登录审计。
- 后台用户身份相关接口。

禁止包含：

- 文章、分类、标签业务。
- 评论审核业务。
- 全局安全框架配置。
- 非身份模块的数据库实现。

### 4.4 `content`

`content` 是内容业务模块。

允许包含：

- 文章列表。
- 文章详情。
- 分类。
- 标签。
- 归档。
- 置顶、推荐。
- 受保护文章访问。

禁止包含：

- 评论提交、审核、恢复逻辑。
- 用户认证流程。
- 菜单权限逻辑。
- 全局对象存储实现。

### 4.5 `comment`

`comment` 是评论业务模块。

允许包含：

- 评论提交。
- 评论展示。
- 评论回复。
- 后台评论审核。
- 评论软删除和恢复。
- 评论提交端 IP、User-Agent 审计。

禁止包含：

- 文章主体管理逻辑。
- 用户登录认证流程。
- 全局通知 MQ 配置。
- 邮件发送具体实现。

### 4.6 `system`

`system` 是后续预留模块，用于后台系统配置和站点配置。

允许包含：

- 站点基础配置。
- 后台系统参数。
- 运行时展示配置。
- 未来后台系统设置接口。

禁止包含：

- `identity` 的登录权限逻辑。
- `content` 的文章业务逻辑。
- `comment` 的评论业务逻辑。
- 部署脚本和服务器配置。

---

## 5. 业务模块内部职责

### 5.1 `web`

`web` 是 HTTP 接入层。

允许放：

- `Controller`。
- HTTP Request。
- HTTP Response。
- Web 层 DTO。
- 参数校验注解。
- OpenAPI 注解。
- Web 层错误码映射。

禁止做：

- 直接调用 MyBatis-Plus Mapper。
- 直接拼 SQL。
- 直接访问数据库 Entity。
- 编写核心业务规则。
- 直接调用其他模块的 infrastructure。
- 把 Controller 写成业务大杂烩。

依赖方向：

```text
web -> application
```

### 5.2 `application`

`application` 是业务用例编排层。

允许放：

- Application Service。
- Command。
- Query。
- 用例级输入输出对象。
- 跨领域对象的流程编排。
- 事务边界。
- 权限、审计、状态流转的用例级组织。

禁止做：

- 直接拼 SQL。
- 直接继承 MyBatis-Plus `BaseMapper`。
- 直接暴露 HTTP Request、Response。
- 放具体数据库 Entity。
- 把外部服务 SDK 直接暴露给 web 或 domain。

依赖方向：

```text
application -> domain
application -> infrastructure 中由本模块提供的端口实现
```

### 5.3 `domain`

`domain` 是业务语义和规则层。

允许放：

- 领域对象。
- 领域枚举。
- 领域规则。
- 状态流转判断。
- 领域服务。
- 不依赖具体技术框架的业务策略。

禁止做：

- 依赖 Spring Web。
- 依赖 MyBatis-Plus。
- 依赖 JdbcTemplate。
- 依赖数据库表 Entity。
- 读取 HTTP Header。
- 返回 HTTP Response。
- 调用外部 SDK。

依赖方向：

```text
domain 不依赖 web/application/infrastructure
```

说明：如果某段业务规则必须依赖数据库或外部服务，应在 application 层编排，通过端口接口隔离，不应让 domain 直接依赖技术实现。

### 5.4 `infrastructure`

业务模块内的 `infrastructure` 是该模块的技术实现层。

允许放：

- MyBatis-Plus Entity。
- MyBatis-Plus Mapper。
- Mapper XML。
- Repository 实现。
- Reader、Writer、Gateway 的数据库实现。
- 旧库字段兼容映射。
- 外部服务适配实现。

禁止做：

- 放 Controller。
- 放 HTTP Request、Response。
- 反向定义业务规则。
- 让其他业务模块直接访问本模块私有 Mapper。
- 把旧库字段细节泄漏到 web 层。

推荐子结构：

```text
infrastructure
├── persistence
│   ├── entity
│   ├── mapper
│   └── repository
└── client
```

---

## 6. MyBatis-Plus 文件放置规则

MyBatis-Plus 相关文件放在各业务模块自己的 `infrastructure.persistence` 下。

示例：

```text
com.tyb.myblog.v2.content.infrastructure.persistence.entity.ArticleEntity
com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ArticleMapper
com.tyb.myblog.v2.content.infrastructure.persistence.repository.MybatisArticleRepository
```

规则：

- Entity 必须有中文 Javadoc。
- Entity 字段必须写明对应表字段和中文含义。
- Mapper 只负责数据库访问，不承载业务规则。
- Repository 实现负责把 Entity 转换为 domain 或 application 需要的对象。
- 复杂 SQL 必须有中文注释说明查询目的和筛选条件。

---

## 7. DTO、Command、Query、Response 放置规则

### 7.1 Web 入参出参

HTTP Request、Response 放在 `web` 下。

示例：

```text
com.tyb.myblog.v2.comment.web.request.CreateCommentRequest
com.tyb.myblog.v2.comment.web.response.CommentResponse
```

### 7.2 用例命令和查询对象

Command、Query 放在 `application` 下。

示例：

```text
com.tyb.myblog.v2.comment.application.command.CreateCommentCommand
com.tyb.myblog.v2.comment.application.query.CommentListQuery
```

### 7.3 领域对象

领域模型放在 `domain` 下。

示例：

```text
com.tyb.myblog.v2.comment.domain.Comment
com.tyb.myblog.v2.comment.domain.CommentReviewStatus
```

---

## 8. 模块之间的访问规则

### 8.1 允许的访问

- `web` 可以调用本模块 `application`。
- `application` 可以使用本模块 `domain`。
- `application` 可以通过接口使用本模块 `infrastructure` 实现。
- `common` 可以被所有模块依赖。
- 顶层 `infrastructure` 可以为所有模块提供全局技术配置。

### 8.2 禁止的访问

- 一个模块的 `web` 直接调用另一个模块的 `infrastructure`。
- 一个模块直接使用另一个模块的 MyBatis-Plus Mapper。
- `domain` 依赖 `web`。
- `domain` 依赖 Spring MVC、MyBatis-Plus、JdbcTemplate。
- `common` 依赖业务模块。
- `infrastructure` 反向把数据库 Entity 暴露给 web 层。

### 8.3 跨模块协作方式

如果业务确实需要跨模块协作，优先使用 application 层暴露的明确服务接口，而不是直接访问对方数据库实现。

示例：

```text
comment.application -> content.application.ArticleVisibilityService
```

禁止：

```text
comment.infrastructure -> content.infrastructure.persistence.mapper.ArticleMapper
```

---

## 9. ArchUnit 约束方向

后续 ArchUnit 规则至少应覆盖：

- `..domain..` 不依赖 `..web..`。
- `..domain..` 不依赖 `..infrastructure..`。
- `..web..` 不访问 `..infrastructure.persistence.mapper..`。
- `..application..` 不访问 MyBatis-Plus Mapper。
- `..common..` 不依赖 `..identity..`、`..content..`、`..comment..`、`..system..`。
- 业务模块不能直接访问其他模块的 `infrastructure.persistence`。

这些规则在 Task 3 中落到 `ArchitectureRulesTest`。

---

## 10. 注释规则在结构中的落点

本结构必须配合 `docs/superpowers/specs/2026-05-31-backend-v2-code-comment-standards.zh-CN.md` 执行。

强制要求：

- `web` 的 Controller 和 Request/Response 必须有中文注释。
- `application` 的公开用例方法必须有中文业务流程注释。
- `domain` 的枚举、状态、规则必须有中文注释。
- `infrastructure.persistence.entity` 的 Entity 和字段必须有中文 Javadoc，并标明数据库表字段。
- 复杂查询、旧库字段兼容、审核、软删除、权限、审计逻辑必须写中文行内注释。
- Git 提交信息必须使用中文。

---

## 11. 不在本结构中提前引入的内容

以下能力不作为当前结构迁移的一部分：

- Redis。
- RabbitMQ。
- AWS S3。
- AWS SES。
- Elasticsearch。
- 自动部署。
- CI/CD。

这些能力必须等业务场景明确后，按单独计划引入。

---

## 12. 迁移验收标准

结构迁移完成后必须满足：

- Java 源码和测试不再出现 `com.aurora.myblog.v2`。
- Java 源码和测试不再出现 `com.tyb.myblog.v2.modules`。
- 业务模块 HTTP 层不再使用 `api` 包。
- `identity`、`content`、`comment` 直接挂在 `com.tyb.myblog.v2` 下。
- `mvn test` 通过。
- `ArchitectureRulesTest` 能约束新结构。

---

## 13. 后续执行顺序

本决策记录提交后，继续按总计划执行：

1. 迁移 Java 基础包名和目录结构。
2. 更新 ArchUnit 架构约束。
3. 补齐 common 和 infrastructure 注释。
4. 补齐 identity、content、comment 业务注释。
5. 引入 MyBatis-Plus、Hutool、springdoc-openapi 基础设施。
6. 选择低风险模块试迁移 MyBatis-Plus。
7. 制定分模块 JdbcTemplate 替换计划。
8. 完成验收后再恢复业务迁移。
