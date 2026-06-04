# Codex Review：docs/ClaudeCode 文档体系阶段评审

> 评审范围：`docs/ClaudeCode/` 下现有 Markdown 文档。
> 评审方式：只阅读文档，不审源码，不修改既有文档。
> 评审日期：2026-06-03

## 总体结论

当前文档已经不是“刚起步”的状态，而是混合了三类内容：

1. 早期后端 V2 已实现状态与遗留问题；
2. 新一轮产品 / schema / 横切设计决定；
3. 给 AI 后续执行用的 rules / workflows / ADR。

主要风险不是“文档不够多”，而是**权威顺序和阶段边界没有收敛**。如果现在直接按 `status.md` / `roadmap.md` 去修 mapper、补测试、建 system，很可能继续沿着旧实现往前走；但 `CLAUDE.md` 和 `product/decisions-draft.md` 又明确说应该先完成业务规格、数据模型、schema 设计，再动实体 / Mapper / Controller。

建议先做一次文档收敛：确定当前阶段到底是“暂停代码，先定产品与 schema”，还是“在既有 V2 代码上局部修补”。我倾向前者，因为文档里多处已经承认 DDL 未冻结，新 schema 才是后续核心。

## 高优先级问题

### P0-1：当前阶段与下一步行动冲突

`CLAUDE.md` 明确写：

- 当前阶段是业务规格梳理，等 `product/feature-inventory.md` 标注完后才动代码。
- 下一步是 `product/use-cases.md` -> `product/data-model.md` -> `arch/schema-design.md` -> V2 后端业务代码重写。

但 `status.md` / `roadmap.md` 又把近期行动写成：

- 修 `ContentCatalogMapper` 的 `@Select`；
- 补 comment / content 集成测试；
- 启动 `system` 模块建设；
- 中期再做富文本 XSS、上传安全、Redis 等。

这会让 AI 很容易误判：到底先补旧 V2，还是先冻结新 schema？尤其 `product/decisions-draft.md` 已写明“DDL 未冻结，下一步自审风险 -> ER 图 -> DDL -> 才能写实体 / Mapper / Controller”，因此继续建模块和改持久层有较大返工风险。

建议：

- 在入口文档新增一个“当前唯一主线”声明；
- 把 `status.md` / `roadmap.md` 里的旧实现修补项移到“旧 V2 遗留，schema 冻结后再评估是否仍需要”；
- 明确禁止在 `product/data-model.md` 和 `arch/schema-design.md` 完成前新增实体、Mapper、Controller。

### P0-2：模块边界存在两套模型

旧 ADR / arch / overview 是：

- `common`
- `infrastructure`
- `identity`
- `content`
- `comment`
- `system`

但 `product/decisions-draft.md` / `v1-vs-v2.md` 又写：

- `identity`
- `content`
- `comment`
- `system`
- `stats`
- `common-infra`

这里的问题不是名字差异，而是会影响包结构、错误码段、Knife4j 分组、ArchUnit 规则、跨模块调用方式。

特别是：

- `stats` 是否是独立业务模块还没被 ADR-0004 / `arch/module-map.md` 接收；
- `common-infra` 是文档概念，还是 Java package / Maven module / 顶层包，目前不清楚；
- 旧文档的 `common` + `infrastructure` 与新文档的 `common-infra` 不是一一对应；
- `system` 在旧文档里还写“字典、菜单”，但产品决定已经倾向删除动态菜单、不引字典表。

建议：

- 新增或替换 ADR-0004，明确最终模块模型；
- 若保留 `stats`，同步更新 `overview.md`、`arch/module-map.md`、`rules/package-layout.md`、ArchUnit 规则说明、错误码空间；
- 若 `common-infra` 只是概念，文档统一写成“common + 顶层 infrastructure”，避免 AI 误建新包。

### P0-3：注释语言与当前 AGENTS 指令冲突

项目当前 AGENTS 指令要求：代码文件里的注释用日文，对话用中文。

但 `rules/comment-style.md` 和 ADR-0011 都要求：

- Javadoc、字段注释、行内注释、测试场景注释、OpenAPI 描述统一中文。

这会直接影响后续 Java 重构风格。按照当前工作环境，AGENTS 优先级更高，但文档体系会持续诱导 AI 写中文注释。

建议你先决定：

- 方案 A：后端代码注释统一日文，则废止或替换 ADR-0011，并改 `rules/comment-style.md`；
- 方案 B：本项目文档规则优先，仍统一中文，则需要同步更新 AGENTS；
- 不建议维持现状，否则每次写代码都要重新解释优先级。

### P1-1：API 文档技术选型表述不一致

ADR-0009 标题和正文是“用 springdoc-openapi 替换 knife4j”，并写 V2 使用 `springdoc-openapi`。

但其它文档又写：

- V2 API 文档是 Knife4j 4.x（基于 springdoc-openapi）；
- `product/decisions-draft.md` D9 要引入 `knife4j-openapi3-jakarta-spring-boot-starter`；
- `frontend-admin/README.md` 也写 Knife4j 4.x。

`decisions/README.md` 试图解释“实际用 knife4j 4.x = springdoc 之上的 UI 层”，但 ADR-0009 本身仍是反向表达。

建议：

- ADR-0009 改为“OpenAPI 基于 springdoc，UI 使用 Knife4j 4.x”；
- 或新增 superseding ADR，把旧 ADR 标为已被取代；
- 否则 AI 可能删除 Knife4j starter，只保留 springdoc。

### P1-2：Redis / TokenRevocationStore 的旧路线没有完全清掉

较新的认证设计已经确定：

- access token + refresh token；
- `token_version` + DB `t_refresh_token`；
- 不依赖 Redis 实现撤销；
- 多实例撤销也能靠 DB 生效。

但 `status.md` / `roadmap.md` 仍把 `TokenRevocationStore` 迁 Redis 写成多实例前必须解决项。

这会把已废弃方案重新拉回来，且违反“不引入未经 ADR 授权中间件”的红线。

建议：

- 从近期 / 中期 roadmap 中移除“TokenRevocationStore 迁 Redis”；
- 保留为历史坑：早期内存撤销已被 R6 C1 取代；
- Redis 只作为未来限流缓存、在线用户、统计等场景的独立 ADR，不再绑定 JWT 撤销。

### P1-3：附件删除策略前后矛盾

`product/decisions-draft.md` A3 写：

- 删除前由 application 层扫描 `cover_attachment_id` + 评论 / 正文 Markdown 引用；
- 软删时若 cover 或正文 Markdown URL 命中，则拒绝软删；
- 正文 Markdown 引用扫描可用 `LIKE`。

但紧接着 A3' 又写：

- 删除 attachment 时只扫描 cover 引用；
- 正文 Markdown 中的 attachment URL 不强制扫描；
- 由定期 audit job 报告孤儿 / 失效引用。

这两个策略行为完全不同，会影响删除接口、测试、用户体验和后台提示。

建议：

- 选一个策略并删掉另一套表述；
- 如果走简单策略，建议写成：删除只强制检查结构化引用（cover），正文 Markdown 引用只做弱审计；
- 不要同时写“拒绝软删”和“不强制扫描”。

### P1-4：`FK` 这个词容易误导为 DB FOREIGN KEY

文档多处强调不使用 DB `FOREIGN KEY`，引用完整性由 application 层维护。

但 `product/decisions-draft.md` 写 `t_article.cover_attachment_id BIGINT NULL FK 到 t_attachment.id`，后面又写“FK 强约束”。这容易让 AI 生成 DDL 时写出真实 `FOREIGN KEY` 约束，直接违反 ADR-0017 / pitfalls R-012。

建议：

- 统一改成“逻辑引用”或“业务引用”；
- DDL 示例中所有 `*_id` 只写普通索引；
- 文档里避免使用“FK 强约束”这种容易被 SQL 化的词。

## 中优先级问题

### P2-1：`system` 模块职责残留 V1 思维

旧文档把 `system` 描述成“系统配置、字典、菜单”等后台基础数据；roadmap 也写“迁移系统配置 / 字典等表”。

但产品决策已经明确：

- 后台菜单删除，前端静态路由；
- 字典表不引入，Java enum + 前端 i18n；
- `system` 更像承载 `t_site_config`、`t_attachment`、`t_friend_link` 等配置 / 资源型能力。

建议重写 system 模块职责，避免 AI 复活 `t_menu` / `t_dict` / 动态菜单。

### P2-2：前端 UI 框架是否已定存在冲突

`feature-inventory.md`、`frontend-admin/README.md` 写后台技术栈是 Vue 3 + Element Plus + Pinia + TypeScript。

但 `product/decisions-draft.md` 顶部又说“前端 UI 框架后置到前端启动时定”，底部 FE-5 仍把 Element Plus / Naive UI / Arco 列为待定。

建议：

- 如果已经定 Element Plus，就把 FE-5 改成“已定”；
- 如果还没定，`frontend-admin/README.md` 不应写成强约束。

### P2-3：文档索引里 ADR 列表不完整

`INDEX.md` 的目录树列到 ADR-0014，但实际已有 ADR-0015 ~ 0018。

这类问题不影响架构，但会影响 AI 的阅读顺序和检索完整性。

建议同步 `INDEX.md` 的目录树。

### P2-4：`arch/schema-design.md` 被多处引用但尚不存在

`CLAUDE.md`、`product/README.md`、`product/decisions-draft.md` 都把 `arch/schema-design.md` 作为下一步关键文档，但当前文件还没有。

建议尽快建空壳或待办模板，至少写清：

- 本文件尚未冻结；
- DDL 输出前不得写实体 / Mapper / Controller；
- 引用 ADR-0014 / 0015 / 0017 / 0018。

## 建议的收敛顺序

1. 先决定“当前唯一主线”：暂停后端实现，先完成业务规格与 schema。
2. 清理 `status.md` / `roadmap.md` 中已废弃的 Redis、旧 mapper 修补优先级。
3. 确认最终模块边界：是否引入 `stats`，是否使用 `common-infra` 这个名字。
4. 统一注释语言：日文还是中文。
5. 统一 API 文档选型：springdoc only，还是 springdoc + Knife4j 4.x UI。
6. 修正附件删除策略与 `FK` 术语。
7. 建 `product/use-cases.md`、`product/data-model.md`、`arch/schema-design.md`，然后再进入后端重写。

## 我建议的短期停止线

在以下文档完成前，不建议继续写后端业务代码：

- `product/use-cases.md`
- `product/business-rules.md`
- `product/data-model.md`
- `arch/schema-design.md`
- 更新后的模块边界 ADR

可以继续做的只有：

- 文档收敛；
- 对既有 V2 代码做只读盘点；
- 整理旧实现与新 schema 的差距清单；
- 不产生新业务实现的构建 / 测试验证。
