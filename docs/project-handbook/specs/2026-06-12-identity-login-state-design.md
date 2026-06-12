# Identity 登录状态更新设计

> **后续执行要求**：实现前先编写分步计划，并按 TDD 和独立提交逐步落地。

## 1. 目标

在现有登录账号读取和凭据校验能力之上，补齐后台登录的持久化状态更新：

- ADMIN、DEMO 密码错误时原子累计失败次数。
- 第 5 次连续密码错误时持久化锁定 10 分钟。
- 登录成功时记录最后登录时间和客户端 IP，并清理失败状态。
- 状态更新失败时禁止后续签发 token。

本设计不实现 Caffeine 接口限流、access/refresh token 编排、Controller、HTTP 错误映射或用户资料读取。

本设计增量修订前置文档 `2026-06-12-identity-login-credential-design.md` 中“凭据校验器不写数据库、不累计失败次数”的边界：账号不存在、GUEST、锁定和密码正确分支仍保持无副作用；仅 ADMIN、DEMO 密码错误分支新增失败状态记录。

## 2. 已确认规则

### 2.1 参与累计的账号

- 只有已查询到、未删除且允许后台登录的 ADMIN、DEMO 账号参与失败累计。
- 用户名不存在时统一返回 `BadCredentials`，不写数据库。
- GUEST 无论密码是否匹配都统一返回 `BadCredentials`，不写数据库。
- 已处于锁定期的账号返回 `Locked`，不校验密码，也不再次累计。

这些规则确保对外结果仍不泄露账号是否存在或账号类型。

### 2.2 失败累计与锁定

- 最大连续失败次数：5。
- 锁定时长：10 分钟。
- 第 1 至第 4 次密码错误：`login_fail_count += 1`。
- 第 5 次密码错误：
  - `locked_until = now + 10分钟`
  - `login_fail_count = 0`
- 锁定期间不校验密码、不累计。
- 锁定到期后重新开始一个失败周期。
- 登录成功后同样将 `login_fail_count` 重置为 0，并清空 `locked_until`。

失败次数不能根据查询得到的 `UserAccount.loginFailCount` 在 Java 中计算。该值在并发请求下可能已经过期，阈值判断必须由数据库在单条 `UPDATE` 中完成。

### 2.3 登录成功审计

凭据校验成功后，登录编排必须先完成以下更新：

- `last_login_at = now`
- `last_login_ip = clientIp`
- `login_fail_count = 0`
- `locked_until = NULL`
- `updated_at = now`
- `updated_by = userId`

客户端 IP 由现有 `ClientIpResolver` 提供。空白 IP 规范化为 `null`，不保存未经可信代理规则确认的转发头。

只有成功更新一行后，后续登录编排才可以签发 access token 和 refresh token。数据库异常或更新行数不是 1 时，异常向上继续传播，最终由统一异常处理返回 `99999`。

## 3. 领域边界

### 3.1 `LoginStateRecorder`

位置：`identity.domain.auth`

该端口只表达登录状态变化，不依赖 MyBatis、Spring Security、Servlet 或 HTTP：

```java
public interface LoginStateRecorder {

    void recordPasswordFailure(
            long userId,
            LocalDateTime failedAt,
            int maxAttempts,
            LocalDateTime lockedUntil);

    void recordSuccessfulLogin(
            long userId,
            LocalDateTime loggedInAt,
            String clientIp);
}
```

两个方法都要求基础设施实现检查影响行数。未更新到恰好一行时抛出不带 HTTP 语义的登录状态更新异常，不能把失败当成成功。

### 3.2 `LoginStateUpdateException`

位置：`identity.domain.auth`

该异常表示登录状态没有按预期更新到唯一账号。异常消息只包含操作类型和账号 ID，不包含用户名、明文密码或密码摘要。基础设施数据库异常不转换为坏凭据，继续向上传播。

### 3.3 `LoginLockPolicy`

位置：`identity.domain.auth`

锁定规则使用不可变值对象表达：

```java
public record LoginLockPolicy(int maxAttempts, Duration lockDuration) {
}
```

首版固定由 Spring 配置提供：

- `myblog.security.password.login-max-attempts = 5`
- `myblog.security.password.login-cooldown = 10m`

值对象负责拒绝小于 1 的次数和非正数时长。领域校验器只依赖该值对象，不直接读取配置文件。

### 3.4 `LoginCredentialVerifier` 调整

现有校验顺序保持不变：

1. 查询未删除账号。
2. 判断账号类型。
3. 判断持久化锁定状态。
4. BCrypt 校验密码。

密码不匹配时，仅对 ADMIN、DEMO 调用：

```java
loginStateRecorder.recordPasswordFailure(
        account.id(),
        now,
        loginLockPolicy.maxAttempts(),
        now.plus(loginLockPolicy.lockDuration()));
```

记录成功后仍返回 `BadCredentials`。记录失败则异常向上传播，不降级为普通坏凭据。

成功凭据结果继续携带同一次查询得到的 `UserAccount`，但领域校验器不写成功审计；成功审计需要客户端 IP，属于后续应用层登录用例的编排职责。

## 4. 持久化设计

### 4.1 Mapper 与 SQL 位置

复用 `UserAccountMapper` 和 `UserAccountMapper.xml`，新增两个更新方法。所有 SQL 继续放在 XML，禁止使用 `@Update` 等 SQL 注解。

### 4.2 密码失败原子更新

目标 SQL 语义：

```sql
UPDATE t_user_auth
SET locked_until = CASE
        WHEN login_fail_count + 1 >= :maxAttempts THEN :lockedUntil
        ELSE NULL
    END,
    login_fail_count = CASE
        WHEN login_fail_count + 1 >= :maxAttempts THEN 0
        ELSE login_fail_count + 1
    END,
    updated_at = :failedAt,
    updated_by = NULL
WHERE id = :userId
  AND deleted = 0
  AND (locked_until IS NULL OR locked_until <= :failedAt)
```

关键点：

- 阈值判断与计数更新在同一条 SQL 中完成，避免并发丢失更新。
- 未到阈值时将过期的 `locked_until` 清空。
- 失败尝试尚未证明操作者就是账号本人，因此 `updated_by = NULL`。
- 查询后账号若被删除或被另一请求锁定，更新行数为 0，必须作为状态更新失败处理。

### 4.3 登录成功原子更新

目标 SQL 语义：

```sql
UPDATE t_user_auth
SET last_login_at = :loggedInAt,
    last_login_ip = :clientIp,
    login_fail_count = 0,
    locked_until = NULL,
    updated_at = :loggedInAt,
    updated_by = :userId
WHERE id = :userId
  AND deleted = 0
  AND (locked_until IS NULL OR locked_until <= :loggedInAt)
```

更新条件再次校验账号未删除且未被并发锁定。若凭据查询后账号状态发生变化，更新行数为 0，登录流程失败且不能签发 token。

### 4.4 事务边界

- 单次失败累计由一条原子 SQL 完成，不需要额外事务。
- 后续完整登录用例必须在应用层事务中依次执行：
  1. 凭据校验。
  2. 成功状态和审计更新。
  3. 创建 refresh token 记录。
  4. 签发 access token。
- access token 的字符串生成不产生持久化副作用，可以放在数据库写入完成之后。
- refresh token 明文只在事务成功后返回；事务回滚时不能向客户端返回任何 token。

完整事务编排不在本轮实现，但本轮接口必须为该顺序提供明确约束。

## 5. 与 Caffeine 限流的关系

持久化锁定和接口限流是两层不同防线：

- `login_fail_count` / `locked_until`：账号维度，跨进程重启保留。
- Caffeine：IP + username 维度，阻止高频尝试，进程重启后可丢失。

本轮不引入 Caffeine。后续限流器必须在账号查询和 BCrypt 前执行，超过阈值直接返回 429，不触发持久化失败累计。

## 6. 错误与安全处理

- 不在日志、异常或响应中写用户名对应的密码摘要、明文密码。
- 用户名不存在、GUEST、普通密码错误仍对外统一映射为 `10001`。
- 已处于持久化锁定期的账号保留独立领域结果，具体 HTTP 映射由登录接口设计决定。
- 状态 SQL 执行失败或影响行数异常属于系统一致性错误，最终映射为 `99999`。
- 状态写入失败时禁止继续 token 签发，不做“登录成功但审计稍后补偿”的降级。

## 7. 测试策略

### 7.1 领域测试

- 用户名不存在和 GUEST 不调用 `LoginStateRecorder`。
- 锁定账号不调用密码校验器和状态记录器。
- ADMIN、DEMO 密码错误时记录一次失败，并返回 `BadCredentials`。
- 记录参数使用统一业务时钟、配置阈值和准确的锁定截止时间。
- 状态记录异常原样向上传播。
- 密码正确时不记录失败，继续返回携带账号的 `Authenticated`。
- `LoginLockPolicy` 拒绝非法阈值和时长。

### 7.2 持久化集成测试

- 前 4 次失败按次递增。
- 第 5 次失败写入 10 分钟锁定并把计数重置为 0。
- 锁定期间更新不到账号。
- 锁定到期后的第一次失败从 1 重新累计，并清除旧锁定时间。
- 并发失败不会丢失计数，达到阈值时最终状态稳定为锁定。
- 登录成功更新 `last_login_at`、`last_login_ip`，并清理失败状态。
- 已删除账号和并发锁定账号的成功更新失败。
- 所有 SQL 位于 XML Mapper。

测试继续使用 H2 + Flyway；涉及 MySQL 特定并发语义时补充 Testcontainers MySQL 验证，Docker 不可用时允许按现有策略跳过。

## 8. 提交拆分

1. `设计登录状态更新边界`
2. `实现登录失败累计与持久化锁定`
3. `实现登录成功状态更新`
4. 后续独立设计并实现 Caffeine 登录限流
5. 后续独立设计并实现双 token 登录事务编排与 Controller

每个代码提交必须先完成对应 TDD 红绿循环，再执行定向测试、ArchUnit 和必要的全量回归。
