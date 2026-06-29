# Backend V2 文章核心纵向切片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在五个可独立验证和提交的批次中完成文章领域、后台管理、公开查询、定时发布、回收站及完整接口契约。

**Architecture:** 继续使用 `web -> application -> domain <- infrastructure`。文章写入通过聚合和引用校验维护状态、分类、标签与附件完整性；后台和公开复杂分页使用独立查询端口；content 只通过 system application 公开能力访问附件，不直接读取 `t_attachment`。

**Tech Stack:** Java 17、Spring Boot 3.5.14、Spring Security 6、MyBatis-Plus 3.5.12、Mapper XML、MapStruct 1.6.3、Lombok、H2、JUnit 5、AssertJ、Mockito、MockMvc、springdoc/Knife4j。

---

## 0. 执行约束

- 设计依据：`docs/superpowers/specs/2026-06-15-backend-v2-article-design.md`。
- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`。
- 不修改冻结的 `MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql`。
- 不使用 `@Select`、`@Insert`、`@Update`、`@Delete` 注解 SQL。
- 不使用 `deleteById`、`removeById` 或物理删除文章。
- DTO、Command、Result、Page、查询条件优先使用 `record`。
- Spring 依赖注入类使用 Lombok `@RequiredArgsConstructor`。
- 生产代码保留必要中文业务注释，不给机械 getter/setter 写无意义注释。
- 所有时间来自注入的 `Clock`，禁止在 application/domain 直接调用系统时间。
- 每个 Task 严格执行 RED -> GREEN -> 定向回归 -> 静态检查 -> 独立中文提交。
- 不使用子代理；按当前会话顺序执行。
- Docker 不作为通过前提，只允许既有 Testcontainers 条件测试在 Docker 不可用时 skipped。

## 1. 文件结构

### 1.1 Task 1 新建

```text
MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/
├── domain/article/
│   ├── Article.java
│   ├── ArticleStatus.java
│   ├── ArticleSlug.java
│   ├── ArticleValidation.java
│   ├── ArticleRepository.java
│   └── NewArticle.java
└── infrastructure/persistence/
    ├── entity/ArticleEntity.java
    ├── mapper/ArticleMapper.java
    ├── mapping/ArticlePersistenceMapping.java
    └── repository/MyBatisArticleRepository.java

MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/
├── AttachmentReferenceResult.java
└── AttachmentReferenceService.java

MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/
├── domain/ArticleDomainTest.java
└── infrastructure/persistence/DatabaseArticleRepositoryTest.java

MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/application/attachment/
└── AttachmentReferenceServiceTest.java
```

修改：

```text
MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/tag/TagRepository.java
MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/TagMapper.java
MyBlog-springboot-v2/src/main/resources/mapper/content/TagMapper.xml
MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/attachment/AttachmentRepository.java
MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence/mapper/AttachmentMapper.java
MyBlog-springboot-v2/src/main/resources/mapper/system/AttachmentMapper.xml
MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence/repository/MyBatisAttachmentRepository.java
```

### 1.2 Task 2 新建

```text
content/application/article/
├── AdminArticleDetailResult.java
├── AdminArticlePageResult.java
├── AdminArticleQuery.java
├── ArticleCreateService.java
├── ArticleQueryService.java
├── ArticleReferenceValidator.java
├── ArticleResult.java
├── ArticleUpdateService.java
├── CreateArticleCommand.java
└── UpdateArticleCommand.java

content/domain/article/
├── AdminArticleDetail.java
├── AdminArticlePage.java
├── AdminArticlePageItem.java
├── AdminArticleQueryRepository.java
└── ArticlePasswordHasher.java

content/infrastructure/security/
└── SpringArticlePasswordHasher.java

content/infrastructure/persistence/projection/
├── AdminArticleDetailRow.java
└── AdminArticlePageRow.java

content/web/
├── AdminArticleController.java
├── AdminArticleDetailVO.java
├── AdminArticlePageItemVO.java
├── ArticleWebMapping.java
├── ArticleWriteOpenApiRequest.java
├── ArticleWriteRequestSupport.java
├── CreateArticleRequest.java
└── UpdateArticleRequest.java

tests:
├── content/application/ArticleWriteServiceTest.java
├── content/application/AdminArticleQueryServiceTest.java
├── content/web/AdminArticleControllerTest.java
└── content/infrastructure/persistence/DatabaseAdminArticleQueryRepositoryTest.java
```

修改：

```text
ArticleMapper.java
ArticleMapper.xml
MyBatisArticleRepository.java
SecurityConfig.java
```

### 1.3 Task 3 新建

```text
content/application/article/
├── PublicArticleDetailResult.java
├── PublicArticlePageResult.java
├── PublicArticleQuery.java
└── PublicArticleQueryService.java

content/domain/article/
├── PublicArticleDetail.java
├── PublicArticlePage.java
├── PublicArticlePageItem.java
├── ArticleTagView.java
└── PublicArticleQueryRepository.java

content/infrastructure/persistence/projection/
├── ArticleTagRow.java
├── PublicArticleDetailRow.java
└── PublicArticlePageRow.java

content/web/
├── PublicArticleController.java
├── PublicArticleDetailVO.java
└── PublicArticlePageItemVO.java

tests:
├── content/application/PublicArticleQueryServiceTest.java
├── content/infrastructure/persistence/DatabasePublicArticleQueryRepositoryTest.java
└── content/web/PublicArticleControllerTest.java
```

修改：

```text
ArticleMapper.java
ArticleMapper.xml
ArticleWebMapping.java
application.yml
application-local.yml
application-test.yml
```

### 1.4 Task 4 新建

```text
content/application/article/
├── ArticleDeleteService.java
├── ArticleRestoreService.java
├── ArticleSchedulePublishService.java
├── DeletedArticlePageResult.java
└── DeletedArticleQueryService.java

content/domain/article/
├── DeletedArticlePage.java
└── DeletedArticlePageItem.java

content/infrastructure/scheduling/
├── ArticlePublishScheduler.java
└── ArticleSchedulingConfiguration.java

tests:
├── content/application/ArticleDeleteRestoreServiceTest.java
├── content/application/ArticleSchedulePublishServiceTest.java
├── content/application/ArticleConcurrencyTest.java
└── content/infrastructure/scheduling/ArticlePublishSchedulerTest.java
```

修改：

```text
Article.java
ArticleRepository.java
AdminArticleQueryRepository.java
ArticleMapper.java
ArticleMapper.xml
MyBatisArticleRepository.java
ArticleQueryService.java
AdminArticleController.java
SecurityConfig.java
application.yml
application-local.yml
application-test.yml
```

### 1.5 Task 5 新建

```text
MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/integration/ArticleIntegrationTest.java
MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/ArticleOpenApiTest.java
docs/project-handbook/api-contract/article.md
```

修改：

```text
docs/project-handbook/api-contract/README.md
docs/project-handbook/product/use-cases.md
docs/project-handbook/roadmap.md
docs/project-handbook/status.md
docs/project-handbook/m3-preflight-review.md
docs/superpowers/specs/2026-06-15-backend-v2-article-design.md
docs/superpowers/plans/2026-06-15-backend-v2-article.md
```

---

## Task 1：建立文章领域模型、持久化基础和附件引用端口

### Files

- Create：Task 1 文件结构列出的生产和测试文件。
- Modify：Task 1 文件结构列出的 Tag 与 Attachment 文件。
- Create：`MyBlog-springboot-v2/src/main/resources/mapper/content/ArticleMapper.xml`

- [ ] **Step 1：先写五态、slug 和标签约束失败测试**

`ArticleDomainTest` 至少覆盖：

```java
@Test
void rejectsPublishedArticleWithoutChineseTitleBodyOrCategory() {
    assertThatThrownBy(() -> NewArticle.create(
            null, null, null,
            null, null, null,
            null, null, 1001L,
            null, ArticleStatus.PUBLISHED,
            null, null, null,
            List.of(), 1001L, NOW))
            .isInstanceOf(IllegalArgumentException.class);
}

@Test
void normalizesOptionalSlugAndRejectsDuplicateTags() {
    assertThat(ArticleSlug.optional(" Spring-JWT ").orElseThrow().value())
            .isEqualTo("spring-jwt");
    assertThatThrownBy(() -> NewArticle.create(
            "文章", null, null,
            null, null, null,
            "正文", 10L, 1001L,
            "article", ArticleStatus.PUBLISHED,
            null, NOW, null,
            List.of(20L, 20L), 1001L, NOW))
            .isInstanceOf(IllegalArgumentException.class);
}

@Test
void requiresPasswordHashAndScheduleTimeForCorrespondingStates() {
    assertThatThrownBy(() -> article(
            ArticleStatus.PASSWORD, null, NOW))
            .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> article(
            ArticleStatus.SCHEDULED, null, null))
            .isInstanceOf(IllegalArgumentException.class);
}

@Test
void reconstitutesDueScheduledArticleForPublisher() {
    Article article = Article.reconstitute(
            10L,
            "文章", null, null,
            null, null, null,
            "正文", 20L, 1001L,
            "article", ArticleStatus.SCHEDULED,
            null, NOW.minusMinutes(1), null,
            0, List.of(), CREATED_AT, 1001L,
            CREATED_AT, 1001L,
            false, null, null);

    assertThat(article.status())
            .isEqualTo(ArticleStatus.SCHEDULED);
    assertThat(article.publishAt())
            .isBefore(NOW);
}
```

- [ ] **Step 2：运行领域测试确认 RED**

```powershell
cd MyBlog-springboot-v2
mvn -Dtest=ArticleDomainTest test
```

Expected：编译失败，`ArticleStatus`、`ArticleSlug`、`NewArticle` 尚不存在。

- [ ] **Step 3：实现状态、slug、Article 和 NewArticle**

`ArticleStatus.java`：

```java
package com.tyb.myblog.v2.content.domain.article;

import java.util.Arrays;

public enum ArticleStatus {
    DRAFT(1),
    PUBLISHED(2),
    PRIVATE(3),
    PASSWORD(4),
    SCHEDULED(5);

    private final int databaseValue;

    ArticleStatus(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public static ArticleStatus fromDatabase(int value) {
        return Arrays.stream(values())
                .filter(status -> status.databaseValue == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "未知文章状态: " + value));
    }
}
```

`ArticleSlug.java`：

```java
package com.tyb.myblog.v2.content.domain.article;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public record ArticleSlug(String value) {
    private static final Pattern PATTERN =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    public static Optional<ArticleSlug> optional(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 160
                || !PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("文章 slug 格式非法");
        }
        return Optional.of(new ArticleSlug(normalized));
    }
}
```

`ArticleValidation` 必须区分持久化重建和写入候选：

```java
static ArticleValues validateStored(ArticleValues values)

static ArticleValues validateForWrite(
        ArticleValues values,
        LocalDateTime now)
```

`validateStored` 校验字段长度、状态组合、分类、密码和发布时间是否存在，
但允许 SCHEDULED 的 `publishAt` 已早于当前时间，否则到期任务无法重建文章。
`validateForWrite` 先调用 `validateStored`，再要求 SCHEDULED 的
`publishAt` 不早于 `now`。

`ArticleValidation.java` 同文件定义包内 record：

```java
record ArticleValues(
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        String body,
        Long categoryId,
        long authorId,
        String slug,
        ArticleStatus status,
        String accessPassword,
        LocalDateTime publishAt,
        Long coverAttachmentId,
        int commentCount,
        List<Long> tagIds) {
}
```

两个入口共同执行以下规则：

```java
if (status != ArticleStatus.DRAFT) {
    requireText(titleZh, 255, "中文标题");
    requireText(body, Integer.MAX_VALUE, "正文");
    requirePositive(categoryId, "分类");
}
if (status == ArticleStatus.PASSWORD && blank(accessPassword)) {
    throw new IllegalArgumentException("密码文章必须保存密码哈希");
}
if (status != ArticleStatus.PASSWORD && accessPassword != null) {
    throw new IllegalArgumentException("非密码文章不得保留密码哈希");
}
if (status == ArticleStatus.SCHEDULED && publishAt == null) {
    throw new IllegalArgumentException("定时文章必须有发布时间");
}
if ((status == ArticleStatus.PUBLISHED
        || status == ArticleStatus.PASSWORD)
        && publishAt == null) {
    throw new IllegalArgumentException("公开文章必须有发布时间");
}
List<Long> normalizedTagIds = validateTagIds(tagIds, 20);
```

`validateForWrite` 额外执行：

```java
if (values.status() == ArticleStatus.SCHEDULED
        && values.publishAt().isBefore(now)) {
    throw new IllegalArgumentException(
            "定时发布时间不得早于当前时间");
}
```

`Article.reconstitute` 接收完整持久化字段并调用 `validateStored`；
`NewArticle.create` 固定 `commentCount=0`，保留 `createdBy` 和
`createdAt`，并调用 `validateForWrite`。`Article.replace` 同样调用
`validateForWrite`。

`Article` 的字段顺序固定为：

```java
public record Article(
        long id,
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        String body,
        Long categoryId,
        long authorId,
        String slug,
        ArticleStatus status,
        String accessPassword,
        LocalDateTime publishAt,
        Long coverAttachmentId,
        int commentCount,
        List<Long> tagIds,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy,
        boolean deleted,
        LocalDateTime deletedAt,
        Long deletedBy) {
}
```

- [ ] **Step 4：写附件引用服务失败测试**

```java
@Test
void locksActiveImageAndRejectsNonImage() {
    when(repository.findActiveByIdForUpdate(10L))
            .thenReturn(Optional.of(attachment(
                    10L, "text/plain", "/files/a.txt")));

    assertThatThrownBy(() ->
            service.requireActiveImageForUpdate(10L))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo(ApiErrorCode.CONFLICT);
}

@Test
void resolvesPublicUrlsInOneBatch() {
    when(repository.findActiveByIds(List.of(10L, 20L)))
            .thenReturn(List.of(
                    attachment(10L, "image/png", "/a.png"),
                    attachment(20L, "image/jpeg", "/b.jpg")));

    assertThat(service.resolvePublicUrls(Set.of(20L, 10L)))
            .containsEntry(10L, "/a.png")
            .containsEntry(20L, "/b.jpg");
}
```

- [ ] **Step 5：扩展附件仓储并实现 application 公开能力**

`AttachmentRepository` 新增：

```java
Optional<Attachment> findActiveByIdForUpdate(long id);

List<Attachment> findActiveByIds(List<Long> ids);
```

`AttachmentReferenceResult.java`：

```java
package com.tyb.myblog.v2.system.application.attachment;

public record AttachmentReferenceResult(
        long id,
        String publicUrl,
        String contentType) {
}
```

`AttachmentReferenceService.java`：

```java
@Service
@RequiredArgsConstructor
public class AttachmentReferenceService {
    private final AttachmentRepository repository;

    public AttachmentReferenceResult requireActiveImageForUpdate(long id) {
        if (id <= 0) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "封面附件 ID 必须为正数");
        }
        Attachment attachment = repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "封面附件不存在"));
        if (!attachment.contentType().startsWith("image/")) {
            throw new ApiException(
                    ApiErrorCode.CONFLICT,
                    "封面附件必须是图片");
        }
        return new AttachmentReferenceResult(
                attachment.id(),
                attachment.publicUrl(),
                attachment.contentType());
    }

    public Map<Long, String> resolvePublicUrls(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        List<Long> normalized = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .sorted()
                .toList();
        return repository.findActiveByIds(normalized).stream()
                .collect(Collectors.toUnmodifiableMap(
                        Attachment::id,
                        Attachment::publicUrl));
    }
}
```

附件 Mapper XML 增加：

```xml
<!-- 文章写入前锁定 active 封面，避免校验后被并发软删除。 -->
<select id="selectActiveByIdForUpdate"
        resultType="com.tyb.myblog.v2.system.infrastructure.persistence.entity.AttachmentEntity">
    SELECT <include refid="attachmentColumns"/>
    FROM t_attachment
    WHERE id = #{id}
      AND deleted = 0
    FOR UPDATE
</select>

<!-- 文章列表批量解析封面 URL，避免逐篇查询。 -->
<select id="selectActiveByIds"
        resultType="com.tyb.myblog.v2.system.infrastructure.persistence.entity.AttachmentEntity">
    SELECT <include refid="attachmentColumns"/>
    FROM t_attachment
    WHERE deleted = 0
      AND id IN
    <foreach collection="ids" item="id"
             open="(" separator="," close=")">
        #{id}
    </foreach>
    ORDER BY id ASC
</select>
```

- [ ] **Step 6：写 Article Repository 失败测试**

覆盖：

- ASSIGN_ID 插入并固定 `comment_count=0`。
- active 详情和 `FOR UPDATE`。
- `t_article_tag` 按标签 ID 升序读取。
- 全量替换标签。
- 未知数据库状态拒绝重建。

关键断言：

```java
Article inserted = repository.insert(newArticle());
assertThat(inserted.id()).isPositive();
assertThat(inserted.commentCount()).isZero();

repository.replaceTags(inserted.id(), List.of(30L, 20L));
assertThat(repository.findActiveById(inserted.id())
        .orElseThrow().tagIds())
        .containsExactly(20L, 30L);
```

- [ ] **Step 7：运行 Repository 测试确认 RED**

```powershell
mvn -Dtest=DatabaseArticleRepositoryTest,AttachmentReferenceServiceTest test
```

Expected：编译失败，文章 Repository 和附件扩展方法尚未实现。

- [ ] **Step 8：实现 Article 持久化**

`ArticleRepository.java`：

```java
public interface ArticleRepository {
    Article insert(NewArticle article);

    Optional<Article> findActiveById(long id);

    Optional<Article> findActiveByIdForUpdate(long id);

    Optional<Article> findDeletedByIdForUpdate(long id);

    boolean update(
            Article article,
            LocalDateTime updatedAt,
            Long updatedBy);

    void replaceTags(long articleId, List<Long> tagIds);
}
```

`ArticleEntity` 继承 `BaseEntity`，字段与冻结 DDL 一一对应：

```java
@Getter
@Setter
@TableName("t_article")
public class ArticleEntity extends BaseEntity {
    private String titleZh;
    private String titleJa;
    private String titleEn;
    private String summaryZh;
    private String summaryJa;
    private String summaryEn;
    private String body;
    private Long categoryId;
    private Long authorId;
    private String slug;
    private Integer status;
    private String accessPassword;
    private LocalDateTime publishAt;
    private Long coverAttachmentId;
    private Integer commentCount;
}
```

`ArticleMapper` 只声明方法，不写注解 SQL：

```java
ArticleEntity selectActiveById(@Param("id") long id);

ArticleEntity selectActiveByIdForUpdate(@Param("id") long id);

ArticleEntity selectDeletedByIdForUpdate(@Param("id") long id);

List<Long> selectTagIds(@Param("articleId") long articleId);

int updateActive(
        @Param("article") ArticleEntity article,
        @Param("updatedAt") LocalDateTime updatedAt,
        @Param("updatedBy") Long updatedBy);

int deleteTagRelations(@Param("articleId") long articleId);

int insertTagRelation(
        @Param("articleId") long articleId,
        @Param("tagId") long tagId);
```

`ArticleMapper.xml` 必须定义完整 `articleColumns`，active/删除锁定查询和完整更新。标签替换 SQL：

```xml
<delete id="deleteTagRelations">
    DELETE FROM t_article_tag
    WHERE article_id = #{articleId}
</delete>

<insert id="insertTagRelation">
    INSERT INTO t_article_tag (article_id, tag_id)
    VALUES (#{articleId}, #{tagId})
</insert>
```

`MyBatisArticleRepository.replaceTags` 固定按升序逐条插入：

```java
@Override
public void replaceTags(long articleId, List<Long> tagIds) {
    mapper.deleteTagRelations(articleId);
    tagIds.stream()
            .sorted()
            .forEach(tagId -> {
                if (mapper.insertTagRelation(articleId, tagId) != 1) {
                    throw new IllegalStateException("文章标签关联写入失败");
                }
            });
}
```

`insert` 在 MyBatis-Plus 回填 ID 后必须使用 `NewArticle.tagIds()` 重建返回值，
不能从不含标签列的 `ArticleEntity` 猜测标签：

```java
@Override
public Article insert(NewArticle article) {
    ArticleEntity entity = mapping.toEntity(article);
    if (mapper.insert(entity) != 1
            || entity.getId() == null
            || entity.getId() <= 0) {
        throw new IllegalStateException("文章写入失败");
    }
    return mapping.toDomain(entity, article.tagIds());
}
```

`ArticlePersistenceMapping.toDomain(ArticleEntity entity, List<Long> tagIds)`
使用 default method 显式解析状态枚举和不可变标签集合，并从 Entity 读取
deleted、deletedAt、deletedBy；禁止让 MapStruct处理状态枚举或标签集合。

- [ ] **Step 9：为标签补充批量升序锁定**

`TagRepository` 新增：

```java
List<Tag> findActiveByIdsForUpdate(List<Long> ids);
```

XML：

```xml
<!-- 文章写入按 ID 升序锁定标签，降低并发死锁风险。 -->
<select id="selectActiveByIdsForUpdate"
        resultType="com.tyb.myblog.v2.content.infrastructure.persistence.entity.TagEntity">
    SELECT <include refid="tagColumns"/>
    FROM t_tag
    WHERE deleted = 0
      AND id IN
    <foreach collection="ids" item="id"
             open="(" separator="," close=")">
        #{id}
    </foreach>
    ORDER BY id ASC
    FOR UPDATE
</select>
```

- [ ] **Step 10：运行 Task 1 测试确认 GREEN**

```powershell
mvn -Dtest=ArticleDomainTest,DatabaseArticleRepositoryTest,AttachmentReferenceServiceTest,DatabaseAttachmentRepositoryTest test
```

Expected：全部 PASS。

- [ ] **Step 11：静态检查并提交 Task 1**

```powershell
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/content src/main/java/com/tyb/myblog/v2/system
git diff --check
git status --short
git add src/main/java/com/tyb/myblog/v2/content `
  src/main/java/com/tyb/myblog/v2/system/application/attachment `
  src/main/java/com/tyb/myblog/v2/system/domain/attachment `
  src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence `
  src/main/resources/mapper/content `
  src/main/resources/mapper/system `
  src/test/java/com/tyb/myblog/v2/content `
  src/test/java/com/tyb/myblog/v2/system
git commit -m "建立文章领域模型与持久化基础"
```

Expected：注解 SQL 无结果；提交只包含 Task 1 文件。

---

## Task 2：实现后台文章查询、创建和完整编辑

### Files

- Create：Task 2 文件结构列出的文件。
- Modify：Task 2 文件结构列出的文件。

- [ ] **Step 1：先写后台写入服务失败测试**

覆盖：

- ADMIN 创建 PUBLISHED 时补当前 JST `publishAt`。
- authorId 来自 principal。
- 分类和标签按升序锁定。
- 封面通过 `AttachmentReferenceService` 校验。
- PASSWORD 创建必须有明文密码并保存哈希。
- PASSWORD 编辑传 `null` 保留原哈希，传新值替换哈希。
- 离开 PASSWORD 清除哈希。
- DEMO 写入返回 403。
- 标签缺失、分类缺失、封面失效返回明确错误。
- 编辑与标签替换同事务。

关键测试：

```java
@Test
void createsPublishedArticleWithCurrentPublishTime() {
    when(categoryRepository.findActiveByIdsForUpdate(List.of(10L)))
            .thenReturn(List.of(category(10L)));
    when(tagRepository.findActiveByIdsForUpdate(List.of(20L, 30L)))
            .thenReturn(List.of(tag(20L), tag(30L)));
    when(articleRepository.insert(any()))
            .thenAnswer(invocation -> articleFrom(
                    invocation.getArgument(0), 100L));

    ArticleResult result = createService.create(
            ADMIN,
            createCommand(
                    ArticleStatus.PUBLISHED,
                    null,
                    null,
                    List.of(30L, 20L)));

    assertThat(result.publishAt()).isEqualTo(NOW);
    assertThat(result.authorId()).isEqualTo(1001L);
    verify(articleRepository).replaceTags(
            100L, List.of(20L, 30L));
}
```

- [ ] **Step 2：运行写入测试确认 RED**

```powershell
mvn -Dtest=ArticleWriteServiceTest test
```

Expected：编译失败，Command、Validator 和 Service 尚不存在。

- [ ] **Step 3：定义 Command、密码端口和引用校验器**

`CreateArticleCommand`：

```java
public record CreateArticleCommand(
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        String body,
        Long categoryId,
        List<Long> tagIds,
        String slug,
        ArticleStatus status,
        String password,
        LocalDateTime publishAt,
        Long coverAttachmentId) {
}
```

`UpdateArticleCommand`：

```java
public record UpdateArticleCommand(
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        String body,
        Long categoryId,
        List<Long> tagIds,
        String slug,
        ArticleStatus status,
        String password,
        LocalDateTime publishAt,
        Long coverAttachmentId) {
}
```

其中 password 的 `null` 语义是保留已有 PASSWORD 哈希。

`ArticleResult` 只承载写操作后的非敏感结果：

```java
public record ArticleResult(
        long id,
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        String body,
        Long categoryId,
        long authorId,
        String slug,
        ArticleStatus status,
        LocalDateTime publishAt,
        Long coverAttachmentId,
        int commentCount,
        List<Long> tagIds,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {

    public static ArticleResult from(Article article) {
        return new ArticleResult(
                article.id(),
                article.titleZh(),
                article.titleJa(),
                article.titleEn(),
                article.summaryZh(),
                article.summaryJa(),
                article.summaryEn(),
                article.body(),
                article.categoryId(),
                article.authorId(),
                article.slug(),
                article.status(),
                article.publishAt(),
                article.coverAttachmentId(),
                article.commentCount(),
                article.tagIds(),
                article.createdAt(),
                article.createdBy(),
                article.updatedAt(),
                article.updatedBy());
    }
}
```

该类型没有 `accessPassword` 字段。

`ArticlePasswordHasher`：

```java
public interface ArticlePasswordHasher {
    String hash(String rawPassword);
}
```

`SpringArticlePasswordHasher`：

```java
@Component
@RequiredArgsConstructor
public class SpringArticlePasswordHasher
        implements ArticlePasswordHasher {
    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()
                || rawPassword.length() > 200) {
            throw new IllegalArgumentException("文章密码格式非法");
        }
        return passwordEncoder.encode(rawPassword);
    }
}
```

`ArticleReferenceValidator`：

```java
@Component
@RequiredArgsConstructor
public class ArticleReferenceValidator {
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final AttachmentReferenceService attachmentService;

    public void lockAndValidate(
            ArticleStatus status,
            Long categoryId,
            List<Long> tagIds,
            Long coverAttachmentId) {
        if (categoryId != null) {
            requireExactCategories(List.of(categoryId));
        } else if (status != ArticleStatus.DRAFT) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "当前文章状态必须选择分类");
        }
        List<Long> normalizedTags = normalizeTagIds(tagIds);
        requireExactTags(normalizedTags);
        if (coverAttachmentId != null) {
            attachmentService.requireActiveImageForUpdate(
                    coverAttachmentId);
        }
    }
}
```

`requireExactCategories` 和 `requireExactTags` 对输入 ID 与锁定结果 ID 做完整相等比较；缺失目标返回 `404 + 90003`。

- [ ] **Step 4：实现创建和编辑服务**

创建服务核心流程：

```java
@Transactional
public ArticleResult create(
        AuthenticatedPrincipal principal,
        CreateArticleCommand command) {
    long actorId = authorization.requireAdmin(principal);
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime publishAt = normalizePublishAt(
            command.status(), command.publishAt(), now);
    String passwordHash = command.status() == ArticleStatus.PASSWORD
            ? passwordHasher.hash(command.password())
            : requireNullPassword(command.password());
    referenceValidator.lockAndValidate(
            command.status(),
            command.categoryId(),
            command.tagIds(),
            command.coverAttachmentId());
    NewArticle candidate = NewArticle.create(
            command.titleZh(), command.titleJa(), command.titleEn(),
            command.summaryZh(), command.summaryJa(), command.summaryEn(),
            command.body(), command.categoryId(), actorId,
            command.slug(), command.status(), passwordHash,
            publishAt, command.coverAttachmentId(),
            command.tagIds(), actorId, now);
    Article inserted = repository.insert(candidate);
    repository.replaceTags(inserted.id(), inserted.tagIds());
    return ArticleResult.from(inserted);
}
```

编辑服务：

```java
@Transactional
public ArticleResult update(
        AuthenticatedPrincipal principal,
        long id,
        UpdateArticleCommand command) {
    long actorId = authorization.requireAdmin(principal);
    Article current = repository.findActiveByIdForUpdate(id)
            .orElseThrow(() -> new ApiException(
                    ApiErrorCode.NOT_FOUND,
                    "文章不存在"));
    LocalDateTime now = LocalDateTime.now(clock);
    String passwordHash = resolvePasswordHash(current, command);
    LocalDateTime publishAt = resolvePublishAt(current, command, now);
    referenceValidator.lockAndValidate(
            command.status(),
            command.categoryId(),
            command.tagIds(),
            command.coverAttachmentId());
    Article replacement = current.replace(
            command.titleZh(), command.titleJa(), command.titleEn(),
            command.summaryZh(), command.summaryJa(), command.summaryEn(),
            command.body(), command.categoryId(), command.slug(),
            command.status(), passwordHash, publishAt,
            command.coverAttachmentId(), command.tagIds(), now);
    if (!repository.update(replacement, now, actorId)) {
        throw new ApiException(ApiErrorCode.CONFLICT);
    }
    repository.replaceTags(id, replacement.tagIds());
    return ArticleResult.from(replacement);
}
```

`resolvePublishAt` 规则：

- PUBLISHED/PASSWORD 且 current.publishAt 非空时保留。
- PUBLISHED/PASSWORD 且历史为空、请求为空时使用 now。
- SCHEDULED 必须使用请求时间并由领域校验不早于 now。
- PRIVATE/DRAFT 接受请求中的 nullable publishAt，保留显式提交值。

- [ ] **Step 5：先写后台查询 Repository 与 Service 失败测试**

后台分页条件：

```java
public record AdminArticleQuery(
        int page,
        int size,
        ArticleStatus status,
        Long categoryId,
        Long tagId,
        String titleKeyword,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        LocalDateTime publishFrom,
        LocalDateTime publishTo) {
}
```

测试必须验证：

- active 文章分页和稳定 `updated_at DESC, id DESC` 排序。
- 状态、分类、标签、标题、创建时间和发布时间筛选。
- 详情包含三语字段、正文、分类、标签和封面 ID。
- 结果不包含密码哈希。
- ADMIN、DEMO 可读。
- page/size 和时间范围非法时 400。

- [ ] **Step 6：实现后台查询投影和服务**

`AdminArticleQueryRepository`：

```java
public interface AdminArticleQueryRepository {
    AdminArticlePage findActivePage(AdminArticleQuery query);

    Optional<AdminArticleDetail> findActiveDetail(long id);
}
```

Mapper XML 分页使用 `EXISTS` 处理标签筛选，避免 join 放大 count：

```xml
<if test="query.tagId != null">
    AND EXISTS (
        SELECT 1
        FROM t_article_tag filter_at
        WHERE filter_at.article_id = a.id
          AND filter_at.tag_id = #{query.tagId}
    )
</if>
```

标题搜索：

```xml
<if test="query.titleKeyword != null">
    AND (
        a.title_zh LIKE CONCAT('%', #{query.titleKeyword}, '%')
        OR a.title_ja LIKE CONCAT('%', #{query.titleKeyword}, '%')
        OR a.title_en LIKE CONCAT('%', #{query.titleKeyword}, '%')
    )
</if>
```

应用服务先执行 `authorization.requireReadable(principal)`，规范化查询后调用端口。详情缺失返回 404。

- [ ] **Step 7：运行 application/repository 测试确认 GREEN**

```powershell
mvn -Dtest=ArticleWriteServiceTest,AdminArticleQueryServiceTest,DatabaseArticleRepositoryTest,DatabaseAdminArticleQueryRepositoryTest test
```

Expected：全部 PASS。

- [ ] **Step 8：先写 Request presence 和 Controller 失败测试**

POST/PUT 必须拒绝：

- 任一字段缺失。
- 未知字段。
- 非法状态名。
- 非法 slug、标签数量、分页和时间范围。
- DEMO 写入。

必须接受可选字段显式 `null`。

测试还要确认 PASSWORD 响应没有 `password` 或 `accessPassword`。

- [ ] **Step 9：实现后台 Request、VO、Mapping 和 Controller**

`ArticleWriteRequestSupport` 使用现有 `content.web.SubmittedField` 记录 14 个字段是否出现。`requireAllFields()` 必须逐项检查：

```java
void requireAllFields() {
    if (!titleZh.submitted()
            || !titleJa.submitted()
            || !titleEn.submitted()
            || !summaryZh.submitted()
            || !summaryJa.submitted()
            || !summaryEn.submitted()
            || !body.submitted()
            || !categoryId.submitted()
            || !tagIds.submitted()
            || !slug.submitted()
            || !status.submitted()
            || !password.submitted()
            || !publishAt.submitted()
            || !coverAttachmentId.submitted()) {
        throw new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                "文章请求字段不完整");
    }
}
```

Controller：

```java
@Tag(name = "后台文章", description = "文章查询、创建与完整编辑")
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {
    private final ArticleQueryService queryService;
    private final ArticleCreateService createService;
    private final ArticleUpdateService updateService;
    private final ArticleWebMapping mapping;

    @GetMapping
    public ApiResponse<PageResponse<AdminArticlePageItemVO>> page(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String titleKeyword,
            @RequestParam(required = false) LocalDateTime createdFrom,
            @RequestParam(required = false) LocalDateTime createdTo,
            @RequestParam(required = false) LocalDateTime publishFrom,
            @RequestParam(required = false) LocalDateTime publishTo) {
        AdminArticlePageResult result = queryService.adminPage(
                principal,
                new AdminArticleQuery(
                        page, size, status, categoryId, tagId,
                        titleKeyword, createdFrom, createdTo,
                        publishFrom, publishTo));
        return ApiResponse.ok(mapping.toAdminPage(result));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<AdminArticleDetailVO> detail(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id) {
        return ApiResponse.ok(mapping.toAdminDetail(
                queryService.adminDetail(principal, id)));
    }

    @PostMapping
    public ApiResponse<AdminArticleDetailVO> create(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestBody CreateArticleRequest request) {
        request.requireAllFields();
        ArticleResult created = createService.create(
                principal, request.toCommand());
        return ApiResponse.ok(mapping.toAdminDetail(
                queryService.adminDetail(
                        principal, created.id())));
    }

    @PutMapping("/{id:\\d+}")
    public ApiResponse<AdminArticleDetailVO> update(
            @CurrentUser AuthenticatedPrincipal principal,
            @PathVariable long id,
            @RequestBody UpdateArticleRequest request) {
        request.requireAllFields();
        ArticleResult updated = updateService.update(
                principal, id, request.toCommand());
        return ApiResponse.ok(mapping.toAdminDetail(
                queryService.adminDetail(
                        principal, updated.id())));
    }
}
```

OpenAPI 使用 `ArticleWriteOpenApiRequest` 声明 14 个 required 字段。
`ArticleWebMapping.toAdminDetail` 只接收
`AdminArticleDetailResult`；写操作完成后必须重新走后台详情查询，
确保响应包含分类、标签名称和封面公开 URL。

- [ ] **Step 10：更新 Security 并运行 Web 测试**

在通用 `/api/admin/**` 规则前加入：

```java
.requestMatchers(
        HttpMethod.GET,
        "/api/admin/articles",
        "/api/admin/articles/*")
.hasAnyRole("ADMIN", "DEMO")
```

运行：

```powershell
mvn -Dtest=AdminArticleControllerTest,ArticleWriteServiceTest,AdminArticleQueryServiceTest,DatabaseAdminArticleQueryRepositoryTest,SecurityConfigTest test
```

Expected：全部 PASS。

- [ ] **Step 11：静态检查并提交 Task 2**

```powershell
rg -n "accessPassword|passwordHash" src/main/java/com/tyb/myblog/v2/content/web src/main/java/com/tyb/myblog/v2/content/application
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/content
git diff --check
git status --short
git add src/main/java/com/tyb/myblog/v2/content `
  src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java `
  src/main/resources/mapper/content `
  src/test/java/com/tyb/myblog/v2/content `
  src/test/java/com/tyb/myblog/v2/common/security
git commit -m "实现后台文章查询与编辑"
```

Expected：web/result 不暴露密码哈希；提交只包含 Task 2。

---

## Task 3：实现公开文章查询、筛选和 PASSWORD 元数据隔离

### Files

- Create：Task 3 文件结构列出的文件。
- Modify：Task 3 文件结构列出的文件。

- [ ] **Step 1：先写公开查询 Repository 失败测试**

准备 PUBLISHED、PASSWORD、DRAFT、PRIVATE、SCHEDULED 和 deleted 数据，验证：

- 列表只返回 PUBLISHED、PASSWORD。
- 按 `publish_at DESC, id DESC`。
- 分类、标签、`yyyy-MM` 归档月份可组合筛选。
- keyword 搜索当前语言 fallback 后的标题和摘要。
- `ja/en` 缺失时 fallback 到中文。
- PASSWORD 列表项 `locked=true` 且没有正文。
- PUBLISHED 详情返回正文。
- PASSWORD 状态探测可被应用层识别，非公开状态不返回详情。

关键断言：

```java
PublicArticlePage page = repository.findPublicPage(
        new PublicArticleQuery(
                ContentLanguage.JA,
                1, 10, categoryId, tagId,
                YearMonth.of(2026, 6), "JWT"));

assertThat(page.records()).extracting("status")
        .containsOnly(
                ArticleStatus.PUBLISHED,
                ArticleStatus.PASSWORD);
assertThat(page.records().get(0).title())
        .isEqualTo("中文回退标题");
```

- [ ] **Step 2：运行 Repository 测试确认 RED**

```powershell
mvn -Dtest=DatabasePublicArticleQueryRepositoryTest test
```

Expected：编译失败，公开查询端口和投影尚不存在。

- [ ] **Step 3：定义公开查询类型和端口**

`PublicArticleQuery`：

```java
public record PublicArticleQuery(
        ContentLanguage language,
        int page,
        int size,
        Long categoryId,
        Long tagId,
        YearMonth archiveMonth,
        String keyword) {

    public static PublicArticleQuery from(
            String lang,
            int page,
            int size,
            Long categoryId,
            Long tagId,
            String archiveMonth,
            String keyword) {
        try {
            ContentLanguage language = ContentLanguage.parse(lang);
            YearMonth month = archiveMonth == null
                    ? null
                    : YearMonth.parse(
                            archiveMonth,
                            DateTimeFormatter.ofPattern("uuuu-MM")
                                    .withResolverStyle(
                                            ResolverStyle.STRICT));
            String normalizedKeyword = keyword == null
                    ? null
                    : keyword.trim();
            if (keyword != null
                    && (normalizedKeyword.isEmpty()
                    || normalizedKeyword.length() > 100)) {
                throw new IllegalArgumentException(
                        "关键字长度非法");
            }
            return new PublicArticleQuery(
                    language, page, size, categoryId, tagId,
                    month, normalizedKeyword);
        } catch (IllegalArgumentException
                 | DateTimeException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "公开文章筛选参数非法");
        }
    }
}
```

`PublicArticleQueryRepository`：

```java
public interface PublicArticleQueryRepository {
    PublicArticlePage findPublicPage(PublicArticleQuery query);

    Optional<ArticleStatus> findActiveStatus(long id);

    Optional<PublicArticleDetail> findPublishedDetail(
            long id,
            ContentLanguage language);
}
```

公开 PageItem 字段：

```java
public record PublicArticlePageItem(
        long id,
        String title,
        String summary,
        String slug,
        ArticleStatus status,
        boolean locked,
        long categoryId,
        String categoryName,
        String categorySlug,
        List<ArticleTagView> tags,
        Long coverAttachmentId,
        LocalDateTime publishAt,
        int commentCount) {
}
```

详情在上述字段基础上增加 `body`，只用于 PUBLISHED。

- [ ] **Step 4：实现公开分页和详情 SQL**

Mapper 先分页查询文章行，再批量查询标签，避免分页被标签 join 放大。

语言字段使用 MyBatis `<choose>`：

```xml
<choose>
    <when test="query.language.name() == 'JA'">
        COALESCE(NULLIF(a.title_ja, ''), a.title_zh)
    </when>
    <when test="query.language.name() == 'EN'">
        COALESCE(NULLIF(a.title_en, ''), a.title_zh)
    </when>
    <otherwise>
        a.title_zh
    </otherwise>
</choose>
AS localized_title
```

公开条件固定：

```sql
a.deleted = 0
AND a.status IN (2, 4)
```

归档月份使用半开区间，由 application 将 `YearMonth` 转为：

```java
LocalDateTime archiveFrom =
        query.archiveMonth().atDay(1).atStartOfDay();
LocalDateTime archiveTo =
        query.archiveMonth().plusMonths(1)
                .atDay(1).atStartOfDay();
```

SQL：

```xml
<if test="query.archiveFrom != null">
    AND a.publish_at &gt;= #{query.archiveFrom}
    AND a.publish_at &lt; #{query.archiveTo}
</if>
```

标签批量查询必须按 `article_id ASC, tag_id ASC` 排序。适配器按分页 ID 原顺序重组，不能依赖 `IN` 返回顺序。

- [ ] **Step 5：先写公开 application 失败测试**

覆盖：

- lang 必填且只允许 zh/ja/en。
- page 1..、size 1..50。
- archiveMonth 严格 `yyyy-MM`。
- keyword 缺失时为 null；参数已提交但 trim 后为空或超过 100 时返回 400。
- 分类和标签 ID 必须为正数。
- 列表批量解析封面 URL。
- PUBLISHED 详情返回。
- PASSWORD 详情返回 `403 + 10003`。
- DRAFT/PRIVATE/SCHEDULED/deleted/不存在统一 `404 + 90003`。

关键测试：

```java
@Test
void rejectsPasswordBodyUntilUnlockSliceExists() {
    when(repository.findActiveStatus(20L))
            .thenReturn(Optional.of(ArticleStatus.PASSWORD));

    assertThatThrownBy(() -> service.detail(
            20L, ContentLanguage.ZH))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo(ApiErrorCode.FORBIDDEN);
}
```

- [ ] **Step 6：实现公开查询服务和封面批量解析**

列表流程：

```java
public PublicArticlePageResult page(PublicArticleQuery query) {
    PublicArticleQuery normalized = normalize(query);
    PublicArticlePage page = repository.findPublicPage(normalized);
    Map<Long, String> coverUrls = attachmentService.resolvePublicUrls(
            page.records().stream()
                    .map(PublicArticlePageItem::coverAttachmentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()));
    return PublicArticlePageResult.from(page, coverUrls);
}
```

详情流程：

```java
public PublicArticleDetailResult detail(
        long id,
        ContentLanguage language) {
    if (id <= 0 || language == null) {
        throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
    }
    ArticleStatus status = repository.findActiveStatus(id)
            .orElseThrow(() -> new ApiException(
                    ApiErrorCode.NOT_FOUND,
                    "文章不存在"));
    if (status == ArticleStatus.PASSWORD) {
        throw new ApiException(
                ApiErrorCode.FORBIDDEN,
                "密码文章尚未解锁");
    }
    if (status != ArticleStatus.PUBLISHED) {
        throw new ApiException(
                ApiErrorCode.NOT_FOUND,
                "文章不存在");
    }
    PublicArticleDetail detail =
            repository.findPublishedDetail(id, language)
                    .orElseThrow(() -> new ApiException(
                            ApiErrorCode.NOT_FOUND,
                            "文章不存在"));
    String coverUrl = detail.coverAttachmentId() == null
            ? null
            : attachmentService.resolvePublicUrls(
                    Set.of(detail.coverAttachmentId()))
                    .get(detail.coverAttachmentId());
    return PublicArticleDetailResult.from(detail, coverUrl);
}
```

- [ ] **Step 7：先写公开 Controller 失败测试**

验证：

```http
GET /api/public/articles?lang=zh&page=1&size=10
GET /api/public/articles/{id}?lang=zh
```

包括：

- 匿名访问。
- 组合筛选参数传递。
- PASSWORD 列表锁标识。
- PASSWORD 详情 403。
- 未公开详情 404。
- 缺少 lang、非法月份、空 keyword、size>50 返回 400。
- 响应不含 body（列表）、authorId、accessPassword 和审计字段。

- [ ] **Step 8：实现公开 Controller、VO 和 Security 白名单**

Controller：

```java
@Tag(name = "公开文章", description = "文章列表、筛选与详情")
@RestController
@RequestMapping("/api/public/articles")
@RequiredArgsConstructor
public class PublicArticleController {
    private final PublicArticleQueryService queryService;
    private final ArticleWebMapping mapping;

    @GetMapping
    public ApiResponse<PageResponse<PublicArticlePageItemVO>> page(
            @RequestParam String lang,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String archiveMonth,
            @RequestParam(required = false) String keyword) {
        PublicArticlePageResult result = queryService.page(
                PublicArticleQuery.from(
                        lang, page, size, categoryId, tagId,
                        archiveMonth, keyword));
        return ApiResponse.ok(mapping.toPublicPage(result));
    }

    @GetMapping("/{id:\\d+}")
    public ApiResponse<PublicArticleDetailVO> detail(
            @PathVariable long id,
            @RequestParam String lang) {
        return ApiResponse.ok(mapping.toPublicDetail(
                queryService.detail(
                        id, ContentLanguage.parse(lang))));
    }
}
```

三个配置文件加入：

```yaml
- method: GET
  path: /api/public/articles
- method: GET
  path: /api/public/articles/*
```

- [ ] **Step 9：运行 Task 3 定向测试**

```powershell
mvn -Dtest=DatabasePublicArticleQueryRepositoryTest,PublicArticleQueryServiceTest,PublicArticleControllerTest,SecurityConfigTest test
```

Expected：全部 PASS。

- [ ] **Step 10：静态检查并提交 Task 3**

```powershell
rg -n "body|authorId|accessPassword|deletedAt|deletedBy" src/main/java/com/tyb/myblog/v2/content/web/PublicArticlePageItemVO.java
rg -n "t_attachment" src/main/resources/mapper/content src/main/java/com/tyb/myblog/v2/content
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/content
git diff --check
git status --short
git add src/main/java/com/tyb/myblog/v2/content `
  src/main/resources/mapper/content `
  src/main/resources/application.yml `
  src/main/resources/application-local.yml `
  src/test/resources/application-test.yml `
  src/test/java/com/tyb/myblog/v2/content `
  src/test/java/com/tyb/myblog/v2/common/security
git commit -m "实现公开文章查询与筛选"
```

Expected：列表 VO 不含禁止字段；content 不读取 `t_attachment`。

---

## Task 4：实现定时发布、软删除、回收站与恢复

### Files

- Create：Task 4 文件结构列出的文件。
- Modify：Task 4 文件结构列出的文件。

- [ ] **Step 1：先写删除和恢复服务失败测试**

覆盖：

- ADMIN 删除 active 文章，完整写入五个审计字段。
- DEMO 删除和恢复返回 403。
- 删除文章不调用 `replaceTags`，标签关联保留。
- 回收站 ADMIN/DEMO 可读，按 `deleted_at DESC, id DESC`。
- 恢复前重新校验分类、标签和封面。
- 引用失效返回 409，不自动清空。
- 恢复清空 deleted 三字段并更新 updated 两字段。

关键测试：

```java
@Test
void rejectsRestoreWhenReferencedTagIsDeleted() {
    Article deleted = deletedArticle(
            10L, List.of(20L));
    when(repository.findDeletedByIdForUpdate(10L))
            .thenReturn(Optional.of(deleted));
    when(tagRepository.findActiveByIdsForUpdate(List.of(20L)))
            .thenReturn(List.of());

    assertThatThrownBy(() ->
            restoreService.restore(ADMIN, 10L))
            .isInstanceOf(ApiException.class)
            .extracting("code")
            .isEqualTo(ApiErrorCode.CONFLICT);
    verify(repository, never()).restore(
            anyLong(), any(), anyLong());
}
```

- [ ] **Step 2：运行删除恢复测试确认 RED**

```powershell
mvn -Dtest=ArticleDeleteRestoreServiceTest test
```

Expected：编译失败，删除、恢复和回收站服务尚不存在。

- [ ] **Step 3：扩展 Repository 并实现删除恢复**

`ArticleRepository` 新增：

```java
boolean softDelete(
        long id,
        LocalDateTime deletedAt,
        long deletedBy);

boolean restore(
        long id,
        LocalDateTime updatedAt,
        long updatedBy);
```

XML：

```xml
<!-- 显式软删除文章，保留 t_article_tag 供回收站恢复。 -->
<update id="softDelete">
    UPDATE t_article
    SET deleted = 1,
        deleted_at = #{deletedAt},
        deleted_by = #{deletedBy},
        updated_at = #{deletedAt},
        updated_by = #{deletedBy}
    WHERE id = #{id}
      AND deleted = 0
</update>

<!-- 恢复前已由应用层重新校验全部逻辑引用。 -->
<update id="restore">
    UPDATE t_article
    SET deleted = 0,
        deleted_at = NULL,
        deleted_by = NULL,
        updated_at = #{updatedAt},
        updated_by = #{updatedBy}
    WHERE id = #{id}
      AND deleted = 1
</update>
```

恢复服务必须捕获引用校验的 404 并转换为 409：

```java
try {
    referenceValidator.lockAndValidate(
            deleted.status(),
            deleted.categoryId(),
            deleted.tagIds(),
            deleted.coverAttachmentId());
} catch (ApiException exception) {
    if (exception.code() == ApiErrorCode.NOT_FOUND) {
        throw new ApiException(
                ApiErrorCode.CONFLICT,
                "文章引用已失效，无法恢复");
    }
    throw exception;
}
```

- [ ] **Step 4：实现回收站查询**

`AdminArticleQueryRepository` 新增：

```java
DeletedArticlePage findDeletedPage(int page, int size);
```

查询固定：

```sql
WHERE a.deleted = 1
ORDER BY a.deleted_at DESC, a.id DESC
LIMIT #{size} OFFSET #{offset}
```

`DeletedArticleQueryService.page` 方法：

- `authorization.requireReadable(principal)`。
- page 最小 1。
- size 范围 1..100。
- 返回文章 ID、三语标题、状态、分类 ID、删除时间和删除人。

- [ ] **Step 5：先写定时发布服务失败测试**

覆盖：

- 只处理到期的 active SCHEDULED。
- 每批最大 50。
- 按 `publishAt ASC, id ASC` 锁定。
- 转为 PUBLISHED，保留 publishAt。
- `updatedBy=null`。
- 条件更新失败返回冲突并回滚。
- 两个并发调度请求不会重复发布同一文章。

测试：

```java
@Test
void publishesDueScheduledArticlesAsSystem() {
    when(repository.findDueScheduledForUpdate(NOW, 50))
            .thenReturn(List.of(scheduled(10L, NOW.minusMinutes(1))));

    int count = service.publishDue();

    assertThat(count).isEqualTo(1);
    verify(repository).updateStatus(
            10L,
            ArticleStatus.SCHEDULED,
            ArticleStatus.PUBLISHED,
            NOW,
            null);
}
```

- [ ] **Step 6：实现定时发布 Repository 和 Service**

`ArticleRepository` 新增：

```java
List<Article> findDueScheduledForUpdate(
        LocalDateTime now,
        int limit);

boolean updateStatus(
        long id,
        ArticleStatus expected,
        ArticleStatus target,
        LocalDateTime updatedAt,
        Long updatedBy);
```

XML：

```xml
<!-- 固定顺序和批量上限，多个实例并发时由行锁串行领取任务。 -->
<select id="selectDueScheduledForUpdate"
        resultType="com.tyb.myblog.v2.content.infrastructure.persistence.entity.ArticleEntity">
    SELECT <include refid="articleColumns"/>
    FROM t_article
    WHERE deleted = 0
      AND status = 5
      AND publish_at &lt;= #{now}
    ORDER BY publish_at ASC, id ASC
    LIMIT #{limit}
    FOR UPDATE
</select>

<update id="updateStatus">
    UPDATE t_article
    SET status = #{target},
        updated_at = #{updatedAt},
        updated_by = #{updatedBy}
    WHERE id = #{id}
      AND deleted = 0
      AND status = #{expected}
</update>
```

Service：

```java
@Service
@RequiredArgsConstructor
public class ArticleSchedulePublishService {
    private static final int BATCH_SIZE = 50;

    private final ArticleRepository repository;
    private final Clock clock;

    @Transactional
    public int publishDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Article> due = repository.findDueScheduledForUpdate(
                now, BATCH_SIZE);
        for (Article article : due) {
            if (!repository.updateStatus(
                    article.id(),
                    ArticleStatus.SCHEDULED,
                    ArticleStatus.PUBLISHED,
                    now,
                    null)) {
                throw new ApiException(ApiErrorCode.CONFLICT);
            }
        }
        return due.size();
    }
}
```

- [ ] **Step 7：实现可配置调度器并关闭测试自动调度**

`ArticleSchedulingConfiguration.java`：

```java
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "myblog.content.article-publish",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ArticleSchedulingConfiguration {
}
```

`ArticlePublishScheduler.java`：

```java
@Component
@RequiredArgsConstructor
public class ArticlePublishScheduler {
    private final ArticleSchedulePublishService service;

    @Scheduled(
            cron = "${myblog.content.article-publish.cron:0 * * * * *}",
            zone = "Asia/Tokyo")
    public void publishDueArticles() {
        service.publishDue();
    }
}
```

`application-test.yml`：

```yaml
myblog:
  content:
    article-publish:
      enabled: false
```

`application.yml` 和 `application-local.yml` 明确：

```yaml
myblog:
  content:
    article-publish:
      enabled: true
      cron: "0 * * * * *"
```

- [ ] **Step 8：扩展后台 Controller**

新增：

```http
DELETE /api/admin/articles/{id}
GET    /api/admin/articles/recycle-bin
POST   /api/admin/articles/{id}/restore
```

Controller 必须先声明静态 `/recycle-bin`，ID 路径继续使用 `/{id:\d+}`。

```java
@GetMapping("/recycle-bin")
public ApiResponse<PageResponse<DeletedArticlePageItemVO>>
        recycleBin(
                @CurrentUser AuthenticatedPrincipal principal,
                @RequestParam(defaultValue = "1") int page,
                @RequestParam(defaultValue = "20") int size) {
    DeletedArticlePageResult result =
            deletedQueryService.page(principal, page, size);
    return ApiResponse.ok(mapping.toDeletedPage(result));
}

@DeleteMapping("/{id:\\d+}")
public ApiResponse<Void> delete(
        @CurrentUser AuthenticatedPrincipal principal,
        @PathVariable long id) {
    deleteService.delete(principal, id);
    return ApiResponse.ok(null);
}

@PostMapping("/{id:\\d+}/restore")
public ApiResponse<AdminArticleDetailVO> restore(
        @CurrentUser AuthenticatedPrincipal principal,
        @PathVariable long id) {
    ArticleResult restored =
            restoreService.restore(principal, id);
    return ApiResponse.ok(mapping.toAdminDetail(
            queryService.adminDetail(
                    principal, restored.id())));
}
```

- [ ] **Step 9：写并发集成测试**

`ArticleConcurrencyTest` 使用两个独立事务和同步闩锁验证：

- 编辑与删除同一文章串行，最终删除状态不被旧编辑覆盖。
- 删除与恢复串行，条件更新不会产生半恢复。
- 两个 publishDue 并发调用，同一 SCHEDULED 最多转一次。
- 编辑标签和删除标签并发时，标签锁定保证引用完整性。

每个并发测试设置 10 秒超时，并在结束后查询数据库最终状态。

- [ ] **Step 10：运行 Task 4 定向测试**

```powershell
mvn -Dtest=ArticleDeleteRestoreServiceTest,ArticleSchedulePublishServiceTest,ArticleConcurrencyTest,ArticlePublishSchedulerTest,AdminArticleControllerTest test
```

Expected：全部 PASS。

- [ ] **Step 11：静态检查并提交 Task 4**

```powershell
rg -n "deleteById|removeById" src/main/java/com/tyb/myblog/v2/content
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/content
rg -n "LocalDateTime\\.now\\(\\)|Instant\\.now\\(\\)" src/main/java/com/tyb/myblog/v2/content
git diff --check
git status --short
git add src/main/java/com/tyb/myblog/v2/content `
  src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java `
  src/main/resources/mapper/content `
  src/main/resources/application.yml `
  src/main/resources/application-local.yml `
  src/test/resources/application-test.yml `
  src/test/java/com/tyb/myblog/v2/content
git commit -m "实现文章定时发布与回收站"
```

Expected：三项静态违规均无结果。

---

## Task 5：完成真实 HTTP、OpenAPI、接口文档和全量收尾

### Files

- Create：Task 5 文件结构列出的文件。
- Modify：Task 5 文件结构列出的文件。

- [ ] **Step 1：编写真实 HTTP 集成测试**

`ArticleIntegrationTest` 使用真实 H2、Security、Mapper XML、PasswordEncoder、JWT 和 MockMvc，覆盖完整流程：

1. 创建 ADMIN、DEMO、分类、标签和 active 图片附件。
2. ADMIN 创建 DRAFT、PUBLISHED、PASSWORD、PRIVATE、SCHEDULED。
3. DEMO 可查看后台列表和详情，但 POST/PUT/DELETE/restore 均 403。
4. PUBLISHED 未传 publishAt 时保存当前 JST。
5. PASSWORD 密码保存为 BCrypt 哈希，API 响应不泄露哈希。
6. 公开列表仅返回 PUBLISHED、PASSWORD，PASSWORD `locked=true`。
7. 公开列表的 ja/en 缺失字段 fallback 到中文。
8. 分类、标签、归档月份和 keyword 组合筛选。
9. PUBLISHED 详情返回正文；PASSWORD 详情 403；其余状态 404。
10. 编辑 PASSWORD 传 null 保留哈希，传新密码替换哈希，离开 PASSWORD 清除哈希。
11. 删除文章后公开和普通后台查询不可见，标签关联仍存在。
12. 回收站 ADMIN/DEMO 可读。
13. 删除引用标签或封面后恢复返回 409。
14. 引用有效时恢复成功并清空删除审计。
15. 到期 SCHEDULED 经 application service 发布后公开可见，保留计划时间且 updated_by 为空。

运行：

```powershell
mvn -Dtest=ArticleIntegrationTest test
```

Expected：PASS。

- [ ] **Step 2：编写 OpenAPI 契约测试**

验证路径方法：

```text
/api/public/articles                 GET
/api/public/articles/{id}            GET
/api/admin/articles                  GET, POST
/api/admin/articles/{id}             GET, PUT, DELETE
/api/admin/articles/recycle-bin      GET
/api/admin/articles/{id}/restore     POST
```

验证 schema：

- `ArticleWriteOpenApiRequest` 14 个字段全部 required。
- 公共列表无 `body`。
- 公共详情有 `body`。
- 后台响应无 `password/accessPassword`。
- 所有响应无 Entity、Mapper、SubmittedField、deleted、deletedAt、deletedBy。

测试核心：

```java
String document = root.toString();
assertThat(document).doesNotContain(
        "ArticleEntity",
        "ArticleMapper",
        "SubmittedField",
        "accessPassword",
        "\"passwordHash\"",
        "\"deleted\"",
        "\"deletedAt\"",
        "\"deletedBy\"");
```

运行：

```powershell
mvn -Dtest=ArticleOpenApiTest test
```

Expected：PASS。

- [ ] **Step 3：编写文章接口契约文档**

`docs/project-handbook/api-contract/article.md` 必须包含：

- 公开和后台权限矩阵。
- 六条后台路径与两条公开路径。
- 分页、筛选、排序、时间和语言规则。
- POST/PUT 完整字段 JSON。
- 五态字段约束表。
- PASSWORD 当前只公开元数据、详情返回 403。
- slug 非唯一、可空、可改和 canonical 规则。
- 软删除、回收站、恢复引用校验。
- 定时发布 JST 语义。
- 400/401/403/404/409/500 错误矩阵。
- 明确本轮不含 article access token、评论授权、置顶推荐和 `comment_enabled`。

更新 `api-contract/README.md`，加入：

```markdown
| `article.md` | 文章五态、后台管理、公开筛选、定时发布与回收站 | ✅ 已落地 |
```

- [ ] **Step 4：同步项目状态文档**

更新：

- `product/use-cases.md`：删除“ADMIN 可以设置是否允许评论”，改为“当前所有满足评论状态条件的文章统一允许评论；逐篇关闭评论不在冻结 schema 中”。
- `roadmap.md`：标记 `t_article/t_article_tag`、slug 查询和五态状态机完成；PASSWORD access token 保持未完成。
- `status.md`：记录文章核心切片能力和下一步 PASSWORD 解锁。
- `m3-preflight-review.md`：新增文章核心纵向切片验收行。
- 设计文档状态改为已实施，并回填五个真实短 SHA。
- 本计划勾选完成项并回填最终测试统计。

- [ ] **Step 5：运行定向契约与集成回归**

```powershell
mvn -Dtest=ArticleIntegrationTest,ArticleOpenApiTest,CategoryTagIntegrationTest,AttachmentIntegrationTest test
```

Expected：全部 PASS。

- [ ] **Step 6：运行全量验证**

```powershell
mvn clean test
```

Expected：

- BUILD SUCCESS。
- 0 failures。
- 0 errors。
- 只允许既有 Docker/Testcontainers 条件测试 skipped。
- Maven Enforcer 和 ArchUnit 通过。

- [ ] **Step 7：执行最终静态审计**

```powershell
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/content
rg -n "deleteById|removeById" src/main/java/com/tyb/myblog/v2/content
rg -n "Entity|Mapper|SubmittedField" src/main/java/com/tyb/myblog/v2/content/application src/main/java/com/tyb/myblog/v2/content/domain
rg -n "t_attachment" src/main/java/com/tyb/myblog/v2/content src/main/resources/mapper/content
rg -n "LocalDateTime\\.now\\(\\)|Instant\\.now\\(\\)" src/main/java/com/tyb/myblog/v2/content
git diff --check
git status --short
git diff --exit-code HEAD -- src/main/resources/db/migration/V1__init.sql
```

Expected：

- 前五条 `rg` 无违规结果。
- `git diff --check` 通过。
- V1 DDL 无差异。
- 工作区只包含 Task 5 测试和文档。

- [ ] **Step 8：提交 Task 5**

```powershell
git add src/test/java/com/tyb/myblog/v2/content `
  docs/project-handbook/api-contract `
  docs/project-handbook/product/use-cases.md `
  docs/project-handbook/roadmap.md `
  docs/project-handbook/status.md `
  docs/project-handbook/m3-preflight-review.md `
  docs/superpowers/specs/2026-06-15-backend-v2-article-design.md `
  docs/superpowers/plans/2026-06-15-backend-v2-article.md
git diff --cached --check
git commit -m "完成文章核心纵向切片"
```

- [ ] **Step 9：确认五批提交和干净工作区**

```powershell
git status --short
git log -7 --oneline
```

Expected：

```text
完成文章核心纵向切片
实现文章定时发布与回收站
实现公开文章查询与筛选
实现后台文章查询与编辑
建立文章领域模型与持久化基础
制定文章核心纵向切片实施计划
设计文章核心纵向切片
```

---

## 2. 最终验收清单

- [ ] Article 聚合不依赖 Spring、MyBatis、Servlet 或 HTTP。
- [ ] 五态、字段组合、slug 和标签数量由统一领域规则约束。
- [ ] PUBLISHED/PASSWORD 首次公开时间和 SCHEDULED 计划时间语义正确。
- [ ] ADMIN、DEMO 后台读权限正确，只有 ADMIN 可写。
- [ ] PASSWORD 明文仅进入 hash 端口，哈希不进入 Result、VO、日志或 OpenAPI。
- [ ] 分类和标签按 ID 升序锁定，封面通过 system application 校验。
- [ ] content 不直接依赖 system domain/infrastructure，不读取 `t_attachment`。
- [ ] 文章创建、编辑和标签替换位于同一事务。
- [ ] 公开列表只返回 PUBLISHED/PASSWORD，PASSWORD 只返回锁定元数据。
- [ ] 公开详情只返回 PUBLISHED 正文，PASSWORD 为 403，其余非公开状态为 404。
- [ ] 分类、标签、归档月份和 keyword 可组合筛选。
- [ ] 公开与后台分页均有稳定排序和上限。
- [ ] 软删除完整写入五个审计字段，并保留标签关联。
- [ ] 回收站只读和恢复；恢复前重新校验全部引用。
- [ ] 定时任务按 JST、固定批次和行锁发布到期文章，updatedBy 为空。
- [ ] 不增加 `comment_enabled`、置顶、推荐、slug history 或物理删除。
- [ ] 所有生产 SQL 位于 Mapper XML，包含必要中文业务注释。
- [ ] 冻结的 V1 DDL 未修改，不新增数据库外键。
- [ ] 五个实施批次分别形成中文本地提交。
- [ ] Maven、Enforcer、ArchUnit、真实 HTTP、OpenAPI 和静态审计全部通过。

---

## 3. 执行结果

截至 2026-06-16，五个批次已按顺序执行完成：

1. `4164560 建立文章领域模型与持久化基础`
2. `1091dbf 实现后台文章查询与编辑`
3. `ff335fa 实现公开文章查询与筛选`
4. `e7a9645 实现文章定时发布与回收站`
5. `本提交 完成文章核心纵向切片`

最终验证：

- 定向契约与集成回归：`mvn "-Dtest=ArticleIntegrationTest,ArticleOpenApiTest,CategoryTagIntegrationTest,AttachmentIntegrationTest" test`，8 tests，0 failures，0 errors，0 skipped。
- 失败项修正后回归：`mvn "-Dtest=BackendPropertiesTest,CategoryTagOpenApiTest,ArticleOpenApiTest" test`，6 tests，0 failures，0 errors，0 skipped。
- 全量验证：`mvn clean test`，534 tests，0 failures，0 errors，4 skipped；Maven Enforcer 与 ArchUnit 通过。
- 静态审计：content 无注解 SQL、无物理删除 helper、无直接系统时间、无 application/domain 内部类型泄露、无直接读取 `t_attachment`。
- `git diff --check` 通过；冻结的 `V1__init.sql` 无差异。

剩余明确延期项：PASSWORD 文章访问 token 完整流程、评论授权、置顶、推荐、slug history 和逐篇评论开关。
