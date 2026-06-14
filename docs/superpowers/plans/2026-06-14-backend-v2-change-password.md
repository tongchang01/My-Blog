# Backend V2 修改密码实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. The user explicitly requires inline execution and prohibits subagents. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 提供仅 ADMIN 可调用的当前用户修改密码接口，并在改密成功后原子更新 BCrypt 摘要、递增 token version、撤销全部 refresh token。

**Architecture:** 在 identity 模块新增改密专用账号投影和仓储端口，通过 XML Mapper 对账号行加锁，并由单个事务应用服务完成校验、改密和全端会话撤销。Web 层只接收当前密码和新密码，不接收用户 ID，也不在成功后签发新 token。

**Tech Stack:** Java 17、Spring Boot 3、Spring Security、BCrypt、MyBatis-Plus、MyBatis XML、Lombok、JUnit 5、Mockito、MockMvc、AssertJ、H2 MySQL mode、Testcontainers MySQL。

---

## 0. 执行约束

- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`
- 后端目录：`E:\My-Blog\.worktrees\backend-v2-refactor\MyBlog-springboot-v2`
- 只在当前 `backend-v2-refactor` worktree 内执行。
- 不使用子代理。
- 每个 Task 独立执行 RED、GREEN、定向回归和中文提交。
- 不修改已冻结的 `V1__init.sql`，本功能不新增 Flyway。
- 所有手写 SQL 写入 `src/main/resources/mapper/identity/UserAccountMapper.xml`。
- 禁止在 Mapper 注解中写 SQL。
- 新增 Java 类型必须有中文类级 Javadoc；DTO/Command 字段和关键公开方法必须有中文说明。
- 明文密码和密码摘要不得写入日志、异常消息、OpenAPI 示例或响应。
- 本轮不实现找回密码、重置他人密码、密码历史、单设备会话或改密后自动续签 token。
- 每完成一个 Task，勾选本文件对应 checkbox 并与该 Task 一起提交。

## 1. 文件结构

### Task 1：密码摘要能力与账号持久化

**Create**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/ChangeablePasswordAccount.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/PasswordAccountRepository.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/PasswordHashService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/security/SpringPasswordHashService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisPasswordAccountRepository.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/security/SpringPasswordHashServiceTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabasePasswordAccountRepositoryTest.java`

**Modify**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifier.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/config/IdentityLoginConfiguration.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java`
- `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifierTest.java`

**Delete**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/PasswordHashVerifier.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/security/SpringPasswordHashVerifier.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/security/SpringPasswordHashVerifierTest.java`

### Task 2：修改密码事务服务

**Create**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/ChangePasswordCommand.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/ChangePasswordApplicationService.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/ChangePasswordApplicationServiceTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/ChangePasswordTransactionIntegrationTest.java`

### Task 3：HTTP、权限与 OpenAPI

**Create**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/ChangePasswordRequest.java`

**Modify**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/CurrentUserController.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/CurrentUserControllerTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/CurrentUserOpenApiTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`

### Task 4：完整会话与并发验收

**Create**

- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/integration/ChangePasswordIntegrationTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/ChangePasswordConcurrencyTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/MySqlChangePasswordConcurrencyTest.java`

### Task 5：文档和 identity 收尾

**Modify**

- `docs/project-handbook/api-contract/auth.md`
- `docs/project-handbook/arch/auth-flow.md`
- `docs/project-handbook/rules/security-baseline.md`
- `docs/project-handbook/status.md`
- `docs/project-handbook/roadmap.md`
- `docs/project-handbook/m3-preflight-review.md`
- `docs/superpowers/specs/2026-06-14-backend-v2-change-password-design.md`
- `docs/superpowers/plans/2026-06-14-backend-v2-change-password.md`

---

## Task 1：密码摘要能力与账号持久化

**提交信息：** `补齐修改密码持久化能力`

- [ ] **Step 1：先写密码摘要服务失败测试**

将旧 `SpringPasswordHashVerifierTest` 替换为 `SpringPasswordHashServiceTest`：

```java
class SpringPasswordHashServiceTest {

    @Test
    void encodesAndMatchesPasswordWithSpringEncoder() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        PasswordHashService service = new SpringPasswordHashService(encoder);

        String passwordHash = service.encode("new-password");

        assertThat(passwordHash).isNotEqualTo("new-password");
        assertThat(service.matches("new-password", passwordHash)).isTrue();
        assertThat(service.matches("wrong-password", passwordHash)).isFalse();
    }
}
```

同步把 `LoginCredentialVerifierTest` 中的 mock 类型由 `PasswordHashVerifier` 改为尚不存在的 `PasswordHashService`，保持既有登录行为断言不变。

- [ ] **Step 2：运行密码测试并确认 RED**

Run:

```powershell
mvn -Dtest=SpringPasswordHashServiceTest,LoginCredentialVerifierTest test
```

Expected: FAIL，原因是 `PasswordHashService` 和 `SpringPasswordHashService` 尚不存在。

- [ ] **Step 3：实现统一密码摘要端口**

创建：

```java
/**
 * 后台账号密码摘要服务。
 */
public interface PasswordHashService {

    /**
     * 校验明文密码与已保存摘要是否匹配。
     */
    boolean matches(String rawPassword, String passwordHash);

    /**
     * 使用当前安全配置生成不可逆密码摘要。
     */
    String encode(String rawPassword);
}
```

创建适配器：

```java
/**
 * 使用 Spring Security PasswordEncoder 处理后台账号密码摘要。
 */
@Component
@RequiredArgsConstructor
public class SpringPasswordHashService implements PasswordHashService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
```

更新 `LoginCredentialVerifier` 和 `IdentityLoginConfiguration` 使用 `PasswordHashService`。删除旧端口、旧适配器和旧测试，确保：

```powershell
rg -n "PasswordHashVerifier|SpringPasswordHashVerifier" MyBlog-springboot-v2/src
```

Expected: 无输出。

- [ ] **Step 4：运行密码与登录领域测试并确认 GREEN**

Run:

```powershell
mvn -Dtest=SpringPasswordHashServiceTest,LoginCredentialVerifierTest test
```

Expected: PASS。

- [ ] **Step 5：写改密账号仓储失败测试**

`DatabasePasswordAccountRepositoryTest` 使用 `@SpringBootTest`、`@ActiveProfiles("test")` 和真实 H2 Mapper，覆盖：

```java
@Test
void locksAndReturnsOnlyActiveAccountForPasswordChange() {
    insertAccount(1001L, 1, "old-hash", 3, 0);

    ChangeablePasswordAccount account =
            repository.findActiveByIdForUpdate(1001L).orElseThrow();

    assertThat(account.id()).isEqualTo(1001L);
    assertThat(account.type()).isEqualTo(AccountType.ADMIN);
    assertThat(account.passwordHash()).isEqualTo("old-hash");
}

@Test
void excludesDeletedAccount() {
    insertAccount(1002L, 1, "old-hash", 0, 1);

    assertThat(repository.findActiveByIdForUpdate(1002L)).isEmpty();
}

@Test
void updatesPasswordAndIncrementsTokenVersionWithAudit() {
    insertAccount(1003L, 1, "old-hash", 7, 0);

    boolean updated = repository.updatePasswordAndIncrementTokenVersion(
            1003L, "new-hash", FIXED_TIME, 1003L);

    assertThat(updated).isTrue();
    assertThat(jdbcTemplate.queryForMap(
            "SELECT password_hash, token_version, updated_at, updated_by "
                    + "FROM t_user_auth WHERE id = ?", 1003L))
            .containsEntry("PASSWORD_HASH", "new-hash")
            .containsEntry("TOKEN_VERSION", 8)
            .containsEntry("UPDATED_BY", 1003L);
}
```

测试 SQL 只用于测试数据准备和断言；生产 SQL 必须在 XML。

- [ ] **Step 6：运行仓储测试并确认 RED**

Run:

```powershell
mvn -Dtest=DatabasePasswordAccountRepositoryTest test
```

Expected: FAIL，原因是领域投影、端口和仓储实现尚不存在。

- [ ] **Step 7：实现领域投影与仓储端口**

创建：

```java
/**
 * 修改密码事务所需的最小账号快照。
 *
 * @param id 账号 ID
 * @param type 账号类型
 * @param passwordHash 当前 BCrypt 密码摘要
 */
public record ChangeablePasswordAccount(
        long id,
        AccountType type,
        String passwordHash
) {
}
```

创建：

```java
/**
 * 修改密码账号持久化端口。
 */
public interface PasswordAccountRepository {

    Optional<ChangeablePasswordAccount> findActiveByIdForUpdate(long userId);

    boolean updatePasswordAndIncrementTokenVersion(
            long userId,
            String passwordHash,
            LocalDateTime updatedAt,
            Long updatedBy);
}
```

- [ ] **Step 8：扩展 Mapper 和 XML**

`UserAccountMapper` 增加：

```java
UserAccountEntity selectActivePasswordAccountForUpdate(
        @Param("userId") long userId);

int updatePasswordAndIncrementTokenVersion(
        @Param("userId") long userId,
        @Param("passwordHash") String passwordHash,
        @Param("updatedAt") LocalDateTime updatedAt,
        @Param("updatedBy") Long updatedBy);
```

`UserAccountMapper.xml` 增加：

```xml
<!-- 修改密码前锁定未删除账号，保证并发旧密码请求最多一个成功。 -->
<select id="selectActivePasswordAccountForUpdate"
        resultType="com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserAccountEntity">
    SELECT id,
           type,
           password_hash
    FROM t_user_auth
    WHERE id = #{userId}
      AND deleted = 0
    FOR UPDATE
</select>

<!-- 原子更新密码摘要并递增 token_version，使历史 access token 立即失效。 -->
<update id="updatePasswordAndIncrementTokenVersion">
    UPDATE t_user_auth
    SET password_hash = #{passwordHash},
        token_version = token_version + 1,
        updated_at = #{updatedAt},
        updated_by = #{updatedBy}
    WHERE id = #{userId}
      AND deleted = 0
</update>
```

- [ ] **Step 9：实现持久化适配器**

`MyBatisPasswordAccountRepository`：

```java
/**
 * 基于 MyBatis XML 的修改密码账号持久化适配器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisPasswordAccountRepository
        implements PasswordAccountRepository {

    private final UserAccountMapper mapper;

    @Override
    public Optional<ChangeablePasswordAccount> findActiveByIdForUpdate(
            long userId) {
        return Optional.ofNullable(
                        mapper.selectActivePasswordAccountForUpdate(userId))
                .map(entity -> new ChangeablePasswordAccount(
                        entity.getId(),
                        AccountType.fromDatabaseValue(entity.getType()),
                        entity.getPasswordHash()));
    }

    @Override
    public boolean updatePasswordAndIncrementTokenVersion(
            long userId,
            String passwordHash,
            LocalDateTime updatedAt,
            Long updatedBy) {
        return mapper.updatePasswordAndIncrementTokenVersion(
                userId, passwordHash, updatedAt, updatedBy) == 1;
    }
}
```

- [ ] **Step 10：运行 Task 1 测试**

Run:

```powershell
mvn -Dtest=SpringPasswordHashServiceTest,LoginCredentialVerifierTest,DatabasePasswordAccountRepositoryTest,DatabaseUserAccountRepositoryTest test
```

Expected: PASS。

- [ ] **Step 11：规则检查并提交 Task 1**

Run:

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity
git diff --check
```

Expected: 没有新增注解 SQL；`git diff --check` 无输出。

勾选本 Task 后：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-14-backend-v2-change-password.md
git commit -m "补齐修改密码持久化能力"
```

---

## Task 2：修改密码事务服务

**提交信息：** `实现修改密码事务`

- [ ] **Step 1：写应用服务失败测试**

`ChangePasswordApplicationServiceTest` 使用 Mockito，覆盖：

- ADMIN + 正确旧密码：生成新摘要、更新密码和版本、撤销全部 refresh token。
- DEMO：在仓储调用前抛 `FORBIDDEN`。
- 非法主体 ID：抛 `INVALID_TOKEN`。
- 新密码不足 8 位或超过 128 位：抛 `VALIDATION_ERROR`。
- 当前密码错误：抛 `BAD_CREDENTIALS`，不编码、不更新、不撤销 token。
- 新旧密码相同：抛 `VALIDATION_ERROR`，不编码、不更新。
- 账号缺失：抛 `INTERNAL_ERROR`。
- 更新返回 false：抛 `INTERNAL_ERROR`，不撤销 refresh token。

成功测试核心断言：

```java
when(repository.findActiveByIdForUpdate(1001L))
        .thenReturn(Optional.of(account(AccountType.ADMIN, "old-hash")));
when(passwordHashService.matches("old-password", "old-hash"))
        .thenReturn(true);
when(passwordHashService.matches("new-password", "old-hash"))
        .thenReturn(false);
when(passwordHashService.encode("new-password")).thenReturn("new-hash");
when(repository.updatePasswordAndIncrementTokenVersion(
        1001L, "new-hash", FIXED_TIME, 1001L)).thenReturn(true);

service.change(adminPrincipal(),
        new ChangePasswordCommand("old-password", "new-password"));

verify(repository).updatePasswordAndIncrementTokenVersion(
        1001L, "new-hash", FIXED_TIME, 1001L);
verify(refreshTokenRepository).revokeAllByUserId(1001L);
```

- [ ] **Step 2：运行应用服务测试并确认 RED**

Run:

```powershell
mvn -Dtest=ChangePasswordApplicationServiceTest test
```

Expected: FAIL，原因是命令和应用服务尚不存在。

- [ ] **Step 3：实现命令**

```java
/**
 * 当前 ADMIN 修改本人密码的应用命令。
 *
 * @param currentPassword 当前明文密码
 * @param newPassword 新明文密码
 */
public record ChangePasswordCommand(
        String currentPassword,
        String newPassword
) {
}
```

- [ ] **Step 4：实现事务应用服务**

实现要点：

```java
/**
 * 修改当前 ADMIN 密码并使其全部认证会话失效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChangePasswordApplicationService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final PasswordAccountRepository passwordAccountRepository;
    private final PasswordHashService passwordHashService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;

    /**
     * 在单个事务中校验旧密码、写入新摘要并撤销全部 token。
     */
    @Transactional
    public void change(
            AuthenticatedPrincipal principal,
            ChangePasswordCommand command) {
        requireAdmin(principal);
        long userId = parsePositiveUserId(principal.id());
        validateCommand(command);

        ChangeablePasswordAccount account =
                passwordAccountRepository.findActiveByIdForUpdate(userId)
                        .orElseThrow(() -> missingAccount(userId));
        if (!passwordHashService.matches(
                command.currentPassword(), account.passwordHash())) {
            throw new ApiException(ApiErrorCode.BAD_CREDENTIALS);
        }
        if (passwordHashService.matches(
                command.newPassword(), account.passwordHash())) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "新密码不能与当前密码相同");
        }

        String newPasswordHash =
                passwordHashService.encode(command.newPassword());
        LocalDateTime updatedAt = LocalDateTime.now(clock);
        if (!passwordAccountRepository.updatePasswordAndIncrementTokenVersion(
                userId, newPasswordHash, updatedAt, userId)) {
            log.error("修改密码更新账号行数异常，userId={}", userId);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
```

辅助方法必须满足：

```java
private void requireAdmin(AuthenticatedPrincipal principal) {
    if (principal == null || !principal.roles().contains("ADMIN")) {
        throw new ApiException(ApiErrorCode.FORBIDDEN);
    }
}
```

主体 ID 非正整数映射 `INVALID_TOKEN`。命令或字段为 null、当前密码空、密码长度非法均映射 `VALIDATION_ERROR`。日志只允许 userId，不允许密码或摘要。

- [ ] **Step 5：运行单元测试并确认 GREEN**

Run:

```powershell
mvn -Dtest=ChangePasswordApplicationServiceTest test
```

Expected: PASS。

- [ ] **Step 6：写事务回滚失败测试**

`ChangePasswordTransactionIntegrationTest`：

1. 插入 ADMIN，旧密码使用真实 BCrypt，`token_version=3`。
2. 插入一枚未撤销 refresh token。
3. 使用 `@Primary` 包装 `RefreshTokenRepository`，让 `revokeAllByUserId` 抛出 `IllegalStateException`。
4. 调用真实 `ChangePasswordApplicationService`。
5. 断言异常后旧密码仍匹配、`token_version` 仍为 3、refresh token 仍未撤销。

关键断言：

```java
assertThatThrownBy(() -> service.change(
        principal(),
        new ChangePasswordCommand(OLD_PASSWORD, NEW_PASSWORD)))
        .isInstanceOf(IllegalStateException.class);

Map<String, Object> account = jdbcTemplate.queryForMap(
        "SELECT password_hash, token_version FROM t_user_auth WHERE id = ?",
        USER_ID);
assertThat(passwordEncoder.matches(
        OLD_PASSWORD, (String) account.get("PASSWORD_HASH"))).isTrue();
assertThat(account.get("TOKEN_VERSION")).isEqualTo(3);
assertThat(activeRefreshTokenCount()).isEqualTo(1);
```

- [ ] **Step 7：运行事务测试并确认 GREEN**

Run:

```powershell
mvn -Dtest=ChangePasswordApplicationServiceTest,ChangePasswordTransactionIntegrationTest test
```

Expected: PASS，并证明服务由 Spring 代理执行事务。

- [ ] **Step 8：提交 Task 2**

Run:

```powershell
git diff --check
```

勾选本 Task 后：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-14-backend-v2-change-password.md
git commit -m "实现修改密码事务"
```

---

## Task 3：HTTP、权限与 OpenAPI

**提交信息：** `开放当前用户修改密码接口`

- [ ] **Step 1：写 Controller 失败测试**

扩展 `CurrentUserControllerTest`，新增 `@MockitoBean ChangePasswordApplicationService`，覆盖：

```java
@Test
void changesCurrentAdminPassword() throws Exception {
    mockMvc.perform(put("/api/auth/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                              "currentPassword":"old-password",
                              "newPassword":"new-password"
                            }
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"))
            .andExpect(jsonPath("$.data").isEmpty());

    verify(changePasswordService).change(
            eq(principal),
            eq(new ChangePasswordCommand(
                    "old-password", "new-password")));
}
```

同时覆盖：

- 当前密码空白。
- 新密码不足 8 位。
- 新密码超过 128 位。
- JSON 非法。
- 应用服务抛 `BAD_CREDENTIALS` 时返回 `401 + 10001`。
- 应用服务抛 `FORBIDDEN` 时返回 `403 + 10003`。

- [ ] **Step 2：写 Security 失败测试**

扩展 `SecurityConfigTest`：

- ADMIN 可 PUT `/api/auth/me/password`。
- DEMO PUT 返回 `403 + 10003`。
- 未认证 PUT 返回 `401 + 10002`。
- POST/PATCH 同一路径不因 PUT 规则而放行。

- [ ] **Step 3：写 OpenAPI 失败测试**

扩展 `CurrentUserOpenApiTest`：

- `/api/auth/me/password` 只存在 `put`。
- request schema 只包含 `currentPassword`、`newPassword`。
- 两字段均 required，类型为 string。
- schema 不包含 `passwordHash`、`tokenVersion`。
- 不包含明文密码 example/default。

- [ ] **Step 4：运行 Web 测试并确认 RED**

Run:

```powershell
mvn -Dtest=CurrentUserControllerTest,SecurityConfigTest,CurrentUserOpenApiTest test
```

Expected: FAIL，原因是请求 DTO、Controller 方法和 PUT 权限规则尚不存在。

- [ ] **Step 5：实现请求 DTO**

```java
/**
 * 当前用户修改密码请求。
 *
 * @param currentPassword 当前密码，原样参与 BCrypt 校验
 * @param newPassword 新密码，长度为 8 至 128 个字符
 */
public record ChangePasswordRequest(
        @Schema(description = "当前密码", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "当前密码不能为空")
        @Size(max = 128, message = "当前密码长度不能超过128个字符")
        String currentPassword,

        @Schema(description = "新密码", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 128,
                message = "新密码长度必须为8至128个字符")
        String newPassword
) {

    public ChangePasswordCommand toCommand() {
        return new ChangePasswordCommand(currentPassword, newPassword);
    }
}
```

禁止添加 `example` 或 `defaultValue`。

- [ ] **Step 6：扩展 Controller**

注入 `ChangePasswordApplicationService` 并增加：

```java
/**
 * 修改当前 ADMIN 账号密码，并使该账号全部会话失效。
 */
@Operation(summary = "修改当前用户密码")
@PutMapping("/password")
public ApiResponse<Void> changePassword(
        @CurrentUser AuthenticatedPrincipal principal,
        @Valid @RequestBody ChangePasswordRequest request) {
    changePasswordService.change(principal, request.toCommand());
    return ApiResponse.ok(null);
}
```

- [ ] **Step 7：增加精确安全规则**

在 profile PATCH 规则前增加：

```java
.requestMatchers(
        HttpMethod.PUT,
        "/api/auth/me/password")
.hasRole("ADMIN")
```

不得加入 public endpoint 配置。

- [ ] **Step 8：运行 Web 测试并确认 GREEN**

Run:

```powershell
mvn -Dtest=CurrentUserControllerTest,SecurityConfigTest,CurrentUserOpenApiTest test
```

Expected: PASS。

- [ ] **Step 9：提交 Task 3**

Run:

```powershell
git diff --check
```

勾选本 Task 后：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-14-backend-v2-change-password.md
git commit -m "开放当前用户修改密码接口"
```

---

## Task 4：完整会话与并发验收

**提交信息：** `补齐修改密码集成验收`

- [ ] **Step 1：写完整 HTTP 会话失败测试**

`ChangePasswordIntegrationTest` 使用真实 Spring context、MockMvc、H2、JWT 登录和 BCrypt，至少包含：

```java
@Test
void changesPasswordAndInvalidatesAllSessionsWithoutAffectingAnotherAccount()
        throws Exception {
    insertAdmin(1001L, "admin-one", OLD_PASSWORD, 3);
    insertAdmin(2002L, "admin-two", OTHER_PASSWORD, 5);

    TokenView firstSession = login("admin-one", OLD_PASSWORD);
    TokenView secondSession = login("admin-one", OLD_PASSWORD);
    TokenView otherSession = login("admin-two", OTHER_PASSWORD);

    changePassword(firstSession.accessToken(), OLD_PASSWORD, NEW_PASSWORD)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("00000"));

    assertProtectedRequestUnauthorized(firstSession.accessToken());
    assertProtectedRequestUnauthorized(secondSession.accessToken());
    assertRefreshUnauthorized(firstSession.refreshToken());
    assertRefreshUnauthorized(secondSession.refreshToken());
    assertLoginUnauthorized("admin-one", OLD_PASSWORD);
    assertThat(login("admin-one", NEW_PASSWORD).accessToken()).isNotBlank();
    assertProtectedRequestSucceeds(otherSession.accessToken());
    assertThat(currentTokenVersion(1001L)).isEqualTo(4);
    assertThat(currentTokenVersion(2002L)).isEqualTo(5);
}
```

再覆盖：

- 当前密码错误返回 `401 + 10001`，旧 token 和旧密码仍有效。
- 新旧密码相同返回 `400 + 90001`，状态不变。
- DEMO 返回 `403 + 10003`。
- 未认证返回 `401 + 10002`。

- [ ] **Step 2：运行完整会话测试并确认 RED**

Run:

```powershell
mvn -Dtest=ChangePasswordIntegrationTest test
```

Expected: 在测试类未完成或功能边界遗漏时 FAIL；完善测试辅助方法后必须能真实覆盖登录、改密、refresh 和受保护接口。

- [ ] **Step 3：写 H2 并发失败测试**

`ChangePasswordConcurrencyTest` 参考现有资料并发测试：

- 使用两个线程和两个独立事务调用真实 `ChangePasswordApplicationService`。
- 用 `@Primary CoordinatedPasswordAccountRepository` 包装真实仓储。
- 第一线程取得账号行锁后等待。
- 第二线程开始加锁读取，并证明在第一事务提交前不能返回。
- 释放第一事务后，第一请求成功；第二请求因旧密码不再匹配返回 `BAD_CREDENTIALS`。
- 最终数据库只保存第一请求的新密码，`token_version` 只增加 1。

关键结果断言：

```java
assertThat(firstResult.get(10, TimeUnit.SECONDS)).isEqualTo(SUCCESS);
assertThat(secondResult.get(10, TimeUnit.SECONDS))
        .isEqualTo(ApiErrorCode.BAD_CREDENTIALS);
assertThat(currentTokenVersion()).isEqualTo(4);
assertThat(passwordEncoder.matches(
        FIRST_NEW_PASSWORD, currentPasswordHash())).isTrue();
```

- [ ] **Step 4：写 MySQL 条件并发测试**

`MySqlChangePasswordConcurrencyTest`：

- 使用与现有 `MySqlLoginFailureConcurrencyTest` 相同的 `@Testcontainers(disabledWithoutDocker = true)`。
- 启动 MySQL 8。
- 执行 Flyway。
- 运行两个使用同一旧密码的并发改密事务。
- 断言最多一个成功、版本只增加 1。
- 增加改密与 refresh 并发场景，设置有限超时，断言没有无限等待；若 MySQL 返回可重试死锁异常，先修正锁序再通过测试，不允许在测试中吞掉异常。

- [ ] **Step 5：运行并发测试并确认 GREEN**

Run:

```powershell
mvn -Dtest=ChangePasswordConcurrencyTest,MySqlChangePasswordConcurrencyTest test
```

Expected:

- H2 并发测试 PASS。
- Docker 可用时 MySQL 测试 PASS。
- Docker 不可用时仅 MySQL 条件测试 SKIPPED。

- [ ] **Step 6：运行 identity 与架构回归**

Run:

```powershell
mvn -Dtest="com.tyb.myblog.v2.identity.**,ArchitectureRulesTest" test
```

若 Surefire 不接受包通配，改为：

```powershell
mvn -Dtest=ChangePasswordIntegrationTest,ChangePasswordConcurrencyTest,AuthSessionIntegrationTest,AuthLoginIntegrationTest,CurrentUserProfileIntegrationTest,ArchitectureRulesTest test
```

Expected: PASS。

- [ ] **Step 7：静态规则检查并提交 Task 4**

Run:

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity
rg -n "currentPassword|newPassword|passwordHash" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity | rg "log\\.|Logger"
git diff --check
```

Expected:

- 无新增注解 SQL。
- 无密码或摘要日志。
- `git diff --check` 无输出。

勾选本 Task 后：

```powershell
git add MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-14-backend-v2-change-password.md
git commit -m "补齐修改密码集成验收"
```

---

## Task 5：文档和 identity 收尾

**提交信息：** `完成identity模块收尾`

- [ ] **Step 1：更新认证接口契约**

`api-contract/auth.md`：

- 增加 `PUT /api/auth/me/password` 请求和响应。
- 明确仅 ADMIN 可用。
- 明确当前密码错误、新旧相同、参数错误和 token 错误。
- 明确成功后不返回新 token，全部旧 token 失效，客户端重新登录。
- 从“尚未开放”删除修改密码。
- 更新权限矩阵。

- [ ] **Step 2：更新认证流程和安全基线**

`arch/auth-flow.md`：

- 增加账号行锁、BCrypt 校验、密码与 token version 原子更新、refresh token 全撤销流程。
- 明确改密后全部会话失效。
- 更新关键代码路径和测试基线。

`rules/security-baseline.md`：

- 明确改密必须校验当前密码。
- 明确新密码不 trim、长度 8 至 128、不能与当前密码相同。
- 明确改密事务和日志红线。

- [ ] **Step 3：更新状态和路线图**

`status.md`：

- 记录修改密码接口、事务、回滚、全端失效和并发保证。
- 把 identity 标记为完成。
- 下一步改为 `system` 模块，先设计 `t_site_config`、`t_attachment`、`t_friend_link` 的首个纵向切片。
- 测试数量以最终 Maven 输出为准。

`roadmap.md`：

- 勾选 identity。
- 保留 system、content、comment、stats/common-infra 未完成。
- 不把前端修改密码页面误标为后端已完成。

`m3-preflight-review.md`：

- 将 identity 改密缺口标记关闭。
- 如 MySQL 并发测试因 Docker 不可用跳过，保留准确说明。

- [ ] **Step 4：更新设计和计划实施证据**

先执行：

```powershell
git log -4 --format="%h %s"
```

把输出中的 Task 1 至 Task 4 真实 SHA 和中文提交信息写入设计文档顶部，并将状态改为“已实施（2026-06-14）”。全量验证完成后，再把 Maven 输出中的真实 tests、failures、errors 和 skipped 数量写入文档。

Task 5 自身 SHA 无法在提交前写入正文，因此设计文档只记录四个代码实施提交，并明确文档收尾由当前提交承载，不伪造 SHA。

- [ ] **Step 5：运行全量验证**

Run:

```powershell
git diff --check
mvn clean test
```

Expected:

- `git diff --check` 无输出。
- Maven `BUILD SUCCESS`。
- 0 failures，0 errors。
- Docker 不可用时仅条件化 Testcontainers 测试 skipped。

- [ ] **Step 6：最终静态审查**

Run:

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity
rg -n "PasswordHashVerifier|SpringPasswordHashVerifier" MyBlog-springboot-v2/src
rg -n "currentPassword|newPassword|passwordHash" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity | rg "log\\.|Logger"
git status --short
```

Expected:

- 无注解 SQL。
- 无旧密码摘要抽象残留。
- 无敏感密码日志。
- 提交前只有本轮文档和计划勾选变更。

- [ ] **Step 7：提交 Task 5**

将所有 Task 标记完成，然后：

```powershell
git add docs/project-handbook docs/superpowers/specs/2026-06-14-backend-v2-change-password-design.md docs/superpowers/plans/2026-06-14-backend-v2-change-password.md
git commit -m "完成identity模块收尾"
```

- [ ] **Step 8：最终确认**

Run:

```powershell
git status --short
git log -5 --oneline
```

Expected:

- 工作区干净。
- 最近五个提交依次对应持久化、事务、HTTP、集成验收和文档收尾。

---

## 2. 完成标准

- `PUT /api/auth/me/password` 已开放，且只允许 ADMIN 修改本人密码。
- DEMO、未认证、非法主体和错误当前密码返回稳定错误码。
- 当前密码与新密码均不 trim；新密码长度为 8 至 128。
- 新密码不能与当前密码相同。
- BCrypt 摘要能力由单一领域端口提供，登录和改密共用。
- 改密账号查询使用 `SELECT ... FOR UPDATE`。
- 密码摘要和 token version 在同一条 XML UPDATE 中更新。
- 密码更新、token version 递增和 refresh token 全撤销位于同一事务。
- 中途失败时密码、版本和 refresh token 状态全部回滚。
- 成功后不签发新 token，历史 access token 和 refresh token 全部失效。
- 两个使用相同旧密码的并发改密请求最多一个成功。
- 其他账号会话不受影响。
- 不新增表、Flyway、Redis、密码历史、找回密码或单设备会话。
- 没有 SQL 注解、敏感密码日志或缺失的中文业务注释。
- 五个 Task 各自形成独立中文提交。
- 全量 Maven 测试和 `git diff --check` 通过。
