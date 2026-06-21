# Phase 5c · 清理与最终验证

> **目标**：清理 Phase 5a/5b 遗留项，移除 unused imports，确保全局 TS 0 错误，代码无冗余。
>
> **耗时**：30-40 分钟
>
> **沙盒验证**：✅ 通过（3 commits，10 files cleaned，TS errors 0，build 成功）

---

## Phase 5c 范围

| 任务 | 状态 | 文件数 | 说明 |
|---|---|---|---|
| App.vue 迁移 | ✅ 完成 | 1 | 最后一个根组件迁移到 `<script setup>` |
| Pages 清理 | ✅ 完成 | 5 | 移除 unused imports (useI18n, components) |
| Components 清理 | ✅ 完成 | 3 | 移除 unused variables (computed, refs) |
| TS 验证 | ✅ 通过 | N/A | `tsc --noEmit` 0 errors |
| Build 验证 | ✅ 通过 | N/A | 442.80 KB bundle，成功构建 |

---

## 为什么 Phase 5c 是必要的

Phase 5a 和 5b 专注于迁移语法（`defineComponent({ setup() })` → `<script setup>`），但留下了：
1. **Unused imports** — `defineComponent`、`useI18n`、组件导入
2. **Unused variables** — computed、ref 变量被定义但从未使用
3. **最后的 defineComponent** — `src/App.vue` 仍用 Options API

Phase 5c 是 "扫尾清洁" 阶段，确保代码库干净、无冗余、TS 零错误。

---

## 核心改动总结

### 1. App.vue 迁移（Commit 1）

**文件**：`src/App.vue`

**改动**：
- ❌ 移除：`defineComponent({ setup() })`，`components: { ... }`
- ✅ 新增：`<script setup>` 顶层语法
- ❌ 移除未使用：`useI18n` 的 `t`、`scripts`、`handleEscKey`、`configReady`

**净行数**：-30 行（166 → 136）

**Commit message**：
```
chore(phase-5c): 迁移 App.vue 到 script setup 语法
```

---

### 2. Pages 清理（Commit 2）

**文件**：
- `src/pages/about.vue`
- `src/pages/index.vue`
- `src/pages/links.vue`
- `src/pages/page/[slug].vue`
- `src/pages/post/search/index.vue`

**清理项**：

| 文件 | Removed |
|---|---|
| about.vue | `import { useI18n }` / `const { t } = useI18n()` |
| index.vue | `import { MainTitle }`，`gradientText` computed，`gradientBackground` computed |
| links.vue | `import LinkCard`，`import { useI18n }` / `const { t }` |
| page/[slug].vue | `import { useI18n }` / `const { t }` |
| post/search/index.vue | `const pageType = ref('search')` |

**净行数**：-19 行

**Commit message**：
```
chore(phase-5c): 清理 App.vue 和 pages 目录的 unused imports
```

---

### 3. Components 清理（Commit 3）

**文件**：
- `src/components/ArticleCard/src/HorizontalArticle.vue`
- `src/components/PageContent.vue`
- `src/components/Sticky.vue`

**清理项**：

| 文件 | Removed |
|---|---|
| HorizontalArticle.vue | `const isMobile = computed(() => commonStore.isMobile)` (直接用 `commonStore.isMobile`) |
| PageContent.vue | `import { useI18n }` / `const { t }` |
| Sticky.vue | `const newTop = ref(0)` |

**净行数**：-4 行

**Commit message**：
```
chore(phase-5c): 清理 components 目录的 unused imports
```

---

## 验证结果

### ✅ TypeScript 验证

```bash
./node_modules/.bin/tsc --noEmit
```

**结果**：✅ **0 errors**

---

### ✅ Build 验证

```bash
npm run build
```

**结果**：✅ 成功构建

**Bundle size**：
- Main JS: **442.80 KB**（gzip: 153.91 KB）
- Total CSS: **103.23 KB**（gzip: ~19 KB）
- Build time: **4.36s**

**对比 Phase 5b**：
- Phase 5b 预期：~448 KB
- Phase 5c 实际：442.80 KB
- 优化：**-5.2 KB** (-1.2%)

---

## Stores 评估结果

**检查了** `src/stores/` 目录（12 个 store 文件）：
- **当前状态**：Pinia Options API 风格（`defineStore({ state, getters, actions })`）
- **决策**：✅ **保持现状**
- **原因**：
  1. Pinia Options API 是官方推荐的标准写法之一
  2. 功能正常，性能无差异
  3. 迁移到 Setup 风格收益低，风险高
  4. 后续有需要再单独评估

---

## LoadingSkeleton 决策

**评估结果**：
- **当前状态**：2 个文件保留 `defineComponent` + `render()` 函数
- **决策**：✅ **保持现状**
- **原因**：
  1. `render()` 函数不兼容 `<script setup>`
  2. 重构为 `<template>` 工作量大，收益低
  3. 已通过 `as string` 类型断言修复 TS error
  4. 标记为技术债，后续单独优化

---

## Phase 5c 完整性检查清单

- [x] ✅ App.vue 迁移到 `<script setup>`
- [x] ✅ main.ts 审查（无需改动）
- [x] ✅ Pages 目录 unused imports 清理（5 files）
- [x] ✅ Components 目录 unused imports 清理（3 files）
- [x] ✅ Stores 评估（保持现状）
- [x] ✅ LoadingSkeleton 决策（保持现状）
- [x] ✅ 全局 TS 验证（0 errors）
- [x] ✅ Build 验证（成功，442.80 KB）

---

## 最终统计

### 提交记录

| Commit | 文件数 | 净行数 | 说明 |
|---|---|---|---|
| 1 | 1 | -30 | App.vue 迁移 |
| 2 | 6 | -19 | Pages + App.vue 清理 |
| 3 | 3 | -4 | Components 清理 |
| **总计** | **10** | **-53** | **3 commits** |

### 全局指标

| 指标 | Phase 5b 后 | Phase 5c 后 | 变化 |
|---|---|---|---|
| `defineComponent` in src/ | 3 (App.vue + 2 LoadingSkeleton) | 2 (仅 LoadingSkeleton) | ✅ -1 |
| `setup()` functions | 1 (App.vue) | 0 | ✅ -1 |
| TS errors | 0 | 0 | ✅ 保持 |
| Bundle size | ~448 KB | 442.80 KB | ✅ -1.2% |
| Unused imports | 多个 | 0 | ✅ 全部清理 |

---

## 不做事项

- ❌ **不迁移 Stores** — Pinia Options API 是标准写法，保持现状
- ❌ **不重构 LoadingSkeleton** — render() 函数不兼容 script setup，后续单独评估
- ❌ **不修改 ESLint 规则** — `vue/multi-word-component-names` 等警告不影响功能

---

## 后续建议

### 可选优化（非必需）

1. **ESLint 规则调整**：
   - 可在 `eslint.config.mjs` 中禁用 `vue/multi-word-component-names` 规则
   - 或重命名单字组件（如 `Breadcrumbs` → `BreadcrumbNav`）

2. **Prettier 格式化**：
   - 运行 `npm run lint -- --fix` 自动修复格式问题

3. **LoadingSkeleton 重构**：
   - 将 `render()` 函数改写为 `<template>` + `<script setup>`
   - 需评估渲染逻辑复杂度和收益

---

## 文档导航

| 文件 | 内容 |
|---|---|
| [README.md](./README.md) | Phase 5c 概述与范围 |
| [01-summary.md](./01-summary.md) | 详细改动清单与代码示例 |

---

## 出口验收

Phase 5c 完成标志：

```
✅ App.vue 迁移完成
✅ Unused imports 全部清理
✅ TypeScript 0 errors
✅ Build 成功（442.80 KB）
✅ 所有验证通过
```

---

**Phase 5 全系列完成** 🎉

- Phase 5a：10 pages 迁移 → `<script setup>`
- Phase 5b：42 components 迁移 → `<script setup>`
- Phase 5c：清理 + 验证 + App.vue 迁移

**下一步**：→ Phase 6（如有）或部署验证
