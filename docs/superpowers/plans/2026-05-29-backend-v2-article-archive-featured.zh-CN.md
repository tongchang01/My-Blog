# 后端 V2 文章归档与置顶推荐读取实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐个实现。步骤使用 checkbox（`- [ ]`）语法跟踪状态。

**目标：** 在后端 V2 的 `content` 业务域中补齐读者端文章归档和置顶推荐读取能力。

**架构：** 继续沿用当前 `content` 模块分层：API 只负责 HTTP 契约，application 负责编排，domain 定义只读模型和端口，infrastructure 用 `JdbcTemplate` 兼容旧表。新能力仍然只读取 `t_article`、`t_article_tag`、`t_tag`、`t_category`、`t_user_info`，不修改真实 MySQL 表结构。

**技术栈：** Java 17, Spring Boot 3.5, JdbcTemplate, JUnit 5, AssertJ, MockMvc, H2, MySQL local smoke.

---

## 背景与边界

上一阶段已经完成 `content` 第一批只读能力：

- `GET /api/categories`
- `GET /api/tags`
- `GET /api/tags/top`
- `GET /api/articles`
- `GET /api/categories/{categoryId}/articles`
- `GET /api/tags/{tagId}/articles`
- `GET /api/articles/{articleId}`

本阶段只补两个读者端能力：

- 文章置顶推荐读取。
- 文章归档读取。

旧后端对应接口：

```text
GET /articles/topAndFeatured
GET /archives/all
```

V2 新接口建议：

```text
GET /api/articles/featured
GET /api/articles/archives?page=1&size=10
```

本阶段继续坚持公开内容规则：

```sql
a.is_delete = 0
and a.status = 1
```

这意味着旧接口里会返回的 `status = 2` 密码文章，在 V2 本阶段仍然不返回。密码文章访问流程后续单独做。

## 文件结构

本计划会复用已有 `content` 文件，避免新增不必要的 reader 类。

### 修改生产代码

- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleReader.java`
  - 扩展文章只读端口，新增置顶推荐和归档方法。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/FeaturedArticles.java`
  - 新增领域模型，表达一个置顶文章和最多两个推荐文章。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArchiveMonth.java`
  - 新增领域模型，表达某年某月的归档文章组。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/DatabaseArticleReader.java`
  - 新增 SQL 读取置顶推荐、归档数据。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/application/ContentQueryService.java`
  - 新增应用层方法。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/FeaturedArticlesResponse.java`
  - 新增置顶推荐 API 响应 DTO。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArchiveMonthResponse.java`
  - 新增归档 API 响应 DTO。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ContentArticleController.java`
  - 新增两个 GET 接口。
- `MyBlog-springboot-v2/src/main/resources/application.yml`
  - 新增公开端点。
- `MyBlog-springboot-v2/src/main/resources/application-local.yml`
  - 新增公开端点。
- `MyBlog-springboot-v2/src/test/resources/application-test.yml`
  - 新增公开端点。

### 修改测试代码

- `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`
  - 调整内容测试数据，让归档跨月份，置顶推荐规则可测试。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java`
  - 新增数据库 reader 测试。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentArticleControllerTest.java`
  - 新增 API 测试。

## Task 1: 补强 H2 内容测试数据

**Files:**
- Modify: `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java`

- [ ] **Step 1: 调整测试文章时间和推荐字段**

把测试文章数据调整为：

```sql
insert into t_article (
    id, user_id, category_id, article_cover, article_title, article_abstract,
    article_content, is_top, is_featured, is_delete, status, type, create_time, update_time
)
values
    (1, 1, 1, '/cover/java-1.png', '后端V2第一篇', '摘要一', '正文一', 1, 1, 0, 1, 1, timestamp '2026-05-28 10:00:00', timestamp '2026-05-28 10:00:00'),
    (2, 1, 2, '/cover/life-1.png', '生活记录第一篇', '摘要二', '正文二', 0, 1, 0, 1, 1, timestamp '2026-04-20 11:00:00', timestamp '2026-04-20 11:00:00'),
    (3, 1, 1, '/cover/protected.png', '密码文章', '不应出现在第一阶段', '密码正文', 1, 1, 0, 2, 1, timestamp '2026-03-18 12:00:00', timestamp '2026-03-18 12:00:00'),
    (4, 1, 1, '/cover/draft.png', '草稿文章', '不应出现在第一阶段', '草稿正文', 1, 1, 0, 3, 1, timestamp '2026-02-16 13:00:00', timestamp '2026-02-16 13:00:00'),
    (5, 1, 2, '/cover/deleted.png', '已删除文章', '不应出现在第一阶段', '删除正文', 1, 1, 1, 1, 1, timestamp '2026-01-14 14:00:00', timestamp '2026-01-14 14:00:00');
```

说明：

- `id=1` 是公开置顶文章。
- `id=2` 是公开推荐文章。
- `id=3` 是密码文章，即使置顶推荐字段为 1，也不能出现在 V2 本阶段接口。
- `id=4` 是草稿，即使置顶推荐字段为 1，也不能出现在 V2 本阶段接口。
- `id=5` 是删除文章，即使置顶推荐字段为 1，也不能出现在 V2 本阶段接口。
- 公开文章跨 2026-05 和 2026-04 两个月，归档测试能验证分组排序。

- [ ] **Step 2: 运行现有迁移测试确认数据可加载**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=FlywayMigrationTest'
```

Expected:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 3: 运行现有内容测试确认调整不破坏旧能力**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest,DatabaseContentCatalogReaderTest,ContentCatalogControllerTest'
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 4: 提交**

```powershell
git add MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql
git commit -m "调整后端V2内容归档测试数据"
```

## Task 2: 新增置顶推荐读取

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/FeaturedArticles.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/FeaturedArticlesResponse.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/DatabaseArticleReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/application/ContentQueryService.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ContentArticleController.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentArticleControllerTest.java`

- [ ] **Step 1: 写失败测试**

在 `DatabaseArticleReaderTest` 追加：

```java
@Test
void findsPublishedFeaturedArticlesOnly() {
    var featured = reader.findFeaturedArticles();

    assertThat(featured.topArticle()).isPresent();
    assertThat(featured.topArticle().get().id()).isEqualTo(1);
    assertThat(featured.featuredArticles()).extracting("id").containsExactly(2);
}
```

在 `ContentArticleControllerTest` 追加：

```java
@Test
void returnsFeaturedArticlesWithoutToken() throws Exception {
    mockMvc.perform(get("/api/articles/featured"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.topArticle.id").value(1))
            .andExpect(jsonPath("$.data.featuredArticles.length()").value(1))
            .andExpect(jsonPath("$.data.featuredArticles[0].id").value(2));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

Expected:

```text
Compilation failure: cannot find symbol findFeaturedArticles
```

- [ ] **Step 3: 新增领域模型**

Create `FeaturedArticles.java`:

```java
package com.aurora.myblog.v2.modules.content.domain;

import java.util.List;
import java.util.Optional;

public record FeaturedArticles(Optional<ArticleSummary> topArticle, List<ArticleSummary> featuredArticles) {

    public FeaturedArticles {
        topArticle = topArticle == null ? Optional.empty() : topArticle;
        featuredArticles = featuredArticles == null ? List.of() : List.copyOf(featuredArticles);
    }
}
```

Modify `ArticleReader.java`:

```java
FeaturedArticles findFeaturedArticles();
```

- [ ] **Step 4: 实现数据库读取**

在 `DatabaseArticleReader` 新增：

```java
@Override
public FeaturedArticles findFeaturedArticles() {
    List<Integer> ids = jdbcTemplate.queryForList("""
            select a.id
            from t_article a
            where a.is_delete = 0
              and a.status = 1
              and (a.is_top = 1 or a.is_featured = 1)
            order by a.is_top desc, a.is_featured desc, a.id desc
            limit 3
            """, Integer.class);
    List<ArticleSummary> articles = loadArticleSummaries(ids);
    Optional<ArticleSummary> topArticle = articles.stream()
            .filter(ArticleSummary::top)
            .findFirst();
    List<ArticleSummary> featuredArticles = articles.stream()
            .filter(article -> topArticle.map(top -> top.id() != article.id()).orElse(true))
            .filter(ArticleSummary::featured)
            .limit(2)
            .toList();
    return new FeaturedArticles(topArticle, featuredArticles);
}
```

同时给 `loadArticleSummaries` 加一个按输入 ID 顺序排序的保障。当前 SQL 固定 `order by a.id desc`，对置顶推荐排序不够通用。把 `groupRows(rows)` 后的结果按 `ids` 顺序重排：

```java
private List<ArticleSummary> loadArticleSummaries(List<Integer> ids) {
    if (ids.isEmpty()) {
        return List.of();
    }
    String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
    List<ArticleSummaryRow> rows = jdbcTemplate.query("""
                    ...
                    where a.id in (%s)
                    order by a.id desc, t.id asc
                    """.formatted(placeholders),
            ...,
            ids.toArray());
    List<ArticleSummary> grouped = groupRows(rows);
    Map<Integer, ArticleSummary> byId = grouped.stream()
            .collect(Collectors.toMap(ArticleSummary::id, article -> article));
    return ids.stream()
            .map(byId::get)
            .filter(Objects::nonNull)
            .toList();
}
```

新增 imports：

```java
import java.util.Objects;
import java.util.stream.Collectors;
```

- [ ] **Step 5: 扩展应用服务**

在 `ContentQueryService` 新增：

```java
public FeaturedArticles getFeaturedArticles() {
    return articleReader.findFeaturedArticles();
}
```

新增 import：

```java
import com.aurora.myblog.v2.modules.content.domain.FeaturedArticles;
```

- [ ] **Step 6: 新增 API DTO 和 Controller**

Create `FeaturedArticlesResponse.java`:

```java
package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.FeaturedArticles;

import java.util.List;

public record FeaturedArticlesResponse(
        ArticleSummaryResponse topArticle,
        List<ArticleSummaryResponse> featuredArticles
) {

    static FeaturedArticlesResponse from(FeaturedArticles featuredArticles) {
        return new FeaturedArticlesResponse(
                featuredArticles.topArticle().map(ArticleSummaryResponse::from).orElse(null),
                featuredArticles.featuredArticles().stream()
                        .map(ArticleSummaryResponse::from)
                        .toList());
    }
}
```

在 `ContentArticleController` 新增，必须放在 `/api/articles/{articleId}` 前面，避免路径可读性混淆：

```java
@GetMapping("/api/articles/featured")
public ApiResponse<FeaturedArticlesResponse> getFeaturedArticles() {
    return ApiResponse.ok(FeaturedArticlesResponse.from(contentQueryService.getFeaturedArticles()));
}
```

- [ ] **Step 7: 新增公开端点配置**

在三个配置文件都加入：

```yaml
- /api/articles/featured
```

文件：

```text
MyBlog-springboot-v2/src/main/resources/application.yml
MyBlog-springboot-v2/src/main/resources/application-local.yml
MyBlog-springboot-v2/src/test/resources/application-test.yml
```

- [ ] **Step 8: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 9: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/main/resources/application.yml MyBlog-springboot-v2/src/main/resources/application-local.yml MyBlog-springboot-v2/src/test/resources/application-test.yml
git commit -m "新增后端V2置顶推荐读取接口"
```

## Task 3: 新增文章归档读取

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArchiveMonth.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArchiveMonthResponse.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/DatabaseArticleReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/application/ContentQueryService.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ContentArticleController.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentArticleControllerTest.java`

- [ ] **Step 1: 写失败测试**

在 `DatabaseArticleReaderTest` 追加：

```java
@Test
void listsPublishedArchivesGroupedByMonth() {
    var page = reader.listPublishedArchives(new ArticlePageQuery(1, 10));

    assertThat(page.total()).isEqualTo(2);
    assertThat(page.records()).extracting("month").containsExactly("2026-05", "2026-04");
    assertThat(page.records().get(0).articles()).extracting("id").containsExactly(1);
    assertThat(page.records().get(1).articles()).extracting("id").containsExactly(2);
}
```

在 `ContentArticleControllerTest` 追加：

```java
@Test
void returnsArticleArchivesWithoutToken() throws Exception {
    mockMvc.perform(get("/api/articles/archives?page=1&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.total").value(2))
            .andExpect(jsonPath("$.data.records[0].month").value("2026-05"))
            .andExpect(jsonPath("$.data.records[0].articles[0].id").value(1))
            .andExpect(jsonPath("$.data.records[1].month").value("2026-04"))
            .andExpect(jsonPath("$.data.records[1].articles[0].id").value(2));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

Expected:

```text
Compilation failure: cannot find symbol ArchiveMonth
```

- [ ] **Step 3: 新增领域模型和端口**

Create `ArchiveMonth.java`:

```java
package com.aurora.myblog.v2.modules.content.domain;

import java.util.List;

public record ArchiveMonth(String month, List<ArticleSummary> articles) {

    public ArchiveMonth {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }
}
```

Modify `ArticleReader.java`:

```java
PageResponse<ArchiveMonth> listPublishedArchives(ArticlePageQuery query);
```

- [ ] **Step 4: 实现数据库读取**

在 `DatabaseArticleReader` 新增：

```java
@Override
public PageResponse<ArchiveMonth> listPublishedArchives(ArticlePageQuery query) {
    Long total = jdbcTemplate.queryForObject("""
            select count(distinct date_format(a.create_time, '%Y-%m'))
            from t_article a
            where a.is_delete = 0
              and a.status = 1
            """, Long.class);
    List<String> months = jdbcTemplate.queryForList("""
            select date_format(a.create_time, '%Y-%m') as archive_month
            from t_article a
            where a.is_delete = 0
              and a.status = 1
            group by archive_month
            order by archive_month desc
            limit ? offset ?
            """, String.class, query.size(), query.offset());
    if (months.isEmpty()) {
        return new PageResponse<>(List.of(), total == null ? 0 : total, query.page(), query.size());
    }
    List<ArchiveArticleRow> rows = loadArchiveRows(months);
    Map<String, List<ArticleSummary>> articlesByMonth = rows.stream()
            .collect(Collectors.groupingBy(
                    ArchiveArticleRow::month,
                    LinkedHashMap::new,
                    Collectors.mapping(ArchiveArticleRow::article, Collectors.toList())));
    List<ArchiveMonth> records = months.stream()
            .map(month -> new ArchiveMonth(month, articlesByMonth.getOrDefault(month, List.of())))
            .toList();
    return new PageResponse<>(records, total == null ? 0 : total, query.page(), query.size());
}
```

H2 的 MySQL mode 对 `date_format` 支持有限。如果测试失败，改用兼容表达式：

```sql
formatdatetime(a.create_time, 'yyyy-MM')
```

生产 MySQL 要用 `date_format`。为了兼容两边，推荐在 Java 里按 `create_time` 分组，不在 SQL 中使用数据库特定日期函数。实际实现使用下面这个稳定方案：

```java
@Override
public PageResponse<ArchiveMonth> listPublishedArchives(ArticlePageQuery query) {
    List<ArticleSummary> articles = loadArchiveArticles();
    Map<String, List<ArticleSummary>> grouped = articles.stream()
            .collect(Collectors.groupingBy(
                    article -> "%04d-%02d".formatted(article.createdAt().getYear(), article.createdAt().getMonthValue()),
                    LinkedHashMap::new,
                    Collectors.toList()));
    List<ArchiveMonth> allMonths = grouped.entrySet().stream()
            .map(entry -> new ArchiveMonth(entry.getKey(), entry.getValue()))
            .toList();
    int fromIndex = Math.min(query.offset(), allMonths.size());
    int toIndex = Math.min(fromIndex + query.size(), allMonths.size());
    return new PageResponse<>(allMonths.subList(fromIndex, toIndex), allMonths.size(), query.page(), query.size());
}
```

新增私有方法：

```java
private List<ArticleSummary> loadArchiveArticles() {
    List<Integer> ids = jdbcTemplate.queryForList("""
            select a.id
            from t_article a
            where a.is_delete = 0
              and a.status = 1
            order by a.create_time desc, a.id desc
            """, Integer.class);
    return loadArticleSummaries(ids);
}
```

这个方案对当前个人博客体量足够，避免为了归档先引入数据库方言分支。后续文章量大时再把归档月份分页下推到 SQL。

- [ ] **Step 5: 扩展应用服务**

在 `ContentQueryService` 新增：

```java
public PageResponse<ArchiveMonth> listArchives(Integer page, Integer size) {
    return articleReader.listPublishedArchives(ArticlePageQuery.of(page, size));
}
```

新增 import：

```java
import com.aurora.myblog.v2.modules.content.domain.ArchiveMonth;
```

- [ ] **Step 6: 新增 API DTO 和 Controller**

Create `ArchiveMonthResponse.java`:

```java
package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.ArchiveMonth;

import java.util.List;

public record ArchiveMonthResponse(String month, List<ArticleSummaryResponse> articles) {

    static ArchiveMonthResponse from(ArchiveMonth archiveMonth) {
        return new ArchiveMonthResponse(
                archiveMonth.month(),
                archiveMonth.articles().stream()
                        .map(ArticleSummaryResponse::from)
                        .toList());
    }
}
```

在 `ContentArticleController` 新增，必须放在 `/api/articles/{articleId}` 前面：

```java
@GetMapping("/api/articles/archives")
public ApiResponse<PageResponse<ArchiveMonthResponse>> listArchives(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {
    PageResponse<ArchiveMonth> archives = contentQueryService.listArchives(page, size);
    return ApiResponse.ok(new PageResponse<>(
            archives.records().stream().map(ArchiveMonthResponse::from).toList(),
            archives.total(),
            archives.page(),
            archives.size()));
}
```

新增 import：

```java
import com.aurora.myblog.v2.modules.content.domain.ArchiveMonth;
```

- [ ] **Step 7: 新增公开端点配置**

在三个配置文件都加入：

```yaml
- /api/articles/archives
```

文件：

```text
MyBlog-springboot-v2/src/main/resources/application.yml
MyBlog-springboot-v2/src/main/resources/application-local.yml
MyBlog-springboot-v2/src/test/resources/application-test.yml
```

- [ ] **Step 8: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 9: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/main/resources/application.yml MyBlog-springboot-v2/src/main/resources/application-local.yml MyBlog-springboot-v2/src/test/resources/application-test.yml
git commit -m "新增后端V2文章归档读取接口"
```

## Task 4: 全量验证和计划状态同步

**Files:**
- Modify: `docs/superpowers/plans/2026-05-29-backend-v2-article-archive-featured.zh-CN.md`

- [ ] **Step 1: 全量测试**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
```

Expected:

```text
Tests run: 72 或更多, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 2: 打包**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: 本地 MySQL 只读检查**

不要把本地密码写进文件。先在当前 PowerShell 会话中设置 `MYSQL_PWD`，命令只读取环境变量：

```powershell
if (-not $env:MYSQL_PWD) { throw 'MYSQL_PWD is required' }
mysql -h localhost -P 3306 -u root -N -e "select count(*) from aurora.t_article where is_delete = 0 and status = 1; select count(*) from aurora.t_article where is_delete = 0 and status = 1 and (is_top = 1 or is_featured = 1);"
```

Expected:

```text
第一行是公开文章数量，大于 0。
第二行是公开置顶推荐候选数量，可以为 0，但 SQL 必须执行成功。
```

- [ ] **Step 4: 本地 API 冒烟**

启动服务时通过环境变量注入数据库密码：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
if (-not $env:MYBLOG_DATASOURCE_PASSWORD) { throw 'MYBLOG_DATASOURCE_PASSWORD is required' }
mvn -f MyBlog-springboot-v2/pom.xml spring-boot:run
```

调用：

```powershell
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/articles/featured'
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/articles/archives?page=1&size=10'
```

Expected:

```text
两个接口都返回 success=true。
```

- [ ] **Step 5: 更新本计划的执行结果**

先读取实际提交记录：

```powershell
git log --oneline -4
```

再在本文档末尾追加执行结果。提交记录必须从上面的命令复制实际短 SHA 和提交信息，不手写占位内容：

```markdown
## 执行结果

执行日期：2026-05-29。

### 任务提交记录

- 任务 1：复制 `git log --oneline -4` 中对应的实际提交行。
- 任务 2：复制 `git log --oneline -4` 中对应的实际提交行。
- 任务 3：复制 `git log --oneline -4` 中对应的实际提交行。
- 任务 4：复制 `git log --oneline -4` 中对应的实际提交行。

### 验证结果

- `mvn test`：通过。
- `mvn clean package`：通过。
- 本地 MySQL 只读检查：通过。
- 本地 API 冒烟：通过。

### 未迁移能力

- 密码文章访问流程。
- 搜索接口。
- Redis 浏览量统计。
- 后台文章管理和写操作。
```

- [ ] **Step 6: 提交**

```powershell
git add docs/superpowers/plans/2026-05-29-backend-v2-article-archive-featured.zh-CN.md
git commit -m "同步后端V2归档推荐计划状态"
```

## 自检结果

- 覆盖范围：已覆盖置顶推荐读取、文章归档读取、公开端点配置、H2 测试、本地 MySQL 冒烟和计划状态同步。
- 边界控制：不做密码文章、搜索、浏览量、后台 CRUD、真实表结构调整。
- 类型一致性：计划中新增的 `FeaturedArticles`、`ArchiveMonth`、`ArticleReader` 方法、DTO 和 Controller 方法名称一致。
- 占位扫描：文档没有待实现占位词；本地密码只通过环境变量读取，不能写入代码、文档或 Git。
