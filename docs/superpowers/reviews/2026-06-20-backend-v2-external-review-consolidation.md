# 后端 V2 外部审查整合复核

## 1. 结论

本次复核了 `C:\Users\TYB\OneDrive\Desktop\后端` 下 4 份 review 和 1 份 implementation plan，并以当前 `backend-v2-refactor` 分支代码、项目手册和最近修复提交为准重新取证。

外部材料不是可以直接执行的最终方案：其中包含真实缺陷，也包含已修复问题、与现行 ADR 冲突的建议，以及会扩大首版范围的重构。结合上一轮未闭环内容后，综合采纳 9 组问题：6 组实施修复，3 组通过契约、发布检查和文档/OpenAPI 治理收口。

- 未发现新的 Critical 漏洞或数据破坏问题。
- 已确认 4 个应优先处理的实现问题：未知路由误报 500、评论黑名单仍是硬编码占位、后台文章标签 N+1、PASSWORD 正文在拒绝前被读取。
- Web→Domain 边界问题成立，但应采用“稳定枚举精确白名单 + 非枚举依赖迁出”，不复制镜像枚举。
- 公开文章和附件 DTO 存在不必要字段，前端尚未冻结前适合一次性收窄。
- trusted proxy 与 CORS 是部署前置条件，不应在应用里硬编码任意网段；需要发布检查和部署文档，而不是照抄外部配置。
- 外部计划提出的“删除 approve 端点”“新增 5 套镜像枚举”“一次性增加 16 个模块错误码”均不采纳。

## 2. 复核范围与当前基线

### 2.1 外部输入

- `reviews/2026-06-19-api-design-review.md`
- `reviews/2026-06-19-database-performance-review.md`
- `reviews/2026-06-19-independent-architecture-review.md`
- `reviews/2026-06-19-security-review.md`
- `plans/2026-06-19-implementation-plan.md`

外部 `deploy/` 目录只作为部署假设的参考，没有视为仓库当前配置，也不直接导入。

### 2.2 当前分支已经完成的相关修复

| 提交        | 已解决内容                                                 |
| --------- | ----------------------------------------------------- |
| `05b81ec` | Profile 公开端点改为基础列表与 profile 附加列表合并，解决 local 匿名接口 401。 |
| `537c6be` | 审计集成测试结束后删除临时表。                                       |
| `276097d` | 同步当前阶段和部分规则文档。                                        |
| `d3086cf` | DEMO 不再读取非公开文章正文。                                     |
| `78ce38b` | DEMO 评论审计敏感字段固定返回 `null`。                             |
| `b13a78a` | 冻结首版 PASSWORD 锁定占位和 DEMO 权限边界。                        |

最近一次阶段验证为 H2 622 项和 MySQL 622 项，均 0 失败、0 错误、4 跳过。该结果证明当前基线稳定，但不能替代下面每个小任务的局部验证。

## 3. 采纳的复核结果

### [Important-1] 未知 API 路由被兜底成 500

- 外部编号：A-M3、M-1。
- 状态：成立，未修复。
- 证据：`GlobalExceptionHandler` 未处理 `NoResourceFoundException`，最终进入 `Exception` 兜底并返回 `500 + 99999`。
- 影响：客户端路径错误被误判为服务端故障，监控也会产生伪 5xx。
- 处理：增加专用 404 映射和 handler 单元测试，不改变已有业务资源不存在的 `90003` 语义。

### [Important-2] 评论审核策略仍以硬编码 `spam` 充当黑名单

- 外部编号：B-1。
- 状态：问题成立，外部修复方案错误。
- 证据：`CommentCreateService.audit()` 只判断是否包含字面量 `spam`；项目契约已经定义“未命中黑名单为 PASS，命中为 PENDING”，后台 approve/hide 工作流也已落地。
- 影响：审核状态可达，但策略不可配置，不能表达产品定义的基础关键词黑名单。
- 处理：保留乐观发布与 approve 端点；把审核判断提取为可配置策略，默认空黑名单，命中任一规范化关键词时进入 PENDING。
- 明确不做：不能按外部计划删除 approve 端点及测试，否则命中黑名单的评论永远不能正常发布。

### [Important-3] 后台文章分页逐条读取标签

- 外部编号：DB-I1。
- 状态：成立，未修复。
- 证据：`MyBatisAdminArticleQueryRepository.findActivePage()` 每映射一条记录调用一次 `selectTagIds(id)`；20 条分页会产生 1 次主查询、20 次标签查询和 1 次 count。
- 影响：管理列表查询次数随页大小线性增长。
- 处理：复用现有 `ArticleTagRow` 批量查询形态，按文章 ID 集合一次读取并分组；详情查询继续单条读取。

### [Important-4] PASSWORD 文章在返回 403 前读取完整正文

- 外部编号：I-3、DB-D2。
- 状态：成立，未修复。
- 证据：`PublicArticleQueryService.detail()` 先执行包含 MEDIUMTEXT `body` 的 `findPublicDetail()`，之后才检查 `ArticleStatus.PASSWORD`。
- 影响：锁定文章请求产生无意义正文读取，也违背最小数据读取原则。
- 处理：为公开详情增加不含正文的访问元数据查询；不存在或不可见仍返回 404，PASSWORD 直接返回 403，只有可公开正文的文章才执行详情查询。

### [Important-5] Web→Domain 规则与实现、守护不一致

- 外部编号：I-1、A-D1。
- 状态：成立，未修复；外部“镜像全部枚举”方案不采纳。
- 已冻结边界：Web 可精确复用以下稳定领域枚举：`AccountType`、`ArticleStatus`、`CommentAuditStatus`、`CommentTargetType`、`FriendLinkStatus`。
- 必须迁出的非枚举依赖：`UserProfile`、`AdminArticlePageItem`、`ContentName`、`ContentSlug`、`ArticleSlug`。
- 处理：application 暴露结果类型或原始输入契约；ArchUnit 禁止 Web 依赖 Domain，但对上述 5 个具体枚举做类型级白名单，并增加故意违例 fixture。
- 明确不做：不创建 `*StatusView` 镜像枚举。镜像会制造双向转换和枚举同步风险，却没有改变 JSON 契约。

### [Design-1] 公开 DTO 暴露了不需要的内部字段

- 外部编号：A-D1、A-D2、A-D3、A-D4。
- 状态：大部分成立，安全影响被外部报告放大，但契约耦合真实存在。
- 公开文章：`status` 与 `locked` 重复；`coverAttachmentId` 与 `coverUrl` 重复；详情缺少读者有价值的 `updatedAt`。
- 后台附件：`bucket`、`objectKey`、`hashSha256`、`storageType` 属于存储实现或服务端去重信息，当前前端没有必要依赖。接口有鉴权，因此不是直接漏洞，但会把存储迁移变成 API 破坏。
- 处理：前端契约冻结前移除冗余/内部字段，公开文章详情增加 `updatedAt`，同步 OpenAPI 和控制器测试。

### [Design-2] 部署时必须明确真实客户端 IP 与 CORS 模式

- 外部编号：S-I1、S-D1。
- 状态：条件成立，不是可由通用代码自动修复的问题。
- 证据：`ClientIpResolver` 的安全策略正确，只信任显式配置的代理；若部署在反向代理后却未设置 `MYBLOG_WEB_TRUSTED_PROXIES`，所有请求会使用代理 IP。生产 CORS 空列表在跨域部署时会阻断浏览器，但在同源反代部署中是合法配置。
- 处理：把部署拓扑分成“同源反代”和“跨域前端”两种，发布检查必须验证真实客户端 IP、限流键和浏览器预检；部署方显式设置代理 IP/CIDR，禁止在仓库里默认信任 `10.0.0.0/8` 等宽网段。

### [Minor-1] 契约文档有三处语义需要明确

- 外部编号：A-B1、A-I3、I-2。
- 时间：当前 ADR 明确所有 `LocalDateTime` 使用 Asia/Tokyo 本地时间字符串，不带 offset。外部把 `date-format` 改成 `xxx` 的建议不能让 `LocalDateTime` 自动拥有可靠 offset，也会直接违反已冻结契约。应补充前端解析约定，而不是改一行 YAML。
- 评论分页：`total` 应明确为根评论数，`replies` 当前完整返回；达到规模阈值后再设计独立回复分页。
- 错误码：规则文档保留了模块码空间，但实现采用少量稳定语义码。首版不一次性扩张 16 个新码；先把“模块码强制”改为“保留空间，出现客户端必须分支处理的稳定业务语义时再新增”。

### [Design-3] 上一轮审查遗留的文档与 OpenAPI 治理仍未完全闭环

- 来源：`2026-06-18-backend-v2-release-review.md` 的 Important-6、Minor-2、Minor-3、Minor-4。
- 状态：部分成立，未被本轮外部 review 覆盖，现补入整合范围。
- OpenAPI/Javadoc：已有模块级 OpenAPI 测试和部分 `@Operation`/`@Schema`，但规则仍要求近乎全覆盖，实现没有统一门禁。不能继续依赖人工抽样，也不应机械补写翻译代码的注释。
- 旧库示例：`comment-style.md`、`sql-placement.md`、`testing-policy.md`、`arch/persistence-strategy.md` 仍把旧库兼容、JdbcTemplate 过渡期作为当前规则或示例，与 ADR-0013 和现行 MyBatis XML 实现冲突。
- 测试基线：`testing-policy.md` 在 `276097d` 更新为 615 tests，但当前阶段验证已经达到 622 tests，说明把易变数量硬编码在规则入口会持续漂移。
- 授权规则：`security-baseline.md` 仍把 DEMO 写保护绑定为必须使用 `@PreAuthorize`；当前有效实现是 SecurityFilterChain 与 application 授权双层守护，规则应约束安全结果和测试证据，而非指定唯一注解。
- 处理：增加独立的规则清理任务和 OpenAPI 可执行覆盖任务；与业务修复分开提交。

## 4. 已修复或原报告判断不成立

| 外部编号 | 复核结论    | 原因                                                                                                                                                      |
| ---- | ------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| B-2  | 已修复     | `05b81ec` 已改为 shared + additional 合并，并有 profile 集成测试。                                                                                                   |
| S-I2 | 核心判断不成立 | Request 无 Bean Validation 注解，但 `CommentAuthor` 已限制昵称 64、邮箱 128、站点 255，`CommentContent` 已限制 Markdown 5000、HTML 20000，并校验必填/邮箱/协议。可选的 Web 层早拒绝不等于当前“无限制”。 |
| S-M2 | 生产已处理   | `application-prod.yml` 对 `MYBLOG_STATS_HASH_SECRET` 无默认值，缺失时不能正常绑定/启动；仅开发测试允许空值。                                                                        |
| A-D5 | 不采纳     | 评论可能返回 PENDING，并非永远 PASS；创建后重新读取列表可获得服务端真实排序和审核可见性，完整对象响应属于体验优化而非首版缺陷。                                                                                  |
| M-2  | 不成立     | token 位于响应 body，当前没有需要暴露给浏览器脚本的自定义响应头。                                                                                                                  |

### 4.1 上一轮 review 的完成状态

| 上一轮编号 | 当前状态 | 证据或去向 |
|---|---|---|
| Important-1 阶段入口过时 | 已修复 | `276097d` 已同步 `CLAUDE.md` 和 `module-map.md`。 |
| Important-2 Web→Domain | 未完成，已纳入 | 本文 Important-5，计划 Task 5。 |
| Important-3 local 公开端点 | 已修复 | `05b81ec`。 |
| Important-4 PASSWORD 边界 | 已裁决 | `f9092a3`、`6753a2e`、`b13a78a`；当前首版保持锁定占位。 |
| Important-5 DEMO 敏感读取 | 已修复 | `d3086cf`、`78ce38b`、`b13a78a`。 |
| Important-6 注释/OpenAPI 覆盖 | 未完成，已补入 | 本文 Design-3，新增独立计划任务。 |
| Minor-1 ADR 链接错误 | 已修复 | `276097d`。 |
| Minor-2 旧库示例残留 | 未完成，已补入 | 本文 Design-3，新增规则清理任务。 |
| Minor-3 测试基线漂移 | 再次发生，已补入 | 当前规则仍写 615，阶段验证已为 622；改为低漂移维护方式。 |
| Minor-4 `@PreAuthorize` 绑定 | 未完成，已补入 | 本文 Design-3，新增规则清理任务。 |
| Minor-5 生产注释描述旧阶段 | 已修复 | `276097d` 已更新 `MyBatisPlusConfig`、`UserAgentResolver` 注释。 |
| Minor-6 MySQL 临时表 | 已修复 | `537c6be`。 |

## 5. 合理但本轮延后

| 外部编号  | 结论                                  | 重新评估条件                                               |
| ----- | ----------------------------------- | ---------------------------------------------------- |
| A-I1  | 结构化字段错误有价值，但把多条错误拼进 `msg` 不是好契约。    | 前端表单开始联调并确定字段错误数据结构后单独设计。                            |
| DB-D1 | 回收站 `(deleted, deleted_at)` 索引方向合理。 | 先在接近生产的数据量上执行 MySQL `EXPLAIN ANALYZE`；低数据量不为规范感增加索引。 |
| DB-M1 | `%keyword%` 扫描正文确实不能走普通索引。          | 评论量达到万级且后台搜索延迟可观测后再评估 FULLTEXT。                      |
| S-D2  | refresh token 绝对会话上限是有效安全增强。        | 多设备会话管理或更高安全等级进入产品范围时单独设计 Schema 与撤销策略。              |
| S-M1  | 进程内限流不适合水平扩展。                       | 部署从单实例变为多实例时迁移 Redis/网关限流。                           |
| D-6   | 多语言选择逻辑存在重复。                        | 后续出现第四种语言或规则分叉时再抽取，当前保持局部代码更直观。                      |

## 6. 不采纳的问题与方案

| 外部编号     | 结论                                                                              |
| -------- | ------------------------------------------------------------------------------- |
| A-I2     | 分类排序返回 Void、友链返回规范化列表是不同用例契约，不需要为表面一致性制造破坏性变更。                                  |
| A-M1     | `totalPages`、`hasMore` 可由 `total/page/size` 无歧义计算，服务端不重复派生。                     |
| A-M2     | 后台筛选支持到秒是有用能力；只接受日期会丢失精度。应在契约中说明 JST，而不是降级参数类型。                                 |
| DB-M2    | 查询有 `created_at` 区间和对应索引，不是“全表扫描”；个人博客五分钟增量窗口无需优化。                              |
| DB-M3    | 单实例默认 10 连接与当前负载匹配；固定 5 秒 leak detection 容易产生慢查询误报。                             |
| D-1～D-3  | site_config、refresh_token、article_tag 的字段设计均在当前 Schema 决策中有明确例外，不能因形式统一再次改 DDL。 |
| D-4      | 评论软删除/恢复是业务状态，不只是持久化细节，领域对象需要表达该状态。                                             |
| D-5      | 写后读取用于返回数据库规范化后的完整详情和引用结果；当前低写入量下不值得重构事务返回模型。                                   |
| 外部计划 Q3  | 一次性新增 16 个模块错误码会扩大 API 迁移面，且部分场景分类并不准确。                                         |
| 外部计划 Q2  | application 镜像领域枚举会产生重复模型和同步负担；采用稳定枚举精确白名单。                                     |
| 外部计划 B-1 | 删除 approve 会破坏既有 PENDING 审核闭环，与产品黑名单策略冲突。                                       |

## 7. 建议执行顺序

1. 未知路由 404：小改动，先消除错误监控噪声。
2. 评论审核策略：把产品已经冻结的黑名单语义真正落地。
3. 后台文章标签批量查询：消除确定的 N+1。
4. PASSWORD 详情访问预检：避免读取被拒绝的正文。
5. Web→Domain 边界：按精确白名单迁移非枚举依赖并补守护。
6. 公开 DTO 收窄：在前端契约冻结前完成破坏性清理。
7. 契约和发布检查：同步时间、评论分页、错误码、代理与 CORS 约定。
8. 文档与 OpenAPI 治理：清理上一轮遗留规则，建立可执行的接口描述门禁。

每项必须独立测试、独立提交。阶段结束后再执行 fresh H2 全量和授权的 MySQL 广泛回归。
