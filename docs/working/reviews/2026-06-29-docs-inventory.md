# 文档盘点表

> 状态：P0 初盘
> 适用范围：`docs/`、`frontend/apps/admin/docs/`
> 最后校准：2026-06-29
> 权威程度：整理过程材料

## 本文档回答什么问题

本文档用于正式重组前盘点现有文档，明确每类文件的建议动作、目标位置和注意事项。它不是新的权威开发手册；完成迁移后，本文件应归档到 `docs/working/reviews/` 或 `docs/archive/`。

## 处理动作定义

| 动作 | 含义 |
|------|------|
| 保留校准 | 内容仍是当前开发依据，需要迁入新结构并对照代码修正 |
| 合并提炼 | 不直接保留原文，把有效结论合入权威文档 |
| 迁移改名 | 内容基本可用，移动到新目录并按命名规则改名 |
| 归档 | 历史过程材料，仅保留追溯价值 |
| 建立跳转 | 原位置保留短 README，指向新权威位置 |
| 待确认 | 暂不能判断是否仍有效，需要后续人工或代码核对 |
| 删除候选 | 仅在确认无追溯价值后删除；默认先不删 |

## 总体结论

1. `docs/project-handbook/` 是当前最接近权威源的目录，应作为 `docs/handbook/` 的主要来源。
2. `docs/superpowers/` 主要是历史计划、设计草案和 review，应整体转入 `archive/`，但先提炼其中仍有效的结论。
3. `frontend/apps/admin/docs/` 记录了后台近期大量真实开发进度，应迁入 `docs/handbook/frontend/admin/`，原目录只保留跳转说明。
4. `docs/archive/frontend-user-v2-migration/` 已经是归档资料，原则上不重写，只补归档说明。
5. 根目录下的展示、多语言和调研文档应从开发手册中分离，分别进入 `showcase/`、`working/research/` 或 `archive/`。
6. 未完成和争议事项不应继续散在 roadmap、plan、review 中，应统一进入 `docs/handbook/start-here/open-issues.md`。

## 目录级盘点

| 当前路径 | 类型 | 当前状态 | 建议动作 | 目标路径 | 备注 |
|----------|------|----------|----------|----------|------|
| `docs/MyBlog-V2-docs-reorganization-plan.md` | 整理计划 | 当前草案 | 迁移改名 | `docs/working/plans/2026-06-29-docs-reorganization-plan.md` | 当前作为本轮整理依据，完成后归档 |
| `docs/project-handbook/` | V2 开发手册 | 接近权威源但路径和内容需校准 | 保留校准 | `docs/handbook/` | 本轮迁移主体 |
| `docs/project-handbook/CLAUDE.md` | AI 入口 | 当前有效但需改路径引用 | 合并提炼 | `docs/handbook/start-here/ai-entry.md` 或根 `AGENTS.md` 引用 | 后续避免和总 README 重复 |
| `docs/project-handbook/INDEX.md` | 文档索引 | 当前有效但路径已计划变化 | 合并提炼 | `docs/handbook/README.md` | 新结构确认后重写 |
| `docs/project-handbook/overview.md` | 项目概览 | 部分过期 | 保留校准 | `docs/handbook/start-here/project-overview.md` | 需补 frontend/apps 当前状态 |
| `docs/project-handbook/status.md` | 当前进度 | 应作为状态权威源但需校准 | 保留校准 | `docs/handbook/start-here/current-status.md` | 已完成/未完成事项统一整理 |
| `docs/project-handbook/roadmap.md` | 路线图 | 内容有历史阶段沉积 | 保留校准 | `docs/handbook/start-here/roadmap.md` | 已完成内容移到 current-status |
| `docs/project-handbook/pitfalls.md` | 红线与踩坑 | 长期有效 | 迁移改名 | `docs/handbook/start-here/pitfalls.md` | 可继续作为权威文档 |
| `docs/project-handbook/arch/` | 架构现状 | 多数仍有效 | 保留校准 | `docs/handbook/architecture/` | 对照当前代码修正 |
| `docs/project-handbook/rules/` | 开发规则 | 多数仍有效 | 保留校准 | `docs/handbook/rules/` | 新增 `documentation.md` |
| `docs/project-handbook/decisions/` | ADR | 长期有效 | 迁移改名 | `docs/handbook/adr/` | 文件名保持编号 |
| `docs/project-handbook/api-contract/` | API 契约 | 需要重点校准 | 保留校准 | `docs/handbook/api/` | 对照 Controller/VO/错误码 |
| `docs/project-handbook/product/` | 产品与业务规格 | 多数仍有效 | 保留校准 | `docs/handbook/product/` | `decisions-draft.md` 需判断是否转 ADR/open issue |
| `docs/project-handbook/frontend-user/` | 前台规格 | 当前有效但过于集中 | 合并提炼 | `docs/handbook/frontend/blog/` | 拆成 README、routing、integration-status 等 |
| `docs/project-handbook/workflows/` | SOP | 多数仍有效 | 迁移改名 | `docs/handbook/workflows/` 或 `docs/handbook/ops/` | build/local/release 更适合 ops |
| `docs/project-handbook/migration/` | V1 到 V2 迁移 | 部分仍可能有用 | 待确认 | `docs/handbook/migration/` 或 `docs/archive/migration/` | 取决于是否还会执行数据导入 |
| `docs/project-handbook/plans/` | 阶段计划 | 历史过程材料 | 归档 | `docs/archive/project-handbook/plans/` | 未完成事项提炼进 open-issues |
| `docs/project-handbook/specs/` | 阶段设计 | 多为已落地设计 | 合并提炼 | `docs/archive/project-handbook/specs/` | 有效结论进入 architecture/api/rules |
| `docs/project-handbook/codex的review.md` | review | 命名不统一，需判断价值 | 待确认 | `docs/working/reviews/` 或 `docs/archive/reviews/` | 先读后决定提炼内容 |
| `docs/project-handbook/MIGRATION-STATUS.md` | 文档迁移状态 | 当前仅描述旧迁移 | 合并提炼 | `docs/working/reviews/` | 可被新盘点表替代 |
| `docs/superpowers/` | 旧计划/spec/review 集合 | 历史过程材料 | 归档 | `docs/archive/superpowers/` | 搬迁前提炼近期有效结论 |
| `docs/archive/frontend-user-v2-migration/` | 前台旧迁移档案 | 已归档 | 保留归档 | `docs/archive/frontend-user-v2-migration/` | 可补 archive README，不重写细节 |
| `docs/repository-governance/` | 仓库治理 | 仍有效 | 迁移改名 | `docs/governance/` | 分支策略、仓库整理计划归这里 |
| `docs/local-development.md` | 本地开发说明 | 与 handbook ops 重复 | 合并提炼 | `docs/handbook/ops/local-development.md` | 环境变量转 `environment.md` |
| `docs/deep-research-report.md` | 调研报告 | 不适合作为权威源 | 待确认 | `docs/working/research/` 或 `docs/archive/research/` | 先判断是否仍有行动项 |
| `docs/refactor-plan.zh-CN.md` | 旧重构计划 | 历史计划 | 归档 | `docs/archive/refactor-plan.zh-CN.md` | 当前结论已进入 handbook/roadmap |
| `docs/MyBlog-项目展示.md` | 项目展示 | 可保留 | 迁移改名 | `docs/showcase/myblog-showcase.zh-CN.md` | 展示文档允许多语言 |
| `docs/MyBlog-Project-Showcase.en.md` | 项目展示 | 可保留 | 迁移改名 | `docs/showcase/myblog-showcase.en.md` | 不作为开发文档 |
| `docs/MyBlog-プロジェクト紹介.ja.md` | 项目展示 | 可保留 | 迁移改名 | `docs/showcase/myblog-showcase.ja.md` | 不作为开发文档 |
| `frontend/apps/admin/docs/` | 后台设计/计划/验收 | 近期有效材料 | 合并提炼 | `docs/handbook/frontend/admin/` + `docs/working/plans/` | 原目录建立跳转 README |

## `frontend/apps/admin/docs/` 初步拆分

| 当前文件组 | 建议动作 | 目标文档 | 备注 |
|------------|----------|----------|------|
| `2026-06-20-admin-foundation-design.md` | 合并提炼 | `docs/handbook/frontend/admin/foundation.md` | 后台工程基础、权限和国际化 |
| `2026-06-20-admin-foundation-plan.md` | 归档 | `docs/archive/frontend-admin/plans/` | 已执行计划，结论进入 foundation/auth-session |
| `2026-06-21-admin-article-list-design.md` | 合并提炼 | `docs/handbook/frontend/admin/article-management.md` | 文章列表当前事实 |
| `2026-06-21-admin-article-list-plan.md` | 归档 | `docs/archive/frontend-admin/plans/` | 已执行计划 |
| `2026-06-21-article-editor-design.md` | 合并提炼 | `docs/handbook/frontend/admin/article-management.md` | 若编辑器未完全完成，未完成部分进 open-issues |
| `2026-06-21-article-editor-plan.md` | 归档/待确认 | `docs/archive/frontend-admin/plans/` | 对照代码确认完成度 |
| `2026-06-21-category-tag-management-*` | 合并提炼 | `docs/handbook/frontend/admin/category-tag-management.md` | 若页面已完成则标已完成，否则入 open-issues |
| `2026-06-22-article-lifecycle-management-*` | 合并提炼 | `docs/handbook/frontend/admin/article-management.md` | acceptance 可提炼到 integration-status |
| `2026-06-23-comment-management-*` | 合并提炼 | `docs/handbook/frontend/admin/comment-management.md` | 需确认评论管理当前实现 |
| `2026-06-24-friend-link-management-*` | 合并提炼 | `docs/handbook/frontend/admin/friend-link-management.md` | 需确认完成状态 |
| `2026-06-25-attachment-management-*` | 合并提炼 | `docs/handbook/frontend/admin/attachment-management.md` | 需确认上传和选择器状态 |
| `2026-06-25-site-config-profile-management-*` | 合并提炼 | `docs/handbook/frontend/admin/site-config-management.md` | 需确认配置页状态 |
| `2026-06-26-*plan.md` | 待确认 | `docs/working/plans/` 或 `docs/archive/frontend-admin/plans/` | 判断是否已完成 |
| `README.md` | 建立跳转 | 保留在原目录 | 指向 `docs/handbook/frontend/admin/` |
| `*.png` | 保留或迁移 | `docs/handbook/frontend/admin/assets/` 或 `archive` | 与 QA 对比相关，按引用情况决定 |

## 首批应建立的权威文档

| 目标路径 | 来源 | 目的 | 优先级 |
|----------|------|------|--------|
| `docs/README.md` | 新建 | 全 docs 总入口 | P1 |
| `docs/handbook/README.md` | `project-handbook/INDEX.md` | 当前权威手册入口 | P1 |
| `docs/handbook/start-here/current-status.md` | `project-handbook/status.md` + roadmap + admin docs | 统一当前进度 | P1 |
| `docs/handbook/start-here/open-issues.md` | roadmap + plans + reviews | 统一未完成和争议项 | P1 |
| `docs/handbook/start-here/glossary.md` | 新建 | 统一术语 | P1 |
| `docs/handbook/rules/documentation.md` | 新建 | 文档维护规则 | P1 |
| `docs/handbook/ops/environment.md` | overview + local-development + build-and-test | 环境变量唯一权威源 | P2 |
| `docs/handbook/frontend/admin/integration-status.md` | admin docs + 当前代码 | 后台完成度 | P2 |
| `docs/handbook/frontend/blog/integration-status.md` | frontend-user README + 当前代码 | 前台完成度 | P2 |

## 首批 open issues 候选

| 编号 | 事项 | 当前判断 | 来源 |
|------|------|----------|------|
| O-001 | PASSWORD 文章完整解锁流程 | 未完成/有争议 | roadmap、security-baseline、auth-flow |
| O-002 | DEMO 敏感字段裁剪边界 | 需要逐页确认 | security-baseline、admin docs |
| O-003 | 前台分类/标签/归档/友链/关于/搜索 | 未完成 | frontend-user README、roadmap |
| O-004 | 前台评论/留言/统计接入 | 未完成 | frontend-user README、roadmap |
| O-005 | 后台文章编辑器、附件上传和内容生产闭环 | 需确认完成度 | admin docs、roadmap |
| O-006 | 后台其他业务页完成度 | 需确认完成度 | admin docs、roadmap |
| O-007 | RSS、Sitemap、SEO、部署和备份 | 未完成 | roadmap |
| O-008 | localStorage token 存储是否升级 | 暂缓/有争议 | auth 实现、安全评估 |
| O-009 | 多实例部署下限流方案 | 后置 | security-baseline |

## 下一步建议

1. 先执行 P1：建立 `docs/README.md`、`docs/handbook/README.md`、`open-issues.md`、`glossary.md`、`documentation.md`。
2. 不立刻移动 `project-handbook/`，先让新入口能解释“当前以旧目录为来源，正在迁移”。
3. P1 完成后，再迁移 `project-handbook/status.md`、`roadmap.md`、`overview.md` 到 `handbook/start-here/` 并校准内容。
4. 后台文档单独作为一个批次处理，因为它的完成度和计划状态需要对照 `frontend/apps/admin/src/`。
