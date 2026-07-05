# 前台搜索 keyword 接入实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 前台搜索弹窗停止读取旧 `/search.json`，改用 V2 公开文章列表 `keyword` 查询展示搜索结果。

**架构：** 不新增搜索接口、不生成静态搜索索引、不引入全文搜索引擎。后端复用现有 `GET /api/public/articles?keyword=...`，前台复用 `features/articles` 的 API、DTO 和 mapper，仅补一个搜索用 store 方法并改造现有 `SearchModal.vue`。

**技术栈：** Spring Boot 3、MyBatis、MockMvc、Vue 3、Pinia、Vue Router、Vitest、vue-tsc。

---

## 范围

本批只处理 O-017。友链、评论、留言、访问统计、PASSWORD 完整解锁不进入本批。

## 已确认现状

- 后端 `ArticleMapper.xml` 的公开列表 keyword 条件已查询三语标题和三语摘要。
- 后端公开列表不会返回正文，`PASSWORD` 文章只返回公开元数据和 `locked=true`。
- 前台 `frontend/apps/blog/src/api/index.ts` 仍有 `fetchSearchIndexes()` 读取 `/search.json`。
- 前台 `frontend/apps/blog/src/stores/search.ts` 仍维护本地 `SearchIndexes`。
- 前台 `frontend/apps/blog/src/components/SearchModal.vue` 已有弹窗、最近搜索、上下键和回车跳转。
- 当前文章详情路由名是 `article-detail`，参数为 `{ lang, id, slug }`，不能继续跳旧 `post-slug`。

## 不做

- 不新增 `GET /api/public/search`。
- 不恢复 `/search.json`。
- 不搜索正文。
- 不做正文片段截取。
- 不做服务端或前端高亮。
- 不新增依赖。

## Task 1: 后端 keyword 搜索口径补测试和契约

**Files:**
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/integration/ArticleIntegrationTest.java`
- Modify: `docs/handbook/api/article.md`

- [ ] **Step 1: 补后端集成测试**

在 `ArticleIntegrationTest` 中新增一个测试，直接插入 3 篇文章：

```java
@Test
void publicKeywordSearchMatchesTitleAndSummaryButNotBody()
        throws Exception {
    insertCategory(10L);
    insertTag(20L);
    insertAttachment(300L);
    insertPublishedArticle(100L, "Spring 标题", "普通摘要", "正文不重要");
    insertPublishedArticle(101L, "普通标题", "摘要包含 Spring", "正文不重要");
    insertPublishedArticle(102L, "普通标题", "普通摘要", "正文包含 Spring");

    mockMvc.perform(get("/api/public/articles")
                    .queryParam("lang", "zh")
                    .queryParam("keyword", "Spring"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.records[*].id")
                    .value(org.hamcrest.Matchers.containsInAnyOrder(
                            "100",
                            "101")))
            .andExpect(jsonPath("$.data.records[*].body").doesNotExist());
}
```

为该测试新增最小 helper：

```java
private void insertPublishedArticle(
        long id,
        String titleZh,
        String summaryZh,
        String body) {
    jdbcTemplate.update("""
            INSERT INTO t_article (
                id, title_zh, title_ja, title_en,
                summary_zh, summary_ja, summary_en,
                body_md, category_id, slug, status, publish_at,
                cover_attachment_id, homepage_slot,
                created_at, created_by, updated_at, updated_by, deleted
            ) VALUES (?, ?, null, null, ?, null, null, ?, 10, ?,
                2, CURRENT_TIMESTAMP, 300, 0,
                CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1001, 0)
            """,
            id,
            titleZh,
            summaryZh,
            body,
            "article-" + id);
}
```

- [ ] **Step 2: 运行后端定向测试**

Run:

```bash
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=ArticleIntegrationTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: 更新 API 契约**

在 `docs/handbook/api/article.md` 的公开文章列表 `keyword` 参数说明和列表口径后补充：

```text
`keyword` 第一版只匹配三语标题和三语摘要，不搜索正文，不返回高亮片段。搜索结果仍使用公开文章列表响应，不返回正文和密码信息。
```

- [ ] **Step 4: 提交**

提交前检查：

```bash
git diff --stat
git status --short
```

提交：

```bash
git add MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/integration/ArticleIntegrationTest.java docs/handbook/api/article.md
git commit -m "后端：明确公开文章关键词搜索口径"
```

## Task 2: 前台文章 API 支持 keyword 查询

**Files:**
- Modify: `frontend/apps/blog/src/features/articles/api.ts`
- Modify: `frontend/apps/blog/src/features/articles/api.test.ts`
- Modify: `frontend/apps/blog/src/stores/search.ts`

- [ ] **Step 1: 给公开文章 API 增加 keyword 参数**

在 `LoadPublicArticlesParams` 中增加：

```ts
keyword?: string
```

在 `loadPublicArticles` 解构和 params 中增加：

```ts
keyword
```

最终请求参数形状：

```ts
params: { page, size, lang, categorySlug, tagSlug, keyword }
```

- [ ] **Step 2: 补 API 测试**

在 `frontend/apps/blog/src/features/articles/api.test.ts` 增加：

```ts
it('loads public articles with keyword', async () => {
  mockedRequestApi.mockResolvedValueOnce({
    records: [],
    total: 0,
    page: 1,
    size: 8
  })

  await loadPublicArticles({
    page: 1,
    size: 8,
    lang: 'zh',
    keyword: 'Spring'
  })

  expect(mockedRequestApi).toHaveBeenCalledWith({
    method: 'GET',
    url: '/public/articles',
    params: {
      page: 1,
      size: 8,
      lang: 'zh',
      categorySlug: undefined,
      tagSlug: undefined,
      keyword: 'Spring'
    },
    signal: undefined
  })
})
```

同时把 import 改为：

```ts
import { loadPublicArchives, loadPublicArticles } from './api'
```

- [ ] **Step 3: 改造 search store**

在 `frontend/apps/blog/src/stores/search.ts` 中删除旧 `fetchSearchIndexes` 数据源，保留 `RecentSearchResults` 和弹窗状态。

新增搜索状态和远程查询：

```ts
import { normalizeApiError } from '@/shared/http/client'
import type { ApiError } from '@/shared/http/error'
import type { SupportedLocale } from '@/shared/i18n/locale'
import { loadPublicArticles } from '@/features/articles/api'
import { mapArticlePage } from '@/features/articles/mapper'
import type { ArticleCardViewModel } from '@/features/articles/model'

const searchResults = ref<ArticleCardViewModel[]>([])
const searchStatus = ref<'idle' | 'loading' | 'ready' | 'empty' | 'error'>('idle')
const searchError = ref<ApiError | null>(null)
let activeSearchRequest: AbortController | null = null

const searchArticles = async (
  keyword: string,
  locale: SupportedLocale
): Promise<void> => {
  const normalizedKeyword = keyword.trim()
  activeSearchRequest?.abort()
  if (normalizedKeyword === '') {
    searchResults.value = []
    searchStatus.value = 'idle'
    searchError.value = null
    return
  }

  const request = new AbortController()
  activeSearchRequest = request
  searchStatus.value = 'loading'
  searchError.value = null

  try {
    const page = mapArticlePage(
      await loadPublicArticles({
        page: 1,
        size: 8,
        lang: locale,
        keyword: normalizedKeyword,
        signal: request.signal
      }),
      locale
    )
    searchResults.value = page.records
    searchStatus.value = page.records.length > 0 ? 'ready' : 'empty'
  } catch (cause) {
    if (request.signal.aborted) return
    searchResults.value = []
    searchError.value = normalizeApiError(cause)
    searchStatus.value = 'error'
  } finally {
    if (activeSearchRequest === request) activeSearchRequest = null
  }
}
```

Return 中暴露：

```ts
searchResults,
searchStatus,
searchError,
searchArticles
```

- [ ] **Step 4: 运行前台定向测试**

Run:

```bash
pnpm --dir frontend/apps/blog test src/features/articles/api.test.ts
```

Expected:

```text
PASS
```

- [ ] **Step 5: 提交**

提交前检查：

```bash
git diff --stat
git status --short
```

提交：

```bash
git add frontend/apps/blog/src/features/articles/api.ts frontend/apps/blog/src/features/articles/api.test.ts frontend/apps/blog/src/stores/search.ts
git commit -m "前台：搜索数据层接入公开文章查询"
```

## Task 3: 搜索弹窗迁移到远程 keyword 查询

**Files:**
- Modify: `frontend/apps/blog/src/components/SearchModal.vue`
- Modify: `frontend/apps/blog/src/models/Search.class.ts`
- Modify: `frontend/apps/blog/src/api/index.ts`

- [ ] **Step 1: 调整 Search 结果模型**

在 `SearchResultType` 中把 slug 改为可携带文章 ID：

```ts
export type SearchResultType = {
  id?: string
  title: string
  content: string
  slug: string
}
```

`SearchResult` class 增加：

```ts
id = ''
```

这样最近搜索可继续缓存旧结构，也能缓存新文章 ID。

- [ ] **Step 2: 删除旧 search index API**

从 `frontend/apps/blog/src/api/index.ts` 删除：

```ts
import { SearchIndexes } from '@/models/Search.class'
export async function fetchSearchIndexes(): Promise<AxiosResponse<any>> {
  return request.get<SearchIndexes[]>('/search.json')
}
```

保留其他旧 page API，避免本批扩散。

- [ ] **Step 3: 改造 SearchModal 数据源**

删除：

```ts
const searchIndexStatus = ref(false)
onBeforeMount(initSearch)
searchStore.fetchSearchIndex()
searchStore.searchIndexes.search(keyword)
```

新增 mapper：

```ts
import { useAppStore } from '@/stores/app'
import type { ArticleCardViewModel } from '@/features/articles/model'

const appStore = useAppStore()

const toSearchResult = (article: ArticleCardViewModel): SearchResultType => ({
  id: article.id,
  title: article.title,
  content: article.summary || article.title,
  slug: article.slug
})
```

把 `searchKeyword` 改成远程查询：

```ts
const searchKeyword = _.debounce(async (event: Event) => {
  const target = event.target as HTMLInputElement | null
  const query = target?.value.trim() ?? ''
  if (query !== '') {
    await searchStore.searchArticles(query, appStore.locale)
    searchResults.value = searchStore.searchResults.map(toSearchResult)
    isEmpty.value = searchResults.value.length === 0
    resetIndex(searchResults.value.length)
  } else {
    isEmpty.value = false
    searchResults.value = []
    resetIndex(recentResults.value.length)
  }
}, 300)
```

跳转改为 ID 主导详情路由：

```ts
if (result.id) {
  router.push({
    name: 'article-detail',
    params: {
      lang: appStore.locale,
      id: result.id,
      slug: result.slug
    }
  })
}
```

最近搜索中的旧记录可能没有 `id`。对这种记录不要跳旧 `post-slug`，只关闭弹窗并忽略跳转；用户执行一次新搜索后缓存会自然更新。

- [ ] **Step 4: 保留现有交互**

确认以下逻辑仍存在：

```ts
@keydown.arrow-up.stop.prevent="handleArrowUp"
@keydown.arrow-down.stop.prevent="handleArrowDown"
@keydown.enter.stop.prevent="handleEnterDown"
reloadRecentResult()
addRecentSearch(result)
```

本批不新增加载动画。`searchStatus='loading'` 时继续显示最近搜索或空态；以后有体验问题再加。

- [ ] **Step 5: 运行前台验证**

Run:

```bash
pnpm --dir frontend/apps/blog typecheck
pnpm --dir frontend/apps/blog test
```

Expected:

```text
typecheck 通过
vitest 全部通过
```

- [ ] **Step 6: 提交**

提交前检查：

```bash
git diff --stat
git status --short
```

提交：

```bash
git add frontend/apps/blog/src/components/SearchModal.vue frontend/apps/blog/src/models/Search.class.ts frontend/apps/blog/src/api/index.ts
git commit -m "前台：搜索弹窗改用公开文章关键词查询"
```

## Task 4: 回填文档并关闭 O-017

**Files:**
- Modify: `docs/handbook/frontend/blog/integration-status.md`
- Modify: `docs/handbook/start-here/open-issues.md`

- [ ] **Step 1: 更新前台 Blog 联调状态**

将搜索从待补齐列表移到已完成列表，说明：

```text
搜索弹窗已接入公开文章 `keyword` 查询，不再读取旧 `/search.json`；第一版只展示标题和摘要，不搜索正文，不做高亮片段。
```

- [ ] **Step 2: 关闭 O-017**

将 O-017 状态改为已关闭，关闭原因写明：

```text
前台搜索弹窗已改用 `GET /api/public/articles?keyword=...`，结果使用公开文章列表口径，只展示标题和摘要；最近搜索、键盘上下选择和回车跳转保留；旧 `/search.json` 和正文高亮逻辑已下线。第一版不做全文搜索或 search index。
```

- [ ] **Step 3: 文档验证**

Run:

```bash
rg -n "O-017|search.json|keyword|搜索" docs/handbook/start-here/open-issues.md docs/handbook/frontend/blog/integration-status.md docs/handbook/api/article.md
```

Expected:

```text
O-017 标记为已关闭
前台状态文档不再把搜索列为待补齐
文章 API 文档明确 keyword 不搜索正文
```

- [ ] **Step 4: 提交**

提交前检查：

```bash
git diff --stat
git status --short
```

提交：

```bash
git add docs/handbook/frontend/blog/integration-status.md docs/handbook/start-here/open-issues.md
git commit -m "文档：关闭前台搜索关键词接入事项"
```

## 阶段验收

- [ ] `frontend/apps/blog/src/api/index.ts` 不再请求 `/search.json`。
- [ ] `frontend/apps/blog/src/components/SearchModal.vue` 不再调用 `fetchSearchIndex()` 或 `searchIndexes.search()`。
- [ ] 搜索请求使用 `/api/public/articles?keyword=...&page=1&size=8`。
- [ ] 搜索结果跳转使用 `article-detail` 的 `{ lang, id, slug }`。
- [ ] 后端测试覆盖标题/摘要命中和正文不命中。
- [ ] `mvn -f MyBlog-springboot-v2/pom.xml -Dtest=ArticleIntegrationTest test` 通过。
- [ ] `pnpm --dir frontend/apps/blog typecheck` 通过。
- [ ] `pnpm --dir frontend/apps/blog test` 通过。
- [ ] 文档中 O-017 已关闭。

## 提交拆分

1. `后端：明确公开文章关键词搜索口径`
2. `前台：搜索数据层接入公开文章查询`
3. `前台：搜索弹窗改用公开文章关键词查询`
4. `文档：关闭前台搜索关键词接入事项`
