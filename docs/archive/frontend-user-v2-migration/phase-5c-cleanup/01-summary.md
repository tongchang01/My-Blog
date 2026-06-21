# 01 · Phase 5c 详细改动清单

---

## Commit 1: App.vue 迁移

### 文件：`src/App.vue`

**改动类型**：Options API `defineComponent({ setup() })` → `<script setup>`

#### 移除的代码

```typescript
// ❌ Before
export default defineComponent({
  name: 'App',
  components: {
    HeaderMain,
    FooterContainer,
    MobileMenu,
    VueEasyLightbox,
    FooterLink
  },
  setup() {
    // ... 大量逻辑
    return {
      title: computed(() => metaStore.getTitle),
      theme: computed(() => appStore.theme),
      scripts: computed(() => metaStore.scripts),  // ← Unused!
      themeConfig: computed(() => appStore.themeConfig),
      // ... 更多 return
      handleEscKey: appStore.handleEscKey,  // ← Unused!
      configReady: computed(() => appStore.configReady),  // ← Unused!
      t  // ← Unused!
    }
  }
})
```

#### 新增的代码

```typescript
// ✅ After
<script setup lang="ts">
import { ... } from 'vue'
// ... other imports (removed: useI18n)

const appStore = useAppStore()
// ... all logic stays the same

// 模板中用到的变量直接声明（无需 return）
const title = computed(() => metaStore.getTitle)
const theme = computed(() => appStore.theme)
// scripts, handleEscKey, configReady - removed (unused)
const themeConfig = computed(() => appStore.themeConfig)
// ...
</script>
```

#### 清理的 Unused 变量

| 变量 | 类型 | 原因 |
|---|---|---|
| `t` | `useI18n()` | 模板中未使用国际化 |
| `scripts` | `computed` | `metaStore.scripts` 未在模板中引用 |
| `handleEscKey` | `function` | 从 appStore 获取但模板未调用 |
| `configReady` | `computed` | 仅内部逻辑使用，模板无绑定 |

**净行数**：-30 行（166 → 136）

---

## Commit 2: Pages 目录清理

### 2.1 `src/pages/about.vue`

#### 清理内容

```typescript
// ❌ Before
import { useI18n } from 'vue-i18n'
const { t } = useI18n()  // ← Unused!

// ✅ After
// (removed import and usage)
```

**原因**：模板中无任何 `{{ t('...') }}` 调用。

---

### 2.2 `src/pages/index.vue`

#### 清理内容

```typescript
// ❌ Before
import { MainTitle } from '@/components/Title'  // ← Never used!

const gradientText = computed(
  () => appStore.themeConfig.theme.background_gradient_style
)  // ← Unused!

const gradientBackground = computed(() => {
  return { background: appStore.themeConfig.theme.header_gradient_css }
})  // ← Unused!

// ✅ After
// (all removed)
```

**原因**：
- `MainTitle` 组件导入但模板中未使用
- `gradientText` 和 `gradientBackground` computed 但从未绑定到模板

---

### 2.3 `src/pages/links.vue`

#### 清理内容

```typescript
// ❌ Before
import LinkCard from '@/components/Link/LinkCard.vue'  // ← Unused!
import { useI18n } from 'vue-i18n'
const { t } = useI18n()  // ← Unused!

// ✅ After
// (removed both)
```

**原因**：
- `LinkCard` 导入但模板中用的是 `LinkCategoryList` 和 `LinkList`
- `t` 函数未调用

---

### 2.4 `src/pages/page/[slug].vue`

#### 清理内容

```typescript
// ❌ Before
import { useI18n } from 'vue-i18n'
const { t } = useI18n()

// ✅ After
// (removed)
```

**原因**：模板中未使用 `t()` 国际化函数。

---

### 2.5 `src/pages/post/search/index.vue`

#### 清理内容

```typescript
// ❌ Before
const pageType = ref('search')  // ← Unused!

// 模板中被注释：
<!-- <Breadcrumbs :current="t(pageType)" /> -->

// ✅ After
// (removed pageType)
```

**原因**：`pageType` 定义但模板中被注释掉，实际未使用。

---

## Commit 3: Components 目录清理

### 3.1 `src/components/ArticleCard/src/HorizontalArticle.vue`

#### 清理内容

```typescript
// ❌ Before
const isMobile = computed(() => commonStore.isMobile)  // ← Unused!

const numberOfTags = computed(() => {
  const tagCount = post.value.tags.length
  if (commonStore.isMobile) {  // ← Directly accessing
    return tagCount > TagLimit.forMobile ? TagLimit.forMobile : tagCount
  }
  return tagCount > TagLimit.default ? TagLimit.default : tagCount
})

// ✅ After
// (removed isMobile computed)

const numberOfTags = computed(() => {
  const tagCount = post.value.tags.length
  if (commonStore.isMobile) {  // ← Still works!
    return tagCount > TagLimit.forMobile ? TagLimit.forMobile : tagCount
  }
  return tagCount > TagLimit.default ? TagLimit.default : tagCount
})
```

**原因**：`isMobile` computed 被定义但从未使用。代码中直接访问 `commonStore.isMobile` 即可，无需中间变量。

---

### 3.2 `src/components/PageContent.vue`

#### 清理内容

```typescript
// ❌ Before
import { useI18n } from 'vue-i18n'
const { t } = useI18n()  // ← Unused!

// ✅ After
// (removed import and usage)
```

**原因**：组件模板中无国际化调用。

---

### 3.3 `src/components/Sticky.vue`

#### 清理内容

```typescript
// ❌ Before
const newTop = ref(0)  // ← Defined but never read!

const sticky = (topValue: number, positionValue: any) => {
  if (active.value) {
    return
  }
  top.value = topValue  // ← Uses 'top', not 'newTop'
  // ...
}

// ✅ After
// (removed newTop)

const sticky = (topValue: number, positionValue: any) => {
  if (active.value) {
    return
  }
  top.value = topValue  // ← Still works!
  // ...
}
```

**原因**：`newTop` ref 被声明但从未被赋值或读取，完全冗余。

---

## ESLint 报告分析

### 运行 `npm run lint` 发现的主要问题

#### 1. Unused vars（已修复）

| 文件 | 行 | 变量 | 类型 | 修复 |
|---|---|---|---|---|
| App.vue | 66 | `t` | useI18n | ✅ 移除 |
| App.vue | 182 | `scripts` | computed | ✅ 移除 |
| App.vue | 197 | `handleEscKey` | function | ✅ 移除 |
| App.vue | 198 | `configReady` | computed | ✅ 移除 |
| HorizontalArticle.vue | 176 | `isMobile` | computed | ✅ 移除 |
| PageContent.vue | 117 | `t` | useI18n | ✅ 移除 |
| Sticky.vue | 70 | `newTop` | ref | ✅ 移除 |
| about.vue | 22 | `t` | useI18n | ✅ 移除 |
| index.vue | 102 | `MainTitle` | import | ✅ 移除 |
| index.vue | 230 | `gradientText` | computed | ✅ 移除 |
| index.vue | 233 | `gradientBackground` | computed | ✅ 移除 |
| links.vue | 100 | `LinkCard` | import | ✅ 移除 |
| links.vue | 124 | `t` | useI18n | ✅ 移除 |
| [slug].vue | 37 | `t` | useI18n | ✅ 移除 |
| search/index.vue | 81 | `pageType` | ref | ✅ 移除 |

**总计**：15 个 unused 变量/导入 → 全部清理 ✅

#### 2. 其他 ESLint 问题（未修复，不影响功能）

- `vue/multi-word-component-names`：单字组件名警告（如 `Breadcrumbs`、`Header`）
  - **决策**：保持现状，不影响功能
  
- `prettier/prettier`：格式化问题（换行、缩进等）
  - **决策**：可运行 `npm run lint -- --fix` 自动修复，但非必需
  
- `console.log` 警告：开发调试代码
  - **决策**：保留，生产环境会自动移除

---

## 验证流程

### 1. TypeScript 检查

```bash
cd c:/tyb/verify-phase1/hexo-theme-aurora-main
./node_modules/.bin/tsc --noEmit
```

**结果**：✅ **0 errors**（无输出 = 成功）

---

### 2. Build 检查

```bash
npm run build
```

**输出**：

```
✓ 286 modules transformed.
rendering chunks...
computing gzip size...

dist/static/js/DtrEhboE.js    442.80 kB │ gzip: 153.91 kB
✓ built in 4.36s
```

**结果**：✅ 成功构建

**Bundle 分析**：
- Main JS: 442.80 KB (未压缩)，153.91 KB (gzip)
- 对比 Phase 5b 预期（~448 KB）：**-5.2 KB** (-1.2%)

---

## 对比：Phase 5a → 5b → 5c

| 指标 | Phase 5a 后 | Phase 5b 后 | Phase 5c 后 | 总变化 |
|---|---|---|---|---|
| Pages with `setup()` | 0 | 0 | 0 | ✅ -10 |
| Components with `setup()` | 42 | 0 | 0 | ✅ -42 |
| App.vue with `setup()` | 1 | 1 | 0 | ✅ -1 |
| Unused imports | N/A | 多个 | 0 | ✅ -15 |
| TS errors | 2 | 0 | 0 | ✅ -2 |
| Bundle size | ~450 KB | ~448 KB | 442.80 KB | ✅ -7.2 KB |

---

## 提交信息模板

如果在主仓库重新执行，使用以下 commit messages：

```bash
# Commit 1
git add src/App.vue
git commit -m "chore(phase-5c): 迁移 App.vue 到 script setup 语法"

# Commit 2
git add src/App.vue src/pages/
git commit -m "chore(phase-5c): 清理 App.vue 和 pages 目录的 unused imports"

# Commit 3
git add src/components/
git commit -m "chore(phase-5c): 清理 components 目录的 unused imports"
```

---

## 快速检查清单

在主仓库执行 Phase 5c 前后，运行以下检查：

```bash
# 1. 检查剩余 defineComponent（应该只有 LoadingSkeleton）
grep -rn "defineComponent" src/ | grep -v "LoadingSkeleton"
# 期望输出：（空）

# 2. 检查剩余 setup() 函数
grep -rn "setup()" src/ | grep -v "LoadingSkeleton" | grep -v "script setup"
# 期望输出：（空）

# 3. TypeScript 检查
./node_modules/.bin/tsc --noEmit
# 期望输出：（无错误）

# 4. Build 检查
npm run build
# 期望输出：✓ built in X.XXs

# 5. ESLint unused vars 检查
npm run lint 2>&1 | grep "is assigned a value but never used"
# 期望输出：（空，或仅 LoadingSkeleton/非关键文件）
```

---

**Phase 5c 完成** ✅

所有改动已清理，代码库干净无冗余，全局 TS 0 错误。
