# Identity Refresh 与全端退出设计

> **状态：已实施（2026-06-13）**
>
> 实现提交：`2bf15d9`、`f37ec25`、`67a84f7`、`a6084b2`。全量验证：198 tests、0 failures、0 errors、2 skipped。

## 1. 目标

在现有后台双 token 登录基础上，闭合认证会话生命周期：

- 提供 `POST /api/auth/refresh`，使用 refresh token 轮换出新的 access token 与 refresh token。
- 提供 `POST /api/auth/logout`，使用有效 access token 执行全端退出。
- 保证同一枚旧 refresh token 并发刷新时最多一次成功。
- 保证 access token 签发失败时，refresh token 轮换整体回滚，旧 token 仍可重试。
- 保持统一 `code/msg/data` 响应、精确安全白名单和现有模块边界。

本轮不实现当前用户资料、`t_user_info`、修改密码、单设备会话、Cookie token、CSRF token、Redis、多实例 Caffeine 或前端 token 存储策略。

## 2. 已确认决策

### 2.1 Refresh token 传输

`POST /api/auth/refresh` 使用 JSON 请求体传递 refresh token：

```json
{
  "refreshToken": "<opaque-random-token>"
}
```

本轮不改为 HttpOnly Cookie，避免同时引入跨域 Cookie、SameSite、CSRF 和前端部署域名策略。

### 2.2 Logout 鉴权

`POST /api/auth/logout` 必须携带当前仍有效的 Bearer access token：

```http
Authorization: Bearer <access-token>
```

Controller 通过 `@CurrentUser AuthenticatedPrincipal` 读取当前用户，不接收客户端提供的 `userId`，也不要求提交 refresh token。

logout 不加入匿名白名单。旧 access token 在首次全端退出后立即失效，重复调用返回 `401 + 10002`。

### 2.3 退出范围

首版 logout 采用全端退出：

1. 递增当前用户的 `token_version`。
2. 撤销该用户全部未撤销 refresh token。
3. 两项操作在现有 `UserTokenRevocationService` 事务内原子完成。

不实现“只退出当前设备”。当前 schema 没有设备或 session family 标识，强行模拟单设备退出会产生不可靠语义。

### 2.4 Refresh 轮换

refresh 固定采用单次轮换：

- 旧 refresh token 成功消费后立即撤销。
- 每次成功刷新都返回新的 access token 和新的 refresh token。
- 同一旧 token 不能重复使用。
- 同一旧 token 并发请求时最多一个成功，其余返回 `401 + 10002`。

## 3. HTTP 契约

### 3.1 刷新会话

```http
POST /api/auth/refresh
Content-Type: application/json
```

该接口按 `POST + /api/auth/refresh` 精确加入匿名白名单。客户端不需要提供 access token，过期 access token 也不影响 refresh。

请求 DTO：

```java
public record RefreshTokenRequest(
        @NotBlank(message = "refresh token不能为空")
        @Size(max = 512, message = "refresh token长度不能超过512个字符")
        String refreshToken
) {
}
```

长度上限用于在摘要计算和数据库查询前拒绝异常大输入，不对合法 token 长度形成业务承诺。

成功响应 HTTP 200，与登录响应字段完全一致：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "accessToken": "<new-jwt>",
    "refreshToken": "<new-opaque-random-token>",
    "accessExpiresIn": 900,
    "refreshExpiresIn": 604800
  }
}
```

复用现有 `LoginTokenVO`，避免为相同结构新增重复 DTO。应用层可使用更通用的 `SessionTokenResult`，或在本轮小范围内继续复用 `LoginTokenResult`；实施计划应根据实际引用范围选择改名还是保留，禁止同时保留两个相同结果类型。

失败响应：

| 场景 | HTTP | code | 对外语义 |
|---|---:|---|---|
| 请求体为空、字段为空、超长或 JSON 非法 | 400 | `90001` | 参数校验失败 |
| token 不存在、过期、已撤销或重复消费 | 401 | `10002` | 登录状态已失效 |
| token 所属用户不存在、已删除、仍锁定或不是后台账号 | 401 | `10002` | 登录状态已失效 |
| 持久化或 access token 签发异常 | 500 | `99999` | 系统内部错误 |

所有 refresh 失败原因统一为 `10002`，不得暴露 token 是否存在、用户状态或并发消费结果。

### 3.2 全端退出

```http
POST /api/auth/logout
Authorization: Bearer <access-token>
```

请求无 Body。成功响应 HTTP 200：

```json
{
  "code": "00000",
  "msg": "success",
  "data": null
}
```

失败响应：

| 场景 | HTTP | code | 说明 |
|---|---:|---|---|
| access token 缺失、无效、过期或版本不一致 | 401 | `10002` | 请求不会进入 Controller |
| 当前用户在并发期间被删除，撤销事务未更新账号 | 401 | `10002` | 不暴露账号状态 |
| 未预期数据库异常 | 500 | `99999` | 不返回内部异常 |

logout 成功后：

- 当前及历史 access token 因 `token_version` 不一致全部失效。
- 当前用户全部 refresh token 被撤销。
- 再次使用旧 access token 调用 logout 返回 `401 + 10002`。
- 再次使用任一旧 refresh token 调用 refresh 返回 `401 + 10002`。

## 4. Refresh 架构

### 4.1 采用方案

使用无事务外层编排 + 独立刷新短事务：

```text
AuthController.refresh
        │
        ▼
RefreshSessionApplicationService
        │  无事务；映射无效会话错误
        ▼
RefreshSessionTransactionService
        │  @Transactional
        ├─ 锁定旧 refresh token
        ├─ 查询可刷新账号快照
        ├─ 撤销旧 refresh token
        ├─ 保存新 refresh token
        ├─ 签发新 access token
        └─ 返回新 token 对
```

外层服务只处理应用结果和稳定错误码。短事务服务负责所有必须原子完成的持久化状态变化与 access token 签发。

### 4.2 未采用方案

#### 直接扩展 `RefreshTokenService`

现有服务职责是 refresh token 的签发、轮换与撤销。如果继续加入账号完整快照查询、角色转换、access token 签发和 API 错误映射，会把 token 持久化能力变成认证总服务，不利于后续维护。

#### Controller 分步调用

Controller 依次调用轮换和 access token 签发虽然代码少，但旧 refresh token 可能已提交撤销，而 access token 签发随后失败。客户端既拿不到新会话，也无法重试旧 token，因此不采用。

## 5. 应用与领域组件

### 5.1 `RefreshSessionApplicationService`

位置：`identity.application.auth`

职责：

- 接收 refresh token 明文。
- 调用 `RefreshSessionTransactionService.refresh(...)`。
- 将空结果映射为 `ApiException(ApiErrorCode.INVALID_TOKEN)`。
- 不记录 token，不开启事务。

建议方法：

```java
public LoginTokenResult refresh(String rawRefreshToken)
```

输入已经通过 Bean Validation，但应用层仍不假设 token 有效。

### 5.2 `RefreshSessionTransactionService`

位置：`identity.application.auth`

独立 Spring Bean，公开方法标注 `@Transactional`：

```java
public Optional<LoginTokenResult> refresh(String rawRefreshToken)
```

固定事务顺序：

1. 计算旧 refresh token 的 SHA-256。
2. `SELECT ... FOR UPDATE` 查询未撤销且未过期记录。
3. 按 `userId` 查询可刷新账号快照。
4. 校验账号未删除、锁定已结束且类型为 ADMIN 或 DEMO。
5. 撤销旧 refresh token。
6. 生成并保存新 refresh token。
7. 使用账号最新 `tokenVersion` 签发 access token。
8. 组装 TTL 秒数并返回结果。

无效 token 或不可刷新账号返回 `Optional.empty()`。持久化异常、摘要算法异常和 JWT 签发异常原样抛出，使 Spring 回滚整个事务。

### 5.3 可刷新账号快照

新增只面向认证会话的领域查询结果：

```java
public record RefreshableAccount(
        long id,
        String username,
        AccountType type,
        int tokenVersion
) {
}
```

新增查询端口：

```java
public interface RefreshableAccountRepository {

    Optional<RefreshableAccount> findRefreshableById(
            long userId,
            LocalDateTime now);
}
```

持久化查询必须一次读取：

- `id`
- `username`
- `type`
- `token_version`

并在 SQL 中限制：

- `deleted = 0`
- `locked_until IS NULL OR locked_until <= now`
- `type IN (ADMIN, DEMO)` 对应的数据库值

不复用 `UserAccountRepository.findActiveByUsername(...)`，因为 refresh 已知 userId，不应反向查询用户名，也不需要读取密码哈希、失败次数等登录专属字段。

现有 `UserTokenVersionRepository.findRefreshableTokenVersion(...)` 在新端口落地后只保留确有调用者的能力。若仅被旧 `RefreshTokenService.rotate(...)` 使用，应在迁移调用后删除该冗余方法，而不是维持两套可刷新状态查询。

### 5.4 Refresh token 原子操作

现有 `RefreshTokenService.rotate(...)` 将账号版本校验嵌在 token 服务中，但无法返回签发 access token 所需的账号信息。本轮固定将其重构为由刷新事务显式编排的三个应用能力：

```java
public Optional<RefreshTokenRecord> findActiveForUpdate(
        String rawToken,
        LocalDateTime now);

public boolean revoke(long tokenId);

public IssuedRefreshToken issue(long userId);
```

- `findActiveForUpdate(...)` 负责摘要明文并调用仓储行锁查询，不自行开启事务；调用方必须处于刷新短事务中。
- `revoke(...)` 只撤销指定记录，不查用户状态。
- `issue(...)` 继续集中处理安全随机数、SHA-256 和新记录保存，并加入外层事务。
- 删除原有 `rotate(...)`，避免保留一条绕过账号快照和 access token 原子签发的旧路径。

该重构必须满足：

- SQL 仍位于 XML Mapper。
- token 摘要逻辑只有一个实现。
- 不把明文 token 写入领域持久化对象、日志或异常。
- 不通过同类内部调用依赖 `@Transactional` 生效。

## 6. Logout 架构

`AuthController.logout(...)`：

```java
@PostMapping("/logout")
public ApiResponse<Void> logout(
        @CurrentUser AuthenticatedPrincipal principal
) {
    logoutApplicationService.logout(principal.id());
    return ApiResponse.ok(null);
}
```

使用独立 `LogoutApplicationService` 封装 ID 解析和错误映射，使 Controller 继续只负责 HTTP 转换。

`LogoutApplicationService`：

- 接收 `principal.id()` 字符串，不依赖 web 或 Spring Security 身份类型。
- 严格解析正整数 userId；认证上下文异常统一映射 `10002`。
- 调用 `UserTokenRevocationService.revokeAll(userId, userId)`。
- 返回成功或抛 `INVALID_TOKEN`。

不允许客户端传入目标用户 ID。当前操作者与被撤销用户均为本人，因此 `updated_by` 使用当前 userId。

## 7. 事务与并发

### 7.1 Refresh 并发

旧 refresh token 通过 `SELECT ... FOR UPDATE` 加行锁。两个并发请求的结果必须是：

- 第一个请求锁定、撤销并提交新 token 对。
- 第二个请求获得锁后发现旧记录已撤销，返回 `401 + 10002`。

禁止两个请求都成功签发新 token 对。

### 7.2 Refresh 回滚

若新 refresh token 已保存，但 access token 签发失败：

- 旧 refresh token 撤销回滚。
- 新 refresh token 插入回滚。
- 客户端收到 `500 + 99999`。
- 客户端可使用原 refresh token 重试。

这也是将 access token 签发放入短事务末尾的主要原因。

### 7.3 Logout 原子性

`UserTokenRevocationService.revokeAll(...)` 已在一个事务内：

1. 递增 `token_version`。
2. 撤销全部 refresh token。

任一步抛出运行时异常时整体回滚，不允许只失效 access token 或只失效 refresh token。

### 7.4 Refresh 与 Logout 并发

不额外引入分布式锁。依赖数据库事务和最新账号版本：

- refresh 必须读取最新 `token_version` 签发 access token。
- logout 递增版本后，使用旧版本签发或已签发的 access token都会在后续请求校验时失效。
- logout 撤销全部 refresh token 后，已提交的旧 refresh token 不能再使用。

极端并发下 refresh 可能先提交一个新 token 对，随后 logout 立即将其全部撤销；这是全端退出的正确结果。

## 8. 安全配置

配置只新增：

```yaml
- method: POST
  path: /api/auth/refresh
```

该精确白名单同步到：

- `application.yml`
- `application-local.yml`
- `application-test.yml`

不得新增 `/api/auth/**` 通配白名单。`POST /api/auth/logout` 不配置为公开端点，由现有 JWT 过滤器保护。

## 9. 日志与敏感数据

- 不记录 refresh token、token hash、access token 或完整请求体。
- 无效、过期、撤销和并发重复 refresh 属于预期认证失败，不逐次记录 warn/error。
- logout 成功不新增普通业务日志，撤销事实由数据库状态体现。
- 未预期异常由 `GlobalExceptionHandler` 记录堆栈，返回通用 `99999`。
- 错误消息不得包含 token 片段、hash、用户 ID 与账号状态组合。

## 10. 测试策略

### 10.1 应用层单元测试

`RefreshSessionApplicationServiceTest`：

- 短事务返回 token 结果时原样返回。
- 短事务返回空时抛 `INVALID_TOKEN`。
- 短事务异常原样传播，不伪装为 token 无效。
- 类和公开方法不带长事务。

`LogoutApplicationServiceTest`：

- 使用当前用户 ID 同时作为目标用户和操作者调用全端撤销。
- 撤销成功时正常返回。
- 用户 ID 非法或撤销返回 false 时抛 `INVALID_TOKEN`。
- 不接受请求中的任意目标 userId。

### 10.2 Refresh 事务集成测试

使用 H2 + Flyway + 测试专用 `@Primary AccessTokenIssuer` 验证：

- 有效 token 被撤销，并保存一枚新 refresh token。
- 新 token hash 与返回明文匹配，数据库不保存明文。
- access token 参数包含最新 userId、username、ADMIN/DEMO role 和 token version。
- 过期、已撤销、未知用户、已删除用户、锁定用户和 GUEST 返回空。
- access token 签发失败时旧 token 仍有效，新 token 不落库。
- 同一旧 token 并发刷新最多一次成功。

### 10.3 Web 与完整 HTTP 测试

- refresh 可匿名访问。
- 空、超长和非法 JSON 返回 `400 + 90001`。
- 有效 refresh 返回完整新 token 对。
- 旧 refresh token 随后返回 `401 + 10002`。
- 新 refresh token 可以继续轮换。
- 新 access token claims 使用最新 username、role、token version。
- logout 无 token 返回 `401 + 10002`。
- logout 有效 token 返回 `200 + 00000 + data:null`。
- logout 后原 access token 访问受保护接口返回 `401 + 10002`。
- logout 后该用户全部 refresh token 均无法刷新。
- 其他用户 token 不受影响。

### 10.4 架构与规则扫描

- web 不依赖 Mapper、Repository 实现或 identity infrastructure。
- application 不依赖 Servlet API。
- domain 不依赖 Spring、HTTP、Caffeine 或通用 API 错误码。
- SQL 仅位于 XML Mapper，不新增 SQL 注解。
- DTO 使用 record，依赖注入使用 Lombok 或现有简洁模式。
- Java 类型、字段与非显然逻辑保留中文注释。
- identity 不记录密码或 token。

## 11. 文档同步

实施完成后更新：

- `api-contract/auth.md`：将 refresh 和 logout 从“尚未开放”改为真实契约。
- `arch/auth-flow.md`：把目标流程更新为实际组件和事务顺序。
- `status.md`：记录认证会话生命周期闭合及真实测试基线。
- `roadmap.md`：identity 仍标记部分完成，剩余 `t_user_info`、当前用户资料和修改密码。
- 本设计与对应实施计划：记录真实提交 SHA 和测试结果。

## 12. 提交拆分

建议拆成四个代码批次和一个文档批次：

1. `补齐可刷新账号查询`
   - 新增账号快照、查询端口、XML SQL 与集成测试。
2. `实现refresh会话事务`
   - 新增刷新编排、短事务、原子轮换和回滚测试。
3. `实现refresh与全端退出接口`
   - 新增 DTO、Controller、精确白名单和 web 测试。
4. `补齐认证会话集成验收`
   - 覆盖完整 HTTP、并发刷新、logout 全端失效和架构规则。
5. `同步refresh与退出实施结果`
   - 更新契约、状态、路线和设计实施证据。

每批独立执行 RED、GREEN、定向回归、规则扫描和中文提交。不得合并为一次大提交。

## 13. 验收标准

- `POST /api/auth/refresh` 可匿名调用，只接受 JSON Body 中的 refresh token。
- refresh 成功返回新的 access token 和 refresh token，字段与登录响应一致。
- 旧 refresh token 单次消费，重复或并发使用统一返回 `401 + 10002`。
- refresh 重新读取 ADMIN/DEMO 账号的 username、role 和最新 token version。
- 用户删除、锁定、GUEST、过期或撤销统一返回 `401 + 10002`。
- access token 签发失败时整个轮换回滚，旧 refresh token 可重试。
- `POST /api/auth/logout` 必须携带有效 access token，不在匿名白名单。
- logout 使用当前认证用户执行全端退出，不接受客户端 userId。
- logout 原子递增 `token_version` 并撤销该用户全部 refresh token。
- logout 后历史 access token 和 refresh token 全部失效，其他用户不受影响。
- 不新增 Cookie、CSRF token、Redis、表、Flyway 迁移、SQL 注解或用户资料功能。
- 不记录 access token、refresh token、token hash、密码或密码摘要。
- 定向测试、并发测试、ArchUnit、全量 `mvn clean test` 和 `git diff --check` 通过。
