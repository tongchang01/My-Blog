# 前台归档时间线实施计划

> **给执行者：** 实施本计划时必须使用 `superpowers:executing-plans`，按任务逐项执行。步骤使用 `- [ ]` 复选框，执行时逐步更新状态。

**目标：** 新增 V2 公开归档时间线接口，并把前台 Blog 归档页从旧 `/archives/{page}.json` 迁移到 V2 后端。

**架构：** 后端继续放在 content/article 边界内，复用公开文章列表的可见性口径，返回按发布年月分组的窄归档 DTO。前端继续复用 `features/articles`，不单独新建归档 store。

**技术栈：** Spring Boot 3、MyBatis XML、MockMvc、OpenAPI 测试、Vue 3、Pinia、Vitest、pnpm。

---

## 范围

本计划只实现 O-016：公开归档时间线。

本计划不实现搜索、关于、友链、评论、留言、统计、RSS、Sitemap 或 PASSWORD 完整解锁流程；也不新增独立月份索引接口。

旧归档思路里建议归档文章项只返回 `title/slug/publishedAt/summary`，这里需要修正：当前文章详情路由已经是 ID 主导 `/:lang/posts/:id/:slug?`，所以归档文章项必须返回 `id`。否则前端只能退回旧 `post-slug` 路由，和 O-014 的已关闭结论冲突。

## 文件范围

### 后端

- 修改：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleQueryService.java`
  - 新增 `archives(PublicArticleQuery query)`。
  - 复用 `validateQuery(...)`。
  - 查询当前页公开文章后按 `publishAt` 年月分组。
- 新增：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArchivePageResult.java`
  - 应用层归档结果，包含 `records/total/page/size`、年月分组和文章项。
- 新增：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArchiveController.java`
  - 暴露 `GET /api/public/archives?page=1&size=12&lang=zh`。
- 新增：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArchivePageVO.java`
  - HTTP VO，文章 `id` 输出为 JSON string。
- 修改：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`
  - 增加 `PublicArchivePageResult -> PageResponse<PublicArchivePageVO.Group>` 映射。
- 修改：`MyBlog-springboot-v2/src/main/resources/application.yml`
  - 将 `GET /api/public/archives` 加入公开白名单。
- 测试：
  - 修改：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/PublicArticleQueryServiceTest.java`
  - 新增：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/PublicArchiveControllerTest.java`
  - 修改：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/ArticleOpenApiTest.java`
  - 如精确白名单断言失败，再修改：
    - `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/ApplicationConfigurationTest.java`
    - `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/BackendPropertiesTest.java`
    - `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`

第一版不新增 MyBatis 查询。直接复用 `PublicArticleQueryRepository.findPublicPage(...)`，因为它已经覆盖公开口径：`PUBLISHED/PASSWORD + publish_at <= now + deleted=0`，并按 `publish_at DESC, id DESC` 稳定排序。这样少写 SQL，避免公开口径漂移。只有实际压测证明归档页需要更窄查询时，再新增专用 SQL。

### 前端

- 修改：`frontend/apps/blog/src/features/articles/contract.ts`
  - 新增归档 DTO。
- 修改：`frontend/apps/blog/src/features/articles/api.ts`
  - 新增 `loadPublicArchives(...)`。
- 修改：`frontend/apps/blog/src/features/articles/model.ts`
  - 新增归档 ViewModel。
- 修改：`frontend/apps/blog/src/features/articles/mapper.ts`
  - 新增 `mapArchivePage(...)`。
- 修改：`frontend/apps/blog/src/features/articles/store.ts`
  - 新增归档状态、加载和重试逻辑，复用现有 abort/error 模式。
- 修改测试：
  - `frontend/apps/blog/src/features/articles/mapper.test.ts`
  - `frontend/apps/blog/src/features/articles/store.test.ts`
- 修改：`frontend/apps/blog/src/pages/archives.vue`
  - 移除 `usePostStore().fetchArchives(...)` 和旧 `Archives` 模型。
  - 改用 `useArticleStore().loadArchives(...)`。
  - 文章链接改到 `article-detail`，参数为 `{ lang, id, slug }`。

### 文档

- 修改：`docs/handbook/api/article.md`
  - 增加公开归档时间线接口。
  - 如果触碰公开文章详情示例，顺手把示例 ID 修正为 JSON string。
- 修改：`docs/handbook/frontend/blog/integration-status.md`
  - 归档页迁移完成后从待补齐移到已完成。
- 修改：`docs/handbook/start-here/open-issues.md`
  - 关闭 O-016。
- 修改：`docs/handbook/start-here/current-status.md`
  - 归档完成后从前台剩余主链路中移除。

## API 契约

```http
GET /api/public/archives?page=1&size=12&lang=zh
```

成功响应：

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

规则：

- `page` 从 1 开始。
- `size` 范围为 1 到 100；前端默认 12。
- `lang` 支持 `zh/ja/en`；缺失或非法时按公开文章列表规则回退 `zh`。
- `total` 是文章总数，不是月份分组总数。
- 先按文章数分页，再对当前页文章按年月分组。
- 排序为 `publishAt DESC, id DESC`。
- 公开口径和公开文章列表一致：`PUBLISHED` / `PASSWORD`、`publish_at <= now`、未软删除。
- 不返回正文、分类、标签、封面、评论数、状态、锁定标记、密码字段或存储字段。

## 任务 1：后端应用层契约

**文件：**

- 新增：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArchivePageResult.java`
- 修改：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleQueryService.java`
- 修改测试：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/PublicArticleQueryServiceTest.java`

- [ ] **步骤 1：先写失败测试**

在 `PublicArticleQueryServiceTest` 增加：

```java
@Test
void returnsArchiveTimelineGroupedByCurrentPageMonths() {
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
}
```

如现有 `pageItem(...)` helper 不支持自定义 `publishAt`，补一个重载，不改原测试语义。

- [ ] **步骤 2：运行测试确认失败**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArticleQueryServiceTest#returnsArchiveTimelineGroupedByCurrentPageMonths" test
```

预期：缺少 `archives(...)` 或 `PublicArchivePageResult`，测试失败。

- [ ] **步骤 3：实现最小应用层代码**

新增 `PublicArchivePageResult`：

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

在 `PublicArticleQueryService` 增加 `archives(PublicArticleQuery query)`：

- 调用 `validateQuery(query)`。
- 使用 `LocalDateTime.now(clock)`。
- 调用 `repository.findPublicPage(query.toCriteria(now))`。
- 使用现有 `normalizeLang(...)` 和 `localized(...)`。
- 用 `YearMonth.from(item.publishAt())` 按当前页文章分组。
- 不解析封面 URL，不读取正文。

- [ ] **步骤 4：运行应用层测试**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArticleQueryServiceTest" test
```

预期：`BUILD SUCCESS`。

- [ ] **步骤 5：提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArchivePageResult.java `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleQueryService.java `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/PublicArticleQueryServiceTest.java
git commit -m "后端：增加公开归档时间线应用结果"
```

## 任务 2：后端 HTTP API

**文件：**

- 新增：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArchiveController.java`
- 新增：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArchivePageVO.java`
- 修改：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`
- 修改：`MyBlog-springboot-v2/src/main/resources/application.yml`
- 新增测试：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/PublicArchiveControllerTest.java`
- 修改测试：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/ArticleOpenApiTest.java`
- 必要时修改公开白名单配置/安全测试。

- [ ] **步骤 1：先写 Controller 失败测试**

`PublicArchiveControllerTest` 使用 `@WebMvcTest(PublicArchiveController.class)`，mock `PublicArticleQueryService`。

关键断言：

```java
mockMvc.perform(get("/api/public/archives")
                .queryParam("page", "2")
                .queryParam("size", "12")
                .queryParam("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.records[0].yearMonth").value("2026-06"))
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

同时 verify service 收到：

```java
new PublicArticleQuery(2, 12, "en", null, null, null, null, null, null)
```

- [ ] **步骤 2：运行 Controller 测试确认失败**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArchiveControllerTest" test
```

预期：缺少 Controller/VO。

- [ ] **步骤 3：实现 Controller 和 VO**

Controller：

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

VO：

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

`ArticleWebMapping` 返回 `PageResponse<PublicArchivePageVO.Group>`。

- [ ] **步骤 4：加入公开白名单**

在 `application.yml` 的 `myblog.security.public-endpoints` 增加：

```yaml
- method: GET
  path: /api/public/archives
```

如果配置测试依赖精确索引，优先改成 tuple 断言，减少后续插入白名单时的维护成本。

- [ ] **步骤 5：更新 OpenAPI 测试**

在 `ArticleOpenApiTest`：

- 断言 `/api/public/archives` 有 `get`。
- 断言 `PublicArchivePageVO.Item.id` 是 string/int64。
- 断言归档文章项不暴露 `body/categoryId/tags/coverUrl/commentCount/locked/status`。

- [ ] **步骤 6：运行后端 Web/配置测试**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArchiveControllerTest,ArticleOpenApiTest,ApplicationConfigurationTest,BackendPropertiesTest,SecurityConfigTest" test
```

预期：`BUILD SUCCESS`。

- [ ] **步骤 7：提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web `
  MyBlog-springboot-v2/src/main/resources/application.yml `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security
git commit -m "后端：公开归档时间线接口"
```

## 任务 3：前端归档数据层

**文件：**

- 修改：`frontend/apps/blog/src/features/articles/contract.ts`
- 修改：`frontend/apps/blog/src/features/articles/api.ts`
- 修改：`frontend/apps/blog/src/features/articles/model.ts`
- 修改：`frontend/apps/blog/src/features/articles/mapper.ts`
- 修改：`frontend/apps/blog/src/features/articles/store.ts`
- 修改测试：
  - `frontend/apps/blog/src/features/articles/mapper.test.ts`
  - `frontend/apps/blog/src/features/articles/store.test.ts`

- [ ] **步骤 1：先写 mapper 失败测试**

新增 `maps archive page into timeline groups`。

期望结果：

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

- [ ] **步骤 2：先写 store 失败测试**

mock `loadPublicArchives`，执行：

```ts
await store.loadArchives({ page: 1, size: 12, lang: 'en' })
```

断言：

```ts
expect(store.archiveStatus).toBe('ready')
expect(store.archive.records[0].posts[0].id).toBe('9007199254740993')
```

再补一个错误后 `retryArchives()` 的断言，模式参考现有 `retry()` 和 `retryHome()`。

- [ ] **步骤 3：运行前端测试确认失败**

```powershell
pnpm --dir frontend/apps/blog test
```

预期：缺少归档类型或函数。

- [ ] **步骤 4：实现 DTO/API/model/mapper/store**

DTO：

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

API：

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

Mapper：

- 后端 `month: 6` 映射为 `settings.months[5]`。
- `publishedAt` 使用 `parseJst(...)`，不要直接 `new Date(...)`。
- 保留 `id`，供 `article-detail` 路由使用。

Store：

- 复用 `ArticleListStatus`。
- 新增 `archive/archiveStatus/archiveError/loadArchives/retryArchives`。
- 使用独立 `AbortController` 管理归档请求。

- [ ] **步骤 5：运行前端数据层测试**

```powershell
pnpm --dir frontend/apps/blog test
```

预期：测试通过。

- [ ] **步骤 6：提交**

```powershell
git add frontend/apps/blog/src/features/articles
git commit -m "前端：增加归档时间线数据层"
```

## 任务 4：前端归档页迁移

**文件：**

- 修改：`frontend/apps/blog/src/pages/archives.vue`

- [ ] **步骤 1：替换旧数据源**

移除：

```ts
import { Archives } from '@/models/Post.class'
import { usePostStore } from '@/stores/post'
```

改用：

```ts
import { useArticleStore } from '@/features/articles/store'
import { isSupportedLocale } from '@/shared/i18n/locale'
import { useAppStore } from '@/stores/app'
```

- [ ] **步骤 2：文章链接改成 ID 主导详情路由**

旧链接：

```vue
<router-link :to="{ name: 'post-slug', params: { slug: post.slug } }">
```

替换为：

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

- [ ] **步骤 3：补齐 loading/error/empty 状态**

使用 store 状态：

- `archiveStatus === 'loading'`：显示时间线 skeleton 或现有 `ob-skeleton`。
- `archiveStatus === 'empty'`：显示本地化空状态。
- `archiveStatus === 'error'`：显示本地化错误和重试按钮。
- `ready`：渲染 `articleStore.archive.records`。

保留现有时间线 CSS，不重做视觉。

- [ ] **步骤 4：运行前端验证**

```powershell
pnpm --dir frontend/apps/blog typecheck
pnpm --dir frontend/apps/blog test
```

预期：全部通过。

- [ ] **步骤 5：提交**

```powershell
git add frontend/apps/blog/src/pages/archives.vue frontend/apps/blog/src/features/articles
git commit -m "前端：迁移归档页到V2接口"
```

## 任务 5：文档回填和最终验证

**文件：**

- 修改：`docs/handbook/api/article.md`
- 修改：`docs/handbook/frontend/blog/integration-status.md`
- 修改：`docs/handbook/start-here/open-issues.md`
- 修改：`docs/handbook/start-here/current-status.md`

- [ ] **步骤 1：更新 API 文档**

在公开文章列表附近增加：

```markdown
## 公开归档时间线

GET /api/public/archives?page=1&size=12&lang=zh

鉴权：匿名。

分页单位是文章数，`total` 是公开可见文章总数，不是月份总数。
```

响应字段按本计划 API 契约写清楚。若公开详情示例仍显示 JSON number ID，顺手改成 string。

- [ ] **步骤 2：更新状态文档**

- `frontend/blog/integration-status.md`：归档从待补齐移到已完成。
- `open-issues.md`：关闭 O-016，写明验证命令。
- `current-status.md`：从 Blog 剩余主链路里移除归档。

- [ ] **步骤 3：运行最终本地检查**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArticleQueryServiceTest,PublicArchiveControllerTest,ArticleOpenApiTest,ApplicationConfigurationTest,BackendPropertiesTest,SecurityConfigTest" test
pnpm --dir frontend/apps/blog typecheck
pnpm --dir frontend/apps/blog test
git diff --stat
git status --short
```

预期：

- Maven `BUILD SUCCESS`。
- Blog typecheck 退出码 0。
- Blog test 退出码 0。
- diff 只包含 O-016 后端、前端和文档文件。

- [ ] **步骤 4：提交文档**

```powershell
git add docs/handbook/api/article.md `
  docs/handbook/frontend/blog/integration-status.md `
  docs/handbook/start-here/open-issues.md `
  docs/handbook/start-here/current-status.md
git commit -m "文档：关闭公开归档时间线问题"
```

- [ ] **步骤 5：推送并跑 CI**

```powershell
git push -u origin feature/blog-archive-timeline
gh workflow run CI --ref feature/blog-archive-timeline
gh run watch --exit-status
```

预期：CI 全部通过。

## 自检

- 需求覆盖：O-016 API、后端公开过滤、按文章分页后分组、前端归档页迁移、ID 主导详情路由、文档和验证均已覆盖。
- 占位扫描：没有占位标记作为执行步骤。
- 类型一致性：后端 `PublicArchivePageResult -> PublicArchivePageVO -> PageResponse<Group>`；前端 `PublicArchiveGroupDto -> ArchiveGroupViewModel`。
- 简化说明：后端第一版复用公开文章分页 repository，不新增归档专用 SQL；这是为了减少重复公开口径。
