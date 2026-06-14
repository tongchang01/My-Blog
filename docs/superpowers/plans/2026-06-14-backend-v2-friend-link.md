# Backend V2 友链纵向切片实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Do not use subagents.

**Goal:** 为 `system` 模块完成 `t_friend_link` 的公开读取、后台只读、ADMIN 新增编辑、状态切换、批量排序和完整软删除审计。

**Architecture:** 友链沿用 system 四层结构，领域负责字段和状态不变量，Repository 端口隔离 MyBatis Entity 与 XML SQL。公开读取只返回 VISIBLE，后台读取允许 ADMIN/DEMO，全部写操作由独立应用服务在事务中锁定 active 行；软删除显式写入删除和更新审计五个字段。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring Security、MyBatis-Plus 3.5、Mapper XML、H2、JUnit 5、Mockito、MockMvc、springdoc、Lombok。

---

## 0. 执行约束

- 在 worktree `E:\My-Blog\.worktrees\backend-v2-refactor` 中执行。
- 使用 `superpowers:executing-plans`，不使用子代理。
- 五个 Task 必须分别形成中文本地提交。
- SQL 只能写在 `FriendLinkMapper.xml`，禁止注解 SQL。
- 新业务类、锁和软删除分支必须有必要的中文注释。
- 不修改冻结的 `V1__init.sql`，不增加 version 列或唯一索引。
- 不实现在线申请、审核、恢复、回收站、批量删除和 URL 可用性探测。
- URL 允许重复。
- Task 1 至 Task 4 运行定向测试；Task 5 运行全量 `mvn clean test`。
- 每次提交前执行 `git diff --check`，确认没有混入其它任务文件。

## 1. 文件结构

### Domain

- `system/domain/friendlink/FriendLinkStatus.java`
  - 稳定映射数据库 1/2 与 API 枚举。
- `system/domain/friendlink/FriendLink.java`
  - active 友链聚合根。
- `system/domain/friendlink/NewFriendLink.java`
  - 新增命令的领域值。
- `system/domain/friendlink/FriendLinkValidation.java`
  - 字符串、URL、排序范围的统一校验。
- `system/domain/friendlink/FriendLinkPage.java`
  - 领域分页结果。
- `system/domain/friendlink/FriendLinkRepository.java`
  - 查询、锁、插入、更新、排序和软删除端口。

### Persistence

- `system/infrastructure/persistence/entity/FriendLinkEntity.java`
- `system/infrastructure/persistence/mapper/FriendLinkMapper.java`
- `system/infrastructure/persistence/repository/MyBatisFriendLinkRepository.java`
- `src/main/resources/mapper/system/FriendLinkMapper.xml`

### Application

- `system/application/friendlink/FriendLinkResult.java`
- `system/application/friendlink/PublicFriendLinkResult.java`
- `system/application/friendlink/FriendLinkPageResult.java`
- `system/application/friendlink/FriendLinkQueryService.java`
- `system/application/friendlink/CreateFriendLinkCommand.java`
- `system/application/friendlink/UpdateFriendLinkCommand.java`
- `system/application/friendlink/UpdateFriendLinkStatusCommand.java`
- `system/application/friendlink/FriendLinkSortItem.java`
- `system/application/friendlink/UpdateFriendLinkSortOrdersCommand.java`
- `system/application/friendlink/FriendLinkCreateService.java`
- `system/application/friendlink/FriendLinkUpdateService.java`
- `system/application/friendlink/FriendLinkStatusService.java`
- `system/application/friendlink/FriendLinkSortService.java`
- `system/application/friendlink/FriendLinkDeleteService.java`
- `system/application/friendlink/FriendLinkAuthorization.java`
  - 包内组件，统一 ADMIN/DEMO 和正数用户 ID 校验，避免六个服务复制逻辑。

### Web

- `system/web/PublicFriendLinkController.java`
- `system/web/AdminFriendLinkController.java`
- `system/web/PublicFriendLinkVO.java`
- `system/web/AdminFriendLinkVO.java`
- `system/web/CreateFriendLinkRequest.java`
- `system/web/UpdateFriendLinkRequest.java`
- `system/web/FriendLinkWriteOpenApiRequest.java`
- `system/web/UpdateFriendLinkStatusRequest.java`
- `system/web/UpdateFriendLinkSortOrdersRequest.java`

新增和编辑请求继续使用现有 `SubmittedField` 保存 JSON presence，并通过独立
OpenAPI record 隔离内部 presence 类型。

### Tests

- `system/domain/friendlink/FriendLinkTest.java`
- `system/infrastructure/persistence/DatabaseFriendLinkRepositoryTest.java`
- `system/application/friendlink/FriendLinkQueryServiceTest.java`
- `system/application/friendlink/FriendLinkWriteServiceTest.java`
- `system/application/friendlink/FriendLinkSortDeleteServiceTest.java`
- `system/application/friendlink/FriendLinkConcurrencyTest.java`
- `system/web/PublicFriendLinkControllerTest.java`
- `system/web/AdminFriendLinkControllerTest.java`
- `system/web/FriendLinkOpenApiTest.java`
- `system/integration/FriendLinkIntegrationTest.java`

### Existing files to modify

- `common/security/SecurityConfig.java`
- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/test/resources/application-test.yml`
- `common/config/BackendPropertiesTest.java`
- `common/security/SecurityConfigTest.java`
- `docs/project-handbook/api-contract/README.md`
- `docs/project-handbook/status.md`
- `docs/project-handbook/roadmap.md`
- `docs/project-handbook/m3-preflight-review.md`
- friend-link spec and this plan.

---

## Task 1：建立友链领域与持久化查询

**提交信息：** `建立友链领域与持久化查询`

### Step 1：写领域失败测试

- [ ] 新建 `FriendLinkTest.java`，覆盖：

```java
@Test
void parsesStableStatusValues() {
    assertThat(FriendLinkStatus.fromDatabaseValue(1))
            .isEqualTo(FriendLinkStatus.VISIBLE);
    assertThat(FriendLinkStatus.fromDatabaseValue(2))
            .isEqualTo(FriendLinkStatus.HIDDEN);
    assertThatThrownBy(() -> FriendLinkStatus.fromDatabaseValue(3))
            .isInstanceOf(IllegalArgumentException.class);
}

@Test
void normalizesBusinessFields() {
    FriendLink link = FriendLink.reconstitute(
            10L,
            " Example ",
            " https://example.com/path ",
            " https://example.com/logo.png ",
            " 介绍 ",
            20,
            FriendLinkStatus.VISIBLE,
            LocalDateTime.of(2026, 6, 14, 12, 0),
            1001L,
            LocalDateTime.of(2026, 6, 14, 12, 30),
            1001L);

    assertThat(link.name()).isEqualTo("Example");
    assertThat(link.url()).isEqualTo("https://example.com/path");
    assertThat(link.avatarUrl())
            .isEqualTo("https://example.com/logo.png");
    assertThat(link.description()).isEqualTo("介绍");
}
```

同时覆盖：

- id 非正数。
- name 空白或超过 64。
- URL 相对地址、ftp、无 host、带 user-info、超过 255。
- avatarUrl 空白归一化为 null，非法 URL 拒绝。
- description 空白归一化为 null，超过 255 拒绝。
- sortOrder 小于 0 或大于 1,000,000 拒绝。
- `NewFriendLink.create` 不需要 ID，但执行相同业务校验。
- 两条相同 URL 的领域对象均可创建。

### Step 2：运行领域测试确认 RED

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkTest" test
```

预期：`FriendLink`、`FriendLinkStatus`、`NewFriendLink` 尚不存在，测试编译失败。

### Step 3：实现状态与统一校验

- [ ] 新建 `FriendLinkStatus.java`：

```java
public enum FriendLinkStatus {
    VISIBLE(1),
    HIDDEN(2);

    private final int databaseValue;

    FriendLinkStatus(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public static FriendLinkStatus fromDatabaseValue(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("友链状态不能为空");
        }
        return switch (value) {
            case 1 -> VISIBLE;
            case 2 -> HIDDEN;
            default -> throw new IllegalArgumentException(
                    "不支持的友链状态值");
        };
    }
}
```

- [ ] 新建 `FriendLinkValidation.java`，固定常量和校验：

```java
final class FriendLinkValidation {
    static final int MAX_NAME_LENGTH = 64;
    static final int MAX_URL_LENGTH = 255;
    static final int MAX_DESCRIPTION_LENGTH = 255;
    static final int MAX_SORT_ORDER = 1_000_000;

    static Values validate(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status) {
        String normalizedName = requiredText(
                name, MAX_NAME_LENGTH, "友链名称");
        String normalizedUrl = httpUrl(
                url, true, "友链地址");
        String normalizedAvatar = httpUrl(
                avatarUrl, false, "友链头像地址");
        String normalizedDescription = optionalText(
                description, MAX_DESCRIPTION_LENGTH, "友链介绍");
        if (sortOrder < 0 || sortOrder > MAX_SORT_ORDER) {
            throw new IllegalArgumentException(
                    "友链排序值必须在0到1000000之间");
        }
        if (status == null) {
            throw new IllegalArgumentException("友链状态不能为空");
        }
        return new Values(
                normalizedName,
                normalizedUrl,
                normalizedAvatar,
                normalizedDescription,
                sortOrder,
                status);
    }

    private static String requiredText(
            String value,
            int maxLength,
            String fieldName) {
        String normalized = optionalText(value, maxLength, fieldName);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return normalized;
    }

    private static String optionalText(
            String value,
            int maxLength,
            String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + "长度不能超过" + maxLength);
        }
        return normalized;
    }

    private static String httpUrl(
            String value,
            boolean required,
            String fieldName) {
        String normalized = optionalText(
                value, MAX_URL_LENGTH, fieldName);
        if (normalized == null) {
            if (required) {
                throw new IllegalArgumentException(
                        fieldName + "不能为空");
            }
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            boolean supportedScheme =
                    "http".equalsIgnoreCase(uri.getScheme())
                            || "https".equalsIgnoreCase(
                                    uri.getScheme());
            if (!supportedScheme
                    || uri.getHost() == null
                    || uri.getHost().isBlank()
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException(
                        fieldName + "必须是有效的HTTP或HTTPS绝对地址");
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    fieldName + "必须是有效的HTTP或HTTPS绝对地址",
                    exception);
        }
    }

    record Values(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status
    ) {
    }
}
```

补充 `java.net.URI` 导入。上述实现固定要求 scheme 为 http/https、host
非空、`getUserInfo()` 为 null；必填 URL trim 后非空，可选 URL 空白转
null，长度在解析前完成检查。

### Step 4：实现领域对象

- [ ] 新建 `FriendLink.java`：

```java
public record FriendLink(
        long id,
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy
) {
    public static FriendLink reconstitute(
            long id,
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status,
            LocalDateTime createdAt,
            Long createdBy,
            LocalDateTime updatedAt,
            Long updatedBy) {
        if (id <= 0) {
            throw new IllegalArgumentException("友链 ID 必须为正数");
        }
        FriendLinkValidation.Values values =
                FriendLinkValidation.validate(
                        name, url, avatarUrl, description,
                        sortOrder, status);
        return new FriendLink(
                id,
                values.name(),
                values.url(),
                values.avatarUrl(),
                values.description(),
                values.sortOrder(),
                values.status(),
                createdAt,
                createdBy,
                updatedAt,
                updatedBy);
    }

    public FriendLink replace(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status) {
        return reconstitute(
                id, name, url, avatarUrl, description, sortOrder,
                status, createdAt, createdBy, updatedAt, updatedBy);
    }

    public FriendLink withStatus(FriendLinkStatus newStatus) {
        return replace(
                name, url, avatarUrl, description,
                sortOrder, newStatus);
    }
}
```

- [ ] 新建 `NewFriendLink.java`：

```java
public record NewFriendLink(
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status,
        long createdBy
) {
    public static NewFriendLink create(
            String name,
            String url,
            String avatarUrl,
            String description,
            int sortOrder,
            FriendLinkStatus status,
            long createdBy) {
        if (createdBy <= 0) {
            throw new IllegalArgumentException(
                    "友链创建人 ID 必须为正数");
        }
        FriendLinkValidation.Values values =
                FriendLinkValidation.validate(
                        name, url, avatarUrl, description,
                        sortOrder, status);
        return new NewFriendLink(
                values.name(),
                values.url(),
                values.avatarUrl(),
                values.description(),
                values.sortOrder(),
                values.status(),
                createdBy);
    }
}
```

### Step 5：领域测试确认 GREEN

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkTest" test
```

预期：领域测试全部通过。

### Step 6：写 Repository 失败测试

- [ ] 新建 `DatabaseFriendLinkRepositoryTest.java`，使用 `@ActiveProfiles("test")`
和 `@SpringBootTest`，每次测试前 `DELETE FROM t_friend_link`。

覆盖：

```java
@Test
void readsOnlyVisibleActiveLinksForPublicViewInStableOrder() {
    insert(101L, 1, 20, false);
    insert(102L, 2, 0, false);
    insert(103L, 1, 10, false);
    insert(104L, 1, 0, true);

    assertThat(repository.findPublicVisible())
            .extracting(FriendLink::id)
            .containsExactly(103L, 101L);
}

@Test
void pagesAllActiveStatusesInStableOrder() {
    insert(101L, 1, 20, false);
    insert(102L, 2, 0, false);
    insert(103L, 1, 0, false);

    FriendLinkPage page = repository.findActivePage(1, 2);

    assertThat(page.total()).isEqualTo(3);
    assertThat(page.records()).extracting(FriendLink::id)
            .containsExactly(102L, 103L);
}
```

同时覆盖：

- `findActiveById` 忽略 deleted。
- `findActiveByIdForUpdate` 返回 active。
- `findActiveByIdsForUpdate(List.of(103,101))` 结果按 ID 升序。
- 插入使用 ASSIGN_ID，并保存 createdBy/updatedBy。
- 未知 status 数值在 Repository 转换时抛异常。

### Step 7：运行 Repository 测试确认 RED

- [ ] 执行：

```powershell
mvn "-Dtest=DatabaseFriendLinkRepositoryTest" test
```

预期：Repository、Entity、Mapper 和 XML 尚不存在。

### Step 8：实现分页与 Repository 端口

- [ ] 新建 `FriendLinkPage.java`：

```java
public record FriendLinkPage(
        List<FriendLink> records,
        long total,
        int page,
        int size
) {
    public FriendLinkPage {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
```

- [ ] 新建 `FriendLinkRepository.java`：

```java
public interface FriendLinkRepository {
    List<FriendLink> findPublicVisible();
    FriendLinkPage findActivePage(int page, int size);
    Optional<FriendLink> findActiveById(long id);
    Optional<FriendLink> findActiveByIdForUpdate(long id);
    List<FriendLink> findActiveByIdsForUpdate(List<Long> ids);
    FriendLink insert(NewFriendLink friendLink);
    boolean update(
            FriendLink friendLink,
            LocalDateTime updatedAt,
            long updatedBy);
    boolean updateStatus(
            long id,
            FriendLinkStatus status,
            LocalDateTime updatedAt,
            long updatedBy);
    boolean updateSortOrder(
            long id,
            int sortOrder,
            LocalDateTime updatedAt,
            long updatedBy);
    boolean softDelete(
            long id,
            LocalDateTime deletedAt,
            long deletedBy);
}
```

### Step 9：实现 Entity、Mapper 和 XML 查询

- [ ] 新建 `FriendLinkEntity.java`：

```java
@Getter
@Setter
@TableName("t_friend_link")
public class FriendLinkEntity extends BaseEntity {
    private String name;
    private String url;
    private String avatarUrl;
    private String description;
    private Integer sortOrder;
    private Integer status;
}
```

- [ ] 新建 `FriendLinkMapper.java`，只声明方法，不写 SQL 注解：

```java
@Mapper
public interface FriendLinkMapper
        extends BaseMapper<FriendLinkEntity> {
    List<FriendLinkEntity> selectPublicVisible();
    List<FriendLinkEntity> selectActivePage(
            @Param("offset") long offset,
            @Param("size") int size);
    long countActive();
    FriendLinkEntity selectActiveById(@Param("id") long id);
    FriendLinkEntity selectActiveByIdForUpdate(@Param("id") long id);
    List<FriendLinkEntity> selectActiveByIdsForUpdate(
            @Param("ids") List<Long> ids);
    int updateActive(
            @Param("link") FriendLinkEntity link,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);
    int updateStatus(
            @Param("id") long id,
            @Param("status") int status,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);
    int updateSortOrder(
            @Param("id") long id,
            @Param("sortOrder") int sortOrder,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") long updatedBy);
    int softDelete(
            @Param("id") long id,
            @Param("deletedAt") LocalDateTime deletedAt,
            @Param("deletedBy") long deletedBy);
}
```

- [ ] 新建 `FriendLinkMapper.xml`，先实现本 Task 使用的查询：

```xml
<sql id="friendLinkColumns">
    id, name, url, avatar_url, description, sort_order, status,
    created_at, created_by, updated_at, updated_by,
    deleted, deleted_at, deleted_by
</sql>

<select id="selectPublicVisible"
        resultType="com.tyb.myblog.v2.system.infrastructure.persistence.entity.FriendLinkEntity">
    SELECT <include refid="friendLinkColumns"/>
    FROM t_friend_link
    WHERE deleted = 0
      AND status = 1
    ORDER BY sort_order ASC, id ASC
</select>

<select id="selectActivePage"
        resultType="com.tyb.myblog.v2.system.infrastructure.persistence.entity.FriendLinkEntity">
    SELECT <include refid="friendLinkColumns"/>
    FROM t_friend_link
    WHERE deleted = 0
    ORDER BY sort_order ASC, id ASC
    LIMIT #{size} OFFSET #{offset}
</select>

<select id="countActive" resultType="long">
    SELECT COUNT(*) FROM t_friend_link WHERE deleted = 0
</select>

<select id="selectActiveById"
        resultType="com.tyb.myblog.v2.system.infrastructure.persistence.entity.FriendLinkEntity">
    SELECT <include refid="friendLinkColumns"/>
    FROM t_friend_link
    WHERE id = #{id} AND deleted = 0
</select>

<select id="selectActiveByIdForUpdate"
        resultType="com.tyb.myblog.v2.system.infrastructure.persistence.entity.FriendLinkEntity">
    SELECT <include refid="friendLinkColumns"/>
    FROM t_friend_link
    WHERE id = #{id} AND deleted = 0
    FOR UPDATE
</select>

<select id="selectActiveByIdsForUpdate"
        resultType="com.tyb.myblog.v2.system.infrastructure.persistence.entity.FriendLinkEntity">
    SELECT <include refid="friendLinkColumns"/>
    FROM t_friend_link
    WHERE deleted = 0
      AND id IN
      <foreach collection="ids" item="id" open="(" separator="," close=")">
          #{id}
      </foreach>
    ORDER BY id ASC
    FOR UPDATE
</select>
```

Repository 不得用空列表调用 `selectActiveByIdsForUpdate`。

### Step 10：实现 Repository 适配器

- [ ] 新建 `MyBatisFriendLinkRepository.java`：

```java
@Repository
@RequiredArgsConstructor
public class MyBatisFriendLinkRepository
        implements FriendLinkRepository {
    private final FriendLinkMapper mapper;

    @Override
    public List<FriendLink> findPublicVisible() {
        return mapper.selectPublicVisible().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public FriendLinkPage findActivePage(int page, int size) {
        long offset = Math.multiplyExact((long) page - 1L, size);
        return new FriendLinkPage(
                mapper.selectActivePage(offset, size).stream()
                        .map(this::toDomain)
                        .toList(),
                mapper.countActive(),
                page,
                size);
    }

    private FriendLink toDomain(FriendLinkEntity entity) {
        return FriendLink.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getUrl(),
                entity.getAvatarUrl(),
                entity.getDescription(),
                entity.getSortOrder(),
                FriendLinkStatus.fromDatabaseValue(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy());
    }
}
```

实现 active ID 和锁查询。写方法可以在本 Task 中按端口完整实现，但不在本 Task
暴露应用接口。

插入时显式设置：

```java
entity.setCreatedBy(friendLink.createdBy());
entity.setUpdatedBy(friendLink.createdBy());
mapper.insert(entity);
```

插入后调用 `selectActiveById` 返回数据库结果。

### Step 11：运行定向测试和静态检查

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkTest,DatabaseFriendLinkRepositoryTest,ArchitectureRulesTest" test
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
git diff --check
```

预期：测试通过；无注解 SQL；无格式错误。

### Step 12：提交 Task 1

- [ ] 执行：

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/friendlink MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence MyBlog-springboot-v2/src/main/resources/mapper/system/FriendLinkMapper.xml MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/domain/friendlink MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/infrastructure/persistence/DatabaseFriendLinkRepositoryTest.java
git commit -m "建立友链领域与持久化查询"
```

---

## Task 2：实现公开与后台友链查询

**提交信息：** `实现公开与后台友链查询`

### Step 1：写查询应用服务失败测试

- [ ] 新建 `FriendLinkQueryServiceTest.java`，mock Repository，覆盖：

```java
@Test
void returnsPublicVisibleResultsWithoutAuditFields() {
    when(repository.findPublicVisible())
            .thenReturn(List.of(friendLink(FriendLinkStatus.VISIBLE)));

    assertThat(service.publicList())
            .singleElement()
            .extracting(PublicFriendLinkResult::name)
            .isEqualTo("Example");
}

@Test
void allowsAdminAndDemoToReadAdminPage() {
    when(repository.findActivePage(1, 20))
            .thenReturn(new FriendLinkPage(
                    List.of(friendLink(FriendLinkStatus.HIDDEN)),
                    1, 1, 20));

    assertThat(service.adminPage(principal("ADMIN"), 1, 20).total())
            .isEqualTo(1);
    assertThat(service.adminPage(principal("DEMO"), 1, 20).total())
            .isEqualTo(1);
}
```

同时覆盖：

- null principal → INVALID_TOKEN。
- GUEST/未知角色 → FORBIDDEN。
- page < 1、size < 1、size > 100 → VALIDATION_ERROR。
- ID 非正数 → VALIDATION_ERROR。
- 详情不存在 → NOT_FOUND。
- 应用层返回 `FriendLinkPageResult`，不依赖 `common.web.PageResponse`。

### Step 2：写 Controller 失败测试

- [ ] 新建 `PublicFriendLinkControllerTest.java`：

- GET 成功返回数组。
- 公开 VO 只有 id/name/url/avatarUrl/description。
- status、sortOrder、createdAt、updatedAt、deleted 不存在。

- [ ] 新建 `AdminFriendLinkControllerTest.java`：

- GET 集合默认 page=1/size=20。
- GET 详情。
- 分页字段为 records/total/page/size。
- 后台 VO 包含 status、sortOrder 和创建/更新审计。
- 删除审计字段不存在。

Controller slice 使用：

```java
@WebMvcTest({
        PublicFriendLinkController.class,
        AdminFriendLinkController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
```

并 `@MockitoBean FriendLinkQueryService`。

### Step 3：扩展 Security 和配置失败测试

- [ ] 扩展 `SecurityConfigTest`：

- 匿名 GET `/api/public/friend-links` 返回 200。
- 匿名后台 GET 返回 401。
- ADMIN/DEMO 后台列表和详情可进入。
- DEMO POST 仍为 403。

- [ ] 扩展 `BackendPropertiesTest`，test profile 白名单顺序增加：

```java
tuple("GET", "/api/public/friend-links")
```

- [ ] 扩展配置静态测试，确认 base/local/test 均包含公开友链 GET；prod 继承 base，
不重复配置白名单。

### Step 4：运行测试确认 RED

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkQueryServiceTest,PublicFriendLinkControllerTest,AdminFriendLinkControllerTest,SecurityConfigTest,BackendPropertiesTest" test
```

预期：查询服务、Controller 和配置项不存在。

### Step 5：实现应用结果与查询服务

- [ ] 新建：

```java
public record PublicFriendLinkResult(
        long id,
        String name,
        String url,
        String avatarUrl,
        String description
) {
    public static PublicFriendLinkResult from(FriendLink link) {
        return new PublicFriendLinkResult(
                link.id(), link.name(), link.url(),
                link.avatarUrl(), link.description());
    }
}

public record FriendLinkResult(
        long id,
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy
) {
    public static FriendLinkResult from(FriendLink link) {
        return new FriendLinkResult(
                link.id(), link.name(), link.url(),
                link.avatarUrl(), link.description(),
                link.sortOrder(), link.status(),
                link.createdAt(), link.createdBy(),
                link.updatedAt(), link.updatedBy());
    }
}

public record FriendLinkPageResult(
        List<FriendLinkResult> records,
        long total,
        int page,
        int size
) {
    public FriendLinkPageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
```

- [ ] 新建 `FriendLinkAuthorization.java`：

```java
@Component
class FriendLinkAuthorization {
    void requireReadable(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        boolean readable = principal.roles().stream()
                .anyMatch(role ->
                        "ADMIN".equals(role) || "DEMO".equals(role));
        if (!readable) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
    }

    long requireAdmin(AuthenticatedPrincipal principal) {
        if (principal == null) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        if (!principal.roles().contains("ADMIN")) {
            throw new ApiException(ApiErrorCode.FORBIDDEN);
        }
        try {
            long id = Long.parseLong(principal.id());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }
}
```

- [ ] 新建 `FriendLinkQueryService.java`：

```java
@Service
@RequiredArgsConstructor
public class FriendLinkQueryService {
    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;

    public List<PublicFriendLinkResult> publicList() {
        return repository.findPublicVisible().stream()
                .map(PublicFriendLinkResult::from)
                .toList();
    }

    public FriendLinkPageResult adminPage(
            AuthenticatedPrincipal principal,
            int page,
            int size) {
        authorization.requireReadable(principal);
        validatePage(page, size);
        FriendLinkPage result = repository.findActivePage(page, size);
        return new FriendLinkPageResult(
                result.records().stream()
                        .map(FriendLinkResult::from)
                        .toList(),
                result.total(), result.page(), result.size());
    }

    public FriendLinkResult adminDetail(
            AuthenticatedPrincipal principal,
            long id) {
        authorization.requireReadable(principal);
        validateId(id);
        return repository.findActiveById(id)
                .map(FriendLinkResult::from)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "友链不存在"));
    }
}
```

### Step 6：实现公开和后台查询 Controller

- [ ] 新建 VO：

```java
public record PublicFriendLinkVO(
        long id,
        String name,
        String url,
        String avatarUrl,
        String description
) {
    static PublicFriendLinkVO from(PublicFriendLinkResult result) {
        return new PublicFriendLinkVO(
                result.id(), result.name(), result.url(),
                result.avatarUrl(), result.description());
    }
}

public record AdminFriendLinkVO(
        long id,
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy
) {
    static AdminFriendLinkVO from(FriendLinkResult result) {
        return new AdminFriendLinkVO(
                result.id(), result.name(), result.url(),
                result.avatarUrl(), result.description(),
                result.sortOrder(), result.status(),
                result.createdAt(), result.createdBy(),
                result.updatedAt(), result.updatedBy());
    }
}
```

- [ ] 新建 `PublicFriendLinkController`：

```java
@Tag(name = "公开友链", description = "前台友链列表")
@RestController
@RequestMapping("/api/public/friend-links")
@RequiredArgsConstructor
public class PublicFriendLinkController {
    private final FriendLinkQueryService queryService;

    @GetMapping
    public ApiResponse<List<PublicFriendLinkVO>> list() {
        return ApiResponse.ok(queryService.publicList().stream()
                .map(PublicFriendLinkVO::from)
                .toList());
    }
}
```

- [ ] 新建 `AdminFriendLinkController`，本 Task 先实现 GET：

```java
@GetMapping
public ApiResponse<PageResponse<AdminFriendLinkVO>> page(
        @CurrentUser AuthenticatedPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
    FriendLinkPageResult result =
            queryService.adminPage(principal, page, size);
    return ApiResponse.ok(new PageResponse<>(
            result.records().stream()
                    .map(AdminFriendLinkVO::from)
                    .toList(),
            result.total(), result.page(), result.size()));
}

@GetMapping("/{id:\\d+}")
public ApiResponse<AdminFriendLinkVO> detail(
        @CurrentUser AuthenticatedPrincipal principal,
        @PathVariable long id) {
    return ApiResponse.ok(AdminFriendLinkVO.from(
            queryService.adminDetail(principal, id)));
}
```

使用 `{id:\d+}` 明确避免静态 `/sort-orders` 被当作 ID。

### Step 7：配置白名单与后台只读权限

- [ ] 在 `application.yml` 的 public endpoints 增加：

```yaml
- method: GET
  path: /api/public/friend-links
```

- [ ] `application-local.yml` 和 `src/test/resources/application-test.yml` 的完整覆盖列表
也增加同一条目。

- [ ] `SecurityConfig` 在 `/api/admin/**` 前增加：

```java
.requestMatchers(
        HttpMethod.GET,
        "/api/admin/friend-links",
        "/api/admin/friend-links/*")
.hasAnyRole("ADMIN", "DEMO")
```

### Step 8：定向验证与提交

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkQueryServiceTest,PublicFriendLinkControllerTest,AdminFriendLinkControllerTest,SecurityConfigTest,BackendPropertiesTest,ArchitectureRulesTest" test
git diff --check
```

预期：全部通过。

- [ ] 提交：

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/friendlink MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java MyBlog-springboot-v2/src/main/resources/application.yml MyBlog-springboot-v2/src/main/resources/application-local.yml MyBlog-springboot-v2/src/test/resources/application-test.yml MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common
git commit -m "实现公开与后台友链查询"
```

---

## Task 3：实现友链新增与编辑

**提交信息：** `实现友链新增与编辑`

### Step 1：写应用服务失败测试

- [ ] 新建 `FriendLinkWriteServiceTest.java`，mock Repository、Authorization 和固定 Clock。

新增覆盖：

- null command → VALIDATION_ERROR。
- 非 ADMIN → FORBIDDEN。
- 非数字/非正 principal ID → INVALID_TOKEN。
- 非法领域字段 → VALIDATION_ERROR 且不 insert。
- 合法请求创建 `NewFriendLink`，调用 insert 并返回结果。
- 重复 URL 不做冲突查询，仍可 insert。

编辑覆盖：

- ID 非正数 → VALIDATION_ERROR。
- 目标不存在 → NOT_FOUND。
- 先 `findActiveByIdForUpdate` 后更新。
- 完整替换所有六个业务字段。
- update 返回 false → INTERNAL_ERROR。
- 成功后重新 `findActiveById` 返回数据库审计结果。

核心断言：

```java
verify(repository).findActiveByIdForUpdate(10L);
verify(repository).update(
        argThat(link ->
                link.id() == 10L
                        && "New".equals(link.name())
                        && link.status() == FriendLinkStatus.HIDDEN),
        eq(NOW),
        eq(1001L));
```

### Step 2：写 Web 请求失败测试

- [ ] 扩展 `AdminFriendLinkControllerTest`：

- POST 成功调用 create service。
- PUT `/10` 成功调用 update service。
- 缺任一六字段返回 400 + 90001。
- avatarUrl/description 显式 null 可进入应用层。
- 未知字段返回 400 + 90001。
- 非法枚举返回 400 + 90001。

请求完整性测试使用实际 JSON，不直接构造 request。

### Step 3：运行测试确认 RED

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkWriteServiceTest,AdminFriendLinkControllerTest" test
```

预期：command、写服务和 POST/PUT 尚不存在。

### Step 4：实现应用命令

- [ ] 新建：

```java
public record CreateFriendLinkCommand(
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status
) {
}

public record UpdateFriendLinkCommand(
        String name,
        String url,
        String avatarUrl,
        String description,
        int sortOrder,
        FriendLinkStatus status
) {
}
```

### Step 5：实现新增服务

- [ ] 新建 `FriendLinkCreateService.java`：

```java
@Service
@RequiredArgsConstructor
public class FriendLinkCreateService {
    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;

    @Transactional
    public FriendLinkResult create(
            AuthenticatedPrincipal principal,
            CreateFriendLinkCommand command) {
        long actorId = authorization.requireAdmin(principal);
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链请求不能为空");
        }
        try {
            return FriendLinkResult.from(repository.insert(
                    NewFriendLink.create(
                            command.name(),
                            command.url(),
                            command.avatarUrl(),
                            command.description(),
                            command.sortOrder(),
                            command.status(),
                            actorId)));
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
    }
}
```

### Step 6：实现编辑服务

- [ ] 新建 `FriendLinkUpdateService.java`：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkUpdateService {
    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;
    private final Clock clock;

    @Transactional
    public FriendLinkResult update(
            AuthenticatedPrincipal principal,
            long id,
            UpdateFriendLinkCommand command) {
        long actorId = authorization.requireAdmin(principal);
        validateId(id);
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "友链请求不能为空");
        }
        FriendLink current = repository.findActiveByIdForUpdate(id)
                .orElseThrow(this::notFound);
        FriendLink updated;
        try {
            updated = current.replace(
                    command.name(),
                    command.url(),
                    command.avatarUrl(),
                    command.description(),
                    command.sortOrder(),
                    command.status());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.update(updated, now, actorId)) {
            log.error("友链更新行数异常，friendLinkId={}", id);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
        return repository.findActiveById(id)
                .map(FriendLinkResult::from)
                .orElseThrow(this::missingAfterUpdate);
    }
}
```

### Step 7：实现 Repository 写 SQL

- [ ] `FriendLinkMapper.xml` 增加：

```xml
<update id="updateActive">
    UPDATE t_friend_link
    SET name = #{link.name},
        url = #{link.url},
        avatar_url = #{link.avatarUrl},
        description = #{link.description},
        sort_order = #{link.sortOrder},
        status = #{link.status},
        updated_at = #{updatedAt},
        updated_by = #{updatedBy}
    WHERE id = #{link.id}
      AND deleted = 0
</update>
```

Repository `update` 将枚举转为 databaseValue 后构造 Entity。

### Step 8：实现 presence 请求模型

- [ ] 新建 `FriendLinkWriteOpenApiRequest`：

```java
public record FriendLinkWriteOpenApiRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String url,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String avatarUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Integer sortOrder,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        FriendLinkStatus status
) {
}
```

- [ ] `CreateFriendLinkRequest` 和 `UpdateFriendLinkRequest` 使用六个
`SubmittedField`，各自：

- `@JsonSetter` 记录字段出现。
- `@JsonAnySetter` 返回“不支持的友链字段：x”。
- `toCommand()` 在任一字段缺失时抛“请求必须包含全部友链字段”。
- sortOrder 用 `SubmittedField<Integer>`，status 用
  `SubmittedField<FriendLinkStatus>`，允许值本身为 null 后交领域校验。

不要把 `SubmittedField` 暴露给 OpenAPI。

### Step 9：扩展 Controller

- [ ] `AdminFriendLinkController` 注入 create/update service，增加：

```java
@PostMapping
@Operation(
        summary = "新增友链",
        requestBody = @RequestBody(
                content = @Content(schema = @Schema(
                        implementation =
                                FriendLinkWriteOpenApiRequest.class))))
public ApiResponse<AdminFriendLinkVO> create(
        @CurrentUser AuthenticatedPrincipal principal,
        @org.springframework.web.bind.annotation.RequestBody
        CreateFriendLinkRequest request) {
    return ApiResponse.ok(AdminFriendLinkVO.from(
            createService.create(principal, request.toCommand())));
}

@PutMapping("/{id:\\d+}")
public ApiResponse<AdminFriendLinkVO> update(
        @CurrentUser AuthenticatedPrincipal principal,
        @PathVariable long id,
        @org.springframework.web.bind.annotation.RequestBody
        UpdateFriendLinkRequest request) {
    return ApiResponse.ok(AdminFriendLinkVO.from(
            updateService.update(
                    principal, id, request.toCommand())));
}
```

### Step 10：定向验证与提交

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkWriteServiceTest,AdminFriendLinkControllerTest,DatabaseFriendLinkRepositoryTest,SecurityConfigTest,ArchitectureRulesTest" test
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
git diff --check
```

- [ ] 提交：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
git commit -m "实现友链新增与编辑"
```

---

## Task 4：实现友链状态排序与软删除

**提交信息：** `实现友链状态排序与软删除`

### Step 1：写状态、排序、删除失败测试

- [ ] 新建 `FriendLinkSortDeleteServiceTest.java`。

状态覆盖：

- 非 ADMIN 拒绝。
- ID 非正数、command/status null 拒绝。
- 锁查询不存在 → NOT_FOUND。
- 相同状态仍调用 updateStatus 并刷新审计。
- 更新 false → INTERNAL_ERROR。

排序覆盖：

- command/items null。
- 0 项和 101 项。
- ID 非正数、sortOrder 越界。
- 请求 ID 重复。
- 服务按升序 ID 调 `findActiveByIdsForUpdate`。
- 锁定行数量或 ID 集合不一致 → NOT_FOUND，且不更新。
- 任一 updateSortOrder false → INTERNAL_ERROR。
- 所有更新共用同一个 now 和 actorId。

删除覆盖：

- 锁查询不存在 → NOT_FOUND。
- 调 softDelete 写 now 和 actorId。
- softDelete false → INTERNAL_ERROR。

### Step 2：扩展 Repository 真实写测试

- [ ] 扩展 `DatabaseFriendLinkRepositoryTest`：

```java
@Test
void softDeletesWithCompleteAuditFields() {
    insert(101L, 1, 0, false);
    LocalDateTime now =
            LocalDateTime.of(2026, 6, 14, 13, 0);

    assertThat(repository.softDelete(101L, now, 1001L)).isTrue();

    assertThat(jdbcTemplate.queryForMap("""
            SELECT deleted, deleted_at, deleted_by,
                   updated_at, updated_by
            FROM t_friend_link WHERE id = 101
            """))
            .containsEntry("DELETED", 1)
            .containsEntry("DELETED_AT", now)
            .containsEntry("DELETED_BY", 1001L)
            .containsEntry("UPDATED_AT", now)
            .containsEntry("UPDATED_BY", 1001L);
}
```

同时验证：

- updateStatus 只更新 active。
- updateSortOrder 只更新 active。
- 第二次 softDelete 返回 false。
- 软删除后所有 active 查询不可见。

### Step 3：写并发失败测试

- [ ] 新建 `FriendLinkConcurrencyTest.java`，真实 Spring/H2，并通过 Primary 包装
`MyBatisFriendLinkRepository` 协调锁时序。

至少覆盖：

1. 更新持有行锁时删除等待；更新提交后删除成功，最终 deleted=1。
2. 删除先完成后更新读取不到 active，返回 NOT_FOUND。
3. 排序请求锁定两个 ID 时，删除其中一个等待；排序提交后删除成功。

测试必须断言 `AopUtils.isAopProxy(service)`，证明事务代理生效。

### Step 4：运行测试确认 RED

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkSortDeleteServiceTest,DatabaseFriendLinkRepositoryTest,FriendLinkConcurrencyTest" test
```

预期：服务、命令、Repository 写方法和 XML SQL 尚未完成。

### Step 5：实现命令与状态服务

- [ ] 新建：

```java
public record UpdateFriendLinkStatusCommand(
        FriendLinkStatus status
) {
}

public record FriendLinkSortItem(long id, int sortOrder) {
}

public record UpdateFriendLinkSortOrdersCommand(
        List<FriendLinkSortItem> items
) {
    public UpdateFriendLinkSortOrdersCommand {
        items = items == null ? null : List.copyOf(items);
    }
}
```

- [ ] 新建 `FriendLinkStatusService`，事务流程：

1. `authorization.requireAdmin`。
2. 校验 ID 和 status。
3. `findActiveByIdForUpdate`。
4. 固定 Clock 获取 now。
5. `updateStatus`。
6. 重新按 ID 读取并返回。

### Step 6：实现批量排序服务

- [ ] 新建 `FriendLinkSortService`：

```java
@Transactional
public List<FriendLinkResult> update(
        AuthenticatedPrincipal principal,
        UpdateFriendLinkSortOrdersCommand command) {
    long actorId = authorization.requireAdmin(principal);
    List<FriendLinkSortItem> items = validate(command);
    List<Long> sortedIds = items.stream()
            .map(FriendLinkSortItem::id)
            .sorted()
            .toList();
    List<FriendLink> locked =
            repository.findActiveByIdsForUpdate(sortedIds);
    Set<Long> lockedIds = locked.stream()
            .map(FriendLink::id)
            .collect(Collectors.toSet());
    if (lockedIds.size() != sortedIds.size()
            || !lockedIds.containsAll(sortedIds)) {
        throw new ApiException(
                ApiErrorCode.NOT_FOUND,
                "部分友链不存在");
    }
    LocalDateTime now = LocalDateTime.now(clock);
    for (FriendLinkSortItem item : items) {
        if (!repository.updateSortOrder(
                item.id(), item.sortOrder(), now, actorId)) {
            throw internalUpdateFailure(item.id());
        }
    }
    return items.stream()
            .map(item -> repository.findActiveById(item.id())
                    .map(FriendLinkResult::from)
                    .orElseThrow(this::missingAfterUpdate))
            .toList();
}
```

`validate` 必须返回 defensive copy，并用 Set 拦截重复 ID。

### Step 7：实现删除服务

- [ ] 新建 `FriendLinkDeleteService`：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkDeleteService {
    private final FriendLinkRepository repository;
    private final FriendLinkAuthorization authorization;
    private final Clock clock;

    @Transactional
    public void delete(
            AuthenticatedPrincipal principal,
            long id) {
        long actorId = authorization.requireAdmin(principal);
        validateId(id);
        repository.findActiveByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.NOT_FOUND,
                        "友链不存在"));
        LocalDateTime now = LocalDateTime.now(clock);
        if (!repository.softDelete(id, now, actorId)) {
            log.error("友链软删除行数异常，friendLinkId={}", id);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
    }
}
```

### Step 8：完成 XML 写 SQL

- [ ] `FriendLinkMapper.xml` 增加：

```xml
<update id="updateStatus">
    UPDATE t_friend_link
    SET status = #{status},
        updated_at = #{updatedAt},
        updated_by = #{updatedBy}
    WHERE id = #{id}
      AND deleted = 0
</update>

<update id="updateSortOrder">
    UPDATE t_friend_link
    SET sort_order = #{sortOrder},
        updated_at = #{updatedAt},
        updated_by = #{updatedBy}
    WHERE id = #{id}
      AND deleted = 0
</update>

<update id="softDelete">
    UPDATE t_friend_link
    SET deleted = 1,
        deleted_at = #{deletedAt},
        deleted_by = #{deletedBy},
        updated_at = #{deletedAt},
        updated_by = #{deletedBy}
    WHERE id = #{id}
      AND deleted = 0
</update>
```

不得调用 `BaseMapper.deleteById` 或 `@TableLogic` 自动删除。

### Step 9：实现 Web 请求与接口

- [ ] 新建：

```java
public record UpdateFriendLinkStatusRequest(
        FriendLinkStatus status
) {
    UpdateFriendLinkStatusCommand toCommand() {
        return new UpdateFriendLinkStatusCommand(status);
    }
}

public record UpdateFriendLinkSortOrdersRequest(
        List<Item> items
) {
    public UpdateFriendLinkSortOrdersCommand toCommand() {
        return new UpdateFriendLinkSortOrdersCommand(
                items == null
                        ? null
                        : items.stream()
                                .map(item -> new FriendLinkSortItem(
                                        item.id(), item.sortOrder()))
                                .toList());
    }

    public record Item(long id, int sortOrder) {
    }
}
```

- [ ] `AdminFriendLinkController` 增加：

```java
@PatchMapping("/{id:\\d+}/status")
public ApiResponse<AdminFriendLinkVO> updateStatus(
        @CurrentUser AuthenticatedPrincipal principal,
        @PathVariable long id,
        @RequestBody UpdateFriendLinkStatusRequest request) {
    return ApiResponse.ok(AdminFriendLinkVO.from(
            statusService.update(
                    principal, id, request.toCommand())));
}

@PutMapping("/sort-orders")
public ApiResponse<List<AdminFriendLinkVO>> updateSortOrders(
        @CurrentUser AuthenticatedPrincipal principal,
        @RequestBody UpdateFriendLinkSortOrdersRequest request) {
    return ApiResponse.ok(sortService.update(
                    principal, request.toCommand())
            .stream()
            .map(AdminFriendLinkVO::from)
            .toList());
}

@DeleteMapping("/{id:\\d+}")
public ApiResponse<Void> delete(
        @CurrentUser AuthenticatedPrincipal principal,
        @PathVariable long id) {
    deleteService.delete(principal, id);
    return ApiResponse.ok(null);
}
```

确认静态 `/sort-orders` 声明和 `{id:\d+}` 不冲突。

### Step 10：扩展 Security 测试

- [ ] `SecurityConfigTest` 对 DEMO 验证：

- PATCH status 403。
- PUT sort-orders 403。
- DELETE 403。
- ADMIN 请求可进入 Controller，并因缺失/非法 body 返回 400 或目标不存在 404，
  不能在 Security 层被拒绝。

### Step 11：定向验证和软删除静态检查

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkSortDeleteServiceTest,DatabaseFriendLinkRepositoryTest,FriendLinkConcurrencyTest,AdminFriendLinkControllerTest,SecurityConfigTest,ArchitectureRulesTest" test
rg -n "deleteById|removeById|@TableLogic" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
git diff --check
```

预期：friendlink 不使用通用删除；SQL 仅在 XML；测试通过。

### Step 12：提交 Task 4

- [ ] 执行：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
git commit -m "实现友链状态排序与软删除"
```

---

## Task 5：完成友链纵向切片

**提交信息：** `完成友链纵向切片`

### Step 1：写完整 HTTP 集成测试

- [ ] 新建 `FriendLinkIntegrationTest.java`，使用真实 Spring context、H2、JWT、
MockMvc。

`@BeforeEach`：

- 删除 `t_friend_link`、refresh token、user info、user auth。
- 插入 ADMIN、DEMO 账号。
- 插入 VISIBLE、HIDDEN、deleted 友链。

完整流程覆盖：

1. 匿名公开 GET 只返回 VISIBLE active，并按 sortOrder/id 排序。
2. DEMO 可查看后台分页和详情。
3. DEMO 的 POST/PUT/PATCH/sort/DELETE 全部 403。
4. ADMIN POST 创建，数据库审计 createdBy/updatedBy 为 ADMIN。
5. ADMIN PUT 完整编辑，可显式清空 avatarUrl/description。
6. PATCH HIDDEN 后公开列表不可见，再 PATCH VISIBLE 后恢复公开。
7. 批量排序后公开和后台顺序更新。
8. DELETE 后公开、后台列表和详情不可见。
9. 数据库删除审计五字段完整。
10. 重复 URL 可创建两条。
11. 缺字段、未知字段、非法 URL、重复排序 ID 返回 400。
12. 排序包含不存在 ID 时全批回滚。

### Step 2：写 OpenAPI 契约测试

- [ ] 新建 `FriendLinkOpenApiTest.java`：

- `/api/public/friend-links` 只有 GET。
- `/api/admin/friend-links` 只有 GET、POST。
- `/{id}` 只有 GET、PUT、DELETE。
- `/{id}/status` 只有 PATCH。
- `/sort-orders` 只有 PUT。
- 公开 VO 字段精确为 id/name/url/avatarUrl/description。
- 后台 VO 包含完整业务与创建/更新审计。
- 不含 deleted/deletedAt/deletedBy/Entity/SubmittedField。
- POST/PUT 六字段全部 required，avatarUrl/description 为 string|null。
- 状态 schema 只包含 VISIBLE/HIDDEN。
- 分页 schema 为 records/total/page/size。

### Step 3：运行新增测试确认 RED 或发现契约差异

- [ ] 执行：

```powershell
mvn "-Dtest=FriendLinkIntegrationTest,FriendLinkOpenApiTest" test
```

预期：若前四批完整，集成行为应基本通过；OpenAPI 可能暴露实际 schema 差异，按设计
修正 Controller 注解或独立文档模型，不放宽核心断言。

### Step 4：完成接口文档

- [ ] 新建 `docs/project-handbook/api-contract/friend-link.md`，记录：

- 七个接口。
- ADMIN/DEMO/匿名权限矩阵。
- 公开与后台字段差异。
- VISIBLE/HIDDEN 和 deleted 的语义区别。
- 字段长度、HTTP/HTTPS URL 和 URL 可重复规则。
- 批量排序 1..100、重复/缺失 ID 整体回滚。
- 软删除五字段审计。
- 不提供申请审核、恢复和回收站。

- [ ] 更新 `api-contract/README.md`：

```markdown
| `friend-link.md` | 公开友链、后台 CRUD、状态、排序和软删除 | ✅ 已落地 |
```

### Step 5：更新项目状态

- [ ] 更新：

- `docs/project-handbook/status.md`
- `docs/project-handbook/roadmap.md`
- `docs/project-handbook/m3-preflight-review.md`
- `docs/superpowers/specs/2026-06-14-backend-v2-friend-link-design.md`
- 本计划

记录 Task 1 至 Task 4 的真实 SHA，设计状态改为“已实施”。system 后端模块标记完成，
下一步改为 `content` 模块规格与纵向切片设计。Task 5 不预写自身 SHA。

P2-5 “逻辑删除审计没有统一路径”更新为：

- [x] 友链标准业务表已建立显式软删除路径。
- 后续标准业务表复用同一规则，禁止通用 delete 方法。

不要声称所有尚未实现的业务表已经覆盖。

### Step 6：运行全量验证

- [ ] 执行：

```powershell
git diff --check
mvn clean test
```

预期：

- BUILD SUCCESS。
- 0 failures。
- 0 errors。
- Docker 不可用时仅既有 MySQL Testcontainers 条件测试 skipped。

将真实 tests/failures/errors/skipped 数量写回 status、spec 和 plan。

### Step 7：最终静态审查

- [ ] 执行：

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
rg -n "deleteById|removeById" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
rg -n "Entity|Mapper|SubmittedField" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/friendlink
rg -n "t_article|t_comment" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
git status --short
```

预期：

- 无注解 SQL。
- friendlink 不调用通用删除。
- application 不依赖 Entity、Mapper 或 Web presence 类型。
- system 友链不跨模块访问 content/comment 表。
- 提交前只有 Task 5 集成测试、OpenAPI 和文档变更。

### Step 8：提交 Task 5

- [ ] 执行：

```powershell
git add MyBlog-springboot-v2/src/test docs/project-handbook docs/superpowers/specs/2026-06-14-backend-v2-friend-link-design.md docs/superpowers/plans/2026-06-14-backend-v2-friend-link.md
git commit -m "完成友链纵向切片"
```

### Step 9：最终确认

- [ ] 执行：

```powershell
git status --short
git log -7 --oneline
```

预期工作区干净，最近相关提交依次为：

1. `设计友链纵向切片`
2. `制定友链纵向切片实施计划`
3. `建立友链领域与持久化查询`
4. `实现公开与后台友链查询`
5. `实现友链新增与编辑`
6. `实现友链状态排序与软删除`
7. `完成友链纵向切片`

---

## 完成标准

- [ ] 公开接口只返回 VISIBLE active 友链。
- [ ] ADMIN/DEMO 可读，DEMO 所有写操作被拒绝。
- [ ] ADMIN 可新增、完整编辑、显示、隐藏、批量排序和软删除。
- [ ] URL 和 avatarUrl 只接受绝对 HTTP/HTTPS，URL 允许重复。
- [ ] 批量排序最多 100 项，重复或缺失 ID 整体回滚。
- [ ] 更新、状态、排序和删除使用 active 行锁。
- [ ] 软删除写入 deleted/deletedAt/deletedBy/updatedAt/updatedBy。
- [ ] 不使用 deleteById，不修改 V1 DDL。
- [ ] 所有 SQL 在 Mapper XML。
- [ ] 分页为 records/total/page/size。
- [ ] OpenAPI 不暴露 Entity、SubmittedField 或删除审计字段。
- [ ] system 友链不跨模块访问 content/comment。
- [ ] 五个实现 Task 分别形成中文本地提交。
- [ ] 全量 Maven、Enforcer、ArchUnit 和 `git diff --check` 通过。
