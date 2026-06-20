# Phase 5b · `<script setup>` 迁移（components）

> **目标**：把 `src/components/**` 下 42 个 `.vue` 文件从 `defineComponent({ setup() })` 改写到 `<script setup>`，逻辑 0 改动。
>
> **耗时**：2-3 小时
>
> **沙盒验证**：✅ 通过（12 个 commit + 1 TS fix，42 files，净 -442 行，TS errors 0）

---

## 为什么 Phase 5b 比 5a 复杂

| 维度 | Phase 5a (pages) | Phase 5b (components) |
|---|---|---|
| 文件数 | 10 | 42 |
| props/emits | ❌ 都没有 | ✅ 大量使用 |
| 互相引用 | ❌ 基本独立 | ✅ 高度耦合 |
| 类型签名 | N/A | ✅ `defineProps<{}>()` / `defineEmits<{}>()` 需掌握 |
| 架构异常 | ❌ 无 | ✅ LoadingSkeleton 的 `render()` 函数（跳过）|
| 验证方式 | 10 页打开逐个看 | TypeScript + 模板渲染（隐式）|

---

## 范围矩阵与分组策略

**分组方式**：按功能相关性，避免单文件 commit（文件多、逻辑独立时打包 2-7 个）

| Commit | 分组 | Files | 特殊性 | 净行数 |
|---|---|---|---|---|
| 1 | Button 族 + Icon | 5 | SvgIcon 有 `export enum`（需双 `<script>` 块） | -48 |
| 2 | Tag + Title | 4 | 标准 pattern | -30 |
| 3 | Dropdown + Feature | 5 | inject/provide 链 | -39 |
| 4 | 核心组件族 | 3 | Breadcrumbs/ProgressBar/Social | -20 |
| 5 | 卡片组件 | 4 | ArticleCard/Paginator/PageContent | -43 |
| 6 | Link 族 | 5 | 参数密集（Link items 拆分） | -41 |
| 7 | Footer | 2 | 计算属性繁复 | -27 |
| 8 | Header | 5 | 多层嵌套组件 | -62 |
| 9 | Sidebar | 7 | 最大单一 commit；CategoryBox/Profile/Toc 等 | -71 |
| 10 | PostStats + Sticky | 2 | Sticky 是大文件（396 行 → 327 行） | -8 |
| 11 | MobileMenu | 1 | 大单文件（275 行） | -15 |
| 12 | Comment + SearchModal | 2 | 最大两个文件（665 + 550 行） | -38 |

---

## 转换规则速查（5b 专项补充）

| 模式 | 转换方法 |
|---|---|
| `props: { foo: String, bar: { type: Array, required: true } }` | `defineProps<{ foo?: string, bar: any[] }>()` |
| `emits: ['update', 'close']` | `defineEmits<{ (e: 'update', payload): void; (e: 'close'): void }>()` |
| `emits: { close: null }` (validation only) | `defineEmits<{ (e: 'close'): void }>()` |
| 模板 ref：`ref="myRef"` + `this.$refs.myRef` | `const myRef = ref<HTMLElement\|null>(null)` → 模板用 `:ref="myRef"` |
| 暴露方法给父组件 | `defineExpose({ method1, method2 })` |
| module-level `enum`（需导出） | 双 `<script>` 块：第一块含 `export enum`，第二块是 `<script setup>` |
| module-level `interface`（仅组件内用） | 直接在 `<script setup>` 内声明（不导出） |
| `this.$emit('event', payload)` | `emit('event', payload)` |
| `this.$i18n` / `this.$router` | 用 `useI18n()` / `useRouter()` composable |

**核心铁律**（继承自 5a）：
1. **逻辑一行不改** — 只换包装
2. **保留所有 import**
3. **非导出的 `enum`/`interface` 可在 `<script setup>` 内声明**（5a 无此场景）

---

## 架构异常与决策

### LoadingSkeleton 例外处理

**Issue**：两个文件使用 imperative `render()` 函数（不兼容 `<script setup>`）

| 文件 | 行数 | 理由 | 决策 |
|---|---|---|---|
| `src/components/LoadingSkeleton/Skeleton.vue` | 94 | 动态生成多个 VNode 元素 | ⏭️ 保留 `defineComponent` |
| `src/components/LoadingSkeleton/SkeletonTheme.vue` | ~60 | 高阶 render 逻辑 | ⏭️ 保留 `defineComponent` |

**TS Error Fix**：`LoadingSkeleton/index.ts` 的 component registration 加 `as string` 类型断言

```typescript
export const registerObSkeleton = (app: App): void => {
  app.component(ObSkeleton.name as string, ObSkeleton)    // ← 加 as string
  app.component(ObSkeletonTheme.name as string, ObSkeletonTheme)
}
```

---

## 文档导航

| 文件 | 内容 |
|---|---|
| [01-pre-flight.md](./01-pre-flight.md) | 入口条件 + 基线 tag |
| [02-migrate.md](./02-migrate.md) | 12 个 commit 批次的具体改法 |
| [03-verify.md](./03-verify.md) | 4 项出口验收 |
| [04-troubleshooting.md](./04-troubleshooting.md) | 常见坑（TypeScript 推导、props 类型、inject/provide） |

---

## 出口验收速览

| 项 | 期望 |
|---|---|
| `grep -rn "defineComponent\|setup()" src/components/ \| grep -v LoadingSkeleton` | 仅剩 LoadingSkeleton（2 files） |
| `tsc --noEmit` | **0 errors**（LoadingSkeleton TS error 已修复）|
| `vite build` | ✓ 通过；bundle size 继续缩减 |
| 模板渲染 | 所有引用 components 的页面打开正常 |

---

## 不做事项

- ❌ **不动 LoadingSkeleton**（render() 不兼容 script setup；单独评估）
- ❌ **不动 App.vue / main.ts**——5c 才做
- ❌ **不清 unused imports**——5c 集中清
- ❌ **不改导出类型**（除了 `enum`/`interface` 声明位置）
- ❌ **不重构逻辑**

---

## 快速对照：5a vs 5b 的差异表

| 特性 | 5a (pages) | 5b (components) |
|---|---|---|
| `defineProps()` 使用 | ❌ | ✅ 常见 |
| `defineEmits()` 使用 | ❌ | ✅ 常见 |
| `defineExpose()` 使用 | ❌ | ✅ 偶见（PostStats） |
| 双 `<script>` 块 | ❌ | ✅ SvgIcon（enum 导出） |
| `inject()` / `provide()` | ❌ | ✅ Dropdown + DropdownMenu |
| 跳过的文件 | 0 | 2 (LoadingSkeleton) |
