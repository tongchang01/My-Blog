# 后端 V2 密码文章访问流程实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐个实现。步骤使用 checkbox（`- [ ]`）语法跟踪状态。

**目标：** 在后端 V2 的 `content` 业务域中补齐密码文章校验和受保护文章详情读取能力。

**架构：** 继续沿用当前 `content` 分层：API 负责 HTTP 契约，application 编排密码校验和详情读取，domain 定义访问令牌与文章访问检查模型，infrastructure 只读旧表 `t_article`。本阶段不引入 Redis，不改真实 MySQL 表结构，校验成功后返回短期签名访问令牌，详情接口携带令牌后才允许读取 `status = 2` 的密码文章。

**技术栈：** Java 17, Spring Boot 3.5, JdbcTemplate, JUnit 5, AssertJ, MockMvc, H2, MySQL local smoke.

---

## 背景与边界

旧后端密码文章流程：

```text
POST /articles/access
GET /articles/{articleId}
```

旧实现中，`POST /articles/access` 校验文章密码，校验成功后把访问状态写入 Redis；之后 `GET /articles/{articleId}` 再从 Redis 判断当前用户是否已经解锁文章。

V2 本阶段不引入 Redis，原因：

- 当前后端 V2 还处于业务迁移早期，Redis 应该等浏览量、热门文章、缓存、限流等能力一起规划。
- 密码文章访问是轻量读者端能力，可以用短期签名访问令牌先完成业务闭环。
- 使用签名访问令牌不会改表，不需要服务端保存状态，也方便前端保存到内存或 sessionStorage。

V2 新接口建议：

```text
POST /api/articles/{articleId}/access
GET /api/articles/{articleId}
```

访问规则：

- `status = 1` 公开文章：不需要访问令牌，保持当前行为。
- `status = 2` 密码文章：默认 `GET /api/articles/{articleId}` 返回 `403 FORBIDDEN`。
- 密码文章校验成功后，`POST /api/articles/{articleId}/access` 返回 `accessToken`。
- 再次读取详情时，通过请求头 `X-Article-Access-Token` 携带 `accessToken`。
- `status = 3` 草稿、`is_delete = 1` 删除文章、缺失文章：继续返回 `404 NOT_FOUND`。
- 密码文章不进入文章列表、分类文章、标签文章、归档、置顶推荐。
- 本阶段不做浏览量 Redis 统计、不做登录用户绑定、不做永久解锁。

## 文件结构

### 修改生产代码

- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleAccessCheck.java`
  - 新增领域模型，表达文章是否存在、状态、密码和删除状态。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleAccessToken.java`
  - 新增领域模型，表达访问令牌和过期时间。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleAccessTokenService.java`
  - 新增领域端口，签发和校验密码文章访问令牌。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/SignedArticleAccessTokenService.java`
  - 新增 HMAC-SHA256 签名实现，复用 `myblog.security.jwt.secret` 作为签名密钥。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleReader.java`
  - 扩展文章读取端口，增加访问检查和受保护详情读取方法。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/DatabaseArticleReader.java`
  - 新增读取 `status = 2` 密码文章详情的 SQL，保留公开列表过滤规则。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/application/ContentQueryService.java`
  - 增加密码校验、访问令牌签发、带令牌读取详情流程。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArticleAccessRequest.java`
  - 新增请求 DTO。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArticleAccessResponse.java`
  - 新增响应 DTO。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ContentArticleController.java`
  - 新增 `POST /api/articles/{articleId}/access`，并让详情接口读取 `X-Article-Access-Token`。
- `MyBlog-springboot-v2/src/main/resources/application.yml`
  - 新增公开端点 `/api/articles/*/access`。
- `MyBlog-springboot-v2/src/main/resources/application-local.yml`
  - 新增公开端点 `/api/articles/*/access`。
- `MyBlog-springboot-v2/src/test/resources/application-test.yml`
  - 新增公开端点 `/api/articles/*/access`。

### 修改测试代码

- `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`
  - 给 `id = 3` 的密码文章补充测试密码。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/SignedArticleAccessTokenServiceTest.java`
  - 新增访问令牌签名、校验和篡改测试。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java`
  - 新增密码文章访问检查和受保护详情读取测试。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentArticleControllerTest.java`
  - 新增 API 测试。

## Task 1: 补充密码文章测试数据

**Files:**

- Modify: `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentArticleControllerTest.java`

- [ ] **Step 1: 给 H2 密码文章写入测试密码**

把 `t_article` 插入语句的字段从：

```sql
article_content, is_top, is_featured, is_delete, status, type, create_time, update_time
```

改为：

```sql
article_content, is_top, is_featured, is_delete, status, type, password, create_time, update_time
```

把五条文章数据改为：

```sql
values
    (1, 1, 1, '/cover/java-1.png', '后端V2第一篇', '摘要一', '正文一', 1, 1, 0, 1, 1, null, timestamp '2026-05-28 10:00:00', timestamp '2026-05-28 10:00:00'),
    (2, 1, 2, '/cover/life-1.png', '生活记录第一篇', '摘要二', '正文二', 0, 1, 0, 1, 1, null, timestamp '2026-04-20 11:00:00', timestamp '2026-04-20 11:00:00'),
    (3, 1, 1, '/cover/protected.png', '密码文章', '不应出现在公开列表', '密码正文', 1, 1, 0, 2, 1, 'open-sesame', timestamp '2026-03-18 12:00:00', timestamp '2026-03-18 12:00:00'),
    (4, 1, 1, '/cover/draft.png', '草稿文章', '不应出现在第一阶段', '草稿正文', 1, 1, 0, 3, 1, null, timestamp '2026-02-16 13:00:00', timestamp '2026-02-16 13:00:00'),
    (5, 1, 2, '/cover/deleted.png', '已删除文章', '不应出现在第一阶段', '删除正文', 1, 1, 1, 1, 1, null, timestamp '2026-01-14 14:00:00', timestamp '2026-01-14 14:00:00');
```

- [ ] **Step 2: 运行迁移测试**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=FlywayMigrationTest'
```

Expected:

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 3: 运行现有内容测试**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 4: 提交**

```powershell
git add MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql
git commit -m "补充后端V2密码文章测试数据"
```

## Task 2: 新增文章访问令牌服务

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleAccessToken.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleAccessTokenService.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/SignedArticleAccessTokenService.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/SignedArticleAccessTokenServiceTest.java`

- [ ] **Step 1: 写失败测试**

Create `SignedArticleAccessTokenServiceTest.java`:

```java
package com.aurora.myblog.v2.modules.content;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import com.aurora.myblog.v2.modules.content.infrastructure.SignedArticleAccessTokenService;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SignedArticleAccessTokenServiceTest {

    private final SignedArticleAccessTokenService service = new SignedArticleAccessTokenService(
            new SecurityJwtProperties("myblog-v2-test", "test-secret-test-secret-test-secret-123456", Duration.ofMinutes(15)));

    @Test
    void issuesAndVerifiesArticleAccessToken() {
        var token = service.issue(3);

        assertThat(token.value()).isNotBlank();
        assertThat(token.expiresAt()).isNotNull();
        assertThat(service.verify(3, token.value())).isTrue();
    }

    @Test
    void rejectsTokenForDifferentArticle() {
        var token = service.issue(3);

        assertThat(service.verify(4, token.value())).isFalse();
    }

    @Test
    void rejectsTamperedToken() {
        var token = service.issue(3);

        assertThat(service.verify(3, token.value() + "x")).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=SignedArticleAccessTokenServiceTest'
```

Expected:

```text
Compilation failure: cannot find symbol SignedArticleAccessTokenService
```

- [ ] **Step 3: 新增领域模型和端口**

Create `ArticleAccessToken.java`:

```java
package com.aurora.myblog.v2.modules.content.domain;

import java.time.Instant;

public record ArticleAccessToken(String value, Instant expiresAt) {
}
```

Create `ArticleAccessTokenService.java`:

```java
package com.aurora.myblog.v2.modules.content.domain;

public interface ArticleAccessTokenService {

    ArticleAccessToken issue(int articleId);

    boolean verify(int articleId, String token);
}
```

- [ ] **Step 4: 实现签名访问令牌服务**

Create `SignedArticleAccessTokenService.java`:

```java
package com.aurora.myblog.v2.modules.content.infrastructure;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import com.aurora.myblog.v2.modules.content.domain.ArticleAccessToken;
import com.aurora.myblog.v2.modules.content.domain.ArticleAccessTokenService;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class SignedArticleAccessTokenService implements ArticleAccessTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);

    private final byte[] secret;

    public SignedArticleAccessTokenService(SecurityJwtProperties jwtProperties) {
        this.secret = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ArticleAccessToken issue(int articleId) {
        Instant expiresAt = Instant.now().plus(ACCESS_TOKEN_TTL);
        String payload = articleId + ":" + expiresAt.getEpochSecond();
        String signature = sign(payload);
        return new ArticleAccessToken(encode(payload) + "." + signature, expiresAt);
    }

    @Override
    public boolean verify(int articleId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) {
            return false;
        }
        String payload = decode(parts[0]);
        String[] payloadParts = payload.split(":", -1);
        if (payloadParts.length != 2) {
            return false;
        }
        try {
            int tokenArticleId = Integer.parseInt(payloadParts[0]);
            Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(payloadParts[1]));
            return tokenArticleId == articleId
                    && Instant.now().isBefore(expiresAt)
                    && MessageDigestSafeEquals.equals(sign(payload), parts[1]);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign article access token", ex);
        }
    }

    private String encode(String payload) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static final class MessageDigestSafeEquals {

        private MessageDigestSafeEquals() {
        }

        static boolean equals(String expected, String actual) {
            byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
            byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
            if (expectedBytes.length != actualBytes.length) {
                return false;
            }
            int result = 0;
            for (int i = 0; i < expectedBytes.length; i++) {
                result |= expectedBytes[i] ^ actualBytes[i];
            }
            return result == 0;
        }
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=SignedArticleAccessTokenServiceTest'
```

Expected:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleAccessToken.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleAccessTokenService.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/SignedArticleAccessTokenService.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/SignedArticleAccessTokenServiceTest.java
git commit -m "新增后端V2文章访问令牌服务"
```

## Task 3: 新增密码校验接口

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleAccessCheck.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArticleAccessRequest.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArticleAccessResponse.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/DatabaseArticleReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/application/ContentQueryService.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ContentArticleController.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentArticleControllerTest.java`

- [ ] **Step 1: 写失败测试**

在 `DatabaseArticleReaderTest` 追加：

```java
@Test
void findsArticleAccessCheckForProtectedArticle() {
    var check = reader.findArticleAccessCheckById(3);

    assertThat(check).isPresent();
    assertThat(check.get().id()).isEqualTo(3);
    assertThat(check.get().protectedArticle()).isTrue();
    assertThat(check.get().password()).isEqualTo("open-sesame");
}
```

在 `ContentArticleControllerTest` 追加：

```java
@Test
void returnsAccessTokenForCorrectProtectedArticlePassword() throws Exception {
    mockMvc.perform(post("/api/articles/3/access")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"password":"open-sesame"}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.articleId").value(3))
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
}
```

同时给 `ContentArticleControllerTest` 增加 imports：

```java
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

Expected:

```text
Compilation failure: cannot find symbol findArticleAccessCheckById
```

- [ ] **Step 3: 新增访问检查模型和 reader 端口**

Create `ArticleAccessCheck.java`:

```java
package com.aurora.myblog.v2.modules.content.domain;

public record ArticleAccessCheck(int id, int status, boolean deleted, String password) {

    public boolean publicArticle() {
        return !deleted && status == 1;
    }

    public boolean protectedArticle() {
        return !deleted && status == 2;
    }
}
```

Modify `ArticleReader.java`:

```java
Optional<ArticleAccessCheck> findArticleAccessCheckById(int articleId);
```

- [ ] **Step 4: 实现数据库访问检查**

在 `DatabaseArticleReader` 新增 import：

```java
import com.aurora.myblog.v2.modules.content.domain.ArticleAccessCheck;
```

新增方法：

```java
@Override
public Optional<ArticleAccessCheck> findArticleAccessCheckById(int articleId) {
    List<ArticleAccessCheck> checks = jdbcTemplate.query("""
                    select a.id,
                           a.status,
                           a.is_delete,
                           a.password
                    from t_article a
                    where a.id = ?
                    """,
            (rs, rowNum) -> new ArticleAccessCheck(
                    rs.getInt("id"),
                    rs.getInt("status"),
                    rs.getInt("is_delete") == 1,
                    rs.getString("password")),
            articleId);
    return checks.stream().findFirst();
}
```

- [ ] **Step 5: 新增 API 请求和响应 DTO**

Create `ArticleAccessRequest.java`:

```java
package com.aurora.myblog.v2.modules.content.api;

import jakarta.validation.constraints.NotBlank;

public record ArticleAccessRequest(@NotBlank(message = "password must not be blank") String password) {
}
```

Create `ArticleAccessResponse.java`:

```java
package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.ArticleAccessToken;

import java.time.Instant;

public record ArticleAccessResponse(int articleId, String accessToken, Instant expiresAt) {

    static ArticleAccessResponse from(int articleId, ArticleAccessToken token) {
        return new ArticleAccessResponse(articleId, token.value(), token.expiresAt());
    }
}
```

- [ ] **Step 6: 实现应用服务密码校验**

在 `ContentQueryService` 新增字段和构造参数：

```java
private final ArticleAccessTokenService articleAccessTokenService;

public ContentQueryService(ContentCatalogReader catalogReader,
                           ArticleReader articleReader,
                           ArticleAccessTokenService articleAccessTokenService) {
    this.catalogReader = catalogReader;
    this.articleReader = articleReader;
    this.articleAccessTokenService = articleAccessTokenService;
}
```

新增 import：

```java
import com.aurora.myblog.v2.modules.content.domain.ArticleAccessToken;
import com.aurora.myblog.v2.modules.content.domain.ArticleAccessTokenService;
```

新增方法：

```java
public ArticleAccessToken accessProtectedArticle(int articleId, String password) {
    var check = articleReader.findArticleAccessCheckById(articleId)
            .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
    if (!check.protectedArticle()) {
        throw new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在");
    }
    if (!Objects.equals(check.password(), password)) {
        throw new ApiException(ApiErrorCode.FORBIDDEN, "文章访问密码错误");
    }
    return articleAccessTokenService.issue(articleId);
}
```

同时新增 import：

```java
import java.util.Objects;
```

- [ ] **Step 7: 新增 Controller 接口**

在 `ContentArticleController` 新增 import：

```java
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
```

新增方法：

```java
@PostMapping("/api/articles/{articleId}/access")
public ApiResponse<ArticleAccessResponse> accessArticle(
        @PathVariable int articleId,
        @Valid @RequestBody ArticleAccessRequest request) {
    return ApiResponse.ok(ArticleAccessResponse.from(
            articleId,
            contentQueryService.accessProtectedArticle(articleId, request.password())));
}
```

- [ ] **Step 8: 新增公开端点配置**

三个配置文件都加入：

```yaml
- /api/articles/*/access
```

文件：

```text
MyBlog-springboot-v2/src/main/resources/application.yml
MyBlog-springboot-v2/src/main/resources/application-local.yml
MyBlog-springboot-v2/src/test/resources/application-test.yml
```

- [ ] **Step 9: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 10: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/main/resources/application.yml MyBlog-springboot-v2/src/main/resources/application-local.yml MyBlog-springboot-v2/src/test/resources/application-test.yml
git commit -m "新增后端V2密码文章访问校验"
```

## Task 4: 支持带访问令牌读取密码文章详情

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/DatabaseArticleReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/application/ContentQueryService.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ContentArticleController.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentArticleControllerTest.java`

- [ ] **Step 1: 写失败测试**

在 `DatabaseArticleReaderTest` 追加：

```java
@Test
void findsAccessibleProtectedArticleDetail() {
    var article = reader.findAccessibleArticleById(3);

    assertThat(article).isPresent();
    assertThat(article.get().id()).isEqualTo(3);
    assertThat(article.get().content()).isEqualTo("密码正文");
}
```

在 `ContentArticleControllerTest` 追加：

```java
@Test
void rejectsProtectedArticleDetailWithoutAccessToken() throws Exception {
    mockMvc.perform(get("/api/articles/3"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
}

@Test
void returnsProtectedArticleDetailWithAccessToken() throws Exception {
    String response = mockMvc.perform(post("/api/articles/3/access")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"password":"open-sesame"}
                            """))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String accessToken = JsonPath.read(response, "$.data.accessToken");

    mockMvc.perform(get("/api/articles/3").header("X-Article-Access-Token", accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(3))
            .andExpect(jsonPath("$.data.content").value("密码正文"));
}
```

同时给 `ContentArticleControllerTest` 增加 import：

```java
import com.jayway.jsonpath.JsonPath;
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

Expected:

```text
Compilation failure: cannot find symbol findAccessibleArticleById
```

- [ ] **Step 3: 扩展 reader 端口**

Modify `ArticleReader.java`:

```java
Optional<ArticleDetail> findAccessibleArticleById(int articleId);
```

- [ ] **Step 4: 实现受保护详情读取**

把 `DatabaseArticleReader.findPublishedArticleById` 中的 SQL 提取为私有方法：

```java
private Optional<ArticleDetail> findArticleByIdAndStatuses(int articleId, List<Integer> statuses) {
    String statusPlaceholders = String.join(",", statuses.stream().map(status -> "?").toList());
    List<Object> args = new ArrayList<>();
    args.add(articleId);
    args.addAll(statuses);
    List<ArticleDetailRow> rows = jdbcTemplate.query("""
                    select a.id,
                           a.article_title,
                           a.article_abstract,
                           a.article_content,
                           a.article_cover,
                           a.is_top,
                           a.is_featured,
                           a.create_time,
                           a.update_time,
                           c.id as category_id,
                           c.category_name,
                           u.id as author_id,
                           u.nickname,
                           u.avatar,
                           t.id as tag_id,
                           t.tag_name
                    from t_article a
                    join t_category c on c.id = a.category_id
                    join t_user_info u on u.id = a.user_id
                    left join t_article_tag at on at.article_id = a.id
                    left join t_tag t on t.id = at.tag_id
                    where a.id = ?
                      and a.is_delete = 0
                      and a.status in (%s)
                    order by t.id asc
                    """.formatted(statusPlaceholders),
            (rs, rowNum) -> new ArticleDetailRow(
                    rs.getInt("id"),
                    rs.getString("article_title"),
                    rs.getString("article_abstract"),
                    rs.getString("article_content"),
                    rs.getString("article_cover"),
                    rs.getInt("is_top") == 1,
                    rs.getInt("is_featured") == 1,
                    toLocalDateTime(rs.getTimestamp("create_time")),
                    toLocalDateTime(rs.getTimestamp("update_time")),
                    rs.getInt("category_id"),
                    rs.getString("category_name"),
                    rs.getInt("author_id"),
                    rs.getString("nickname"),
                    rs.getString("avatar"),
                    (Integer) rs.getObject("tag_id"),
                    rs.getString("tag_name")),
            args.toArray());
    return toArticleDetail(rows);
}
```

然后改成：

```java
@Override
public Optional<ArticleDetail> findPublishedArticleById(int articleId) {
    return findArticleByIdAndStatuses(articleId, List.of(1));
}

@Override
public Optional<ArticleDetail> findAccessibleArticleById(int articleId) {
    return findArticleByIdAndStatuses(articleId, List.of(1, 2));
}
```

- [ ] **Step 5: 修改应用服务读取流程**

把 `ContentQueryService.getArticleDetail` 改为：

```java
public ArticleDetail getArticleDetail(int articleId, String accessToken) {
    var check = articleReader.findArticleAccessCheckById(articleId)
            .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
    if (check.publicArticle()) {
        return articleReader.findPublishedArticleById(articleId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
    }
    if (!check.protectedArticle()) {
        throw new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在");
    }
    if (!articleAccessTokenService.verify(articleId, accessToken)) {
        throw new ApiException(ApiErrorCode.FORBIDDEN, "文章访问令牌无效");
    }
    return articleReader.findAccessibleArticleById(articleId)
            .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
}
```

- [ ] **Step 6: 修改 Controller 读取请求头**

把 `getArticleDetail` 改为：

```java
@GetMapping("/api/articles/{articleId}")
public ApiResponse<ArticleDetailResponse> getArticleDetail(
        @PathVariable int articleId,
        @RequestHeader(value = "X-Article-Access-Token", required = false) String accessToken) {
    return ApiResponse.ok(ArticleDetailResponse.from(contentQueryService.getArticleDetail(articleId, accessToken)));
}
```

新增 import：

```java
import org.springframework.web.bind.annotation.RequestHeader;
```

- [ ] **Step 7: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 8: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content
git commit -m "支持后端V2密码文章详情读取"
```

## Task 5: 全量验证和计划状态同步

**Files:**

- Modify: `docs/superpowers/plans/2026-05-29-backend-v2-protected-article-access.zh-CN.md`

- [ ] **Step 1: 全量测试**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 2: 打包**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: 本地 MySQL 只读检查**

不要把本地密码写进文件。先在当前 PowerShell 会话中设置 `MYSQL_PWD`，命令只读取环境变量：

```powershell
if (-not $env:MYSQL_PWD) { throw 'MYSQL_PWD is required' }
mysql -h localhost -P 3306 -u root -N -e "select id, status, is_delete, password is not null from aurora.t_article where status = 2 limit 5;"
```

Expected:

```text
SQL 执行成功；如果本地库存在密码文章，应至少看到一行 status = 2。
```

- [ ] **Step 4: 本地 API 冒烟**

启动服务时通过环境变量注入数据库密码：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
if (-not $env:MYBLOG_DATASOURCE_PASSWORD) { throw 'MYBLOG_DATASOURCE_PASSWORD is required' }
mvn -f MyBlog-springboot-v2/pom.xml spring-boot:run
```

调用：

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/articles/3/access' -ContentType 'application/json' -Body '{"password":"open-sesame"}'
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/articles/3'
```

Expected:

```text
第一个接口在 H2 或包含相同测试数据的环境中返回 success=true。
第二个接口不带 X-Article-Access-Token 时返回 403。
```

真实 MySQL 数据不一定存在 `id = 3` 且密码为 `open-sesame` 的文章；如果没有，API 冒烟只记录“本地库缺少测试密码文章，跳过真实密码校验冒烟”，不能伪造通过。

- [ ] **Step 5: 更新本计划的执行结果**

先读取实际提交记录：

```powershell
git log --oneline -5
```

再在本文档末尾追加真实执行结果。提交记录必须复制实际短 SHA 和提交信息。

- [ ] **Step 6: 提交**

```powershell
git add docs/superpowers/plans/2026-05-29-backend-v2-protected-article-access.zh-CN.md
git commit -m "同步后端V2密码文章计划状态"
```

## 自检结果

- 覆盖范围：已覆盖测试数据、访问令牌、密码校验接口、带访问令牌读取密码文章详情、公开端点配置和完整验证。
- 边界控制：不引入 Redis，不改真实表结构，不把密码文章加入列表、归档、推荐，不做浏览量统计。
- 类型一致性：`ArticleAccessToken`、`ArticleAccessCheck`、`ArticleAccessTokenService`、`ArticleReader` 方法、DTO 和 Controller 方法命名一致。
- 占位扫描：文档没有待替换占位词；本地数据库密码只通过环境变量读取，不能写入代码、文档或 Git。
