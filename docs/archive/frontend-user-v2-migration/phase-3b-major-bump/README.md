# Phase 3b · 跨 major 依赖升级

> 升 2 个**跨 major** 的工具链：Vite 4 → 5、ESLint 8 → 9。每个 step 独立 commit，便于单独 revert。

---

## 入口条件

- Phase 3a 已完成（`phase-3a-done` tag 存在）
- `vite` / `vite build` 都跑得通

---

## 本 phase 做什么

| Step | 升级 | 关键变化 | 风险 |
|---|---|---|---|
| **3b.1** | Vite 4 → 5（+ plugin-vue 5 → 6, vite-plugin-pages 0.31 → 0.33） | `vite.config.js` 必须重命名为 `.mjs`（CJS → ESM） | 🟡 中 |
| **3b.2** | ESLint 8 → 9 + 全套配套插件 | `.eslintrc.js` 必须重写为 `eslint.config.mjs` (flat config) | 🟡 中 |

---

## 本 phase 不做什么

- ❌ 不升 Vite 6/7（让生态再稳一阵）
- ❌ 不修工程历史 lint 报错（22 个 ts/no-unused-vars + 8 个 no-console，Phase 4 一起清）
- ❌ 不升 Tailwind 3→4（放 Phase 9a，可选）
- ❌ 不升 vue-i18n 9→11（放 Phase 6b）

---

## 子文档

1. [01-pre-flight.md](./01-pre-flight.md) — 打 tag、看当前版本
2. [02-vite-5.md](./02-vite-5.md) — Step 3b.1 完整流程（含 .mjs 改名）
3. [03-eslint-9.md](./03-eslint-9.md) — Step 3b.2 flat config 迁移
4. [04-verify.md](./04-verify.md) — 总出口验收
5. [05-troubleshooting.md](./05-troubleshooting.md) — 沙盒踩过的 4 个坑

---

## 出口

- Vite 在 5.4.x；plugin-vue 在 6.x
- ESLint 在 9.x；`.eslintrc.js` / `.eslintignore` 已删，`eslint.config.mjs` 在位
- `vite` / `vite build` / `eslint .` 三件套都跑得通
- commit + tag `phase-3b-1-done` 和 `phase-3b-2-done`（最终 `phase-3b-done`）

---

## 下一步

→ [Phase 4 · `<script setup>` 迁移](../phase-4-script-setup/)（待撰写）

Phase 4 会做：

- 把 61 个 `.vue` 文件从 `defineComponent({ setup() })` 迁到 `<script setup>`
- 顺手清掉本 phase 留下的历史 lint 错误（unused vars 等）
- 分 4a / 4b / 4c 三个子步（pages → components → app shell）
