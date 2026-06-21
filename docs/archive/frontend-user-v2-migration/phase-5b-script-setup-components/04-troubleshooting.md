# 04 · 常见坑排查（TypeScript、Props、Inject/Provide）

---

## 坑 1：Props 类型推导错误

### 症状
```
TS2345: Argument of type '...' is not assignable to parameter of type '...'
```

### 原因 & 解决

#### ❌ 错误：可选字段写成了 `required: true`
```typescript
// ❌ 错误
const props = defineProps<{
  title: string  // 这样是必填
  icon: string   // 这样也是必填
}>()

// ✅ 正确
const props = defineProps<{
  title: string
  icon?: string  // 可选用 ?
}>()
```

#### ❌ 错误：数组/对象类型推导
```typescript
// ❌ 错误（any 是最后的手段）
const props = defineProps<{
  items: any[]
}>()

// ✅ 正确（精确类型）
interface Item { id: string; name: string }
const props = defineProps<{
  items: Item[]
}>()
```

#### ❌ 错误：联合类型写法
```typescript
// ❌ 错误
const props = defineProps<{
  value: string | number | boolean
}>()

// ✅ 正确
const props = defineProps<{
  value: string | number | boolean
}>()
```

---

## 坑 2：defineEmits 签名不完整

### 症状
```
TS2345: '(e: "update", payload)' is not assignable to type '(e: "update"): void'
```

### 原因 & 解决

#### ❌ 错误：缺少 `: void` 返回类型
```typescript
// ❌ 错误（TypeScript 推导不出返回类型）
const emit = defineEmits<{
  (e: 'update', payload: string)
  (e: 'close')
}>()

// ✅ 正确
const emit = defineEmits<{
  (e: 'update', payload: string): void
  (e: 'close'): void
}>()
```

#### ❌ 错误：多参数 emit 签名
```typescript
// ❌ 错误（标准 emit 最多一个 payload）
const emit = defineEmits<{
  (e: 'update', p1: string, p2: number): void
}>()

// ✅ 正确（用对象包装多个参数）
interface UpdatePayload { text: string; count: number }
const emit = defineEmits<{
  (e: 'update', payload: UpdatePayload): void
}>()
```

#### ❌ 错误：忘记调用 emit
```typescript
// ❌ 错误
const handleClick = () => {
  // 定义了 emits 但没调用
  state.value++
}

// ✅ 正确
const handleClick = () => {
  state.value++
  emit('update', state.value)
}
```

---

## 坑 3：inject/provide 键不匹配

### 症状
```
TypeError: Cannot read property 'xxx' of undefined
// 或组件显示为空，无法访问注入的数据
```

### 原因 & 解决

#### ❌ 错误：键名拼写不一致
```typescript
// Dropdown.vue（提供者）
provide('_dropdownMenu', { menu: ref(null) })

// DropdownItem.vue（消费者）
const menu = inject('_dropdown', {})  // ❌ 键名不同！

// ✅ 正确
const menu = inject('_dropdownMenu', {})
```

#### ❌ 错误：忘记提供默认值
```typescript
// ❌ 错误（如果父组件未提供，则为 undefined）
const menu = inject('_dropdownMenu')

// ✅ 正确（提供默认值）
const menu = inject('_dropdownMenu', { isOpen: false })
```

#### ❌ 错误：provide 被意外覆盖
```typescript
// ❌ 错误（嵌套组件意外重新 provide 相同键）
// Dropdown.vue
provide('_dropdownMenu', { level: 1 })

// DropdownMenu.vue
provide('_dropdownMenu', { level: 2 })  // 覆盖了！

// DropdownItem.vue
const menu = inject('_dropdownMenu')  // level: 2（错误！）

// ✅ 正确（使用不同的键或独立 scope）
// Dropdown.vue
provide('_dropdown', { level: 1 })

// DropdownMenu.vue
provide('_dropdownMenu', { level: 2 })

// DropdownItem.vue
const dropdownMenu = inject('_dropdownMenu', {})
```

---

## 坑 4：模板 ref 类型不安全

### 症状
```
TS2345: Property 'focus' does not exist on type 'HTMLElement | undefined'
```

### 原因 & 解决

#### ❌ 错误：ref 类型推导不出元素类型
```typescript
// ❌ 错误（TypeScript 无法推导）
const inputRef = ref()

onMounted(() => {
  inputRef.value?.focus()  // TS2345: focus 不存在
})
```

#### ✅ 正确：显式指定 ref 类型
```typescript
// ✅ 正确方案 1：HTMLInputElement
const inputRef = ref<HTMLInputElement | null>(null)

onMounted(() => {
  inputRef.value?.focus()  // ✓ 现在 TypeScript 认识 focus()
})
```

```typescript
// ✅ 正确方案 2：组件 ref（获取子组件实例）
import PostStats from '@/components/Post/PostStats.vue'

const postStatsRef = ref<InstanceType<typeof PostStats> | null>(null)

onMounted(() => {
  postStatsRef.value?.getCommentCount()  // ✓ 调用子组件暴露的方法
})
```

#### ❌ 错误：在模板中绑定 ref 时用了 `ref="xxx"`
```vue
<!-- ❌ 错误（没有冒号，值被当作字符串） -->
<input ref="inputRef" />

<!-- ✅ 正确（有冒号，绑定响应式变量） -->
<input :ref="inputRef" />
```

---

## 坑 5：defineExpose 暴露的方法签名

### 症状
```
TS2345: Property 'getCommentCount' does not exist on type 'Ref<...>'
```

### 原因 & 解决

#### ❌ 错误：暴露的方法调用时参数不匹配
```typescript
// PostStats.vue
const getCommentCount = async () => {
  // ...
}

defineExpose({ getCommentCount })

// 父组件中
const postStatsRef = ref<InstanceType<typeof PostStats> | null>(null)

onMounted(() => {
  postStatsRef.value?.getCommentCount('extra-param')  // ❌ 签名不匹配
})

// ✅ 正确
onMounted(() => {
  postStatsRef.value?.getCommentCount()  // ✓
})
```

#### ❌ 错误：忘记 defineExpose
```typescript
// ❌ 错误（定义了方法但没暴露）
const getCommentCount = async () => { ... }
// 缺少 defineExpose!

// ✅ 正确
const getCommentCount = async () => { ... }
defineExpose({ getCommentCount })
```

---

## 坑 6：双 `<script>` 块与导出 enum

### 症状
```
TS1371: Cannot re-export a type when the '--isolatedModules' flag is set
```

### 原因 & 解决

#### ❌ 错误：enum 在 `<script setup>` 中导出
```vue
<!-- ❌ 错误 -->
<script setup lang="ts">
export enum SvgTypes {
  fill = 'fill',
  stroke = 'stroke'
}

const props = defineProps<{ iconClass: string }>()
</script>
```

#### ✅ 正确：双 `<script>` 块（enum 单独导出）
```vue
<!-- ✅ 正确 -->
<script lang="ts">
export enum SvgTypes {
  fill = 'fill',
  stroke = 'stroke'
}
</script>

<script setup lang="ts">
const props = defineProps<{ iconClass: string }>()
</script>
```

#### ❌ 错误：interface 放在导出块中
```vue
<!-- ❌ 冗余（不需要导出） -->
<script lang="ts">
export interface MyInterface { ... }
</script>

<script setup lang="ts">
// ...
</script>

<!-- ✅ 简洁（interface 仅组件内用） -->
<script setup lang="ts">
interface MyInterface { ... }
const props = defineProps<MyInterface>()
</script>
```

---

## 坑 7：顶层变量 `let` 初始化

### 症状
```
ReferenceError: waline is not defined
// 或: Cannot read property 'update' of undefined
```

### 原因 & 解决

#### ❌ 错误：let 变量未初始化
```typescript
// ❌ 错误（waline 未初始化就被赋值）
let waline

const enabledComment = () => {
  waline = getWalineInstance()  // ✓ 赋值
}

watch(() => appStore.locale, () => {
  waline.update()  // ❌ 此时 waline 可能未初始化
})
```

#### ✅ 正确：显式初始化或类型注解
```typescript
// ✅ 方案 1：显式初始化为 undefined 的类型
let waline: any = undefined

const enabledComment = () => {
  waline = getWalineInstance()
}

watch(() => appStore.locale, () => {
  if (waline) waline.update()  // ✓ 安全检查
})
```

```typescript
// ✅ 方案 2：使用 ref（推荐）
const waline = ref<any>(undefined)

const enabledComment = () => {
  waline.value = getWalineInstance()
}

watch(() => appStore.locale, () => {
  if (waline.value) waline.value.update()
})
```

---

## 坑 8：CDN 全局变量声明

### 症状
```
TS2304: Cannot find name '_'
```

### 原因 & 解决

#### ❌ 错误：没有声明全局变量
```typescript
// ❌ 错误（lodash 是全局的，但 TS 不认识）
const debounced = _.debounce((e: any) => { ... }, 500)
```

#### ✅ 正确：declare 声明
```typescript
// ✅ 正确（告诉 TS：这个变量由外部 CDN 提供）
declare const _: any

const debounced = _.debounce((e: any) => { ... }, 500)
```

#### ❌ 错误：declare 位置在 script setup 内
```vue
<!-- ❌ 错误 -->
<script setup lang="ts">
declare const _: any  // ❌ declare 应该在顶层脚本块

const debounced = _.debounce(...)
</script>

<!-- ✅ 正确 -->
<script lang="ts">
declare const _: any  // ✓ 顶层脚本块或注释
</script>

<script setup lang="ts">
const debounced = _.debounce(...)
</script>
```

---

## 坑 9：watch 依赖更新不触发

### 症状
```
console: [HMR] connected  // 热更新正常
// 但 watch 回调不执行
```

### 原因 & 解决

#### ❌ 错误：watch 源不是响应式
```typescript
// ❌ 错误（props 被解构后失去响应性）
const { author } = props
watch(author, (newAuthor) => {
  // ❌ author 不是响应式的
})

// ✅ 正确（watch 箭头函数访问 props）
watch(() => props.author, (newAuthor) => {
  // ✓ 现在能检测到 props.author 变化
})
```

#### ❌ 错误：watch 回调中的异步状态
```typescript
// ❌ 错误（race condition）
watch(() => appStore.configReady, async (newValue) => {
  // 可能在异步操作中丢失状态
  const result = await fetchData()
  state.value = result  // ❌ 状态可能已过时
})

// ✅ 正确（检查状态有效性）
watch(() => appStore.configReady, async (newValue) => {
  if (!newValue) return  // 只在 true 时执行
  const result = await fetchData()
  if (appStore.configReady) {  // ✓ 再次检查
    state.value = result
  }
})
```

#### ❌ 错误：旧 watch 语法（Options API）
```typescript
// ❌ 错误（Options API 写法）
watch: {
  author(newVal) { ... }
}

// ✅ 正确（Composition API）
watch(() => props.author, (newVal) => { ... })
```

---

## 坑 10：onMounted 中 this 转换错误

### 症状
```
TS2339: Property 'xxx' does not exist on type 'any'
```

### 原因 & 解决

#### ❌ 错误：在 onMounted 中仍用 this
```typescript
// ❌ 错误（<script setup> 没有 this）
onMounted(() => {
  this.fetchData()  // ❌ TS2339
  this.$router.push(...)  // ❌ TS2339
})

// ✅ 正确（使用 composables 或顶层变量）
const fetchData = () => { ... }
const router = useRouter()

onMounted(() => {
  fetchData()
  router.push(...)
})
```

#### ❌ 错误：忘记 import composables
```typescript
// ❌ 错误（useRouter 未导入）
onMounted(() => {
  router.push(...)  // ❌ router 未定义
})

// ✅ 正确
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  router.push(...)
})
```

---

## 坑 11：v-model props 不能正确响应

### 症状
```
组件更新 props 但 v-model 不变化
```

### 原因 & 解决

#### ❌ 错误：v-model 中直接修改 props
```typescript
// ❌ 错误（props 是只读的）
const props = defineProps<{ modelValue: string }>()

const handleInput = (e: Event) => {
  props.modelValue = (e.target as HTMLInputElement).value  // ❌ 错误
}

// ✅ 正确（emit 事件给父组件）
const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const handleInput = (e: Event) => {
  emit('update:modelValue', (e.target as HTMLInputElement).value)
}
```

---

## 坑 12：组件注册时 name 属性为 undefined

### 症状
```
TS2345: Argument of type 'string | undefined' is not assignable to parameter of type 'string'
```

### 原因 & 解决

#### ❌ 错误：render() 函数组件的 name 属性
```typescript
// LoadingSkeleton/Skeleton.vue
export default defineComponent({
  render() { ... }  // render() 函数组件的 name 推导为 undefined
})

// LoadingSkeleton/index.ts
app.component(ObSkeleton.name, ObSkeleton)  // ❌ TS2345
```

#### ✅ 正确：类型断言
```typescript
// LoadingSkeleton/index.ts
app.component(ObSkeleton.name as string, ObSkeleton)  // ✓
app.component(ObSkeletonTheme.name as string, ObSkeletonTheme)
```

---

## 坑 13：computed 在 script setup 中不响应

### 症状
```
computed 返回的值在 props 变化时不更新
```

### 原因 & 解决

#### ❌ 错误：computed 依赖过期
```typescript
// ❌ 错误（computed 未添加响应式依赖）
const props = defineProps<{ items: any[] }>()

const filteredItems = computed(() => {
  return props.items.filter(i => i.active)  // ✓ 依赖 props
})

// 但如果 props 是嵌套对象，变化检测可能失败
// ✅ 正确（在修改时重新赋值整个对象）
const handleUpdate = () => {
  props.items = [...props.items]  // 触发重新计算
}
```

#### ❌ 错误：缓存了计算结果
```typescript
// ❌ 错误（值被缓存）
const props = defineProps<{ count: number }>()
const doubleCount = props.count * 2  // 不会自动更新

// ✅ 正确（用 computed）
const doubleCount = computed(() => props.count * 2)
```

---

## 快速排查清单

```bash
# 1. TypeScript 错误？
npm run type-check

# 2. 检查 Props/Emits 签名
grep -n "defineProps\|defineEmits" src/components/*.vue

# 3. 检查 inject/provide 键匹配
grep -n "provide\|inject" src/components/**/*.vue

# 4. 检查 ref 类型
grep -n "ref<" src/components/**/*.vue

# 5. 检查 script setup 中是否有 declare
grep -n "declare const" src/components/**/*.vue

# 6. 检查是否有未转换的 this
grep -n "this\." src/components/**/*.vue | grep -v "this\.xxx"  # 排除注释

# 7. 检查 defineExpose 调用
grep -n "defineExpose" src/components/**/*.vue
```

---

**完成排查**：如问题未解决，对比 [02-migrate.md](./02-migrate.md) 中对应 commit 的具体代码示例。
