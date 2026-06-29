# Backend V2 修改密码设计

> **状态：已实施（2026-06-14）**
>
> 实施提交：`d69894f 补齐修改密码持久化能力`、`82a4f7f 实现修改密码事务`、`d51701b 开放当前用户修改密码接口`、`61caebc 补齐修改密码集成验收`。文档收尾由当前提交承载，不预写自身 SHA。

## 1. 目标

为当前已登录的 ADMIN 账号提供修改本人密码能力，并在修改成功后立即使该账号的全部 access token 和 refresh token 失效。

本轮只闭合 identity 模块的最后一个缺口，不实现找回密码、重置他人密码、密码历史、单设备会话、修改密码后自动续签 token 或前端页面。

## 2. 已确认决策

### 2.1 成功后的会话语义

修改密码成功后：

1. 更新 `t_user_auth.password_hash`。
2. 递增同一账号的 `token_version`。
3. 撤销同一账号全部 refresh token。
4. 接口返回成功，但不签发新 token。
5. 客户端收到成功响应后清理本地 token，并跳转登录页重新登录。

密码更新、token version 递增和 refresh token 撤销必须处于同一数据库事务。任一步失败时全部回滚。

### 2.2 权限

- ADMIN 可以修改本人密码。
- DEMO 不允许修改密码，返回 `403 + 10003`。
- 接口不接受客户端提供的用户 ID。
- 当前用户只从 `@CurrentUser AuthenticatedPrincipal` 获取。

### 2.3 密码规则

- 当前密码和新密码均按原样参与 BCrypt，不做 trim。
- 当前密码、新密码必填，最大长度均为 128。
- 新密码长度为 8 至 128 个字符。
- 新密码不能与当前密码相同。
- 本轮不增加复杂度规则，避免把无法解释的字符组合要求固化到后端。

## 3. HTTP 契约

```http
PUT /api/auth/me/password
Authorization: Bearer <access-token>
Content-Type: application/json
```

请求：

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password"
}
```

成功响应：

```json
{
  "code": "00000",
  "msg": "success",
  "data": null
}
```

失败响应：

| 场景 | HTTP | code | 对外语义 |
|---|---:|---|---|
| 请求体为空、字段缺失、长度非法或 JSON 非法 | 400 | `90001` | 参数校验失败 |
| 新密码与当前密码相同 | 400 | `90001` | 参数校验失败 |
| 当前密码错误 | 401 | `10001` | 用户名或密码错误 |
| access token 缺失、失效或主体 ID 非法 | 401 | `10002` | 登录状态已失效 |
| DEMO 调用 | 403 | `10003` | 无权执行当前操作 |
| 账号并发删除或持久化异常 | 500 | `99999` | 系统内部错误 |

当前密码错误复用 `BAD_CREDENTIALS`，不新增只服务一个接口的错误码。响应和日志不得包含明文密码、密码摘要或请求体。

## 4. 架构

采用单个事务服务完成加锁、校验、改密和全端失效：

```text
CurrentUserController.changePassword
        │
        ▼
ChangePasswordApplicationService
        │  @Transactional
        ├─ 校验 ADMIN 与主体 ID
        ├─ SELECT 当前账号 FOR UPDATE
        ├─ BCrypt 校验当前密码
        ├─ 拒绝新旧密码相同
        ├─ BCrypt 生成新摘要
        ├─ UPDATE password_hash + token_version + 1
        └─ 撤销全部 refresh token
```

不采用“先改密码，再调用现有 `UserTokenRevocationService`”的分步方案。该方案虽然表面复用更多，但事务所有权分散，难以证明密码写入与 refresh token 撤销始终原子。

不在成功后签发新 token。否则当前设备会成为全端失效规则的特殊例外，并增加响应模型和前端状态分支。

## 5. 组件设计

### 5.1 修改密码命令

位置：`identity.application.auth`

```java
public record ChangePasswordCommand(
        String currentPassword,
        String newPassword
) {
}
```

Web DTO 负责基础长度和必填校验，应用服务仍负责权限、主体、当前密码和新旧密码关系等业务校验。

### 5.2 密码摘要端口

现有 `PasswordHashVerifier` 只支持 `matches`。修改密码需要生成摘要，因此将端口扩展为：

```java
public interface PasswordHashService {

    boolean matches(String rawPassword, String passwordHash);

    String encode(String rawPassword);
}
```

`SpringPasswordHashService` 使用现有 Spring Security `PasswordEncoder` 实现。登录校验同步依赖新端口，删除旧 `PasswordHashVerifier` 和 `SpringPasswordHashVerifier`，避免并存两套密码抽象。

### 5.3 可修改账号

新增仅用于改密事务的领域投影：

```java
public record ChangeablePasswordAccount(
        long id,
        AccountType type,
        String passwordHash
) {
}
```

新增仓储端口：

```java
public interface PasswordAccountRepository {

    Optional<ChangeablePasswordAccount> findActiveByIdForUpdate(long userId);

    boolean updatePasswordAndIncrementTokenVersion(
            long userId,
            String passwordHash,
            LocalDateTime updatedAt,
            Long updatedBy);
}
```

使用独立端口是为了让改密用例只读取所需字段，并明确加锁与原子写入语义，不扩大登录仓储的职责。

### 5.4 XML Mapper

扩展现有 `UserAccountMapper` 和 `UserAccountMapper.xml`，手写 SQL 不进入注解。

加锁查询：

```sql
SELECT id, type, password_hash
FROM t_user_auth
WHERE id = ?
  AND deleted = 0
FOR UPDATE
```

密码和版本原子更新：

```sql
UPDATE t_user_auth
SET password_hash = ?,
    token_version = token_version + 1,
    updated_at = ?,
    updated_by = ?
WHERE id = ?
  AND deleted = 0
```

影响行数必须为 1，否则视为内部状态异常。不能拆成密码更新和 token version 更新两条 SQL。

### 5.5 应用服务

`ChangePasswordApplicationService.change(AuthenticatedPrincipal, ChangePasswordCommand)` 固定按以下顺序执行：

1. 在访问仓储前校验角色必须包含 `ADMIN`。
2. 严格解析正整数主体 ID，失败映射 `INVALID_TOKEN`。
3. 校验新密码长度为 8 至 128。
4. 加锁读取未删除账号；缺失时记录不含敏感数据的错误日志并返回 `INTERNAL_ERROR`。
5. 使用 BCrypt 校验当前密码；失败返回 `BAD_CREDENTIALS`。
6. 使用 BCrypt 判断新密码是否与当前密码相同；相同返回 `VALIDATION_ERROR`。
7. 生成新 BCrypt 摘要。
8. 原子更新密码摘要并递增 `token_version`。
9. 撤销该用户全部 refresh token。
10. 正常返回。

步骤 4 至 9 位于同一事务。预期的参数、权限和密码错误不记录 warn/error；未预期的账号缺失或更新行数异常记录中文错误日志，但不记录密码。

## 6. 并发与事务

### 6.1 两个并发改密请求

两个请求都携带旧密码时：

- 第一个请求锁定账号、完成改密并提交。
- 第二个请求获得行锁后读取到新摘要，旧密码校验失败。
- 最终只有一个请求成功。

测试必须证明去掉 `FOR UPDATE` 或事务边界后会破坏该语义。

### 6.2 中途失败回滚

若密码和 token version 已更新，但撤销 refresh token 时抛出运行时异常：

- 密码摘要回滚为旧值。
- `token_version` 回滚。
- refresh token 保持原状态。
- 客户端收到 `500 + 99999`，原会话仍可继续，允许重试。

### 6.3 与 refresh/logout 并发

账号行锁和 refresh token 行锁的顺序必须保持稳定，避免形成相反锁序：

- 改密先锁账号，再撤销该用户 refresh token。
- logout 目前先更新账号版本，再撤销 refresh token，与改密锁序一致。
- refresh 先锁单枚 refresh token，再读取账号，存在与改密相反的访问顺序。

本轮实施时必须增加并发验证，并检查数据库锁行为。若发现真实 MySQL 存在死锁窗口，refresh 流程需在独立修正批次统一锁序，不能通过无限重试掩盖问题。

## 7. Web 与安全配置

`CurrentUserController` 增加：

```java
@PutMapping("/password")
public ApiResponse<Void> changePassword(
        @CurrentUser AuthenticatedPrincipal principal,
        @Valid @RequestBody ChangePasswordRequest request)
```

`ChangePasswordRequest` 使用 record，字段保留中文 Javadoc 和 OpenAPI 描述。

Security 增加精确规则：

```java
.requestMatchers(HttpMethod.PUT, "/api/auth/me/password")
.hasRole("ADMIN")
```

接口不加入匿名白名单，不开放 DEMO 写权限，不使用 `/api/auth/**` 通配规则替代精确权限。

## 8. 测试策略

### 8.1 密码摘要与应用服务

- 新密码可生成 BCrypt 摘要，并可再次匹配。
- ADMIN 正确旧密码时完成更新与 refresh token 撤销。
- DEMO 在仓储访问前返回 `FORBIDDEN`。
- 主体 ID 非法返回 `INVALID_TOKEN`。
- 当前密码错误返回 `BAD_CREDENTIALS`，不生成新摘要、不写数据库。
- 新旧密码相同返回 `VALIDATION_ERROR`。
- 新密码长度非法返回 `VALIDATION_ERROR`。
- 账号缺失或更新行数异常返回 `INTERNAL_ERROR`。
- refresh token 撤销异常时密码和 token version 回滚。

### 8.2 持久化与并发

- 加锁查询只返回未删除账号。
- 更新同时修改密码摘要、递增 token version 和写入审计字段。
- 两个使用相同旧密码的并发请求最多一个成功。
- MySQL 条件测试用于验证真实行锁和死锁风险；Docker 不可用时保持条件跳过，但 H2 并发测试仍必须运行。

### 8.3 Web、安全与完整会话

- ADMIN 可调用接口，成功返回 `data:null`。
- DEMO 返回 `403 + 10003`。
- 未认证返回 `401 + 10002`。
- 空字段、短密码、超长密码和非法 JSON 返回 `400 + 90001`。
- 当前密码错误返回 `401 + 10001`。
- 成功后旧 access token 无法访问受保护接口。
- 成功后旧 refresh token 无法刷新。
- 新密码可以重新登录，旧密码不能登录。
- 其他账号会话不受影响。
- OpenAPI 不暴露密码默认值、示例明文或内部摘要字段。

## 9. 文档同步

实施完成后更新：

- `api-contract/auth.md`
- `arch/auth-flow.md`
- `rules/security-baseline.md`
- `status.md`
- `roadmap.md`
- 本设计与对应实施计划

identity 修改密码完成后，路线图中的 identity 模块可以标记完成，下一模块进入 `system`。

实施结果：接口、权限、事务回滚、会话失效、H2 并发和 MySQL 条件并发测试均已落地。2026-06-14 执行 `mvn clean test`：288 tests、0 failures、0 errors、4 skipped；跳过项均为 Docker 不可用时的 Testcontainers MySQL 条件测试。

## 10. 提交拆分

实施计划按五个小批次拆分：

1. 密码摘要能力与账号持久化。
2. 修改密码事务服务与回滚测试。
3. HTTP 接口、权限和 OpenAPI。
4. 完整会话与并发验收。
5. 文档同步和 identity 收尾。

每批执行 RED、GREEN、定向回归和中文提交，禁止合并为一次大提交。

## 11. 验收标准

- 仅 ADMIN 能修改本人密码。
- 当前密码错误不改变任何状态。
- 新密码满足长度要求且不能等于当前密码。
- 密码使用 BCrypt，任何输出和日志不包含密码或摘要。
- 密码摘要更新、token version 递增、refresh token 全撤销原子完成。
- 成功后全部旧 access token 和 refresh token 失效，不返回新 token。
- 并发旧密码改密最多一次成功。
- 不新增表、Flyway、Redis、密码历史、找回密码或单设备会话。
- SQL 全部位于 XML，新增 Java 类型和关键方法有中文注释。
- 定向测试、并发测试、ArchUnit、全量 `mvn clean test` 和 `git diff --check` 通过。
