# Java Backend V2 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first executable MyBlog Java backend V2 foundation without mutating the legacy backend module: a Java 17 Spring Boot 3.5 backend skeleton with explicit API, security, configuration, database migration, and architecture-test baselines.

**Architecture:** Create `MyBlog-springboot-v2` beside the current `MyBlog-springboot` module so the old backend stays available as a migration source while V2 gets a clean module boundary. The foundation module is a modular monolith seed: `common` owns cross-cutting web/security/configuration primitives, `modules` owns future business domains, and `infrastructure` owns adapters such as persistence and storage. This plan stops before business-domain migration; later plans migrate `identity`, `content`, `media`, and the remaining domains against this baseline.

**Tech Stack:** Java 17, Maven, Spring Boot 3.5.14, Spring Security 6.5.x through Spring Boot dependency management, Spring Validation, Spring Actuator, Flyway, H2 test database, JUnit 5, MockMvc, ArchUnit.

---

## Scope Guard

This plan implements only the V2 backend foundation:

- A new backend V2 module that builds independently of the legacy backend.
- A stable API envelope and global error baseline.
- Default-deny HTTP security with a deliberately small public endpoint policy.
- Typed configuration and test profiles.
- Flyway migration wiring and one migration smoke test.
- Architecture tests that keep the modular-monolith seed from collapsing back into package soup.

This plan does not migrate existing MyBlog users, articles, comments, uploads, permissions, jobs, search, RabbitMQ consumers, or object-storage implementations. Those require separate implementation plans after this baseline is executable.

## Primary References

- Approved V2 design: `docs/superpowers/specs/2026-05-22-myblog-v2-refactor-design.md`
- Current legacy backend build: `MyBlog-springboot/pom.xml`
- Current legacy security config: `MyBlog-springboot/src/main/java/com/aurora/config/WebSecurityConfig.java`
- Current legacy MVC config: `MyBlog-springboot/src/main/java/com/aurora/config/WebMvcConfig.java`
- Current legacy response envelope: `MyBlog-springboot/src/main/java/com/aurora/model/vo/ResultVO.java`
- Current legacy error handler: `MyBlog-springboot/src/main/java/com/aurora/handler/ControllerAdviceHandler.java`
- Current legacy JWT filter: `MyBlog-springboot/src/main/java/com/aurora/filter/JwtAuthenticationTokenFilter.java`

## File Structure

Create this initial V2 backend structure:

```text
MyBlog-springboot-v2
├─ pom.xml
├─ src
│  ├─ main
│  │  ├─ java/com/aurora/myblog/v2
│  │  │  ├─ MyBlogV2Application.java
│  │  │  ├─ common
│  │  │  │  ├─ config
│  │  │  │  │  ├─ ApiCorsProperties.java
│  │  │  │  │  └─ SecurityPublicEndpointProperties.java
│  │  │  │  ├─ error
│  │  │  │  │  ├─ ApiErrorCode.java
│  │  │  │  │  ├─ ApiException.java
│  │  │  │  │  └─ GlobalExceptionHandler.java
│  │  │  │  ├─ security
│  │  │  │  │  ├─ SecurityConfig.java
│  │  │  │  │  └─ SecurityProbeController.java
│  │  │  │  └─ web
│  │  │  │     └─ ApiResponse.java
│  │  │  ├─ infrastructure
│  │  │  │  └─ persistence
│  │  │  │     └─ package-info.java
│  │  │  └─ modules
│  │  │     └─ package-info.java
│  │  └─ resources
│  │     ├─ application.yml
│  │     ├─ application-local.yml
│  │     └─ db/migration/V1__create_v2_schema_marker.sql
│  └─ test
│     ├─ java/com/aurora/myblog/v2
│     │  ├─ ArchitectureRulesTest.java
│     │  ├─ MyBlogV2ApplicationTest.java
│     │  ├─ common/config/BackendPropertiesTest.java
│     │  ├─ common/error/GlobalExceptionHandlerTest.java
│     │  ├─ common/security/SecurityConfigTest.java
│     │  └─ infrastructure/persistence/FlywayMigrationTest.java
│     └─ resources/application-test.yml
```

`SecurityProbeController` is intentionally tiny. It gives security tests one public probe and one protected probe before real V2 domain controllers exist.

### Task 1: Scaffold the Spring Boot V2 Module

**Files:**

- Create: `MyBlog-springboot-v2/pom.xml`

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/MyBlogV2Application.java`

- Create: `MyBlog-springboot-v2/src/main/resources/application.yml`

- Create: `MyBlog-springboot-v2/src/main/resources/application-local.yml`

- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/MyBlogV2ApplicationTest.java`

- Create: `MyBlog-springboot-v2/src/test/resources/application-test.yml`

- [ ] **Step 1: Write the first context smoke test**

Create `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/MyBlogV2ApplicationTest.java`:

```java
package com.aurora.myblog.v2;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class MyBlogV2ApplicationTest {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run the new test and verify it fails before the module exists**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=MyBlogV2ApplicationTest
```

Expected: Maven fails because `MyBlog-springboot-v2/pom.xml` does not exist yet.

- [ ] **Step 3: Create the Maven module build**

Create `MyBlog-springboot-v2/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>com.aurora</groupId>
    <artifactId>myblog-springboot-v2</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <name>myblog-springboot-v2</name>

    <properties>
        <java.version>17</java.version>
        <archunit.version>1.4.1</archunit.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>${archunit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Create the application entry point**

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/MyBlogV2Application.java`:

```java
package com.aurora.myblog.v2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class MyBlogV2Application {

    public static void main(String[] args) {
        SpringApplication.run(MyBlogV2Application.class, args);
    }
}
```

- [ ] **Step 5: Create profile-safe baseline configuration**

Create `MyBlog-springboot-v2/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: myblog-v2
  profiles:
    default: local

management:
  endpoints:
    web:
      exposure:
        include: health
```

Create `MyBlog-springboot-v2/src/main/resources/application-local.yml`:

```yaml
myblog:
  cors:
    allowed-origins:
      - http://localhost:5173
      - http://localhost:5174
  security:
    public-endpoints:
      - /actuator/health
      - /api/public/security-probe
```

Create `MyBlog-springboot-v2/src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:myblog_v2_test;MODE=MySQL;DB_CLOSE_DELAY=-1
    username: sa
    password:
  flyway:
    enabled: true

myblog:
  cors:
    allowed-origins:
      - http://localhost:5173
  security:
    public-endpoints:
      - /actuator/health
      - /api/public/security-probe
```

- [ ] **Step 6: Run the context test**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=MyBlogV2ApplicationTest
```

Expected: PASS with one Spring context smoke test.

- [ ] **Step 7: Commit the V2 module scaffold**

```powershell
git add MyBlog-springboot-v2
git commit -m "build: scaffold backend v2 module"
```

### Task 2: Add the API Response and Error Baseline

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/ApiResponse.java`

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiErrorCode.java`

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiException.java`

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/GlobalExceptionHandler.java`

- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/error/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write failing API error tests**

Create `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/error/GlobalExceptionHandlerTest.java`:

```java
package com.aurora.myblog.v2.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.ErrorProbeController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc;

    GlobalExceptionHandlerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void returnsValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("title must not be blank"));
    }

    @Test
    void returnsBusinessEnvelope() throws Exception {
        mockMvc.perform(post("/api/test/errors/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("duplicate title"));
    }

    @RestController
    static class ErrorProbeController {

        @PostMapping("/api/test/errors/validation")
        void validate(@Valid @RequestBody TitleRequest request) {
        }

        @PostMapping("/api/test/errors/business")
        void conflict() {
            throw new ApiException(ApiErrorCode.CONFLICT, "duplicate title");
        }
    }

    record TitleRequest(@NotBlank(message = "title must not be blank") String title) {
    }
}
```

- [ ] **Step 2: Run the API error test and verify it fails**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=GlobalExceptionHandlerTest
```

Expected: FAIL because `ApiException`, `ApiErrorCode`, and `GlobalExceptionHandler` do not exist yet.

- [ ] **Step 3: Create the API response type**

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/ApiResponse.java`:

```java
package com.aurora.myblog.v2.common.web;

public record ApiResponse<T>(boolean success, String code, String message, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", "success", data);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
```

- [ ] **Step 4: Create typed API errors**

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiErrorCode.java`:

```java
package com.aurora.myblog.v2.common.error;

import org.springframework.http.HttpStatus;

public enum ApiErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    CONFLICT(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ApiErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
```

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiException.java`:

```java
package com.aurora.myblog.v2.common.error;

public class ApiException extends RuntimeException {

    private final ApiErrorCode code;

    public ApiException(ApiErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ApiErrorCode code() {
        return code;
    }
}
```

- [ ] **Step 5: Add the global error handler**

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/GlobalExceptionHandler.java`:

```java
package com.aurora.myblog.v2.common.error;

import com.aurora.myblog.v2.common.web.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.code().status())
                .body(ApiResponse.fail(exception.code().name(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("request validation failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ApiErrorCode.VALIDATION_ERROR.name(), message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        log.error("Unhandled API exception", exception);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(ApiErrorCode.INTERNAL_ERROR.name(), "internal server error"));
    }
}
```

- [ ] **Step 6: Run API error tests**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=GlobalExceptionHandlerTest
```

Expected: PASS with validation and business-error envelope assertions.

- [ ] **Step 7: Commit the API baseline**

```powershell
git add MyBlog-springboot-v2
git commit -m "feat: add backend v2 api error baseline"
```

### Task 3: Add Typed CORS and Public Endpoint Configuration

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/ApiCorsProperties.java`

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/SecurityPublicEndpointProperties.java`

- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/config/BackendPropertiesTest.java`

- [ ] **Step 1: Write failing configuration binding tests**

Create `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/config/BackendPropertiesTest.java`:

```java
package com.aurora.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class BackendPropertiesTest {

    @Autowired
    private ApiCorsProperties corsProperties;

    @Autowired
    private SecurityPublicEndpointProperties securityProperties;

    @Test
    void bindsCorsOrigins() {
        assertThat(corsProperties.allowedOrigins()).containsExactly("http://localhost:5173");
    }

    @Test
    void bindsPublicEndpoints() {
        assertThat(securityProperties.publicEndpoints())
                .contains("/actuator/health", "/api/public/security-probe");
    }
}
```

- [ ] **Step 2: Run the configuration binding test and verify it fails**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=BackendPropertiesTest
```

Expected: FAIL because both properties records do not exist.

- [ ] **Step 3: Add typed configuration records**

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/ApiCorsProperties.java`:

```java
package com.aurora.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("myblog.cors")
public record ApiCorsProperties(List<String> allowedOrigins) {
}
```

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/SecurityPublicEndpointProperties.java`:

```java
package com.aurora.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("myblog.security")
public record SecurityPublicEndpointProperties(List<String> publicEndpoints) {
}
```

- [ ] **Step 4: Run the properties tests**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=BackendPropertiesTest
```

Expected: PASS with typed CORS and public endpoint config bound from the test profile.

- [ ] **Step 5: Commit the typed configuration baseline**

```powershell
git add MyBlog-springboot-v2
git commit -m "feat: add backend v2 typed security config"
```

### Task 4: Enforce Default-Deny HTTP Security

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityConfig.java`

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityProbeController.java`

- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/SecurityConfigTest.java`

- [ ] **Step 1: Write failing security policy tests**

Create `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/SecurityConfigTest.java`:

```java
package com.aurora.myblog.v2.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(SecurityProbeController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void permitsOnlyConfiguredPublicProbe() throws Exception {
        mockMvc.perform(get("/api/public/security-probe"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnconfiguredApiRoute() throws Exception {
        mockMvc.perform(get("/api/admin/security-probe"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run the security test and verify it fails**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=SecurityConfigTest
```

Expected: FAIL because the security config and probe controller do not exist.

- [ ] **Step 3: Add security probes**

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityProbeController.java`:

```java
package com.aurora.myblog.v2.common.security;

import com.aurora.myblog.v2.common.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityProbeController {

    @GetMapping("/api/public/security-probe")
    ApiResponse<String> publicProbe() {
        return ApiResponse.ok("public");
    }

    @GetMapping("/api/admin/security-probe")
    ApiResponse<String> protectedProbe() {
        return ApiResponse.ok("protected");
    }
}
```

- [ ] **Step 4: Add `SecurityFilterChain` based security**

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityConfig.java`:

```java
package com.aurora.myblog.v2.common.security;

import com.aurora.myblog.v2.common.config.ApiCorsProperties;
import com.aurora.myblog.v2.common.config.SecurityPublicEndpointProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties({ApiCorsProperties.class, SecurityPublicEndpointProperties.class})
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http,
                                    SecurityPublicEndpointProperties publicEndpointProperties) throws Exception {
        String[] publicEndpoints = publicEndpointProperties.publicEndpoints().toArray(String[]::new);
        return http
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(publicEndpoints).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(ApiCorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
```

This foundation keeps Spring Security CSRF defaults intact. The later authentication plan must decide whether the final browser/API contract uses cookie-backed sessions, bearer tokens, or a deliberately documented split before state-changing frontend integration begins.

- [ ] **Step 5: Run the security tests**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=SecurityConfigTest
```

Expected: PASS. The public probe returns 200 and the unlisted admin probe returns 401.

- [ ] **Step 6: Commit the default-deny security baseline**

```powershell
git add MyBlog-springboot-v2
git commit -m "feat: default deny backend v2 endpoints"
```

### Task 5: Add Flyway Migration Smoke Coverage

**Files:**

- Create: `MyBlog-springboot-v2/src/main/resources/db/migration/V1__create_v2_schema_marker.sql`

- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/infrastructure/persistence/FlywayMigrationTest.java`

- [ ] **Step 1: Write the failing migration test**

Create `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/infrastructure/persistence/FlywayMigrationTest.java`:

```java
package com.aurora.myblog.v2.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesTheV2BaselineMigration() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from v2_schema_marker",
                Integer.class);

        assertThat(count).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run the migration test and verify it fails**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=FlywayMigrationTest
```

Expected: FAIL because `v2_schema_marker` has not been created by any Flyway migration.

- [ ] **Step 3: Add the Flyway baseline migration**

Create `MyBlog-springboot-v2/src/main/resources/db/migration/V1__create_v2_schema_marker.sql`:

```sql
create table v2_schema_marker (
    id bigint primary key,
    description varchar(128) not null
);

insert into v2_schema_marker (id, description)
values (1, 'myblog v2 migration baseline');
```

- [ ] **Step 4: Run the migration test**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=FlywayMigrationTest
```

Expected: PASS. Flyway applies `V1__create_v2_schema_marker.sql` against the H2 test database.

- [ ] **Step 5: Commit migration wiring**

```powershell
git add MyBlog-springboot-v2
git commit -m "feat: add backend v2 migration baseline"
```

### Task 6: Add Modular Monolith Architecture Rules

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/package-info.java`

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/infrastructure/persistence/package-info.java`

- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/ArchitectureRulesTest.java`

- [ ] **Step 1: Add package markers for the first architecture boundaries**

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/package-info.java`:

```java
/**
 * Business modules live below this package.
 */
package com.aurora.myblog.v2.modules;
```

Create `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/infrastructure/persistence/package-info.java`:

```java
/**
 * Persistence adapters and migration-facing support live below this package.
 */
package com.aurora.myblog.v2.infrastructure.persistence;
```

- [ ] **Step 2: Write architecture rules**

Create `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/ArchitectureRulesTest.java`:

```java
package com.aurora.myblog.v2;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.aurora.myblog.v2", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule modules_do_not_depend_on_common_security_implementation =
            noClasses()
                    .that().resideInAPackage("..modules..")
                    .should().dependOnClassesThat().resideInAPackage("..common.security..");

    @ArchTest
    static final ArchRule common_does_not_depend_on_business_modules =
            noClasses()
                    .that().resideInAPackage("..common..")
                    .should().dependOnClassesThat().resideInAPackage("..modules..");
}
```

- [ ] **Step 3: Run the architecture test**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=ArchitectureRulesTest
```

Expected: PASS. The initial boundary rules are executable before the first domain module migrates.

- [ ] **Step 4: Commit architecture guardrails**

```powershell
git add MyBlog-springboot-v2
git commit -m "test: guard backend v2 module boundaries"
```

### Task 7: Verify the Foundation as One Unit

**Files:**

- Verify: `MyBlog-springboot-v2/**`

- [ ] **Step 1: Run the full V2 backend test suite**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test
```

Expected: PASS for context, API error, typed configuration, security, Flyway, and architecture tests.

- [ ] **Step 2: Build the runnable V2 backend artifact**

Run:

```powershell
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

Expected: PASS and a backend artifact under `MyBlog-springboot-v2/target/`.

- [ ] **Step 3: Record the next-plan handoff**

Create `docs/superpowers/plans/2026-05-22-java-backend-v2-security-capabilities.md` only in the next planning session. That next plan should cover:

- authentication model and JWT/session decision,

- identity module migration,

- security error JSON integration,

- upload validation,

- log redaction,

- XSS sanitization,

- rate-limit failure policy.

- [ ] **Step 4: Commit only if verification or handoff docs changed**

If no files changed after verification, do not create an empty commit. If a handoff note changed, stage only that note and commit it explicitly.

## Execution Notes

- Work in an isolated V2 branch or worktree before implementing this plan. Do not implement this foundation on the online-release branch.
- Keep `MyBlog-springboot` untouched during this plan. It is a behavior and data-model reference until V2 domain migration begins.
- Do not copy legacy `WebSecurityConfig`, `ResultVO`, `ControllerAdviceHandler`, `FileUtil`, or `HTMLUtil` into V2. Their current behavior is evidence for the redesign, not a template.
- Keep the public endpoint list deliberately small until the V2 API contract plan declares each public resource.
