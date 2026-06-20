# 博客前台 V2 首批后端联调实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让博客前台通过 V2 API 加载站点配置、公开文章列表和非 PASSWORD 文章详情，并建立三语路由、JST 时间、统一错误和安全 Markdown 基线。

**Architecture:** 后端先把公开文章业务 ID 收窄为 JSON string；前端按 `shared` 基础能力和 `features` 纵向切片组织。后端 DTO 只能经 mapper 转为 ViewModel，页面和组件不直接依赖 Axios 或旧 Hexo 数据模型。

**Tech Stack:** Java 17、Spring Boot 3.5、MockMvc、Vue 3.5、TypeScript 5.6、Pinia 2.3、Vue Router 4.5、Axios 1.7、Vitest 2.1、markdown-it 14、Vite 5、pnpm 9、Node 24

---

## 文件结构

本计划新增的稳定边界：

```text
frontend/apps/blog/src/
├─ shared/
│  ├─ http/{client,contract,error}.ts
│  ├─ i18n/locale.ts
│  ├─ time/jst.ts
│  └─ markdown/render.ts
└─ features/
   ├─ site-settings/{api,contract,defaults,mapper,model,store}.ts
   └─ articles/{api,contract,mapper,model,store}.ts
```

不得创建空的 `frontend/apps/admin` 或 `frontend/packages`。现有未迁移页面继续使用旧 store/model，但新代码不得依赖 `src/api/index.ts`、`Post.class.ts` 或 Hexo 顶层配置。

---

### Task 1: 将公开文章业务 ID 改为 JSON string

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArticleTagVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArticlePageItemVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArticleDetailVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/PublicArticleControllerTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/ArticleOpenApiTest.java`
- Modify: `docs/project-handbook/api-contract/article.md`

- [ ] **Step 1: 写超过 JavaScript 安全整数的失败测试**

在 `PublicArticleControllerTest` 增加常量并把 fixture 的文章、分类、标签 ID 替换为这些值：

```java
private static final long ARTICLE_ID = 9_007_199_254_740_993L;
private static final long CATEGORY_ID = 9_007_199_254_740_994L;
private static final long TAG_ID = 9_007_199_254_740_995L;
```

列表和详情断言：

```java
.andExpect(jsonPath("$.data.records[0].id")
        .value(Long.toString(ARTICLE_ID)))
.andExpect(jsonPath("$.data.records[0].categoryId")
        .value(Long.toString(CATEGORY_ID)))
.andExpect(jsonPath("$.data.records[0].tags[0].id")
        .value(Long.toString(TAG_ID)));
```

详情使用同样三项断言。`ArticleOpenApiTest` 断言三个公开 Schema 中 ID 属性的 `type` 为 `string`。

- [ ] **Step 2: 运行测试并确认失败原因**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=PublicArticleControllerTest,ArticleOpenApiTest" test
```

Expected: FAIL；JSONPath 得到 number 或 OpenAPI 类型为 integer，而不是 string。

- [ ] **Step 3: 创建公开标签 Web DTO**

```java
package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.article.PublicArticleTagResult;

public record PublicArticleTagVO(
        String id,
        String name,
        String slug) {

    static PublicArticleTagVO from(PublicArticleTagResult result) {
        return new PublicArticleTagVO(
                Long.toString(result.id()),
                result.name(),
                result.slug());
    }
}
```

- [ ] **Step 4: 修改公开文章 DTO 和映射**

两个公开文章 VO 的字段改为：

```java
String id,
String categoryId,
List<PublicArticleTagVO> tags
```

`ArticleWebMapping` 增加：

```java
private String id(long value) {
    return Long.toString(value);
}

private String nullableId(Long value) {
    return value == null ? null : Long.toString(value);
}

private List<PublicArticleTagVO> publicTags(
        List<PublicArticleTagResult> tags) {
    return tags.stream().map(PublicArticleTagVO::from).toList();
}
```

只在 `toPublicPageItem` 和 `toPublicDetail` 使用这些方法。后台 VO、应用结果、数据库字段和 Controller path variable 保持 `long`。

- [ ] **Step 5: 同步契约文档**

在 `article.md` 通用约定中写明公开文章的 `id/categoryId/tags[].id` 是十进制字符串；请求路径仍使用十进制 ID。示例 ID 必须使用引号。

- [ ] **Step 6: 运行后端局部验证**

Run:

```powershell
mvn "-Dtest=PublicArticleControllerTest,ArticleOpenApiTest" test
```

Expected: PASS，0 failures，0 errors。

- [ ] **Step 7: 检查并提交**

```powershell
git diff --stat
git status --short
git add -- MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web docs/project-handbook/api-contract/article.md
git commit -m "修复公开文章ID前端精度契约"
```

---

### Task 2: 建立可执行的前端质量基线

**Files:**
- Modify: `frontend/apps/blog/package.json`
- Modify: `frontend/apps/blog/pnpm-lock.yaml`
- Modify: `frontend/apps/blog/src/App.vue`
- Modify: `frontend/apps/blog/src/components/Header/src/Controls.vue`
- Modify: `frontend/apps/blog/src/components/MobileMenu.vue`
- Modify: `frontend/apps/blog/src/components/SearchModal.vue`
- Modify: `frontend/apps/blog/src/components/Paginator.vue`

- [ ] **Step 1: 安装固定版本的类型检查与测试工具**

```powershell
cd frontend/apps/blog
pnpm add -D --save-exact vue-tsc@2.2.12 vitest@2.1.9
```

在 `package.json` 增加：

```json
"typecheck": "vue-tsc --noEmit",
"test": "vitest"
```

- [ ] **Step 2: 运行类型检查并保存现有失败基线**

Run: `pnpm typecheck`

Expected: FAIL，集中出现 App scripts 参数、Dropdown locale、`zh-TW`、EventTarget 和 Paginator emit 相关的 10 个错误。

- [ ] **Step 3: 修复现有 10 个类型错误**

精确修改：

```ts
// App.vue
metaStore.addScripts(...appStore.themeConfig.site_meta.cdn.prismjs)

// Controls.vue；Task 3 再替换为共享 type guard
const handleClick = (name: string): void => {
  if (name === 'en' || name === 'zh-CN' || name === 'ja') {
    appStore.changeLocale(name)
  }
}

// SearchModal.vue
const searchKeyword = _.debounce((event: Event) => {
  const target = event.target as HTMLInputElement | null
  const keyword = target?.value ?? ''
  // 后续逻辑统一使用 keyword
})
```

`MobileMenu.vue` 的两个 `zh-TW` 分支改为 `ja`。`Paginator.vue` 将 emit 收窄为 number：

```ts
const emit = defineEmits<{
  (e: 'pageChange', page: number): void
}>()

const pageChangeEmitter = (page: number | string) => {
  if (typeof page !== 'number') return
  emit('pageChange', page)
}
```

- [ ] **Step 4: 验证质量基线**

Run:

```powershell
pnpm typecheck
pnpm lint
pnpm build
```

Expected: 三条命令退出 0；Lint 可保留现有 6 条 `no-console` warning，本任务不顺带清理。

- [ ] **Step 5: 检查并提交**

```powershell
git diff --stat
git status --short
git add -- frontend/apps/blog/package.json frontend/apps/blog/pnpm-lock.yaml frontend/apps/blog/src/App.vue frontend/apps/blog/src/components/Header/src/Controls.vue frontend/apps/blog/src/components/MobileMenu.vue frontend/apps/blog/src/components/SearchModal.vue frontend/apps/blog/src/components/Paginator.vue
git commit -m "建立前台类型检查与测试基线"
```

---

### Task 3: 实现三语选择和 id-led 路由

**Files:**
- Create: `frontend/apps/blog/src/shared/i18n/locale.ts`
- Create: `frontend/apps/blog/src/shared/i18n/locale.test.ts`
- Modify: `frontend/apps/blog/src/models/ThemeConfig.class.ts`
- Rename: `frontend/apps/blog/src/locales/languages/zh-CN.json` -> `frontend/apps/blog/src/locales/languages/zh.json`
- Modify: `frontend/apps/blog/src/locales/index.ts`
- Modify: `frontend/apps/blog/src/stores/app.ts`
- Modify: `frontend/apps/blog/src/components/Header/src/Controls.vue`
- Modify: `frontend/apps/blog/src/components/MobileMenu.vue`
- Modify: `frontend/apps/blog/src/router/index.ts`
- Modify: `frontend/apps/blog/src/router/guard.ts`

- [ ] **Step 1: 写语言选择失败测试**

```ts
import { describe, expect, it } from 'vitest'
import { resolveInitialLocale, isSupportedLocale } from './locale'

describe('locale selection', () => {
  it('prefers a saved locale', () => {
    expect(resolveInitialLocale('ja', 'zh-CN')).toBe('ja')
  })

  it.each([
    ['zh-CN', 'zh'],
    ['zh-TW', 'zh'],
    ['ja-JP', 'ja'],
    ['fr-FR', 'en']
  ])('maps %s to %s', (system, expected) => {
    expect(resolveInitialLocale(null, system)).toBe(expected)
  })

  it('rejects unsupported route params', () => {
    expect(isSupportedLocale('de')).toBe(false)
  })
})
```

- [ ] **Step 2: 运行测试确认模块不存在**

Run: `pnpm test --run src/shared/i18n/locale.test.ts`

Expected: FAIL，无法导入 `./locale`。

- [ ] **Step 3: 实现语言模块**

```ts
export const SUPPORTED_LOCALES = ['zh', 'ja', 'en'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

export const isSupportedLocale = (
  value: unknown
): value is SupportedLocale =>
  typeof value === 'string' &&
  SUPPORTED_LOCALES.includes(value as SupportedLocale)

export const systemLocale = (language: string): SupportedLocale => {
  const normalized = language.toLowerCase()
  if (normalized.startsWith('zh')) return 'zh'
  if (normalized.startsWith('ja')) return 'ja'
  return 'en'
}

export const resolveInitialLocale = (
  saved: string | null,
  language: string
): SupportedLocale =>
  isSupportedLocale(saved) ? saved : systemLocale(language)
```

- [ ] **Step 4: 收敛 locale 类型与资源名**

`Locales` 改为 `'zh' | 'ja' | 'en'`，资源文件改名为 `zh.json`，i18n 默认与 fallback 改为 `en`。Cookie key 保持 `locale`，但旧值 `zh-CN` 读取时迁移为 `zh`。

- [ ] **Step 5: 建立手工语言路由**

`router/index.ts` 在生成路由前增加：

```ts
const localizedRoutes = [
  {
    path: '/',
    name: 'root',
    redirect: () => ({ name: 'home', params: { lang: initialLocale() } })
  },
  {
    path: '/:lang(zh|ja|en)',
    name: 'home',
    component: () => import('@/pages/index.vue')
  },
  {
    path: '/:lang(zh|ja|en)/posts/:id(\\d+)/:slug?',
    name: 'article-detail',
    component: () => import('@/pages/post/[slug].vue')
  }
]

const remainingGeneratedRoutes = routes.filter(
  route => route.name !== 'index' && route.name !== 'post-slug'
)
```

Router 的最终 routes 使用 `[...localizedRoutes, ...remainingGeneratedRoutes]`。`initialLocale()` 从 localStorage/Cookie 中读取已保存值，否则使用 `navigator.language`。守卫验证 `to.params.lang`，设置 i18n locale 和 app store；语言切换使用当前 route name/params/query 重建 URL，不跳回首页。

- [ ] **Step 6: 验证语言与路由**

```powershell
pnpm test --run src/shared/i18n/locale.test.ts
pnpm typecheck
pnpm lint
pnpm build
```

Expected: 全部退出 0。

- [ ] **Step 7: 检查并提交**

```powershell
git diff --stat
git status --short
git add -- frontend/apps/blog/src/shared/i18n frontend/apps/blog/src/models/ThemeConfig.class.ts frontend/apps/blog/src/locales frontend/apps/blog/src/stores/app.ts frontend/apps/blog/src/components/Header/src/Controls.vue frontend/apps/blog/src/components/MobileMenu.vue frontend/apps/blog/src/router
git commit -m "建立前台三语路由基线"
```

---

### Task 4: 实现统一 HTTP、错误与 JST 工具

**Files:**
- Create: `frontend/apps/blog/src/shared/http/contract.ts`
- Create: `frontend/apps/blog/src/shared/http/error.ts`
- Create: `frontend/apps/blog/src/shared/http/client.ts`
- Create: `frontend/apps/blog/src/shared/http/client.test.ts`
- Create: `frontend/apps/blog/src/shared/time/jst.ts`
- Create: `frontend/apps/blog/src/shared/time/jst.test.ts`
- Modify: `frontend/apps/blog/vite.config.mjs`
- Modify: `frontend/apps/blog/.env`
- Modify: `frontend/apps/blog/.env.production`

- [ ] **Step 1: 写响应解包和 JST 失败测试**

测试必须覆盖：成功码返回 data；非 `00000` 抛出包含 code/msg 的 `ApiError`；`2026-06-15T10:00:00` 在任意机器时区都格式化为 JST 2026-06-15 10:00。

```ts
expect(unwrapApiResponse({ code: '00000', msg: 'success', data: 1 }))
  .toBe(1)
expect(() => unwrapApiResponse({ code: '20001', msg: '不存在', data: null }))
  .toThrowError(ApiError)
expect(formatJst('2026-06-15T10:00:00', 'en')).toContain('2026')
```

- [ ] **Step 2: 运行测试确认失败**

Run: `pnpm test --run src/shared/http/client.test.ts src/shared/time/jst.test.ts`

Expected: FAIL，模块不存在。

- [ ] **Step 3: 实现统一契约和错误**

```ts
export interface ApiResponse<T> {
  code: string
  msg: string
  data: T | null
}

export interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly code?: string,
    readonly cause?: unknown
  ) {
    super(message)
    this.name = 'ApiError'
  }
}
```

`client.ts` 创建 `axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || '/api', timeout: 5000 })`。成功响应必须调用 `unwrapApiResponse`；Axios 错误转换为 `ApiError`，保留 HTTP status、响应 code/msg 和 cause。请求函数接受 `AbortSignal`。

- [ ] **Step 4: 实现 JST 工具**

```ts
export const parseJst = (value: string): Date => {
  const parsed = new Date(`${value}+09:00`)
  if (Number.isNaN(parsed.getTime())) throw new TypeError('Invalid JST date')
  return parsed
}
```

格式化使用 `Intl.DateTimeFormat`，强制 `timeZone: 'Asia/Tokyo'`，locale 映射为 `zh-CN/ja-JP/en-US`。禁止调用 `new Date(rawLocalDateTime)`。

- [ ] **Step 5: 配置本地代理**

环境变量：

```dotenv
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://localhost:8080
```

Vite server 增加 `/api` proxy，target 读取 `VITE_API_PROXY_TARGET`，`changeOrigin: true`，不 rewrite path。生产默认使用同源 `/api`。

- [ ] **Step 6: 验证并提交**

```powershell
pnpm test --run src/shared/http/client.test.ts src/shared/time/jst.test.ts
pnpm typecheck
pnpm lint
pnpm build
git diff --stat
git status --short
git add -- frontend/apps/blog/src/shared/http frontend/apps/blog/src/shared/time frontend/apps/blog/vite.config.mjs frontend/apps/blog/.env frontend/apps/blog/.env.production
git commit -m "建立前台HTTP与JST基础能力"
```

---

### Task 5: 迁移站点配置并移除 site.json 依赖

**Files:**
- Create: `frontend/apps/blog/src/features/site-settings/contract.ts`
- Create: `frontend/apps/blog/src/features/site-settings/model.ts`
- Create: `frontend/apps/blog/src/features/site-settings/defaults.ts`
- Create: `frontend/apps/blog/src/features/site-settings/mapper.ts`
- Create: `frontend/apps/blog/src/features/site-settings/mapper.test.ts`
- Create: `frontend/apps/blog/src/features/site-settings/api.ts`
- Create: `frontend/apps/blog/src/features/site-settings/store.ts`
- Create: `frontend/apps/blog/src/features/site-settings/store.test.ts`
- Modify: `frontend/apps/blog/src/stores/app.ts`
- Modify: `frontend/apps/blog/src/App.vue`
- Modify: `frontend/apps/blog/src/api/index.ts`
- Delete: `frontend/apps/blog/public/api/site.json`
- Delete: `frontend/apps/blog/public/api/site.config.md`

- [ ] **Step 1: 写配置映射和失败降级测试**

Contract 精确对应后端七个字段，全部可选字段使用 `string | null`：

```ts
export interface PublicSiteConfigDto {
  siteTitle: string
  siteSubtitle: string | null
  aboutMd: string | null
  logoUrl: string | null
  faviconUrl: string | null
  icpNo: string | null
  spotifyPlaylistId: string | null
}
```

测试断言：后端标题覆盖默认标题；后端显式 `null` 保持空值；主题色、导航、作者和社交链接始终来自 defaults；API 失败时 store 状态为 `degraded` 且仍有可渲染 settings。

- [ ] **Step 2: 运行测试确认失败**

Run: `pnpm test --run src/features/site-settings`

Expected: FAIL，feature 模块不存在。

- [ ] **Step 3: 建立 typed defaults**

把当前 `public/api/site.json` 中仍有效的 `theme_config` 内容机械迁入 `defaults.ts`，删除所有 Hexo 顶层字段。导出只读的 `DEFAULT_SITE_SETTINGS`，包含：主题、导航、作者、社交、布局、首页动画和后端字段 fallback。不得保留 Waline 等将被自研后端替代的启用配置。

- [ ] **Step 4: 实现 mapper、API 和 store**

```ts
export type SiteSettingsStatus = 'idle' | 'loading' | 'ready' | 'degraded'

export interface SiteSettingsViewModel {
  siteTitle: string
  siteSubtitle: string | null
  aboutMd: string | null
  logoUrl: string | null
  faviconUrl: string | null
  icpNo: string | null
  spotifyPlaylistId: string | null
  themeConfig: ThemeConfig
}
```

`loadPublicSiteConfig(locale, signal)` 调用 `/public/site-config?lang=...`。Store 在每次 load 前 abort 上次请求；失败时保留 defaults 并设置 `degraded`，不抛出阻断 App 初始化的异常。

- [ ] **Step 5: 通过 app store 提供兼容出口**

`useAppStore` 委托 `useSiteSettingsStore`，继续返回现有组件所需的 `themeConfig`，同时暴露 `siteTitle/siteSubtitle/configStatus`。`fetchConfig` 改为调用新 store；删除 200ms 人工延时和 `fetchHexoConfig`。

- [ ] **Step 6: 删除静态配置读取与文件**

确认 `rg "site.json|fetchHexoConfig" frontend/apps/blog/src` 无结果后，删除 `site.json`、说明文件和旧 API 函数。其他 mock 文件不动。

- [ ] **Step 7: 验证并提交**

```powershell
pnpm test --run src/features/site-settings
pnpm typecheck
pnpm lint
pnpm build
git diff --stat
git status --short
git add -- frontend/apps/blog/src/features/site-settings frontend/apps/blog/src/stores/app.ts frontend/apps/blog/src/App.vue frontend/apps/blog/src/api/index.ts frontend/apps/blog/public/api/site.json frontend/apps/blog/public/api/site.config.md
git commit -m "接入前台公开站点配置"
```

---

### Task 6: 接入公开文章列表和首页状态

**Files:**
- Create: `frontend/apps/blog/src/features/articles/contract.ts`
- Create: `frontend/apps/blog/src/features/articles/model.ts`
- Create: `frontend/apps/blog/src/features/articles/mapper.ts`
- Create: `frontend/apps/blog/src/features/articles/mapper.test.ts`
- Create: `frontend/apps/blog/src/features/articles/api.ts`
- Create: `frontend/apps/blog/src/features/articles/store.ts`
- Create: `frontend/apps/blog/src/features/articles/store.test.ts`
- Modify: `frontend/apps/blog/src/pages/index.vue`
- Modify: `frontend/apps/blog/src/components/ArticleCard/src/ArticleCard.vue`
- Modify: `frontend/apps/blog/src/components/ArticleCard/src/HorizontalArticle.vue`
- Modify: `frontend/apps/blog/src/components/Feature/src/Feature.vue`
- Modify: `frontend/apps/blog/src/components/Feature/src/FeatureList.vue`
- Modify: `frontend/apps/blog/src/api/index.ts`
- Delete: `frontend/apps/blog/public/api/posts/1.json`
- Delete: `frontend/apps/blog/public/api/features.json`

- [ ] **Step 1: 写列表 mapper 失败测试**

DTO 中所有业务 ID 使用 string：

```ts
export interface PublicArticleListItemDto {
  id: string
  title: string
  summary: string | null
  categoryId: string | null
  categoryName: string | null
  slug: string
  publishAt: string
  coverUrl: string | null
  commentCount: number
  tags: Array<{ id: string; name: string; slug: string }>
  createdAt: string
  locked: boolean
}
```

测试使用 `id: '9007199254740993'`，断言映射后完全一致；总页数为 `Math.ceil(total / size)`；JST 日期不受机器时区影响；null 封面使用 ViewModel 的 `coverUrl: null`。

- [ ] **Step 2: 运行测试确认失败**

Run: `pnpm test --run src/features/articles/mapper.test.ts`

Expected: FAIL，articles 模块不存在。

- [ ] **Step 3: 实现列表 ViewModel 与 mapper**

```ts
export interface ArticleCardViewModel {
  id: string
  slug: string
  title: string
  summary: string
  coverUrl: string | null
  category: { id: string; name: string } | null
  tags: Array<{ id: string; name: string; slug: string }>
  publishedAt: string
  locked: boolean
  commentCount: number
}

export interface ArticlePageViewModel {
  records: ArticleCardViewModel[]
  total: number
  page: number
  size: number
  pages: number
}
```

- [ ] **Step 4: 实现列表 API 和 Store 测试**

`loadPublicArticles({ page, size: 12, lang, signal })` 调用 `/public/articles`。Store 状态固定为 `idle/loading/ready/empty/error`，每次 load abort 上次请求；取消错误不进入 error。测试 mock API，覆盖成功、空页、失败、重试和旧请求取消。

- [ ] **Step 5: 迁移首页与卡片**

首页只读取 article feature store。加载时显示骨架，empty 显示明确空状态，error 显示错误文本和重试按钮。首页暂不加载分类、标签和 RecentComment 的真实数据。

卡片跳转固定为：

```ts
router.push({
  name: 'article-detail',
  params: { lang: route.params.lang, id: article.id, slug: article.slug }
})
```

普通卡片、横向卡片和 Feature 区统一接收 `ArticleCardViewModel`。Feature 区使用当前页第一条作为主卡、后续最多三条作为副卡，不新增后端精选字段。

- [ ] **Step 6: 删除列表静态读取链路**

删除 `fetchPostsList`、`fetchFeature` 对首页的调用及两个 mock 文件。旧 API 中仍被归档等页面使用的函数保留。

- [ ] **Step 7: 验证并提交**

```powershell
pnpm test --run src/features/articles
pnpm typecheck
pnpm lint
pnpm build
git diff --stat
git status --short
git add -- frontend/apps/blog/src/features/articles frontend/apps/blog/src/pages/index.vue frontend/apps/blog/src/components/ArticleCard frontend/apps/blog/src/components/Feature frontend/apps/blog/src/api/index.ts frontend/apps/blog/public/api/posts/1.json frontend/apps/blog/public/api/features.json
git commit -m "接入前台公开文章列表"
```

---

### Task 7: 接入文章详情和安全 Markdown

**Files:**
- Create: `frontend/apps/blog/src/shared/markdown/render.ts`
- Create: `frontend/apps/blog/src/shared/markdown/render.test.ts`
- Modify: `frontend/apps/blog/package.json`
- Modify: `frontend/apps/blog/pnpm-lock.yaml`
- Modify: `frontend/apps/blog/src/features/articles/contract.ts`
- Modify: `frontend/apps/blog/src/features/articles/model.ts`
- Modify: `frontend/apps/blog/src/features/articles/mapper.ts`
- Modify: `frontend/apps/blog/src/features/articles/mapper.test.ts`
- Modify: `frontend/apps/blog/src/features/articles/api.ts`
- Modify: `frontend/apps/blog/src/features/articles/store.ts`
- Modify: `frontend/apps/blog/src/features/articles/store.test.ts`
- Modify: `frontend/apps/blog/src/pages/post/[slug].vue`
- Modify: `frontend/apps/blog/src/api/index.ts`

- [ ] **Step 1: 安装 Markdown 依赖**

```powershell
pnpm add --save-exact markdown-it@14.1.0
pnpm add -D --save-exact @types/markdown-it@14.1.2
```

- [ ] **Step 2: 写 Markdown 安全失败测试**

```ts
expect(renderMarkdown('# Title')).toContain('<h1>Title</h1>')
expect(renderMarkdown('<script>alert(1)</script>')).not.toContain('<script>')
expect(renderMarkdown('[x](https://example.com)'))
  .toContain('rel="nofollow noopener noreferrer"')
```

- [ ] **Step 3: 实现最小 Markdown 渲染器**

```ts
const markdown = new MarkdownIt({ html: false, linkify: true, typographer: false })
const defaultLinkOpen = markdown.renderer.rules.link_open

markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  tokens[index].attrSet('rel', 'nofollow noopener noreferrer')
  const href = tokens[index].attrGet('href') ?? ''
  if (/^https?:\/\//i.test(href)) tokens[index].attrSet('target', '_blank')
  return defaultLinkOpen
    ? defaultLinkOpen(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options)
}

export const renderMarkdown = (source: string): string => markdown.render(source)
```

- [ ] **Step 4: 写详情 mapper 与 Store 失败测试**

详情 DTO 扩展 `body/updatedAt`。测试覆盖：string ID、Markdown 转 HTML、canonical slug、403+10003 -> locked、404 -> notFound、网络/5xx -> error、AbortError 静默。

详情状态：

```ts
export type ArticleDetailStatus =
  | 'idle' | 'loading' | 'ready' | 'locked' | 'notFound' | 'error'
```

- [ ] **Step 5: 实现详情 API、mapper 和 store**

`loadPublicArticle(id, lang, signal)` 请求 `/public/articles/${encodeURIComponent(id)}`。mapper 输出 `ArticleDetailViewModel`，其中 `bodyHtml` 只能来自 `renderMarkdown(dto.body)`。

Store 捕获 `ApiError`：status 403 且 code `10003` -> locked；404 -> notFound；其他 -> error。成功后返回当前 slug 给页面做 canonical replace。

- [ ] **Step 6: 迁移详情页**

从 route 读取 `lang/id/slug`。成功且 slug 缺失或不一致时：

```ts
await router.replace({
  name: 'article-detail',
  params: { lang, id: article.id, slug: article.slug },
  hash: route.hash
})
```

页面按状态显示骨架、正文、锁定、404 或重试。正文只 `v-html="article.bodyHtml"`。首批禁用旧第三方评论、上一篇/下一篇、旧 PostStats 调用；它们在对应后端批次恢复，不能继续向静态/第三方接口发请求。

- [ ] **Step 7: 删除旧详情静态 API 调用**

确认迁移后的详情页不再导入 `Post`、`usePostStore`、`fetchPostBySlug`。若无其他消费者，再从旧 API/store 中删除对应函数；不要删除归档、分类等仍在使用的旧代码。

- [ ] **Step 8: 验证并提交**

```powershell
pnpm test --run src/shared/markdown src/features/articles
pnpm typecheck
pnpm lint
pnpm build
git diff --stat
git status --short
git add -- frontend/apps/blog/package.json frontend/apps/blog/pnpm-lock.yaml frontend/apps/blog/src/shared/markdown frontend/apps/blog/src/features/articles ':(literal)frontend/apps/blog/src/pages/post/[slug].vue' frontend/apps/blog/src/api/index.ts frontend/apps/blog/src/stores/post.ts
git commit -m "接入前台公开文章详情"
```

---

### Task 8: 完成真实联调与阶段文档

**Files:**
- Modify: `docs/project-handbook/frontend-user/README.md`
- Modify: `docs/project-handbook/status.md`
- Modify: `docs/project-handbook/roadmap.md`

- [ ] **Step 1: 运行后端完整验证**

```powershell
cd MyBlog-springboot-v2
mvn clean test
```

Expected: 0 failures，0 errors；Docker 不可用时只允许既有 Testcontainers 条件测试 skipped。

- [ ] **Step 2: 运行前端完整验证**

```powershell
cd ../frontend/apps/blog
pnpm install --frozen-lockfile
pnpm lint
pnpm typecheck
pnpm test --run
pnpm build
```

Expected: 全部退出 0。

- [ ] **Step 3: 启动真实后端和前台**

终端 A：

```powershell
cd MyBlog-springboot-v2
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

终端 B：

```powershell
cd frontend/apps/blog
pnpm dev
```

- [ ] **Step 4: 执行浏览器验收清单**

逐项验证：首次语言跳转、已保存语言、首页配置、真实列表、分页、语言切换取消旧请求、非 PASSWORD 详情、canonical replace、PASSWORD 锁定、404、停止后端后的重试。浏览器 Network 中不得再出现 `site.json`、`posts/1.json`、`features.json` 或 `/articles/{slug}.json`。

- [ ] **Step 5: 更新阶段文档**

`frontend-user/README.md` 写入当前目录、启动命令、Node/pnpm 基线、已完成范围和下一批次；`status.md` 与 `roadmap.md` 将“前台骨架”更新为首批站点配置与文章链路已完成，但后台、分类评论等仍未完成。

- [ ] **Step 6: 检查并提交文档**

```powershell
git diff --stat
git status --short
git add -- docs/project-handbook/frontend-user/README.md docs/project-handbook/status.md docs/project-handbook/roadmap.md
git commit -m "记录前台首批后端联调进度"
```

---

## 完成条件

- 后端公开文章 ID 在 JSON 中不丢精度。
- `/`、`/zh`、`/ja`、`/en` 和 id-led 文章路由按设计工作。
- 首页只从 V2 后端读取站点配置与文章列表。
- 非 PASSWORD 详情渲染安全 Markdown；PASSWORD/404/网络错误状态可区分。
- 被迁移链路不再请求对应 Hexo mock。
- 每个任务均有独立中文提交，且局部验证先于阶段级完整验证。
