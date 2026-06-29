# 后台真实统计仪表盘实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前空 Dashboard 替换为复用后端真实统计接口的后台统计总览。

**Architecture:** 前端新增 stats API 与 dashboard 领域模型，Dashboard 页面在挂载时请求 `/api/admin/stats/dashboard`。页面只展示后端返回的真实数据，提供 loading、error retry、empty state，不新增后端接口、不伪造统计。

**Tech Stack:** Vue 3 `<script setup>`、Element Plus、Vitest、axios-mock-adapter、现有 `http` 封装与 i18n YAML。

---

## 文件结构

- Create: `frontend/apps/admin/src/features/dashboard/model.ts`
  - 定义 `StatsDashboard`、`StatsTrendPoint`、`StatsTopArticle`、`StatsLanguageDistribution` 类型。
- Create: `frontend/apps/admin/src/features/dashboard/useStatsDashboard.ts`
  - 管理 dashboard 加载、错误、刷新和空数据判断。
- Create: `frontend/apps/admin/src/api/stats.ts`
  - 封装 `GET /api/admin/stats/dashboard`，query 参数为可选 `from`、`to`。
- Modify: `frontend/apps/admin/src/features/dashboard/index.vue`
  - 接入真实统计数据，保留账号概览和 DEMO 只读提示。
- Modify: `frontend/apps/admin/src/features/dashboard/index.test.ts`
  - 先写失败测试，覆盖真实数据渲染、空态、错误重试、DEMO 只读提示。
- Modify: `frontend/apps/admin/locales/zh-CN.yaml`
- Modify: `frontend/apps/admin/locales/ja.yaml`
- Modify: `frontend/apps/admin/locales/en.yaml`
  - 补齐 dashboard 统计文案。
- Modify: `frontend/apps/admin/docs/README.md`
  - 更新 Dashboard 状态，不再标记为空仪表盘。

## Task 1: Dashboard API 与状态管理

- [ ] Step 1: 在 `frontend/apps/admin/src/features/dashboard/index.test.ts` 写失败测试。
  - mock `GET /api/admin/stats/dashboard` 返回：
    - `periodPv: 1234`
    - `todayPv: 56`
    - `todayUv: 34`
    - `averageDailyUv: 12.3`
    - `trend: [{ date: "2026-06-25", pv: 100, uv: 20 }]`
    - `topArticles: [{ articleId: "9007199254743001", title: "文章 A", pv: 88, dailyUvSum: 30 }]`
    - `languageDistribution: [{ language: "zh", pv: 800, ratio: 0.648 }]`
  - 断言页面出现 `data-testid="dashboard-metric-period-pv"`、`data-testid="dashboard-top-article-9007199254743001"` 和 `data-testid="dashboard-language-zh"`。
- [ ] Step 2: 运行 `npm test -- dashboard/index`，预期失败，原因是当前页面没有请求 stats 接口，也没有 metric 区块。
- [ ] Step 3: 新增 `model.ts`、`api/stats.ts`、`useStatsDashboard.ts`，实现最小加载状态。
- [ ] Step 4: 修改 `index.vue` 渲染 summary cards、trend list、top article list、language distribution list。
- [ ] Step 5: 运行 `npm test -- dashboard/index`，预期通过。
- [ ] Step 6: `git diff --stat` 和 `git status --short` 确认仅包含 Dashboard/API/i18n 相关文件。
- [ ] Step 7: 提交：`git commit -m "接入后台真实统计仪表盘"`。

## Task 2: 错误、空态与文档

- [ ] Step 1: 在 `index.test.ts` 增加错误重试测试。
  - 第一次 `/api/admin/stats/dashboard` 返回 500。
  - 页面显示 `data-testid="dashboard-error"`。
  - 点击 `data-testid="dashboard-retry"` 后第二次返回成功。
- [ ] Step 2: 在 `index.test.ts` 增加空态测试。
  - 返回所有计数为 0 且三个列表为空。
  - 页面显示 `data-testid="dashboard-empty"`。
- [ ] Step 3: 运行 `npm test -- dashboard/index`，预期新增测试先失败。
- [ ] Step 4: 补齐 `useStatsDashboard.ts` 的 error、empty、refresh 状态，补齐页面渲染。
- [ ] Step 5: 更新 `frontend/apps/admin/docs/README.md`。
- [ ] Step 6: 运行 `npm test -- dashboard/index`，预期通过。
- [ ] Step 7: 运行完整验证：
  - `npm test`
  - `npm run typecheck`
  - `npm run build`
- [ ] Step 8: 提交：`git commit -m "完善统计仪表盘状态与文档"`。

## 自检

- 本计划只接入现有后端接口，不新增接口。
- 首版不引入 ECharts 图表，以降低复杂度；用卡片和列表先保证真实数据可用。
- DEMO 只读提示保留，但不阻止读取统计。
- 所有行为先写测试并观察失败，再实现。
