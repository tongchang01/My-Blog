# 文档索引（给所有 AI 工具阅读）

本目录的文档供任意 AI 编码助手（Claude Code、Cursor、Copilot、Codex、通义灵码等）和人类开发者共同使用。
本目录位于仓库内 `docs/project-handbook/`，是 MyBlog V2 重构的长期文档入口。

## 当前最高优先级原则

1. **V2 不兼容 V1 数据库结构。** V2 schema 重新设计，V1 表只作为业务语义参考，数据通过一次性导入迁移。
2. **当前后端业务代码是过渡实现。** 安全、异常、响应、包结构、MyBatis-Plus、ArchUnit 等基盘保留；identity/content/comment 的最终业务实现以后续产品规格、领域模型和新 schema 为准。
3. **长期规则看本目录，阶段任务看 `docs/superpowers/plans/`。** 如果长期规则与历史计划冲突，以本目录中的 ADR、rules、arch 为准。

## 阅读顺序建议

1. **首次进入项目** → 先读 `CLAUDE.md`（AI 工作入口）和 `overview.md`（项目概览）
2. **写代码前** → 必查 `rules/` 下相关规则文件
3. **遇到不理解的设计** → 翻 `decisions/` 找原因
4. **想了解架构现状** → 看 `arch/`
5. **要执行某项操作（构建、新增模块等）** → 查 `workflows/`
6. **任何时候** → `pitfalls.md` 记录的红线必须避开

## 目录结构与用途

```
docs/project-handbook/
├── INDEX.md              本文件 — 文档目录总说明
├── CLAUDE.md             AI 工作入口，首要阅读
├── overview.md           项目是什么 / 技术栈 / 目录布局 / 构建命令
├── status.md             当前进度（每次开工先看）
├── v1-vs-v2.md           V1 问题清单 + V2 重构方向
├── roadmap.md            短期 / 中期 / 长期路线图
├── pitfalls.md           【红线】+ 历史踩坑 + 未解决项
├── MIGRATION-STATUS.md   旧 docs/superpowers/ 内容迁移进度
│
├── rules/                【强约束】写代码必须遵守的规则
│   ├── README.md
│   ├── package-layout.md
│   ├── sql-placement.md
│   ├── comment-style.md
│   ├── error-handling.md
│   ├── api-response.md
│   ├── security-baseline.md
│   └── testing-policy.md
│
├── arch/                 【现状描述】当前架构快照
│   ├── README.md
│   ├── module-map.md
│   ├── persistence-strategy.md
│   ├── auth-flow.md
│   └── request-flow.md
│
├── decisions/            【为什么】ADR
│   ├── README.md
│   ├── 0001-modular-monolith.md
│   ├── 0002-package-base-com-tyb-myblog-v2.md
│   ├── 0003-four-layer-architecture.md
│   ├── 0004-six-business-modules.md
│   ├── 0005-mybatis-plus-as-primary-orm.md
│   ├── 0006-upgrade-to-spring-boot-3.md
│   ├── 0007-jwt-via-spring-security-jose.md
│   ├── 0008-hutool-scoped-usage.md
│   ├── 0009-springdoc-replaces-knife4j.md
│   ├── 0010-sql-placement-strategy.md
│   ├── 0011-chinese-only-comments.md
│   ├── 0012-archunit-guards.md
│   ├── 0013-no-v1-compatibility.md
│   └── 0014-schema-redesign-principles.md
│
├── workflows/            【怎么做】可重复 SOP
│   ├── README.md
│   ├── add-new-module.md
│   ├── add-new-api.md
│   ├── migrate-jdbc-to-mybatis-plus.md
│   ├── add-new-table.md
│   ├── build-and-test.md
│   └── write-adr.md
│
├── product/              【业务规格】要做什么（先于代码与表）
│   ├── README.md
│   └── feature-inventory.md   ← V1 全功能清单，待人工标注去/留
│
├── api-contract/         【三端共识】HTTP 接口契约（前后端单一事实源）
│   └── README.md
│
├── frontend-user/        【前台规格】博客访客端
│   └── README.md
│
├── frontend-admin/       【后台规格】管理员端
│   └── README.md
│
└── migration/            【数据迁移】V1 → V2 一次性导入
    └── README.md
```

## 各类文档的差异（避免混淆）

| 类型 | 回答的问题 | 时效性 | 文件命名 |
|------|-----------|--------|----------|
| **rules/** | "怎么写才对？" | 长期有效，随项目演进 | 主题命名，如 `sql-placement.md` |
| **arch/** | "现在长什么样？" | 反映当下，过期即更新 | 主题命名，如 `module-map.md` |
| **decisions/** | "为什么这么定？" | 永久保留，被取代时标注 superseded | 编号 + 主题，如 `0001-use-mybatis-plus.md` |
| **workflows/** | "我要做 X，步骤是？" | 长期有效 | 动作命名，如 `add-new-module.md` |
| **product/** | "要做什么业务？" | 随业务演进 | 主题命名 |
| **api-contract/** | "前后端约定的接口是什么？" | 与代码同步 | 主题命名 |
| **frontend-user/**, **frontend-admin/** | "前端做什么？长什么样？" | 与代码同步 | 主题命名 |
| **migration/** | "V1 数据怎么导到 V2？" | 一次性，导完即归档 | 主题命名 |
| **pitfalls.md** | "不要这样做" | 持续追加，不删 | 单一文件 |

## 写作约定

- **语言**：统一中文。不维护英文/日文副本。
- **文件名**：英文小写 + 短横线，如 `sql-placement.md`；不使用日期前缀（除 ADR 编号）。
- **文件大小**：单文件控制在 5–10KB，超过就该拆分。
- **每文件开头**：以一段"本文档回答什么问题 / 适用范围"开始。
- **更新时**：直接修改原文件，历史变更靠 git log 追溯；重大语义变化在文末附"更新历史"。
- **被取代时**：原文件保留，开头加 `> ⚠️ 已被 xxx.md 取代`，避免 AI 误读。

## 与旧文档（docs/superpowers/）的关系

- `docs/superpowers/plans/` 是阶段性执行计划和历史记录，任务完成后会过期。
- `docs/superpowers/specs/` 中仍保留部分已落地规范，后续可逐步迁入本目录。
- 本目录是长期规则、架构、ADR、产品、接口契约和迁移文档入口。如两者冲突，**以本目录为准**。
- 迁移进度：见 `MIGRATION-STATUS.md`。
