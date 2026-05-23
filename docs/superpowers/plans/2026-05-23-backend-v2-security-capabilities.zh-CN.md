# Java 后端 V2 安全与认证能力实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐项执行本计划。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 在已完成的 V2 foundation 上建立第一版可运行、可测试、可被前台和后台共同依赖的认证与授权基线。

**架构：** 本阶段选择无状态 Bearer JWT，不采用 Cookie Session。登录、令牌签发、当前用户解析和角色授权先按模块化单体边界落到 `common.security` 与 `modules.identity`，真实用户表、角色表和资源权限表迁移留给后续 identity 业务域计划。本计划只建立认证契约和安全默认值，不迁移旧后端的 QQ 登录、动态资源权限、在线用户列表或 Redis 登录态。

**技术栈：** Java 17、Spring Boot 3.5.14、Spring Security 6.5.x、Spring Validation、Spring Security OAuth2 JOSE、BCrypt、JUnit 5、MockMvc、ArchUnit。

---

## 本阶段关键决策

- 认证方式使用 `Authorization: Bearer <token>`。
- 访问令牌默认有效期短，不在 JWT 中放密码、邮箱、昵称等可变资料。
- JWT 只承载 `subject`、`roles`、`issuer`、`issuedAt`、`expiresAt` 和 `jti`。
- 登出采用令牌撤销表接口，当前先用内存实现，后续 Redis 专项替换实现。
- API 使用 Bearer Token，不依赖浏览器 Cookie，因此本阶段会显式关闭 CSRF。
- 角色先只支持 `USER` 和 `ADMIN`，不直接复刻旧后端动态资源权限。
- 真实用户持久化不在本计划内；先用可配置本地账号适配器完成认证闭环。

## 文件结构

```text
MyBlog-springboot-v2
├─ pom.xml
├─ src/main/java/com/aurora/myblog/v2
│  ├─ common
│  │  ├─ config
│  │  │  └─ SecurityJwtProperties.java
│  │  ├─ error
│  │  │  └─ ApiErrorCode.java
│  │  └─ security
│  │     ├─ SecurityConfig.java
│  │     ├─ SecurityProblemSupport.java
│  │     ├─ auth
│  │     │  ├─ CurrentUser.java
│  │     │  ├─ CurrentUserArgumentResolver.java
│  │     │  ├─ JwtAuthenticationFilter.java
│  │     │  ├─ JwtTokenService.java
│  │     │  ├─ TokenClaims.java
│  │     │  ├─ TokenPair.java
│  │     │  └─ TokenRevocationStore.java
│  │     └─ support
│  │        └─ InMemoryTokenRevocationStore.java
│  └─ modules
│     └─ identity
│        ├─ api
│        │  ├─ AuthController.java
│        │  ├─ LoginRequest.java
│        │  ├─ LoginResponse.java
│        │  └─ MeResponse.java
│        ├─ application
│        │  └─ AuthService.java
│        ├─ domain
│        │  ├─ AuthenticatedUser.java
│        │  ├─ AuthRole.java
│        │  ├─ LoginCommand.java
│        │  └─ UserCredentialReader.java
│        └─ infrastructure
│           ├─ ConfiguredIdentityProperties.java
│           └─ ConfiguredUserCredentialReader.java
├─ src/main/resources/application.yml
└─ src/test/java/com/aurora/myblog/v2
   ├─ common/security
   │  ├─ JwtAuthenticationFilterTest.java
   │  ├─ JwtTokenServiceTest.java
   │  └─ JwtPropertiesTest.java
   └─ modules/identity
      ├─ AuthControllerTest.java
      └─ AuthServiceTest.java
```

## 验收标准

- 登录成功返回统一 `ApiResponse<LoginResponse>`。
- 登录失败返回统一错误响应，不能泄露账号是否存在。
- 未登录访问受保护接口返回 401，统一错误码。
- 角色不足访问后台接口返回 403，统一错误码。
- 携带有效 Bearer Token 可以访问 `/api/auth/me`。
- 登出后同一个 token 再访问受保护接口返回 401。
- 所有安全能力都有 MockMvc 或单元测试覆盖。

### 任务 1：增加 JWT 配置与依赖

**文件：**

- 修改：`MyBlog-springboot-v2/pom.xml`

- 修改：`MyBlog-springboot-v2/src/main/resources/application.yml`

- 修改：`MyBlog-springboot-v2/src/test/resources/application-test.yml`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/SecurityJwtProperties.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/JwtPropertiesTest.java`

- [x] **步骤 1：先写会失败的 JWT 配置绑定测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/JwtPropertiesTest.java`：

```java
package com.aurora.myblog.v2.common.security;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class JwtPropertiesTest {

    @Autowired
    private SecurityJwtProperties jwtProperties;

    @Test
    void bindsJwtSettings() {
        assertThat(jwtProperties.issuer()).isEqualTo("myblog-v2-test");
        assertThat(jwtProperties.accessTokenTtl()).isEqualTo(Duration.ofSeconds(900));
        assertThat(jwtProperties.secret()).hasSizeGreaterThanOrEqualTo(32);
    }
}
```

- [x] **步骤 2：运行测试，确认它先失败**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=JwtPropertiesTest
```

预期：失败，因为 `SecurityJwtProperties` 还不存在。

- [x] **步骤 3：增加 Spring Security JOSE 依赖**

修改 `MyBlog-springboot-v2/pom.xml`，在 `spring-boot-starter-security` 后加入：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
```

- [x] **步骤 4：创建 JWT 配置 record**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/SecurityJwtProperties.java`：

```java
package com.aurora.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("myblog.security.jwt")
public record SecurityJwtProperties(
        String issuer,
        String secret,
        Duration accessTokenTtl
) {
}
```

- [x] **步骤 5：注册配置类并增加配置项**

修改 `SecurityConfig.java`，把 `SecurityJwtProperties` 加入配置绑定：

```java
@EnableConfigurationProperties({
        ApiCorsProperties.class,
        SecurityPublicEndpointProperties.class,
        SecurityJwtProperties.class
})
public class SecurityConfig {
}
```

修改 `MyBlog-springboot-v2/src/main/resources/application.yml`：

```yaml
myblog:
  security:
    jwt:
      issuer: myblog-v2
      secret: ${MYBLOG_JWT_SECRET:change-me-change-me-change-me-change-me}
      access-token-ttl: 15m
```

修改 `MyBlog-springboot-v2/src/test/resources/application-test.yml`：

```yaml
myblog:
  security:
    jwt:
      issuer: myblog-v2-test
      secret: test-secret-test-secret-test-secret-123456
      access-token-ttl: 15m
```

- [x] **步骤 6：重新运行配置测试**

运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=JwtPropertiesTest
```

预期：通过。

- [x] **步骤 7：提交 JWT 配置基线**

```powershell
git add MyBlog-springboot-v2/pom.xml MyBlog-springboot-v2/src/main/resources/application.yml MyBlog-springboot-v2/src/test/resources/application-test.yml MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/SecurityJwtProperties.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/JwtPropertiesTest.java
git commit -m "新增后端V2 JWT配置基线"
```

### 任务 2：建立身份领域模型与本地账号适配器

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/AuthRole.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/AuthenticatedUser.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/LoginCommand.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/domain/UserCredentialReader.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/ConfiguredIdentityProperties.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/infrastructure/ConfiguredUserCredentialReader.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/ConfiguredUserCredentialReaderTest.java`

- [x] **步骤 1：先写会失败的本地账号适配器测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/ConfiguredUserCredentialReaderTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.infrastructure.ConfiguredUserCredentialReader;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredUserCredentialReaderTest {

    @Test
    void loadsConfiguredUserWithoutExposingPasswordInPrincipal() {
        String hash = new BCryptPasswordEncoder().encode("correct-password");
        ConfiguredUserCredentialReader reader = ConfiguredUserCredentialReader.singleUser(
                "admin@example.com",
                hash,
                List.of(AuthRole.ADMIN));

        var credential = reader.findByUsername("admin@example.com");

        assertThat(credential).isPresent();
        assertThat(credential.get().username()).isEqualTo("admin@example.com");
        assertThat(credential.get().roles()).containsExactly(AuthRole.ADMIN);
        assertThat(credential.get().passwordHash()).isEqualTo(hash);
    }
}
```

- [x] **步骤 2：运行测试，确认它先失败**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=ConfiguredUserCredentialReaderTest
```

预期：失败，因为身份领域模型和本地账号适配器还不存在。

- [x] **步骤 3：创建身份领域类型**

创建 `AuthRole.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

public enum AuthRole {
    USER,
    ADMIN
}
```

创建 `AuthenticatedUser.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

import java.util.Set;

public record AuthenticatedUser(
        String id,
        String username,
        Set<AuthRole> roles
) {
    public boolean hasRole(AuthRole role) {
        return roles.contains(role);
    }
}
```

创建 `LoginCommand.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

public record LoginCommand(String username, String password) {
}
```

创建 `UserCredentialReader.java`：

```java
package com.aurora.myblog.v2.modules.identity.domain;

import java.util.List;
import java.util.Optional;

public interface UserCredentialReader {
    Optional<UserCredential> findByUsername(String username);

    record UserCredential(String id, String username, String passwordHash, List<AuthRole> roles) {
    }
}
```

- [x] **步骤 4：创建配置账号适配器**

创建 `ConfiguredIdentityProperties.java`：

```java
package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("myblog.identity")
public record ConfiguredIdentityProperties(List<User> users) {

    public ConfiguredIdentityProperties {
        users = users == null ? List.of() : List.copyOf(users);
    }

    public record User(String id, String username, String passwordHash, List<AuthRole> roles) {
        public User {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
```

创建 `ConfiguredUserCredentialReader.java`：

```java
package com.aurora.myblog.v2.modules.identity.infrastructure;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.UserCredentialReader;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@EnableConfigurationProperties(ConfiguredIdentityProperties.class)
public class ConfiguredUserCredentialReader implements UserCredentialReader {

    private final ConfiguredIdentityProperties properties;

    public ConfiguredUserCredentialReader(ConfiguredIdentityProperties properties) {
        this.properties = properties;
    }

    public static ConfiguredUserCredentialReader singleUser(String username, String passwordHash, List<AuthRole> roles) {
        ConfiguredIdentityProperties.User user =
                new ConfiguredIdentityProperties.User("test-user", username, passwordHash, roles);
        return new ConfiguredUserCredentialReader(new ConfiguredIdentityProperties(List.of(user)));
    }

    @Override
    public Optional<UserCredential> findByUsername(String username) {
        return properties.users().stream()
                .filter(user -> user.username().equalsIgnoreCase(username))
                .findFirst()
                .map(user -> new UserCredential(user.id(), user.username(), user.passwordHash(), user.roles()));
    }
}
```

- [x] **步骤 5：增加测试账号配置**

在 `application-test.yml` 中增加：

```yaml
myblog:
  identity:
    users:
      - id: test-admin
        username: admin@example.com
        password-hash: $2a$10$djjHOm86X5nKsIY0Zv0lO.iLoVuEO5J6mkBGdf1G.i/8qHL7IaOxy
        roles:
          - ADMIN
```

该 hash 对应明文密码 `password123`，只允许用于测试 profile。

- [x] **步骤 6：重新运行本地账号测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=ConfiguredUserCredentialReaderTest
```

预期：通过。

- [x] **步骤 7：提交身份模型基线**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity MyBlog-springboot-v2/src/test/resources/application-test.yml
git commit -m "新增后端V2身份模型基线"
```

### 任务 3：实现 JWT 签发、解析与撤销能力

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/auth/TokenClaims.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/auth/TokenPair.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/auth/TokenRevocationStore.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/support/InMemoryTokenRevocationStore.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/auth/JwtTokenService.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/JwtTokenServiceTest.java`

- [x] **步骤 1：先写会失败的 token 服务测试**

创建 `JwtTokenServiceTest.java`：

```java
package com.aurora.myblog.v2.common.security;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import com.aurora.myblog.v2.common.security.auth.JwtTokenService;
import com.aurora.myblog.v2.common.security.support.InMemoryTokenRevocationStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private final JwtTokenService tokenService = new JwtTokenService(
            new SecurityJwtProperties("myblog-v2-test", "test-secret-test-secret-test-secret-123456", Duration.ofMinutes(15)),
            new InMemoryTokenRevocationStore());

    @Test
    void issuesAndParsesAccessToken() {
        var token = tokenService.issueAccessToken("user-1", "admin@example.com", List.of("ADMIN"));
        var claims = tokenService.parse(token.accessToken()).orElseThrow();

        assertThat(claims.userId()).isEqualTo("user-1");
        assertThat(claims.username()).isEqualTo("admin@example.com");
        assertThat(claims.roles()).containsExactly("ADMIN");
    }

    @Test
    void revokedTokenCannotBeParsed() {
        var token = tokenService.issueAccessToken("user-1", "admin@example.com", List.of("ADMIN"));
        tokenService.revoke(token.accessToken());

        assertThat(tokenService.parse(token.accessToken())).isEmpty();
    }
}
```

- [x] **步骤 2：运行测试，确认它先失败**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=JwtTokenServiceTest
```

预期：失败，因为 token 类型和服务还不存在。

- [x] **步骤 3：创建 token 数据类型与撤销接口**

创建 `TokenClaims.java`：

```java
package com.aurora.myblog.v2.common.security.auth;

import java.time.Instant;
import java.util.List;

public record TokenClaims(
        String tokenId,
        String userId,
        String username,
        List<String> roles,
        Instant expiresAt
) {
}
```

创建 `TokenPair.java`：

```java
package com.aurora.myblog.v2.common.security.auth;

import java.time.Instant;

public record TokenPair(String accessToken, Instant expiresAt) {
}
```

创建 `TokenRevocationStore.java`：

```java
package com.aurora.myblog.v2.common.security.auth;

import java.time.Instant;

public interface TokenRevocationStore {
    void revoke(String tokenId, Instant expiresAt);

    boolean isRevoked(String tokenId);
}
```

创建 `InMemoryTokenRevocationStore.java`：

```java
package com.aurora.myblog.v2.common.security.support;

import com.aurora.myblog.v2.common.security.auth.TokenRevocationStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryTokenRevocationStore implements TokenRevocationStore {

    private final Map<String, Instant> revokedTokenIds = new ConcurrentHashMap<>();

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        revokedTokenIds.put(tokenId, expiresAt);
    }

    @Override
    public boolean isRevoked(String tokenId) {
        Instant expiresAt = revokedTokenIds.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            revokedTokenIds.remove(tokenId);
            return false;
        }
        return true;
    }
}
```

- [x] **步骤 4：实现 JWT token 服务**

创建 `JwtTokenService.java`：

```java
package com.aurora.myblog.v2.common.security.auth;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final SecurityJwtProperties properties;
    private final TokenRevocationStore revocationStore;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public JwtTokenService(SecurityJwtProperties properties, TokenRevocationStore revocationStore) {
        this.properties = properties;
        this.revocationStore = revocationStore;
        SecretKey secretKey = new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    public TokenPair issueAccessToken(String userId, String username, List<String> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(tokenId)
                .subject(userId)
                .claim("username", username)
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenPair(accessToken, expiresAt);
    }

    public Optional<TokenClaims> parse(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            if (revocationStore.isRevoked(jwt.getId())) {
                return Optional.empty();
            }
            return Optional.of(new TokenClaims(
                    jwt.getId(),
                    jwt.getSubject(),
                    jwt.getClaimAsString("username"),
                    readRoles(jwt),
                    jwt.getExpiresAt()));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public void revoke(String token) {
        parse(token).ifPresent(claims -> revocationStore.revoke(claims.tokenId(), claims.expiresAt()));
    }

    private List<String> readRoles(Jwt jwt) {
        List<String> roleNames = jwt.getClaimAsStringList("roles");
        if (roleNames == null) {
            return List.of();
        }
        return List.copyOf(roleNames);
    }
}
```

- [x] **步骤 5：重新运行 token 服务测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=JwtTokenServiceTest
```

预期：通过。

- [x] **步骤 6：提交 token 服务**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/auth MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/support MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/JwtTokenServiceTest.java
git commit -m "新增后端V2 JWT令牌服务"
```

### 任务 4：实现登录用例与认证 API

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/AuthService.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/application/AuthTokenService.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/infrastructure/security/JwtAuthTokenServiceAdapter.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/LoginRequest.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/LoginResponse.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/MeResponse.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthServiceTest.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java`

实施调整：

- `common.security` 是安全实现包，`modules.identity` 不能直接依赖它；实际实现中通过 identity 自己的 `AuthTokenService` 端口和 `infrastructure.security.JwtAuthTokenServiceAdapter` 连接 JWT 服务。
- `/api/auth/me` 依赖任务 5 的 Bearer 过滤器与 `@CurrentUser` 参数解析器，本任务先保留 `MeResponse` DTO，不提前实现 `/me`。
- 登录接口必须公开访问，因此本任务同步关闭 stateless API 的 CSRF，并把 `/api/auth/login` 加入公开端点。

- [x] **步骤 1：先写会失败的登录服务测试**

创建 `AuthServiceTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import com.aurora.myblog.v2.common.security.auth.JwtTokenService;
import com.aurora.myblog.v2.common.security.support.InMemoryTokenRevocationStore;
import com.aurora.myblog.v2.modules.identity.application.AuthService;
import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import com.aurora.myblog.v2.modules.identity.infrastructure.ConfiguredUserCredentialReader;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenService tokenService = new JwtTokenService(
            new SecurityJwtProperties("myblog-v2-test", "test-secret-test-secret-test-secret-123456", Duration.ofMinutes(15)),
            new InMemoryTokenRevocationStore());

    @Test
    void logsInWithValidCredential() {
        AuthService authService = new AuthService(
                ConfiguredUserCredentialReader.singleUser("admin@example.com", passwordEncoder.encode("password123"), List.of(AuthRole.ADMIN)),
                passwordEncoder,
                tokenService);

        var result = authService.login(new LoginCommand("admin@example.com", "password123"));

        assertThat(result.user().username()).isEqualTo("admin@example.com");
        assertThat(result.user().roles()).containsExactly(AuthRole.ADMIN);
        assertThat(result.token().accessToken()).isNotBlank();
    }

    @Test
    void rejectsInvalidCredentialWithSameError() {
        AuthService authService = new AuthService(
                ConfiguredUserCredentialReader.singleUser("admin@example.com", passwordEncoder.encode("password123"), List.of(AuthRole.ADMIN)),
                passwordEncoder,
                tokenService);

        assertThatThrownBy(() -> authService.login(new LoginCommand("admin@example.com", "wrong")))
                .hasMessage("用户名或密码错误");
    }
}
```

- [x] **步骤 2：运行服务测试，确认它先失败**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=AuthServiceTest
```

预期：失败，因为 `AuthService` 和 API 类型还不存在。

- [x] **步骤 3：实现登录服务和密码编码器**

修改 `SecurityConfig.java`，增加密码编码器 Bean：

```java
@Bean
PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

需要新增 import：

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
```

创建 `AuthService.java`：

```java
package com.aurora.myblog.v2.modules.identity.application;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.identity.domain.AuthenticatedUser;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import com.aurora.myblog.v2.modules.identity.domain.UserCredentialReader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private final UserCredentialReader credentialReader;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;

    public AuthService(UserCredentialReader credentialReader, PasswordEncoder passwordEncoder, AuthTokenService tokenService) {
        this.credentialReader = credentialReader;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResult login(LoginCommand command) {
        var credential = credentialReader.findByUsername(command.username())
                .filter(user -> passwordEncoder.matches(command.password(), user.passwordHash()))
                .orElseThrow(() -> new ApiException(ApiErrorCode.BAD_CREDENTIALS, "用户名或密码错误"));
        AuthenticatedUser user = new AuthenticatedUser(
                credential.id(),
                credential.username(),
                Set.copyOf(credential.roles()));
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

同时扩展 `ApiErrorCode.java`：

```java
BAD_CREDENTIALS("AUTH_001", "用户名或密码错误"),
AUTHENTICATION_REQUIRED("AUTH_002", "用户未登录"),
FORBIDDEN("AUTH_003", "权限不足"),
INVALID_TOKEN("AUTH_004", "登录状态无效")
```

修改 `GlobalExceptionHandler.java`，让认证类业务异常返回正确 HTTP 状态：

```java
@ExceptionHandler(ApiException.class)
ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
    HttpStatus status = switch (ex.errorCode()) {
        case BAD_CREDENTIALS, AUTHENTICATION_REQUIRED, INVALID_TOKEN -> HttpStatus.UNAUTHORIZED;
        case FORBIDDEN -> HttpStatus.FORBIDDEN;
        default -> HttpStatus.CONFLICT;
    };
    return ResponseEntity.status(status)
            .body(ApiResponse.fail(ex.errorCode().code(), ex.getMessage()));
}
```

- [x] **步骤 4：创建 API DTO 与 Controller**

创建 `LoginRequest.java`：

```java
package com.aurora.myblog.v2.modules.identity.api;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
```

创建 `LoginResponse.java`：

```java
package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;

import java.time.Instant;
import java.util.Set;

public record LoginResponse(
        String accessToken,
        Instant expiresAt,
        User user
) {
    public record User(String id, String username, Set<AuthRole> roles) {
    }
}
```

创建 `MeResponse.java`：

```java
package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;

import java.util.Set;

public record MeResponse(String id, String username, Set<AuthRole> roles) {
}
```

创建 `AuthController.java`：

```java
package com.aurora.myblog.v2.modules.identity.api;

import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.modules.identity.application.AuthService;
import com.aurora.myblog.v2.modules.identity.domain.LoginCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
    ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authService.login(new LoginCommand(request.username(), request.password()));
        LoginResponse.User user = new LoginResponse.User(
                result.user().id(),
                result.user().username(),
                result.user().roles());
        return ApiResponse.ok(new LoginResponse(result.token().accessToken(), result.token().expiresAt(), user));
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization.replaceFirst("Bearer ", ""));
        return ApiResponse.ok(null);
    }
}
```

- [x] **步骤 5：重新运行登录服务测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=AuthServiceTest
```

预期：通过。

- [x] **步骤 6：提交登录用例**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiErrorCode.java
git commit -m "新增后端V2登录用例"
```

### 任务 5：接入 Bearer Token 过滤器与统一安全错误响应

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/auth/AuthenticatedPrincipal.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/auth/CurrentUser.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/auth/CurrentUserArgumentResolver.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/auth/JwtAuthenticationFilter.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityProblemSupport.java`

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityConfig.java`

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/JwtAuthenticationFilterTest.java`

实施调整：

- 为遵守架构规则，`common.security` 不直接依赖 `modules.identity`；过滤器解析出的当前用户使用 `common.auth.AuthenticatedPrincipal` 表达。
- `modules.identity` 不依赖 `common.security`，控制器只依赖 `common.auth.CurrentUser` 和公共认证主体。
- `JwtAuthenticationFilter` 与 `SecurityProblemSupport` 由 `SecurityConfig` 显式创建，避免 `@WebMvcTest` 切片测试被完整 JWT 实现拖入。

- [x] **步骤 1：先写会失败的鉴权链路测试**

创建 `JwtAuthenticationFilterTest.java`：

```java
package com.aurora.myblog.v2.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsMeWithValidBearerTokenAndRejectsAfterLogout() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(response, "$.data.accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin@example.com"));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
```

- [x] **步骤 2：运行测试，确认它先失败**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=JwtAuthenticationFilterTest
```

预期：失败，因为 Bearer 过滤器和 `@CurrentUser` 解析器还不存在。

- [x] **步骤 3：实现当前用户注解和解析器**

创建 `CurrentUser.java`：

```java
package com.aurora.myblog.v2.common.security.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
```

创建 `CurrentUserArgumentResolver.java`：

```java
package com.aurora.myblog.v2.common.security.auth;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.identity.domain.AuthenticatedUser;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.List;

@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver, WebMvcConfigurer {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new ApiException(ApiErrorCode.AUTHENTICATION_REQUIRED, "用户未登录");
        }
        return user;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(this);
    }
}
```

- [x] **步骤 4：实现 Bearer JWT 过滤器**

创建 `JwtAuthenticationFilter.java`：

```java
package com.aurora.myblog.v2.common.security.auth;

import com.aurora.myblog.v2.modules.identity.domain.AuthRole;
import com.aurora.myblog.v2.modules.identity.domain.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService tokenService;

    public JwtAuthenticationFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length());
            tokenService.parse(token).ifPresent(claims -> {
                Set<AuthRole> roles = Set.copyOf(claims.roles());
                AuthenticatedUser user = new AuthenticatedUser(claims.userId(), claims.username(), roles);
                var authorities = claims.roles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toSet());
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, token, authorities));
            });
        }
        filterChain.doFilter(request, response);
    }
}
```

- [x] **步骤 5：实现统一安全错误响应并接入安全配置**

创建 `SecurityProblemSupport.java`：

```java
package com.aurora.myblog.v2.common.security;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityProblemSupport {

    private final ObjectMapper objectMapper;

    public SecurityProblemSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeUnauthorized(HttpServletResponse response) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED,
                ApiResponse.fail(ApiErrorCode.AUTHENTICATION_REQUIRED.code(), "用户未登录"));
    }

    public void writeForbidden(HttpServletResponse response) throws IOException {
        write(response, HttpServletResponse.SC_FORBIDDEN,
                ApiResponse.fail(ApiErrorCode.FORBIDDEN.code(), "权限不足"));
    }

    private void write(HttpServletResponse response, int status, ApiResponse<Void> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
```

修改 `SecurityConfig.java`：

```java
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http,
                                SecurityPublicEndpointProperties publicEndpointProperties,
                                JwtAuthenticationFilter jwtAuthenticationFilter,
                                SecurityProblemSupport problemSupport) throws Exception {
    String[] publicEndpoints = publicEndpointProperties.publicEndpoints().toArray(String[]::new);
    return http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(publicEndpoints).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, ex) -> problemSupport.writeUnauthorized(response))
                    .accessDeniedHandler((request, response, ex) -> problemSupport.writeForbidden(response)))
            .httpBasic(httpBasic -> httpBasic.disable())
            .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .build();
}
```

并把 `application-test.yml` 的公开端点加入：

```yaml
myblog:
  security:
    public-endpoints:
      - /actuator/health
      - /api/public/security-probe
      - /api/auth/login
```

- [x] **步骤 6：重新运行鉴权链路测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=JwtAuthenticationFilterTest
```

预期：通过。

- [x] **步骤 7：提交 Bearer 安全链路**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security MyBlog-springboot-v2/src/test/resources/application-test.yml
git commit -m "接入后端V2 Bearer认证链路"
```

### 任务 6：补齐认证 API 和角色授权回归

**文件：**

- 修改：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityProbeController.java`

- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/SecurityConfigTest.java`

- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java`

- [ ] **步骤 1：先写角色授权失败测试**

修改 `SecurityConfigTest.java`，增加：

```java
@Test
void returnsForbiddenWhenRoleIsInsufficient() throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"user@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = com.jayway.jsonpath.JsonPath.read(response, "$.data.accessToken");

    mockMvc.perform(get("/api/admin/security-probe").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_003"));
}
```

- [ ] **步骤 2：运行测试，确认它先失败**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=SecurityConfigTest
```

预期：失败，因为测试 profile 还没有普通用户账号，或后台探针授权规则还没覆盖该路径。

- [ ] **步骤 3：补测试用户和后台探针断言**

在 `application-test.yml` 中增加普通用户：

```yaml
myblog:
  identity:
    users:
      - id: test-user
        username: user@example.com
        password-hash: $2a$10$djjHOm86X5nKsIY0Zv0lO.iLoVuEO5J6mkBGdf1G.i/8qHL7IaOxy
        roles:
          - USER
```

保留已有 `admin@example.com`，不要删除。

- [ ] **步骤 4：补认证 API 测试**

创建或完善 `AuthControllerTest.java`：

```java
package com.aurora.myblog.v2.modules.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    void loginReturnsUnifiedResponse() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin@example.com"));
    }

    @Test
    void loginFailureDoesNotRevealAccountExistence() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"missing@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_001"))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void meRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_002"));
    }
}
```

- [ ] **步骤 5：重新运行认证与授权测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=AuthControllerTest,SecurityConfigTest
```

预期：通过。

- [ ] **步骤 6：提交认证 API 回归测试**

```powershell
git add MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/AuthControllerTest.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/SecurityConfigTest.java MyBlog-springboot-v2/src/test/resources/application-test.yml
git commit -m "补齐后端V2认证授权回归"
```

### 任务 7：增加架构规则并做整体验证

**文件：**

- 修改：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/ArchitectureRulesTest.java`

- 验证：`MyBlog-springboot-v2/**`

- [ ] **步骤 1：增加认证边界架构规则**

修改 `ArchitectureRulesTest.java`，增加：

```java
@ArchTest
static final ArchRule identity_domain_does_not_depend_on_spring_security =
        noClasses()
                .that().resideInAPackage("..modules.identity.domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework.security..");

@ArchTest
static final ArchRule common_security_does_not_depend_on_identity_infrastructure =
        noClasses()
                .that().resideInAPackage("..common.security..")
                .should().dependOnClassesThat().resideInAPackage("..modules.identity.infrastructure..");
```

- [ ] **步骤 2：运行架构测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=ArchitectureRulesTest
```

预期：通过。身份领域模型不依赖 Spring Security，公共安全层不反向依赖 identity 的基础设施适配器。

- [ ] **步骤 3：运行完整测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
```

预期：全部测试通过。

- [ ] **步骤 4：运行打包验证**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

预期：构建通过，生成 `MyBlog-springboot-v2/target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar`。

- [ ] **步骤 5：提交架构规则和验证交接**

如果只新增架构规则：

```powershell
git add MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/ArchitectureRulesTest.java
git commit -m "新增后端V2认证架构规则"
```

如果还更新了本计划完成状态，只额外暂存本计划文档：

```powershell
git add docs/superpowers/plans/2026-05-23-backend-v2-security-capabilities.zh-CN.md
git commit -m "同步后端V2安全计划完成状态"
```

## 下一阶段交接

本计划完成后，后端 V2 将具备可执行认证闭环，但仍不代表旧业务身份体系已经迁移。下一份计划建议创建 `docs/superpowers/plans/2026-05-23-backend-v2-identity-domain-migration.zh-CN.md`，覆盖：

- 旧 `t_user_auth`、`t_user_info`、`t_user_role`、`t_role`、`t_resource` 的 schema 盘点。
- V2 identity 表结构与 Flyway 迁移。
- 真实用户登录仓储替换 `ConfiguredUserCredentialReader`。
- 角色、菜单、资源权限是否保留动态模型。
- 旧 token/Redis 登录态到 V2 Bearer JWT 的迁移策略。

## 自检记录

- 覆盖范围：认证方式、登录、当前用户、登出撤销、401/403、角色授权、架构边界、完整验证均有任务。
- 明确排除：QQ 登录、上传安全、日志脱敏、XSS、限流、真实用户表迁移不在本计划内。
- 类型一致性：`AuthenticatedUser`、`AuthRole`、`JwtTokenService`、`AuthService`、`AuthController` 在计划中按同一命名贯穿。
