# Identity 登录状态更新实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为后台登录补齐可配置的失败累计、10 分钟持久化锁定和成功登录状态更新，并确保并发更新、审计失败和账号状态变化不会被静默忽略。

**Architecture:** 领域层定义锁定策略、失败记录端口和状态更新异常；`LoginCredentialVerifier` 只在 ADMIN/DEMO 密码错误分支记录失败。基础设施层通过 MyBatis XML 单条原子 SQL 更新 `t_user_auth`，并在影响行数不是 1 时抛出异常。成功状态更新复用同一端口，但仍不实现 token 编排、Caffeine 限流、Controller 或 HTTP 映射。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring `@ConfigurationProperties`、MyBatis-Plus 3.5、XML Mapper、H2、Testcontainers MySQL 8.4、JUnit 5、AssertJ、Mockito、ArchUnit、Lombok。

---

## 0. 执行约束

- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`
- Maven 模块：`E:\My-Blog\.worktrees\backend-v2-refactor\MyBlog-springboot-v2`
- 所有生产 SQL 必须位于 `src/main/resources/mapper/identity/UserAccountMapper.xml`。
- 禁止新增 `@Select`、`@Update`、`@Delete`、`@Insert` SQL 注解。
- 新增 Java 类型和字段必须有中文注释；构造器使用 Lombok 或 record，避免手写样板代码。
- 每个任务严格执行 RED → GREEN → 回归 → 独立中文提交。
- 不修改冻结的 `V1__init.sql`。
- 不实现 Caffeine、token 签发、refresh token 登录编排、Controller、HTTP 错误码映射。
- 不把密码摘要、明文密码或用户名写入异常消息和日志。

## 1. 文件结构

### Task 1：领域规则与配置

- Create `common/config/SecurityPasswordProperties.java`
  - 绑定登录失败阈值、锁定时长和 BCrypt 强度。
- Create `identity/domain/auth/LoginLockPolicy.java`
  - 框架无关的不可变锁定规则。
- Create `identity/domain/auth/LoginFailureRecorder.java`
  - 仅表达密码失败状态记录。
- Create `identity/domain/auth/LoginStateUpdateException.java`
  - 表达状态更新没有命中唯一账号。
- Modify `identity/domain/auth/LoginCredentialVerifier.java`
  - 仅在 ADMIN/DEMO 密码错误时记录失败。
- Modify `common/security/SecurityConfig.java`
  - 使用配置的 BCrypt 强度创建现有 `PasswordEncoder` Bean。
- Modify `application.yml` / `application-test.yml`
  - 增加安全密码配置。

### Task 2：失败累计与持久化锁定

- Modify `identity/infrastructure/persistence/mapper/UserAccountMapper.java`
  - 声明失败原子更新方法。
- Modify `mapper/identity/UserAccountMapper.xml`
  - 实现单条原子更新 SQL。
- Create `identity/infrastructure/persistence/repository/MyBatisLoginFailureRecorder.java`
  - 适配领域失败记录端口并检查影响行数。
- Create `identity/infrastructure/config/IdentityLoginConfiguration.java`
  - 在持久化适配器存在后注册 `LoginLockPolicy` 和 `LoginCredentialVerifier`。
- Create H2 集成测试和 MySQL 并发测试。

### Task 3：成功登录状态更新

- Modify `LoginFailureRecorder.java` and rename it to `LoginStateRecorder.java`
  - 在最终形态中统一失败和成功状态记录。
- Modify `MyBatisLoginFailureRecorder.java` and rename it to `MyBatisLoginStateRecorder.java`
  - 增加成功状态更新。
- Modify Mapper / XML / 配置引用。
- Create 成功审计持久化测试。

这种顺序保证 Task 1 不注册依赖尚未实现的 Bean；Task 2 完成后 Spring 上下文才组装完整凭据校验器。Task 3 只扩展成功状态，不提前编写登录应用服务。

---

### Task 1: 定义锁定策略与失败记录边界

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/SecurityPasswordProperties.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginLockPolicy.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginFailureRecorder.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginStateUpdateException.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifier.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/SecurityPasswordPropertiesTest.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/auth/LoginLockPolicyTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifierTest.java`

- [x] **Step 1: 编写密码配置和锁定策略失败测试**

新增 `SecurityPasswordPropertiesTest`：

```java
package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class SecurityPasswordPropertiesTest {

    @Autowired
    private SecurityPasswordProperties properties;

    @Test
    void bindsPasswordSecuritySettings() {
        assertThat(properties.loginMaxAttempts()).isEqualTo(5);
        assertThat(properties.loginCooldown()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.bcryptStrength()).isEqualTo(10);
    }
}
```

新增 `LoginLockPolicyTest`：

```java
package com.tyb.myblog.v2.identity.domain.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LoginLockPolicyTest {

    @Test
    void exposesValidatedThresholdAndDuration() {
        LoginLockPolicy policy = new LoginLockPolicy(5, Duration.ofMinutes(10));

        assertThat(policy.maxAttempts()).isEqualTo(5);
        assertThat(policy.lockDuration()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void rejectsInvalidThresholdOrDuration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginLockPolicy(0, Duration.ofMinutes(10)));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginLockPolicy(5, Duration.ZERO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginLockPolicy(5, Duration.ofMinutes(-1)));
    }
}
```

- [x] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn '-Dtest=SecurityPasswordPropertiesTest,LoginLockPolicyTest' test
```

Expected: 测试编译失败，提示 `SecurityPasswordProperties` 和 `LoginLockPolicy` 不存在。

- [x] **Step 3: 实现配置属性和锁定策略**

新增 `SecurityPasswordProperties`：

```java
package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 后台账号密码与登录锁定配置。
 *
 * @param loginMaxAttempts 单个账号连续密码错误锁定阈值
 * @param loginCooldown 持久化锁定时长
 * @param bcryptStrength BCrypt 计算强度
 */
@ConfigurationProperties("myblog.security.password")
public record SecurityPasswordProperties(
        int loginMaxAttempts,
        Duration loginCooldown,
        int bcryptStrength
) {

    public SecurityPasswordProperties {
        if (loginMaxAttempts < 1) {
            throw new IllegalArgumentException("登录失败阈值必须大于 0");
        }
        if (loginCooldown == null || loginCooldown.isZero() || loginCooldown.isNegative()) {
            throw new IllegalArgumentException("登录锁定时长必须为正数");
        }
        if (bcryptStrength < 4 || bcryptStrength > 31) {
            throw new IllegalArgumentException("BCrypt 强度必须在 4 到 31 之间");
        }
    }
}
```

新增 `LoginLockPolicy`：

```java
package com.tyb.myblog.v2.identity.domain.auth;

import java.time.Duration;

/**
 * 后台账号持久化锁定规则。
 *
 * @param maxAttempts 单个失败周期允许的最大密码错误次数
 * @param lockDuration 达到阈值后的锁定时长
 */
public record LoginLockPolicy(int maxAttempts, Duration lockDuration) {

    public LoginLockPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("登录失败阈值必须大于 0");
        }
        if (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException("登录锁定时长必须为正数");
        }
    }
}
```

在 `application.yml` 和 `application-test.yml` 的 `myblog.security` 下加入：

```yaml
password:
  login-max-attempts: 5
  login-cooldown: 10m
  bcrypt-strength: 10
```

修改 `SecurityConfig`：

```java
@EnableConfigurationProperties({
        ApiCorsProperties.class,
        SecurityPublicEndpointProperties.class,
        SecurityJwtProperties.class,
        SecurityPasswordProperties.class
})
```

并将密码编码器改为：

```java
@Bean
PasswordEncoder passwordEncoder(SecurityPasswordProperties properties) {
    return new BCryptPasswordEncoder(properties.bcryptStrength());
}
```

- [x] **Step 4: 运行配置与策略测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=SecurityPasswordPropertiesTest,LoginLockPolicyTest,SpringPasswordHashVerifierTest,SecurityConfigTest' test
```

Expected: 配置、策略、BCrypt 适配器和 Security 配置测试全部通过。

- [x] **Step 5: 扩展凭据校验失败测试**

修改 `LoginCredentialVerifierTest`，为测试类增加：

```java
private static final LoginLockPolicy LOCK_POLICY =
        new LoginLockPolicy(5, Duration.ofMinutes(10));
```

将创建校验器的代码统一改为传入 `LoginFailureRecorder` 和 `LOCK_POLICY`。新增测试：

```java
@Test
void recordsPasswordFailureOnlyForLoginCapableAccount() {
    LoginFailureRecorder recorder = mock(LoginFailureRecorder.class);
    PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
    UserAccount admin = account(AccountType.ADMIN, null);
    when(passwordVerifier.matches("raw", "hash")).thenReturn(false);
    LoginCredentialVerifier verifier = new LoginCredentialVerifier(
            username -> Optional.of(admin),
            passwordVerifier,
            recorder,
            LOCK_POLICY);

    assertThat(verifier.verify("admin", "raw", NOW))
            .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);

    verify(recorder).recordPasswordFailure(
            1001L,
            NOW,
            5,
            NOW.plusMinutes(10));
}

@Test
void doesNotRecordFailureForMissingGuestLockedOrSuccessfulAccount() {
    LoginFailureRecorder recorder = mock(LoginFailureRecorder.class);
    PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);

    LoginCredentialVerifier missingVerifier = new LoginCredentialVerifier(
            username -> Optional.empty(),
            passwordVerifier,
            recorder,
            LOCK_POLICY);
    assertThat(missingVerifier.verify("missing", "raw", NOW))
            .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);

    LoginCredentialVerifier guestVerifier = new LoginCredentialVerifier(
            username -> Optional.of(account(AccountType.GUEST, null)),
            passwordVerifier,
            recorder,
            LOCK_POLICY);
    assertThat(guestVerifier.verify("guest", "raw", NOW))
            .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);

    LoginCredentialVerifier lockedVerifier = new LoginCredentialVerifier(
            username -> Optional.of(account(AccountType.ADMIN, NOW.plusMinutes(1))),
            passwordVerifier,
            recorder,
            LOCK_POLICY);
    assertThat(lockedVerifier.verify("admin", "raw", NOW))
            .isSameAs(LoginCredentialResult.Locked.INSTANCE);

    when(passwordVerifier.matches("raw", "hash")).thenReturn(true);
    LoginCredentialVerifier successfulVerifier = new LoginCredentialVerifier(
            username -> Optional.of(account(AccountType.ADMIN, null)),
            passwordVerifier,
            recorder,
            LOCK_POLICY);
    assertThat(successfulVerifier.verify("admin", "raw", NOW))
            .isInstanceOf(LoginCredentialResult.Authenticated.class);

    verifyNoInteractions(recorder);
}

@Test
void propagatesFailureRecordingException() {
    LoginFailureRecorder recorder = mock(LoginFailureRecorder.class);
    PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
    UserAccount admin = account(AccountType.ADMIN, null);
    when(passwordVerifier.matches("raw", "hash")).thenReturn(false);
    doThrow(LoginStateUpdateException.passwordFailure(1001L))
            .when(recorder)
            .recordPasswordFailure(anyLong(), any(), anyInt(), any());
    LoginCredentialVerifier verifier = new LoginCredentialVerifier(
            username -> Optional.of(admin),
            passwordVerifier,
            recorder,
            LOCK_POLICY);

    assertThatThrownBy(() -> verifier.verify("admin", "raw", NOW))
            .isInstanceOf(LoginStateUpdateException.class);
}
```

补充静态导入：

```java
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
```

- [x] **Step 6: 运行凭据校验测试并确认 RED**

Run:

```powershell
mvn '-Dtest=LoginCredentialVerifierTest' test
```

Expected: 编译失败，提示 `LoginFailureRecorder`、`LoginStateUpdateException` 或新的构造参数不存在。

- [x] **Step 7: 实现失败记录端口、异常和校验器分支**

新增 `LoginFailureRecorder`：

```java
package com.tyb.myblog.v2.identity.domain.auth;

import java.time.LocalDateTime;

/**
 * 后台登录密码失败状态记录端口。
 */
public interface LoginFailureRecorder {

    /**
     * 原子记录一次已确认后台账号的密码错误。
     */
    void recordPasswordFailure(
            long userId,
            LocalDateTime failedAt,
            int maxAttempts,
            LocalDateTime lockedUntil);
}
```

新增 `LoginStateUpdateException`：

```java
package com.tyb.myblog.v2.identity.domain.auth;

/**
 * 登录状态没有按预期更新到唯一账号。
 */
public class LoginStateUpdateException extends RuntimeException {

    private LoginStateUpdateException(String message) {
        super(message);
    }

    public static LoginStateUpdateException passwordFailure(long userId) {
        return new LoginStateUpdateException("登录失败状态更新失败，账号 ID：" + userId);
    }

    public static LoginStateUpdateException successfulLogin(long userId) {
        return new LoginStateUpdateException("登录成功状态更新失败，账号 ID：" + userId);
    }
}
```

修改 `LoginCredentialVerifier` 字段：

```java
private final UserAccountRepository repository;
private final PasswordHashVerifier passwordHashVerifier;
private final LoginFailureRecorder loginFailureRecorder;
private final LoginLockPolicy loginLockPolicy;
```

将密码比较分支改为：

```java
if (passwordHashVerifier.matches(rawPassword, account.passwordHash())) {
    return new LoginCredentialResult.Authenticated(account);
}

loginFailureRecorder.recordPasswordFailure(
        account.id(),
        now,
        loginLockPolicy.maxAttempts(),
        now.plus(loginLockPolicy.lockDuration()));
return LoginCredentialResult.BadCredentials.INSTANCE;
```

- [x] **Step 8: 运行领域、配置与架构测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=LoginCredentialVerifierTest,LoginLockPolicyTest,SecurityPasswordPropertiesTest,ArchitectureRulesTest' test
```

Expected: 新增领域和配置测试通过，27 个架构测试通过。

- [x] **Step 9: 全量验证并提交**

Run:

```powershell
mvn clean test
git diff --check
```

Expected: Maven 构建成功；Docker 不可用时仅允许 `MySqlFlywayMigrationTest` 跳过。

Commit:

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/SecurityPasswordProperties.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginLockPolicy.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginFailureRecorder.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginStateUpdateException.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifier.java' 'MyBlog-springboot-v2/src/main/resources/application.yml' 'MyBlog-springboot-v2/src/test/resources/application-test.yml' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/SecurityPasswordPropertiesTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/auth/LoginLockPolicyTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifierTest.java'
git diff --cached --check
git commit -m "定义登录锁定策略与失败记录边界"
```

---

### Task 2: 实现失败累计与持久化锁定

**Files:**
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisLoginFailureRecorder.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/config/IdentityLoginConfiguration.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseLoginFailureRecorderTest.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/MySqlLoginFailureConcurrencyTest.java`

- [x] **Step 1: 编写 H2 失败累计集成测试**

新增 `DatabaseLoginFailureRecorderTest`，使用 `@ActiveProfiles("test")`、`@SpringBootTest`，注入 `LoginFailureRecorder` 和 `JdbcTemplate`。`@BeforeEach` 依次清空 `t_refresh_token`、`t_user_auth`。

核心测试：

```java
@Test
void incrementsFourTimesThenLocksAndResetsCounter() {
    insertAccount(1001L, 0, null, 0);
    LocalDateTime now = LocalDateTime.of(2026, 6, 12, 20, 0);

    for (int attempt = 1; attempt <= 4; attempt++) {
        recorder.recordPasswordFailure(
                1001L,
                now.plusSeconds(attempt),
                5,
                now.plusMinutes(10));
        assertThat(readInteger("login_fail_count")).isEqualTo(attempt);
        assertThat(readTime("locked_until")).isNull();
    }

    LocalDateTime lockedUntil = now.plusMinutes(10);
    recorder.recordPasswordFailure(1001L, now.plusSeconds(5), 5, lockedUntil);

    assertThat(readInteger("login_fail_count")).isZero();
    assertThat(readTime("locked_until")).isEqualTo(lockedUntil);
}

@Test
void rejectsFailureWhileLocked() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 12, 20, 0);
    insertAccount(1001L, 0, now.plusMinutes(1), 0);

    assertThatThrownBy(() -> recorder.recordPasswordFailure(
            1001L,
            now,
            5,
            now.plusMinutes(10)))
            .isInstanceOf(LoginStateUpdateException.class);
}

@Test
void restartsCounterAfterExpiredLock() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 12, 20, 0);
    insertAccount(1001L, 0, now.minusSeconds(1), 0);

    recorder.recordPasswordFailure(1001L, now, 5, now.plusMinutes(10));

    assertThat(readInteger("login_fail_count")).isEqualTo(1);
    assertThat(readTime("locked_until")).isNull();
}

@Test
void rejectsDeletedOrMissingAccount() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 12, 20, 0);
    insertAccount(1001L, 0, null, 1);

    assertThatThrownBy(() -> recorder.recordPasswordFailure(
            1001L, now, 5, now.plusMinutes(10)))
            .isInstanceOf(LoginStateUpdateException.class);
    assertThatThrownBy(() -> recorder.recordPasswordFailure(
            9999L, now, 5, now.plusMinutes(10)))
            .isInstanceOf(LoginStateUpdateException.class);
}
```

测试辅助查询必须使用 `JdbcTemplate.queryForObject`，测试夹具 SQL 可以写在测试代码；生产 SQL 仍只能写 XML。

- [x] **Step 2: 运行 H2 集成测试并确认 RED**

Run:

```powershell
mvn '-Dtest=DatabaseLoginFailureRecorderTest' test
```

Expected: Spring 注入失败或编译失败，提示没有 `LoginFailureRecorder` 基础设施 Bean。

- [x] **Step 3: 实现 Mapper、XML 和适配器**

在 `UserAccountMapper` 增加：

```java
int recordPasswordFailure(
        @Param("userId") long userId,
        @Param("failedAt") LocalDateTime failedAt,
        @Param("maxAttempts") int maxAttempts,
        @Param("lockedUntil") LocalDateTime lockedUntil);
```

在 `UserAccountMapper.xml` 增加：

```xml
<!-- 原子累计密码错误；达到阈值时写入锁定时间并开始新的失败周期。 -->
<update id="recordPasswordFailure">
    UPDATE t_user_auth
    SET locked_until = CASE
            WHEN login_fail_count + 1 &gt;= #{maxAttempts} THEN #{lockedUntil}
            ELSE NULL
        END,
        login_fail_count = CASE
            WHEN login_fail_count + 1 &gt;= #{maxAttempts} THEN 0
            ELSE login_fail_count + 1
        END,
        updated_at = #{failedAt},
        updated_by = NULL
    WHERE id = #{userId}
      AND deleted = 0
      AND (locked_until IS NULL OR locked_until &lt;= #{failedAt})
</update>
```

新增 `MyBatisLoginFailureRecorder`：

```java
package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.auth.LoginFailureRecorder;
import com.tyb.myblog.v2.identity.domain.auth.LoginStateUpdateException;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 基于 MyBatis 的登录密码失败状态记录器。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisLoginFailureRecorder implements LoginFailureRecorder {

    private final UserAccountMapper mapper;

    @Override
    public void recordPasswordFailure(
            long userId,
            LocalDateTime failedAt,
            int maxAttempts,
            LocalDateTime lockedUntil
    ) {
        int updated = mapper.recordPasswordFailure(
                userId,
                failedAt,
                maxAttempts,
                lockedUntil);
        if (updated != 1) {
            throw LoginStateUpdateException.passwordFailure(userId);
        }
    }
}
```

新增 `IdentityLoginConfiguration`：

```java
package com.tyb.myblog.v2.identity.infrastructure.config;

import com.tyb.myblog.v2.common.config.SecurityPasswordProperties;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import com.tyb.myblog.v2.identity.domain.auth.LoginCredentialVerifier;
import com.tyb.myblog.v2.identity.domain.auth.LoginFailureRecorder;
import com.tyb.myblog.v2.identity.domain.auth.LoginLockPolicy;
import com.tyb.myblog.v2.identity.domain.auth.PasswordHashVerifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 后台登录领域服务组装配置。
 */
@Configuration
public class IdentityLoginConfiguration {

    @Bean
    LoginLockPolicy loginLockPolicy(SecurityPasswordProperties properties) {
        return new LoginLockPolicy(
                properties.loginMaxAttempts(),
                properties.loginCooldown());
    }

    @Bean
    LoginCredentialVerifier loginCredentialVerifier(
            UserAccountRepository repository,
            PasswordHashVerifier passwordHashVerifier,
            LoginFailureRecorder loginFailureRecorder,
            LoginLockPolicy loginLockPolicy
    ) {
        return new LoginCredentialVerifier(
                repository,
                passwordHashVerifier,
                loginFailureRecorder,
                loginLockPolicy);
    }
}
```

- [x] **Step 4: 运行 H2 与上下文测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=DatabaseLoginFailureRecorderTest,ApplicationConfigurationTest,LoginCredentialVerifierTest' test
```

Expected: 失败累计、Spring 上下文和领域校验测试全部通过。

- [x] **Step 5: 编写真实 MySQL 并发失败测试**

新增 `MySqlLoginFailureConcurrencyTest`：

```java
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@SpringBootTest
class MySqlLoginFailureConcurrencyTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("myblog_v2_login_state")
                    .withUsername("myblog")
                    .withPassword("myblog-test-password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private LoginFailureRecorder recorder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void locksAfterFiveConcurrentFailuresWithoutLosingUpdates() throws Exception {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        jdbcTemplate.update("""
                INSERT INTO t_user_auth (
                    id, username, password_hash, type, token_version,
                    login_fail_count, deleted
                ) VALUES (1001, 'admin', 'hash', 1, 0, 0, 0)
                """);
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 20, 0);
        LocalDateTime lockedUntil = now.plusMinutes(10);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch ready = new CountDownLatch(5);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<?>> futures = IntStream.range(0, 5)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        recorder.recordPasswordFailure(
                                1001L,
                                now.plusNanos(index),
                                5,
                                lockedUntil);
                        return null;
                    }))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        Integer failureCount = jdbcTemplate.queryForObject(
                "SELECT login_fail_count FROM t_user_auth WHERE id = 1001",
                Integer.class);
        LocalDateTime storedLockedUntil = jdbcTemplate.queryForObject(
                "SELECT locked_until FROM t_user_auth WHERE id = 1001",
                LocalDateTime.class);
        assertThat(failureCount).isZero();
        assertThat(storedLockedUntil).isEqualTo(lockedUntil);
    }
}
```

补齐上述代码所需的 JUnit、Spring、Testcontainers、并发和集合 import。不得通过 `Thread.sleep` 制造并发。

- [x] **Step 6: 运行 MySQL 并发测试**

Run:

```powershell
mvn '-Dtest=MySqlLoginFailureConcurrencyTest' test
```

Expected:

- Docker 可用：1 test，0 failures。
- Docker 不可用：1 test skipped，构建成功。

- [x] **Step 7: 执行 SQL 规则、定向和全量验证**

Run:

```powershell
$matches = rg -n '@(Select|Update|Delete|Insert)' 'src/main/java/com/tyb/myblog/v2/identity'
if ($LASTEXITCODE -eq 1) { Write-Output '未发现 MyBatis SQL 注解' }
rg -n 'recordPasswordFailure|UPDATE t_user_auth|login_fail_count|locked_until' 'src/main/resources/mapper/identity/UserAccountMapper.xml'
mvn '-Dtest=DatabaseLoginFailureRecorderTest,MySqlLoginFailureConcurrencyTest,LoginCredentialVerifierTest,ArchitectureRulesTest' test
mvn clean test
```

Expected:

- Java 生产代码无 MyBatis SQL 注解。
- XML 中存在失败累计原子更新。
- 定向测试和 27 个架构测试通过。
- 全量测试通过；Docker 不可用时 MySQL 测试允许跳过。

- [x] **Step 8: 提交失败累计与持久化锁定**

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java' 'MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisLoginFailureRecorder.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/config/IdentityLoginConfiguration.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseLoginFailureRecorderTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/MySqlLoginFailureConcurrencyTest.java'
git diff --cached --check
git commit -m "实现登录失败累计与持久化锁定"
```

---

### Task 3: 实现登录成功状态更新

**Files:**
- Move: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginFailureRecorder.java` → `LoginStateRecorder.java`
- Move: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisLoginFailureRecorder.java` → `MyBatisLoginStateRecorder.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifier.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/config/IdentityLoginConfiguration.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml`
- Modify: Task 1 和 Task 2 中引用旧接口名的测试
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseSuccessfulLoginRecorderTest.java`

- [x] **Step 1: 编写成功状态更新失败测试**

新增 `DatabaseSuccessfulLoginRecorderTest`，使用 `@ActiveProfiles("test")`、`@SpringBootTest`，注入最终接口 `LoginStateRecorder` 和 `JdbcTemplate`。

```java
@Test
void recordsSuccessfulLoginAndClearsFailureState() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 12, 20, 0);
    insertAccount(1001L, 4, now.minusMinutes(1), 0);

    recorder.recordSuccessfulLogin(1001L, now, "2001:db8::1");

    Map<String, Object> row = jdbcTemplate.queryForMap("""
            SELECT last_login_at,
                   last_login_ip,
                   login_fail_count,
                   locked_until,
                   updated_at,
                   updated_by
            FROM t_user_auth
            WHERE id = 1001
            """);
    assertThat(row.get("last_login_at")).isEqualTo(now);
    assertThat(row.get("last_login_ip")).isEqualTo("2001:db8::1");
    assertThat(row.get("login_fail_count")).isEqualTo(0);
    assertThat(row.get("locked_until")).isNull();
    assertThat(row.get("updated_at")).isEqualTo(now);
    assertThat(row.get("updated_by")).isEqualTo(1001L);
}

@Test
void acceptsNullClientIp() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 12, 20, 0);
    insertAccount(1001L, 0, null, 0);

    recorder.recordSuccessfulLogin(1001L, now, null);

    assertThat(jdbcTemplate.queryForObject(
            "SELECT last_login_ip FROM t_user_auth WHERE id = 1001",
            String.class))
            .isNull();
}

@Test
void rejectsDeletedLockedOrMissingAccount() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 12, 20, 0);
    insertAccount(1001L, 0, null, 1);
    insertAccount(1002L, 0, now.plusMinutes(1), 0);

    assertThatThrownBy(() -> recorder.recordSuccessfulLogin(
            1001L, now, "127.0.0.1"))
            .isInstanceOf(LoginStateUpdateException.class);
    assertThatThrownBy(() -> recorder.recordSuccessfulLogin(
            1002L, now, "127.0.0.1"))
            .isInstanceOf(LoginStateUpdateException.class);
    assertThatThrownBy(() -> recorder.recordSuccessfulLogin(
            9999L, now, "127.0.0.1"))
            .isInstanceOf(LoginStateUpdateException.class);
}
```

- [x] **Step 2: 运行成功状态测试并确认 RED**

Run:

```powershell
mvn '-Dtest=DatabaseSuccessfulLoginRecorderTest' test
```

Expected: 编译失败，提示 `recordSuccessfulLogin` 不存在，或最终 `LoginStateRecorder` 类型不存在。

- [x] **Step 3: 将失败端口演进为完整登录状态端口**

使用 `apply_patch` 移动并改名：

```java
package com.tyb.myblog.v2.identity.domain.auth;

import java.time.LocalDateTime;

/**
 * 后台登录持久化状态记录端口。
 */
public interface LoginStateRecorder {

    void recordPasswordFailure(
            long userId,
            LocalDateTime failedAt,
            int maxAttempts,
            LocalDateTime lockedUntil);

    void recordSuccessfulLogin(
            long userId,
            LocalDateTime loggedInAt,
            String clientIp);
}
```

同步将 `LoginCredentialVerifier`、`IdentityLoginConfiguration` 和测试中的 `LoginFailureRecorder` 改为 `LoginStateRecorder`。

- [x] **Step 4: 实现成功更新 Mapper 与 XML**

在 `UserAccountMapper` 增加：

```java
int recordSuccessfulLogin(
        @Param("userId") long userId,
        @Param("loggedInAt") LocalDateTime loggedInAt,
        @Param("clientIp") String clientIp);
```

在 XML 增加：

```xml
<!-- 登录成功后写入审计信息，并清理失败次数和过期锁定状态。 -->
<update id="recordSuccessfulLogin">
    UPDATE t_user_auth
    SET last_login_at = #{loggedInAt},
        last_login_ip = #{clientIp},
        login_fail_count = 0,
        locked_until = NULL,
        updated_at = #{loggedInAt},
        updated_by = #{userId}
    WHERE id = #{userId}
      AND deleted = 0
      AND (locked_until IS NULL OR locked_until &lt;= #{loggedInAt})
</update>
```

将适配器移动并改名为 `MyBatisLoginStateRecorder`，保留失败方法并增加：

```java
@Override
public void recordSuccessfulLogin(
        long userId,
        LocalDateTime loggedInAt,
        String clientIp
) {
    int updated = mapper.recordSuccessfulLogin(userId, loggedInAt, clientIp);
    if (updated != 1) {
        throw LoginStateUpdateException.successfulLogin(userId);
    }
}
```

- [x] **Step 5: 运行成功状态和失败累计回归**

Run:

```powershell
mvn '-Dtest=DatabaseSuccessfulLoginRecorderTest,DatabaseLoginFailureRecorderTest,LoginCredentialVerifierTest,ApplicationConfigurationTest' test
```

Expected: 成功状态、失败累计、领域校验和 Spring 上下文测试全部通过。

- [x] **Step 6: 执行规则、架构和全量验证**

Run:

```powershell
$matches = rg -n '@(Select|Update|Delete|Insert)' 'src/main/java/com/tyb/myblog/v2/identity'
if ($LASTEXITCODE -eq 1) { Write-Output '未发现 MyBatis SQL 注解' }
rg -n 'recordSuccessfulLogin|last_login_at|last_login_ip|updated_by' 'src/main/resources/mapper/identity/UserAccountMapper.xml'
mvn '-Dtest=DatabaseSuccessfulLoginRecorderTest,DatabaseLoginFailureRecorderTest,LoginCredentialVerifierTest,ArchitectureRulesTest' test
mvn clean test
git diff --check
```

Expected:

- Java 生产代码无 SQL 注解。
- 登录成功 SQL 位于 XML。
- 定向测试和 27 个架构测试通过。
- 全量测试通过；Docker 不可用时 Testcontainers 测试允许跳过。

- [x] **Step 7: 提交登录成功状态更新**

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginFailureRecorder.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginStateRecorder.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifier.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/config/IdentityLoginConfiguration.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisLoginFailureRecorder.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisLoginStateRecorder.java' 'MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/domain/auth/LoginCredentialVerifierTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseLoginFailureRecorderTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/MySqlLoginFailureConcurrencyTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseSuccessfulLoginRecorderTest.java'
git diff --cached --check
git commit -m "实现登录成功状态更新"
```

---

### Task 4: 同步实施结果文档

**Files:**
- Modify: `docs/project-handbook/status.md`
- Modify: `docs/project-handbook/specs/2026-06-12-identity-login-state-design.md`
- Modify: `docs/project-handbook/plans/2026-06-12-identity-login-state-plan.md`

- [x] **Step 1: 用新鲜测试结果更新状态**

在所有代码任务完成后重新运行：

```powershell
mvn clean test
```

将 `status.md` 更新为实际输出的 tests / failures / errors / skipped 数量，不预填估算数字。

将状态文字改为：

```markdown
- identity 后台登录最小纵向切片已完成账号读取、凭据校验、失败累计、持久化锁定和成功登录状态更新
- 下一步设计并实现 Caffeine 登录限流，再进入双 token 登录事务编排与 Controller
```

- [x] **Step 2: 勾选设计和计划验收项**

在设计文档末尾追加“实施结果”，记录三个代码提交 SHA 和验证命令。

将本计划实际完成的 checkbox 改为 `[x]`，不得提前勾选未执行步骤。

- [x] **Step 3: 检查并提交文档**

Run:

```powershell
rg -n '当前基线|下一步' 'docs/project-handbook/status.md'
rg -n '实施结果' 'docs/project-handbook/specs/2026-06-12-identity-login-state-design.md'
git diff --check
```

Expected: `status.md` 显示本轮新鲜测试基线和下一步，设计文档包含实施结果，差异无空白错误。

Commit:

```powershell
git add -- 'docs/project-handbook/status.md' 'docs/project-handbook/specs/2026-06-12-identity-login-state-design.md' 'docs/project-handbook/plans/2026-06-12-identity-login-state-plan.md'
git diff --cached --check
git commit -m "同步登录状态更新实施结果"
```

---

## 最终验收

- [x] ADMIN、DEMO 密码错误才累计；不存在、GUEST、锁定账号不累计。
- [x] 第 1 至第 4 次失败递增计数，第 5 次写入 10 分钟锁定并重置计数。
- [x] 锁定期间不执行 BCrypt，也不再次累计。
- [x] 锁定到期后的首次失败从 1 开始。
- [ ] MySQL 并发 5 次失败不会丢失更新。
- [x] 成功登录写入 `last_login_at` / `last_login_ip`。
- [x] 成功登录清空 `login_fail_count` / `locked_until`。
- [x] 已删除、并发锁定或不存在账号的状态更新失败。
- [x] 状态更新异常不包含用户名、明文密码或密码摘要。
- [x] 所有生产 SQL 位于 XML Mapper。
- [x] domain 不依赖 Spring、MyBatis、Servlet 或 HTTP 错误类型。
- [x] 未引入 Caffeine、Controller 或 token 登录编排。
- [x] 每个代码任务都有明确 RED 和 GREEN 证据。
- [x] `mvn clean test` 通过，工作区干净。
