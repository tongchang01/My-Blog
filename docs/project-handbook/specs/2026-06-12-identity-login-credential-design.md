# Identity 登录凭据校验设计

> **后续执行要求**：实现前先编写分步计划，并按 TDD 逐个提交。

## 1. 目标

建立后台登录最小纵向切片的领域基础，覆盖账号读取、账号类型、锁定判断和密码摘要校验。

本设计只解决“这组凭据是否具备继续登录的资格”，不签发 token、不写登录审计、不累计失败次数、不实现 Caffeine 限流，也不增加 HTTP 接口。

## 2. 业务边界

- 只有 `ADMIN` 和 `DEMO` 可以登录后台。
- `GUEST` 只是 schema 留位，不开放后台登录。
- 已软删除账号不参与登录查询。
- `lockedUntil` 晚于应用统一时钟时，账号处于锁定状态。
- 用户名不存在、账号类型不可登录、密码错误对外统一映射为 `10001`，避免泄露账号是否存在。
- 当前 schema 没有独立的 `enabled` 字段，本阶段不新增禁用状态。
- 锁定账号返回独立的领域结果，HTTP 状态和错误码由后续应用层设计统一映射。

## 3. 组件设计

### 3.1 `AccountType`

位置：`identity.domain.account`

表示 `t_user_auth.type`：

- `ADMIN(1)`：后台完整权限账号。
- `DEMO(2)`：后台只读演示账号。
- `GUEST(3)`：游客身份留位，不可登录后台。

枚举提供数据库值转换和 `canLoginToAdmin()` 判断。未知数据库值必须失败，不允许静默降级为 GUEST。

### 3.2 `UserAccount`

位置：`identity.domain.account`

领域对象包含本切片需要的最小字段：

- `id`
- `username`
- `passwordHash`
- `type`
- `tokenVersion`
- `loginFailCount`
- `lockedUntil`

领域行为：

- 判断账号类型是否允许后台登录。
- 使用调用方传入的当前时间判断是否仍在锁定期。

领域对象不依赖 Spring、MyBatis-Plus、HTTP 或数据库 Entity。

### 3.3 `UserAccountRepository`

位置：`identity.domain.account`

提供：

```java
Optional<UserAccount> findActiveByUsername(String username);
```

“active”仅表示记录存在且 `deleted=0`。账号类型、锁定和密码规则由领域与应用层判断，不能塞入 SQL 后导致状态语义丢失。

### 3.4 `PasswordHashVerifier`

位置：`identity.domain.auth`

提供：

```java
boolean matches(String rawPassword, String passwordHash);
```

该端口隔离 Spring Security 的 `PasswordEncoder`。基础设施适配器后续使用现有 BCrypt Bean 实现，领域和应用层不直接依赖 `common.security`。

### 3.5 `LoginCredentialVerifier`

位置：`identity.domain.auth`

按固定顺序校验：

1. 账号是否存在。
2. 账号类型是否允许后台登录。
3. 当前是否处于锁定期。
4. BCrypt 摘要是否匹配。

返回密封类型 `LoginCredentialResult`：

- `Authenticated(UserAccount account)`：携带已完成校验的账号，供后续 token 与审计编排复用，避免重复查询。
- `BadCredentials`：不携带账号信息。
- `Locked`：不携带账号信息。

用户名不存在、GUEST、密码错误均返回 `BadCredentials`。校验器不抛 HTTP 异常，不写数据库，不签发 token。

## 4. 持久化边界

后续基础设施实现使用 MyBatis-Plus：

- `UserAccountEntity` 映射 `t_user_auth`。
- `UserAccountMapper` 只声明必要查询方法，SQL 放 XML，不在注解中写 SQL。
- `MyBatisUserAccountRepository` 将 Entity 转换为领域对象。
- 查询必须限制 `deleted=0`，用户名精确匹配。

密码摘要不得写日志、异常消息或响应。

## 5. 测试策略

第一批领域测试覆盖：

- ADMIN 可以继续凭据校验。
- DEMO 可以继续凭据校验。
- GUEST 统一返回 `BadCredentials`。
- 未到 `lockedUntil` 返回 `Locked`。
- 锁定时间等于当前时间时视为已解锁。
- 密码错误返回 `BadCredentials`。
- 成功结果携带同一次查询并完成校验的 `UserAccount`。
- 未知账号类型数据库值转换失败。

后续持久化测试使用 H2 + Flyway 验证：

- 只读取 `deleted=0` 的账号。
- 数字账号类型正确映射。
- Entity 不泄漏到 application/domain 之外。

每个行为必须先看到测试按预期失败，再写最小实现。

## 6. 提交拆分

1. `建立登录账号领域模型`
2. `实现登录账号持久化读取`
3. `实现登录凭据校验`
4. 后续独立任务再处理失败累计、持久化锁定、Caffeine 限流、审计、双 token 编排和登录接口

每个提交只完成一个目的，并执行定向测试、ArchUnit 和必要的全量回归。
