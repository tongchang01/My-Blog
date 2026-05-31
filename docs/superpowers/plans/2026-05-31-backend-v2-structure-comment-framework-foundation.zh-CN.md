# 后端 V2 项目结构、注释规范与框架基础改造实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:executing-plans` 按任务逐个执行。步骤使用 checkbox（`- [ ]`）语法跟踪状态。用户要求“一个任务一个任务来”，每个任务完成并提交后应停下汇报，不要一次性把所有任务做完。

**目标：** 在继续迁移新业务前，先把后端 V2 的项目结构、注释规范和基础框架选型落地，避免后续业务代码继续堆在旧包名、旧持久层和无注释状态上。

**架构：** 后端 V2 保持模块化单体。基础包名从 `com.aurora.myblog.v2` 迁移为 `com.tyb.myblog.v2`；业务模块直接挂在 `v2` 下，去掉 `modules`；HTTP 入口层从 `api` 改名为 `web`。持久层主 ORM 统一转向 MyBatis-Plus，Hutool 和 springdoc-openapi 作为基础能力引入。已有 JdbcTemplate 代码按模块迁移，不在一次提交里粗暴全量替换。

**Tech Stack:** Java 17、Spring Boot 3.5.x、Maven、Spring MVC、Spring Security、MyBatis-Plus、MySQL、H2、Flyway Test Migration、Hutool、springdoc-openapi、JUnit 5、ArchUnit。

---

## 1. 背景和当前问题

当前后端 V2 已经完成一批基础能力和部分业务迁移，但继续往后开发前，我需要先处理三个会影响长期维护的基础问题：

1. **项目结构需要定型。** 当前包名是 `com.aurora.myblog.v2.modules.content.api` 这类结构，存在 `aurora` 旧品牌痕迹、`modules` 语义偏空、`api` 容易和接口契约混淆的问题。
2. **注释规范需要落地。** 当前已有代码大多缺少 Javadoc 和业务注释，尤其是 Entity、DTO、业务方法、状态流转、旧库兼容逻辑没有形成统一说明。
3. **框架选型需要先执行基础改造。** 已经决定后续统一 MyBatis-Plus、引入 Hutool、使用 springdoc-openapi。如果继续扩大 JdbcTemplate 代码，后面迁移成本会更高。

因此，后续不应继续直接推进新业务迁移，而应先完成本计划中的基础改造。

---

## 2. 已冻结的结构决策

### 2.1 基础包名

从：

```text
com.aurora.myblog.v2
```

迁移为：

```text
com.tyb.myblog.v2
```

原因：

- `aurora` 是旧项目痕迹，不适合作为 V2 长期基础包名。
- `tyb` 更适合作为当前项目的个人/组织标识。
- 越早改包名，后续成本越低。

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

每个业务模块内部采用：

```text
content
├── web                 // Controller、Request、Response、Web DTO
├── application         // 用例编排、Command、Query、Application Service
├── domain              // 业务规则、状态、领域模型、领域服务
└── infrastructure      // MyBatis-Plus Entity、Mapper、Repository 实现、外部适配
```

说明：

- `web` 替代当前 `api`，只表示 HTTP 接入层。
- `application` 负责用例编排，不直接写 SQL。
- `domain` 负责业务语义和规则，不依赖 Spring Web、MyBatis、数据库表结构。
- `infrastructure` 负责技术实现，包括 MyBatis-Plus、数据库实体、Mapper、外部服务适配。

---

## 3. 本计划执行原则

- 每个任务单独提交。
- 每个任务完成后停下汇报，等待用户确认后再开始下一任务。
- 结构迁移和行为改动分开提交。
- 注释补齐和业务逻辑改动分开提交。
- MyBatis-Plus 基础设施引入和 JdbcTemplate 业务迁移分开提交。
- 不在本计划中引入 Redis、RabbitMQ、AWS SDK、Elasticsearch。
- RabbitMQ、Redis、AWS SDK 只在后续明确业务场景出现时按单独计划引入。

---

## 4. 参考文档

执行本计划前必须阅读：

- `docs/superpowers/specs/2026-05-31-backend-v2-technology-decisions.zh-CN.md`
- `docs/superpowers/specs/2026-05-31-backend-v2-code-comment-standards.zh-CN.md`
- `docs/superpowers/specs/2026-05-22-myblog-v2-refactor-design.md`

---

## 5. 阶段总览

| 阶段 | 名称 | 目标 | 是否阻塞后续业务迁移 |
| --- | --- | --- | --- |
| Phase 1 | 项目结构决策落地 | 包名、目录、ArchUnit 规则统一 | 是 |
| Phase 2 | 注释规范补齐 | 当前已有 V2 代码补上必要 Javadoc 和业务注释 | 是 |
| Phase 3 | 框架基础设施引入 | MyBatis-Plus、Hutool、springdoc-openapi 基础配置落地 | 是 |
| Phase 4 | 持久层试迁移 | 选择低风险模块验证 MyBatis-Plus 迁移方式 | 是 |
| Phase 5 | 分模块替换 JdbcTemplate | identity、content、comment 逐步转为 MyBatis-Plus | 是 |
| Phase 6 | 恢复业务迁移 | 继续后端 V2 未完成业务能力 | 否 |

---

## Task 1: 固化项目结构决策文档

**Files:**

- Create: `docs/superpowers/specs/2026-05-31-backend-v2-package-structure-decisions.zh-CN.md`
- Read: `docs/superpowers/specs/2026-05-31-backend-v2-technology-decisions.zh-CN.md`
- Read: `docs/superpowers/specs/2026-05-31-backend-v2-code-comment-standards.zh-CN.md`

- [ ] **Step 1: 新增项目结构决策文档**

写清楚以下决策：

```text
基础包名：com.tyb.myblog.v2
去掉 modules
api 改为 web
业务模块直接挂在 v2 下
模块内部结构：web / application / domain / infrastructure
```

- [ ] **Step 2: 写清楚每一层允许放什么**

必须覆盖：

- `common`
- `infrastructure`
- `identity`
- `content`
- `comment`
- `system`
- `web`
- `application`
- `domain`
- `infrastructure`

- [ ] **Step 3: 写清楚每一层禁止做什么**

必须明确：

- `web` 不直接访问 Mapper。
- `domain` 不依赖 Spring Web。
- `application` 不拼 SQL。
- `infrastructure` 不反向污染 domain。
- `common` 不放业务模块私有逻辑。

- [ ] **Step 4: 提交项目结构决策文档**

Run:

```powershell
git add docs/superpowers/specs/2026-05-31-backend-v2-package-structure-decisions.zh-CN.md
git commit -m "记录后端V2项目结构决策"
```

完成后停下汇报。

---

## Task 2: 迁移 Java 基础包名和目录结构

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/**`
- Modify: `MyBlog-springboot-v2/src/test/java/**`

- [ ] **Step 1: 执行前确认工作区干净**

Run:

```powershell
git status --short
```

Expected: 无输出。

- [ ] **Step 2: 将基础包名从 `com.aurora.myblog.v2` 改为 `com.tyb.myblog.v2`**

需要修改：

- main Java 源码目录。
- test Java 源码目录。
- 所有 `package` 声明。
- 所有 `import com.aurora.myblog.v2...`。
- 启动类包名。
- 测试类包名。

- [ ] **Step 3: 去掉 `modules` 目录层级**

从：

```text
com/tyb/myblog/v2/modules/content
com/tyb/myblog/v2/modules/comment
com/tyb/myblog/v2/modules/identity
```

迁移为：

```text
com/tyb/myblog/v2/content
com/tyb/myblog/v2/comment
com/tyb/myblog/v2/identity
```

- [ ] **Step 4: 将业务模块内 `api` 改为 `web`**

从：

```text
content/api
comment/api
identity/api
```

迁移为：

```text
content/web
comment/web
identity/web
```

- [ ] **Step 5: 全局搜索确认旧包名不存在**

Run:

```powershell
rg "com\.aurora\.myblog\.v2|\.modules\.|\.api" MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java
```

Expected: 不再出现旧包名、`.modules.`、业务模块 `.api` 包引用。

- [ ] **Step 6: 运行测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 7: 提交结构迁移**

Run:

```powershell
git add MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java
git commit -m "调整后端V2基础包名和模块结构"
```

完成后停下汇报。

---

## Task 3: 更新 ArchUnit 架构约束

**Files:**

- Modify: `MyBlog-springboot-v2/src/test/java/**/ArchitectureRulesTest.java`

- [ ] **Step 1: 检查现有 ArchUnit 规则**

Run:

```powershell
Get-Content -Raw -Encoding UTF8 MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java
```

- [ ] **Step 2: 更新包名规则**

规则应基于新包名：

```text
com.tyb.myblog.v2
```

- [ ] **Step 3: 增加分层约束**

至少覆盖：

- `web` 不访问 `infrastructure.persistence.mapper`。
- `domain` 不依赖 `web`、`application`、`infrastructure`。
- `application` 可以依赖 `domain`，但不依赖 MyBatis-Plus Mapper。
- 业务模块不能直接访问其他模块的 infrastructure。

- [ ] **Step 4: 运行 ArchUnit 测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=ArchitectureRulesTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交架构约束**

Run:

```powershell
git add MyBlog-springboot-v2/src/test/java
git commit -m "强化后端V2项目结构约束"
```

完成后停下汇报。

---

## Task 4: 补齐 common 和 infrastructure 注释

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/**`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/infrastructure/**`
- Read: `docs/superpowers/specs/2026-05-31-backend-v2-code-comment-standards.zh-CN.md`

- [ ] **Step 1: 列出缺少 Javadoc 的类**

Run:

```powershell
rg "^public|^class|^record|^interface|^enum" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/infrastructure -n
```

- [ ] **Step 2: 补齐配置、安全、错误处理、Web 支撑类注释**

必须覆盖：

- 类职责。
- 配置项用途。
- 安全边界。
- 默认值风险。
- 客户端 IP、User-Agent、JWT 等关键逻辑。

- [ ] **Step 3: 运行 common 相关测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=BackendPropertiesTest,GlobalExceptionHandlerTest,SecurityConfigTest,JwtTokenServiceTest,JwtAuthenticationFilterTest,JwtPropertiesTest,UserAgentResolverTest,ClientIpResolverTest,PageResponseTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 4: 提交 common 注释补齐**

Run:

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/infrastructure
git commit -m "补齐后端V2通用基础设施注释"
```

完成后停下汇报。

---

## Task 5: 补齐 identity、content、comment 业务注释

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/**`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/**`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/**`
- Read: `docs/superpowers/specs/2026-05-31-backend-v2-code-comment-standards.zh-CN.md`

- [ ] **Step 1: identity 注释补齐**

覆盖：

- 登录认证。
- 当前用户资料。
- 角色、菜单。
- 登录审计。
- 旧库字段兼容。

- [ ] **Step 2: 运行 identity 测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=AuthControllerTest,AuthServiceTest,DatabaseCurrentUserProfileReaderTest,AdminIdentityControllerTest,DatabaseUserCredentialReaderTest,ConfiguredUserCredentialReaderTest,RoleNameMapperTest,DatabaseLoginAuditRecorderTest,DatabaseUserMenuReaderTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 3: content 注释补齐**

覆盖：

- 文章列表。
- 文章详情。
- 分类、标签。
- 归档、置顶、推荐。
- 受保护文章访问。

- [ ] **Step 4: 运行 content 测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=ContentArticleControllerTest,ContentCatalogControllerTest,DatabaseArticleReaderTest,DatabaseContentCatalogReaderTest,SignedArticleAccessTokenServiceTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: comment 注释补齐**

覆盖：

- 评论提交。
- 评论展示。
- 后台审核。
- 删除、恢复。
- 客户端 IP、User-Agent 审计。
- 软删除和审核状态兼容。

- [ ] **Step 6: 运行 comment 测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=CommentControllerTest,AdminCommentControllerTest,DatabaseAdminCommentModeratorTest,DatabaseCommentWriterTest,DatabaseCommentReaderTest,DatabaseAdminCommentReaderTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 7: 提交业务注释补齐**

Run:

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment
git commit -m "补齐后端V2业务模块注释"
```

完成后停下汇报。

---

## Task 6: 引入 MyBatis-Plus、Hutool、springdoc-openapi 基础设施

**Files:**

- Modify: `MyBlog-springboot-v2/pom.xml`
- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/**`
- Create or Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/**`

- [ ] **Step 1: 在 `pom.xml` 引入依赖**

新增：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.12</version>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.36</version>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.8</version>
</dependency>
```

注：执行时需要确认版本和 Spring Boot 3.5.x 兼容。如果 Maven 解析失败，优先查官方兼容版本，不要随意降级到 Boot 2 生态依赖。

- [ ] **Step 2: 新增 MyBatis-Plus 配置**

配置内容：

- Mapper 扫描。
- 分页插件。
- 明确 XML mapper 路径。
- Javadoc 说明配置用途。

- [ ] **Step 3: 新增 springdoc 基础配置**

配置内容：

- 文档标题。
- V2 API 描述。
- 当前版本。
- 暂不暴露生产安全策略，后续部署阶段再收口。

- [ ] **Step 4: 新增最小启动测试**

验证：

- Spring Context 可加载。
- MyBatis-Plus 配置可加载。
- springdoc 配置不破坏安全配置。

- [ ] **Step 5: 运行全量测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 6: 提交框架基础设施**

Run:

```powershell
git add MyBlog-springboot-v2/pom.xml MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java
git commit -m "引入后端V2框架基础设施"
```

完成后停下汇报。

---

## Task 7: 选择低风险模块试迁移 MyBatis-Plus

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/**` 或 `content/**`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/**`

- [ ] **Step 1: 选择第一个试迁移目标**

优先级：

1. `DatabaseContentCatalogReader`
2. `DatabaseCurrentUserProfileReader`
3. `DatabaseUserMenuReader`

选择标准：

- SQL 较简单。
- 业务风险低。
- 测试覆盖已有。
- 能体现 Entity、Mapper、Repository 的放置方式。

- [ ] **Step 2: 新增 Entity**

Entity 必须：

- 放在模块 `infrastructure.persistence.entity`。
- 使用 MyBatis-Plus 注解。
- 每个字段都有 Javadoc，写明对应表字段和中文含义。

- [ ] **Step 3: 新增 Mapper**

Mapper 必须：

- 放在模块 `infrastructure.persistence.mapper`。
- 继承 MyBatis-Plus `BaseMapper`。
- 复杂查询使用 XML 或明确注释。

- [ ] **Step 4: 替换对应 JdbcTemplate 实现**

要求：

- application/domain 接口不变。
- Controller 响应不变。
- 测试断言不因实现替换而弱化。

- [ ] **Step 5: 运行目标模块测试**

根据迁移模块运行对应测试。

Expected: `BUILD SUCCESS`。

- [ ] **Step 6: 提交试迁移**

Run:

```powershell
git add MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java
git commit -m "试迁移后端V2持久层到MyBatis-Plus"
```

完成后停下汇报。

---

## Task 8: 制定分模块 JdbcTemplate 替换计划

**Files:**

- Create: `docs/superpowers/plans/YYYY-MM-DD-backend-v2-mybatis-plus-module-migration.zh-CN.md`

- [ ] **Step 1: 盘点所有 JdbcTemplate 使用点**

Run:

```powershell
rg "JdbcTemplate" MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java -n
```

- [ ] **Step 2: 按模块分组**

分为：

- identity
- content
- comment
- common/infrastructure

- [ ] **Step 3: 为每个模块写迁移顺序**

建议：

1. identity 基础读取。
2. content 分类标签读取。
3. content 文章读取。
4. comment 前台读取。
5. comment 写入、审核、删除、恢复。
6. login audit 和 comment audit 补充。

- [ ] **Step 4: 写清楚每个模块的测试命令**

每个模块都必须写明确 Maven 命令和预期结果。

- [ ] **Step 5: 提交分模块迁移计划**

Run:

```powershell
git add docs/superpowers/plans/YYYY-MM-DD-backend-v2-mybatis-plus-module-migration.zh-CN.md
git commit -m "新增后端V2持久层分模块迁移计划"
```

完成后停下汇报。

---

## Task 9: 继续推进业务迁移前的验收

**Files:**

- Modify: `docs/superpowers/plans/2026-05-31-backend-v2-structure-comment-framework-foundation.zh-CN.md`

- [ ] **Step 1: 全量搜索旧结构残留**

Run:

```powershell
rg "com\.aurora\.myblog\.v2|\.modules\.|modules/" MyBlog-springboot-v2 docs
```

Expected: 只允许在历史计划、历史复盘或旧说明中出现；Java 源码和测试中不得出现。

- [ ] **Step 2: 全量搜索 `api` 包残留**

Run:

```powershell
rg "package .*\.api;|import .*\.api\." MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java
```

Expected: 无输出。

- [ ] **Step 3: 全量测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 4: local profile 冒烟**

Run:

```powershell
cd MyBlog-springboot-v2
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Expected: 服务启动成功，不因包扫描、Mapper 扫描、配置变更失败。

- [ ] **Step 5: 更新本计划实施记录**

在本文末尾追加：

```markdown
## 实施记录

- 项目结构迁移：
- 注释补齐：
- 框架基础设施：
- MyBatis-Plus 试迁移：
- 全量测试：
- local 冒烟：
- 后续恢复业务迁移的起点：
```

- [ ] **Step 6: 提交验收记录**

Run:

```powershell
git add docs/superpowers/plans/2026-05-31-backend-v2-structure-comment-framework-foundation.zh-CN.md
git commit -m "同步后端V2基础改造验收记录"
```

完成后停下汇报。

---

## 6. 完成本计划后的业务推进方向

本计划完成后，再继续推进后端 V2 业务迁移。后续方向按以下顺序走：

1. **完成 MyBatis-Plus 分模块替换。** 不再继续新增 JdbcTemplate 实现。
2. **补强现有业务能力。** 优先完善文章、评论、后台管理、身份权限等核心业务。
3. **继续表结构重构。** 针对旧表字段命名、状态值、索引、审计字段做分批兼容迁移。
4. **进入前后台 Vue3 联调准备。** 后端 API、DTO 注释和 springdoc 文档要服务于前后台重构。
5. **按需引入 Redis。** 只在验证码、限流、热点缓存、统计等场景明确时引入。
6. **按需引入 RabbitMQ。** 只用于评论邮件通知、订阅通知、文件后处理、统计异步化等非核心链路。
7. **按业务模块引入 AWS S3/SES。** 文件存储、邮件发送做到时再接入，并通过 infrastructure adapter 隔离。
8. **暂不引入 Elasticsearch。** 文章搜索先用 MySQL，等搜索需求明确后再评估。
9. **最后再处理部署自动化。** 当前阶段不把自动部署、CI/CD、服务器改造混进业务重构。

---

## 7. 风险和回避策略

| 风险 | 影响 | 回避策略 |
| --- | --- | --- |
| 包名迁移影响大量文件 | 容易漏改 import 或测试包名 | 一次性搜索旧包名，跑全量测试 |
| `api` 改 `web` 后测试引用失效 | Controller 测试编译失败 | 结构迁移提交只做包名目录变更，不改业务行为 |
| MyBatis-Plus 引入后与 JdbcTemplate 共存 | 短期技术栈混合 | 明确过渡期，后续分模块替换 |
| 注释补齐混入重构 | diff 过大，难 review | 注释任务不改变行为 |
| Entity 字段注释不准确 | 误导后续维护 | 以数据库表字段、已有 SQL、业务测试为依据 |
| springdoc 与安全配置冲突 | 文档接口无法访问或暴露过多 | 基础配置先保守，部署阶段再收口 |

---

## 8. 计划完成标准

本计划完成时必须满足：

- Java 源码和测试不再使用 `com.aurora.myblog.v2`。
- Java 源码和测试不再使用 `com.tyb.myblog.v2.modules`。
- 业务模块 HTTP 层使用 `web`，不再使用 `api`。
- 注释规范已补到当前已有主要类、字段、方法和复杂业务逻辑。
- MyBatis-Plus、Hutool、springdoc-openapi 已完成基础引入。
- 至少一个低风险持久层模块完成 MyBatis-Plus 试迁移。
- JdbcTemplate 分模块替换计划已写好。
- `mvn test` 通过。
- local profile 能启动。
- 后续业务迁移恢复点清晰。
