# Phase 5a · `<script setup>` 迁移（pages）

> **目标**：把 `src/pages/**` 下 10 个 `.vue` 文件从 `defineComponent({ setup() })` 改写到 `<script setup>`，逻辑 0 改动。
>
> **耗时**：1-2 小时
>
> **沙盒验证**：✅ 通过（7 个 commit，净 -167 行，主 bundle 452.92 → 450.92 KB）

---

## 为什么先做 pages 不做 components

| 顺序 | 理由 |
|---|---|
| pages（5a）先做 | 文件少（10 个），路由文件，肉眼回归直观——每页打开看一眼就行 |
| components（5b）后做 | 数量多（~40），互相被引用，先有 pages 验证"转写模式"安全后再批量改 |
| App shell（5c）最后 | App.vue / Header / Footer / main.ts 最敏感，留最后压轴 |

---

## 范围矩阵

| 文件 | 行数 | 特殊性 |
|---|---|---|
| `src/pages/category.vue` | 38 | 最小，单一 `usePageTitle` 调用 |
| `src/pages/about.vue` | 50 | 标准 setup pattern |
| `src/pages/tags.vue` | 76 | 含 `computed` |
| `src/pages/page/[slug].vue` | 88 | 含 `watch` + 内部 ref 用计算属性包装的小套路 |
| `src/pages/links.vue` | 175 | 含模板 ref（postStatsRef）+ TypeScript 接口 |
| `src/pages/post/search/index.vue` | 192 | 多个函数互相调用，注意定义顺序 |
| `src/pages/index.vue` | 280 | 首页，最多 computed 暴露 |
| `src/pages/[...all].vue` | 289 | 几乎全是 SVG，script 可以直接清空 |
| `src/pages/post/[slug].vue` | 308 | **唯一含 Options API 钩子**（`mounted` / `beforeUnmount`）需等价转换 |
| `src/pages/archives.vue` | 375 | 大部分是 CSS，setup 简单 |

---

## 转换规则速查

| Options API | `<script setup>` 等价 |
|---|---|
| `defineComponent({ ... })` 整个包装 | 直接删 |
| `components: { Foo, Bar }` 注册 | 删 — script setup 中 `import Foo` 自动注册到模板 |
| `name: 'XXX'` | 删 — vite-plugin-pages 用文件名当路由 name |
| `setup() { ... return {...} }` | 把 body 提到模块顶层，删除 return |
| `return { foo: computed(...) }` | `const foo = computed(...)` |
| `mounted() { this.fetchData() }` | `onMounted(() => { fetchData() })` + 加 import |
| `beforeUnmount() { this.x.y() }` | `onBeforeUnmount(() => { x.y() })` + 加 import |
| `this.xxx`（Options API 内） | 直接用 `xxx`（要变量在闭包作用域内） |

**核心铁律**：
1. **逻辑一行不改**——只换包装，不重写、不优化、不"顺手清理"
2. **保留所有 import**，即使转写后某个变量没在模板用（unused-var 警告等 5b/5c 集中处理）
3. **`const` 声明的箭头函数有顺序要求**：函数 A 调用函数 B 时，B 必须先声明（或两者都不立即调用——延迟到 hook 触发时才调用是 OK 的）

---

## 文档导航

| 文件 | 内容 |
|---|---|
| [01-pre-flight.md](./01-pre-flight.md) | 入口条件 + 基线 tag |
| [02-migrate.md](./02-migrate.md) | 7 个 commit 批次的具体改法 |
| [03-verify.md](./03-verify.md) | 4 项出口验收 |
| [04-troubleshooting.md](./04-troubleshooting.md) | 两个值得注意的小坑（Options API 钩子转换 + const 顺序） |

---

## 出口验收速览

| 项 | 期望 |
|---|---|
| `grep -rn defineComponent src/pages/` | 0 输出 |
| `tsc --noEmit` | 仍只剩 2 个 pre-existing LoadingSkeleton errors（与 Phase 4 末持平） |
| `vite build` | ✓ 通过；主 bundle 与 Phase 4 末（沙盒 452.92 KB）相比小幅缩减 |
| Dev server 冒烟 | 10 个页面各自打开正常渲染 |

---

## 不做事项

- ❌ **不动 `src/components/**`**——5b 才做
- ❌ **不动 App.vue / Header / Footer / main.ts**——5c 才做
- ❌ **不清 unused imports / unused vars**——会留下临时 lint warning，集中在 5b/5c 清
- ❌ **不重构逻辑**——纯包装替换，不要"顺手把 if 写成 ternary"等小动作
- ❌ **不改 props/emits 类型签名**——本 phase 10 个 pages 都没有 props/emits，5b 才会遇到 `defineProps` / `defineEmits`
