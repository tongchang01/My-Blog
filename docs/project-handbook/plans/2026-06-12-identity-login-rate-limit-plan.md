# Identity 登录限流实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为后台登录提供可配置的单实例 Caffeine 连续失败限流组件，在同一 IP + username 第 5 次失败后冷却 10 分钟，并为后续登录编排提供稳定领域端口和 HTTP 429 错误码。

**Architecture:** `identity.domain.auth` 只定义框架无关的 `LoginRateLimiter` 端口；配置属性、Caffeine 缓存和组装代码位于基础设施层。缓存以规范化 IP + username 为键，使用 `asMap().compute` 原子累计、`expireAfterWrite` 控制冷却、`maximumSize` 控制内存；本计划不创建 Controller、登录应用服务或 token 编排。

**Tech Stack:** Java 17、Spring Boot 3.5、Caffeine、Spring `@ConfigurationProperties`、JUnit 5、AssertJ、ArchUnit、Maven。

---

## 0. 执行约束

- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`
- Maven 模块：`E:\My-Blog\.worktrees\backend-v2-refactor\MyBlog-springboot-v2`
- 每个任务严格执行 RED → GREEN → 回归 → 独立中文提交。
- 新增 Java 类型、字段和非显然并发逻辑必须有中文注释。
- 构造器使用 Lombok、record 或显式依赖注入，不编写无意义 getter/setter 样板。
- 不新增 Mapper、XML SQL、Entity 或 Flyway 迁移。
- 不实现 Controller、`AuthApplicationService`、双 token 登录事务、验证码、Redis、文章解锁限流或评论限流。
- 不把用户名、客户端 IP、密码、密码摘要或完整限流键写入日志和异常。
- 计划中的第 5 次仅写入阻止状态并仍由调用方返回 `10001`；第 6 次调用 `isBlocked` 才得到 true。

## 1. 文件结构

### Task 1：配置、错误码和领域端口

- Create `common/config/LoginRateLimitProperties.java`
  - 绑定失败阈值、冷却时间和最大缓存容量。
- Create `identity/domain/auth/LoginRateLimiter.java`
  - 定义前置检查、失败记录和成功重置端口。
- Modify `common/error/ApiErrorCode.java`
  - 增加 HTTP 429 + `90002`。
- Modify `application.yml` / `application-test.yml`
  - 增加登录限流默认配置。
- Create / Modify 对应配置、错误码和架构测试。

### Task 2：Caffeine 原子限流实现

- Modify `pom.xml`
  - 引入由 Spring Boot 依赖管理的 Caffeine。
- Create `identity/infrastructure/ratelimit/LoginRateLimitKey.java`
  - 在基础设施内部规范化 IP 和用户名。
- Create `identity/infrastructure/ratelimit/CaffeineLoginRateLimiter.java`
  - 原子累计、固定冷却和容量限制。
- Create `identity/infrastructure/config/IdentityLoginRateLimitConfiguration.java`
  - 创建生产 `Ticker` 和限流器 Bean。
- Create `identity/infrastructure/ratelimit/CaffeineLoginRateLimiterTest.java`
  - 使用可控 Ticker 验证时间、键、并发和容量行为。

### Task 3：同步实施结果

- Modify `docs/project-handbook/status.md`
- Modify `docs/project-handbook/specs/2026-06-12-identity-login-rate-limit-design.md`
- Modify `docs/project-handbook/plans/2026-06-12-identity-login-rate-limit-plan.md`

---

### Task 1: 引入登录限流配置、错误码与领域端口

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/LoginRateLimitProperties.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginRateLimiter.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/error/ApiErrorCode.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/LoginRateLimitPropertiesTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/error/ApiErrorCodeTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`

- [x] **Step 1: 编写配置绑定失败测试**

新增 `LoginRateLimitPropertiesTest`：

```java
package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 登录限流配置测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class LoginRateLimitPropertiesTest {

    @Autowired
    private LoginRateLimitProperties properties;

    @Test
    void shouldBindLoginRateLimitSettings() {
        assertThat(properties.loginMaxFailures()).isEqualTo(5);
        assertThat(properties.loginCooldown()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.loginMaximumSize()).isEqualTo(10_000);
    }

    @Test
    void shouldRejectInvalidSettings() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginRateLimitProperties(
                        0,
                        Duration.ofMinutes(10),
                        10_000));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginRateLimitProperties(
                        5,
                        Duration.ZERO,
                        10_000));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginRateLimitProperties(
                        5,
                        Duration.ofMinutes(10),
                        0));
    }
}
```

- [x] **Step 2: 扩展错误码失败测试**

在 `ApiErrorCodeTest` 增加：

```java
@Test
void shouldExposeRateLimitError() {
    assertThat(ApiErrorCode.RATE_LIMITED.code()).isEqualTo("90002");
    assertThat(ApiErrorCode.RATE_LIMITED.status())
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    assertThat(ApiErrorCode.RATE_LIMITED.defaultMessage())
            .isEqualTo("请求过于频繁");
}
```

补充 import：

```java
import org.springframework.http.HttpStatus;
```

- [x] **Step 3: 增加领域端口架构失败测试**

在 `ArchitectureRulesTest` 现有 domain 规则附近增加：

```java
// identity domain 的限流端口不能泄漏缓存实现或 HTTP 错误类型。
@ArchTest
static final ArchRule identity_domain_does_not_depend_on_rate_limit_implementation =
        noClasses()
                .that().resideInAPackage("..identity.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "com.github.benmanes.caffeine..",
                        "..common.error..")
                .allowEmptyShould(true);
```

Spring、Servlet、MyBatis 和 infrastructure 依赖继续由该测试类已有 domain 规则守护，不新建第二套扫描入口。

- [x] **Step 4: 运行测试并确认 RED**

Run:

```powershell
mvn '-Dtest=LoginRateLimitPropertiesTest,ApiErrorCodeTest,ArchitectureRulesTest' test
```

Expected: 测试编译失败，提示 `LoginRateLimitProperties`、`LoginRateLimiter` 或 `RATE_LIMITED` 不存在。

- [x] **Step 5: 实现配置属性**

新增 `LoginRateLimitProperties`：

```java
package com.tyb.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 单实例登录失败限流配置。
 *
 * @param loginMaxFailures 单个 IP 与用户名组合允许的连续失败次数
 * @param loginCooldown 达到阈值后的冷却时间
 * @param loginMaximumSize 登录限流缓存最大条目数
 */
@ConfigurationProperties("myblog.ratelimit")
public record LoginRateLimitProperties(
        int loginMaxFailures,
        Duration loginCooldown,
        long loginMaximumSize
) {

    public LoginRateLimitProperties {
        if (loginMaxFailures < 1) {
            throw new IllegalArgumentException("登录限流失败阈值必须大于 0");
        }
        if (loginCooldown == null || loginCooldown.isZero() || loginCooldown.isNegative()) {
            throw new IllegalArgumentException("登录限流冷却时间必须为正数");
        }
        if (loginMaximumSize < 1) {
            throw new IllegalArgumentException("登录限流缓存容量必须大于 0");
        }
    }
}
```

在 `application.yml` 的 `myblog` 下增加：

```yaml
ratelimit:
  login-max-failures: 5
  login-cooldown: 10m
  login-maximum-size: 10000
```

在 `application-test.yml` 使用相同测试配置：

```yaml
ratelimit:
  login-max-failures: 5
  login-cooldown: 10m
  login-maximum-size: 10000
```

配置由主应用现有 `@ConfigurationPropertiesScan` 自动发现，不在 `SecurityConfig` 重复注册。

- [x] **Step 6: 实现领域端口**

新增 `LoginRateLimiter`：

```java
package com.tyb.myblog.v2.identity.domain.auth;

/**
 * 后台登录连续失败限流端口。
 */
public interface LoginRateLimiter {

    /**
     * 判断当前 IP 与用户名组合是否处于冷却期。
     *
     * @param clientIp 可信客户端 IP，允许为空
     * @param normalizedUsername 已规范化的登录用户名
     * @return 达到失败阈值且冷却尚未结束时返回 {@code true}
     */
    boolean isBlocked(String clientIp, String normalizedUsername);

    /**
     * 原子记录一次已确认的凭据失败。
     *
     * @param clientIp 可信客户端 IP，允许为空
     * @param normalizedUsername 已规范化的登录用户名
     */
    void recordFailure(String clientIp, String normalizedUsername);

    /**
     * 登录凭据成功后清除当前组合的连续失败状态。
     *
     * @param clientIp 可信客户端 IP，允许为空
     * @param normalizedUsername 已规范化的登录用户名
     */
    void reset(String clientIp, String normalizedUsername);
}
```

- [x] **Step 7: 实现 HTTP 429 错误码**

在 `ApiErrorCode` 的 `VALIDATION_ERROR` 后增加：

```java
/**
 * 请求超过基础设施限流阈值，对应 HTTP 429。
 */
RATE_LIMITED("90002", HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁"),
```

- [x] **Step 8: 运行定向测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=LoginRateLimitPropertiesTest,ApiErrorCodeTest,ArchitectureRulesTest,ApplicationConfigurationTest' test
```

Expected:

- 配置绑定和非法值校验通过。
- `90002` 映射 HTTP 429。
- 既有架构测试加新增限流端口规则全部通过。
- Spring 上下文可以绑定新配置。

- [x] **Step 9: 执行本任务回归和静态检查**

Run:

```powershell
rg -n 'LoginRateLimitProperties|LoginRateLimiter|RATE_LIMITED' 'src/main/java' 'src/test/java'
$matches = rg -n 'com\.github\.benmanes\.caffeine|org\.springframework|jakarta\.servlet|ApiErrorCode' 'src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginRateLimiter.java'
if ($LASTEXITCODE -eq 1) { Write-Output '领域端口未依赖框架或 HTTP 类型' }
git diff --check
```

Expected: 新类型和错误码可定位；领域端口不包含框架、Servlet、Caffeine 或 `ApiErrorCode` 依赖；差异无空白错误。

- [x] **Step 10: 提交配置、错误码与领域端口**

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/LoginRateLimitProperties.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/error/ApiErrorCode.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/LoginRateLimiter.java' 'MyBlog-springboot-v2/src/main/resources/application.yml' 'MyBlog-springboot-v2/src/test/resources/application-test.yml' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/LoginRateLimitPropertiesTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/error/ApiErrorCodeTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java'
git diff --cached --check
git diff --cached --stat
git commit -m "引入登录限流配置与领域端口"
```

---

### Task 2: 实现 Caffeine 登录失败限流

**Files:**
- Modify: `MyBlog-springboot-v2/pom.xml`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/ratelimit/LoginRateLimitKey.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/ratelimit/CaffeineLoginRateLimiter.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/config/IdentityLoginRateLimitConfiguration.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/ratelimit/CaffeineLoginRateLimiterTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/ApplicationConfigurationTest.java`

- [x] **Step 1: 引入 Caffeine 依赖**

在 `pom.xml` 的依赖区增加，版本由 Spring Boot 3.5.14 dependency management 管理：

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

Run:

```powershell
mvn dependency:tree '-Dincludes=com.github.ben-manes.caffeine:caffeine'
```

Expected: 依赖树只出现一套收敛后的 Caffeine 版本，Maven Enforcer 不报 dependency convergence。

- [x] **Step 2: 编写阈值、键和重置失败测试**

新增 `CaffeineLoginRateLimiterTest` 的首批测试：

```java
package com.tyb.myblog.v2.identity.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import com.tyb.myblog.v2.common.config.LoginRateLimitProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Caffeine 登录失败限流器测试。
 */
class CaffeineLoginRateLimiterTest {

    private static final String IP = "127.0.0.1";
    private static final String USERNAME = "admin";

    @Test
    void shouldBlockOnlyAfterFifthFailureIsRecorded() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);

        assertThat(limiter.isBlocked(IP, USERNAME)).isFalse();
        for (int attempt = 1; attempt <= 4; attempt++) {
            limiter.recordFailure(IP, USERNAME);
            assertThat(limiter.isBlocked(IP, USERNAME)).isFalse();
        }

        limiter.recordFailure(IP, USERNAME);

        assertThat(limiter.isBlocked(IP, USERNAME)).isTrue();
    }

    @Test
    void shouldResetAfterSuccessfulCredentials() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);
        recordFailures(limiter, 5);

        limiter.reset(IP, USERNAME);

        assertThat(limiter.isBlocked(IP, USERNAME)).isFalse();
    }

    @Test
    void shouldNormalizeIpAndUsernameIntoSameKey() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);

        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.recordFailure(" 2001:DB8::1 ", " Admin ");
        }

        assertThat(limiter.isBlocked("2001:db8::1", "admin")).isTrue();
    }

    @Test
    void shouldUseUnknownIpBucketAndKeepDifferentKeysIndependent() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);
        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.recordFailure(null, "admin");
        }

        assertThat(limiter.isBlocked(" ", "ADMIN")).isTrue();
        assertThat(limiter.isBlocked("127.0.0.1", "admin")).isFalse();
        assertThat(limiter.isBlocked(null, "other")).isFalse();
    }

    private static CaffeineLoginRateLimiter limiter(Ticker ticker, long maximumSize) {
        return new CaffeineLoginRateLimiter(
                new LoginRateLimitProperties(
                        5,
                        Duration.ofMinutes(10),
                        maximumSize),
                ticker);
    }

    private static void recordFailures(CaffeineLoginRateLimiter limiter, int count) {
        for (int attempt = 0; attempt < count; attempt++) {
            limiter.recordFailure(IP, USERNAME);
        }
    }

    private static final class MutableTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
```

- [x] **Step 3: 运行首批测试并确认 RED**

Run:

```powershell
mvn '-Dtest=CaffeineLoginRateLimiterTest' test
```

Expected: 编译失败，提示 `CaffeineLoginRateLimiter` 不存在。

- [x] **Step 4: 实现限流键**

新增包内不可见 `LoginRateLimitKey`：

```java
package com.tyb.myblog.v2.identity.infrastructure.ratelimit;

import java.util.Locale;

/**
 * 登录限流缓存键。
 *
 * @param clientIp 规范化客户端 IP
 * @param username 规范化用户名
 */
record LoginRateLimitKey(String clientIp, String username) {

    private static final String UNKNOWN_IP = "<unknown>";

    LoginRateLimitKey {
        clientIp = normalizeIp(clientIp);
        username = normalizeUsername(username);
    }

    private static String normalizeIp(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_IP;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeUsername(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("登录限流用户名不能为空");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
```

用户名空值由未来 Controller Bean Validation 阻止；基础设施仍拒绝空值，避免生成所有非法请求共享的空用户名键。

- [x] **Step 5: 实现最小 Caffeine 限流器**

新增 `CaffeineLoginRateLimiter`：

```java
package com.tyb.myblog.v2.identity.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.tyb.myblog.v2.common.config.LoginRateLimitProperties;
import com.tyb.myblog.v2.identity.domain.auth.LoginRateLimiter;

/**
 * 基于单实例 Caffeine 缓存的登录连续失败限流器。
 */
public class CaffeineLoginRateLimiter implements LoginRateLimiter {

    private final Cache<LoginRateLimitKey, Integer> failures;
    private final int maxFailures;

    public CaffeineLoginRateLimiter(
            LoginRateLimitProperties properties,
            Ticker ticker
    ) {
        this.maxFailures = properties.loginMaxFailures();
        this.failures = Caffeine.newBuilder()
                .expireAfterWrite(properties.loginCooldown())
                .maximumSize(properties.loginMaximumSize())
                .ticker(ticker)
                .build();
    }

    @Override
    public boolean isBlocked(String clientIp, String normalizedUsername) {
        Integer failureCount = failures.getIfPresent(
                new LoginRateLimitKey(clientIp, normalizedUsername));
        return failureCount != null && failureCount >= maxFailures;
    }

    @Override
    public void recordFailure(String clientIp, String normalizedUsername) {
        LoginRateLimitKey key = new LoginRateLimitKey(clientIp, normalizedUsername);
        failures.asMap().compute(key, (ignored, current) -> {
            int next = current == null ? 1 : current + 1;
            // 达到阈值后保持固定值，避免攻击流量导致计数无界增长。
            return Math.min(next, maxFailures);
        });
    }

    @Override
    public void reset(String clientIp, String normalizedUsername) {
        failures.invalidate(new LoginRateLimitKey(clientIp, normalizedUsername));
    }

    /**
     * 返回维护后的缓存估算大小，仅供同包测试验证容量边界。
     */
    long estimatedSizeAfterCleanup() {
        failures.cleanUp();
        return failures.estimatedSize();
    }
}
```

- [x] **Step 6: 运行首批测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=CaffeineLoginRateLimiterTest' test
```

Expected: 阈值、重置、规范化、未知 IP 和键隔离测试全部通过。

- [x] **Step 7: 增加时间、并发和容量失败测试**

在 `CaffeineLoginRateLimiterTest` 增加：

```java
@Test
void shouldExpireTenMinutesAfterFifthFailureWithoutExtendingOnRead() {
    MutableTicker ticker = new MutableTicker();
    CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);
    recordFailures(limiter, 4);
    ticker.advance(Duration.ofMinutes(3));
    limiter.recordFailure(IP, USERNAME);

    ticker.advance(Duration.ofMinutes(9));
    assertThat(limiter.isBlocked(IP, USERNAME)).isTrue();

    ticker.advance(Duration.ofMinutes(1));
    assertThat(limiter.isBlocked(IP, USERNAME)).isFalse();
}

@Test
void shouldKeepConcurrentFailureCountWithoutGrowingBeyondThreshold() throws Exception {
    MutableTicker ticker = new MutableTicker();
    CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);
    int threadCount = 20;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch start = new CountDownLatch(1);

    try {
        List<? extends Future<?>> futures = IntStream.range(0, threadCount)
                .mapToObj(index -> executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    limiter.recordFailure(IP, USERNAME);
                    return null;
                }))
                .toList();

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
    } finally {
        executor.shutdownNow();
    }

    assertThat(limiter.isBlocked(IP, USERNAME)).isTrue();
    assertThat(limiter.failureCountForTesting(IP, USERNAME)).isEqualTo(5);
}

@Test
void shouldBoundCacheSize() {
    MutableTicker ticker = new MutableTicker();
    CaffeineLoginRateLimiter limiter = limiter(ticker, 2);

    limiter.recordFailure("127.0.0.1", "user-1");
    limiter.recordFailure("127.0.0.1", "user-2");
    limiter.recordFailure("127.0.0.1", "user-3");

    assertThat(limiter.estimatedSizeAfterCleanup()).isLessThanOrEqualTo(2);
}
```

补充 import：

```java
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
```

- [x] **Step 8: 为并发封顶断言增加包内测试观察方法**

在 `CaffeineLoginRateLimiter` 增加：

```java
/**
 * 读取当前失败次数，仅供同包并发测试验证原子累计和阈值封顶。
 */
int failureCountForTesting(String clientIp, String normalizedUsername) {
    Integer count = failures.getIfPresent(
            new LoginRateLimitKey(clientIp, normalizedUsername));
    return count == null ? 0 : count;
}
```

该方法保持包内可见，不进入 `LoginRateLimiter` 领域端口，也不被生产调用方依赖。

- [x] **Step 9: 运行完整限流器测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=CaffeineLoginRateLimiterTest' test
```

Expected: 时间推进无需 `Thread.sleep`；并发 20 次失败后计数固定为 5；容量清理后估算大小不超过 2。

- [x] **Step 10: 编写 Spring Bean 装配失败测试**

在 `ApplicationConfigurationTest` 增加注入字段：

```java
@Autowired
private LoginRateLimiter loginRateLimiter;
```

增加测试：

```java
@Test
void shouldProvideLoginRateLimiter() {
    assertThat(loginRateLimiter).isInstanceOf(CaffeineLoginRateLimiter.class);
}
```

补充 import：

```java
import com.tyb.myblog.v2.identity.domain.auth.LoginRateLimiter;
import com.tyb.myblog.v2.identity.infrastructure.ratelimit.CaffeineLoginRateLimiter;
```

- [x] **Step 11: 运行上下文测试并确认 RED**

Run:

```powershell
mvn '-Dtest=ApplicationConfigurationTest' test
```

Expected: Spring 注入失败，提示不存在 `LoginRateLimiter` Bean。

- [x] **Step 12: 实现限流器 Spring 装配**

新增 `IdentityLoginRateLimitConfiguration`：

```java
package com.tyb.myblog.v2.identity.infrastructure.config;

import com.github.benmanes.caffeine.cache.Ticker;
import com.tyb.myblog.v2.common.config.LoginRateLimitProperties;
import com.tyb.myblog.v2.identity.domain.auth.LoginRateLimiter;
import com.tyb.myblog.v2.identity.infrastructure.ratelimit.CaffeineLoginRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 后台登录限流基础设施配置。
 */
@Configuration
public class IdentityLoginRateLimitConfiguration {

    /**
     * 创建生产环境使用的单调时钟。
     */
    @Bean
    Ticker loginRateLimitTicker() {
        return Ticker.systemTicker();
    }

    /**
     * 创建单实例 Caffeine 登录失败限流器。
     */
    @Bean
    LoginRateLimiter loginRateLimiter(
            LoginRateLimitProperties properties,
            Ticker loginRateLimitTicker
    ) {
        return new CaffeineLoginRateLimiter(properties, loginRateLimitTicker);
    }
}
```

- [x] **Step 13: 运行装配、限流和架构测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=CaffeineLoginRateLimiterTest,ApplicationConfigurationTest,LoginRateLimitPropertiesTest,ApiErrorCodeTest,ArchitectureRulesTest' test
```

Expected: Caffeine 行为、Spring Bean、配置、错误码和全部架构规则通过。

- [x] **Step 14: 执行依赖、范围和全量验证**

Run:

```powershell
mvn dependency:tree '-Dincludes=com.github.ben-manes.caffeine:caffeine'
$matches = rg -n 'Caffeine|com\.github\.benmanes' 'src/main/java/com/tyb/myblog/v2/identity/domain'
if ($LASTEXITCODE -eq 1) { Write-Output 'domain 未依赖 Caffeine' }
$matches = rg -n '@(Select|Insert|Update|Delete)\b' 'src/main/java'
if ($LASTEXITCODE -eq 1) { Write-Output '未发现 MyBatis SQL 注解' }
rg -n 'Thread\.sleep' 'src/test/java/com/tyb/myblog/v2/identity/infrastructure/ratelimit'
mvn clean test
git diff --check
```

Expected:

- Caffeine 依赖收敛。
- domain 无 Caffeine 依赖。
- 生产代码无 MyBatis SQL 注解。
- 限流测试不使用 `Thread.sleep`。
- 全量测试通过；Docker 不可用时只允许既有两个 Testcontainers MySQL 测试跳过。

- [x] **Step 15: 提交 Caffeine 限流实现**

```powershell
git add -- 'MyBlog-springboot-v2/pom.xml' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/config/IdentityLoginRateLimitConfiguration.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/ratelimit/LoginRateLimitKey.java' 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/ratelimit/CaffeineLoginRateLimiter.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/ratelimit/CaffeineLoginRateLimiterTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/ApplicationConfigurationTest.java'
git diff --cached --check
git diff --cached --stat
git commit -m "实现Caffeine登录失败限流"
```

---

### Task 3: 同步登录限流实施结果

**Files:**
- Modify: `docs/project-handbook/status.md`
- Modify: `docs/project-handbook/specs/2026-06-12-identity-login-rate-limit-design.md`
- Modify: `docs/project-handbook/plans/2026-06-12-identity-login-rate-limit-plan.md`

- [x] **Step 1: 运行新鲜全量验证**

Run:

```powershell
mvn clean test
```

记录 Maven 最终输出中的 tests、failures、errors、skipped，禁止使用预估数字。Docker 不可用时明确写出被跳过的 Testcontainers 测试。

- [x] **Step 2: 更新当前状态**

将 `status.md` 的 identity 状态更新为：

```markdown
- identity 后台登录基础能力已完成账号读取、凭据校验、失败累计、持久化锁定、成功状态更新和单实例 Caffeine 登录限流
- 下一步设计双 token 登录事务编排与 Controller，把限流、凭据校验、审计和 token 签发接入完整登录用例
```

将当前测试基线替换为 Step 1 的真实输出。

- [x] **Step 3: 记录设计实施结果**

在设计文档末尾追加：

```markdown
## 11. 实施结果

本设计按两个独立代码提交落地，依次记录“引入登录限流配置与领域端口”和“实现 Caffeine 登录失败限流”的真实短 SHA。

验证结果记录以下事实：

- 配置、错误码、Caffeine 阈值、冷却、重置、键隔离、并发和容量测试通过。
- domain 未依赖 Caffeine、Spring、Servlet 或 HTTP 类型。
- 未新增 Mapper、SQL 或 Flyway 迁移。
- `mvn clean test` 的真实 tests、failures、errors、skipped 数字。
- 限流组件尚未接入 HTTP 登录流程；接入工作属于下一轮双 token 登录事务编排与 Controller。
```

先运行以下命令取得真实提交信息：

```powershell
git log -2 --format='%h %s'
```

再使用 `apply_patch` 写入真实 SHA 和 Maven 输出，不在文档中保留示例值或占位符。

- [x] **Step 4: 勾选本计划实际完成项**

将 Task 1、Task 2 和 Task 3 已执行步骤改为 `[x]`。最终验收只勾选当前独立组件可以证明的事项；“登录请求在第 6 次返回 HTTP 429”属于尚未实现的 Controller/应用编排，不得提前勾选。

- [x] **Step 5: 检查文档**

Run:

```powershell
rg -n '当前基线|下一步' 'docs/project-handbook/status.md'
rg -n '实施结果|Caffeine|mvn clean test' 'docs/project-handbook/specs/2026-06-12-identity-login-rate-limit-design.md'
rg -n '待补|待填|占位符' 'docs/project-handbook/specs/2026-06-12-identity-login-rate-limit-design.md' 'docs/project-handbook/status.md'
git diff --check
```

Expected: 状态和实施结果包含真实数据，项目手册中没有本计划遗留占位符，差异无空白错误。

- [x] **Step 6: 提交文档**

```powershell
git add -- 'docs/project-handbook/status.md' 'docs/project-handbook/specs/2026-06-12-identity-login-rate-limit-design.md' 'docs/project-handbook/plans/2026-06-12-identity-login-rate-limit-plan.md'
git diff --cached --check
git diff --cached --stat
git commit -m "同步登录限流实施结果"
```

---

## 最终验收

- [x] 登录限流配置可绑定 5 次、10 分钟和最大容量 10000。
- [x] 非正阈值、冷却时间和缓存容量会被拒绝。
- [x] `LoginRateLimiter` 位于 domain 且不依赖框架、Servlet、HTTP 或 Caffeine。
- [x] 第 1 至第 4 次失败不阻止，第 5 次记录后进入阻止状态。
- [x] 第 5 次失败由调用方继续按 `10001` 处理；未来第 6 次请求可在凭据校验前检测到阻止。
- [x] 冷却期检查不延长过期时间，10 分钟后自动允许。
- [x] 成功重置后立即允许新的失败周期。
- [x] IP 和用户名组合隔离，用户名大小写与首尾空白被规范化。
- [x] 空 IP 使用 `<unknown>` 固定桶参与限流。
- [x] 并发失败累计不丢失，缓存计数封顶为阈值。
- [x] Caffeine 缓存受最大容量限制。
- [x] `90002` 对应 HTTP 429 和中文默认消息。
- [x] 未新增 Mapper、SQL、Entity、Flyway、Controller、token 编排或 Redis。
- [x] 全量 `mvn clean test` 通过，工作树干净。
- [ ] HTTP 登录接口第 6 次实际返回 429 + `90002`。（留待下一轮登录编排与 Controller）
