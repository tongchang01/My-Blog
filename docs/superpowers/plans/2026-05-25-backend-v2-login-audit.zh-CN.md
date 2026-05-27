# 后端 V2 登录审计字段更新实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐项执行本计划。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 登录成功后回写旧表 `t_user_auth.last_login_time` 和 `t_user_auth.ip_address`，补齐后端 V2 真实账号登录的最小审计闭环。

**架构：** 在 identity 领域层新增 `LoginAuditRecorder` 端口，应用层 `AuthService` 只依赖端口；基础设施层用 `JdbcTemplate` 更新旧表。Controller 只负责从 HTTP 请求中提取客户端 IP，并通过 `LoginCommand` 传给应用层。

**技术栈：** Java 17、Spring Boot 3.5、Spring MVC、Spring JDBC、JUnit 5、AssertJ、MockMvc、H2 MySQL mode、Flyway test migration。

---

## 范围边界

本计划只做：

- 登录成功后更新 `last_login_time`。
- 登录成功后更新 `ip_address`。
- 密码错误、账号不存在、禁用账号不更新审计字段。
- 从 `X-Forwarded-For`、`X-Real-IP`、`request.getRemoteAddr()` 提取客户端 IP。

本计划不做：

- 不更新 `ip_source`。
- 不引入 IP 归属地解析。
- 不改登录响应结构。
- 不改 JWT 签发、解析、撤销逻辑。
- 不做 Redis 在线用户、踢下线、设备管理。
- 不调整旧数据库表结构。
- 不修改前台或后台管理端。

## 文件结构

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/LoginAuditRecorder.java`
  - identity 领域端口，只表达“记录登录成功审计”的能力。
- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/LoginCommand.java`
  - 增加 `clientIp` 字段，并保留双参数构造器。
- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/AuthService.java`
  - 密码校验成功后调用 `LoginAuditRecorder`。
- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthServiceTest.java`
  - 用 fake recorder 验证成功调用、失败不调用。
- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseLoginAuditRecorder.java`
  - JDBC 实现，更新旧表 `t_user_auth`。
- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseLoginAuditRecorderTest.java`
  - 验证数据库字段回写。
- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/ClientIpResolver.java`
  - HTTP 客户端 IP 提取规则。
- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/ClientIpResolverTest.java`
  - 单测 IP 提取优先级和空白处理。
- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java`
  - 登录时传入客户端 IP。
- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java`
  - 登录成功和失败的审计字段回归。

## 任务 1：建立登录审计领域端口并接入 AuthService

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/LoginAuditRecorder.java`

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/LoginCommand.java`

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/AuthService.java`

- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthServiceTest.java`

- [x] **步骤 1：先改 AuthServiceTest，写出成功登录调用审计的期望**

修改 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthServiceTest.java`，把测试类调整成下面结构：

```java
package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.identity.application.AuthService;
import com.aurora.myblog.v2.modules.identity.application.AuthTokenService;
import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.LoginAuditRecorder;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import com.aurora.myblog.v2.modules.identity.infrastructure.ConfiguredUserCredentialReader;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void logsInAndRecordsSuccessfulAudit() {
        RecordingLoginAuditRecorder auditRecorder = new RecordingLoginAuditRecorder();
        AuthService authService = new AuthService(
                ConfiguredUserCredentialReader.singleUser(
                        "admin@example.com",
                        passwordEncoder.encode("password123"),
                        List.of(AuthRole.ADMIN)),
                passwordEncoder,
                fixedTokenService(),
                auditRecorder);

        var result = authService.login(new LoginCommand("admin@example.com", "password123", "127.0.0.1"));

        assertThat(result.user().username()).isEqualTo("admin@example.com");
        assertThat(result.token().accessToken()).isEqualTo("access-token");
        assertThat(auditRecorder.authId).isEqualTo("test-user");
        assertThat(auditRecorder.clientIp).isEqualTo("127.0.0.1");
        assertThat(auditRecorder.callCount).isEqualTo(1);
    }

    @Test
    void loginFailureDoesNotRecordAudit() {
        RecordingLoginAuditRecorder auditRecorder = new RecordingLoginAuditRecorder();
        AuthService authService = new AuthService(
                ConfiguredUserCredentialReader.singleUser(
                        "admin@example.com",
                        passwordEncoder.encode("password123"),
                        List.of(AuthRole.ADMIN)),
                passwordEncoder,
                fixedTokenService(),
                auditRecorder);

        assertThatThrownBy(() -> authService.login(new LoginCommand("admin@example.com", "wrong", "127.0.0.1")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("用户名或密码错误");
        assertThat(auditRecorder.callCount).isZero();
    }

    private AuthTokenService fixedTokenService() {
        return new AuthTokenService() {
            @Override
            public TokenIssueResult issueAccessToken(com.aurora.myblog.v2.modules.identity.domain.AuthenticatedUser user) {
                return new TokenIssueResult("access-token", Instant.parse("2030-01-01T00:00:00Z"));
            }

            @Override
            public void revoke(String accessToken) {
            }
        };
    }

    private static class RecordingLoginAuditRecorder implements LoginAuditRecorder {
        private String authId;
        private String clientIp;
        private int callCount;

        @Override
        public void recordSuccessfulLogin(String authId, String clientIp) {
            this.authId = authId;
            this.clientIp = clientIp;
            this.callCount++;
        }
    }
}
```

- [x] **步骤 2：运行 AuthServiceTest，确认先失败**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=AuthServiceTest'
```

预期：编译失败，错误应指向 `LoginAuditRecorder` 不存在、`LoginCommand` 缺少三参数构造或 `AuthService` 构造器参数不匹配。

- [x] **步骤 3：新增 LoginAuditRecorder 领域端口**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/LoginAuditRecorder.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

public interface LoginAuditRecorder {
    void recordSuccessfulLogin(String authId, String clientIp);
}
```

- [x] **步骤 4：扩展 LoginCommand，保留旧调用兼容**

修改 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/LoginCommand.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

public record LoginCommand(String username, String password, String clientIp) {
    public LoginCommand(String username, String password) {
        this(username, password, null);
    }
}
```

- [x] **步骤 5：AuthService 注入并调用登录审计端口**

修改 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/AuthService.java`：

```java
package com.aurora.myblog.v2.modules.identity.application;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.identity.domain.AuthenticatedUser;
import com.aurora.myblog.v2.modules.identity.domain.LoginAuditRecorder;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import com.aurora.myblog.v2.modules.identity.domain.UserCredentialReader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserCredentialReader credentialReader;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;
    private final LoginAuditRecorder auditRecorder;

    public AuthService(
            UserCredentialReader credentialReader,
            PasswordEncoder passwordEncoder,
            AuthTokenService tokenService,
            @Nullable
            LoginAuditRecorder auditRecorder) {
        this.credentialReader = credentialReader;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditRecorder = auditRecorder;
    }

    public LoginResult login(LoginCommand command) {
        var credential = credentialReader.findByUsername(command.username())
                .filter(user -> passwordEncoder.matches(command.password(), user.passwordHash()))
                .orElseThrow(() -> new ApiException(ApiErrorCode.BAD_CREDENTIALS, "用户名或密码错误"));
        AuthenticatedUser user = new AuthenticatedUser(
                credential.id(),
                credential.username(),
                Set.copyOf(credential.roles()));
        if (auditRecorder != null) {
            auditRecorder.recordSuccessfulLogin(credential.id(), command.clientIp());
        }
        AuthTokenService.TokenIssueResult token = tokenService.issueAccessToken(user);
        return new LoginResult(user, token);
    }

    public void logout(String accessToken) {
        tokenService.revoke(accessToken);
    }

    public record LoginResult(AuthenticatedUser user, AuthTokenService.TokenIssueResult token) {
    }
}
```

实施说明：任务 1 结束时 `DatabaseLoginAuditRecorder` 还未创建。为了避免任务 1 的中间提交破坏 Spring 上下文，`AuthService` 对 `LoginAuditRecorder` 使用 `@Nullable` 临时兼容；任务 2 新增数据库实现 Bean 后，登录审计会进入真实回写路径。

- [x] **步骤 6：运行 AuthServiceTest，确认通过**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=AuthServiceTest'
```

预期：通过，2 个测试，0 失败。

- [x] **步骤 7：提交领域端口与应用层接入**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/LoginAuditRecorder.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/LoginCommand.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/AuthService.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthServiceTest.java
git commit -m "接入后端V2登录审计端口"
```

## 任务 2：实现数据库登录审计适配器

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseLoginAuditRecorder.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseLoginAuditRecorderTest.java`

- [x] **步骤 1：先写数据库审计记录器测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseLoginAuditRecorderTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.infrastructure.DatabaseLoginAuditRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseLoginAuditRecorder.class)
class DatabaseLoginAuditRecorderTest {

    @Autowired
    private DatabaseLoginAuditRecorder recorder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void updatesLastLoginTimeAndIpAddress() {
        recorder.recordSuccessfulLogin("1", "203.0.113.10");

        AuditRow row = loadAuditRow("1");
        assertThat(row.lastLoginTime()).isNotNull();
        assertThat(row.ipAddress()).isEqualTo("203.0.113.10");
        assertThat(row.ipSource()).isNull();
    }

    @Test
    void allowsNullIpAddressWithoutChangingIpSource() {
        jdbcTemplate.update("update t_user_auth set ip_source = ? where id = ?", "保留归属地", 1);

        recorder.recordSuccessfulLogin("1", null);

        AuditRow row = loadAuditRow("1");
        assertThat(row.lastLoginTime()).isNotNull();
        assertThat(row.ipAddress()).isNull();
        assertThat(row.ipSource()).isEqualTo("保留归属地");
    }

    private AuditRow loadAuditRow(String authId) {
        return jdbcTemplate.queryForObject("""
                        select last_login_time, ip_address, ip_source
                        from t_user_auth
                        where id = ?
                        """,
                (rs, rowNum) -> new AuditRow(
                        rs.getTimestamp("last_login_time"),
                        rs.getString("ip_address"),
                        rs.getString("ip_source")),
                authId);
    }

    private record AuditRow(java.sql.Timestamp lastLoginTime, String ipAddress, String ipSource) {
    }
}
```

- [x] **步骤 2：运行测试，确认先失败**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseLoginAuditRecorderTest'
```

预期：编译失败，错误应指向 `DatabaseLoginAuditRecorder` 不存在。

- [x] **步骤 3：实现 DatabaseLoginAuditRecorder**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseLoginAuditRecorder.java`：

```java
package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.LoginAuditRecorder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseLoginAuditRecorder implements LoginAuditRecorder {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseLoginAuditRecorder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void recordSuccessfulLogin(String authId, String clientIp) {
        jdbcTemplate.update("""
                        update t_user_auth
                        set last_login_time = current_timestamp,
                            ip_address = ?
                        where id = ?
                        """,
                clientIp,
                authId);
    }
}
```

- [x] **步骤 4：运行数据库审计测试，确认通过**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseLoginAuditRecorderTest'
```

预期：通过，2 个测试，0 失败。

- [x] **步骤 5：提交数据库审计适配器**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/DatabaseLoginAuditRecorder.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/DatabaseLoginAuditRecorderTest.java
git commit -m "新增后端V2数据库登录审计"
```

## 任务 3：Controller 提取客户端 IP 并补齐登录回归

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/ClientIpResolver.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/ClientIpResolverTest.java`

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java`

- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java`

- [x] **步骤 1：写 ClientIpResolver 单元测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/ClientIpResolverTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.api.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void usesFirstForwardedForIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 198.51.100.20");
        request.addHeader("X-Real-IP", "198.51.100.30");
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.30");
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.30");
    }

    @Test
    void fallsBackToRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(ClientIpResolver.resolve(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void returnsNullWhenAllValuesAreBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", " , ");
        request.addHeader("X-Real-IP", " ");
        request.setRemoteAddr(" ");

        assertThat(ClientIpResolver.resolve(request)).isNull();
    }
}
```

- [x] **步骤 2：运行测试，确认先失败**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=ClientIpResolverTest'
```

预期：编译失败，错误应指向 `ClientIpResolver` 不存在。

- [x] **步骤 3：实现 ClientIpResolver**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/ClientIpResolver.java`：

```java
package com.aurora.myblog.v2.modules.identity.api;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }
        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return normalize(request.getRemoteAddr());
    }

    private static String firstForwardedIp(String value) {
        if (value == null) {
            return null;
        }
        for (String part : value.split(",")) {
            String ip = normalize(part);
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
```

- [x] **步骤 4：运行 ClientIpResolverTest，确认通过**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=ClientIpResolverTest'
```

预期：通过，4 个测试，0 失败。

- [x] **步骤 5：修改 AuthController 传入客户端 IP**

修改 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java`：

```java
package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.aurora.myblog.v2.common.auth.CurrentUser;
import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.modules.identity.application.AuthService;
import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        var result = authService.login(new LoginCommand(
                request.username(),
                request.password(),
                ClientIpResolver.resolve(servletRequest)));
        LoginResponse.User user = new LoginResponse.User(
                result.user().id(),
                result.user().username(),
                result.user().roles());
        return ApiResponse.ok(new LoginResponse(result.token().accessToken(), result.token().expiresAt(), user));
    }

    @GetMapping("/me")
    ApiResponse<MeResponse> me(@CurrentUser AuthenticatedPrincipal user) {
        return ApiResponse.ok(new MeResponse(
                user.id(),
                user.username(),
                user.roles().stream().map(AuthRole::valueOf).collect(java.util.stream.Collectors.toSet())));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization.replaceFirst("Bearer ", ""));
        return ApiResponse.ok(null);
    }
}
```

- [x] **步骤 6：修改 AuthControllerTest，验证登录审计回写和失败不回写**

修改 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetLoginAuditFields() {
        jdbcTemplate.update("update t_user_auth set last_login_time = null, ip_address = null, ip_source = null");
    }

    @Test
    void logsInWithDatabaseCredentialWithoutExistingAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.10, 198.51.100.20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin@163.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin@163.com"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("ADMIN"));

        AuditRow row = loadAuditRow("1");
        assertThat(row.lastLoginTime()).isNotNull();
        assertThat(row.ipAddress()).isEqualTo("203.0.113.10");
        assertThat(row.ipSource()).isNull();
    }

    @Test
    void rejectsInvalidCredentialWithUnauthorizedEnvelope() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        AuditRow row = loadAuditRow("1");
        assertThat(row.lastLoginTime()).isNull();
        assertThat(row.ipAddress()).isNull();
    }

    @Test
    void loginFailureDoesNotRevealAccountExistence() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"missing@example.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void rejectsDisabledUserWithSameBadCredentialResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"disabled@163.com","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void meRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    private AuditRow loadAuditRow(String authId) {
        return jdbcTemplate.queryForObject("""
                        select last_login_time, ip_address, ip_source
                        from t_user_auth
                        where id = ?
                        """,
                (rs, rowNum) -> new AuditRow(
                        rs.getTimestamp("last_login_time"),
                        rs.getString("ip_address"),
                        rs.getString("ip_source")),
                authId);
    }

    private record AuditRow(java.sql.Timestamp lastLoginTime, String ipAddress, String ipSource) {
    }
}
```

- [x] **步骤 7：运行 Controller 与 IP 回归测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=ClientIpResolverTest,AuthControllerTest'
```

预期：通过，9 个测试，0 失败。

- [x] **步骤 8：提交 Controller 登录审计接入**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/ClientIpResolver.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/ClientIpResolverTest.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java
git commit -m "接入后端V2登录IP审计"
```

## 任务 4：整体验证与本地 MySQL 冒烟确认

**文件：**

- 验证：`MyBlog-springboot-v2/**`

- 可选修改：`docs/superpowers/plans/2026-05-25-backend-v2-login-audit.zh-CN.md`

- [ ] **步骤 1：运行登录审计相关测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=AuthServiceTest,DatabaseLoginAuditRecorderTest,ClientIpResolverTest,AuthControllerTest'
```

预期：全部通过。

- [ ] **步骤 2：运行全量测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
```

预期：全部通过。

- [ ] **步骤 3：运行打包验证**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

预期：通过，并生成：

```text
MyBlog-springboot-v2/target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar
```

- [ ] **步骤 4：本地 MySQL 冒烟前记录当前审计字段**

运行：

```powershell
$env:MYSQL_PWD='2423137093'
mysql -h 127.0.0.1 -P 3306 -u root --default-character-set=utf8mb4 -N aurora -e "select id, username, last_login_time, ip_address, ip_source from t_user_auth where username = 'tongyibin1@gmail.com';"
```

预期：能看到真实账号当前审计字段。不要在文档或提交中记录真实密码。

- [ ] **步骤 5：启动本地 V2 服务**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:MYBLOG_JWT_SECRET='local-dev-secret-local-dev-secret-123456'
mvn -f MyBlog-springboot-v2/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
```

预期：应用启动成功，监听 `8080`，local profile 下 Flyway 不会迁移本地 `aurora` 库。

- [ ] **步骤 6：调用真实账号登录接口**

另开一个终端运行。命令会在终端提示输入本地真实密码，密码只保存在当前 PowerShell 变量里，不写入任何文件：

```powershell
$password = Read-Host '请输入本地真实密码'
$body = @{ username = 'tongyibin1@gmail.com'; password = $password } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -Headers @{ 'X-Forwarded-For' = '203.0.113.10' } -ContentType 'application/json' -Body $body
```

预期：

```text
success : True
code    : OK
data    : 包含 accessToken，user.username 为 tongyibin1@gmail.com
```

- [ ] **步骤 7：只读确认本地 MySQL 审计字段已更新**

运行：

```powershell
$env:MYSQL_PWD='2423137093'
mysql -h 127.0.0.1 -P 3306 -u root --default-character-set=utf8mb4 -N aurora -e "select id, username, last_login_time, ip_address, ip_source from t_user_auth where username = 'tongyibin1@gmail.com';"
```

预期：

- `last_login_time` 不为空，并晚于步骤 4 读取到的值。

- `ip_address` 为 `203.0.113.10`。

- `ip_source` 保持原值，不因本任务被主动覆盖。

- [ ] **步骤 8：更新本计划完成状态**

如果实施者按本计划逐项完成，可以把本文件对应任务步骤勾选为 `[x]`。只勾选实际完成的步骤。

- [ ] **步骤 9：提交阶段验证状态**

如果只更新计划勾选状态或实施记录：

```powershell
git add docs/superpowers/plans/2026-05-25-backend-v2-login-audit.zh-CN.md
git commit -m "同步后端V2登录审计计划状态"
```

如果没有修改计划文档，则不用提交。

## 自检记录

- 覆盖范围：领域端口、应用层调用、数据库适配器、HTTP IP 提取、成功登录回写、失败登录不回写、本地 MySQL 冒烟验证。
- 明确排除：`ip_source` 归属地解析、Redis 在线用户、动态资源权限、菜单接口、前端联调。
- 类型一致性：`LoginCommand.clientIp` 只从 Controller 传入，`AuthService` 只依赖 `LoginAuditRecorder` 端口，数据库更新只在 infrastructure 层完成。
- 风险控制：真实密码只用于本地手动验证，不写入计划、不进入 Git；测试环境继续使用 H2 和 test migration。
