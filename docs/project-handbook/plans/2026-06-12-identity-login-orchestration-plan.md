# Identity 双 Token 登录编排与 Controller Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成后台登录最小纵向切片，提供符合冻结响应契约的 `POST /api/auth/login`，并把限流、凭据校验、登录审计、refresh token 持久化和 access token 签发接入同一用例。

**Architecture:** 使用无事务的 `AuthApplicationService` 编排限流与凭据结果，使用独立 `LoginSuccessTransactionService` 在短事务内完成成功审计、refresh token 持久化和 access token 签发。Controller 只负责 Bean Validation、可信客户端 IP 提取和 DTO 转换；统一响应先纠正为 `code/msg/data`。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring MVC、Spring Security、Spring Transaction、MyBatis-Plus、H2、Caffeine、Bean Validation、springdoc-openapi、Lombok、JUnit 5、Mockito、AssertJ、MockMvc、ArchUnit、Maven。

---

## 0. 执行约束

- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`
- Maven 模块：`E:\My-Blog\.worktrees\backend-v2-refactor\MyBlog-springboot-v2`
- 设计依据：`docs/project-handbook/specs/2026-06-12-identity-login-orchestration-design.md`
- 严格按 Task 1 → Task 5 执行，不跨批次提前修改后续文件。
- 每个代码任务执行 RED → GREEN → 定向回归 → 规则扫描 → 独立中文提交。
- Java 类型、字段和非显然逻辑使用中文注释。
- DTO、命令和结果使用 record；依赖注入使用 Lombok `@RequiredArgsConstructor`，不手写模板 getter/setter。
- SQL 只允许位于 XML Mapper。本轮不新增 SQL、Entity 或 Flyway 迁移。
- 不实现 refresh、logout、当前用户、修改密码、验证码、Redis或前端 token 存储。
- 不记录用户名与 IP 组成的完整限流键、明文密码、密码摘要、access token 或 refresh token。
- Docker 暂不作为本轮通过条件；只允许现有两个 Testcontainers MySQL 测试按既定策略跳过。

## 1. 文件结构

### Task 1：统一 API 响应契约

- Modify `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/web/ApiResponse.java`
  - 删除 `success`，把 `message` 改为 `msg`，成功码改为 `00000`。
- Create `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/web/ApiResponseTest.java`
  - 验证成功和失败 JSON 契约。
- Modify `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/error/GlobalExceptionHandlerTest.java`
  - 把旧字段断言改为 `code/msg/data`，并断言旧字段不存在。
- Modify `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`
  - 保持安全异常测试与新字段一致。

### Task 2：双 Token 登录应用编排

- Create `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/LoginCommand.java`
  - 承载已通过 web 校验的用户名、密码和可信 IP。
- Create `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/LoginTokenResult.java`
  - 承载双 token 和两个 TTL 秒数。
- Create `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/LoginSuccessTransactionService.java`
  - 短事务内完成成功审计、refresh token 持久化和 access token 签发。
- Create `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/AuthApplicationService.java`
  - 无事务编排限流、凭据校验、错误映射和成功事务。
- Create `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/AuthApplicationServiceTest.java`
  - 纯单元测试外层分支和调用顺序。
- Create `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/LoginSuccessTransactionServiceUnitTest.java`
  - 单元验证成功事务参数与结果组装。

### Task 3：后台登录 HTTP 接口

- Create `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/LoginRequest.java`
  - Bean Validation 请求 DTO。
- Create `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/LoginTokenVO.java`
  - 面向前端的登录 token 响应。
- Create `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/AuthController.java`
  - 提供 `POST /api/auth/login`。
- Create `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/AuthControllerTest.java`
  - web slice 验证参数、IP 传递和响应转换。
- Modify `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- Modify `MyBlog-springboot-v2/src/test/resources/application-test.yml`
  - 精确公开 `POST /api/auth/login`。
- Modify `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`
  - 验证登录端点匿名可访问且方法维度不放宽。

### Task 4：事务与完整 HTTP 验收

- Create `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/LoginSuccessTransactionServiceIntegrationTest.java`
  - 验证成功提交以及 access token 签发失败时审计和 refresh token 一起回滚。
- Create `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/AuthLoginIntegrationTest.java`
  - 使用真实 Spring、H2、BCrypt、Caffeine、JWT 和 MockMvc 验证完整登录链。
- Modify `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
  - 明确 web/application/domain 边界继续生效。

### Task 5：实施结果与权威文档同步

- Modify `docs/project-handbook/status.md`
- Modify `docs/project-handbook/roadmap.md`
- Modify `docs/project-handbook/arch/auth-flow.md`
- Modify `docs/project-handbook/rules/api-response.md`
- Modify `docs/project-handbook/arch/request-flow.md`
- Create `docs/project-handbook/api-contract/auth.md`
- Modify `docs/project-handbook/api-contract/README.md`
- Modify `docs/project-handbook/specs/2026-06-12-identity-login-orchestration-design.md`
- Modify `docs/project-handbook/plans/2026-06-12-identity-login-orchestration-plan.md`

---

### Task 1: 纠正统一 API 响应契约

**Files:**
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/web/ApiResponse.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/web/ApiResponseTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/error/GlobalExceptionHandlerTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`

- [ ] **Step 1: 编写 ApiResponse 冻结契约失败测试**

新增 `ApiResponseTest`：

```java
package com.tyb.myblog.v2.common.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一 API 响应契约测试。
 */
class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesSuccessWithFrozenFieldsAndCode() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(ApiResponse.ok("payload")));

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.has("code")).isTrue();
        assertThat(json.has("msg")).isTrue();
        assertThat(json.has("data")).isTrue();
        assertThat(json.path("code").asText()).isEqualTo("00000");
        assertThat(json.path("msg").asText()).isEqualTo("success");
        assertThat(json.path("data").asText()).isEqualTo("payload");
        assertThat(json.has("success")).isFalse();
        assertThat(json.has("message")).isFalse();
    }

    @Test
    void serializesFailureWithNullData() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(ApiResponse.fail("10001", "用户名或密码错误")));

        assertThat(json.path("code").asText()).isEqualTo("10001");
        assertThat(json.path("msg").asText()).isEqualTo("用户名或密码错误");
        assertThat(json.path("data").isNull()).isTrue();
    }
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```powershell
mvn '-Dtest=ApiResponseTest' test
```

Expected: 测试失败，实际 JSON 仍包含 `success/message`，成功码仍为 `OK`。

- [ ] **Step 3: 实现最小 ApiResponse 契约**

将 `ApiResponse` 改为：

```java
package com.tyb.myblog.v2.common.web;

/**
 * 统一 API 响应体。
 *
 * @param code 5 位业务错误码，成功时固定为 {@code 00000}
 * @param msg 人类可读消息
 * @param data 成功响应的数据载荷，失败时为 {@code null}
 */
public record ApiResponse<T>(String code, String msg, T data) {

    /**
     * 创建成功响应。
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("00000", "success", data);
    }

    /**
     * 创建失败响应。
     */
    public static ApiResponse<Void> fail(String code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }
}
```

- [ ] **Step 4: 更新现有 HTTP 响应断言**

在 `GlobalExceptionHandlerTest`：

- 删除全部 `$.success` 断言。
- 把 `$.message` 改为 `$.msg`。
- 每个失败分支增加 `$.data` 为空。
- 至少一个分支增加 `$.success`、`$.message` 不存在断言。

关键断言：

```java
.andExpect(jsonPath("$.code").value("90001"))
.andExpect(jsonPath("$.msg").value("title must not be blank"))
.andExpect(jsonPath("$.data").isEmpty())
.andExpect(jsonPath("$.success").doesNotExist())
.andExpect(jsonPath("$.message").doesNotExist());
```

检查 `SecurityConfigTest`，若存在 `message` 或 `success` 断言，同步改为 `msg/data`；现有只断言 `code` 的分支保持不动。

- [ ] **Step 5: 运行定向测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=ApiResponseTest,GlobalExceptionHandlerTest,SecurityConfigTest' test
```

Expected: 三个测试类全部通过。

- [ ] **Step 6: 扫描旧响应字段**

Run:

```powershell
rg -n '\.success\(\)|\.message\(\)|"\$\.success"|"\$\.message"|"OK"' src/main/java src/test/java
```

Expected: 不再发现依赖旧 `ApiResponse` 字段或旧成功码的生产代码和测试；与业务无关的普通字符串命中需人工确认。

- [ ] **Step 7: 提交响应契约批次**

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/web/ApiResponse.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/web/ApiResponseTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/error/GlobalExceptionHandlerTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java'
git diff --cached --check
git diff --cached --stat
git commit -m "纠正统一API响应契约"
```

---

### Task 2: 实现双 Token 登录应用编排

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/LoginCommand.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/LoginTokenResult.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/LoginSuccessTransactionService.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/AuthApplicationService.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/AuthApplicationServiceTest.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/LoginSuccessTransactionServiceUnitTest.java`

- [ ] **Step 1: 编写外层登录编排失败测试**

`AuthApplicationServiceTest` 使用 Mockito 创建：

```java
private final LoginCredentialVerifier credentialVerifier = mock(LoginCredentialVerifier.class);
private final LoginRateLimiter rateLimiter = mock(LoginRateLimiter.class);
private final LoginSuccessTransactionService successService =
        mock(LoginSuccessTransactionService.class);
private final Clock clock = Clock.fixed(
        Instant.parse("2026-06-12T03:00:00Z"),
        ZoneId.of("Asia/Tokyo"));

private final AuthApplicationService service = new AuthApplicationService(
        credentialVerifier,
        rateLimiter,
        successService,
        clock);
```

至少覆盖：

```java
@Test
void rejectsBlockedKeyBeforeCredentialVerification() {
    when(rateLimiter.isBlocked("203.0.113.10", "admin")).thenReturn(true);

    assertThatThrownBy(() -> service.login(
            new LoginCommand(" Admin ", "secret", "203.0.113.10")))
            .isInstanceOfSatisfying(ApiException.class,
                    ex -> assertThat(ex.code()).isEqualTo(ApiErrorCode.RATE_LIMITED));

    verifyNoInteractions(credentialVerifier, successService);
}

@Test
void recordsCaffeineFailureForBadCredentials() {
    when(credentialVerifier.verify(
            "admin",
            "secret",
            LocalDateTime.of(2026, 6, 12, 12, 0)))
            .thenReturn(LoginCredentialResult.BadCredentials.INSTANCE);

    assertThatThrownBy(() -> service.login(
            new LoginCommand(" Admin ", "secret", "203.0.113.10")))
            .isInstanceOfSatisfying(ApiException.class,
                    ex -> assertThat(ex.code()).isEqualTo(ApiErrorCode.BAD_CREDENTIALS));

    verify(rateLimiter).recordFailure("203.0.113.10", "admin");
    verifyNoInteractions(successService);
}

@Test
void mapsLockedAccountWithoutGrowingCaffeineCount() {
    when(credentialVerifier.verify(anyString(), anyString(), any()))
            .thenReturn(LoginCredentialResult.Locked.INSTANCE);

    assertThatThrownBy(() -> service.login(
            new LoginCommand("admin", "secret", "203.0.113.10")))
            .isInstanceOfSatisfying(ApiException.class,
                    ex -> assertThat(ex.code()).isEqualTo(ApiErrorCode.BAD_CREDENTIALS));

    verify(rateLimiter, never()).recordFailure(anyString(), anyString());
    verifyNoInteractions(successService);
}
```

成功分支使用固定 `UserAccount`，断言：

- 规范化用户名同时用于限流和凭据校验。
- 密码原样传递。
- `reset` 发生在 `successService.complete(...)` 之前，使用 Mockito `InOrder`。
- 成功事务异常原样传播，不转换为 `10001`。

- [ ] **Step 2: 运行外层编排测试并确认 RED**

Run:

```powershell
mvn '-Dtest=AuthApplicationServiceTest' test
```

Expected: 编译失败，提示 `LoginCommand`、`AuthApplicationService` 和 `LoginSuccessTransactionService` 不存在。

- [ ] **Step 3: 新增应用命令与结果**

`LoginCommand`：

```java
package com.tyb.myblog.v2.identity.application.auth;

/**
 * 后台登录应用命令。
 *
 * @param username 未规范化的登录用户名
 * @param password 原样传递的明文密码
 * @param clientIp 可信客户端 IP，允许为空
 */
public record LoginCommand(String username, String password, String clientIp) {
}
```

`LoginTokenResult`：

```java
package com.tyb.myblog.v2.identity.application.auth;

/**
 * 后台登录成功后的双 token 结果。
 */
public record LoginTokenResult(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn
) {
}
```

- [ ] **Step 4: 实现 AuthApplicationService**

```java
package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.auth.LoginCredentialResult;
import com.tyb.myblog.v2.identity.domain.auth.LoginCredentialVerifier;
import com.tyb.myblog.v2.identity.domain.auth.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 后台登录用例编排。
 */
@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final LoginCredentialVerifier credentialVerifier;
    private final LoginRateLimiter rateLimiter;
    private final LoginSuccessTransactionService successTransactionService;
    private final Clock clock;

    /**
     * 执行后台登录，不在 BCrypt 和失败分支外层开启数据库事务。
     */
    public LoginTokenResult login(LoginCommand command) {
        String username = command.username().trim().toLowerCase(Locale.ROOT);
        if (rateLimiter.isBlocked(command.clientIp(), username)) {
            throw new ApiException(ApiErrorCode.RATE_LIMITED);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LoginCredentialResult credentialResult = credentialVerifier.verify(
                username,
                command.password(),
                now);

        if (credentialResult instanceof LoginCredentialResult.BadCredentials) {
            rateLimiter.recordFailure(command.clientIp(), username);
            throw new ApiException(ApiErrorCode.BAD_CREDENTIALS);
        }
        if (credentialResult instanceof LoginCredentialResult.Locked) {
            throw new ApiException(ApiErrorCode.BAD_CREDENTIALS);
        }

        var authenticated = (LoginCredentialResult.Authenticated) credentialResult;
        rateLimiter.reset(command.clientIp(), username);
        return successTransactionService.complete(
                authenticated.account(),
                command.clientIp(),
                now);
    }
}
```

同一次登录只读取一次业务时间，并同时传给凭据校验和成功事务，避免出现两个时间点。

- [ ] **Step 5: 编写成功事务服务失败测试**

`LoginSuccessTransactionServiceUnitTest` 使用 mock：

- `LoginStateRecorder`
- `RefreshTokenService`
- `AccessTokenIssuer`
- `SecurityJwtProperties`

固定数据：

```java
UserAccount account = new UserAccount(
        1001L,
        "admin",
        "hash",
        AccountType.ADMIN,
        3,
        0,
        null);
LocalDateTime loggedInAt = LocalDateTime.of(2026, 6, 12, 12, 0);
IssuedRefreshToken refresh = new IssuedRefreshToken(
        1001L,
        "refresh-value",
        loggedInAt.plusDays(7));
TokenPair access = new TokenPair(
        "access-value",
        Instant.parse("2026-06-12T03:15:00Z"));
```

断言：

```java
verify(loginStateRecorder).recordSuccessfulLogin(
        1001L, loggedInAt, "203.0.113.10");
verify(refreshTokenService).issue(1001L);
verify(accessTokenIssuer).issueAccessToken(
        "1001", "admin", List.of("ADMIN"), 3);
assertThat(result).isEqualTo(new LoginTokenResult(
        "access-value", "refresh-value", 900, 604800));
```

另加 DEMO 测试，断言角色为 `List.of("DEMO")`。

- [ ] **Step 6: 运行成功事务测试并确认 RED**

Run:

```powershell
mvn '-Dtest=LoginSuccessTransactionServiceUnitTest' test
```

Expected: 编译失败，提示 `LoginSuccessTransactionService` 不存在。

- [ ] **Step 7: 实现短成功事务**

```java
package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.auth.LoginStateRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台登录成功后的短事务。
 */
@Service
@RequiredArgsConstructor
public class LoginSuccessTransactionService {

    private final LoginStateRecorder loginStateRecorder;
    private final RefreshTokenService refreshTokenService;
    private final AccessTokenIssuer accessTokenIssuer;
    private final SecurityJwtProperties jwtProperties;

    /**
     * 原子完成成功审计和 refresh token 持久化，再签发 access token。
     */
    @Transactional
    public LoginTokenResult complete(
            UserAccount account,
            String clientIp,
            LocalDateTime loggedInAt
    ) {
        loginStateRecorder.recordSuccessfulLogin(account.id(), loggedInAt, clientIp);
        var refreshToken = refreshTokenService.issue(account.id());
        var accessToken = accessTokenIssuer.issueAccessToken(
                String.valueOf(account.id()),
                account.username(),
                List.of(account.type().name()),
                account.tokenVersion());

        return new LoginTokenResult(
                accessToken.accessToken(),
                refreshToken.token(),
                jwtProperties.accessTokenTtl().toSeconds(),
                jwtProperties.refreshTokenTtl().toSeconds());
    }
}
```

- [ ] **Step 8: 运行应用层测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=AuthApplicationServiceTest,LoginSuccessTransactionServiceUnitTest,LoginCredentialVerifierTest' test
```

Expected: 限流、坏凭据、锁定、成功重置、角色和 TTL 测试全部通过。

- [ ] **Step 9: 执行架构和范围扫描**

Run:

```powershell
mvn '-Dtest=ArchitectureRulesTest,ApplicationConfigurationTest' test
$matches = rg -n '@Transactional' 'src/main/java/com/tyb/myblog/v2/identity/application/auth/AuthApplicationService.java'
if ($LASTEXITCODE -eq 1) { Write-Output '外层登录编排未开启长事务' }
rg -n '@Transactional' 'src/main/java/com/tyb/myblog/v2/identity/application/auth/LoginSuccessTransactionService.java'
```

Expected:

- `AuthApplicationService` 没有 `@Transactional`。
- `LoginSuccessTransactionService.complete` 存在 `@Transactional`。
- 架构测试与 Spring 上下文通过。

- [ ] **Step 10: 提交应用编排批次**

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/AuthApplicationServiceTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/LoginSuccessTransactionServiceUnitTest.java'
git diff --cached --check
git diff --cached --stat
git commit -m "实现双Token登录事务编排"
```

---

### Task 3: 实现后台登录 HTTP 接口

**Files:**
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/LoginRequest.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/LoginTokenVO.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/AuthController.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/AuthControllerTest.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`

- [ ] **Step 1: 编写 Controller web slice 失败测试**

使用：

```java
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthApplicationService authApplicationService;

    @MockitoBean
    private ClientIpResolver clientIpResolver;
}
```

成功测试：

```java
when(clientIpResolver.resolve(any())).thenReturn("203.0.113.10");
when(authApplicationService.login(new LoginCommand(
        " Admin ", "secret", "203.0.113.10")))
        .thenReturn(new LoginTokenResult(
                "access-value", "refresh-value", 900, 604800));

mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":" Admin ","password":"secret"}
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.msg").value("success"))
        .andExpect(jsonPath("$.data.accessToken").value("access-value"))
        .andExpect(jsonPath("$.data.refreshToken").value("refresh-value"))
        .andExpect(jsonPath("$.data.accessExpiresIn").value(900))
        .andExpect(jsonPath("$.data.refreshExpiresIn").value(604800));
```

参数测试分别覆盖：

- `username=""`
- 65 字符用户名
- `password=""`
- 129 字符密码
- 非法 JSON

全部断言 HTTP 400 + `90001`，且应用服务未被调用。

- [ ] **Step 2: 运行 Controller 测试并确认 RED**

Run:

```powershell
mvn '-Dtest=AuthControllerTest' test
```

Expected: 编译失败，提示 `AuthController`、`LoginRequest` 或 `LoginTokenVO` 不存在。

- [ ] **Step 3: 实现 LoginRequest**

```java
package com.tyb.myblog.v2.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 后台登录请求。
 */
public record LoginRequest(
        @Schema(description = "后台登录用户名")
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过64个字符")
        String username,

        @Schema(description = "后台登录密码")
        @NotBlank(message = "密码不能为空")
        @Size(max = 128, message = "密码长度不能超过128个字符")
        String password
) {
}
```

- [ ] **Step 4: 实现 LoginTokenVO**

```java
package com.tyb.myblog.v2.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 后台登录成功响应。
 */
public record LoginTokenVO(
        @Schema(description = "JWT access token")
        String accessToken,
        @Schema(description = "仅本次返回的 refresh token 明文")
        String refreshToken,
        @Schema(description = "access token 有效秒数")
        long accessExpiresIn,
        @Schema(description = "refresh token 有效秒数")
        long refreshExpiresIn
) {
}
```

- [ ] **Step 5: 实现 AuthController**

```java
package com.tyb.myblog.v2.identity.web;

import com.tyb.myblog.v2.common.web.ApiResponse;
import com.tyb.myblog.v2.common.web.ClientIpResolver;
import com.tyb.myblog.v2.identity.application.auth.AuthApplicationService;
import com.tyb.myblog.v2.identity.application.auth.LoginCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台认证接口。
 */
@Tag(name = "后台认证", description = "后台登录与会话入口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final ClientIpResolver clientIpResolver;

    /**
     * 使用后台账号密码签发双 token。
     */
    @Operation(summary = "后台登录")
    @PostMapping("/login")
    public ApiResponse<LoginTokenVO> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        var result = authApplicationService.login(new LoginCommand(
                request.username(),
                request.password(),
                clientIpResolver.resolve(servletRequest)));
        return ApiResponse.ok(new LoginTokenVO(
                result.accessToken(),
                result.refreshToken(),
                result.accessExpiresIn(),
                result.refreshExpiresIn()));
    }
}
```

- [ ] **Step 6: 运行 Controller 测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=AuthControllerTest' test
```

Expected: 成功映射、参数校验和非法 JSON 全部通过。

- [ ] **Step 7: 编写登录白名单失败测试**

在 `SecurityConfigTest` 增加：

```java
@Test
void permitsConfiguredLoginPostWithoutAccessToken() throws Exception {
    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"missing","password":"wrong"}
                            """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("10001"));
}

@Test
void doesNotPermitUnconfiguredLoginGet() throws Exception {
    mockMvc.perform(get("/api/auth/login"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("10002"));
}
```

POST 返回 `10001` 证明请求进入 Controller，而不是被 Security 以 `10002` 拒绝。

- [ ] **Step 8: 运行安全测试并确认 RED**

Run:

```powershell
mvn '-Dtest=SecurityConfigTest' test
```

Expected: `POST /api/auth/login` 返回 `10002`，因为配置尚未公开。

- [ ] **Step 9: 精确配置登录公开端点**

在三个配置文件现有 `public-endpoints` 中增加：

```yaml
- method: POST
  path: /api/auth/login
```

不得添加 `/api/auth/**` 通配路径。

- [ ] **Step 10: 运行 web 与安全测试并确认 GREEN**

Run:

```powershell
mvn '-Dtest=AuthControllerTest,SecurityConfigTest,GlobalExceptionHandlerTest' test
```

Expected: 登录 POST 可匿名进入业务层，登录 GET 仍返回 `10002`。

- [ ] **Step 11: 执行 web 层架构扫描**

Run:

```powershell
mvn '-Dtest=ArchitectureRulesTest' test
$matches = rg -n 'infrastructure|Mapper|Repository' 'src/main/java/com/tyb/myblog/v2/identity/web'
if ($LASTEXITCODE -eq 1) { Write-Output 'identity web 未依赖持久化实现' }
```

Expected: web 层没有基础设施、Mapper 或 Repository 依赖。

- [ ] **Step 12: 提交 HTTP 接口批次**

```powershell
git add -- 'MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/AuthControllerTest.java' 'MyBlog-springboot-v2/src/main/resources/application.yml' 'MyBlog-springboot-v2/src/main/resources/application-local.yml' 'MyBlog-springboot-v2/src/test/resources/application-test.yml' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java'
git diff --cached --check
git diff --cached --stat
git commit -m "实现后台双Token登录接口"
```

---

### Task 4: 补齐登录事务与完整 HTTP 集成验收

**Files:**
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/LoginSuccessTransactionServiceIntegrationTest.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/AuthLoginIntegrationTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`

- [ ] **Step 1: 编写成功事务回滚集成测试**

`LoginSuccessTransactionServiceIntegrationTest`：

```java
@ActiveProfiles("test")
@SpringBootTest
@Import(LoginSuccessTransactionServiceIntegrationTest.TestIssuerConfiguration.class)
class LoginSuccessTransactionServiceIntegrationTest {

    @Autowired
    private LoginSuccessTransactionService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @TestConfiguration
    static class TestIssuerConfiguration {

        @Bean
        @Primary
        AccessTokenIssuer testAccessTokenIssuer() {
            return mock(AccessTokenIssuer.class);
        }
    }
}
```

使用 `@Primary` 测试 Bean，而不是按接口替换现有 `JwtTokenService`。后者同时实现 `AccessTokenDecoder`，直接替换会破坏认证过滤器和持久化校验器的上下文装配。

在 `@BeforeEach` 中执行 `reset(accessTokenIssuer)`，再清理两张表并插入测试账号，避免单例 mock 的 stub 在测试间残留。

每次测试前清空 `t_refresh_token` 和 `t_user_auth`，插入：

```sql
insert into t_user_auth (
    id, username, password_hash, type, token_version,
    login_fail_count, locked_until, deleted
) values (1001, 'admin', '$2a$10$test-password-hash', 1, 3, 4, ?, 0)
```

`locked_until` 传入 `loggedInAt.minusMinutes(1)`，表示历史锁定已经到期。这样成功审计能够执行，回滚测试才会真正走到 access token 签发失败处。

成功测试让 issuer 返回：

```java
new TokenPair("access-value", Instant.parse("2026-06-12T03:15:00Z"))
```

断言：

- `last_login_at` 等于传入时间。
- `last_login_ip` 等于 `203.0.113.10`。
- `login_fail_count=0`。
- `locked_until is null`。
- `t_refresh_token` 有且仅有一条记录。
- 数据库 `token_hash` 不等于返回的 refresh token 明文。

回滚测试：

```java
when(accessTokenIssuer.issueAccessToken(anyString(), anyString(), anyList(), anyInt()))
        .thenThrow(new IllegalStateException("token signing failed"));

assertThatThrownBy(() -> service.complete(account, ip, loggedInAt))
        .isInstanceOf(IllegalStateException.class);
```

随后断言：

- `last_login_at` 仍为 `null`。
- `login_fail_count` 仍为 4。
- `locked_until` 仍为原值。
- `t_refresh_token` 行数为 0。

- [ ] **Step 2: 运行事务集成测试**

Run:

```powershell
mvn '-Dtest=LoginSuccessTransactionServiceIntegrationTest' test
```

Expected: 成功提交与签发失败回滚测试通过。若回滚测试失败，先检查 `LoginSuccessTransactionService` 是否为独立 Spring Bean 且公开方法带 `@Transactional`。

- [ ] **Step 3: 编写完整 HTTP 登录集成测试**

`AuthLoginIntegrationTest` 使用：

```java
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccessTokenDecoder accessTokenDecoder;
}
```

每次测试前：

- 清空 `t_refresh_token` 和 `t_user_auth`。
- 使用 `passwordEncoder.encode("correct-password")` 插入 ADMIN 或 DEMO。
- 使用不同用户名或测试 IP 隔离 Caffeine key，避免测试之间共享失败计数。

成功测试断言：

```java
MvcResult mvcResult = mockMvc.perform(post("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr("203.0.113.20");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":" Admin ","password":"correct-password"}
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"))
        .andExpect(jsonPath("$.data.accessExpiresIn").value(900))
        .andExpect(jsonPath("$.data.refreshExpiresIn").value(604800))
        .andReturn();
```

从响应读取 access token，通过 `AccessTokenDecoder.decode(...)` 断言：

- `userId="1001"`
- `username="admin"`
- `roles=["ADMIN"]`
- `tokenVersion=3`

查询数据库断言：

- refresh token 只有一条。
- `last_login_ip="203.0.113.20"`。
- 失败计数和锁定已清理。

增加 DEMO 成功测试，断言 roles 只包含 `DEMO`。

- [ ] **Step 4: 增加坏凭据、锁定与限流边界测试**

完整 HTTP 测试覆盖：

1. 未知账号返回 401 + `10001`。
2. GUEST 即使密码正确也返回 401 + `10001`。
3. 持久化锁定账号返回 401 + `10001`。
4. 同一 IP + username 连续 5 次错误：
   - 第 1 至第 5 次均返回 401 + `10001`。
5. 第 6 次返回 429 + `90002`。
6. 第 6 次后数据库失败状态不再变化，证明限流在账号查询和 BCrypt 前生效。
7. 使用正确密码成功后，再次错误从新的 Caffeine 周期开始。

避免测试互相污染：

```java
String username = "limit-" + UUID.randomUUID();
String clientIp = "198.51.100." + uniqueHost;
```

测试不在日志或断言消息中输出密码和 token。

- [ ] **Step 5: 运行完整 HTTP 集成测试**

Run:

```powershell
mvn '-Dtest=AuthLoginIntegrationTest' test
```

Expected: ADMIN、DEMO、坏凭据、GUEST、锁定、第 5/6 次边界和成功重置全部通过。

- [ ] **Step 6: 强化架构守护**

在 `ArchitectureRulesTest` 增加 application 不依赖 Servlet 的规则：

```java
@ArchTest
static final ArchRule application_does_not_depend_on_servlet_api =
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.servlet..")
                .allowEmptyShould(true);
```

该规则确保 `HttpServletRequest` 只停留在 web 层，应用层只接收 `LoginCommand.clientIp`。

- [ ] **Step 7: 执行完整定向回归和规则扫描**

Run:

```powershell
mvn '-Dtest=ApiResponseTest,GlobalExceptionHandlerTest,AuthApplicationServiceTest,LoginSuccessTransactionServiceUnitTest,LoginSuccessTransactionServiceIntegrationTest,AuthControllerTest,AuthLoginIntegrationTest,SecurityConfigTest,ArchitectureRulesTest' test
$matches = rg -n '@(Select|Insert|Update|Delete)\b' 'src/main/java'
if ($LASTEXITCODE -eq 1) { Write-Output '未发现 MyBatis SQL 注解' }
$matches = rg -n 'password|accessToken|refreshToken' 'src/main/java/com/tyb/myblog/v2/identity' | Select-String -Pattern 'log\\.|Logger'
if (-not $matches) { Write-Output 'identity 未记录密码或 token' }
git diff --check
```

Expected:

- 全部定向测试通过。
- 无 SQL 注解。
- 无密码或 token 日志。
- 无空白错误。

- [ ] **Step 8: 执行全量测试**

Run:

```powershell
mvn clean test
```

Expected:

- 0 failures。
- 0 errors。
- 只允许现有 `MySqlFlywayMigrationTest` 和 `MySqlLoginFailureConcurrencyTest` 因 Docker 不可用跳过。
- 记录真实 tests / failures / errors / skipped 数字供 Task 5 使用。

- [ ] **Step 9: 提交集成验收批次**

```powershell
git add -- 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/LoginSuccessTransactionServiceIntegrationTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/AuthLoginIntegrationTest.java' 'MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java'
git diff --cached --check
git diff --cached --stat
git commit -m "补齐后台登录事务与接口验收"
```

---

### Task 5: 同步后台登录实施结果

**Files:**
- Modify: `docs/project-handbook/status.md`
- Modify: `docs/project-handbook/roadmap.md`
- Modify: `docs/project-handbook/arch/auth-flow.md`
- Modify: `docs/project-handbook/rules/api-response.md`
- Modify: `docs/project-handbook/arch/request-flow.md`
- Create: `docs/project-handbook/api-contract/auth.md`
- Modify: `docs/project-handbook/api-contract/README.md`
- Modify: `docs/project-handbook/specs/2026-06-12-identity-login-orchestration-design.md`
- Modify: `docs/project-handbook/plans/2026-06-12-identity-login-orchestration-plan.md`

- [ ] **Step 1: 获取真实实施证据**

Run:

```powershell
git log -4 --format='%h %s'
mvn clean test
```

记录四个代码提交短 SHA 和 Maven 最终 tests / failures / errors / skipped。不得使用计划中的示例值。

- [ ] **Step 2: 更新 status 与 roadmap**

`status.md` 明确：

- identity 后台登录最小纵向切片已经提供真实 HTTP 接口。
- 已接入双 token、限流、数据库锁定、成功审计和统一响应契约。
- refresh/logout/当前用户接口尚未完成。
- 下一步是补齐 refresh 与 logout Controller，或按 roadmap 进入 identity 剩余用户资料能力；以届时状态评审为准。
- 测试基线使用 Step 1 的真实数字。

`roadmap.md`：

- 将 identity 条目改为部分完成说明，不能整项勾选成“identity 全部完成”。
- 明确已完成登录接口，剩余 refresh/logout 与用户资料。

- [ ] **Step 3: 修正认证与响应权威文档**

`arch/auth-flow.md`：

- 锁定账号对外统一为 `401 + 10001`。
- 登录顺序改为：限流 → 凭据校验 → 成功短事务。
- 成功短事务顺序固定为审计 → refresh token → access token。
- 删除“refresh token 是否轮换待定”，写明现有 `RefreshTokenService.rotate` 采用轮换。

`rules/api-response.md` 和 `arch/request-flow.md`：

- 所有响应示例统一为 `code/msg/data`。
- 删除 `success`、`message` 和成功码 `OK`。

- [ ] **Step 4: 创建 auth API 契约**

`api-contract/auth.md` 至少包含：

- `POST /api/auth/login`
- 请求字段、长度限制和示例。
- 成功响应四个 token 字段。
- `90001`、`10001`、`90002`、`99999`。
- 第 5 次和第 6 次限流边界。
- 锁定账号不暴露状态。
- refresh/logout 标记为尚未开放接口，不伪造契约。

把 `api-contract/README.md` 中 auth 状态从“待编写”改为“登录已落地，刷新/登出尚未实现”。

- [ ] **Step 5: 记录设计与计划实施结果**

在设计文档末尾增加“实施结果”，写入：

- 四个真实代码提交 SHA。
- 实际测试数字。
- Docker 跳过事实。
- 尚未实现 refresh/logout Controller。

勾选本计划实际执行步骤。最终验收只勾选有测试证据的事项。

- [ ] **Step 6: 文档一致性自检**

Run:

```powershell
rg -n 'success.*code.*message|"\s*success\s*"|"\s*message\s*"|code.*OK|锁定.*403|是否轮换.*待定' 'docs/project-handbook'
rg -n 'POST /api/auth/login|10001|90002|00000|refresh/logout' 'docs/project-handbook/api-contract/auth.md' 'docs/project-handbook/arch/auth-flow.md' 'docs/project-handbook/status.md'
$forbidden = @('T' + 'BD', 'T' + 'ODO', '待' + '填', '占位' + '符')
Select-String -Path 'docs/project-handbook/specs/2026-06-12-identity-login-orchestration-design.md','docs/project-handbook/plans/2026-06-12-identity-login-orchestration-plan.md','docs/project-handbook/api-contract/auth.md' -Pattern $forbidden
git diff --check
```

Expected:

- 权威文档不再描述旧响应字段和锁定 403。
- 登录契约与实际实现一致。
- 不命中未决标记，且没有空白错误。

- [ ] **Step 7: 提交实施结果文档**

```powershell
git add -- 'docs/project-handbook/status.md' 'docs/project-handbook/roadmap.md' 'docs/project-handbook/arch/auth-flow.md' 'docs/project-handbook/rules/api-response.md' 'docs/project-handbook/arch/request-flow.md' 'docs/project-handbook/api-contract/auth.md' 'docs/project-handbook/api-contract/README.md' 'docs/project-handbook/specs/2026-06-12-identity-login-orchestration-design.md' 'docs/project-handbook/plans/2026-06-12-identity-login-orchestration-plan.md'
git diff --cached --check
git diff --cached --stat
git commit -m "同步后台登录实施结果"
```

---

## 最终验收

- [ ] `ApiResponse` 只序列化 `code/msg/data`。
- [ ] 成功码固定为字符串 `00000`。
- [ ] `POST /api/auth/login` 可匿名访问，其他未配置方法不被公开。
- [ ] 用户名 trim 后使用 `Locale.ROOT` 小写，密码保持原样。
- [ ] 限流发生在账号查询和 BCrypt 之前。
- [ ] 未知账号、GUEST、错误密码和锁定账号均返回 `401 + 10001`。
- [ ] 第 5 次错误返回 `10001`，第 6 次返回 `429 + 90002`。
- [ ] 登录成功清除 Caffeine 当前 key。
- [ ] 登录成功更新审计并清理数据库失败状态。
- [ ] refresh token 明文只返回一次，数据库只保存 SHA-256。
- [ ] access token 包含正确的 user id、username、role 和 token version。
- [ ] ADMIN 与 DEMO 分别获得正确角色。
- [ ] access token 签发失败时成功审计和 refresh token 写入一起回滚。
- [ ] 外层 `AuthApplicationService` 不开启长事务。
- [ ] `LoginSuccessTransactionService` 是独立 Spring Bean 并使用短事务。
- [ ] web 不依赖 Mapper、Repository 实现或 identity infrastructure。
- [ ] application 不依赖 Servlet API。
- [ ] domain 不依赖 HTTP、Spring、Caffeine 或 `ApiErrorCode`。
- [ ] 不新增 SQL 注解、Entity、Flyway、Redis、验证码、refresh/logout Controller。
- [ ] 不记录密码、密码摘要、access token 或 refresh token。
- [ ] 定向测试、ArchUnit、全量 `mvn clean test` 和 `git diff --check` 通过。
- [ ] 实施结果按五个小批次分别提交，工作区干净。
