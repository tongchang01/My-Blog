# 后端 V2 第一版发布前审查实施计划

> **执行要求：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，逐项执行本计划。各步骤使用复选框（`- [ ]`）跟踪进度。

**目标：** 对当前 V2 后端开展一次有证据支撑的只读发布前审查，不预设设计文档或代码实现正确。

**方法：** 以“要求—代码—验证”证据矩阵为基础，逐步形成一份审查报告。先审查文档有效性，再审查架构、契约、安全、持久化、注释和测试；只有经过人工复核的发现才能进入最终的分级报告。

**技术栈：** Java 17、Spring Boot 3.5、Maven、JUnit 5、ArchUnit、MyBatis-Plus、Flyway、H2、MySQL 8、PowerShell、ripgrep

---

## 文件结构

**只读输入：**

- `docs/project-handbook/**/*.md` — 当前产品、API、架构、规则、ADR、状态和工作流声明。
- `docs/superpowers/specs/**/*.md` — 实施阶段已接受、且可能继续影响当前行为的设计。
- `docs/superpowers/reviews/**/*.md` — 历史发现；只能作为证据线索，不得默认仍然有效。
- `MyBlog-springboot-v2/pom.xml` — 依赖、构建插件和测试配置。
- `MyBlog-springboot-v2/src/main/**` — 生产实现、配置、SQL 和 Flyway 迁移。
- `MyBlog-springboot-v2/src/test/**` — 单元、集成、架构、H2 和 Testcontainers 测试。

**新增产物：**

- `docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md` — 唯一的审查结果，包含范围、证据、发现、已验证边界、限制和修复顺序。

执行期间不得修改任何被审查文件。

### 任务 1：冻结审查基线并建立证据台账

**文件：**

- 读取：`docs/superpowers/specs/2026-06-18-backend-v2-release-review-design.md`
- 读取：`MyBlog-springboot-v2/pom.xml`
- 新建：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **步骤 1：记录不可变的 Git 与工具链基线**

在 worktree 根目录运行：

```powershell
git status --short
git branch --show-current
git rev-parse HEAD
git log -5 --oneline
java -version
mvn -version
& 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe' --version
```

预期：当前分支为 `backend-v2-refactor`，创建报告前工作区干净，并取得明确的 Java、Maven、MySQL 版本。如果工作区存在无关变更，必须停止并先报告。

- [ ] **步骤 2：盘点审查范围**

运行：

```powershell
(Get-ChildItem -Recurse -File docs\project-handbook).Count
(Get-ChildItem -Recurse -File MyBlog-springboot-v2\src\main).Count
(Get-ChildItem -Recurse -File MyBlog-springboot-v2\src\test).Count
rg --files MyBlog-springboot-v2/src/main/resources/mapper
rg --files MyBlog-springboot-v2/src/main/resources/db/migration
rg --files MyBlog-springboot-v2/src/test | rg 'Test\.java$'
```

预期：获得完整的文件数量和路径清单。数量只作为范围证据，不作为质量结论。

- [ ] **步骤 3：创建报告骨架**

按以下固定章节创建报告：

```markdown
# 后端 V2 第一版发布前审查

## 1. 总体结论
## 2. 范围、基线与限制
## 3. 设计文档有效性与冲突
## 4. 严重问题（Critical）
## 5. 重要问题（Important）
## 6. 次要问题（Minor）
## 7. 已验证的关键边界
## 8. 未覆盖风险
## 9. 修复批次与顺序
## 10. 前端联调与发布门槛
```

在第 2 节记录准确的提交 SHA、分支、工具版本、使用的数据库、Docker 缺失情况，以及“不得修改被审查文件”的边界。

- [ ] **步骤 4：建立问题模板**

每项发现必须使用以下结构：

```markdown
### [严重度-N] 简短标题

- 类型：设计缺陷 / 实现缺陷 / 测试缺口 / 文档漂移
- 阻塞：前端联调 / 发布 / 不阻塞
- 要求证据：文件与行号，并说明该要求是否有效
- 实现证据：文件与行号
- 验证证据：测试、命令或缺失证据
- 影响：可复现的实际后果
- 建议：最小修复方向，不在本轮实施
```

预期：没有具体影响和文件级证据的问题不得进入报告。

### 任务 2：审查文档效力、时效性与合理性

**文件：**

- 读取：`docs/project-handbook/CLAUDE.md`
- 读取：`docs/project-handbook/INDEX.md`
- 读取：`docs/project-handbook/status.md`
- 读取：`docs/project-handbook/roadmap.md`
- 读取：`docs/project-handbook/m3-preflight-review.md`
- 读取：`docs/project-handbook/pitfalls.md`
- 读取：`docs/project-handbook/product/**/*.md`
- 读取：`docs/project-handbook/api-contract/**/*.md`
- 读取：`docs/project-handbook/arch/**/*.md`
- 读取：`docs/project-handbook/rules/**/*.md`
- 读取：`docs/project-handbook/decisions/**/*.md`
- 修改：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **步骤 1：建立文档状态表**

运行：

```powershell
rg -n "状态：|当前阶段|下一步|已完成|待完成|tests|测试|JdbcTemplate|MyBatis|Redis|Docker|Flyway|V1__init" docs/project-handbook
```

对每份控制性文档，将相关声明标记为 `有效`、`被替代`、`冲突`、`过时`、`不合理` 或 `证据不足`。同时记录最近日期，以及对该声明构成挑战的更新文档或代码。

- [ ] **步骤 2：核验规则引用和文件名**

运行：

```powershell
rg -n "decisions/|rules/|arch/|product/|api-contract/" docs/project-handbook
rg --files docs/project-handbook/decisions docs/project-handbook/rules docs/project-handbook/arch docs/project-handbook/product docs/project-handbook/api-contract
```

预期：识别失效链接、已重命名的 ADR 引用、过时模块名，以及指向已删除类的规则。所有疑似失效引用必须先用文件系统确认，才能写入报告。

- [ ] **步骤 3：质询注释规则本身**

读取 `rules/comment-style.md`、ADR-0011 和当前代码注释规格，分别评估：

1. 全中文注释是否仍是当前有效的语言决定；
2. 强制覆盖类、字段和方法是否比例合理；
3. 规则是否错误鼓励重复代码含义的注释；
4. OpenAPI 描述与 Javadoc 是否被混为一谈；
5. 对 V1 兼容性的引用是否已经过时。

预期：报告必须区分“规则本身不合理”和“代码不符合有效规则”。在规则完成分类前，不统计缺失注释。

- [ ] **步骤 4：质询 SQL 摆放规则本身**

读取 `rules/sql-placement.md`、ADR-0010、持久化策略和 SQL 摆放规格，评估：

1. BaseMapper、Wrapper、注解和 XML 的边界；
2. `projection => XML` 与 `SQL > 10 行 => XML` 是否仍适合作为硬规则；
3. 已过时的 V1 兼容要求；
4. XML 注释要求是否解释当前业务语义，而不是已删除的旧表；
5. Flyway SQL 与运行时 SQL 是否明确分离。

- [ ] **步骤 5：只记录已确认的文档问题**

将文档状态表和已确认冲突写入第 3 至 6 节。历史审查的结论必须根据当前文件重新验证后才能引用。

### 任务 3：审查架构与依赖边界

**文件：**

- 读取：`docs/project-handbook/arch/module-map.md`
- 读取：`docs/project-handbook/rules/package-layout.md`
- 读取：`docs/project-handbook/decisions/0001-modular-monolith.md`
- 读取：`docs/project-handbook/decisions/0003-four-layer-architecture.md`
- 读取：`docs/project-handbook/decisions/0004-six-business-modules.md`
- 读取：`docs/project-handbook/decisions/0012-archunit-guards.md`
- 读取：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
- 读取：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/*.java`
- 修改：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **步骤 1：对比文档模块与实际模块**

运行：

```powershell
Get-ChildItem MyBlog-springboot-v2\src\main\java\com\tyb\myblog\v2 -Directory | Select-Object -ExpandProperty Name
rg -n "identity|content|comment|system|stats|common|infrastructure|common-infra" docs/project-handbook/arch docs/project-handbook/decisions docs/project-handbook/rules
```

预期：形成一份文档模块、实际包和 ArchUnit 覆盖情况的对照矩阵。

- [ ] **步骤 2：运行可执行架构守护**

在 `MyBlog-springboot-v2/` 中运行：

```powershell
mvn -Dtest=ArchitectureRulesTest test
```

预期：记录准确的测试数量和失败数。测试通过只能证明已编码的规则成立，不能证明规则完整。

- [ ] **步骤 3：搜索未被守护的依赖违规**

运行定向搜索：

```powershell
rg -n "^import com\.tyb\.myblog\.v2\.(identity|content|comment|system|stats)\.(domain|infrastructure|web)" MyBlog-springboot-v2/src/main/java
rg -n "BaseMapper|Mapper<|Lambda(Query|Update)Wrapper|JdbcTemplate" MyBlog-springboot-v2/src/main/java/**/application MyBlog-springboot-v2/src/main/java/**/web
rg -n "org\.springframework|com\.baomidou|jakarta\.servlet" MyBlog-springboot-v2/src/main/java/**/domain
```

如果 PowerShell 无法展开 `**`，则对 `src/main/java` 重新运行并手工筛选路径。检查每个匹配项；单独出现 import 只能算线索，不能直接算问题。

- [ ] **步骤 4：追踪每个跨模块网关**

对每个实际跨模块 import，确认调用通过目标模块面向外部的 application 契约进入，且没有暴露其持久化 Entity、Repository、Mapper、Web DTO 或框架类型。

- [ ] **步骤 5：记录架构问题与已验证边界**

将违规写入对应严重度章节；将人工确认通过的边界写入第 7 节，避免报告只有问题而没有已验证结论。

### 任务 4：审查 API 契约、领域规则与授权边界

**文件：**

- 读取：`docs/project-handbook/api-contract/*.md`
- 读取：`docs/project-handbook/product/business-rules.md`
- 读取：`docs/project-handbook/product/use-cases.md`
- 读取：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/web/**/*.java`
- 读取：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/application/**/*.java`
- 读取：对应的 Controller、OpenAPI、Application 和集成测试
- 修改：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **步骤 1：盘点已实现接口**

运行：

```powershell
rg -n "@(RequestMapping|GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)|@PreAuthorize" MyBlog-springboot-v2/src/main/java
```

建立包含 HTTP 方法、路径、Controller、所需角色、请求类型、响应类型、文档契约和测试证据的矩阵。

- [ ] **步骤 2：核验响应与错误契约**

沿 Controller、Application Service、异常处理器和测试，追踪具有代表性的公开、认证、ADMIN 和 DEMO 链路。检查状态码、`ApiResponse`、分页字段、参数校验错误、冲突错误和内部错误脱敏。

- [ ] **步骤 3：核验角色与可见性边界**

检查实现并交叉核对以下测试：

- 匿名公开访问；
- ADMIN 写操作；
- DEMO 允许读取、禁止写入；
- 仅 ADMIN 可访问的敏感读取；
- DRAFT、PRIVATE、PASSWORD、PUBLISHED、SCHEDULED 文章可见性；
- 评论审计字段和治理操作。

预期：每个宣称成立的边界都有实现与测试证据，否则记录为测试缺口。

- [ ] **步骤 4：核验确定性契约细节**

根据契约、代码和测试检查分页默认值与上限、稳定的二级排序、多语言回退、标题补齐、30 天补零、平均日 UV 分母、`dailyUvSum` 以及 TOP 并列排序。

- [ ] **步骤 5：记录契约问题**

只有确认控制性契约仍然有效后，才能报告实现偏差。必须区分“契约已经过时”和“实现违反契约”。

### 任务 5：审查安全、密钥与环境配置

**文件：**

- 读取：`docs/project-handbook/rules/security-baseline.md`
- 读取：`docs/project-handbook/arch/auth-flow.md`
- 读取：`MyBlog-springboot-v2/src/main/resources/application*.yml`
- 读取：安全、认证、限流、上传、代理和启动校验相关生产代码与测试
- 修改：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **步骤 1：扫描密钥和不安全默认值模式**

运行：

```powershell
rg -n -i "password\s*[:=]|secret\s*[:=]|api[-_]?key\s*[:=]|token\s*[:=]|root|allow-all|permitAll|allowed-origins|\*" MyBlog-springboot-v2/src/main MyBlog-springboot-v2/pom.xml
```

结合上下文检查每个匹配项。测试夹具和环境变量占位符不属于密钥泄露。

- [ ] **步骤 2：追踪认证生命周期**

追踪登录 → access token 校验 → refresh → 登出 → 修改密码。核验 token 类型隔离、issuer/过期/version 校验、refresh 哈希存储、撤销、事务边界和审计更新。

- [ ] **步骤 3：审查公开端点与 HTTP 方法匹配**

对比公开端点配置、Spring Security matcher、Controller 路径、HTTP 方法和安全测试，确认不存在仅按路径匹配而误放行其他方法的情况。

- [ ] **步骤 4：审查请求来源安全**

检查 CORS、可信代理处理、转发头、客户端 IP 解析、登录/解锁/评论限流和日志。确认用户可控请求头不会被静默当作可信身份或审计数据。

- [ ] **步骤 5：审查附件与不可信内容处理**

追踪附件上传校验、存储路径构造、MIME/大小检查、公开访问、删除/引用检查、评论 Markdown 清洗和文章正文渲染假设。

- [ ] **步骤 6：记录安全问题**

Critical 必须存在具体可达路径或直接密钥暴露。未经证明的担忧放入第 8 节作为限制，不得升级为缺陷。

### 任务 6：审查持久化、SQL 风格、Flyway 与 MySQL 行为

**文件：**

- 读取：`docs/project-handbook/rules/sql-placement.md`
- 读取：`docs/project-handbook/arch/schema-design.md`
- 读取：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/infrastructure/persistence/**/*.java`
- 读取：`MyBlog-springboot-v2/src/main/resources/mapper/**/*.xml`
- 读取：`MyBlog-springboot-v2/src/main/resources/db/migration/*.sql`
- 读取：持久化和迁移测试
- 修改：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **步骤 1：盘点所有运行时 SQL 机制**

运行：

```powershell
rg -n "@(Select|Insert|Update|Delete)\b|JdbcTemplate|NamedParameterJdbcTemplate|createNativeQuery|SELECT |INSERT |UPDATE |DELETE " MyBlog-springboot-v2/src/main/java
rg -n "<(select|insert|update|delete)\b|<foreach|<if|<where|<choose|JOIN|GROUP BY|ORDER BY|LIMIT" MyBlog-springboot-v2/src/main/resources/mapper
```

将每个匹配项归类为 BaseMapper/Wrapper、注解 SQL、XML、Flyway 或被禁止的 Java SQL。人工确认 join、动态筛选、聚合、分页、projection 和批量 IN 的放置方式。

- [ ] **步骤 2：核验 Mapper 与 XML 关联**

逐个 XML 文件核验：

- 路径为 `resources/mapper/{module}/`；
- 文件名与 Mapper 接口一致；
- namespace 与接口全限定名一致；
- statement id 与方法名一致；
- result type/map 与 projection 字段一致；
- 复杂业务规则和非显然排序具有有效中文注释。

- [ ] **步骤 3：审查 SQL 安全性与确定性**

检查 `${...}` 替换、用户可选排序字段、LIKE 转义、NULL 排序、并列时排序、分页 count 查询、空集合、批量大小和潜在 N+1 循环。

运行：

```powershell
rg -n '\$\{' MyBlog-springboot-v2/src/main/resources/mapper MyBlog-springboot-v2/src/main/java
rg -n "ORDER BY|LIMIT|OFFSET|LIKE|IN \(|FOR UPDATE" MyBlog-springboot-v2/src/main/resources/mapper MyBlog-springboot-v2/src/main/java
```

- [ ] **步骤 4：审查 Flyway 历史与 Schema 不变量**

运行：

```powershell
rg -n -i "foreign key|timestamp|on update|auto_increment|datetime|unique|index|key " MyBlog-springboot-v2/src/main/resources/db/migration
git log --follow --oneline -- MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql
```

根据 Git 历史核验迁移不可变性、V1/V2 顺序、无数据库外键、ID 策略、审计/软删除列、索引对实际查询条件的支持，以及与 Java 时间值的精度兼容性。

- [ ] **步骤 5：使用本地 MySQL 只读确认 Schema**

使用环境提供的凭据连接；不得把密码写入报告或命令历史。只运行元数据读取：

```sql
SELECT VERSION();
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SHOW TABLES;
SHOW CREATE TABLE t_article;
SHOW CREATE TABLE t_refresh_token;
SHOW CREATE TABLE t_page_view_daily;
```

预期：在不执行 DDL/DML 变更的情况下记录 Schema 状态。

- [ ] **步骤 6：记录持久化问题**

对于风格问题，同时引用任务 2 的规则有效性结论和具体 SQL；对于正确性问题，必须包含查询、测试或 MySQL 证据。

### 任务 7：审查注释质量与 OpenAPI 描述

**文件：**

- 读取：任务 2 确认有效的注释要求
- 读取：`MyBlog-springboot-v2/src/main/java/**/*.java`
- 读取：`MyBlog-springboot-v2/src/main/resources/mapper/**/*.xml`
- 读取：`MyBlog-springboot-v2/src/test/java/**/*.java`
- 修改：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **步骤 1：盘点注释敏感类型**

运行：

```powershell
rg -n "^(public )?(class|record|interface|enum) |@(RestController|Configuration|ConfigurationProperties|TableName|Schema)" MyBlog-springboot-v2/src/main/java
rg -n "@Schema\(description|/\*\*|//|<!--" MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
```

按 Entity、DTO/Command/Query/Response、Controller、Application Service、Enum、Mapper/Repository、配置、XML 和特殊业务逻辑分组。

- [ ] **步骤 2：审查每个模块和分层的代表性样本**

审查所有高风险公开契约和持久化类型，再从其余重复类型中抽取并记录样本。检查注释是否解释业务含义、约束、权限、状态、排序、时间或生产风险。

- [ ] **步骤 3：识别误导性和冗余注释**

查找以下注释：

- 只翻译类名、方法名或字段名；
- 描述已删除的 V1 兼容行为；
- 与当前代码冲突；
- 重复 OpenAPI 文本但没有增加维护价值；
- 声称的行为没有代码或测试保护。

- [ ] **步骤 4：对比 Javadoc 与 OpenAPI 语义**

对于通过 OpenAPI 暴露的请求/响应字段，对比 Javadoc、校验注解、`@Schema`、JSON 名称和实际 API 测试。语义冲突优先于单纯缺失进行报告。

- [ ] **步骤 5：按影响程度记录注释问题**

安全、状态流转、审计、时间、SQL 或生产配置缺少必要说明时可判为 Important。机械式注释覆盖和个别过时措辞通常判为 Minor。不得每个字段单列问题，应按一致规则和模块归组。

### 任务 8：审查测试、运行证据、依赖与发布缺口

**文件：**

- 读取：`docs/project-handbook/rules/testing-policy.md`
- 读取：`MyBlog-springboot-v2/pom.xml`
- 读取：`MyBlog-springboot-v2/src/test/**/*.java`
- 读取：`MyBlog-springboot-v2/src/test/resources/application-test.yml`
- 修改：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **步骤 1：建立测试与关键行为映射**

运行：

```powershell
rg -n "@Test|@ParameterizedTest|@SpringBootTest|@WebMvcTest|@EnabledIf|@Disabled|Testcontainers|MySQLContainer" MyBlog-springboot-v2/src/test
```

将测试映射到认证生命周期、ADMIN/DEMO 边界、内容可见性、评论状态流转、附件安全、统计聚合、复杂 SQL、Flyway 和配置启动。

- [ ] **步骤 2：检查测试质量与隔离性**

检查关键测试是否断言真实输出和状态，而不是只验证 mock 调用。检查共享数据库清理、顺序假设、固定时钟、并行安全、条件跳过，以及仅适用于 H2 的 SQL/类型断言。

- [ ] **步骤 3：运行完整 H2 测试套件**

在 `MyBlog-springboot-v2/` 中运行：

```powershell
mvn clean test
```

预期：记录准确的测试数、失败数、错误数、跳过数、耗时，以及跳过测试的名称和原因。出现失败时，在完成分类前不得给出审查结论。

- [ ] **步骤 4：运行本地 MySQL 广泛回归**

只使用用户授权、可清理的 `myblog_v2_dev` 测试库；这些集成测试可能插入、更新、删除或重置测试数据。禁止对任何非测试 Schema 运行。通过进程环境变量提供数据源 URL、用户名、密码、MySQL 驱动和 Flyway 开关，不得把凭据写入报告。运行：

```powershell
mvn '-Dtest=**/*Test,!FlywayMigrationTest,!RefreshSessionTransactionIntegrationTest,!DatabasePasswordAccountRepositoryTest,!DatabaseUserProfileRepositoryTest' test
```

预期：记录准确结果，并明确说明排除四个 H2 专用夹具类的原因。确认 `ArticleIntegrationTest` 以及持久化/集成测试实际在 MySQL 上执行。

- [ ] **步骤 5：检查依赖与构建策略**

运行：

```powershell
mvn dependency:tree
mvn help:effective-pom -Doutput=target/effective-pom-review.xml
```

检查直接依赖、重复技术栈、Flyway MySQL 支持、测试依赖作用域、Maven Enforcer、编译级别和依赖收敛。检查完成后删除 `target/effective-pom-review.xml`，因为它是生成证据，不是审查交付物。

- [ ] **步骤 6：记录运行验证限制**

记录 Docker/Testcontainers 缺口、未执行的生产 Profile 启动、外部邮件/存储依赖，以及只在本地 MySQL 上证明的行为。不得把未执行的检查称为“通过”。

### 任务 9：复核发现并完成审查报告

**文件：**

- 读取：`docs/superpowers/specs/2026-06-18-backend-v2-release-review-design.md`
- 读取：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md` 中收集的全部证据
- 修改：`docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md`

- [ ] **步骤 1：对每项发现去重并进行反证审查**

对每个候选问题逐项确认：

1. 引用的要求是否仍然有效且合理？
2. 实现证据是否完整并包含上下文？
3. 是否存在测试或运行结果可以反驳该担忧？
4. 影响是否具体，而不是个人风格偏好？
5. 严重度是否符合设计中的分级标准？

删除或降级无法通过上述检查的问题。

- [ ] **步骤 2：核验全部证据链接与行号**

按引用行号重新打开每个文件。Critical 和 Important 必须同时具备要求、实现和验证证据；当运行证据不适用时，Minor 文档漂移可以只使用两类证据。

- [ ] **步骤 3：编写发布结论**

结论必须分别回答：

- 现在是否可以开始前端联调？
- 当前后端是否已经达到发布候选状态？
- 哪些问题阻塞前端联调？
- 哪些问题只阻塞生产发布？
- 因 Docker 或生产基础设施不可用，还存在哪些验证限制？

- [ ] **步骤 4：编排修复顺序**

将修复拆成可独立提交的批次，并按以下顺序排列：

1. Critical 安全与数据正确性；
2. 前端契约阻塞项；
3. MySQL、Flyway 和事务正确性；
4. Important 架构与测试缺口；
5. 文档、注释和低风险清理。

每个批次都要列出涉及文件、验证命令，以及是否改变行为。本轮不得实施任何批次。

- [ ] **步骤 5：验证审查范围和报告质量**

运行：

```powershell
git diff --check
git status --short
git diff --stat
rg -n "TBD|TODO|待补|稍后确认|可能有问题" docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md
```

预期：执行期间只有审查报告处于未提交状态；不存在占位结论；任何被审查源码或既有设计文档均未修改。

- [ ] **步骤 6：提交最终报告**

运行：

```powershell
git add -- docs/superpowers/reviews/2026-06-18-backend-v2-release-review.md
git diff --cached --check
git diff --cached --stat
git commit -m "完成后端V2第一版发布前审查"
```

预期：生成一个只包含报告的中文提交。保留当前分支和 worktree，不合并、不推送。
