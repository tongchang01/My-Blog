# 后端 V2 第一版发布前审查

## 1. 总体结论

当前后端的核心业务实现与数据库回归已经达到“可进入前端骨架开发”的程度，但不能把它表述为完整产品 V2 已具备发布条件。

- 未发现 Critical 问题；H2 全量 613 项和本地 MySQL 广泛回归 597 项均为 0 失败、0 错误。
- 前端本地联调前必须先修复 local Profile 遗漏匿名端点，否则站点配置、文章评论和留言板会在认证层返回 401。
- PASSWORD 文章完整解锁链路尚未实现，ADMIN/DEMO 敏感字段可见性也没有形成一致设计，因此完整产品发布范围尚未冻结。
- 架构、注释和 OpenAPI 的可执行守护弱于文档声明；这些问题多数不阻塞前端骨架，但应在首版发布冻结前分批处理。

## 2. 范围、基线与限制

### 2.1 Git 与工具链基线

- 审查分支：`backend-v2-refactor`
- 基线提交：`34539d547f8dd2633ec51cb8a101c75346b4ff57`
- Java：Oracle JDK `17.0.11`
- Maven：`3.9.6`
- MySQL 客户端：MySQL Community Server `8.0.35`
- 操作系统：Windows 11 `10.0`，x86_64
- 审查开始时工作区：干净

### 2.2 文件范围

- `docs/project-handbook/`：80 个文件
- `MyBlog-springboot-v2/src/main/`：464 个文件
- `MyBlog-springboot-v2/src/test/`：175 个文件，其中 164 个 `*Test.java`，共 174 个 Java 测试源文件
- MyBatis Mapper XML：14 个，覆盖 common、identity、content、comment、system、stats
- Flyway 迁移：`V1__init.sql`、`V2__backfill_user_info.sql`

### 2.3 审查边界

- 本轮只新增本报告，不修改后端代码、测试或既有设计文档。
- 本地 MySQL 验证只使用用户授权的 `myblog_v2_dev` 测试库；凭据不写入报告。
- 当前环境未安装 Docker，Testcontainers 条件测试不能执行；相关风险单独记录，不能视为已通过。

## 3. 设计文档有效性与冲突

| 文档 | 状态 | 结论 |
|---|---|---|
| `status.md`、`roadmap.md` | 有效 | 已更新到 2026-06-18，明确 M3 后端结束、下一步进入 M4 前端骨架。 |
| `CLAUDE.md` | 部分过时 | 规则入口仍有效，但阶段说明仍停留在 M3 准备和 identity 登录起步，与当前状态冲突。 |
| `m3-preflight-review.md` | 历史基线 | 适合作为 M3 准入和历史问题证据，不应覆盖更新后的 `status.md`；其中测试数量和“下一步”属于历史快照。 |
| `rules/comment-style.md`、ADR-0011 | 部分有效 | 中文注释语言决定仍有效；“解释业务语义、不翻译代码”有效。旧库兼容示例及全字段强制覆盖与文档自身的新 Schema 说明存在张力，需结合当前代码审查。 |
| `rules/sql-placement.md`、ADR-0010 | 部分有效 | 复杂 SQL 进入 XML、禁止跨层拼 SQL 的原则有效；ADR 链接、旧类违例和旧库兼容示例已过时，projection 规则在“优先”与“硬规则”之间表述不一致。 |
| `rules/testing-policy.md` | 部分过时 | 测试技术栈与关键场景要求有效；全量数量仍为 612，未包含最新回归测试；“已知缺测”含已不存在或已更名服务。 |
| `arch/module-map.md` | 规则有效、状态过时 | 四层依赖和跨模块 application 边界与当前架构一致；`:108` 仍称只有 common/identity 已建立，与六模块完成状态不符。 |
| `docs/superpowers/specs/2026-05-31-*`、`2026-06-01-*` | 被后续规则部分替代 | 可用于追溯规则来源，但大量旧库兼容示例不能作为当前新 Schema 的实现要求。 |
| `rules/security-baseline.md` 的后台读取规则 | 冲突 | 通用表格允许 ADMIN/DEMO 读取，但又把草稿/私密文章和评论审计字段定义为 ADMIN 专属；当前业务规则和各 API 契约明确允许 DEMO 后台只读。 |
| PASSWORD 文章产品规则与当前 API 契约 | 有意延期但未形成统一版本边界 | 产品用例要求完整解锁、正文和评论访问；当前 article/comment 契约明确临时返回 403，roadmap 将完整 token 流程放到“上线后增量”。 |

## 4. 严重问题（Critical）

未发现具有可达利用路径、直接密钥泄露或已复现数据破坏后果的 Critical 问题。

## 5. 重要问题（Important）

### [Important-1] AI 工作入口仍把已结束的 M3 登录切片写成当前任务

- 类型：文档漂移
- 阻塞：不阻塞当前前端联调；阻塞后续任务正确分流
- 要求证据：`docs/project-handbook/CLAUDE.md:15`、`:103-105` 声明当前仍处于 M3，并要求从 identity 登录开始。
- 实现证据：`docs/project-handbook/status.md:13`、`:90-95` 和 `roadmap.md:60-68` 已明确六个后端模块完成，下一步为 M4 前端骨架。
- 验证证据：审查基线最近提交包含 stats 收尾与 MySQL 精度修复；完整测试数量将在任务 8 重新验证。
- 影响：任何按入口文档开工的协作者都可能重复实现已完成模块，或错误判断当前架构状态。
- 建议：后续单独同步 `CLAUDE.md` 的当前阶段、下一步和并行任务，不在本轮修改。

### [Important-2] Web 层直接依赖 Domain 类型，且 ArchUnit 未守护文档声明的单向边界

- 类型：实现缺陷 / 测试缺口
- 阻塞：不阻塞前端联调；建议在第一版冻结前处理或正式裁决规则
- 要求证据：`rules/package-layout.md:43-44`、`:114-118` 与 `arch/module-map.md:55-60` 明确 `web → application`，并把跨层边界列为 ArchUnit 强制规则。
- 实现证据：至少 24 处 Web 类型直接 import 本模块 Domain，例如 `identity/web/UserProfileVO.java:3`、`content/web/AdminArticleController.java:18`、`content/web/ArticleWebMapping.java:9`、`comment/web/AdminCommentController.java:7-8`、`system/web/AdminFriendLinkVO.java:4`。
- 验证证据：`ArchitectureRulesTest.java:94-100` 只禁止 Web 依赖 Infrastructure，没有禁止 Web 依赖 Domain；架构测试 29 项全部通过，因此当前守护无法发现该偏差。
- 影响：HTTP 契约与领域模型直接耦合，领域枚举、值对象或模型调整可能直接改变 Web 编译面和序列化契约；同时文档声称的规则与可执行守护不一致。
- 建议：先裁决是否允许 Web 复用稳定枚举/值对象。若不允许，将状态与结果收口到 application contract，并扩展 ArchUnit；若允许，则修改分层规则并明确仅允许的类型范围。

### [Important-3] local Profile 遗漏三个匿名端点，直接阻塞本地前端联调

- 类型：实现缺陷 / 测试缺口
- 阻塞：前端联调
- 要求证据：`api-contract/site-config.md:10`、`comment.md:21-67` 明确站点配置、文章评论和留言板为匿名接口；`security-baseline.md:91-92` 要求新增公开接口同步配置 method + path 白名单。
- 实现证据：基础配置 `application.yml:73-99` 与测试配置 `application-test.yml:51-77` 均包含这些端点；`application-local.yml:38-58` 重定义公开端点列表，但遗漏 `GET /api/public/site-config`、`POST /api/public/articles/*/comments`、`GET/POST /api/public/guestbook/comments`。
- 验证证据：以 local Profile 在端口 18080 实际启动当前后端：公开友链返回 200；公开站点配置、留言板 GET、文章评论 POST、留言板 POST 均返回 `401 + 10002`。测试 Profile 列表完整，因此现有安全测试未覆盖该差异。验证后进程已停止，端口已释放。
- 影响：本地前台无法匿名加载站点基础配置，也无法提交文章评论或读取/提交留言板，前端联调会在认证层失败。
- 建议：不要在 profile 中复制整份易漂移列表；将公共业务端点保留在基础配置，只在 local/test 追加探针、文档和媒体路径，并增加“基础 + profile 合并后”的安全集成测试。

### [Important-4] “M3 后端完成”不等于产品定义的 PASSWORD 文章闭环完成

- 类型：设计缺陷 / 实现缺口
- 阻塞：不阻塞前端骨架；阻塞 PASSWORD 文章完整联调和完整产品发布
- 要求证据：`product/use-cases.md:34`、`:53` 和 `business-rules.md:134-135` 要求密码校验后才能访问正文、评论列表和评论入口；`product/decisions-draft.md:767-775` 定义独立 article access token。
- 实现证据：`api-contract/article.md:143` 明确当前不开放解锁并返回 403；生产代码没有 `/api/public/articles/{id}/unlock`，`PublicArticleQueryService.java:64-68` 对 PASSWORD 详情固定拒绝，评论策略也固定拒绝。
- 验证证据：`roadmap.md:91` 将完整 token 流程列为“上线后增量”；现有测试只验证固定 403 和 token 类型隔离，没有密码解锁成功链路。
- 影响：如果“第一版”指产品用例闭环，后端尚未完成；前端只能实现锁定占位页，不能实现密码文章阅读和评论。
- 建议：在进入对应前端页面前明确版本边界：要么把 PASSWORD 解锁提升为首版阻塞项，要么在首版范围和前端需求中明确该状态只展示锁定元数据。

### [Important-5] ADMIN/DEMO 敏感读取规则互相冲突，当前实现选择了 DEMO 可读

- 类型：设计缺陷
- 阻塞：不阻塞当前联调；阻塞权限模型正式冻结
- 要求证据：`security-baseline.md:100` 先规定后台 GET 默认 ADMIN/DEMO 可读，`:105` 又规定草稿/私密正文和评论审计字段必须 ADMIN-only；`business-rules.md:43`、`:91` 及 article/comment 契约明确 DEMO 后台只读。
- 实现证据：`SecurityConfig.java:74-107` 对文章、评论等后台 GET 统一允许 ADMIN/DEMO；`ContentAuthorization`、`CommentAuthorization` 的 readable 规则也允许两者。
- 验证证据：文章、评论、统计和 system 集成测试均覆盖 DEMO 读取成功、写入 403；未发现敏感 GET 的 ADMIN-only 测试。
- 影响：当前行为与产品“DEMO 全后台只读”一致，但违反安全基线的敏感读条款；后续维护者无法判断草稿正文、评论邮箱/IP/UA 是否应向演示账号暴露。
- 建议：由产品侧明确 DEMO 可见字段。若 DEMO 面向公开演示环境，应对敏感字段做字段级裁剪或 ADMIN-only；裁决后同步安全基线、API 契约和测试。

### [Important-6] 注释与 OpenAPI 描述没有达到现行规则声明的覆盖范围

- 类型：实现缺陷 / 测试缺口
- 阻塞：不阻塞前端骨架；影响联调文档可信度和发布维护性
- 要求证据：`rules/comment-style.md:25-46`、`:81-86` 要求公开契约、应用服务、持久化类型及字段保留业务 Javadoc，并与 `@Schema` 一致；`rules/api-response.md:153-157` 要求每个公开方法使用 `@Operation`。
- 实现证据：静态盘点发现至少 88 个公开类型所在文件完全没有 Javadoc，代表性文件包括 `comment/application/AdminCommentCommandService.java:20`、`AdminCommentPageQuery.java:6`、`CommentCreateService.java:23`、`comment/infrastructure/persistence/entity/CommentEntity.java:11` 和 `mapper/CommentMapper.java:15`。comment 模块 9 个 Controller 映射均没有 `@Operation`，公开文章两个映射也没有；其他模块亦存在部分覆盖。
- 验证证据：当前 8 组 OpenAPI 测试主要断言契约 DTO 与敏感字段不泄漏，没有断言 operation summary、字段 description 或覆盖率；按 Controller 文件统计，多个文件的 mapping 数明显高于 `@Operation` 数。
- 影响：Knife4j 可以生成路径和结构，但大量接口/字段缺少业务语义，前端与维护者仍需回看代码或 Markdown 契约；同时“强制全字段注释”的规则与实际开发方式长期失配，无法作为可信门禁。
- 建议：先收敛规则，只强制公开 API、状态/权限/时间/SQL 等高风险语义；随后按模块补齐 Controller `@Operation` 和契约 DTO `@Schema`，并增加描述完整性测试，避免机械补写翻译代码的注释。

## 6. 次要问题（Minor）

### [Minor-1] SQL 规则引用了不存在的 ADR 文件名

- 类型：文档漂移
- 阻塞：不阻塞
- 要求证据：`docs/project-handbook/rules/sql-placement.md:5` 引用 `0005-mybatis-plus-as-orm.md` 和 `0010-sql-placement.md`。
- 实现证据：实际文件为 `0005-mybatis-plus-as-primary-orm.md` 和 `0010-sql-placement-strategy.md`。
- 验证证据：文件清单核对确认错误名称不存在。
- 影响：规则无法直接追溯到决策理由，自动化或人工链接检查会失败。
- 建议：后续修正规则中的 ADR 路径。

### [Minor-2] 注释和 SQL 规则仍保留已废止的旧库兼容示例

- 类型：设计缺陷 / 文档漂移
- 阻塞：不阻塞
- 要求证据：`comment-style.md:53` 已明确 ADR-0013 后无需旧库兼容注释；`arch/persistence-strategy.md:76-80` 也说明新 Schema 已取代兼容路线。
- 实现证据：`comment-style.md:31`、`:74`、`:91` 和 `sql-placement.md:28`、`:40`、`:68-72`、`:88` 仍把旧库兼容作为必须说明的当前场景；旧规格中同类示例更多。
- 验证证据：当前 Flyway V1 为全新 V2 Schema，旧 `ContentCatalogMapper` 文件也已不存在。
- 影响：会诱导新增代码写入虚假的历史兼容说明，降低注释可信度。
- 建议：保留历史规格用于追溯，在当前规则中删除旧库强制项和旧示例。

### [Minor-3] 测试基线与已知缺测清单未同步最新代码

- 类型：文档漂移
- 阻塞：不阻塞
- 要求证据：`rules/testing-policy.md:5-6` 声明 164 个测试文件、612 项测试；`:103-105` 列出三个缺测项。
- 实现证据：当前仍有 164 个 `*Test.java`，但最新提交新增了文章时间精度回归测试；`CommentCommandService` 已不存在，当前类名为 `AdminCommentCommandService`。
- 验证证据：fresh `mvn clean test` 实际运行 613 项，0 失败、0 错误、4 跳过，确认文档总数落后 1 项。
- 影响：不会改变行为，但会误导覆盖评估和后续补测范围。
- 建议：后续根据最终全量结果和实际服务名更新测试策略文档。

### [Minor-4] 安全基线把后端强制授权错误地绑定为必须使用 `@PreAuthorize`

- 类型：设计缺陷 / 文档漂移
- 阻塞：不阻塞
- 要求证据：`security-baseline.md:103-105` 把 DEMO 写保护和敏感读保护具体表述为必须使用 `@PreAuthorize`。
- 实现证据：当前没有使用 `@PreAuthorize`；授权由 `SecurityConfig` 的 method + path matcher 与 application 层 `requireAdmin/requireReadable` 双层实现。
- 验证证据：安全和业务集成测试覆盖 ADMIN/DEMO 的 401/403 行为，证明后端强制授权不依赖方法注解。
- 影响：规则把安全目标与单一实现手段混淆，可能导致无收益地重复添加注解，或错误把现有有效授权判为失效。
- 建议：规则改为“必须由后端可执行策略强制并有测试”，将 `@PreAuthorize`、SecurityFilterChain 和 application 授权列为可选实现，并明确避免策略漂移。

### [Minor-5] 少量生产注释仍描述已经不存在的迁移阶段和旧库约束

- 类型：文档漂移
- 阻塞：不阻塞
- 要求证据：`comment-style.md:17-22` 要求注释解释当前业务原因，`:53` 明确不再为旧库语义写兼容说明。
- 实现证据：`MyBatisPlusConfig.java:14` 仍称“当前阶段只引入框架基座、不迁移现有 JdbcTemplate”，但生产代码已无 JdbcTemplate 业务 SQL；`UserAgentResolver.java:16` 仍用“旧库字段较短”解释 255 长度，而当前 V2 Schema 已明确审计字段长度。
- 验证证据：生产源码搜索只命中这段 JdbcTemplate 注释，没有真实 JdbcTemplate 使用；Flyway V1 是全新 V2 Schema。
- 影响：注释会让维护者误判持久化迁移尚未结束或仍承担旧库兼容责任。
- 建议：改成当前约束，例如明确 Mapper 扫描边界，以及 User-Agent 与当前表字段长度/防滥用的关系。

### [Minor-6] 审计字段集成测试在共享 MySQL 测试库留下临时表

- 类型：测试缺口
- 阻塞：不阻塞业务；影响测试隔离
- 要求证据：`rules/testing-policy.md` 要求集成测试可重复、可清理，不依赖或污染共享状态。
- 实现证据：`AuditFieldHandlerIntegrationTest.java:25-41` 在测试前删除并重建 `t_audit_update_test`，但 `:47-50` 的 `@AfterEach` 只清理安全上下文，没有在测试后删除表。
- 验证证据：本地 MySQL 元数据确认 14 个业务表和 Flyway 表之外仍存在 `t_audit_update_test`；MySQL 广泛回归后该表继续存在。
- 影响：重复执行通常仍能通过，但共享测试库永久多出非 Flyway 表，Schema 漂移检查和人工排障会受到干扰。
- 建议：将临时表清理放入测试结束阶段，或把该夹具隔离到专用 Schema/Testcontainers；不要把测试表加入正式 Flyway。

## 7. 已验证的关键边界

- 顶层实际模块为 `common`、`identity`、`content`、`comment`、`system`、`stats`，与当前六模块模型一致。
- 跨业务模块生产依赖共 9 处，均从调用方 application 指向目标模块 application；未发现跨模块直接访问 domain、web 或 infrastructure。
- 未发现 application/web 直接使用 MyBatis Mapper、BaseMapper、Wrapper 或 JdbcTemplate。
- 未发现 domain 依赖 Spring、MyBatis-Plus 或 Servlet API。
- ArchUnit 架构测试共 29 项，0 失败、0 错误、0 跳过；通过项覆盖领域纯净、application/infrastructure 隔离、跨模块 application 边界、循环依赖和 common 反向依赖。
- JWT access token 校验覆盖签名、issuer、时间、`typ=access`、token version 与持久化账号状态；refresh token 使用 32 字节随机值，只保存 SHA-256 摘要，轮换通过行锁和事务保证单次消费。
- 登出与修改密码会在事务内递增 token version 并撤销 refresh token；改密同时锁定账号行并重新核对数据库角色。
- 附件上传会限制大小、检查实际图片内容、规范化存储路径并在登记失败时补偿删除；评论 Markdown 使用 CommonMark 后再经 OWASP sanitizer 清洗。
- 运行时 SQL 未发现注解 SQL、Java 拼接 SQL 或 `${...}` 替换；14 份 Mapper XML 的 namespace、statement id 与接口方法一致，分页与 TOP 查询均有稳定次排序。
- 本地 MySQL 8.0.35 中 Flyway V1/V2 均成功，14 个业务表均为 InnoDB/utf8mb4，无数据库外键、无 `ON UPDATE` 自动时间列；应用连接串会强制 session 时区为 `+09:00`。
- fresh H2 全量：613 项，0 失败、0 错误、4 跳过；fresh MySQL 广泛回归：597 项，0 失败、0 错误、4 跳过。MySQL 日志确认 `ArticleIntegrationTest`、持久化与集成测试实际连接 `myblog_v2_dev`。
- Maven Enforcer 的 Java、Maven 与 dependency convergence 规则通过；编译 release 为 17，H2/Testcontainers/ArchUnit/Test starter 均保持 test scope。

## 8. 未覆盖风险

- Docker 不可用，Testcontainers MySQL 条件测试本轮无法运行。
- 4 个跳过用例来自 `MySqlFlywayMigrationTest`、`MySqlChangePasswordConcurrencyTest`（2 项）和 `MySqlLoginFailureConcurrencyTest`，原因均为 `disabledWithoutDocker=true`；本地 MySQL 广泛回归不能完全替代独立容器中的并发与空库迁移证明。
- 未执行 prod Profile 的真实启动，也未连接真实 S3、Resend 或生产反向代理；相关配置只完成静态检查和启动校验代码审查。
- S3 模式仍会注册 `LocalStorageWebConfiguration` 的 `/media/**` 本地资源映射；生产安全配置未匿名放行该路径，当前未形成匿名泄露路径，但该装配耦合仍应在后续清理并增加 profile/type 测试。
- 本地 MySQL 回归使用共享测试库，测试会重置或修改业务测试数据；本报告只证明当前执行时的行为，不证明任意已有数据集的迁移兼容性。

## 9. 修复批次与顺序

1. **前端联调阻塞批次**：修复 `application-local.yml` 的公开端点合并方式，增加 local Profile 真实安全集成测试。行为改变：是。验证：公开站点配置、文章评论、留言板 GET/POST 在 local 下匿名返回业务响应而非 401。
2. **产品与权限裁决批次**：明确 PASSWORD 文章是否进入首版，冻结 ADMIN/DEMO 对草稿正文和评论审计字段的可见范围，并同步产品规则、API 契约与安全测试。行为改变：取决于裁决。
3. **架构守护批次**：裁决 Web 是否允许复用有限 Domain 值类型；同步分层规则并扩展 ArchUnit。行为改变：通常否，可能产生类型迁移。
4. **测试与数据库卫生批次**：清理 `t_audit_update_test`，补独立 MySQL 空库迁移和并发验证环境。行为改变：否。验证：测试前后 Schema 差异为空、Testcontainers 4 项实际执行。
5. **文档与契约质量批次**：更新 `CLAUDE.md`、测试基线、ADR 链接、旧库注释规则，并按高风险优先补齐 OpenAPI/Javadoc。行为改变：否。验证：链接检查、OpenAPI 描述守护和注释抽样审查。

## 10. 前端联调与发布门槛

- **可以开始的工作**：前端工程骨架、登录、后台常规 CRUD、公开文章列表/详情（非 PASSWORD）、分类标签、友链、站点配置 UI、评论 UI 和统计面板的静态契约开发。
- **联调前门槛**：先修复 Important-3，并用 local Profile 验证所有匿名 method + path；否则前端会把配置错误误判为登录或接口错误。
- **PASSWORD 页面门槛**：在 Important-4 裁决前只允许实现锁定占位态，不应假设解锁接口已经存在。
- **演示账号门槛**：在 Important-5 裁决前，不应把含邮箱、IP、User-Agent 或私密正文的 DEMO 页面部署到公开环境。
- **首版发布门槛**：H2/MySQL 回归继续为零失败；4 个 Docker 条件测试需在可用环境实际通过；完成 prod Profile 启动、空库 Flyway、真实 S3/邮件或明确禁用策略、反向代理/CORS 验证；发布范围与 PASSWORD/DEMO 裁决写入统一有效文档。
