# Java 后端 V2 基础工程实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐项执行本计划。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 在不改动旧后端模块的前提下，建立第一版可执行的 MyBlog Java 后端 V2 基础工程：一个基于 Java 17 和 Spring Boot 3.5 的新后端骨架，并提前固定 API、安全、配置、数据库迁移和架构测试基线。

**架构：** 在当前 `MyBlog-springboot` 旁边新增 `MyBlog-springboot-v2`，让旧后端继续作为业务行为和迁移依据，同时让 V2 拥有清晰的新边界。V2 基础模块先按模块化单体落地：`common` 承接 Web、安全、配置等横切基础能力，`modules` 承接后续业务域，`infrastructure` 承接持久化、存储等基础设施适配。本计划只建立基础工程，不迁移具体业务；后续再分别为 `identity`、`content`、`media` 等模块写迁移计划。

**技术栈：** Java 17、Maven、Spring Boot 3.5.14、由 Spring Boot 依赖管理的 Spring Security 6.5.x、Spring Validation、Spring Actuator、Flyway、H2 测试数据库、JUnit 5、MockMvc、ArchUnit。

---

## 范围约束

本计划只实现后端 V2 foundation：

- 新建一个可独立构建的 V2 后端模块，不修改旧后端模块。
- 建立稳定的 API 响应包裹和全局异常基线。
- 建立默认拒绝的 HTTP 安全基线，并把公开端点收敛到显式配置。
- 建立类型化配置和测试 profile。
- 接入 Flyway 迁移入口，并加一个迁移冒烟测试。
- 建立架构测试，防止模块化单体种子重新退化成无边界包堆叠。

本计划不迁移现有用户、文章、评论、上传、权限、任务、搜索、RabbitMQ 消费者或对象存储实现。它们必须在基础工程可执行后，按业务域拆分后续实施计划。

## 主要参考

- 已确认的 V2 总设计：`docs/superpowers/specs/2026-05-22-myblog-v2-refactor-design.md`
- 当前旧后端构建：`MyBlog-springboot/pom.xml`
- 当前旧安全配置：`MyBlog-springboot/src/main/java/com/aurora/config/WebSecurityConfig.java`
- 当前旧 MVC 配置：`MyBlog-springboot/src/main/java/com/aurora/config/WebMvcConfig.java`
- 当前旧响应模型：`MyBlog-springboot/src/main/java/com/aurora/model/vo/ResultVO.java`
- 当前旧异常处理：`MyBlog-springboot/src/main/java/com/aurora/handler/ControllerAdviceHandler.java`
- 当前旧 JWT 过滤器：`MyBlog-springboot/src/main/java/com/aurora/filter/JwtAuthenticationTokenFilter.java`

## 文件结构

先创建下面这套 V2 后端基础结构：

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

`SecurityProbeController` 只用于基础安全测试。在真实 V2 业务 Controller 还不存在时，它提供一个公开探针和一个受保护探针。

### 任务 1：搭建 Spring Boot V2 模块

**文件：**

- 新建：`MyBlog-springboot-v2/pom.xml`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/MyBlogV2Application.java`

- 新建：`MyBlog-springboot-v2/src/main/resources/application.yml`

- 新建：`MyBlog-springboot-v2/src/main/resources/application-local.yml`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/MyBlogV2ApplicationTest.java`

- 新建：`MyBlog-springboot-v2/src/test/resources/application-test.yml`

- [ ] **步骤 1：先写第一个上下文冒烟测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/MyBlogV2ApplicationTest.java`：

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

- [ ] **步骤 2：运行测试，确认模块不存在时测试会失败**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=MyBlogV2ApplicationTest
```

预期：Maven 失败，因为 `MyBlog-springboot-v2/pom.xml` 还不存在。

- [ ] **步骤 3：创建 Maven 模块构建文件**

创建 `MyBlog-springboot-v2/pom.xml`：

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

- [ ] **步骤 4：创建应用入口**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/MyBlogV2Application.java`：

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

- [ ] **步骤 5：创建不会泄露环境差异的基础配置**

创建 `MyBlog-springboot-v2/src/main/resources/application.yml`：

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

创建 `MyBlog-springboot-v2/src/main/resources/application-local.yml`：

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

创建 `MyBlog-springboot-v2/src/test/resources/application-test.yml`：

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

- [ ] **步骤 6：运行上下文测试**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=MyBlogV2ApplicationTest
```

预期：通过，出现一个 Spring 上下文冒烟测试。

- [ ] **步骤 7：提交 V2 模块骨架**

```powershell
git add MyBlog-springboot-v2
git commit -m "build: scaffold backend v2 module"
```

### 任务 2：建立 API 响应与异常基线

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/ApiResponse.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiErrorCode.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiException.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/GlobalExceptionHandler.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/error/GlobalExceptionHandlerTest.java`

- [ ] **步骤 1：先写会失败的 API 错误测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/error/GlobalExceptionHandlerTest.java`：

```java
package com.aurora.myblog.v2.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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

- [ ] **步骤 2：运行 API 错误测试，确认它先失败**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=GlobalExceptionHandlerTest
```

预期：失败，因为 `ApiException`、`ApiErrorCode` 和 `GlobalExceptionHandler` 还不存在。

- [ ] **步骤 3：创建 API 响应类型**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/ApiResponse.java`：

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

- [ ] **步骤 4：创建类型化 API 错误**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiErrorCode.java`：

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

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiException.java`：

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

- [ ] **步骤 5：增加全局异常处理**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/GlobalExceptionHandler.java`：

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

- [ ] **步骤 6：重新运行 API 错误测试**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=GlobalExceptionHandlerTest
```

预期：通过，校验错误和业务冲突错误都命中统一响应断言。

- [ ] **步骤 7：提交 API 基线**

```powershell
git add MyBlog-springboot-v2
git commit -m "feat: add backend v2 api error baseline"
```

### 任务 3：增加类型化 CORS 与公开端点配置

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/ApiCorsProperties.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/SecurityPublicEndpointProperties.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/config/BackendPropertiesTest.java`

- [ ] **步骤 1：先写会失败的配置绑定测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/config/BackendPropertiesTest.java`：

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

- [ ] **步骤 2：运行配置绑定测试，确认它先失败**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=BackendPropertiesTest
```

预期：失败，因为两个 properties record 还不存在。

- [ ] **步骤 3：创建类型化配置 record**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/ApiCorsProperties.java`：

```java
package com.aurora.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("myblog.cors")
public record ApiCorsProperties(List<String> allowedOrigins) {
}
```

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/config/SecurityPublicEndpointProperties.java`：

```java
package com.aurora.myblog.v2.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("myblog.security")
public record SecurityPublicEndpointProperties(List<String> publicEndpoints) {
}
```

- [ ] **步骤 4：重新运行 properties 测试**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=BackendPropertiesTest
```

预期：通过，测试 profile 中的 CORS 和公开端点配置都能绑定。

- [ ] **步骤 5：提交类型化配置基线**

```powershell
git add MyBlog-springboot-v2
git commit -m "feat: add backend v2 typed security config"
```

### 任务 4：落实默认拒绝的 HTTP 安全基线

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityConfig.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityProbeController.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/SecurityConfigTest.java`

- [ ] **步骤 1：先写会失败的安全策略测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/security/SecurityConfigTest.java`：

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

- [ ] **步骤 2：运行安全测试，确认它先失败**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=SecurityConfigTest
```

预期：失败，因为安全配置和探针 Controller 还不存在。

- [ ] **步骤 3：增加安全探针**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityProbeController.java`：

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

- [ ] **步骤 4：增加基于 `SecurityFilterChain` 的安全配置**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/security/SecurityConfig.java`：

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

这一阶段保留 Spring Security 的 CSRF 默认行为。后续认证专项必须先决定最终浏览器/API 契约是 Cookie 会话、Bearer Token，还是明确拆分两种交互方式，然后再进入会改变状态的前端联调。

- [ ] **步骤 5：重新运行安全测试**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=SecurityConfigTest
```

预期：通过。公开探针返回 200，未列入公开配置的后台探针返回 401。

- [ ] **步骤 6：提交默认拒绝安全基线**

```powershell
git add MyBlog-springboot-v2
git commit -m "feat: default deny backend v2 endpoints"
```

### 任务 5：增加 Flyway 迁移冒烟覆盖

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/resources/db/migration/V1__create_v2_schema_marker.sql`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/infrastructure/persistence/FlywayMigrationTest.java`

- [ ] **步骤 1：先写会失败的迁移测试**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/infrastructure/persistence/FlywayMigrationTest.java`：

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

- [ ] **步骤 2：运行迁移测试，确认它先失败**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=FlywayMigrationTest
```

预期：失败，因为 Flyway 还没有创建 `v2_schema_marker`。

- [ ] **步骤 3：增加 Flyway 基线迁移**

创建 `MyBlog-springboot-v2/src/main/resources/db/migration/V1__create_v2_schema_marker.sql`：

```sql
create table v2_schema_marker (
    id bigint primary key,
    description varchar(128) not null
);

insert into v2_schema_marker (id, description)
values (1, 'myblog v2 migration baseline');
```

- [ ] **步骤 4：重新运行迁移测试**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=FlywayMigrationTest
```

预期：通过。Flyway 会在 H2 测试库上应用 `V1__create_v2_schema_marker.sql`。

- [ ] **步骤 5：提交迁移基线**

```powershell
git add MyBlog-springboot-v2
git commit -m "feat: add backend v2 migration baseline"
```

### 任务 6：增加模块化单体架构规则

**文件：**

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/package-info.java`

- 新建：`MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/infrastructure/persistence/package-info.java`

- 新建：`MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/ArchitectureRulesTest.java`

- [ ] **步骤 1：先给初始架构边界增加包标记**

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/package-info.java`：

```java
/**
 * Business modules live below this package.
 */
package com.aurora.myblog.v2.modules;
```

创建 `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/infrastructure/persistence/package-info.java`：

```java
/**
 * Persistence adapters and migration-facing support live below this package.
 */
package com.aurora.myblog.v2.infrastructure.persistence;
```

- [ ] **步骤 2：写架构规则**

创建 `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/ArchitectureRulesTest.java`：

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

- [ ] **步骤 3：运行架构测试**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=ArchitectureRulesTest
```

预期：通过。首批边界规则在第一个业务域迁移前就已经可执行。

- [ ] **步骤 4：提交架构守护规则**

```powershell
git add MyBlog-springboot-v2
git commit -m "test: guard backend v2 module boundaries"
```

### 任务 7：把 foundation 作为整体验证

**文件：**

- 验证：`MyBlog-springboot-v2/**`

- [ ] **步骤 1：运行 V2 后端完整测试**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml test
```

预期：上下文、API 错误、类型化配置、安全、Flyway 和架构测试全部通过。

- [ ] **步骤 2：构建可运行产物**

运行：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

预期：构建通过，并在 `MyBlog-springboot-v2/target/` 下生成后端产物。

- [ ] **步骤 3：记录下一份计划的交接范围**

下一次计划阶段再创建 `docs/superpowers/plans/2026-05-22-java-backend-v2-security-capabilities.zh-CN.md`。下一份计划应该覆盖：

- 认证模型与 JWT / Session 决策。

- `identity` 模块迁移。

- 安全错误 JSON 集成。

- 上传校验。

- 日志脱敏。

- XSS 清洗。

- 限流失败策略。

- [ ] **步骤 4：只有验证或交接文档发生变化时才提交**

如果整体验证后没有新增改动，不要创建空提交。如果写了交接说明，只暂存那份说明并单独提交。

## 执行注意

- 真正实施本计划前，应先切到独立 V2 分支或 worktree，不要在承担线上发布职责的分支上落地。
- 本计划期间保持 `MyBlog-springboot` 不动。它是旧业务行为和数据模型迁移依据。
- 不要把旧 `WebSecurityConfig`、`ResultVO`、`ControllerAdviceHandler`、`FileUtil` 或 `HTMLUtil` 复制到 V2。它们当前的行为是重构证据，不是 V2 模板。
- 在 V2 API 契约专项明确每一个公开资源前，公开端点列表必须保持小而显式。
