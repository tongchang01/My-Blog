# 身份认证 Refresh 与全端退出实施计划

> **状态：已实施（2026-06-13）**
>
> 批次提交：`2bf15d9`、`f37ec25`、`67a84f7`、`a6084b2`；文档同步由包含本次修改的提交记录。全量验证：198 tests、0 failures、0 errors、2 skipped。

**目标：** 在现有管理员身份认证基础上，完成 refresh token 轮换和全端退出，使 access token、refresh token、账号状态与 `token_version` 的失效语义一致。

**架构：** refresh 接口采用“无事务编排服务 + 独立事务执行服务”。事务执行服务对旧 refresh token 加行锁，在同一事务内校验账号、撤销旧 token、签发新 refresh token 和 access token。logout 复用现有全端撤销能力，在同一事务内递增 `token_version` 并撤销该账号全部 refresh token。

**技术栈：** Java 17、Spring Boot 3.5、Spring Security、MyBatis、MySQL、H2、JUnit 5、Mockito、AssertJ。

**设计依据：** `docs/project-handbook/specs/2026-06-13-identity-refresh-logout-design.md`

---

## 0. 实施约束

1. 仅处理后台 `ADMIN`、`DEMO` 的认证会话，不恢复前台用户体系。
2. refresh token 只允许从 JSON 请求体读取，不引入 Cookie、CSRF、Redis。
3. 前端只依赖业务 `code`；无效、过期、已撤销、重放、账号删除、账号锁定和账号类型不允许刷新，统一返回 HTTP 401、业务码 `10002`。
4. refresh token、Authorization 请求头和 JWT 原文不得写入日志。
5. Mapper SQL 只写在 XML 中，Java 注解中不得出现 SQL。
6. 新增和修改的公开类型、关键事务逻辑、并发约束必须有中文注释。
7. DTO、实体和简单值对象优先使用 Lombok；领域不可变值对象继续使用 `record`。
8. 不新增数据库表和迁移脚本。
9. 每批提交前运行该批测试；第四批必须运行全量测试。
10. 提交信息使用中文，并保持每次提交只表达一个可回退的目的。

## 1. 批次与提交边界

| 批次 | 目标 | 建议提交信息 |
| --- | --- | --- |
| 1 | 补齐可刷新账号查询 | `补齐可刷新账号查询` |
| 2 | 实现 refresh 会话事务 | `实现refresh会话事务` |
| 3 | 暴露 refresh 与 logout 接口 | `实现refresh与全端退出接口` |
| 4 | 补齐端到端和并发验收 | `补齐认证会话集成验收` |
| 5 | 同步实施结果和项目状态 | `同步refresh与退出实施结果` |

---

## 2. 批次一：补齐可刷新账号查询

### 2.1 文件范围

**新增：**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/RefreshableAccount.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account/RefreshableAccountRepository.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisRefreshableAccountRepository.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence/DatabaseRefreshableAccountRepositoryTest.java`

**修改：**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserAccountMapper.java`
- `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml`

### 2.2 先写失败测试

新增 `DatabaseRefreshableAccountRepositoryTest`，至少覆盖：

1. `ADMIN` 且未删除、未锁定时能够查询。
2. `DEMO` 且未删除、未锁定时能够查询。
3. `GUEST` 无法查询。
4. 已删除账号无法查询。
5. `locked_until` 晚于当前时间时无法查询。
6. `locked_until` 等于或早于当前时间时能够查询。
7. 返回值包含 `id`、`username`、`type`、`tokenVersion`。

测试使用固定时间：

```java
private static final LocalDateTime NOW =
        LocalDateTime.of(2026, 6, 13, 10, 0);
```

先运行并确认测试因缺少领域端口或实现而失败：

```powershell
mvn -f .\MyBlog-springboot-v2\pom.xml -Dtest=DatabaseRefreshableAccountRepositoryTest test
```

### 2.3 新增领域模型

`RefreshableAccount.java`：

```java
package com.tyb.myblog.v2.identity.domain.account;

/**
 * 允许继续刷新认证会话的后台账号快照。
 */
public record RefreshableAccount(
        long id,
        String username,
        AccountType type,
        int tokenVersion
) {
}
```

`RefreshableAccountRepository.java`：

```java
package com.tyb.myblog.v2.identity.domain.account;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 查询可刷新认证会话的后台账号。
 */
public interface RefreshableAccountRepository {

    Optional<RefreshableAccount> findRefreshableById(
            long userId,
            LocalDateTime now
    );
}
```

### 2.4 扩展 Mapper

在 `UserAccountMapper.java` 增加方法，不使用 SQL 注解：

```java
UserAccountEntity selectRefreshableById(
        @Param("userId") long userId,
        @Param("now") LocalDateTime now
);
```

在 `UserAccountMapper.xml` 增加查询：

```xml
<select id="selectRefreshableById"
        resultType="com.tyb.myblog.v2.identity.infrastructure.persistence.entity.UserAccountEntity">
    SELECT
        id,
        username,
        type,
        token_version
    FROM t_user_auth
    WHERE id = #{userId}
      AND deleted = 0
      AND type IN (1, 2)
      AND (locked_until IS NULL OR locked_until &lt;= #{now})
</select>
```

账号类型按项目既有数据库编码映射，测试应锁定 `1=ADMIN`、`2=DEMO`，不得在适配器内复制另一套编码规则。

### 2.5 新增持久化适配器

`MyBatisRefreshableAccountRepository.java`：

```java
package com.tyb.myblog.v2.identity.infrastructure.persistence.repository;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccount;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccountRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.mapper.UserAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 基于 MyBatis 的可刷新账号查询实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisRefreshableAccountRepository
        implements RefreshableAccountRepository {

    private final UserAccountMapper userAccountMapper;

    @Override
    public Optional<RefreshableAccount> findRefreshableById(
            long userId,
            LocalDateTime now
    ) {
        return Optional.ofNullable(
                        userAccountMapper.selectRefreshableById(userId, now)
                )
                .map(entity -> new RefreshableAccount(
                        entity.getId(),
                        entity.getUsername(),
                        AccountType.fromDatabaseValue(entity.getType()),
                        entity.getTokenVersion()
                ));
    }
}
```

若现有 `AccountType` 的工厂方法名称不同，应复用已有方法，不新增同义转换入口。

### 2.6 验证与提交

```powershell
mvn -f .\MyBlog-springboot-v2\pom.xml -Dtest=DatabaseRefreshableAccountRepositoryTest test
rg -n "@(Select|Insert|Update|Delete)" MyBlog-springboot-v2/src/main/java
git diff --check
git status --short
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/account MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence MyBlog-springboot-v2/src/main/resources/mapper/identity/UserAccountMapper.xml MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/infrastructure/persistence
git commit -m "补齐可刷新账号查询"
```

完成标准：

- 查询条件全部由 SQL 保证。
- 领域层不知道 MyBatis 实体。
- Java 文件不存在 SQL 注解。
- 本批测试通过且工作区不夹带其他批次文件。

---

## 3. 批次二：实现 Refresh 会话事务

### 3.1 文件范围

**新增：**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/RefreshSessionApplicationService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/RefreshSessionTransactionService.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/RefreshSessionApplicationServiceTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/RefreshSessionTransactionServiceTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/RefreshSessionTransactionIntegrationTest.java`

**修改：**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/token/RefreshTokenService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth/UserTokenVersionRepository.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/UserTokenVersionMapper.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/repository/MyBatisUserTokenVersionRepository.java`
- `MyBlog-springboot-v2/src/main/resources/mapper/identity/UserTokenVersionMapper.xml`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/RefreshTokenServiceTest.java`

### 3.2 先收缩 RefreshTokenService

先修改 `RefreshTokenServiceTest`，删除旧 `rotate` 行为测试，改为覆盖：

1. `issue(userId)` 保存哈希而不是原始 token。
2. `findActiveForUpdate(rawToken, now)` 使用原始 token 的哈希查询。
3. `revoke(tokenId)` 按 refresh token 主键撤销。
4. 查询不到 token 时返回空值，不抛出数据库细节异常。

目标公开方法：

```java
public IssuedRefreshToken issue(long userId);

public Optional<RefreshTokenRecord> findActiveForUpdate(
        String rawToken,
        LocalDateTime now
);

public boolean revoke(long tokenId);
```

`findActiveForUpdate` 和 `revoke(long)` 不声明独立事务，由外层
`RefreshSessionTransactionService` 统一控制锁和写入。

删除：

- `RefreshTokenService.rotate(...)`
- `RefreshTokenService.revoke(String rawToken)`
- `RefreshTokenService` 对 `UserTokenVersionRepository` 的依赖
- `UserTokenVersionRepository.findRefreshableTokenVersion(...)`
- 对应 Mapper 方法、XML 查询和适配器实现

执行：

```powershell
mvn -f .\MyBlog-springboot-v2\pom.xml -Dtest=RefreshTokenServiceTest test
```

先确认修改后的测试失败，再完成服务收缩。

### 3.3 先写应用服务测试

`RefreshSessionApplicationServiceTest` 覆盖：

1. 空 token、空白 token 直接抛出 `ApiException`，业务码 `10002`。
2. 事务服务返回结果时原样返回。
3. 事务服务返回空值时抛出 `ApiException`，业务码 `10002`。
4. 异常信息不包含原始 refresh token。

`RefreshSessionTransactionServiceTest` 覆盖：

1. 查不到旧 token 时返回空值，后续依赖不被调用。
2. 账号不可刷新时撤销旧 token 并返回空值。
3. 撤销旧 token 失败时不签发任何新 token。
4. 成功时严格按“锁定旧 token、查询账号、撤销旧 token、签发新 refresh、签发 access”执行。
5. 成功结果复用 `LoginTokenResult`，字段与登录一致。
6. access token 使用刷新时读取到的最新 `tokenVersion`。

运行并确认缺少实现：

```powershell
mvn -f .\MyBlog-springboot-v2\pom.xml "-Dtest=RefreshSessionApplicationServiceTest,RefreshSessionTransactionServiceTest" test
```

### 3.4 实现无事务编排服务

`RefreshSessionApplicationService.java`：

```java
package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 处理 refresh 请求参数和统一失败语义。
 */
@Service
@RequiredArgsConstructor
public class RefreshSessionApplicationService {

    private final RefreshSessionTransactionService transactionService;

    public LoginTokenResult refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
        return transactionService.refresh(refreshToken)
                .orElseThrow(() ->
                        new ApiException(ApiErrorCode.INVALID_TOKEN));
    }
}
```

### 3.5 实现独立事务服务

`RefreshSessionTransactionService.java`：

```java
package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccount;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccountRepository;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 在同一事务内完成 refresh token 轮换。
 */
@Service
@RequiredArgsConstructor
public class RefreshSessionTransactionService {

    private final RefreshTokenService refreshTokenService;
    private final RefreshableAccountRepository accountRepository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final SecurityJwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public Optional<LoginTokenResult> refresh(String rawRefreshToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<RefreshTokenRecord> tokenOptional =
                refreshTokenService.findActiveForUpdate(
                        rawRefreshToken,
                        now
                );
        if (tokenOptional.isEmpty()) {
            return Optional.empty();
        }

        RefreshTokenRecord token = tokenOptional.get();
        Optional<RefreshableAccount> accountOptional =
                accountRepository.findRefreshableById(
                        token.userId(),
                        now
                );
        if (accountOptional.isEmpty()) {
            refreshTokenService.revoke(token.id());
            return Optional.empty();
        }

        if (!refreshTokenService.revoke(token.id())) {
            return Optional.empty();
        }

        RefreshableAccount account = accountOptional.get();
        IssuedRefreshToken refreshToken =
                refreshTokenService.issue(account.id());
        var accessToken =
                accessTokenIssuer.issueAccessToken(
                        String.valueOf(account.id()),
                        account.username(),
                        java.util.List.of(account.type().name()),
                        account.tokenVersion()
                );

        return Optional.of(new LoginTokenResult(
                accessToken.accessToken(),
                refreshToken.token(),
                jwtProperties.accessTokenTtl().toSeconds(),
                jwtProperties.refreshTokenTtl().toSeconds()
        ));
    }
}
```

该实现直接复用现有 `AccessTokenIssuer`、`IssuedRefreshToken`、
`SecurityJwtProperties` 和 `LoginTokenResult`，不得新建重复结果类型。

### 3.6 验证事务回滚

`RefreshSessionTransactionIntegrationTest` 使用真实数据库仓储和测试替身
`AccessTokenIssuer`，至少覆盖：

1. 正常刷新后旧 token 已撤销，新 token 已保存。
2. JWT 签发抛出运行时异常时，旧 token 的撤销回滚。
3. JWT 签发抛出运行时异常时，新 refresh token 的插入回滚。
4. 回滚后使用同一个旧 refresh token 再次刷新能够成功。
5. 账号不可刷新时旧 token 被撤销，且不生成新 token。

测试替身通过测试配置提供 `@Primary` Bean，不修改生产代码。

运行：

```powershell
mvn -f .\MyBlog-springboot-v2\pom.xml "-Dtest=RefreshTokenServiceTest,RefreshSessionApplicationServiceTest,RefreshSessionTransactionServiceTest,RefreshSessionTransactionIntegrationTest" test
```

### 3.7 验证与提交

```powershell
rg -n "rotate|findRefreshableTokenVersion" MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
rg -n "refreshToken|Authorization" MyBlog-springboot-v2/src/main/java | rg "log\."
rg -n "@(Select|Insert|Update|Delete)" MyBlog-springboot-v2/src/main/java
git diff --check
git status --short
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/domain/auth MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence MyBlog-springboot-v2/src/main/resources/mapper/identity MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application
git commit -m "实现refresh会话事务"
```

完成标准：

- 旧 `rotate` 和旧账号版本查询全部移除。
- refresh 轮换只有一个事务边界。
- JWT 签发失败不会消耗旧 refresh token。
- 原始 token 不进入日志或异常消息。

---

## 4. 批次三：实现 Refresh 与全端退出接口

### 4.1 文件范围

**新增：**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/RefreshTokenRequest.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/LogoutApplicationService.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/LogoutApplicationServiceTest.java`

**修改：**

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/AuthController.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java`
- `MyBlog-springboot-v2/src/main/resources/application.yml`
- `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- `MyBlog-springboot-v2/src/main/resources/application-test.yml`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/AuthControllerTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/BackendPropertiesTest.java`

### 4.2 先写 Logout 应用服务测试

覆盖：

1. 合法正整数用户 ID 调用
   `UserTokenRevocationService.revokeAll(userId, userId)`。
2. `null`、空白、非数字、零和负数 ID 统一抛出业务码 `10002`。
3. `revokeAll` 返回 `false` 时抛出业务码 `10002`。

实现：

```java
package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.application.token.UserTokenRevocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 执行当前账号的全端退出。
 */
@Service
@RequiredArgsConstructor
public class LogoutApplicationService {

    private final UserTokenRevocationService revocationService;

    public void logout(String principalId) {
        long userId = parsePositiveUserId(principalId);
        if (!revocationService.revokeAll(userId, userId)) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }

    private long parsePositiveUserId(String principalId) {
        try {
            long userId = Long.parseLong(principalId);
            if (userId <= 0) {
                throw new NumberFormatException();
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new ApiException(ApiErrorCode.INVALID_TOKEN);
        }
    }
}
```

运行：

```powershell
mvn -f .\MyBlog-springboot-v2\pom.xml -Dtest=LogoutApplicationServiceTest test
```

### 4.3 新增请求 DTO

`RefreshTokenRequest.java` 与现有请求 DTO 一样使用不可变 `record`：

```java
package com.tyb.myblog.v2.identity.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新认证会话请求。
 */
public record RefreshTokenRequest(
        @NotBlank String refreshToken
) {
}
```

### 4.4 扩展 AuthController

新增端点：

```java
@PostMapping("/refresh")
public ApiResponse<LoginTokenVO> refresh(
        @Valid @RequestBody RefreshTokenRequest request
) {
    LoginTokenResult result =
            refreshSessionApplicationService.refresh(
                    request.refreshToken()
            );
    return ApiResponse.ok(new LoginTokenVO(
            result.accessToken(),
            result.refreshToken(),
            result.accessExpiresIn(),
            result.refreshExpiresIn()));
}

@PostMapping("/logout")
public ApiResponse<Void> logout(
        @CurrentUser AuthenticatedPrincipal principal
) {
    logoutApplicationService.logout(principal.id());
    return ApiResponse.ok(null);
}
```

构造器依赖新增：

- `RefreshSessionApplicationService`
- `LogoutApplicationService`

控制器测试覆盖：

1. `POST /api/auth/refresh` 把 JSON token 传给应用服务。
2. refresh 成功返回与登录相同的 `LoginTokenVO` 字段。
3. refresh 缺少字段或空白字段返回参数错误。
4. `POST /api/auth/logout` 把当前认证主体 ID 传给应用服务。
5. logout 成功返回 HTTP 200，`data` 为 `null`。

### 4.5 配置公开端点

仅将以下精确组合加入匿名白名单：

```text
POST /api/auth/login
POST /api/auth/refresh
```

`POST /api/auth/logout` 不加入白名单。

三个环境配置文件保持一致，`BackendPropertiesTest` 对三份配置分别断言。

`SecurityConfigTest` 覆盖：

1. 无 Authorization 的 `POST /api/auth/refresh` 能到达控制器。
2. 无 Authorization 的 `GET /api/auth/refresh` 返回 401。
3. 无 Authorization 的 `POST /api/auth/logout` 返回 401。
4. 携带有效后台 access token 的 logout 能到达控制器。

### 4.6 验证与提交

```powershell
mvn -f .\MyBlog-springboot-v2\pom.xml "-Dtest=LogoutApplicationServiceTest,AuthControllerTest,SecurityConfigTest,BackendPropertiesTest" test
rg -n "/api/auth/(login|refresh|logout)" MyBlog-springboot-v2/src/main/resources MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java
rg -n "@(Select|Insert|Update|Delete)" MyBlog-springboot-v2/src/main/java
git diff --check
git status --short
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/LogoutApplicationService.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java MyBlog-springboot-v2/src/main/resources/application.yml MyBlog-springboot-v2/src/main/resources/application-local.yml MyBlog-springboot-v2/src/main/resources/application-test.yml MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/application/auth/LogoutApplicationServiceTest.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/web/AuthControllerTest.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/config/BackendPropertiesTest.java
git commit -m "实现refresh与全端退出接口"
```

完成标准：

- refresh 匿名范围只开放 POST。
- logout 必须携带有效 access token。
- refresh 响应与登录响应结构一致。
- logout 不接受请求体中的用户 ID。

---

## 5. 批次四：补齐认证会话集成验收

### 5.1 文件范围

**新增：**

- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/integration/AuthSessionIntegrationTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/integration/RefreshSessionConcurrencyTest.java`

**按测试暴露问题最小修改：**

- 本轮前三批已涉及的认证会话文件

不得借集成测试扩大到其他业务模块或无关重构。

### 5.2 完整会话链路

`AuthSessionIntegrationTest` 使用真实 Spring 容器、Security 过滤链、
H2 数据库和 MockMvc，覆盖：

1. 管理员登录成功，获得 access token 和 refresh token。
2. 使用 refresh token 刷新成功，获得一对新 token。
3. 旧 refresh token 再次使用返回 HTTP 401、业务码 `10002`。
4. 新 access token 能访问 `/api/admin/security-probe`。
5. 使用新 access token 调用 logout 成功。
6. logout 后，新旧 access token 均无法通过持久化版本校验。
7. logout 后，该账号所有 refresh token 均无法刷新。
8. 一个账号 logout 不影响另一个账号的 access token 和 refresh token。
9. 已锁定账号的现存 refresh token 无法刷新。
10. 已删除账号的现存 refresh token 无法刷新。
11. `GUEST` 账号的现存 refresh token 无法刷新。

测试不得输出完整 token。失败信息只记录步骤名称、HTTP 状态和业务码。

### 5.3 并发重放验收

`RefreshSessionConcurrencyTest` 直接调用
`RefreshSessionApplicationService`，使用两个独立线程同时提交同一个旧
refresh token：

```java
ExecutorService executor = Executors.newFixedThreadPool(2);
CountDownLatch ready = new CountDownLatch(2);
CountDownLatch start = new CountDownLatch(1);
```

两个任务先递减 `ready`，等待 `start`；主线程确认两个任务都准备完成后
统一释放。断言：

1. 两个请求最多一个成功。
2. 失败请求统一得到业务码 `10002`。
3. 数据库只新增一个有效 refresh token。
4. 旧 refresh token 已撤销。
5. 成功返回的新 refresh token 能继续完成一次正常轮换。

每个线程必须通过 Spring 代理调用事务服务，不能在测试方法事务内直接
共享连接。测试类不要使用类级别 `@Transactional`。

### 5.4 静态规则检查

执行：

```powershell
rg -n "@(Select|Insert|Update|Delete)" MyBlog-springboot-v2/src/main/java
rg -n "refreshToken|Authorization" MyBlog-springboot-v2/src/main/java | rg "log\."
rg -n "findRefreshableTokenVersion|rotate\(" MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
rg -n "com\.aurora\.myblog" MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
```

预期：

- SQL 注解扫描无结果。
- token 日志扫描无结果。
- 已删除的旧刷新方法扫描无结果。
- 旧 `aurora` 包扫描无结果。

### 5.5 全量验证与提交

在工作树根目录执行：

```powershell
mvn -f .\MyBlog-springboot-v2\pom.xml "-Dtest=AuthSessionIntegrationTest,RefreshSessionConcurrencyTest" test
mvn -f .\MyBlog-springboot-v2\pom.xml clean test
git diff --check
git status --short
```

只有全量测试通过后提交：

```powershell
git add MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/integration MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
git commit -m "补齐认证会话集成验收"
```

提交前必须检查暂存区：

```powershell
git diff --cached --stat
git diff --cached --name-only
```

如暂存区包含非认证会话文件，先取消暂存，不得把无关修改带入本批。

完成标准：

- 登录、刷新、重放、退出、版本失效形成完整闭环。
- 同一个 refresh token 并发请求最多一次成功。
- 全量测试通过。
- 未引入 Docker 依赖。

---

## 6. 批次五：同步实施结果和项目状态

### 6.1 文件范围

根据现有文档内容修改：

- `docs/project-handbook/api-contract/auth.md`
- `docs/project-handbook/arch/auth-flow.md`
- `docs/project-handbook/status.md`
- `docs/project-handbook/roadmap.md`
- `docs/project-handbook/specs/2026-06-13-identity-refresh-logout-design.md`
- `docs/project-handbook/plans/2026-06-13-identity-refresh-logout-plan.md`

若真实文件名略有差异，先查找对应“认证接口契约”和“认证流程”文档，
只修改已有权威文档，不创建同义重复文档。

### 6.2 文档更新内容

认证接口契约应记录：

- `POST /api/auth/refresh` 请求、成功响应和统一失败码。
- `POST /api/auth/logout` 的 Bearer token 要求与成功响应。
- refresh token 只放 JSON，不使用 Cookie。
- 前端只根据业务码处理错误，不依赖中文 `msg` 做逻辑判断。

认证流程应记录：

- refresh 行锁和单事务轮换顺序。
- 账号删除、锁定、类型变化后的刷新行为。
- logout 的 `token_version + 1` 和 refresh token 全撤销。
- access token 保持无状态 JWT，但每次受保护请求继续校验持久化版本。

`status.md` 和 `roadmap.md` 应记录：

- 本轮五个批次的完成状态。
- 实际执行的全量测试结果。
- M3 身份认证会话能力已经完成的边界。
- 下一项业务模块及其开始条件。

设计和计划文档应将状态更新为“已实施”，并附五个真实提交哈希。

### 6.3 文档自检

```powershell
rg -n "待定|未决定|占位" docs/project-handbook
rg -n "cookie|Cookie|Redis|GUEST|token_version|10002" docs/project-handbook/api-contract docs/project-handbook/arch docs/project-handbook/specs docs/project-handbook/plans
git diff --check
git diff --stat
git status --short
```

逐项核对：

1. 文档中的端点、字段名和代码一致。
2. 文档没有声称恢复前台用户体系。
3. 文档没有声称采用 Cookie 或 Redis。
4. 文档没有把 `GUEST` 写成可刷新账号。
5. 测试数量和提交哈希来自真实命令结果。

### 6.4 提交

```powershell
git add docs/project-handbook
git diff --cached --stat
git diff --cached --name-only
git commit -m "同步refresh与退出实施结果"
```

完成标准：

- 文档与真实实现一致。
- 不改动无关 Markdown 格式。
- 路线图下一步可以直接用于拆分后续业务任务。

---

## 7. 最终验收清单

- [x] `POST /api/auth/refresh` 仅允许 JSON 请求体。
- [x] refresh 成功返回与登录一致的 token 字段。
- [x] refresh 所有安全失败统一为 HTTP 401、业务码 `10002`。
- [x] 同一个旧 refresh token 串行或并发均最多成功一次。
- [x] JWT 签发失败时整个轮换事务回滚。
- [x] 删除、锁定、`GUEST` 账号不能刷新。
- [x] `POST /api/auth/logout` 必须携带有效 access token。
- [x] logout 递增 `token_version` 并撤销账号全部 refresh token。
- [x] logout 不影响其他账号。
- [x] Java 注解中不存在 SQL。
- [x] 日志中不存在 token 原文。
- [x] 旧 `aurora` 包、旧 `rotate`、旧刷新版本查询均不存在。
- [x] `mvn -f .\MyBlog-springboot-v2\pom.xml clean test` 通过。
- [x] 五个批次分别提交，提交信息为中文。
- [x] 认证契约、流程、状态和路线图已同步。
