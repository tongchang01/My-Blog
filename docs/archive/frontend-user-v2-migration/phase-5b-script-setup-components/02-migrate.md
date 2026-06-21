# 02 · 迁移步骤（12 Commits）

## Commit 1: Button + ToggleSwitch + SvgIcon（5 files）

**文件**：
- `src/components/Button/PrimaryButton.vue`
- `src/components/Button/SecondaryButton.vue`
- `src/components/ToggleSwitch/Toggle.vue`
- `src/components/ToggleSwitch/ThemeToggle.vue`
- `src/components/SvgIcon/index.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| PrimaryButton | 简单 props | `defineProps<{ text?: string }>()` |
| SecondaryButton | 同上 | 同上 |
| Toggle | emits | `defineEmits<{ (e: 'change', value: boolean): void }>()` |
| ThemeToggle | 标准 | toRefs 保留 |
| **SvgIcon** | **导出 enum** | **双 `<script>` 块**（见下） |

**SvgIcon 特殊处理**（含 `export enum SvgTypes`）：

```vue
<script lang="ts">
export enum SvgTypes {
  fill = 'fill',
  stroke = 'stroke'
}
</script>

<script setup lang="ts">
// 组件逻辑
const props = defineProps<{
  iconClass: string
  ...
}>()
// ... rest
</script>
```

**Commit message**：
```
chore(phase-5b): script setup migration - buttons/toggleswitch/svgicon
```

---

## Commit 2: Tag + Title（4 files）

**文件**：
- `src/components/Tag/TagList.vue`
- `src/components/Tag/TagItem.vue`
- `src/components/Title/src/MainTitle.vue`
- `src/components/Title/src/SubTitle.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| TagList | 简单迭代 | props 可选 |
| TagItem | emits 导航 | `defineEmits<{ (e: 'navigate', slug: string): void }>()` |
| MainTitle | 多 props | `defineProps<{ title: string, icon?: string, ... }>()` |
| SubTitle | 计数展示 | computed 在模块顶层 |

**无特殊处理**，标准转换。

**Commit message**：
```
chore(phase-5b): script setup migration - tag/title
```

---

## Commit 3: Dropdown + Feature（5 files）

**文件**：
- `src/components/Dropdown/src/Dropdown.vue`
- `src/components/Dropdown/src/DropdownMenu.vue`
- `src/components/Dropdown/src/DropdownItem.vue`
- `src/components/Feature/src/Feature.vue`
- `src/components/Feature/src/FeatureList.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| Dropdown | `provide()` | 移到模块顶层；emit 触发 |
| DropdownMenu | `inject()` key | `inject('_dropdownMenu', { ... })` |
| DropdownItem | inject 依赖 | 同上 |
| Feature | 标准 | props + computed |
| FeatureList | 迭代 | 简单模板 |

**provide/inject 链保留**：
```typescript
// Dropdown.vue
const dropdownMenu = ref<any>(null)
provide('_dropdownMenu', { ...dropdownMenu })

// DropdownItem.vue
const menu = inject('_dropdownMenu', {})
```

**Commit message**：
```
chore(phase-5b): script setup migration - dropdown/feature
```

---

## Commit 4: Breadcrumbs + ProgressBar + Social（3 files）

**文件**：
- `src/components/Breadcrumbs.vue`
- `src/components/ProgressBar.vue`
- `src/components/Social.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| Breadcrumbs | 数据驱动 | props array；computed 路由构造 |
| **ProgressBar** | **CDN lodash** | **保留 `declare const _: any`** |
| Social | 迭代渲染 | 条件展示；custom socials 处理 |

**ProgressBar 特殊**：需要全局 CDN 注入的 lodash，保留 declare

```typescript
declare const _: any  // ← 保留，不改
```

**Commit message**：
```
chore(phase-5b): script setup migration - breadcrumbs/progressbar/social
```

---

## Commit 5: ArticleCard + Paginator + PageContent（4 files）

**文件**：
- `src/components/ArticleCard/src/ArticleCard.vue`
- `src/components/ArticleCard/src/HorizontalArticle.vue`
- `src/components/Paginator.vue`
- `src/components/PageContent.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| ArticleCard | props 密集 | `defineProps` 中文变量名保留 |
| HorizontalArticle | `enum TagLimit` | 在 `<script setup>` 内声明（不导出） |
| Paginator | emits 复杂 | `defineEmits<{ (e: 'pageChange', page: number\|string): void }>()` |
| PageContent | 模板 ref | `ref<PostStatsExpose>()` + `defineExpose` 暴露方法 |

**PageContent 特殊**（模板 ref + 暴露方法）：

```typescript
interface PostStatsExpose extends Ref<InstanceType<typeof PostStats>> {
  getCommentCount(): void
  getPostView(): void
}

const postStatsRef = ref<PostStatsExpose>()

// 不需要 defineExpose，因为 ref 已在作用域内
```

**Commit message**：
```
chore(phase-5b): script setup migration - articlecard/paginator/pagecontent
```

---

## Commit 6: Link（5 files）

**文件**：
- `src/components/Link/LinkAvatar.vue`
- `src/components/Link/LinkBox.vue`
- `src/components/Link/LinkCard.vue`
- `src/components/Link/LinkCategoryList.vue`
- `src/components/Link/LinkList.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| LinkAvatar | 简单 avatar | props 可选；computed 梯度背景 |
| LinkBox | 按钮事件 | emits；setTimeout 逻辑保留 |
| LinkCard | 条件渲染 | props boolean；computed 分条件样式 |
| LinkCategoryList | 字典迭代 | `Object.keys()` 转方法 |
| LinkList | 数组迭代 | 标准 |

**无特殊处理**，全部标准转换。

**Commit message**：
```
chore(phase-5b): script setup migration - link
```

---

## Commit 7: Footer（2 files）

**文件**：
- `src/components/Footer/FooterContainer.vue`
- `src/components/Footer/FooterLink.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| FooterContainer | watch configReady | `watch(() => appStore.configReady, (newVal, oldVal) => ...)` |
| FooterLink | 复杂 ref 状态 | `ref<Link[]>([])` 初始化；setTimeout 逻辑保留 |

**无特殊处理**，标准转换。

**Commit message**：
```
chore(phase-5b): script setup migration - footer
```

---

## Commit 8: Header（5 files）

**文件**：
- `src/components/Header/src/Controls.vue`
- `src/components/Header/src/Header.vue`
- `src/components/Header/src/Logo.vue`
- `src/components/Header/src/Navigation.vue`
- `src/components/Header/src/Notification.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| Controls | props + emits | props 简单；emits 多个；toRefs 保留 |
| Header | 子组件通信 | Sticky 组件传 props；ref 状态 |
| Logo | router 导航 | `useRouter()` composable |
| Navigation | 外链判断 | `isExternal()` 工具函数保留 |
| Notification | watch 动画 | setTimeout 清理；watch progress |

**无特殊处理**，标准转换。

**Commit message**：
```
chore(phase-5b): script setup migration - header
```

---

## Commit 9: Sidebar（7 files）

**文件**：
- `src/components/Sidebar/src/CategoryBox.vue`
- `src/components/Sidebar/src/Navigator.vue`
- `src/components/Sidebar/src/Profile.vue`
- `src/components/Sidebar/src/RecentComment.vue`
- `src/components/Sidebar/src/Sidebar.vue`
- `src/components/Sidebar/src/TagBox.vue`
- `src/components/Sidebar/src/Toc.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| CategoryBox | onMounted fetch | `onMounted(fetchData)` |
| Navigator | useRouter + hook | `useJumpToEle()` composable |
| Profile | fetch + watch | `watch(() => props.author, ...)` 变化追踪 |
| RecentComment | onMounted 中 this | 改为 `if (isConfigReady.value) fetchRecentComment()` |
| Sidebar | 简单 computed | 最小组件 |
| TagBox | expand state | `ref<boolean>(false)` |
| Toc | 大文件 + 动态高度 | `onMounted/onUnmounted` 窗口事件；保留复杂 resize 逻辑 |

**无特殊处理**，标准转换。

**Commit message**：
```
chore(phase-5b): script setup migration - sidebar
```

---

## Commit 10: PostStats + Sticky（2 files）

**文件**：
- `src/components/Post/PostStats.vue`
- `src/components/Sticky.vue`

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| **PostStats** | **`defineExpose()`** | 父组件调用 `getCommentCount()`、`getPostView()` |
| **Sticky** | **Options API 转 Composition** | **`mounted()` → `onMounted()`**；**`methods` → 函数**；**`this.$el` → `ref` |

**PostStats 暴露方法**：

```typescript
const getCommentCount = async () => {
  commentCount.value = await initCommentPluginCommentCount(props.currentPath)
}

const getPostView = () => {
  intiCommentPluginPageView(props.currentPath)
}

defineExpose({
  getCommentCount,
  getPostView
})
```

**Sticky 转换**（最复杂的单文件）：

```typescript
// Options API methods → 函数
const sticky = (topValue: number, positionValue: any) => {
  // ...this.top = topValue → top.value = topValue
}

const reset = () => { ... }

const handleScroll = () => {
  _.throttle(updateScroll, 100, { trailing: true, leading: true })()
}

// 钩子转换
onMounted(() => {
  height.value = rootEl.value!.getBoundingClientRect().height
  updateScroll()
  document.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', handleResize)
})

onActivated(() => { updateScroll() })

onUnmounted(() => {
  document.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleResize)
})
```

**Commit message**：
```
chore(phase-5b): script setup migration - poststats/sticky
```

---

## Commit 11: MobileMenu（1 file）

**文件**：
- `src/components/MobileMenu.vue`

**关键点**：

| 特性 | 处理 |
|---|---|
| 275 行单文件 | 标准转换；watch openMenu 保留 |
| 路由导航逻辑 | regex match 保留 |
| DOM 事件监听 | `watch(() => navigatorStore.openMenu, ...)` 中 `addEventListener/removeEventListener` |
| 样式动画 | animation string 保留 |

**无特殊处理**，标准转换。

**Commit message**：
```
chore(phase-5b): script setup migration - mobilemenu
```

---

## Commit 12: Comment + SearchModal（2 files）

**文件**：
- `src/components/Comment.vue`（665 行）
- `src/components/SearchModal.vue`（550 行）

**关键点**：

| 文件 | 特性 | 处理 |
|---|---|---|
| **Comment** | watch locale；init 4 种插件 | `watch()` 保留；`enabledComment()` 函数保留逻辑 |
| **SearchModal** | debounce 搜索；keyboard 快捷键 | `_.debounce()` 保留；`onBeforeMount/onMounted/onUpdated/onUnmounted` 转换 |

**Comment 转换**：

```typescript
let waline: any = undefined  // 顶层状态

const enabledComment = (
  postTitleVal: string,
  postBodyVal: string,
  postUidVal: string
) => {
  // 逻辑完全保留
}

watch(() => appStore.configReady, (newValue, oldValue) => {
  if (!oldValue && newValue) {
    const cachePost = postStore.cachePost
    enabledComment(cachePost.title, cachePost.body, cachePost.uid)
  }
})

watch(() => appStore.locale, (newLocale, oldLocale) => {
  if (waline && newLocale !== undefined && newLocale !== oldLocale) {
    waline.update({ lang: newLocale })
  }
})

onMounted(() => {
  enabledComment(postTitle.value, postBody.value, postUid.value)
})
```

**SearchModal 转换**：

```typescript
const searchKeyword = _.debounce((e: any) => {
  // debounce 包装保留
}, 500)

onBeforeMount(initSearch)
onMounted(() => setTimeout(() => { searchInput.value?.focus() }, 200))
onUpdated(() => { ... })
onUnmounted(() => { document.body.classList.remove('modal--active') })

watch(() => searchStore.openModal, (status) => {
  // 动画延迟逻辑保留
})
```

**Commit message**：
```
chore(phase-5b): script setup migration - comment/searchmodal
```

---

## Bonus Commit: TS Error Fix

**文件**：
- `src/components/LoadingSkeleton/index.ts`

**修复**：

```typescript
// Before
app.component(ObSkeleton.name, ObSkeleton)

// After
app.component(ObSkeleton.name as string, ObSkeleton)
app.component(ObSkeletonTheme.name as string, ObSkeletonTheme)
```

**Commit message**：
```
fix: resolve ts error in LoadingSkeleton registration
```

---

## 迁移完整性检查清单

- [ ] Commit 1: Button/ToggleSwitch/SvgIcon (SvgIcon 双 script 块确认)
- [ ] Commit 2: Tag/Title (无特殊处理)
- [ ] Commit 3: Dropdown/Feature (provide/inject 链确认)
- [ ] Commit 4: Breadcrumbs/ProgressBar/Social (lodash declare 保留)
- [ ] Commit 5: ArticleCard/Paginator/PageContent (enum/interface 位置确认)
- [ ] Commit 6: Link (5 files 标准转换)
- [ ] Commit 7: Footer (2 files 标准转换)
- [ ] Commit 8: Header (5 files 标准转换)
- [ ] Commit 9: Sidebar (7 files，最大单 commit)
- [ ] Commit 10: PostStats/Sticky (Sticky 的 Options → Composition；defineExpose)
- [ ] Commit 11: MobileMenu (275 行单文件)
- [ ] Commit 12: Comment/SearchModal (665 + 550 行大文件)
- [ ] Bonus: TS Error Fix (as string 类型断言)

---

**下一步**：→ [03-verify.md](./03-verify.md) 4 项出口验收
