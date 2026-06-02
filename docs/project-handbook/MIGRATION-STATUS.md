# 文档迁移状态

> 跟踪从旧 `docs/superpowers/` 到本目录的内容迁移进度。
> 迁移完成的条目，旧文档保留为历史档案，新文档为权威源。

## 迁移原则

- **specs/ 中的"规则类"内容** → 拆分迁入 `rules/`
- **specs/ 中的"决策理由"** → 回填为 `decisions/` 下的 ADR
- **specs/ 中的"架构方案"** → 提炼现状部分迁入 `arch/`
- **reviews/ 中的教训** → 提炼为 `pitfalls.md` 条目
- **plans/** → 不迁移（一次性消耗品，留在旧目录作为档案）

## 迁移清单

| 旧文档 | 类型 | 目标位置 | 状态 |
|--------|------|----------|------|
| specs/2026-05-22-myblog-v2-refactor-design.md | 总体设计 | arch/* + decisions/0001/0003/0004 | ✅ 已提炼 |
| specs/2026-05-25-backend-v2-login-audit-design.md | 设计 | arch/auth-flow.md + decisions/0007 | ✅ 已提炼 |
| specs/2026-05-31-backend-v2-code-comment-standards.zh-CN.md | 规则 | rules/comment-style.md + decisions/0011 | ✅ 已提炼 |
| specs/2026-05-31-backend-v2-package-structure-decisions.zh-CN.md | 规则+决策 | rules/package-layout.md + decisions/0002/0003/0004 | ✅ 已提炼 |
| specs/2026-05-31-backend-v2-technology-decisions.zh-CN.md | 决策 | decisions/0005/0006/0007/0008/0009/0012 | ✅ 已提炼 |
| specs/2026-06-01-backend-v2-persistence-sql-placement-rules.zh-CN.md | 规则 | rules/sql-placement.md + decisions/0010 | ✅ 已提炼 |
| reviews/2026-05-24-backend-v2-identity-permission-inventory.zh-CN.md | 回顾 | pitfalls.md（P-003/P-004/P-006）| ✅ 已提炼 |
| reviews/2026-05-24-backend-v2-security-phase-review.zh-CN.md | 回顾 | pitfalls.md + arch/auth-flow.md | ✅ 已提炼 |
| reviews/2026-05-28-backend-v2-identity-menu-phase-review.zh-CN.md | 回顾 | pitfalls.md（U-001 system 模块）| ✅ 已提炼 |
| reviews/2026-05-31-backend-v2-phase-review.zh-CN.md | 回顾 | pitfalls.md + status.md | ✅ 已提炼 |

## 注意

- "已提炼"指核心结论已纳入新文档，旧文件作为档案保留，不再更新
- 若新发现旧文档中遗漏的细节，直接补到新文档对应位置，并在此表添注
- 新增 review 不再回到旧 reviews/ 目录，按"踩坑追加 pitfalls.md / 进展更新 status.md"原则即可

## 状态图例

- ⏳ 未开始
- 🚧 进行中
- ✅ 已完成
- ❌ 决定不迁移（说明原因）
