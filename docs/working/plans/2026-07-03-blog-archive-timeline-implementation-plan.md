# Blog Archive Timeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the V2 public archive timeline API and migrate the Blog archive page away from old `/archives/{page}.json`.

**Architecture:** Keep this in the existing content/article and Blog article feature boundaries. Backend reuses the public article visibility criteria and returns a narrow archive DTO grouped by publish month. Frontend adds archive loading to the existing public article feature instead of introducing a new store.

**Tech Stack:** Spring Boot 3, MyBatis XML, MockMvc, OpenAPI tests, Vue 3, Pinia, Vitest, pnpm.

---

## Scope

This plan implements O-016 only.

It does not implement search, about, friend links, comments, guestbook, stats, RSS, sitemap, or PASSWORD unlock. It also does not add a separate month index endpoint.

Important correction from the older archive plan: archive article items must include `id`. Current article detail routing is ID-led (`/:lang/posts/:id/:slug?`), so a slug-only archive response would force the frontend back to the legacy `post-slug` route.

## File Map

### Backend

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleQueryService.java`
  - Add `archives(PublicArticleQuery query)` that validates `page/size/lang`, queries the existing public page repository with no category/tag/search filters, and groups current-page articles by `publishAt` year-month.
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArchivePageResult.java`
  - Application result with `records/total/page/size`, month groups, and narrow article items.
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArchiveController.java`
  - `GET /api/public/archives?page=1&size=12&lang=zh`.
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArchivePageVO.java`
  - HTTP VO with string IDs.
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`
  - Add mapping from `PublicArchivePageResult` to `PublicArchivePageVO`.
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
  - Add `GET /api/public/archives` to public endpoints.
- Tests:
  - Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/PublicArticleQueryServiceTest.java`
  - Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/PublicArchiveControllerTest.java`
  - Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/ArticleOpenApiTest.java`
  - Modify public endpoint config/security tests only if they currently assert exact endpoint lists:
    - `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/ApplicationConfigurationTest.java`
    - `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/BackendPropertiesTest.java`
    - `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`

No new MyBatis query is planned for the first cut. Reuse `PublicArticleQueryRepository.findPublicPage(...)`; it already applies `PUBLISHED/PASSWORD + publish_at <= now + deleted=0` and stable `publish_at DESC, id DESC` ordering. This costs one existing tag query internally, but avoids duplicate SQL and visibility drift. Add a narrower SQL query only if profiling shows archive traffic needs it.

### Frontend

- Modify: `frontend/apps/blog/src/features/articles/contract.ts`
  - Add archive DTO types.
- Modify: `frontend/apps/blog/src/features/articles/api.ts`
  - Add `loadPublicArchives(...)`.
- Modify: `frontend/apps/blog/src/features/articles/model.ts`
  - Add archive view models and status type reuse.
- Modify: `frontend/apps/blog/src/features/articles/mapper.ts`
  - Add `mapArchivePage(...)`.
- Modify: `frontend/apps/blog/src/features/articles/store.ts`
  - Add archive state/load/retry using the same abort/error pattern as articles and home.
- Modify tests:
  - `frontend/apps/blog/src/features/articles/mapper.test.ts`
  - `frontend/apps/blog/src/features/articles/store.test.ts`
- Modify: `frontend/apps/blog/src/pages/archives.vue`
  - Replace `usePostStore().fetchArchives(...)` and old `Archives` model with `useArticleStore().loadArchives(...)`.
  - Link archive items to `article-detail` with `{ lang, id, slug }`.

### Docs

- Modify: `docs/handbook/api/article.md`
  - Add public archive timeline API.
  - Fix public detail example IDs to string if touched.
- Modify: `docs/handbook/frontend/blog/integration-status.md`
  - Mark archive page as migrated after implementation.
- Modify: `docs/handbook/start-here/open-issues.md`
  - Close O-016 after implementation and verification.
- Modify: `docs/handbook/start-here/current-status.md`
  - Remove archive from remaining Blog main-link gaps after implementation.

## API Contract

Endpoint:

```http
GET /api/public/archives?page=1&size=12&lang=zh
```

Success response:

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "records": [
      {
        "yearMonth": "2026-06",
        "year": 2026,
        "month": 6,
        "articles": [
          {
            "id": "9007199254740993",
            "title": "文章标题",
            "slug": "article-slug",
            "publishedAt": "2026-06-15T10:00:00",
            "summary": "文章摘要"
          }
        ]
      }
    ],
    "total": 1,
    "page": 1,
    "size": 12
  }
}
```

Rules:

- `page` starts at 1.
- `size` range is 1 to 100; frontend uses 12.
- `lang` supports `zh/ja/en`; invalid or missing falls back to `zh`, matching public article list behavior.
- `total` is article count, not month group count.
- Pagination is by article count before grouping.
- Grouping is only for the current page.
- Sorting is `publishAt DESC, id DESC`.
- Public visibility matches article list: `PUBLISHED` and `PASSWORD`, `publish_at <= now`, not soft deleted.
- Response does not include body, category, tags, cover, comment count, status, locked flag, password fields, or storage fields.

## Task 1: Backend Application Contract

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArchivePageResult.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleQueryService.java`
- Modify test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/PublicArticleQueryServiceTest.java`

- [ ] **Step 1: Add failing service test for archive grouping**

Add a test named `returnsArchiveTimelineGroupedByCurrentPageMonths`.

Test shape:

```java
PublicArticleQuery query = new PublicArticleQuery(
        1, 3, "en", null, null, null, null, null, null);
when(repository.findPublicPage(query.toCriteria(NOW)))
        .thenReturn(new PublicArticlePage(
                List.of(
                        pageItem(103L, ArticleStatus.PASSWORD, null,
                                LocalDateTime.of(2026, 6, 16, 10, 0)),
                        pageItem(102L, ArticleStatus.PUBLISHED, null,
                                LocalDateTime.of(2026, 6, 15, 10, 0)),
                        pageItem(101L, ArticleStatus.PUBLISHED, null,
                                LocalDateTime.of(2026, 5, 31, 10, 0))),
                4,
                1,
                3));

PublicArchivePageResult result = service.archives(query);

assertThat(result.total()).isEqualTo(4);
assertThat(result.records()).extracting("yearMonth")
        .containsExactly("2026-06", "2026-05");
assertThat(result.records().get(0).articles())
        .extracting("id")
        .containsExactly(103L, 102L);
assertThat(result.records().get(0).articles().get(0).title())
        .isEqualTo("English Title");
assertThat(result.records().get(0).articles().get(0).summary())
        .isEqualTo("中文概要");
```

- [ ] **Step 2: Run the service test and confirm it fails**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArticleQueryServiceTest#returnsArchiveTimelineGroupedByCurrentPageMonths" test
```

Expected: compile failure or missing `archives(...)` / `PublicArchivePageResult`.

- [ ] **Step 3: Add minimal application result and service method**

Create `PublicArchivePageResult` as records:

```java
public record PublicArchivePageResult(
        List<Group> records,
        long total,
        int page,
        int size) {

    public PublicArchivePageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }

    public record Group(
            String yearMonth,
            int year,
            int month,
            List<Item> articles) {

        public Group {
            articles = articles == null ? List.of() : List.copyOf(articles);
        }
    }

    public record Item(
            long id,
            String title,
            String slug,
            LocalDateTime publishedAt,
            String summary) {
    }
}
```

Add `archives(PublicArticleQuery query)` to `PublicArticleQueryService`:

- call existing `validateQuery(query)`;
- call `repository.findPublicPage(query.toCriteria(now))`;
- normalize `lang`;
- map each current-page `PublicArticlePageItem` to a narrow archive item;
- group with insertion order by `YearMonth.from(item.publishAt())`.

Do not resolve cover URLs. Do not read detail bodies.

- [ ] **Step 4: Run service tests**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArticleQueryServiceTest" test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit backend application contract**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArchivePageResult.java `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleQueryService.java `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/PublicArticleQueryServiceTest.java
git commit -m "后端：增加公开归档时间线应用结果"
```

## Task 2: Backend HTTP API

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArchiveController.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArchivePageVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify tests:
  - `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/PublicArchiveControllerTest.java`
  - `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/ArticleOpenApiTest.java`
  - endpoint config/security tests if exact-list failures appear.

- [ ] **Step 1: Add failing controller test**

Create `PublicArchiveControllerTest` with a mocked `PublicArticleQueryService`.

Key assertions:

```java
mockMvc.perform(get("/api/public/archives")
                .queryParam("page", "2")
                .queryParam("size", "12")
                .queryParam("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].yearMonth")
                .value("2026-06"))
        .andExpect(jsonPath("$.data.records[0].articles[0].id")
                .value("9007199254740993"))
        .andExpect(jsonPath("$.data.records[0].articles[0].title")
                .value("Article"))
        .andExpect(jsonPath("$.data.records[0].articles[0].summary")
                .value("Summary"))
        .andExpect(jsonPath("$.data.records[0].articles[0].publishedAt")
                .value("2026-06-15T10:00:00"))
        .andExpect(jsonPath("$.data.records[0].articles[0].body")
                .doesNotExist())
        .andExpect(jsonPath("$.data.records[0].articles[0].categoryId")
                .doesNotExist())
        .andExpect(jsonPath("$.data.records[0].articles[0].tags")
                .doesNotExist())
        .andExpect(jsonPath("$.data.records[0].articles[0].coverUrl")
                .doesNotExist());
```

Also verify the service receives:

```java
new PublicArticleQuery(2, 12, "en", null, null, null, null, null, null)
```

- [ ] **Step 2: Run controller test and confirm it fails**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArchiveControllerTest" test
```

Expected: missing controller/VO.

- [ ] **Step 3: Implement controller and VO**

Controller:

```java
@RestController
@RequestMapping("/api/public/archives")
@RequiredArgsConstructor
public class PublicArchiveController {

    private final PublicArticleQueryService queryService;
    private final ArticleWebMapping mapping;

    @Operation(summary = "分页查询公开归档时间线")
    @GetMapping
    public ApiResponse<PageResponse<PublicArchivePageVO.Group>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "zh") String lang) {
        return ApiResponse.ok(mapping.toPublicArchivePage(
                queryService.archives(new PublicArticleQuery(
                        page, size, lang, null, null, null, null, null, null))));
    }
}
```

VO:

```java
public record PublicArchivePageVO() {
    public record Group(
            String yearMonth,
            int year,
            int month,
            List<Item> articles) {
    }

    public record Item(
            @JsonSerialize(using = ToStringSerializer.class)
            @Schema(type = "string", format = "int64")
            long id,
            String title,
            String slug,
            LocalDateTime publishedAt,
            String summary) {
    }
}
```

Mapping returns `PageResponse<PublicArchivePageVO.Group>`.

- [ ] **Step 4: Add public endpoint config**

Add to `application.yml` under `myblog.security.public-endpoints`:

```yaml
- method: GET
  path: /api/public/archives
```

If config tests assert exact indexes, update them to include this tuple. Prefer assertions by tuple over fragile numeric indexes if the local test already supports it.

- [ ] **Step 5: Update OpenAPI coverage**

In `ArticleOpenApiTest`:

- assert path `/api/public/archives` has `get`;
- assert `PublicArchivePageVO.Item.id` is string int64;
- assert archive item schema does not expose `body`, `categoryId`, `tags`, `coverUrl`, `commentCount`, `locked`, `status`.

- [ ] **Step 6: Run backend web/config tests**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArchiveControllerTest,ArticleOpenApiTest,ApplicationConfigurationTest,BackendPropertiesTest,SecurityConfigTest" test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit backend HTTP API**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web `
  MyBlog-springboot-v2/src/main/resources/application.yml `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security
git commit -m "后端：公开归档时间线接口"
```

## Task 3: Frontend Archive Data Layer

**Files:**

- Modify: `frontend/apps/blog/src/features/articles/contract.ts`
- Modify: `frontend/apps/blog/src/features/articles/api.ts`
- Modify: `frontend/apps/blog/src/features/articles/model.ts`
- Modify: `frontend/apps/blog/src/features/articles/mapper.ts`
- Modify: `frontend/apps/blog/src/features/articles/store.ts`
- Modify tests:
  - `frontend/apps/blog/src/features/articles/mapper.test.ts`
  - `frontend/apps/blog/src/features/articles/store.test.ts`

- [ ] **Step 1: Add failing mapper test**

Add `maps archive page into timeline groups`.

Expected view model:

```ts
expect(result.records[0]).toEqual({
  yearMonth: '2026-06',
  year: 2026,
  month: 'settings.months[5]',
  posts: [
    {
      id: '9007199254740993',
      slug: 'article',
      title: 'Article',
      text: 'Summary',
      date: {
        month: 'settings.months[5]',
        day: 15,
        year: 2026
      }
    }
  ]
})
```

- [ ] **Step 2: Add failing store test**

Mock `loadPublicArchives`, call:

```ts
await store.loadArchives({ page: 1, size: 12, lang: 'en' })
```

Assert:

```ts
expect(store.archiveStatus).toBe('ready')
expect(store.archive.records[0].posts[0].id).toBe('9007199254740993')
```

Also add an error/retry assertion matching existing `load` and `retry` patterns.

- [ ] **Step 3: Run frontend tests and confirm failure**

Run:

```powershell
pnpm --dir frontend/apps/blog test
```

Expected: missing archive types/functions.

- [ ] **Step 4: Implement archive DTO/API/model/mapper/store**

DTOs:

```ts
export interface PublicArchiveArticleDto {
  id: string
  title: string
  slug: string
  publishedAt: string
  summary: string | null
}

export interface PublicArchiveGroupDto {
  yearMonth: string
  year: number
  month: number
  articles: PublicArchiveArticleDto[]
}
```

API:

```ts
export const loadPublicArchives = async ({
  page,
  size,
  lang,
  signal
}: LoadPublicArchivesParams): Promise<PageResponse<PublicArchiveGroupDto>> => {
  const data = await requestApi<PageResponse<PublicArchiveGroupDto>>({
    method: 'GET',
    url: '/public/archives',
    params: { page, size, lang },
    signal
  })
  if (data === null) throw new ApiError('Archive page response is empty')
  return data
}
```

Mapper:

- Convert backend `month: 6` to `settings.months[5]`.
- Parse `publishedAt` with `parseJst(...)`, not plain `new Date(...)`.
- Preserve `id` for article-detail route.

Store:

- Reuse `ArticleListStatus`.
- Add `archive`, `archiveStatus`, `archiveError`, `loadArchives`, `retryArchives`.
- Use one `AbortController` for active archive request.

- [ ] **Step 5: Run frontend data tests**

Run:

```powershell
pnpm --dir frontend/apps/blog test
```

Expected: `Test Files ... passed`.

- [ ] **Step 6: Commit frontend archive data layer**

```powershell
git add frontend/apps/blog/src/features/articles
git commit -m "前端：增加归档时间线数据层"
```

## Task 4: Frontend Archive Page Migration

**Files:**

- Modify: `frontend/apps/blog/src/pages/archives.vue`

- [ ] **Step 1: Replace old JSON data source**

Remove:

```ts
import { Archives } from '@/models/Post.class'
import { usePostStore } from '@/stores/post'
```

Use:

```ts
import { useArticleStore } from '@/features/articles/store'
import { isSupportedLocale } from '@/shared/i18n/locale'
import { useAppStore } from '@/stores/app'
```

- [ ] **Step 2: Route archive links to ID-led article detail**

Replace old link:

```vue
<router-link :to="{ name: 'post-slug', params: { slug: post.slug } }">
```

with:

```vue
<router-link
  :to="{
    name: 'article-detail',
    params: {
      lang: currentLocale,
      id: post.id,
      slug: post.slug
    }
  }"
>
```

- [ ] **Step 3: Wire loading/error/empty states**

Use existing archive store state:

- `archiveStatus === 'loading'`: show a small timeline skeleton or existing `ob-skeleton`.
- `archiveStatus === 'empty'`: show localized empty text.
- `archiveStatus === 'error'`: show localized error text and retry button.
- `ready`: render `articleStore.archive.records`.

Keep existing timeline CSS. Do not redesign the page.

- [ ] **Step 4: Run frontend verification**

Run:

```powershell
pnpm --dir frontend/apps/blog typecheck
pnpm --dir frontend/apps/blog test
```

Expected: both pass.

- [ ] **Step 5: Commit archive page migration**

```powershell
git add frontend/apps/blog/src/pages/archives.vue frontend/apps/blog/src/features/articles
git commit -m "前端：迁移归档页到V2接口"
```

## Task 5: Docs and Final Verification

**Files:**

- Modify: `docs/handbook/api/article.md`
- Modify: `docs/handbook/frontend/blog/integration-status.md`
- Modify: `docs/handbook/start-here/open-issues.md`
- Modify: `docs/handbook/start-here/current-status.md`

- [ ] **Step 1: Update API docs**

Add section after public article list or before home:

```markdown
## 11. 公开归档时间线

GET /api/public/archives?page=1&size=12&lang=zh

鉴权：匿名。

分页单位是文章数，`total` 是公开可见文章总数，不是月份总数。
```

Document the response fields exactly as in this plan. Also fix public detail example IDs to string if they still show JSON number.

- [ ] **Step 2: Update status docs**

- `frontend/blog/integration-status.md`: move archive from pending to completed.
- `open-issues.md`: close O-016 with implementation and verification notes.
- `current-status.md`: remove archive from Blog remaining list.

- [ ] **Step 3: Run final local checks**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArticleQueryServiceTest,PublicArchiveControllerTest,ArticleOpenApiTest,ApplicationConfigurationTest,BackendPropertiesTest,SecurityConfigTest" test
pnpm --dir frontend/apps/blog typecheck
pnpm --dir frontend/apps/blog test
git diff --stat
git status --short
```

Expected:

- Maven `BUILD SUCCESS`.
- Blog typecheck exits 0.
- Blog test exits 0.
- Diff only contains O-016 backend/frontend/docs files.

- [ ] **Step 4: Commit docs**

```powershell
git add docs/handbook/api/article.md `
  docs/handbook/frontend/blog/integration-status.md `
  docs/handbook/start-here/open-issues.md `
  docs/handbook/start-here/current-status.md
git commit -m "文档：关闭公开归档时间线问题"
```

- [ ] **Step 5: Push and run CI**

```powershell
git push -u origin feature/blog-archive-timeline
gh workflow run CI --ref feature/blog-archive-timeline
gh run watch --exit-status
```

Expected: all CI jobs pass.

## Self-Review

- Spec coverage: O-016 API, backend filtering, page-by-article grouping, frontend archive migration, article-detail route, docs, and verification are covered.
- Placeholder scan: no placeholder markers are used as required steps.
- Type consistency: backend uses `PublicArchivePageResult` -> `PublicArchivePageVO` -> `PageResponse<Group>`; frontend uses `PublicArchiveGroupDto` -> `ArchiveGroupViewModel`.
- Simplification accepted: backend reuses existing public page repository instead of adding archive-specific SQL. This is intentional for the first cut and avoids visibility drift.
