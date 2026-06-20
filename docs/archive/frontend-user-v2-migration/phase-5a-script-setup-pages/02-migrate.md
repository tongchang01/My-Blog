# 02 · 迁移（7 个 commit）

> 一次一批，每批改完跑一次 `tsc --noEmit` 确认 = 2 errors，再 commit。
>
> **铁律**：只换包装，逻辑一行不改。

---

## 通用转写公式（每个文件都套这个模板）

```diff
-<script lang="ts">
-import { defineComponent, ... } from 'vue'
+<script setup lang="ts">
+import { ... } from 'vue'
 // 其他 import 保留不动
-
-export default defineComponent({
-  name: 'XxxName',
-  components: { Foo, Bar },
-  setup() {
-    // ... body ...
-    return { a, b, c }
-  }
-})
+// body 提到模块顶层
+// computed/ref 用 const 声明
+// return 删除
 </script>
```

要点：
1. `defineComponent` 从 import 删
2. `name: 'XX'` 删（vite-plugin-pages 用文件名当路由 name）
3. `components: { ... }` 删（script setup 中 `import Foo` 自动注册）
4. `setup() { ... }` 包装删，body 提到顶层
5. `return { x: computed(...) }` → 顶层 `const x = computed(...)`

---

## Commit 1 · category / about / tags（3 个小文件）

**目标**：`src/pages/category.vue` + `src/pages/about.vue` + `src/pages/tags.vue`

### category.vue（最小例子，38 行 → 30 行）

```diff
-<script lang="ts">
-import { defineComponent } from 'vue'
+<script setup lang="ts">
 import { Sidebar } from '@/components/Sidebar'
 import Breadcrumbs from '@/components/Breadcrumbs.vue'
 import usePageTitle from '@/hooks/usePageTitle'

-export default defineComponent({
-  name: 'ArCategory',
-  components: { Sidebar, Breadcrumbs },
-  setup() {
-    const { pageTitle } = usePageTitle()
-    return { pageTitle }
-  }
-})
+const { pageTitle } = usePageTitle()
 </script>
```

### about.vue（标准 onMounted/onUnmounted 模式）

```diff
-<script lang="ts">
-import { defineComponent, onMounted, onUnmounted, ref } from 'vue'
+<script setup lang="ts">
+import { onMounted, onUnmounted, ref } from 'vue'
 // ... 其他 import 不动 ...

-export default defineComponent({
-  name: 'ARAbout',
-  components: { PageContent, Breadcrumbs },
-  setup() {
-    const commonStore = useCommonStore()
-    const articleStore = useArticleStore()
-    const pageData = ref(new Page())
-    const { t } = useI18n()
-    const { pageTitle, updateTitle } = usePageTitle()
-
-    const fetchArticle = async () => {
-      pageData.value = await articleStore.fetchArticle('about')
-      commonStore.setHeaderImage(defaultCover)
-      updateTitle()
-    }
-
-    onMounted(fetchArticle)
-    onUnmounted(() => { commonStore.resetHeaderImage() })
-
-    return { pageTitle, pageData, t }
-  }
-})
+const commonStore = useCommonStore()
+const articleStore = useArticleStore()
+const pageData = ref(new Page())
+const { t } = useI18n()
+const { pageTitle, updateTitle } = usePageTitle()
+
+const fetchArticle = async () => {
+  pageData.value = await articleStore.fetchArticle('about')
+  commonStore.setHeaderImage(defaultCover)
+  updateTitle()
+}
+
+onMounted(fetchArticle)
+onUnmounted(() => { commonStore.resetHeaderImage() })
 </script>
```

### tags.vue（带 computed，要单独提出来）

```diff
-    return {
-      pageTitle,
-      tags: computed(() => {
-        if (tagStore.isLoaded && tagStore.tags.length === 0) return null
-        return tagStore.tags
-      }),
-      t
-    }
+const tags = computed(() => {
+  if (tagStore.isLoaded && tagStore.tags.length === 0) return null
+  return tagStore.tags
+})
```

**commit**：

```bash
git add src/pages/category.vue src/pages/about.vue src/pages/tags.vue
git commit -m "chore(phase-5a): script setup migration - category/about/tags"
```

**沙盒实测**：3 files, +35 -65（净 -30 行）

---

## Commit 2 · page/[slug] + links（2 个中等文件）

**目标**：`src/pages/page/[slug].vue` + `src/pages/links.vue`

### page/[slug].vue ⚠️ 命名冲突要规避

原代码：local `pageTitle: Ref` + return 里 `pageTitle: computed(() => pageTitle.value)`——Options API 闭包内不冲突，提到顶层会冲突。

**做法**：local ref 改名 `pageTitleRef`，对外暴露的 computed 仍叫 `pageTitle`（模板不变）。

```diff
-    const pageTitle = ref()
+const pageTitleRef = ref()
@@
-    const updateTitle = (locale?: Locales) => {
-      // ...
-      pageTitle.value = (routeInfo.i18n && ...) || routeInfo.name
-      metaStore.setTitle(pageTitle.value)
-    }
+const updateTitle = (locale?: Locales) => {
+  // ...
+  pageTitleRef.value = (routeInfo.i18n && ...) || routeInfo.name
+  metaStore.setTitle(pageTitleRef.value)
+}
@@
-    return {
-      enabledComment: computed(...),
-      pageTitle: computed(() => pageTitle.value),
-      pageData,
-      t
-    }
+const enabledComment = computed(...)
+const pageTitle = computed(() => pageTitleRef.value)
```

详见 [04-troubleshooting.md case A](./04-troubleshooting.md)。

### links.vue（含 template ref + 自定义 interface）

`postStatsRef` 保持不动；`PostStatsExpose` interface 留在模块顶层（不要塞 setup 函数内）。

**commit**：

```bash
git add src/pages/page/[slug].vue src/pages/links.vue
git commit -m "chore(phase-5a): script setup migration - page-slug/links"
```

**沙盒实测**：2 files, +64 -103（净 -39 行）

---

## Commit 3 · post/search/index（1 个文件，函数顺序）

**目标**：`src/pages/post/search/index.vue`

⚠️ **函数顺序坑**：原 setup 内 `initPage()` 调用 `fetchPostByTag()` / `fetchPostByCategory()`，但后两者在 `initPage` **之后**声明。Options API 闭包没事（hoisting + 延迟调用），`const` 箭头函数照搬就会触发 `Cannot access 'fetchPostByTag' before initialization`。

**做法**：调换顺序——`fetchPostByTag` / `fetchPostByCategory` 先声明，`initPage` 后声明，`pageChangeHandler` 最后。

详见 [04-troubleshooting.md case B](./04-troubleshooting.md)。

**commit**：

```bash
git add src/pages/post/search/index.vue
git commit -m "chore(phase-5a): script setup migration - post search"
```

**沙盒实测**：1 file, +88 -113（净 -25 行）

---

## Commit 4 · index.vue（首页，1 个大文件）

**目标**：`src/pages/index.vue`

要点：
- 顶部 `useMetaStore().setTitle('home')` 这种 setup 期副作用要原样保留位置（在 store/ref 声明之后、computed 之前）
- 5 个 computed 全部提到顶层（`endEleId`, `gradientText`, `gradientBackground`, `themeConfig`, `categories`）

**commit**：

```bash
git add src/pages/index.vue
git commit -m "chore(phase-5a): script setup migration - index (home)"
```

**沙盒实测**：1 file, +126 -161（净 -35 行）

---

## Commit 5 · [...all].vue（404，1 个文件，最快）

**目标**：`src/pages/[...all].vue`

整个 `<script>` 就一行 `name: 'App'`，全删，留个空 setup 块：

```diff
-<script lang="ts">
-import { defineComponent } from 'vue'
-
-export default defineComponent({
-  name: 'App'
-})
-</script>
+<script setup lang="ts"></script>
```

**commit**：

```bash
git add src/pages/[...all].vue
git commit -m "chore(phase-5a): script setup migration - 404 catch-all"
```

**沙盒实测**：1 file, +1 -7（净 -6 行）

---

## Commit 6 · post/[slug].vue（1 个文件，**唯一 Options API 钩子**）

**目标**：`src/pages/post/[slug].vue`

⚠️ 整个 phase 唯一一个用 `mounted` / `beforeUnmount` 这种 **Options API 选项钩子**（而非 `onMounted`/`onBeforeUnmount` Composition API 钩子）的文件。

**钩子转换**（必须加 import）：

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
-    mounted() {
-      this.fetchData()
-    },
-    beforeUnmount() {
-      this.commonStore.resetHeaderImage()
-    }
+onMounted(() => {
+  fetchData()
+})
+
+onBeforeUnmount(() => {
+  commonStore.resetHeaderImage()
+})
```

注意 `this.fetchData()` → `fetchData()`、`this.commonStore` → `commonStore`（闭包作用域内的变量直接用）。

详见 [04-troubleshooting.md case C](./04-troubleshooting.md)。

**commit**：

```bash
git add src/pages/post/[slug].vue
git commit -m "chore(phase-5a): script setup migration - post detail"
```

**沙盒实测**：1 file, +83 -101（净 -18 行）

---

## Commit 7 · archives.vue（1 个文件，标准模式收尾）

**目标**：`src/pages/archives.vue`

标准 `onBeforeMount` / `onUnmounted` 模式，无特殊坑。

**commit**：

```bash
git add src/pages/archives.vue
git commit -m "chore(phase-5a): script setup migration - archives"
```

**沙盒实测**：1 file, +29 -43（净 -14 行）

---

## 7 commit 累计

| # | 文件数 | +/- | 净 |
|---|---|---|---|
| 1 | 3 | +35 / -65 | -30 |
| 2 | 2 | +64 / -103 | -39 |
| 3 | 1 | +88 / -113 | -25 |
| 4 | 1 | +126 / -161 | -35 |
| 5 | 1 | +1 / -7 | -6 |
| 6 | 1 | +83 / -101 | -18 |
| 7 | 1 | +29 / -43 | -14 |
| **合计** | **10** | **+426 / -593** | **-167** |

---

→ [03-verify.md](./03-verify.md)
