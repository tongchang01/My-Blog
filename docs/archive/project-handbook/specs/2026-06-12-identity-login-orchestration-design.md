# Identity 双 Token 登录编排与 Controller 设计

> **后续执行要求**：实现前先编写分步计划，并按 TDD、独立中文提交和小批次变更逐步落地。

## 1. 目标

完成 identity 后台登录最小纵向切片，把已经存在的能力接入一个可调用的 HTTP 登录接口：

- 使用可信客户端 IP 和规范化用户名执行 Caffeine 前置限流。
- 调用领域凭据校验器完成账号读取、账号类型、持久化锁定和 BCrypt 校验。
- 对密码错误累计 Caffeine 失败次数，同时保留数据库失败累计和锁定。
- 登录成功时更新审计状态、持久化 refresh token 并签发 access token。
- 提供 `POST /api/auth/login`，返回统一响应结构和双 token。
- 纠正当前 `ApiResponse` 实现与冻结 API 契约不一致的问题。

本设计不实现 refresh、logout、当前用户资料、修改密码、验证码、Redis、多实例限流或前端 token 存储策略。refresh token 的签发、轮换和撤销底层能力已经存在，本轮只在登录成功流程中调用签发能力。

## 2. 前置能力

本轮复用以下现有组件，不重复建设：

| 能力 | 现有组件 |
|---|---|
| 账号读取、类型和锁定判断 | `LoginCredentialVerifier` / `UserAccountRepository` |
| BCrypt 校验 | `PasswordHashVerifier` |
| 数据库失败累计与持久化锁定 | `LoginStateRecorder.recordPasswordFailure(...)` |
| 登录成功审计 | `LoginStateRecorder.recordSuccessfulLogin(...)` |
| 进程内登录限流 | `LoginRateLimiter` |
| access token 签发 | `AccessTokenIssuer` |
| refresh token 签发 | `RefreshTokenService.issue(...)` |
| 可信客户端 IP | `ClientIpResolver` |
| 应用统一时钟 | `Clock` |

## 3. 方案选择

### 3.1 采用方案：无事务外层编排 + 短成功事务

登录调用链拆成两个应用层组件：

1. `AuthApplicationService`
   - 不标事务。
   - 负责用户名规范化、限流检查、凭据校验和领域结果映射。
   - BCrypt、账号读取和失败分支不进入长事务。
2. `LoginSuccessTransactionService`
   - 标注 `@Transactional`。
   - 只处理已验证凭据后的成功状态更新、refresh token 持久化和 access token 签发。

这样可以避免把 BCrypt 计算和失败请求包在数据库事务中，同时保证成功审计与 refresh token 记录原子提交。

### 3.2 未采用方案：整个登录方法使用单一事务

该方案实现简单，但事务会覆盖账号查询、持久化锁定判断和 BCrypt：

- 密码计算期间持续占用数据库事务。
- 高频错误登录会产生大量无意义长事务。
- 限流和 HTTP 结果映射与持久化边界混在一起。

首版没有收益足以抵消这些成本，因此不采用。

### 3.3 未采用方案：先提交审计和 refresh token，再在事务外签发 access token

access token 签发通常不会失败，但密钥、算法或运行时异常仍可能发生。若数据库事务已提交而签发失败，会留下客户端从未收到的有效 refresh token 记录。

本设计把 access token 签发放在成功事务末尾。签发异常会使成功审计和 refresh token 写入一起回滚，调用方不会收到任何 token。

## 4. API 响应契约纠偏

当前 `ApiResponse` 仍使用早期结构：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

这与 `rules/api-response.md` 已冻结的契约冲突。本轮在新增首个业务 Controller 前统一纠正为：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {}
}
```

`ApiResponse<T>` 固定为：

```java
public record ApiResponse<T>(String code, String msg, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>("00000", "success", data);
    }

    public static ApiResponse<Void> fail(String code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }
}
```

约束：

- 删除 `success` 字段，不保留兼容别名。
- 成功码固定为字符串 `00000`。
- 消息字段固定为 `msg`，不使用 `message`。
- 同步修正依赖旧字段名和旧成功码的现有测试。
- 这是冻结契约纠偏，不是登录模块私有响应格式。

## 5. HTTP 契约

### 5.1 请求

```http
POST /api/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "plain-text-over-https"
}
```

`LoginRequest`：

```java
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过64个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 128, message = "密码长度不能超过128个字符")
        String password
) {
}
```

限制密码请求长度是为了阻止异常大请求进入 BCrypt。接口只允许通过 HTTPS 暴露；后端不记录请求体、明文密码或密码摘要。

### 5.2 成功响应

HTTP 200：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "accessToken": "<jwt>",
    "refreshToken": "<opaque-random-token>",
    "accessExpiresIn": 900,
    "refreshExpiresIn": 604800
  }
}
```

`LoginTokenVO` 字段：

- `accessToken`：JWT access token。
- `refreshToken`：只在本次签发时返回的 refresh token 明文。
- `accessExpiresIn`：access token 配置 TTL，单位秒。
- `refreshExpiresIn`：refresh token 配置 TTL，单位秒。

响应不返回账号主键、密码状态、失败次数、锁定截止时间或 token version。

### 5.3 失败响应

| 场景 | HTTP | code | 说明 |
|---|---:|---|---|
| 参数无效 | 400 | `90001` | Bean Validation 或请求体格式错误 |
| 用户名不存在 | 401 | `10001` | 与其他凭据失败统一 |
| GUEST 尝试登录 | 401 | `10001` | 不暴露账号类型 |
| 密码错误 | 401 | `10001` | 记录对应失败状态 |
| 账号仍在持久化锁定期 | 401 | `10001` | 不暴露账号存在和锁定状态 |
| Caffeine 冷却命中 | 429 | `90002` | 不查询账号、不执行 BCrypt |
| 审计、refresh token 或签发异常 | 500 | `99999` | 不返回任何 token |

本设计以防止账号枚举为优先，明确将锁定结果映射为 `401 + 10001`。该规则增量修正 `arch/auth-flow.md` 中旧的“锁定返回 403”描述；实现完成后同步更新权威流程文档。

## 6. 用户名规范化

Controller 只执行 Bean Validation，不自行修改请求值。`AuthApplicationService` 统一规范化：

```java
String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
```

同一个规范化结果必须同时传给：

- `LoginRateLimiter`
- `LoginCredentialVerifier`

这样可以确保限流键和数据库账号查询使用相同语义。密码不得 trim、转码或改变大小写。

## 7. 应用层组件

### 7.1 `LoginCommand`

位置：`identity.application.auth`

```java
public record LoginCommand(
        String username,
        String password,
        String clientIp
) {
}
```

该对象承载已经通过 web 参数校验的输入。`clientIp` 来自 `ClientIpResolver`，允许为 `null`。

### 7.2 `LoginTokenResult`

位置：`identity.application.auth`

```java
public record LoginTokenResult(
        String accessToken,
        String refreshToken,
        long accessExpiresIn,
        long refreshExpiresIn
) {
}
```

应用结果不引用 web VO。过期秒数来自 `SecurityJwtProperties`，不根据序列化时刻重新计算。

### 7.3 `AuthApplicationService`

位置：`identity.application.auth`

职责：

- 规范化用户名。
- 前置检查 Caffeine 限流。
- 调用 `LoginCredentialVerifier`。
- 将领域结果映射为稳定 API 错误码。
- 成功时清除当前 IP + username 的 Caffeine 失败状态。
- 调用短事务成功服务。

固定流程：

```text
1. 规范化 username
2. LoginRateLimiter.isBlocked(clientIp, username)
3. blocked -> 抛 RATE_LIMITED
4. LoginCredentialVerifier.verify(username, password, now)
5. BadCredentials -> recordFailure -> 抛 BAD_CREDENTIALS
6. Locked -> 抛 BAD_CREDENTIALS，不修改 Caffeine
7. Authenticated -> reset Caffeine
8. LoginSuccessTransactionService.complete(account, clientIp, now)
9. 返回 LoginTokenResult
```

`AuthApplicationService` 可以依赖 `ApiException` 和 `ApiErrorCode`，因为它位于应用边界，负责把领域结果转换为 API 用例结果。domain 类型仍不得依赖 HTTP 或通用错误码。

### 7.4 Caffeine 重置时机

凭据验证成功后、进入数据库成功事务前立即执行：

```java
loginRateLimiter.reset(clientIp, normalizedUsername);
```

即使后续审计、refresh token 或 access token 失败，也不恢复 Caffeine 失败计数。理由：

- 本次凭据已经证明正确，继续把该组合视为密码攻击不符合语义。
- Caffeine 是尽力型进程内防刷，不参与数据库事务。
- 数据库事务失败仍不会向客户端返回 token。

## 8. 成功事务

### 8.1 `LoginSuccessTransactionService`

位置：`identity.application.auth`

该服务是独立 Spring Bean，公开方法标注 `@Transactional`，避免同类内部调用导致事务代理失效。

事务内固定顺序：

1. `LoginStateRecorder.recordSuccessfulLogin(account.id(), loggedInAt, clientIp)`
2. `RefreshTokenService.issue(account.id())`
3. `AccessTokenIssuer.issueAccessToken(...)`
4. 组装 `LoginTokenResult`

access token 参数：

- `userId`：`String.valueOf(account.id())`
- `username`：领域账号中的规范化用户名
- `roles`：只包含 `account.type().name()`，即 `ADMIN` 或 `DEMO`
- `tokenVersion`：同一次凭据查询得到的 `account.tokenVersion()`

任一步骤抛出运行时异常，事务整体回滚：

- 成功审计不提交。
- refresh token 记录不提交。
- Controller 不返回 access token 或 refresh token。

### 8.2 并发状态边界

成功审计 SQL 已再次检查账号未删除且未被并发锁定。若凭据查询后账号状态变化，`LoginStateRecorder` 抛出 `LoginStateUpdateException`，事务终止。

本轮不在成功事务中再次查询 token version。账号的 `token_version` 只有改密、全端退出或强制撤销时变化，而这些用例尚未与登录并发规则形成更强锁定协议。首版接受极小并发窗口：

- 若登录读取旧版本后同时发生撤销，可能签发旧 `ver` 的 access token。
- 后续请求会因持久化版本不一致而立即失效。
- refresh token 流程仍会重新校验当前账号状态和版本。

不为消除这个短窗口增加账号行锁。

## 9. Web 层

### 9.1 `AuthController`

位置：`identity.web`

Controller 只负责：

1. 接收并校验 `LoginRequest`。
2. 通过 `ClientIpResolver` 读取可信客户端 IP。
3. 构建 `LoginCommand`。
4. 调用 `AuthApplicationService.login(...)`。
5. 将应用结果转换为 `LoginTokenVO` 并包装 `ApiResponse.ok(...)`。

Controller 不执行：

- 用户名规范化。
- BCrypt。
- 账号查询。
- token 签发。
- 事务控制。
- 错误码分支判断。

接口使用：

```java
@PostMapping("/api/auth/login")
```

并通过 OpenAPI 中文注解描述用途和字段。

### 9.2 白名单

在 `application.yml` 增加精确方法与路径：

```yaml
myblog:
  security:
    public-endpoints:
      - method: POST
        path: /api/auth/login
```

保留已有 `GET /actuator/health`。不得使用 `/api/auth/**` 整段通配，以免未来错误公开本应受保护的认证管理接口。

## 10. 日志与敏感数据

- 不记录明文密码、密码摘要、access token、refresh token 或完整登录请求体。
- 普通坏凭据、锁定和限流命中属于预期结果，不逐次记录 warn/error。
- 未预期异常继续由 `GlobalExceptionHandler` 记录堆栈并向客户端返回通用 `99999`。
- 登录成功首版不新增业务日志；审计事实保存在 `t_user_auth.last_login_at` 和 `last_login_ip`。
- 异常消息不得包含用户名与客户端 IP 的组合限流键。

## 11. 测试策略

### 11.1 响应契约测试

- 成功响应只包含 `code/msg/data`。
- 成功码为字符串 `00000`。
- 失败响应 `data=null`。
- JSON 中不存在 `success` 和 `message` 字段。
- `GlobalExceptionHandlerTest` 全部分支同步验证新字段名。

### 11.2 应用编排单元测试

- 用户名按 trim + `Locale.ROOT` 小写规范化。
- 命中限流时不调用凭据校验器。
- `BadCredentials` 记录一次 Caffeine 失败并抛 `10001`。
- `Locked` 映射为 `10001`，不记录新的 Caffeine 失败。
- `Authenticated` 清除 Caffeine 状态并调用成功事务。
- 成功事务异常继续向上传播，不伪装为凭据错误。

### 11.3 成功事务集成测试

- 成功登录更新审计并清理数据库失败状态。
- 数据库只保存 refresh token SHA-256，不保存明文。
- access token 包含正确的 user id、username、role 和 token version。
- ADMIN 与 DEMO 分别签发对应角色。
- 成功审计失败时不保存 refresh token。
- refresh token 保存失败或 access token 签发失败时成功审计回滚。

使用 H2 + Flyway 验证事务回滚。现有 Testcontainers MySQL 测试策略保持不变，Docker 不可用时允许既有两个 MySQL 测试跳过。

### 11.4 Controller 测试

- 合法请求返回 HTTP 200、`00000` 和完整双 token 字段。
- 空用户名、空密码、超长用户名和超长密码返回 400 + `90001`。
- 第 5 次错误仍返回 401 + `10001`。
- 第 6 次请求返回 429 + `90002`，补齐上一轮限流计划唯一未完成验收项。
- 锁定账号返回 401 + `10001`。
- 登录接口可匿名访问。
- 未加入白名单的受保护接口仍需认证。
- 可信代理与直连请求通过现有 `ClientIpResolver` 测试，不在 Controller 重复测试解析算法。

### 11.5 架构与规则扫描

- web 不依赖 Mapper、Repository 实现或基础设施 token 类。
- domain 不依赖 Spring、Servlet、HTTP、`ApiErrorCode` 或 Caffeine。
- SQL 仍只存在于 XML Mapper，不新增 SQL 注解。
- 新增 Java 类型和非显然逻辑使用中文注释。
- DTO 使用 record 和 Bean Validation，不编写无意义 getter/setter。

## 12. 文档同步

实现完成后同步：

- `status.md`：identity 登录最小纵向切片完成情况和真实测试基线。
- `roadmap.md`：勾选 identity 登录、双 token 和限流接入部分，不提前宣告 refresh/logout Controller 完成。
- `arch/auth-flow.md`：
  - 修正锁定账号对外映射为 `401 + 10001`。
  - 固化短成功事务顺序。
  - 删除 refresh token 是否轮换的未决描述，因为现有实现已经采用轮换。
- `rules/api-response.md` / `arch/request-flow.md`：确保示例字段统一使用 `code/msg/data`。
- `api-contract/auth.md`：首次落地登录接口契约。

## 13. 提交拆分

本轮实现拆成四个代码批次和一个结果同步批次，每批独立完成 RED、GREEN 和回归：

1. `纠正统一API响应契约`
   - 只修改 `ApiResponse`、异常响应及相关测试。
2. `实现双Token登录事务编排`
   - 新增应用命令、结果、外层编排和短成功事务。
3. `实现后台登录接口`
   - 新增请求、响应、Controller、白名单和 web 测试。
4. `补齐登录事务与HTTP集成测试`
   - 验证回滚、token claims、审计以及第 6 次返回 429。
5. `同步后台登录实施结果`
   - 更新状态、路线、认证流程和 API 契约文档。

不得把以上批次合并为一次数百文件变更。若某批次实际变更范围明显扩大，必须先继续拆分再实施。

## 14. 验收标准

- `POST /api/auth/login` 可匿名调用。
- 成功响应严格使用 `code/msg/data`，成功码为 `00000`。
- ADMIN 和 DEMO 可以获得 access token 与 refresh token。
- GUEST、未知账号、错误密码和持久化锁定统一返回 `401 + 10001`。
- 同 IP + username 第 5 次失败返回 `10001`，第 6 次返回 `429 + 90002`。
- 限流命中时不查询账号、不执行 BCrypt。
- 登录成功更新审计、清理失败状态并清除 Caffeine 计数。
- refresh token 明文只返回一次，数据库只保存 SHA-256。
- 成功审计、refresh token 写入或 access token 签发任一步失败时，不提交成功状态和 refresh token。
- access token 携带正确的 user id、username、role 和 token version。
- 不记录密码和 token。
- 不新增 SQL 注解、Flyway 迁移、Redis、验证码、refresh/logout Controller。
- 全量测试、ArchUnit、规则扫描和 `git diff --check` 通过。

## 15. 实施结果

实施日期：2026-06-12 至 2026-06-13。

代码批次：

| 提交 | 内容 |
|---|---|
| `2245a79` | 纠正统一API响应契约 |
| `e368c0e` | 实现双Token登录事务编排 |
| `30fd4a1` | 实现后台双Token登录接口 |
| `3baf968` | 补齐后台登录事务与接口验收 |

最终实现与本设计一致：

- `POST /api/auth/login` 已按精确 method + path 白名单开放。
- 响应固定为 `code/msg/data`，成功码为 `00000`。
- ADMIN / DEMO 成功登录后获得 access token 和 refresh token。
- 未知账号、GUEST、密码错误和持久化锁定统一映射为 `401 + 10001`。
- 同一 IP + 规范化用户名第 1 至第 5 次失败返回 `10001`，第 6 次返回 `429 + 90002`。
- 成功短事务按审计、refresh token、access token 顺序执行；access token 签发失败时数据库变更整体回滚。
- refresh token 明文只在响应中返回，数据库仅保存 SHA-256。
- application 层新增 ArchUnit 规则，禁止依赖 Servlet API。

最终执行 `mvn clean test`：169 tests，0 failures，0 errors，2 skipped。跳过项为 Docker 不可用时的既有 `MySqlFlywayMigrationTest` 和 `MySqlLoginFailureConcurrencyTest`。

本轮没有实现 refresh / logout / 当前用户资料 Controller，也没有新增 Entity、SQL 注解、Flyway 迁移、Redis 或验证码。
