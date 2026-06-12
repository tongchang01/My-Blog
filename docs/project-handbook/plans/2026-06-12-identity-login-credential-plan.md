# Identity Login Credential Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立 identity 后台登录的账号领域模型、未删除账号读取能力和无 HTTP 依赖的凭据校验能力。

**Architecture:** `identity.domain` 定义账号模型、仓储端口和密码校验端口；`identity.infrastructure` 使用 MyBatis-Plus + XML SQL 读取 `t_user_auth`，并用现有 Spring Security `PasswordEncoder` 适配 BCrypt。凭据校验器只返回领域结果，不签发 token、不更新失败次数、不写审计、不实现限流或 Controller。

**Tech Stack:** Java 17、Spring Boot 3.5.14、MyBatis-Plus 3.5.12、Spring Security BCrypt、JUnit 5、AssertJ、Mockito、H2 + Flyway、ArchUnit

---

## 文件结构

新增文件：

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/AccountType.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/UserAccount.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/UserAccountRepository.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/PasswordHashVerifier.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialResult.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifier.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/UserAccountEntity.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisUserAccountRepository.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/security/SpringPasswordHashVerifier.java`
- `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml`
- 对应领域、持久化和安全适配测试。

不修改：

- Flyway `V1__init.sql`
- JWT、refresh token 和 token 撤销实现
- Security 白名单
- API 错误码
- Web Controller
- Caffeine 依赖

---

### Task 1: 建立登录账号领域模型

**Files:**
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/account/AccountTypeTest.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/account/UserAccountTest.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/AccountType.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/UserAccount.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/UserAccountRepository.java`

- [ ] **Step 1: 编写 AccountType 失败测试**

```java
package com.tyb.myblog.v2.identity.domain.account;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class AccountTypeTest {

    @Test
    void mapsFrozenDatabaseValuesAndAllowsOnlyBackendAccountsToLogin() {
        assertThat(AccountType.fromDatabaseValue(1)).isEqualTo(AccountType.ADMIN);
        assertThat(AccountType.fromDatabaseValue(2)).isEqualTo(AccountType.DEMO);
        assertThat(AccountType.fromDatabaseValue(3)).isEqualTo(AccountType.GUEST);
        assertThat(AccountType.ADMIN.canLoginToAdmin()).isTrue();
        assertThat(AccountType.DEMO.canLoginToAdmin()).isTrue();
        assertThat(AccountType.GUEST.canLoginToAdmin()).isFalse();
    }

    @Test
    void rejectsUnknownDatabaseValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> AccountType.fromDatabaseValue(99))
                .withMessageContaining("99");
    }
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn '-Dtest=AccountTypeTest' test
```

Expected: 测试编译失败，提示 `AccountType` 不存在。

- [ ] **Step 3: 实现 AccountType**

```java
package com.tyb.myblog.v2.identity.domain.account;

import java.util.Arrays;

/**
 * 后台账号类型，对应 {@code t_user_auth.type}。
 */
public enum AccountType {
    /** 站长账号，可执行全部后台操作。 */
    ADMIN(1, true),
    /** 演示账号，只允许读取后台数据。 */
    DEMO(2, true),
    /** 游客身份留位，不开放后台登录。 */
    GUEST(3, false);

    private final int databaseValue;
    private final boolean canLoginToAdmin;

    AccountType(int databaseValue, boolean canLoginToAdmin) {
        this.databaseValue = databaseValue;
        this.canLoginToAdmin = canLoginToAdmin;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public boolean canLoginToAdmin() {
        return canLoginToAdmin;
    }

    public static AccountType fromDatabaseValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.databaseValue == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知账号类型：" + value));
    }
}
```

- [ ] **Step 4: 运行 AccountType 测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=AccountTypeTest' test
```

Expected: 2 tests，0 failures。

- [ ] **Step 5: 编写 UserAccount 失败测试**

```java
package com.tyb.myblog.v2.identity.domain.account;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 12, 20, 0);

    @Test
    void allowsAdminAndDemoButRejectsGuestFromBackendLogin() {
        assertThat(account(AccountType.ADMIN, null).canLoginToAdmin()).isTrue();
        assertThat(account(AccountType.DEMO, null).canLoginToAdmin()).isTrue();
        assertThat(account(AccountType.GUEST, null).canLoginToAdmin()).isFalse();
    }

    @Test
    void treatsOnlyFutureLockedUntilAsLocked() {
        assertThat(account(AccountType.ADMIN, NOW.plusMinutes(1)).isLockedAt(NOW)).isTrue();
        assertThat(account(AccountType.ADMIN, NOW).isLockedAt(NOW)).isFalse();
        assertThat(account(AccountType.ADMIN, NOW.minusMinutes(1)).isLockedAt(NOW)).isFalse();
        assertThat(account(AccountType.ADMIN, null).isLockedAt(NOW)).isFalse();
    }

    private UserAccount account(AccountType type, LocalDateTime lockedUntil) {
        return new UserAccount(1001L, "admin", "hash", type, 3, 0, lockedUntil);
    }
}
```

- [ ] **Step 6: 运行测试并确认 RED**

Run:

```powershell
mvn '-Dtest=UserAccountTest' test
```

Expected: 测试编译失败，提示 `UserAccount` 不存在。

- [ ] **Step 7: 实现 UserAccount 和仓储端口**

```java
package com.tyb.myblog.v2.identity.domain.account;

import java.time.LocalDateTime;

/**
 * 后台登录账号领域对象。
 *
 * @param id             用户 ID
 * @param username       登录用户名
 * @param passwordHash   BCrypt 密码摘要
 * @param type           账号类型
 * @param tokenVersion   当前 token 撤销版本
 * @param loginFailCount 连续登录失败次数
 * @param lockedUntil    锁定截止时间；未锁定时为空
 */
public record UserAccount(
        long id,
        String username,
        String passwordHash,
        AccountType type,
        int tokenVersion,
        int loginFailCount,
        LocalDateTime lockedUntil
) {
    public boolean canLoginToAdmin() {
        return type.canLoginToAdmin();
    }

    public boolean isLockedAt(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
```

```java
package com.tyb.myblog.v2.identity.domain.account;

import java.util.Optional;

/**
 * 后台登录账号仓储端口。
 */
public interface UserAccountRepository {

    /**
     * 按用户名读取未软删除账号。
     */
    Optional<UserAccount> findActiveByUsername(String username);
}
```

- [ ] **Step 8: 运行领域模型测试和 ArchUnit**

Run:

```powershell
mvn '-Dtest=AccountTypeTest,UserAccountTest,ArchitectureRulesTest' test
```

Expected: 领域测试与 27 个架构测试全部通过。

- [ ] **Step 9: 提交领域模型**

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/account'
git diff --cached --check
git commit -m "建立登录账号领域模型"
```

---

### Task 2: 实现未删除账号持久化读取

**Files:**
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseUserAccountRepositoryTest.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/UserAccountEntity.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisUserAccountRepository.java`
- Create: `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml`

- [ ] **Step 1: 编写持久化失败测试**

```java
package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseUserAccountRepositoryTest {

    @Autowired
    private UserAccountRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearUsers() {
        jdbcTemplate.update("delete from t_refresh_token");
        jdbcTemplate.update("delete from t_user_auth");
    }

    @Test
    void readsActiveAccountAndMapsFrozenFields() {
        LocalDateTime lockedUntil = LocalDateTime.of(2026, 6, 12, 21, 0);
        insertUser(1001L, "admin", 1, 4, 2, lockedUntil, 0);

        var account = repository.findActiveByUsername("admin");

        assertThat(account).isPresent();
        assertThat(account.orElseThrow().type()).isEqualTo(AccountType.ADMIN);
        assertThat(account.orElseThrow().tokenVersion()).isEqualTo(4);
        assertThat(account.orElseThrow().loginFailCount()).isEqualTo(2);
        assertThat(account.orElseThrow().lockedUntil()).isEqualTo(lockedUntil);
    }

    @Test
    void excludesSoftDeletedAccount() {
        insertUser(1001L, "deleted-admin", 1, 0, 0, null, 1);

        assertThat(repository.findActiveByUsername("deleted-admin")).isEmpty();
    }

    private void insertUser(long id, String username, int type, int tokenVersion,
                            int loginFailCount, LocalDateTime lockedUntil, int deleted) {
        jdbcTemplate.update("""
                insert into t_user_auth (
                    id, username, password_hash, type, token_version,
                    login_fail_count, locked_until, deleted
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id, username, "$2a$10$test-password-hash", type, tokenVersion,
                loginFailCount, lockedUntil, deleted);
    }
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn '-Dtest=DatabaseUserAccountRepositoryTest' test
```

Expected: Spring 上下文无法注入 `UserAccountRepository`，测试失败。

- [ ] **Step 3: 实现 UserAccountEntity**

实体使用 Lombok `@Getter` / `@Setter`，映射以下字段并为每个字段写中文业务注释：

```java
@TableName("t_user_auth")
public class UserAccountEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String username;
    private String passwordHash;
    private Integer type;
    private Integer tokenVersion;
    private Integer loginFailCount;
    private LocalDateTime lockedUntil;
}
```

不得为本查询额外映射审计字段或增加业务方法。

- [ ] **Step 4: 实现 Mapper 接口与 XML SQL**

Mapper：

```java
@Mapper
public interface UserAccountMapper extends BaseMapper<UserAccountEntity> {

    /**
     * 按用户名读取未软删除账号。
     */
    UserAccountEntity selectActiveByUsername(@Param("username") String username);
}
```

XML：

```xml
<select id="selectActiveByUsername"
        resultType="com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserAccountEntity">
    SELECT id,
           username,
           password_hash,
           type,
           token_version,
           login_fail_count,
           locked_until
    FROM t_user_auth
    WHERE username = #{username}
      AND deleted = 0
</select>
```

SQL 必须放 XML，不得使用 `@Select`。

- [ ] **Step 5: 实现仓储适配器**

```java
@Repository
public class MyBatisUserAccountRepository implements UserAccountRepository {

    private final UserAccountMapper mapper;

    public MyBatisUserAccountRepository(UserAccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<UserAccount> findActiveByUsername(String username) {
        return Optional.ofNullable(mapper.selectActiveByUsername(username))
                .map(this::toDomain);
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                AccountType.fromDatabaseValue(entity.getType()),
                entity.getTokenVersion(),
                entity.getLoginFailCount(),
                entity.getLockedUntil());
    }
}
```

- [ ] **Step 6: 运行持久化测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=DatabaseUserAccountRepositoryTest' test
```

Expected: 2 tests，0 failures。

- [ ] **Step 7: 检查 SQL 与架构规则**

Run:

```powershell
rg -n "@Select|SELECT|UPDATE|DELETE|INSERT" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity
mvn '-Dtest=DatabaseUserAccountRepositoryTest,ArchitectureRulesTest' test
```

Expected:

- 新账号查询 SQL 只出现在 `UserAccountMapper.xml`。
- 持久化测试与 ArchUnit 全部通过。

- [ ] **Step 8: 提交持久化读取**

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/UserAccountEntity.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisUserAccountRepository.java' 'MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseUserAccountRepositoryTest.java'
git diff --cached --check
git commit -m "实现登录账号持久化读取"
```

---

### Task 3: 实现登录凭据校验

**Files:**
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifierTest.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/security/SpringPasswordHashVerifierTest.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/PasswordHashVerifier.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialResult.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifier.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/security/SpringPasswordHashVerifier.java`

- [ ] **Step 1: 编写凭据校验失败测试**

```java
package com.tyb.myblog.v2.identity.domain.auth;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginCredentialVerifierTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 12, 20, 0);

    @Test
    void authenticatesAdminAndDemoWithMatchingPassword() {
        assertThat(verify(account(AccountType.ADMIN, null), true))
                .isInstanceOf(LoginCredentialResult.Authenticated.class);
        assertThat(verify(account(AccountType.DEMO, null), true))
                .isInstanceOf(LoginCredentialResult.Authenticated.class);
    }

    @Test
    void hidesMissingGuestAndWrongPasswordBehindBadCredentials() {
        UserAccountRepository missingRepository = username -> Optional.empty();
        PasswordHashVerifier missingPasswordVerifier = mock(PasswordHashVerifier.class);
        var missingVerifier = new LoginCredentialVerifier(missingRepository, missingPasswordVerifier);

        assertThat(missingVerifier.verify("missing", "raw", NOW))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);
        verifyNoInteractions(missingPasswordVerifier);
        assertThat(verify(account(AccountType.GUEST, null), true))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);
        assertThat(verify(account(AccountType.ADMIN, null), false))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);
    }

    @Test
    void returnsLockedWithoutCheckingPassword() {
        PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
        UserAccount account = account(AccountType.ADMIN, NOW.plusMinutes(1));
        var verifier = new LoginCredentialVerifier(username -> Optional.of(account), passwordVerifier);

        assertThat(verifier.verify("admin", "raw", NOW))
                .isSameAs(LoginCredentialResult.Locked.INSTANCE);
        verifyNoInteractions(passwordVerifier);
    }

    private LoginCredentialResult verify(UserAccount account, boolean passwordMatches) {
        PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
        when(passwordVerifier.matches("raw", "hash")).thenReturn(passwordMatches);
        var verifier = new LoginCredentialVerifier(username -> Optional.of(account), passwordVerifier);
        return verifier.verify(account.username(), "raw", NOW);
    }

    private UserAccount account(AccountType type, LocalDateTime lockedUntil) {
        return new UserAccount(1001L, "admin", "hash", type, 3, 0, lockedUntil);
    }
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn '-Dtest=LoginCredentialVerifierTest' test
```

Expected: 测试编译失败，提示凭据校验类型不存在。

- [ ] **Step 3: 实现凭据校验端口、结果和领域服务**

```java
package com.tyb.myblog.v2.identity.domain.auth;

/**
 * 密码摘要校验端口。
 */
public interface PasswordHashVerifier {
    boolean matches(String rawPassword, String passwordHash);
}
```

```java
package com.tyb.myblog.v2.identity.domain.auth;

import com.tyb.myblog.v2.identity.domain.account.UserAccount;

/**
 * 后台登录凭据校验结果。
 */
public sealed interface LoginCredentialResult
        permits LoginCredentialResult.Authenticated,
                LoginCredentialResult.BadCredentials,
                LoginCredentialResult.Locked {

    /**
     * 账号和密码有效，携带同一次查询得到的账号进入后续登录编排。
     */
    record Authenticated(UserAccount account) implements LoginCredentialResult {
    }

    /**
     * 用户名、账号类型或密码不符合登录要求。
     */
    enum BadCredentials implements LoginCredentialResult {
        INSTANCE
    }

    /**
     * 账号仍处于持久化锁定期。
     */
    enum Locked implements LoginCredentialResult {
        INSTANCE
    }
}
```

```java
package com.tyb.myblog.v2.identity.domain.auth;

import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 后台登录凭据领域校验器。
 */
public class LoginCredentialVerifier {

    private final UserAccountRepository repository;
    private final PasswordHashVerifier passwordHashVerifier;

    public LoginCredentialVerifier(UserAccountRepository repository,
                                   PasswordHashVerifier passwordHashVerifier) {
        this.repository = repository;
        this.passwordHashVerifier = passwordHashVerifier;
    }

    public LoginCredentialResult verify(String username, String rawPassword, LocalDateTime now) {
        Optional<UserAccount> candidate = repository.findActiveByUsername(username);
        if (candidate.isEmpty()) {
            return LoginCredentialResult.BadCredentials.INSTANCE;
        }
        UserAccount account = candidate.orElseThrow();
        if (!account.canLoginToAdmin()) {
            return LoginCredentialResult.BadCredentials.INSTANCE;
        }
        if (account.isLockedAt(now)) {
            return LoginCredentialResult.Locked.INSTANCE;
        }
        return passwordHashVerifier.matches(rawPassword, account.passwordHash())
                ? new LoginCredentialResult.Authenticated(account)
                : LoginCredentialResult.BadCredentials.INSTANCE;
    }
}
```

- [ ] **Step 4: 运行领域校验测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=LoginCredentialVerifierTest' test
```

Expected: 3 tests，0 failures。

- [ ] **Step 5: 编写 BCrypt 适配器失败测试**

```java
package com.tyb.myblog.v2.identity.infrastructure.security;

import com.tyb.myblog.v2.identity.domain.auth.PasswordHashVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class SpringPasswordHashVerifierTest {

    @Test
    void delegatesToSpringPasswordEncoder() {
        var encoder = new BCryptPasswordEncoder(4);
        PasswordHashVerifier verifier = new SpringPasswordHashVerifier(encoder);
        String hash = encoder.encode("correct-password");

        assertThat(verifier.matches("correct-password", hash)).isTrue();
        assertThat(verifier.matches("wrong-password", hash)).isFalse();
    }
}
```

- [ ] **Step 6: 运行适配器测试并确认 RED**

Run:

```powershell
mvn '-Dtest=SpringPasswordHashVerifierTest' test
```

Expected: 测试编译失败，提示 `SpringPasswordHashVerifier` 不存在。

- [ ] **Step 7: 实现 Spring Security 适配器**

```java
package com.tyb.myblog.v2.identity.infrastructure.security;

import com.tyb.myblog.v2.identity.domain.auth.PasswordHashVerifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 使用 Spring Security PasswordEncoder 校验后台账号密码摘要。
 */
@Component
public class SpringPasswordHashVerifier implements PasswordHashVerifier {

    private final PasswordEncoder passwordEncoder;

    public SpringPasswordHashVerifier(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
```

- [ ] **Step 8: 运行本任务测试、ArchUnit 和全量测试**

Run:

```powershell
mvn '-Dtest=LoginCredentialVerifierTest,SpringPasswordHashVerifierTest,ArchitectureRulesTest' test
mvn clean test
```

Expected:

- 定向测试与 27 个架构测试通过。
- 全量 108 个既有测试加本轮新增测试全部通过。
- Docker 未启动时 `MySqlFlywayMigrationTest` 允许跳过 1 个。

- [ ] **Step 9: 提交凭据校验**

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/PasswordHashVerifier.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialResult.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifier.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/security/SpringPasswordHashVerifier.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifierTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/security/SpringPasswordHashVerifierTest.java'
git diff --cached --check
git commit -m "实现后台登录凭据校验"
```

---

## 最终验收

- [ ] `AccountType` 严格映射 1/2/3，未知值失败。
- [ ] ADMIN、DEMO 可登录，GUEST 统一归为坏凭据。
- [ ] `lockedUntil > now` 才视为锁定，等于当前时间时已解锁。
- [ ] 账号不存在、GUEST、密码错误不泄露差异。
- [ ] 锁定账号不调用密码校验。
- [ ] 成功结果携带已完成本次校验的账号，不重复查询。
- [ ] 账号查询只返回 `deleted=0`。
- [ ] 所有 SQL 位于 XML Mapper。
- [ ] domain/application 不依赖 Mapper、Entity 或 Spring Security。
- [ ] 未引入 Caffeine、Controller、token 编排、失败累计或审计写入。
- [ ] `mvn clean test` 通过。
- [ ] 工作区干净，三个提交目的独立。
