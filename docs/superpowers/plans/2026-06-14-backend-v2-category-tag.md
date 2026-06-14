# Backend V2 分类与标签纵向切片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `content` 模块按五个小批次完成分类和标签的公开读取、后台管理、slug 唯一、分类排序、引用保护软删除及完整契约测试。

**Architecture:** 新建 `content` 的 web、application、domain、infrastructure 四层结构。领域层负责名称、语言和 slug 规则，应用层负责权限、事务、审计、冲突及引用保护，所有 SQL 位于 Mapper XML；MapStruct 1.6.3 只承担无业务判断的机械映射。

**Tech Stack:** Java 17、Spring Boot 3.5.14、Spring Security 6、MyBatis-Plus 3.5.12、MapStruct 1.6.3、Lombok、H2、JUnit 5、AssertJ、MockMvc、springdoc/Knife4j。

---

## 0. 执行约束

- 设计依据：`docs/superpowers/specs/2026-06-14-backend-v2-category-tag-design.md`。
- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`。
- 不修改冻结的 `MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql`。
- 不使用 `@Select`、`@Insert`、`@Update`、`@Delete` 注解 SQL。
- 不使用 `deleteById`、`removeById` 或物理删除。
- DTO、Command、Result 优先使用 `record`；依赖注入使用 Lombok `@RequiredArgsConstructor`。
- 生产代码必须保留必要中文业务注释，禁止给机械 getter/setter 写无意义注释。
- 每个 Task 均执行 RED → GREEN → 定向回归 → `git diff --check` → 独立中文提交。
- Docker 不作为本轮通过前提；仅允许既有 Testcontainers MySQL 条件测试跳过。
- 不使用子代理执行；按用户要求在当前会话顺序实施。

## 1. 文件结构

### 1.1 Task 1 创建

```text
MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/
├── domain/
│   ├── ContentLanguage.java
│   ├── ContentName.java
│   ├── ContentSlug.java
│   ├── ContentSlugConflictException.java
│   ├── category/
│   │   ├── Category.java
│   │   ├── CategoryRepository.java
│   │   └── NewCategory.java
│   └── tag/
│       ├── NewTag.java
│       ├── Tag.java
│       └── TagRepository.java
└── infrastructure/persistence/
    ├── entity/
    │   ├── CategoryEntity.java
    │   └── TagEntity.java
    ├── mapper/
    │   ├── CategoryMapper.java
    │   └── TagMapper.java
    ├── mapping/
    │   ├── CategoryPersistenceMapping.java
    │   └── TagPersistenceMapping.java
    └── repository/
        ├── MyBatisCategoryRepository.java
        └── MyBatisTagRepository.java

MyBlog-springboot-v2/src/main/resources/mapper/content/
├── CategoryMapper.xml
└── TagMapper.xml
```

### 1.2 Task 2 创建

```text
content/application/
├── ContentAuthorization.java
├── category/
│   ├── CategoryQueryService.java
│   ├── CategoryResult.java
│   └── PublicCategoryResult.java
└── tag/
    ├── PublicTagResult.java
    ├── TagQueryService.java
    └── TagResult.java

content/web/
├── AdminCategoryController.java
├── AdminCategoryVO.java
├── AdminTagController.java
├── AdminTagVO.java
├── PublicCategoryController.java
├── PublicCategoryVO.java
├── PublicTagController.java
├── PublicTagVO.java
├── CategoryWebMapping.java
└── TagWebMapping.java
```

### 1.3 Task 3 创建

```text
content/application/category/
├── CategoryCreateService.java
├── CategoryUpdateService.java
├── CreateCategoryCommand.java
└── UpdateCategoryCommand.java

content/application/tag/
├── CreateTagCommand.java
├── TagCreateService.java
├── TagUpdateService.java
└── UpdateTagCommand.java

content/web/
├── CategoryWriteOpenApiRequest.java
├── CategoryWriteRequestSupport.java
├── CreateCategoryRequest.java
├── CreateTagRequest.java
├── TagWriteOpenApiRequest.java
├── TagWriteRequestSupport.java
├── UpdateCategoryRequest.java
└── UpdateTagRequest.java
```

### 1.4 Task 4 创建

```text
content/application/category/
├── CategoryDeleteService.java
├── CategorySortItem.java
├── CategorySortService.java
└── UpdateCategorySortOrdersCommand.java

content/application/tag/
└── TagDeleteService.java

content/web/
└── UpdateCategorySortOrdersRequest.java
```

### 1.5 Task 5 创建

```text
MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/
├── integration/CategoryTagIntegrationTest.java
└── web/CategoryTagOpenApiTest.java

docs/project-handbook/api-contract/category-tag.md
```

---

## Task 1：建立分类标签领域与持久化查询

**Files:**

- Modify: `MyBlog-springboot-v2/pom.xml`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/ContentLanguage.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/ContentName.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/ContentSlug.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/ContentSlugConflictException.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/category/Category.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/category/NewCategory.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/category/CategoryRepository.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/tag/Tag.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/tag/NewTag.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/tag/TagRepository.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/entity/CategoryEntity.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/entity/TagEntity.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/CategoryMapper.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/TagMapper.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapping/CategoryPersistenceMapping.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapping/TagPersistenceMapping.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/repository/MyBatisCategoryRepository.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/repository/MyBatisTagRepository.java`
- Create: `MyBlog-springboot-v2/src/main/resources/mapper/content/CategoryMapper.xml`
- Create: `MyBlog-springboot-v2/src/main/resources/mapper/content/TagMapper.xml`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/domain/CategoryTagDomainTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/infrastructure/persistence/DatabaseCategoryTagRepositoryTest.java`

- [ ] **Step 1：先写领域规则失败测试**

测试必须覆盖：

```java
@Test
void normalizesSlugAndFallsBackToChinese() {
    ContentSlug slug = ContentSlug.of("  Java-Spring  ");
    ContentName name = ContentName.of(" 后端 ", null, " ");

    assertThat(slug.value()).isEqualTo("java-spring");
    assertThat(name.localized(ContentLanguage.ZH)).isEqualTo("后端");
    assertThat(name.localized(ContentLanguage.JA)).isEqualTo("后端");
    assertThat(name.localized(ContentLanguage.EN)).isEqualTo("后端");
}

@ParameterizedTest
@ValueSource(strings = {"", "-java", "java-", "java--spring", "java_spring", "中文"})
void rejectsInvalidSlug(String value) {
    assertThatThrownBy(() -> ContentSlug.of(value))
            .isInstanceOf(IllegalArgumentException.class);
}

@Test
void validatesCategorySortOrder() {
    assertThatThrownBy(() -> NewCategory.create(
            "后端", null, null, "backend", -1, 1001L))
            .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2：运行领域测试确认 RED**

Run:

```powershell
cd MyBlog-springboot-v2
mvn -Dtest=CategoryTagDomainTest test
```

Expected: FAIL，缺少 `ContentSlug`、`ContentName`、`ContentLanguage`、`NewCategory`。

- [ ] **Step 3：实现语言、名称和 slug 值对象**

`ContentLanguage`：

```java
public enum ContentLanguage {
    ZH("zh"), JA("ja"), EN("en");

    private final String code;

    ContentLanguage(String code) {
        this.code = code;
    }

    public static ContentLanguage parse(String value) {
        return Arrays.stream(values())
                .filter(language -> language.code.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的内容语言"));
    }
}
```

`ContentName`：

```java
public record ContentName(String zh, String ja, String en) {
    public static ContentName of(String zh, String ja, String en) {
        String normalizedZh = required(zh, "中文名称", 64);
        return new ContentName(
                normalizedZh,
                optional(ja, "日文名称", 64),
                optional(en, "英文名称", 64));
    }

    public String localized(ContentLanguage language) {
        return switch (language) {
            case ZH -> zh;
            case JA -> ja == null ? zh : ja;
            case EN -> en == null ? zh : en;
        };
    }
}
```

`ContentSlug`：

```java
public record ContentSlug(String value) {
    private static final Pattern PATTERN =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    public static ContentSlug of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("slug 不能为空");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()
                || normalized.length() > 64
                || !PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("slug 格式非法");
        }
        return new ContentSlug(normalized);
    }
}
```

`ContentName` 的 `required/optional` 使用私有静态方法完成 trim、空白转 null 和长度校验。

- [ ] **Step 4：实现分类与标签聚合**

使用不可变 record：

```java
public record Category(
        long id,
        ContentName name,
        ContentSlug slug,
        int sortOrder,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {

    public static Category reconstitute(
            long id,
            String nameZh,
            String nameJa,
            String nameEn,
            String slug,
            int sortOrder,
            LocalDateTime createdAt,
            Long createdBy,
            LocalDateTime updatedAt,
            Long updatedBy) {
        if (id <= 0) {
            throw new IllegalArgumentException("分类 ID 必须为正数");
        }
        validateSortOrder(sortOrder);
        return new Category(
                id,
                ContentName.of(nameZh, nameJa, nameEn),
                ContentSlug.of(slug),
                sortOrder,
                Objects.requireNonNull(createdAt, "分类创建时间不能为空"),
                createdBy,
                Objects.requireNonNull(updatedAt, "分类更新时间不能为空"),
                updatedBy);
    }

    public Category replace(
            String nameZh,
            String nameJa,
            String nameEn,
            String slug,
            int sortOrder) {
        return reconstitute(
                id, nameZh, nameJa, nameEn, slug, sortOrder,
                createdAt, createdBy, updatedAt, updatedBy);
    }
}
```

`Tag` 明确定义为：

```java
public record Tag(
        long id,
        ContentName name,
        ContentSlug slug,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {

    public static Tag reconstitute(
            long id,
            String nameZh,
            String nameJa,
            String nameEn,
            String slug,
            LocalDateTime createdAt,
            Long createdBy,
            LocalDateTime updatedAt,
            Long updatedBy) {
        if (id <= 0) {
            throw new IllegalArgumentException("标签 ID 必须为正数");
        }
        return new Tag(
                id,
                ContentName.of(nameZh, nameJa, nameEn),
                ContentSlug.of(slug),
                Objects.requireNonNull(createdAt, "标签创建时间不能为空"),
                createdBy,
                Objects.requireNonNull(updatedAt, "标签更新时间不能为空"),
                updatedBy);
    }
}
```

创建对象签名固定为：

```java
NewCategory.create(
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        int sortOrder,
        long createdBy)

NewTag.create(
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        long createdBy)
```

两者均要求 `createdBy > 0`。

并发唯一键冲突使用不依赖数据库框架的领域异常：

```java
public class ContentSlugConflictException extends RuntimeException {
    public ContentSlugConflictException() {
        super("slug 已被占用");
    }
}
```

- [ ] **Step 5：运行领域测试确认 GREEN**

Run:

```powershell
mvn -Dtest=CategoryTagDomainTest test
```

Expected: PASS。

- [ ] **Step 6：配置 MapStruct 并增加编译验证**

在 `pom.xml` 增加：

```xml
<mapstruct.version>1.6.3</mapstruct.version>
<lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
```

依赖：

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${mapstruct.version}</version>
</dependency>
```

`maven-compiler-plugin` 的 `annotationProcessorPaths` 按顺序加入：

```xml
<path>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
</path>
<path>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok-mapstruct-binding</artifactId>
    <version>${lombok-mapstruct-binding.version}</version>
</path>
<path>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>${mapstruct.version}</version>
</path>
```

Run:

```powershell
mvn clean compile
```

Expected: BUILD SUCCESS；依赖收敛检查通过。

- [ ] **Step 7：先写 Repository 失败测试**

H2 测试插入分类、标签、软删除记录和引用记录，覆盖：

```java
assertThat(categoryRepository.findAllActive())
        .extracting(category -> category.slug().value())
        .containsExactly("backend", "frontend");

assertThat(categoryRepository.findBySlugIncludingDeleted("old-slug"))
        .isPresent();

assertThat(tagRepository.findAllActive())
        .extracting(tag -> tag.name().zh())
        .containsExactly("Java", "Spring");
```

还要验证：

- active ID 查询。
- `FOR UPDATE` 查询。
- `ASSIGN_ID` 插入。
- 创建/更新审计。
- 已删除数据不进入 active 列表。

- [ ] **Step 8：运行 Repository 测试确认 RED**

Run:

```powershell
mvn -Dtest=DatabaseCategoryTagRepositoryTest test
```

Expected: FAIL，Repository、Entity、Mapper 和 XML 尚不存在。

- [ ] **Step 9：实现 Entity、Mapper 接口和 XML 查询**

Entity：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_category")
public class CategoryEntity extends BaseEntity {
    private String nameZh;
    private String nameJa;
    private String nameEn;
    private String slug;
    private Integer sortOrder;
}
```

`TagEntity` 继承 `BaseEntity`，字段固定为 `nameZh/nameJa/nameEn/slug`，不包含 `sortOrder`。

Mapper 接口只声明 XML 方法：

```java
public interface CategoryMapper extends BaseMapper<CategoryEntity> {
    List<CategoryEntity> selectAllActive();
    CategoryEntity selectActiveById(long id);
    CategoryEntity selectActiveByIdForUpdate(long id);
    List<CategoryEntity> selectActiveByIdsForUpdate(@Param("ids") List<Long> ids);
    CategoryEntity selectBySlugIncludingDeleted(String slug);
}
```

分类 XML 查询：

```xml
<select id="selectAllActive"
        resultType="com.tyb.myblog.v2.content.infrastructure.persistence.entity.CategoryEntity">
  SELECT id, name_zh, name_ja, name_en, slug, sort_order,
         created_at, created_by, updated_at, updated_by,
         deleted, deleted_at, deleted_by
  FROM t_category
  WHERE deleted = 0
  ORDER BY sort_order ASC, id ASC
</select>
```

标签按 `name_zh ASC, id ASC`。`selectBySlugIncludingDeleted` 不增加 `deleted=0`，用于永久唯一检查。

- [ ] **Step 10：实现 MapStruct 持久化映射和 Repository**

映射接口配置：

```java
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryPersistenceMapping {
    // 默认方法显式构造领域对象，确保值对象规则不会被绕过。
    default Category toDomain(CategoryEntity entity) {
        return Category.reconstitute(
                entity.getId(),
                entity.getNameZh(),
                entity.getNameJa(),
                entity.getNameEn(),
                entity.getSlug(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nameZh", source = "name.zh")
    @Mapping(target = "nameJa", source = "name.ja")
    @Mapping(target = "nameEn", source = "name.en")
    @Mapping(target = "slug", source = "slug.value")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", source = "createdBy")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "deletedBy", ignore = true)
    CategoryEntity toEntity(NewCategory source);
}
```

Repository 使用构造器注入 Mapper 和 Mapping：

```java
@Repository
@RequiredArgsConstructor
public class MyBatisCategoryRepository implements CategoryRepository {
    private final CategoryMapper mapper;
    private final CategoryPersistenceMapping mapping;

    @Override
    public List<Category> findAllActive() {
        return mapper.selectAllActive().stream()
                .map(mapping::toDomain)
                .toList();
    }

    @Override
    public Category insert(NewCategory category) {
        CategoryEntity entity = mapping.toEntity(category);
        try {
            if (mapper.insert(entity) != 1
                    || entity.getId() == null
                    || entity.getId() <= 0) {
                throw new IllegalStateException("分类新增失败");
            }
        } catch (DuplicateKeyException exception) {
            throw new ContentSlugConflictException();
        }
        return mapping.toDomain(entity);
    }
}
```

Task 1 的 `CategoryRepository`、`TagRepository` 只声明查询、锁查询和插入方法。更新、排序、引用检查和删除方法在对应 Task 新增，避免接口先于实现膨胀。

- [ ] **Step 11：运行 Task 1 定向测试**

Run:

```powershell
mvn -Dtest=CategoryTagDomainTest,DatabaseCategoryTagRepositoryTest test
```

Expected: PASS。

- [ ] **Step 12：执行静态检查并提交 Task 1**

Run:

```powershell
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/content
git diff --check
git status --short
```

Expected: `rg` 无输出；只包含 Task 1 文件。

Commit:

```powershell
git add MyBlog-springboot-v2/pom.xml `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content `
  MyBlog-springboot-v2/src/main/resources/mapper/content `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/domain `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/infrastructure
git commit -m "建立分类标签领域与持久化查询"
```

---

## Task 2：实现公开与后台分类标签查询

**Files:**

- Create: Task 2 中列出的 application/web 文件
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/CategoryTagQueryServiceTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/PublicCategoryTagControllerTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/AdminCategoryTagControllerTest.java`
- Modify Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`
- Modify Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/ApplicationConfigurationTest.java`

- [ ] **Step 1：先写查询应用服务失败测试**

覆盖：

```java
assertThat(categoryQueryService.publicList("ja"))
        .containsExactly(new PublicCategoryResult(1L, "中文分类", "backend"));

assertThat(tagQueryService.publicList("en"))
        .containsExactly(new PublicTagResult(2L, "Java", "java"));

assertThat(categoryQueryService.adminList(demoPrincipal))
        .hasSize(2);

assertThatThrownBy(() -> tagQueryService.adminDetail(guestPrincipal, 2L))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ApiErrorCode.FORBIDDEN);
```

公开查询传入 `null`、空字符串、`ZH`、`fr` 均映射为 `400 + 90001`。

- [ ] **Step 2：运行应用服务测试确认 RED**

```powershell
mvn -Dtest=CategoryTagQueryServiceTest test
```

Expected: FAIL，查询服务和结果类型不存在。

- [ ] **Step 3：实现查询结果、权限和服务**

结果类型：

```java
public record PublicCategoryResult(long id, String name, String slug) {}

public record CategoryResult(
        long id,
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        int sortOrder,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {}
```

`TagResult` 去掉 `sortOrder`。

查询服务：

```java
@Service
@RequiredArgsConstructor
public class CategoryQueryService {
    private final CategoryRepository repository;

    public List<PublicCategoryResult> publicList(String lang) {
        ContentLanguage language = parseLanguage(lang);
        return repository.findAllActive().stream()
                .map(category -> new PublicCategoryResult(
                        category.id(),
                        category.name().localized(language),
                        category.slug().value()))
                .toList();
    }
}
```

`adminList/adminDetail` 允许 ADMIN、DEMO；不存在返回 `NOT_FOUND`。

- [ ] **Step 4：运行应用服务测试确认 GREEN**

```powershell
mvn -Dtest=CategoryTagQueryServiceTest test
```

Expected: PASS。

- [ ] **Step 5：先写 Controller、Security、YAML 失败测试**

MockMvc 必须覆盖：

- `GET /api/public/categories?lang=ja` 匿名 200。
- `GET /api/public/tags?lang=en` 匿名 200。
- 缺失或非法 `lang` 返回 400。
- ADMIN、DEMO 调后台 GET 返回 200。
- 匿名后台 GET 返回 401。
- POST 到公开路径不允许。
- 公开 schema 不出现 `nameZh`、`sortOrder`、审计字段。

Security 测试明确验证：

```java
mockMvc.perform(get("/api/admin/categories").with(jwtDemo()))
        .andExpect(status().isOk());
mockMvc.perform(post("/api/admin/categories").with(jwtDemo()))
        .andExpect(status().isForbidden());
```

- [ ] **Step 6：运行 Web/Security 测试确认 RED**

```powershell
mvn -Dtest=PublicCategoryTagControllerTest,AdminCategoryTagControllerTest,SecurityConfigTest,ApplicationConfigurationTest test
```

Expected: FAIL，Controller、白名单和 DEMO GET 规则不存在。

- [ ] **Step 7：实现 Web VO、MapStruct Web Mapping 和 Controller**

公开 VO：

```java
public record PublicCategoryVO(long id, String name, String slug) {}
```

Web Mapping：

```java
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CategoryWebMapping {
    PublicCategoryVO toPublicVO(PublicCategoryResult source);
    AdminCategoryVO toAdminVO(CategoryResult source);
}
```

Controller：

```java
@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {
    private final CategoryQueryService service;
    private final CategoryWebMapping mapping;

    @GetMapping
    public ApiResponse<List<PublicCategoryVO>> list(
            @RequestParam String lang) {
        return ApiResponse.ok(service.publicList(lang).stream()
                .map(mapping::toPublicVO)
                .toList());
    }
}
```

后台 Controller 的 GET 列表和详情分别调用 `adminList`、`adminDetail`。

- [ ] **Step 8：更新 Security 和公开端点配置**

`application.yml`、`application-local.yml` 增加：

```yaml
- method: GET
  path: /api/public/categories
- method: GET
  path: /api/public/tags
```

`SecurityConfig` 在 `/api/admin/**` 前增加：

```java
.requestMatchers(
        HttpMethod.GET,
        "/api/admin/categories",
        "/api/admin/categories/*",
        "/api/admin/tags",
        "/api/admin/tags/*")
.hasAnyRole("ADMIN", "DEMO")
```

- [ ] **Step 9：运行 Task 2 定向测试**

```powershell
mvn -Dtest=CategoryTagQueryServiceTest,PublicCategoryTagControllerTest,AdminCategoryTagControllerTest,SecurityConfigTest,ApplicationConfigurationTest test
```

Expected: PASS。

- [ ] **Step 10：检查范围并提交 Task 2**

```powershell
git diff --check
git status --short
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java `
  MyBlog-springboot-v2/src/main/resources/application.yml `
  MyBlog-springboot-v2/src/main/resources/application-local.yml `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common
git commit -m "实现公开与后台分类标签查询"
```

---

## Task 3：实现分类标签新增与编辑

**Files:**

- Create: Task 3 中列出的 application/web 文件
- Modify: `CategoryRepository.java`
- Modify: `TagRepository.java`
- Modify: `CategoryMapper.java`
- Modify: `TagMapper.java`
- Modify: `CategoryMapper.xml`
- Modify: `TagMapper.xml`
- Modify: `MyBatisCategoryRepository.java`
- Modify: `MyBatisTagRepository.java`
- Modify: `AdminCategoryController.java`
- Modify: `AdminTagController.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/CategoryTagWriteServiceTest.java`
- Modify Test: `DatabaseCategoryTagRepositoryTest.java`
- Modify Test: `AdminCategoryTagControllerTest.java`

- [ ] **Step 1：先写新增和编辑失败测试**

必须覆盖：

- ADMIN 可新增。
- DEMO 写入返回 `FORBIDDEN`。
- principal ID 非正数返回 `FORBIDDEN`。
- slug 已被 active 或 deleted 数据占用返回 `CONFLICT`。
- PUT 完整覆盖，可把日英名称显式清空为 null。
- 编辑前锁定 active 行。
- 不存在或已删除目标返回 `NOT_FOUND`。
- 更新行数不是 1 时返回 `INTERNAL_ERROR`。

示例：

```java
assertThatThrownBy(() -> categoryCreateService.create(
        admin, new CreateCategoryCommand(
                "后端", null, null, "existing", 10)))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ApiErrorCode.CONFLICT);
```

- [ ] **Step 2：运行写服务测试确认 RED**

```powershell
mvn -Dtest=CategoryTagWriteServiceTest test
```

Expected: FAIL，Command 和写服务不存在。

- [ ] **Step 3：实现 Command 与新增服务**

Command：

```java
public record CreateCategoryCommand(
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        int sortOrder) {}
```

创建服务事务：

```java
@Transactional
public CategoryResult create(
        AuthenticatedPrincipal principal,
        CreateCategoryCommand command) {
    long actorId = authorization.requireAdmin(principal);
    NewCategory candidate = NewCategory.create(
            command.nameZh(), command.nameJa(), command.nameEn(),
            command.slug(), command.sortOrder(), actorId);
    ensureSlugAvailable(candidate.slug().value(), null);
    try {
        return CategoryResult.from(repository.insert(candidate));
    } catch (ContentSlugConflictException exception) {
        throw new ApiException(ApiErrorCode.CONFLICT);
    }
}
```

`TagCreateService` 使用 `TagRepository.findBySlugIncludingDeleted` 执行相同的永久唯一检查，并捕获 infrastructure 已转换的 `ContentSlugConflictException`，再映射为 `ApiErrorCode.CONFLICT`。application 不直接依赖 `DuplicateKeyException`。

- [ ] **Step 4：实现编辑服务和 XML 完整更新**

编辑服务先锁行，再排除自身检查 slug：

```java
Category current = repository.findActiveByIdForUpdate(id)
        .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
Category replacement = current.replace(
        command.nameZh(),
        command.nameJa(),
        command.nameEn(),
        command.slug(),
        command.sortOrder());
ensureSlugAvailable(replacement.slug().value(), id);
LocalDateTime now = LocalDateTime.now(clock);
if (!repository.update(replacement, now, actorId)) {
    throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
}
```

分类 XML：

```xml
<update id="updateActive">
  UPDATE t_category
  SET name_zh = #{category.nameZh},
      name_ja = #{category.nameJa},
      name_en = #{category.nameEn},
      slug = #{category.slug},
      sort_order = #{category.sortOrder},
      updated_at = #{updatedAt},
      updated_by = #{updatedBy}
  WHERE id = #{category.id}
    AND deleted = 0
</update>
```

标签 XML 不包含排序字段。

- [ ] **Step 5：运行应用和 Repository 测试确认 GREEN**

```powershell
mvn -Dtest=CategoryTagWriteServiceTest,DatabaseCategoryTagRepositoryTest test
```

Expected: PASS。

- [ ] **Step 6：先写 Request presence 和 Controller 失败测试**

POST、PUT 必须拒绝：

- 字段缺失。
- 未知字段。
- 非法 slug。
- 超长名称。
- 分类非法 sortOrder。

必须接受可选名称显式 null。

Request support 使用与友链相同的 presence 设计，但放在 `content.web`，不跨模块复用 `system.web.SubmittedField`。

- [ ] **Step 7：实现 Request、OpenAPI 文档模型和 POST/PUT**

Request support 保存字段值和 `submitted` 标记：

```java
abstract class CategoryWriteRequestSupport {
    private SubmittedField<String> nameZh = SubmittedField.missing();
    private SubmittedField<String> nameJa = SubmittedField.missing();
    private SubmittedField<String> nameEn = SubmittedField.missing();
    private SubmittedField<String> slug = SubmittedField.missing();
    private SubmittedField<Integer> sortOrder = SubmittedField.missing();

    @JsonSetter("nameZh")
    public void setNameZh(String value) {
        this.nameZh = SubmittedField.present(value);
    }

    @JsonSetter("nameJa")
    public void setNameJa(String value) {
        this.nameJa = SubmittedField.present(value);
    }

    @JsonSetter("nameEn")
    public void setNameEn(String value) {
        this.nameEn = SubmittedField.present(value);
    }

    @JsonSetter("slug")
    public void setSlug(String value) {
        this.slug = SubmittedField.present(value);
    }

    @JsonSetter("sortOrder")
    public void setSortOrder(Integer value) {
        this.sortOrder = SubmittedField.present(value);
    }

    void requireAllFields() {
        if (!nameZh.submitted()
                || !nameJa.submitted()
                || !nameEn.submitted()
                || !slug.submitted()
                || !sortOrder.submitted()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR);
        }
    }
}
```

`content.web.SubmittedField` 为包内可见 record，避免依赖 system web。

Controller：

```java
@PostMapping
public ApiResponse<AdminCategoryVO> create(
        @CurrentUser AuthenticatedPrincipal principal,
        @Valid @RequestBody CreateCategoryRequest request) {
    request.requireAllFields();
    return ApiResponse.ok(mapping.toAdminVO(
            createService.create(principal, request.toCommand())));
}
```

PUT 调用相同的 `requireAllFields()`，然后构造 `UpdateCategoryCommand` 或 `UpdateTagCommand`，不提供通用 PATCH。

- [ ] **Step 8：运行 Task 3 定向测试**

```powershell
mvn -Dtest=CategoryTagWriteServiceTest,DatabaseCategoryTagRepositoryTest,AdminCategoryTagControllerTest test
```

Expected: PASS。

- [ ] **Step 9：静态检查并提交 Task 3**

```powershell
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/content
git diff --check
git status --short
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content `
  MyBlog-springboot-v2/src/main/resources/mapper/content `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content
git commit -m "实现分类标签新增与编辑"
```

Expected: 注解 SQL 无结果；只包含 Task 3 文件。

---

## Task 4：实现分类排序与引用保护删除

**Files:**

- Create: Task 4 中列出的 application/web 文件
- Modify: `CategoryRepository.java`
- Modify: `TagRepository.java`
- Modify: `CategoryMapper.java`
- Modify: `TagMapper.java`
- Modify: `CategoryMapper.xml`
- Modify: `TagMapper.xml`
- Modify: `MyBatisCategoryRepository.java`
- Modify: `MyBatisTagRepository.java`
- Modify: `AdminCategoryController.java`
- Modify: `AdminTagController.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/CategorySortDeleteServiceTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/CategoryTagConcurrencyTest.java`
- Modify Test: `DatabaseCategoryTagRepositoryTest.java`
- Modify Test: `AdminCategoryTagControllerTest.java`

- [ ] **Step 1：先写分类排序失败测试**

覆盖：

- 1 到 100 项。
- ID 必须为正数。
- ID 不得重复。
- sortOrder 范围 0 到 1,000,000。
- 按升序 ID 锁定。
- 任一 active 目标缺失整体失败。
- 任一更新行数异常整体回滚。
- 同 sortOrder 允许，由 ID 保证稳定顺序。

- [ ] **Step 2：运行排序测试确认 RED**

```powershell
mvn -Dtest=CategorySortDeleteServiceTest test
```

Expected: FAIL，排序 Command 和 Service 不存在。

- [ ] **Step 3：实现分类排序服务和 XML**

Command：

```java
public record UpdateCategorySortOrdersCommand(
        List<CategorySortItem> items) {}

public record CategorySortItem(long id, int sortOrder) {}
```

事务流程：

```java
@Transactional
public void update(
        AuthenticatedPrincipal principal,
        UpdateCategorySortOrdersCommand command) {
    long actorId = authorization.requireAdmin(principal);
    List<CategorySortItem> items = validate(command);
    List<Long> ids = items.stream()
            .map(CategorySortItem::id)
            .sorted()
            .toList();
    List<Category> locked = repository.findActiveByIdsForUpdate(ids);
    requireExactIds(ids, locked);
    LocalDateTime now = LocalDateTime.now(clock);
    for (CategorySortItem item : items) {
        if (!repository.updateSortOrder(
                item.id(), item.sortOrder(), now, actorId)) {
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
    }
}
```

XML `selectActiveByIdsForUpdate` 必须 `ORDER BY id ASC FOR UPDATE`。

- [ ] **Step 4：先写引用保护删除失败测试**

覆盖：

```java
when(categoryRepository.hasActiveArticleReference(10L))
        .thenReturn(true);

assertThatThrownBy(() -> categoryDeleteService.delete(admin, 10L))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ApiErrorCode.CONFLICT);

verify(categoryRepository, never())
        .softDelete(anyLong(), any(), anyLong());
```

标签测试明确 stub `tagRepository.hasActiveArticleReference(tagId)`，并验证冲突时不调用 `softDelete`。还要验证：

- 无引用时软删除。
- 404、403、非法 actor。
- 删除写入五个审计字段。
- 已删除文章不阻塞分类或标签删除。

- [ ] **Step 5：实现引用查询和显式软删除 XML**

分类引用查询：

```xml
<!-- 删除分类前判断是否仍被未删除文章引用。
     只判断引用完整性，不复制文章公开可见性规则。 -->
<select id="existsActiveArticleReference" resultType="boolean">
  SELECT EXISTS(
    SELECT 1
    FROM t_article
    WHERE category_id = #{categoryId}
      AND deleted = 0
  )
</select>
```

标签引用查询：

```xml
<!-- 删除标签前判断是否仍被未删除文章引用。
     t_article_tag 无软删列，必须联结 t_article 过滤 active 文章。 -->
<select id="existsActiveArticleReference" resultType="boolean">
  SELECT EXISTS(
    SELECT 1
    FROM t_article_tag at
    INNER JOIN t_article a ON a.id = at.article_id
    WHERE at.tag_id = #{tagId}
      AND a.deleted = 0
  )
</select>
```

软删除：

```xml
<update id="softDelete">
  UPDATE t_category
  SET deleted = 1,
      deleted_at = #{deletedAt},
      deleted_by = #{deletedBy},
      updated_at = #{deletedAt},
      updated_by = #{deletedBy}
  WHERE id = #{id}
    AND deleted = 0
</update>
```

`TagMapper.xml` 定义对应 `softDelete`，写入相同五个审计字段并限定 `deleted = 0`。

- [ ] **Step 6：实现删除服务**

```java
@Transactional
public void delete(AuthenticatedPrincipal principal, long id) {
    long actorId = authorization.requireAdmin(principal);
    repository.findActiveByIdForUpdate(id)
            .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND));
    if (repository.hasActiveArticleReference(id)) {
        throw new ApiException(ApiErrorCode.CONFLICT);
    }
    LocalDateTime now = LocalDateTime.now(clock);
    if (!repository.softDelete(id, now, actorId)) {
        throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
```

- [ ] **Step 7：实现排序/删除 Controller 并写 Web 测试**

分类：

```http
PUT /api/admin/categories/sort-orders
DELETE /api/admin/categories/{id}
```

标签：

```http
DELETE /api/admin/tags/{id}
```

排序 Request：

```json
{
  "items": [
    {"id": 101, "sortOrder": 0},
    {"id": 102, "sortOrder": 10}
  ]
}
```

确认静态 `/sort-orders` 路径不会被 `/{id}` 错误解析。

- [ ] **Step 8：写并发集成测试**

使用 H2 两个独立事务和同步闩锁验证：

- 编辑与删除同一分类串行，最终删除优先。
- 排序与删除同一分类时，删除导致排序整体失败或排序先完成后再删除，不出现部分排序。
- 两个并发创建使用同一 slug 时最多一个成功，另一个稳定为冲突。

并发测试必须设置超时，执行结束后校验数据库最终状态。

- [ ] **Step 9：运行 Task 4 定向测试**

```powershell
mvn -Dtest=CategorySortDeleteServiceTest,CategoryTagConcurrencyTest,DatabaseCategoryTagRepositoryTest,AdminCategoryTagControllerTest test
```

Expected: PASS。

- [ ] **Step 10：静态检查并提交 Task 4**

```powershell
rg -n "deleteById|removeById" src/main/java/com/tyb/myblog/v2/content
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/content
git diff --check
git status --short
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content `
  MyBlog-springboot-v2/src/main/resources/mapper/content `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content
git commit -m "实现分类排序与引用保护删除"
```

Expected: 两条 `rg` 均无输出。

---

## Task 5：完成集成测试、OpenAPI 契约和文档收尾

**Files:**

- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/integration/CategoryTagIntegrationTest.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/CategoryTagOpenApiTest.java`
- Create: `docs/project-handbook/api-contract/category-tag.md`
- Modify: `docs/project-handbook/api-contract/README.md`
- Modify: `docs/project-handbook/roadmap.md`
- Modify: `docs/project-handbook/status.md`
- Modify: `docs/project-handbook/m3-preflight-review.md`
- Modify: `docs/superpowers/specs/2026-06-14-backend-v2-category-tag-design.md`
- Modify: `docs/superpowers/plans/2026-06-14-backend-v2-category-tag.md`

- [ ] **Step 1：编写真实 HTTP 集成测试**

使用真实 H2、Security、Mapper XML 和事务，覆盖完整流程：

1. ADMIN 新增分类和标签。
2. DEMO 可读取后台列表但不能写。
3. 公开接口按 `zh/ja/en` 返回正确名称和中文 fallback。
4. ADMIN 完整编辑并清空可选语言。
5. 分类批量排序后公开顺序变化。
6. active 文章引用阻止分类和标签删除。
7. 将引用文章软删除后，分类和标签可删除。
8. 删除后公开和后台查询均不可见。
9. 删除后的 slug 仍不能重新创建。

测试 SQL 直接准备引用文章时必须填写 `t_article` 的所有必填列，不新增生产文章代码。

- [ ] **Step 2：运行集成测试确认契约**

```powershell
mvn -Dtest=CategoryTagIntegrationTest test
```

Expected: PASS。

- [ ] **Step 3：编写 OpenAPI 契约测试**

验证：

- `/api/public/categories`、`/api/public/tags` 只有 GET。
- 后台集合路径只有 GET、POST。
- `/{id}` 只有 GET、PUT、DELETE。
- 分类 `/sort-orders` 只有 PUT。
- 标签没有排序路径。
- 公开 schema 只有 `id/name/slug`。
- 后台 schema 不包含 `deleted/deletedAt/deletedBy`。
- Request schema 字段 required 与 presence 规则一致。
- OpenAPI 不出现 Entity、Mapper、`ContentName`、`ContentSlug`。

- [ ] **Step 4：运行 OpenAPI 测试**

```powershell
mvn -Dtest=CategoryTagOpenApiTest test
```

Expected: PASS。

- [ ] **Step 5：编写接口契约文档**

`category-tag.md` 必须包含：

- 权限矩阵。
- 公开和后台路径。
- 三语 fallback。
- POST/PUT 完整字段示例。
- slug 规则和永久唯一语义。
- 分类排序请求。
- 引用保护软删除。
- 400/401/403/404/409/500 错误语义。
- 明确不返回文章数。

更新 `api-contract/README.md` 索引。

- [ ] **Step 6：更新状态、路线图和设计实施记录**

更新内容：

- `roadmap.md` 将 content 的分类/标签子切片标记完成，文章仍未完成。
- `status.md` 记录分类标签能力和下一步进入文章纵向切片设计。
- `m3-preflight-review.md` 增加本轮验收记录。
- 设计文档状态改为已实施，并回填五个真实短 SHA。
- 本计划勾选完成项并回填最终测试统计。

- [ ] **Step 7：运行全量验证**

Run:

```powershell
cd MyBlog-springboot-v2
mvn clean test
```

Expected:

- BUILD SUCCESS。
- 0 failures。
- 0 errors。
- 仅既有 Docker/Testcontainers 条件测试允许 skipped。
- Maven Enforcer 和 ArchUnit 通过。

- [ ] **Step 8：执行最终静态审计**

```powershell
rg -n "@(Select|Insert|Update|Delete)\(" src/main/java/com/tyb/myblog/v2/content
rg -n "deleteById|removeById" src/main/java/com/tyb/myblog/v2/content
rg -n "Entity|Mapper|SubmittedField" src/main/java/com/tyb/myblog/v2/content/application
rg -n "com\.tyb\.myblog\.v2\.system\.domain" src/main/java/com/tyb/myblog/v2/content
git diff --check
git status --short
```

Expected:

- 前四条 `rg` 无输出。
- `git diff --check` 通过。
- 工作区只包含 Task 5 测试和文档。

- [ ] **Step 9：提交 Task 5**

```powershell
git add MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content `
  docs/project-handbook/api-contract `
  docs/project-handbook/roadmap.md `
  docs/project-handbook/status.md `
  docs/project-handbook/m3-preflight-review.md `
  docs/superpowers/specs/2026-06-14-backend-v2-category-tag-design.md `
  docs/superpowers/plans/2026-06-14-backend-v2-category-tag.md
git diff --cached --check
git commit -m "完成分类标签纵向切片"
```

- [ ] **Step 10：确认五批提交和干净工作区**

```powershell
git status --short
git log -7 --oneline
```

Expected:

- `git status --short` 无输出。
- 最新五个实施提交依次为：
  1. `完成分类标签纵向切片`
  2. `实现分类排序与引用保护删除`
  3. `实现分类标签新增与编辑`
  4. `实现公开与后台分类标签查询`
  5. `建立分类标签领域与持久化查询`

---

## 2. 最终验收清单

- [ ] 分类和标签领域对象不依赖 Spring、MyBatis、Servlet 或 HTTP。
- [ ] `content` 使用独立 `ContentLanguage`，不依赖 `system.domain`。
- [ ] MapStruct 1.6.3 与 Lombok 编译成功。
- [ ] MapStruct 不承载 fallback、slug、权限、唯一性或引用判断。
- [ ] 公开接口要求 `lang=zh|ja|en`，日英缺失 fallback 到中文。
- [ ] 后台接口返回全部三语字段。
- [ ] ADMIN、DEMO 可读，只有 ADMIN 可写。
- [ ] 分类支持 1..100 项批量排序，事务失败整体回滚。
- [ ] 标签不提供排序接口。
- [ ] slug 规范化且永久唯一，并发冲突稳定返回 409。
- [ ] 被 active 文章引用的分类和标签不能删除。
- [ ] 软删除完整写入五个审计字段。
- [ ] 不自动解绑、不级联、不物理删除。
- [ ] 所有生产 SQL 位于 Mapper XML并包含必要中文注释。
- [ ] 不修改冻结的 V1 DDL。
- [ ] OpenAPI 不暴露 Entity、Mapper、值对象或删除审计字段。
- [ ] 五个实现 Task 分别形成中文本地提交。
- [ ] 全量 Maven、Enforcer、ArchUnit 和 `git diff --check` 通过。
