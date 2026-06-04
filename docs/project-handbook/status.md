# 当前进度

> 本文档回答："V2 现在做到哪了？接下来按什么主线推进？"
> 更新时机：每个里程碑完成后更新。
> 当前日期：2026-06

## ⚠️ 当前唯一主线：业务规格 + Schema 设计

**V2 后端实现暂停**。R5-R7 完成后多处与既有代码冲突（ADR-0015 审计列三件套、ADR-0017 无 FK、R5 模块边界含 stats 与 common-infra、ADR-0018 时区五层、Role 三态枚举等），继续修旧实现 = 返工。

**在以下文档完成前，不得新增 Entity / Mapper / Controller / Flyway DDL：**

- `product/use-cases.md`
- `product/business-rules.md`
- `product/data-model.md`
- `arch/schema-design.md`
- 更新后的模块边界 ADR（覆盖或取代 ADR-0004）

允许做的：文档收敛 / 对 V2 代码只读盘点 / 旧实现 vs 新 schema 差距清单 / 不产生新业务实现的构建验证。

## 1. 文档体系状态

| 类别 | 状态 |
|---|---|
| 产品决策 R1-R7（`product/decisions-draft.md`） | ✅ 完成 |
| V1 功能清单（`product/feature-inventory.md`） | ✅ ⑳ 项全部回填决策 |
| ADR 0001-0018 | ✅ 0014 部分被 0015 / 0018 超越；0015-0018 为 R5-R7 衍生 |
| 红线（`pitfalls.md`） | ✅ R-001 ~ R-013 |
| Rules（`api-response.md` / `error-handling.md` / `security-baseline.md` 等） | ✅ 按 R5-R7 重写完毕 |
| `arch/auth-flow.md` | ✅ 双 token 完整流程 |
| `arch/schema-design.md` | 📝 DDL 草案已生成并完成硬规则修正；Flyway V1__init.sql 已生成并烟测通过，待评审冻结 |
| `product/use-cases.md` / `product/data-model.md` / `product/business-rules.md` / `product/er-diagram.md` | ✅ 已生成 |
| Codex review 反馈消化 | ✅ P0/P1/P2 已处理 |

## 2. V2 后端代码现状（只读盘点）

详见 `MIGRATION-STATUS.md`（待更新）。摘要：

- 包结构 `com.tyb.myblog.v2` 符合 ADR-0002
- 已写：`common/`（错误码 / 响应包装 / Security 链路 / Exception handler / ArchUnit）、`infrastructure/security/`、CategoryEntity / TagEntity、6 个 Controller 骨架
- 未写：BaseEntity / AuditFieldHandler / Clock Bean / i18n 配置 / 14 张表 Flyway 初始化脚本 / 核心实体（Article / Comment / User 等）
- 与新决策冲突：`is_delete` 单列软删（→需改三件套）、USER/ADMIN 二态（→需改 ADMIN/DEMO/GUEST）、AUTO_INCREMENT 主键（→需改 ASSIGN_ID）

**处置方案**：DDL 冻结后清掉业务层（content / comment / identity 三个模块的 domain/application/web），保留基础设施层（common / Security / ArchUnit），再按新 schema 长出新业务代码。

## 3. 下一步推进顺序（详见 `roadmap.md`）

1. 人工评审 `product/er-diagram.md`、`product/use-cases.md`、`product/business-rules.md`、`product/data-model.md`
2. 人工评审 `arch/schema-design.md` + Flyway `V1__init.sql`
3. **DDL 冻结里程碑**
4. V2 后端代码清理 + 重建（按 `MIGRATION-STATUS.md` checklist）
5. 前台 / 后台前端骨架

## 4. 相关文档

- 路线图：`roadmap.md`
- 历史与红线：`pitfalls.md`
- V1 vs V2：`v1-vs-v2.md`
- 文档索引：`INDEX.md`
