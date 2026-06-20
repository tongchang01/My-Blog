# 04 · Troubleshooting

> 沙盒实际踩到的 3 个坑——都不算大，但第一次见会愣两分钟。

---

## A. `page/[slug].vue` 命名冲突

**症状**（按照公式硬搬时）：

```
[vue/compiler-sfc] Identifier 'pageTitle' has already been declared
```

或者 TS 报：

```
Cannot redeclare block-scoped variable 'pageTitle'.
```

**根因**：原 Options API 里有这两个东西**同名**且都叫 `pageTitle`，能共存只是因为分别在 setup 闭包内（local ref）和外部 return（computed）：

```ts
setup() {
  const pageTitle = ref()                              // (1) 内部 ref
  // ...
  return {
    pageTitle: computed(() => pageTitle.value),        // (2) 对外暴露的 computed
  }
}
```

提到 `<script setup>` 顶层后，两个 `const pageTitle` 在同一作用域——TS / 编译器立刻拒绝。

**修复**：内部 ref 改名为 `pageTitleRef`，对外暴露的 computed 仍叫 `pageTitle`（这样**模板里 `{{ pageTitle }}` 不用改**）：

```ts
const pageTitleRef = ref()                             // 改名

const updateTitle = (locale?: Locales) => {
  // ...
  pageTitleRef.value = ...                             // 内部赋值用新名
  metaStore.setTitle(pageTitleRef.value)
}

const pageTitle = computed(() => pageTitleRef.value)   // 对外暴露的名字保持不变
```

**预防**：迁移每个文件前先扫一眼原 `return { ... }`，找有没有 `xxx: computed(() => xxx.value)` 这种"内外同名"的模式。命中就先在心里规划好新名字（一般 `Ref` / `Inner` / `_xxx` 后缀都行，沙盒选了 `Ref` 后缀）。

---

## B. `const` 箭头函数声明顺序

**症状**（沙盒在 `post/search/index.vue` 踩到）：

```
ReferenceError: Cannot access 'fetchPostByTag' before initialization
```

dev server 启动正常，但点开 `/post/search?tag=xxx` 路由就崩。

**根因**：Options API 闭包里的函数声明顺序无所谓——`setup()` 整个执行完后才会有人调用它们，那时所有 `const` 都已初始化：

```ts
setup() {
  const initPage = () => {
    fetchPostByTag()           // ← 这里调用
  }

  const fetchPostByTag = () => { ... }   // ← 在 initPage 之后声明，但 OK
}
```

提到 `<script setup>` 顶层照搬同样的顺序，函数定义本身的执行**不会**报错（箭头函数没立即调用）。但当 `onBeforeMount` 实际触发 → `pageChangeHandler()` → `initPage()` → 这时 JS 引擎才去找 `fetchPostByTag`——TDZ（暂时性死区）规则下，`const` 在声明前访问就抛 `ReferenceError`。

⚠️ 但**不是所有"反向调用"都会炸**：本案能炸是因为 `pageChangeHandler` 在 `onBeforeMount` 内**同步**调用了 `initPage`，整个调用链发生在文件顶层执行刚结束的瞬间。如果只是"用户点按钮才调"那种延迟调用，等到那时全部 const 都已初始化，反而不会炸。

**修复**：调换声明顺序，"被调者"在前，"调用者"在后：

```ts
// 先声明 leaf 函数
const fetchPostByTag = () => { ... }
const fetchPostByCategory = () => { ... }

// 再声明调用它们的
const initPage = () => {
  fetchPostByTag()
  // ...
}

const pageChangeHandler = () => {
  // ...
  initPage()
}

// 最后注册钩子
onBeforeMount(() => {
  pageChangeHandler()
})
```

**预防**：转写一个文件后，把 setup 函数体的代码块**按依赖顺序重排**（被依赖者在前）。一般顺序：

1. store / route / hook 调用
2. ref / reactive 声明
3. 工具函数 / 业务函数（自底向上：被调者在前）
4. watch
5. computed
6. 生命周期钩子（`onMounted` 等）

---

## C. `post/[slug].vue` Options API 钩子

**症状**：转写后 dev server 不报错，但打开 `/post/xxx` 页面空白 + Console 无 fetch 请求。

**根因**：原文件用的是 **Options API 的选项钩子**（混在 `defineComponent({...})` 对象里），不是 Composition API 的 `onMounted`：

```ts
export default defineComponent({
  setup() {
    const fetchData = async () => { ... }
    return { fetchData, ... }
  },
  // ↓ 这俩是 Options API 选项，挂在对象上
  mounted() {
    this.fetchData()
  },
  beforeUnmount() {
    this.commonStore.resetHeaderImage()
  }
})
```

转 `<script setup>` 时如果只把 `setup()` body 提到顶层，**整个 `defineComponent({})` 对象会被删掉**——`mounted` / `beforeUnmount` 跟着一起没了，没人调 `fetchData`，页面当然空白。

**修复**：用 Composition API 等价钩子替换，并加 import：

```diff
 import {
   Ref,
   computed,
   nextTick,
+  onBeforeUnmount,
+  onMounted,
   ref,
   watch
 } from 'vue'
```

```diff
-mounted() {
-  this.fetchData()
-},
-beforeUnmount() {
-  this.commonStore.resetHeaderImage()
-}
+onMounted(() => {
+  fetchData()
+})
+
+onBeforeUnmount(() => {
+  commonStore.resetHeaderImage()
+})
```

注意 `this.xxx` 全部去掉——闭包作用域内的 `fetchData` / `commonStore` 直接用名字访问。

**预防**：每个文件开始转写前先扫一眼**整个 `defineComponent({ ... })` 对象的同级 key**——除了 `name`、`components`、`setup` 之外的所有 key 都是 Options API 选项：

| 常见 Options 选项 | Composition 等价 |
|---|---|
| `mounted() {}` | `onMounted(() => {})` |
| `beforeUnmount() {}` | `onBeforeUnmount(() => {})` |
| `created() {}` | 直接放 setup 顶层（无对应钩子，setup 本身就是 created 时机） |
| `data() { return { x: 1 } }` | `const x = ref(1)` |
| `methods: { foo() {} }` | `const foo = () => {}` |
| `computed: { bar() {} }` | `const bar = computed(() => {})` |
| `watch: { x(v) {} }` | `watch(() => x.value, v => {})` |
| `props: {...}` | `defineProps<...>()`（本 phase 没遇到，5b 再说） |
| `emits: [...]` | `defineEmits<...>()`（本 phase 没遇到，5b 再说） |

沙盒里 10 个文件**只有 `post/[slug].vue` 有 Options API 选项钩子**，其余 9 个都纯粹是 `setup()` + 末尾 return。

---

## D. 没列在这里的报错

逐项跑 [03-verify.md](./03-verify.md) 的 4 项验收，定位失败 commit 后回到 [02-migrate.md](./02-migrate.md) 对应 Commit 段比对。

---

## 想回退

**回到某个 commit 之前**（比如 Commit 4 后发现 index.vue 改坏了）：

```bash
git reset --hard HEAD~1     # 撤销最近一个 commit（保留前 N-1 个）
```

**整个 Phase 5a 都不要了**：

```bash
git tag -d phase-5a-done    # 如果已经打过
git reset --hard pre-script-setup
```

之后 `pre-script-setup` 就是 Phase 4 末的状态，干干净净从头来。
