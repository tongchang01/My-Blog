# Phase 3a · 同 major 依赖小升级

> 把 7 个**不跨 major** 的核心包升到当前稳定版。零 breaking，一条命令搞定。

---

## 入口条件

- Phase 2 已完成（`phase-2-done` tag 存在）
- 工程 `pnpm install` / `vite` / `vite build` 都跑得通
- `tsconfig.json` 已在 Phase 2 Step 2.3 清理过 `jest` 引用

---

## 本 phase 做什么

| 包 | 当前 | 目标 | 风险 |
|---|---|---|---|
| `vue` | ^3.3.4 | ~3.5 | 极低 |
| `vue-router` | ^4.2.4 | ~4.5 | 极低 |
| `pinia` | 2.1.6 | ~2.3 | 极低 |
| `typescript` | ^5.1.0 | ~5.6 | 极低 |
| `axios` | ^1.5.0 | ~1.7 | 极低 |
| `@vitejs/plugin-vue` | ^4.3.4 | **~5**（不要跳 6） | 中（plugin-vue 6 是 ESM-only） |
| `@types/node` | ^20.5.7 | latest | 极低 |

⚠️ **特别注意 `@vitejs/plugin-vue`**：v6 要求 Vite 5+，而本 phase 还在 Vite 4 上。沙盒实测：升到 v6 后 `vite build` 直接报 `"@vitejs/plugin-vue" resolved to an ESM file. ESM file cannot be loaded by 'require'`。所以本 phase **必须固定 `~5`**。升 v6 留到 Phase 3b（Vite 4→5）一起做。

---

## 本 phase 不做什么

- ❌ 不升 Vite、ESLint（跨 major，放 Phase 3b）
- ❌ 不升 Tailwind 3→4（放 Phase 9a，可选）
- ❌ 不升 vue-i18n 9→11（放 Phase 6b）
- ❌ 不修工程里已存在的潜在 TS 错误（`stores/routers.ts` 等 ~7 处）；这些是历史问题，放 Phase 4 `<script setup>` 迁移时一起清

---

## 子文档

1. [01-pre-flight.md](./01-pre-flight.md) — 打 tag、检查现版本
2. [02-upgrade.md](./02-upgrade.md) — 一条 `pnpm up` 命令
3. [03-verify.md](./03-verify.md) — 5 项出口验收
4. [04-troubleshooting.md](./04-troubleshooting.md) — 常见报错对照

---

## 出口

- 7 个包到目标版本
- `vite` / `vite build` 均通过
- commit + tag `phase-3a-done`
- 工程从 Vue 3.3 进入 3.5 时代

---

## 下一步

→ [Phase 3b · 跨 major 升级（Vite 4→5、ESLint 8→9）](../phase-3b-major-bump/)
