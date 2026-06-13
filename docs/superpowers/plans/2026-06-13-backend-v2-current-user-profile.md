# Backend V2 Current User Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地 `t_user_info` 持久化，并提供 ADMIN/DEMO 查询当前用户资料、仅 ADMIN 部分更新本人资料的 HTTP 接口。

**Architecture:** 在 `identity` 模块内新增独立的 `profile` 领域与应用包，查询时组合当前账号投影和 `UserProfile`，更新时使用 presence-aware `PatchValue<T>` 合并完整资料。自定义查询与更新 SQL 全部写入 MyBatis XML；Web 层通过自定义 Jackson setter 表达 PATCH 的未出现、赋值和显式清空三态。

**Tech Stack:** Java 17、Spring Boot 3、Spring Security、MyBatis-Plus、MyBatis XML、Flyway、Jackson、Lombok、JUnit 5、Mockito、MockMvc、AssertJ、H2 MySQL mode、Testcontainers MySQL。

---

## 0. 执行约束

- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`
- 后端目录：`E:\My-Blog\.worktrees\backend-v2-refactor\MyBlog-springboot-v2`
- 每个 Task 完成测试后单独提交，提交信息必须使用中文。
- 不修改已冻结的 `V1__init.sql`；数据补齐使用 `V2__backfill_user_info.sql`。
- Java 文件必须有中文类级 Javadoc；非显然的公开方法补中文 Javadoc。
- 手写 SQL 必须放在 `src/main/resources/mapper/identity/*.xml`，禁止 `@Select`、`@Update`。
- 本计划不实现修改密码、头像上传、公开个人主页或按 ID 管理用户。
- 每完成一个 Task，勾选本文件对应 checkbox 后与该 Task 一起提交。

## 1. 文件结构

### Task 1：资料领域、迁移和持久化

**Create**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/profile/UserProfile.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/profile/UserProfileRepository.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/UserProfileEntity.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserProfileMapper.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisUserProfileRepository.java`
- `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserProfileMapper.xml`
- `MyBlog-springboot-v2/src/main/resources/db/migration/V2__backfill_user_info.sql`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/profile/UserProfileTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseUserProfileRepositoryTest.java`

**Modify**

- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/infrastructure/persistence/FlywayMigrationTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/infrastructure/persistence/MySqlFlywayMigrationTest.java`

### Task 2：当前用户资料查询

**Create**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/CurrentAccount.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/CurrentAccountRepository.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileResult.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileQueryService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisCurrentAccountRepository.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileQueryServiceTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseCurrentAccountRepositoryTest.java`

**Modify**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java`
- `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml`

### Task 3：PATCH 合并与资料更新

**Create**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/PatchValue.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/UpdateCurrentUserProfileCommand.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileUpdateService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/profile/ProfileFieldPatch.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/profile/UserProfilePatch.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileUpdateServiceTest.java`

**Modify**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/profile/UserProfile.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/profile/UserProfileRepository.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserProfileMapper.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisUserProfileRepository.java`
- `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserProfileMapper.xml`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/profile/UserProfileTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseUserProfileRepositoryTest.java`

### Task 4：HTTP 接口、权限与端到端验收

**Create**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/CurrentUserController.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/CurrentUserVO.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/UserProfileVO.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/UpdateCurrentUserProfileRequest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/CurrentUserControllerTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/integration/CurrentUserProfileIntegrationTest.java`

**Modify**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`

### Task 5：契约、状态与全量验证

**Modify**

- `docs/project-handbook/api-contract/auth.md`
- `docs/project-handbook/api-contract/README.md`
- `docs/project-handbook/status.md`
- `docs/project-handbook/roadmap.md`
- `docs/superpowers/plans/2026-06-13-backend-v2-current-user-profile.md`

---

## Task 1：资料领域、迁移和持久化

**提交信息：** `落地用户资料持久化`

- [x] **Step 1：先写 `UserProfile` 领域测试**

创建 `UserProfileTest`，至少覆盖：

```java
@Test
void shouldNormalizeOptionalBlankValuesToNull() {
    UserProfile profile = UserProfile.create(
            1001L, " TYB ", " ", null, null, null,
            " Tokyo ", null, null, null, null, null, null, null, null);

    assertThat(profile.nickname()).isEqualTo("TYB");
    assertThat(profile.avatarUrl()).isNull();
    assertThat(profile.location()).isEqualTo("Tokyo");
}

@Test
void shouldRejectBlankNickname() {
    assertThatThrownBy(() -> UserProfile.create(
            1001L, " ", null, null, null, null,
            null, null, null, null, null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("昵称不能为空");
}
```

再覆盖昵称 65 字符、简介 5001 字符、公开邮箱格式错误、非 HTTP/HTTPS URL。

- [x] **Step 2：运行领域测试并确认失败**

Run:

```powershell
mvn -Dtest=UserProfileTest test
```

Expected: FAIL，原因是 `UserProfile` 尚不存在。

- [x] **Step 3：实现 `UserProfile` 和只读仓储端口**

`UserProfile` 使用 record，字段必须与 schema 一一对应：

```java
public record UserProfile(
        long userId,
        String nickname,
        String avatarUrl,
        String bioZh,
        String bioJa,
        String bioEn,
        String location,
        String website,
        String emailPublic,
        String githubUrl,
        String twitterUrl,
        String linkedinUrl,
        String zhihuUrl,
        String qiitaUrl,
        String juejinUrl
) {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public static UserProfile create(
            long userId,
            String nickname,
            String avatarUrl,
            String bioZh,
            String bioJa,
            String bioEn,
            String location,
            String website,
            String emailPublic,
            String githubUrl,
            String twitterUrl,
            String linkedinUrl,
            String zhihuUrl,
            String qiitaUrl,
            String juejinUrl) {
        if (userId <= 0) {
            throw new IllegalArgumentException("用户 ID 必须为正数");
        }
        String normalizedNickname = required(nickname, "昵称", 64);
        return new UserProfile(
                userId,
                normalizedNickname,
                url(avatarUrl, "头像 URL"),
                optional(bioZh, "中文简介", 5000),
                optional(bioJa, "日文简介", 5000),
                optional(bioEn, "英文简介", 5000),
                optional(location, "所在地", 64),
                url(website, "个人主页"),
                email(emailPublic),
                url(githubUrl, "GitHub 链接"),
                url(twitterUrl, "Twitter 链接"),
                url(linkedinUrl, "LinkedIn 链接"),
                url(zhihuUrl, "知乎链接"),
                url(qiitaUrl, "Qiita 链接"),
                url(juejinUrl, "掘金链接"));
    }

    private static String required(String value, String field, int maxLength) {
        String normalized = optional(value, field, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return normalized;
    }

    private static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private static String email(String value) {
        String normalized = optional(value, "公开邮箱", 128);
        if (normalized != null
                && !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("公开邮箱格式错误");
        }
        return normalized;
    }

    private static String url(String value, String field) {
        String normalized = optional(value, field, 255);
        if (normalized == null) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + "格式错误");
        }
        if (uri.getHost() == null
                || (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException(field + "仅支持 HTTP 或 HTTPS");
        }
        return normalized;
    }
}
```

规则：

- `userId > 0`
- `nickname` trim 后非空且不超过 64
- `location` 不超过 64
- `emailPublic` 不超过 128，使用基础邮箱正则 `^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$`
- URL 字段不超过 255，`URI` 的 scheme 只能为 `http`/`https` 且 host 非空
- 三个 bio 各不超过 5000
- 所有可选字符串 trim 后为空统一变成 `null`

仓储端口先定义：

```java
public interface UserProfileRepository {
    Optional<UserProfile> findActiveByUserId(long userId);
    void insert(UserProfile profile);
}
```

- [x] **Step 4：运行领域测试并确认通过**

Run:

```powershell
mvn -Dtest=UserProfileTest test
```

Expected: PASS。

- [x] **Step 5：写迁移和仓储失败测试**

在 `FlywayMigrationTest` 增加断言：迁移版本数量为 2，并验证 V2 不会凭空创建账号。

在 `MySqlFlywayMigrationTest`：

- 把 `migrationsExecuted` 期望值从 1 改为 2。
- 在只执行 V1 后手工插入一个 `t_user_auth`，再执行 V2，断言自动生成 `t_user_info` 且 `nickname = username`。
- 再次执行 `flyway.migrate()`，断言不会重复插入。

创建 `DatabaseUserProfileRepositoryTest`，覆盖：

- `shouldReadActiveProfile`：插入账号与资料，断言 15 个领域字段完整映射。
- `shouldIgnoreDeletedProfile`：把资料的 `deleted` 设为 1，断言返回 `Optional.empty()`。
- `shouldInsertProfileWithAuditColumns`：通过仓储插入资料，断言 `user_id`、`nickname`、`created_at`、`updated_at` 已写入。

清理顺序必须先 `DELETE FROM t_user_info`，再删 `t_user_auth`。

- [x] **Step 6：运行迁移和仓储测试并确认失败**

Run:

```powershell
mvn -Dtest=FlywayMigrationTest,MySqlFlywayMigrationTest,DatabaseUserProfileRepositoryTest test
```

Expected: FAIL，原因是 V2 迁移、Entity、Mapper 和仓储实现尚不存在。Docker 未运行时 MySQL 测试允许按现有条件跳过。

- [x] **Step 7：实现 V2 数据补齐迁移**

`V2__backfill_user_info.sql` 内容：

```sql
INSERT INTO t_user_info (
    user_id,
    nickname,
    created_at,
    created_by,
    updated_at,
    updated_by,
    deleted
)
SELECT ua.id,
       ua.username,
       CURRENT_TIMESTAMP,
       ua.id,
       CURRENT_TIMESTAMP,
       ua.id,
       0
FROM t_user_auth ua
WHERE ua.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM t_user_info ui
      WHERE ui.user_id = ua.id
  );
```

说明：首次部署仍应由运维 SQL在同一事务中同时插入账号和资料；V2 仅修复升级时已经存在的账号。

- [x] **Step 8：实现 Entity、XML Mapper 和仓储**

`UserProfileEntity`：

```java
@Getter
@Setter
@TableName("t_user_info")
public class UserProfileEntity extends AuditOnlyBase {
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private String bioZh;
    private String bioJa;
    private String bioEn;
    private String location;
    private String website;
    private String emailPublic;
    private String githubUrl;
    private String twitterUrl;
    private String linkedinUrl;
    private String zhihuUrl;
    private String qiitaUrl;
    private String juejinUrl;
}
```

`UserProfileMapper` 继承 `BaseMapper<UserProfileEntity>`，只声明：

```java
UserProfileEntity selectActiveByUserId(@Param("userId") long userId);
```

`UserProfileMapper.xml` 明确列出全部业务字段和审计字段，并使用 `deleted = 0`。禁止 `SELECT *`。

`MyBatisUserProfileRepository`：

- `findActiveByUserId` 把 Entity 转为 `UserProfile.create(...)`
- `insert` 使用 `mapper.insert(entity)`，依赖现有 `AuditFieldHandler`
- 插入行数不是 1 时抛出 `IllegalStateException("用户资料创建失败，userId=" + userId)`

- [x] **Step 9：运行 Task 1 测试**

Run:

```powershell
mvn -Dtest=UserProfileTest,FlywayMigrationTest,MySqlFlywayMigrationTest,DatabaseUserProfileRepositoryTest test
```

Expected: PASS；Docker 不可用时仅 Testcontainers 用例跳过。

- [x] **Step 10：检查并提交 Task 1**

Run:

```powershell
git diff --check
git status --short
```

然后勾选本 Task，提交：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-13-backend-v2-current-user-profile.md
git commit -m "落地用户资料持久化"
```

---

## Task 2：当前用户资料查询

**提交信息：** `实现当前用户资料查询`

- [x] **Step 1：写当前账号仓储和查询服务失败测试**

`DatabaseCurrentAccountRepositoryTest` 覆盖：

- 按 ID 读取未删除账号的 `id`、`username`、`AccountType`
- 软删除账号返回空

`CurrentUserProfileQueryServiceTest` 使用 Mockito 覆盖：

```java
@Test
void shouldCombineAccountAndProfile() {
    when(accountRepository.findActiveById(1001L))
            .thenReturn(Optional.of(new CurrentAccount(1001L, "admin", AccountType.ADMIN)));
    when(profileRepository.findActiveByUserId(1001L))
            .thenReturn(Optional.of(profile));

    CurrentUserProfileResult result = service.query("1001");

    assertThat(result.username()).isEqualTo("admin");
    assertThat(result.type()).isEqualTo(AccountType.ADMIN);
    assertThat(result.profile()).isEqualTo(profile);
}
```

另覆盖主体 ID 为 null、空白、非数字、0、负数；账号缺失；资料缺失。后三类异常断言 `ApiErrorCode.INTERNAL_ERROR`，无效主体 ID 断言 `INVALID_TOKEN`。

- [x] **Step 2：运行测试并确认失败**

Run:

```powershell
mvn -Dtest=DatabaseCurrentAccountRepositoryTest,CurrentUserProfileQueryServiceTest test
```

Expected: FAIL，原因是查询投影、仓储和应用服务尚不存在。

- [x] **Step 3：实现当前账号投影和 XML 查询**

领域类型：

```java
public record CurrentAccount(long id, String username, AccountType type) {
}

public interface CurrentAccountRepository {
    Optional<CurrentAccount> findActiveById(long userId);
}
```

在 `UserAccountMapper` 增加：

```java
UserAccountEntity selectActiveById(@Param("userId") long userId);
```

在 `UserAccountMapper.xml` 增加：

```xml
<select id="selectActiveById"
        resultType="com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserAccountEntity">
    SELECT id,
           username,
           type
    FROM t_user_auth
    WHERE id = #{userId}
      AND deleted = 0
</select>
```

`MyBatisCurrentAccountRepository` 把数据库 `type` 通过 `AccountType.fromDatabaseValue` 转为领域枚举。

- [x] **Step 4：实现查询结果和应用服务**

结果类型：

```java
public record CurrentUserProfileResult(
        long id,
        String username,
        AccountType type,
        UserProfile profile
) {
}
```

`CurrentUserProfileQueryService.query(String principalId)`：

1. 严格解析正 long。
2. 无效主体 ID 抛 `ApiException(INVALID_TOKEN)`。
3. 分别查询账号和资料。
4. 任一缺失时用 SLF4J 记录中文错误：

```java
log.error(
        "当前用户资料数据不完整，userId={}，accountPresent={}，profilePresent={}",
        userId, account.isPresent(), profile.isPresent());
```

5. 对外抛 `ApiException(INTERNAL_ERROR)`。
6. 返回组合结果，不返回密码、token version 或锁定字段。

- [x] **Step 5：运行 Task 2 测试**

Run:

```powershell
mvn -Dtest=DatabaseCurrentAccountRepositoryTest,CurrentUserProfileQueryServiceTest test
```

Expected: PASS。

- [x] **Step 6：检查并提交 Task 2**

Run:

```powershell
git diff --check
```

勾选本 Task 后提交：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-13-backend-v2-current-user-profile.md
git commit -m "实现当前用户资料查询"
```

---

## Task 3：PATCH 合并与资料更新

**提交信息：** `实现用户资料部分更新`

- [ ] **Step 1：写 `PatchValue`、合并和更新服务失败测试**

`PatchValue` 测试可放入 `CurrentUserProfileUpdateServiceTest`，必须断言：

- `PatchValue.absent().present()` 为 false
- `PatchValue.of(null).present()` 为 true 且 value 为 null

扩展 `UserProfileTest`：

- 未出现字段保持旧值
- 可选字段显式 null 清空
- 可选字段空白清空
- nickname 显式 null 或空白失败
- 合并后的 URL、邮箱和长度仍执行完整校验

`CurrentUserProfileUpdateServiceTest` 覆盖：

- ADMIN 成功更新并返回更新后资料
- DEMO 在仓储查询前返回 `FORBIDDEN`
- 空 patch 返回 `VALIDATION_ERROR`
- 资料缺失返回 `INTERNAL_ERROR`
- 更新行数异常返回 `INTERNAL_ERROR`

- [ ] **Step 2：运行测试并确认失败**

Run:

```powershell
mvn -Dtest=UserProfileTest,CurrentUserProfileUpdateServiceTest test
```

Expected: FAIL，原因是 PATCH 类型和更新服务尚不存在。

- [ ] **Step 3：实现 `PatchValue` 和更新命令**

```java
public record PatchValue<T>(boolean present, T value) {
    public static <T> PatchValue<T> absent() {
        return new PatchValue<>(false, null);
    }

    public static <T> PatchValue<T> of(T value) {
        return new PatchValue<>(true, value);
    }
}
```

`UpdateCurrentUserProfileCommand` 明确定义 14 个可编辑字段：

```java
public record UpdateCurrentUserProfileCommand(
        PatchValue<String> nickname,
        PatchValue<String> avatarUrl,
        PatchValue<String> bioZh,
        PatchValue<String> bioJa,
        PatchValue<String> bioEn,
        PatchValue<String> location,
        PatchValue<String> website,
        PatchValue<String> emailPublic,
        PatchValue<String> githubUrl,
        PatchValue<String> twitterUrl,
        PatchValue<String> linkedinUrl,
        PatchValue<String> zhihuUrl,
        PatchValue<String> qiitaUrl,
        PatchValue<String> juejinUrl
) {
    public UpdateCurrentUserProfileCommand {
        nickname = normalize(nickname);
        avatarUrl = normalize(avatarUrl);
        bioZh = normalize(bioZh);
        bioJa = normalize(bioJa);
        bioEn = normalize(bioEn);
        location = normalize(location);
        website = normalize(website);
        emailPublic = normalize(emailPublic);
        githubUrl = normalize(githubUrl);
        twitterUrl = normalize(twitterUrl);
        linkedinUrl = normalize(linkedinUrl);
        zhihuUrl = normalize(zhihuUrl);
        qiitaUrl = normalize(qiitaUrl);
        juejinUrl = normalize(juejinUrl);
    }

    public boolean hasAnyPresentField() {
        return nickname.present()
                || avatarUrl.present()
                || bioZh.present()
                || bioJa.present()
                || bioEn.present()
                || location.present()
                || website.present()
                || emailPublic.present()
                || githubUrl.present()
                || twitterUrl.present()
                || linkedinUrl.present()
                || zhihuUrl.present()
                || qiitaUrl.present()
                || juejinUrl.present();
    }

    private static PatchValue<String> normalize(PatchValue<String> value) {
        return value == null ? PatchValue.absent() : value;
    }
}
```

- [ ] **Step 4：扩展领域合并能力**

在 `UserProfile` 增加：

```java
public UserProfile apply(UserProfilePatch patch) {
    return UserProfile.create(
            userId,
            patch.nickname().present() ? patch.nickname().value() : nickname,
            patch.avatarUrl().present() ? patch.avatarUrl().value() : avatarUrl,
            patch.bioZh().present() ? patch.bioZh().value() : bioZh,
            patch.bioJa().present() ? patch.bioJa().value() : bioJa,
            patch.bioEn().present() ? patch.bioEn().value() : bioEn,
            patch.location().present() ? patch.location().value() : location,
            patch.website().present() ? patch.website().value() : website,
            patch.emailPublic().present() ? patch.emailPublic().value() : emailPublic,
            patch.githubUrl().present() ? patch.githubUrl().value() : githubUrl,
            patch.twitterUrl().present() ? patch.twitterUrl().value() : twitterUrl,
            patch.linkedinUrl().present() ? patch.linkedinUrl().value() : linkedinUrl,
            patch.zhihuUrl().present() ? patch.zhihuUrl().value() : zhihuUrl,
            patch.qiitaUrl().present() ? patch.qiitaUrl().value() : qiitaUrl,
            patch.juejinUrl().present() ? patch.juejinUrl().value() : juejinUrl);
}
```

`UserProfile` 只能依赖同包的领域类型，禁止 import `application`。

- [ ] **Step 5：先补 `UserProfilePatch` 领域类型**

创建：

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/profile/ProfileFieldPatch.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/profile/UserProfilePatch.java`

领域层使用：

```java
public record ProfileFieldPatch(boolean present, String value) {
    public static ProfileFieldPatch absent() { return new ProfileFieldPatch(false, null); }
    public static ProfileFieldPatch of(String value) { return new ProfileFieldPatch(true, value); }
}
```

`UserProfilePatch` 明确定义 14 个 `ProfileFieldPatch`：

```java
public record UserProfilePatch(
        ProfileFieldPatch nickname,
        ProfileFieldPatch avatarUrl,
        ProfileFieldPatch bioZh,
        ProfileFieldPatch bioJa,
        ProfileFieldPatch bioEn,
        ProfileFieldPatch location,
        ProfileFieldPatch website,
        ProfileFieldPatch emailPublic,
        ProfileFieldPatch githubUrl,
        ProfileFieldPatch twitterUrl,
        ProfileFieldPatch linkedinUrl,
        ProfileFieldPatch zhihuUrl,
        ProfileFieldPatch qiitaUrl,
        ProfileFieldPatch juejinUrl
) {
}
```

在 `UpdateCurrentUserProfileCommand` 增加 `toDomainPatch()`，逐字段把 `PatchValue<String>` 转成 `ProfileFieldPatch`。应用层的泛型 `PatchValue<String>` 只承担 Web/Application 边界表达。

- [ ] **Step 6：扩展仓储的加锁查询和 XML 更新**

仓储端口增加：

```java
Optional<UserProfile> findActiveByUserIdForUpdate(long userId);
boolean update(UserProfile profile);
```

Mapper 增加：

```java
UserProfileEntity selectActiveByUserIdForUpdate(@Param("userId") long userId);

int updateActiveProfile(
        @Param("profile") UserProfileEntity profile,
        @Param("updatedAt") LocalDateTime updatedAt,
        @Param("updatedBy") Long updatedBy);
```

XML：

```xml
<select id="selectActiveByUserIdForUpdate"
        resultType="com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserProfileEntity">
    SELECT user_id, nickname, avatar_url, bio_zh, bio_ja, bio_en,
           location, website, email_public, github_url, twitter_url,
           linkedin_url, zhihu_url, qiita_url, juejin_url,
           created_at, created_by, updated_at, updated_by,
           deleted, deleted_at, deleted_by
    FROM t_user_info
    WHERE user_id = #{userId}
      AND deleted = 0
    FOR UPDATE
</select>
```

`updateActiveProfile` 必须显式更新所有可编辑列，允许把 nullable 列写为 `NULL`，并带：

```sql
updated_at = #{updatedAt},
updated_by = #{updatedBy}
WHERE user_id = #{profile.userId}
  AND deleted = 0
```

`MyBatisUserProfileRepository` 注入项目 `Clock` 和 `SecurityContextAuditor`，用相同时间源和当前审计用户调用 XML 更新；影响行数为 1 返回 true，否则 false。

- [ ] **Step 7：实现事务更新服务**

`CurrentUserProfileUpdateService.update(AuthenticatedPrincipal principal, UpdateCurrentUserProfileCommand command)`：

```java
@Transactional
public UserProfile update(
        AuthenticatedPrincipal principal,
        UpdateCurrentUserProfileCommand command) {
    requireAdmin(principal);
    long userId = parsePrincipalId(principal.id());
    if (!command.hasAnyPresentField()) {
        throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "至少提交一个资料字段");
    }
    UserProfile current = repository.findActiveByUserIdForUpdate(userId)
            .orElseThrow(() -> profileMissing(userId));
    UserProfile updated;
    try {
        updated = current.apply(command.toDomainPatch());
    } catch (IllegalArgumentException exception) {
        throw new ApiException(ApiErrorCode.VALIDATION_ERROR, exception.getMessage());
    }
    if (!repository.update(updated)) {
        log.error("当前用户资料更新行数异常，userId={}", userId);
        throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
    return updated;
}
```

`requireAdmin` 使用 `principal.roles().contains("ADMIN")`。DEMO 必须在任何仓储调用前失败。

- [ ] **Step 8：运行 Task 3 测试**

Run:

```powershell
mvn -Dtest=UserProfileTest,CurrentUserProfileUpdateServiceTest,DatabaseUserProfileRepositoryTest test
```

Expected: PASS。

- [ ] **Step 9：检查并提交 Task 3**

Run:

```powershell
git diff --check
```

勾选本 Task后提交：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-13-backend-v2-current-user-profile.md
git commit -m "实现用户资料部分更新"
```

---

## Task 4：HTTP 接口、权限与端到端验收

**提交信息：** `开放当前用户资料接口`

- [ ] **Step 1：写 Controller 失败测试**

`CurrentUserControllerTest` 使用 `@WebMvcTest(CurrentUserController.class)`、`addFilters = false` 和 `GlobalExceptionHandler`，覆盖：

- GET 返回 `id`、`username`、`type` 和完整 profile
- GET 不出现 passwordHash、tokenVersion、loginFailCount、lockedUntil
- PATCH 只传 nickname 时其他字段在命令中为 absent
- PATCH 显式 `twitterUrl: null` 时对应字段为 present/null
- PATCH 空对象返回 `400 + 90001`
- PATCH 未知字段返回 `400 + 90001`
- PATCH nickname 空白返回 `400 + 90001`
- 应用服务返回 `FORBIDDEN` 时响应 `403 + 10003`

- [ ] **Step 2：写 Security 失败测试**

在 `SecurityConfigTest` 增加：

- ADMIN 可 PATCH `/api/auth/me/profile`
- DEMO PATCH 返回 `403 + 10003`
- ADMIN 和 DEMO 均可 GET `/api/auth/me`
- 未认证 GET/PATCH 均返回 `401 + 10002`

Security 配置增加精确 matcher：

```java
.requestMatchers(HttpMethod.PATCH, "/api/auth/me/profile").hasRole("ADMIN")
```

该规则放在 `.anyRequest().authenticated()` 前。

- [ ] **Step 3：运行 Web 和 Security 测试并确认失败**

Run:

```powershell
mvn -Dtest=CurrentUserControllerTest,SecurityConfigTest test
```

Expected: FAIL，原因是 Controller、DTO 和精确权限规则尚不存在。

- [ ] **Step 4：实现 presence-aware 请求 DTO**

`UpdateCurrentUserProfileRequest` 使用 Lombok `@Getter`，每个字段初始化为 absent：

```java
@Getter
public class UpdateCurrentUserProfileRequest {
    private PatchValue<String> nickname = PatchValue.absent();
    private PatchValue<String> avatarUrl = PatchValue.absent();
    private PatchValue<String> bioZh = PatchValue.absent();
    private PatchValue<String> bioJa = PatchValue.absent();
    private PatchValue<String> bioEn = PatchValue.absent();
    private PatchValue<String> location = PatchValue.absent();
    private PatchValue<String> website = PatchValue.absent();
    private PatchValue<String> emailPublic = PatchValue.absent();
    private PatchValue<String> githubUrl = PatchValue.absent();
    private PatchValue<String> twitterUrl = PatchValue.absent();
    private PatchValue<String> linkedinUrl = PatchValue.absent();
    private PatchValue<String> zhihuUrl = PatchValue.absent();
    private PatchValue<String> qiitaUrl = PatchValue.absent();
    private PatchValue<String> juejinUrl = PatchValue.absent();

    @JsonSetter("nickname")
    public void setNickname(String value) {
        nickname = PatchValue.of(value);
    }

    @JsonSetter("avatarUrl")
    public void setAvatarUrl(String value) {
        avatarUrl = PatchValue.of(value);
    }

    @JsonSetter("bioZh")
    public void setBioZh(String value) { bioZh = PatchValue.of(value); }

    @JsonSetter("bioJa")
    public void setBioJa(String value) { bioJa = PatchValue.of(value); }

    @JsonSetter("bioEn")
    public void setBioEn(String value) { bioEn = PatchValue.of(value); }

    @JsonSetter("location")
    public void setLocation(String value) { location = PatchValue.of(value); }

    @JsonSetter("website")
    public void setWebsite(String value) { website = PatchValue.of(value); }

    @JsonSetter("emailPublic")
    public void setEmailPublic(String value) { emailPublic = PatchValue.of(value); }

    @JsonSetter("githubUrl")
    public void setGithubUrl(String value) { githubUrl = PatchValue.of(value); }

    @JsonSetter("twitterUrl")
    public void setTwitterUrl(String value) { twitterUrl = PatchValue.of(value); }

    @JsonSetter("linkedinUrl")
    public void setLinkedinUrl(String value) { linkedinUrl = PatchValue.of(value); }

    @JsonSetter("zhihuUrl")
    public void setZhihuUrl(String value) { zhihuUrl = PatchValue.of(value); }

    @JsonSetter("qiitaUrl")
    public void setQiitaUrl(String value) { qiitaUrl = PatchValue.of(value); }

    @JsonSetter("juejinUrl")
    public void setJuejinUrl(String value) { juejinUrl = PatchValue.of(value); }

    @JsonAnySetter
    public void rejectUnknown(String name, JsonNode value) {
        throw new IllegalArgumentException("不支持的资料字段：" + name);
    }

    public UpdateCurrentUserProfileCommand toCommand() {
        return new UpdateCurrentUserProfileCommand(
                nickname,
                avatarUrl,
                bioZh,
                bioJa,
                bioEn,
                location,
                website,
                emailPublic,
                githubUrl,
                twitterUrl,
                linkedinUrl,
                zhihuUrl,
                qiitaUrl,
                juejinUrl);
    }
}
```

这些 setter 不能用 Lombok 自动生成，因为 Jackson 是否调用 setter 就是 presence 信息。

- [ ] **Step 5：实现 VO 和 Controller**

`UserProfileVO` 与 `UserProfile` 字段一致但不暴露 `userId`。

`CurrentUserVO`：

```java
public record CurrentUserVO(
        long id,
        String username,
        AccountType type,
        UserProfileVO profile
) {
}
```

`CurrentUserController`：

```java
@Tag(name = "当前用户", description = "当前后台账号与个人资料")
@RestController
@RequestMapping("/api/auth/me")
@RequiredArgsConstructor
public class CurrentUserController {
    private final CurrentUserProfileQueryService queryService;
    private final CurrentUserProfileUpdateService updateService;

    @GetMapping
    public ApiResponse<CurrentUserVO> get(
            @CurrentUser AuthenticatedPrincipal principal) {
        return ApiResponse.ok(toView(queryService.query(principal.id())));
    }

    @PatchMapping("/profile")
    public ApiResponse<UserProfileVO> update(
            @CurrentUser AuthenticatedPrincipal principal,
            @RequestBody UpdateCurrentUserProfileRequest request) {
        return ApiResponse.ok(toView(
                updateService.update(principal, request.toCommand())));
    }
}
```

空对象校验由 `UpdateCurrentUserProfileCommand.hasAnyPresentField()` 在应用层完成。Jackson setter 抛出的未知字段异常应被包装为 `HttpMessageNotReadableException`，由现有全局处理器映射为 `90001`。

- [ ] **Step 6：运行 Web 和 Security 测试**

Run:

```powershell
mvn -Dtest=CurrentUserControllerTest,SecurityConfigTest test
```

Expected: PASS。

- [ ] **Step 7：写完整 HTTP 集成测试**

`CurrentUserProfileIntegrationTest` 使用真实 Spring context、MockMvc、H2、JWT 登录链路：

1. 每个测试先清理 `t_refresh_token`、`t_user_info`、`t_user_auth`。
2. 插入 ADMIN/DEMO 账号时同步插入资料行。
3. ADMIN 登录后 GET，断言完整资料。
4. ADMIN PATCH：
   - 修改 nickname
   - 显式清空 twitterUrl
   - 未提交 website，断言保持旧值
   - 查询数据库确认 `updated_at` 和 `updated_by = adminId`
5. DEMO GET 成功，PATCH 返回 `403 + 10003` 且数据库不变。
6. 删除 ADMIN 的资料行后 GET 返回 `500 + 99999`。
7. 无 token GET 返回 `401 + 10002`。

- [ ] **Step 8：运行 Task 4 验收测试**

Run:

```powershell
mvn -Dtest=CurrentUserControllerTest,SecurityConfigTest,CurrentUserProfileIntegrationTest test
```

Expected: PASS。

- [ ] **Step 9：检查并提交 Task 4**

Run:

```powershell
git diff --check
```

勾选本 Task后提交：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-13-backend-v2-current-user-profile.md
git commit -m "开放当前用户资料接口"
```

---

## Task 5：契约、状态与全量验证

**提交信息：** `同步当前用户资料实施结果`

- [ ] **Step 1：更新 API 契约**

在 `api-contract/auth.md`：

- 把“当前用户资料查询”从尚未开放移除。
- 增加 GET/PATCH 请求与响应示例。
- 明确 ADMIN/DEMO 权限矩阵。
- 明确 PATCH 三态语义、字段限制和错误码。
- 保留“修改密码”在尚未开放列表。

在 `api-contract/README.md` 把 auth 状态更新为：

```text
✅ 登录 / 刷新 / 全端退出 / 当前用户资料已落地；修改密码尚未实现
```

- [ ] **Step 2：更新 status 和 roadmap**

`status.md`：

- 记录 `t_user_info`、GET `/api/auth/me`、PATCH `/api/auth/me/profile` 已完成。
- 更新全量测试数量，以实际 Maven 输出为准。
- 下一步改为 identity 修改密码设计，或进入 system 模块。

`roadmap.md`：

- 把 identity 当前用户资料部分标记完成。
- identity 仍因修改密码未完成而保持未完成，除非项目决策明确把修改密码后置。

- [ ] **Step 3：运行静态检查和全量测试**

Run:

```powershell
git diff --check
mvn clean test
```

Expected:

- `git diff --check` 无输出。
- Maven `BUILD SUCCESS`。
- 0 failures，0 errors。
- Docker 不可用时仅现有条件化 Testcontainers 测试 skipped；Docker 可用时必须执行并通过。

- [ ] **Step 4：确认 SQL 和注释规则**

Run:

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity
rg -L "^/\\*\\*|^package " MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity
```

Expected:

- identity 中没有新增注解 SQL。
- 新增 Java 类型均包含中文类级 Javadoc。

人工确认 `UserProfileMapper.xml`：

- 没有 `SELECT *`
- 加锁查询包含 `FOR UPDATE`
- 更新 SQL 包含 `deleted = 0`
- nullable 字段可以被更新为 SQL `NULL`

- [ ] **Step 5：勾选计划并提交文档**

将本计划全部 Task 标记完成，然后：

```powershell
git add docs/project-handbook docs/superpowers/plans/2026-06-13-backend-v2-current-user-profile.md
git commit -m "同步当前用户资料实施结果"
```

- [ ] **Step 6：最终确认**

Run:

```powershell
git status --short
git log -5 --oneline
```

Expected:

- 工作区干净。
- 最近 5 个提交依次对应持久化、查询、更新、HTTP 接口和文档同步。

---

## 2. 完成标准

- `t_user_info` 已有领域模型、Entity、XML Mapper 和仓储实现。
- 升级时缺失的历史资料通过 V2 Flyway 迁移补齐。
- ADMIN 和 DEMO 可查询本人账号及资料。
- 仅 ADMIN 可编辑本人资料。
- PATCH 正确区分字段未出现、赋值和显式清空。
- 可选空白字符串统一存为 `NULL`，nickname 不可清空。
- 资料缺失和更新行数异常对外返回 `500 + 99999`，内部记录中文日志。
- 所有手写 SQL 位于 XML。
- 全量 Maven 测试通过。
- 5 个 Task 各自形成独立中文提交。
