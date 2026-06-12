# Identity 登录限流设计

> **后续执行要求**：实现前先编写分步计划，并按 TDD 和独立提交逐步落地。

## 1. 目标

为后台登录增加单实例进程内限流，在账号查询和 BCrypt 前拦截同一客户端对同一用户名的连续高频失败：

- 限流维度为可信客户端 IP + 规范化用户名。
- 连续失败达到 5 次后冷却 10 分钟。
- 第 5 次失败仍返回用户名或密码错误；第 6 次起返回 HTTP 429 + `90002`。
- 登录成功后清除对应限流状态。
- 限流状态只存于 Caffeine，允许应用重启后丢失。

本设计不实现登录 Controller、双 token 登录事务编排、验证码、Redis、多实例共享限流、PASSWORD 文章解锁限流或评论限流。

## 2. 方案选择

### 2.1 采用方案：失败结果驱动的连续失败计数

登录用例在凭据校验前检查是否处于冷却期，只在得到 `BadCredentials` 后累计一次失败。成功登录清除计数，账号持久化锁定和系统异常不累计。

优点：

- 符合“连续凭据失败”的业务语义。
- 不会把成功登录、数据库故障或审计故障当成攻击。
- 第 5 次失败可以同时触发 Caffeine 冷却和数据库账号锁定。
- 不存在账号和 GUEST 同样累计，避免通过限流行为泄露账号是否存在。

限制：

- 并发请求可能在同一时刻通过前置检查后共同进入凭据校验。
- Caffeine 只负责降低单实例高频尝试，真实账号仍由数据库持久化锁定提供最终保护。

### 2.2 未采用方案

**按所有登录请求计数**：可以更早阻止请求，但正常登录、网络重试和客户端重复提交也会占用额度，不符合当前“失败 5 次”的规则。

**在校验前预占次数，成功后回滚**：可以收紧并发边界，但状态机更复杂，异常和超时会留下错误计数，收益不足以覆盖首版复杂度。

**直接使用 Redis**：支持多实例共享状态，但 V2 已确定单实例部署，提前引入 Redis 会增加部署和运维成本。多实例限流留到 V3 独立设计。

## 3. 已确认规则

### 3.1 限流键

限流键由两部分组成：

```text
clientIp + normalizedUsername
```

用户名规范化规则：

1. 请求参数先通过非空和最大 64 字符校验。
2. 去除首尾空白。
3. 使用 `Locale.ROOT` 转为小写。
4. 同一规范化结果同时传给限流器和账号查询，避免限流键与数据库查询语义不一致。

客户端 IP 必须来自现有 `ClientIpResolver`：

- 直连请求忽略转发头。
- 只有可信代理来源才读取 `X-Forwarded-For` 或 `X-Real-IP`。
- 返回值再次去除首尾空白并转为小写。
- 无法取得 IP 时使用内部固定值 `<unknown>` 参与限流，不把空值当成“不限流”。

限流键只保存在进程内，不写数据库、不写业务日志，也不包含密码或密码摘要。

### 3.2 计数与冷却

- 初始状态没有缓存条目，允许请求继续。
- 第 1 至第 4 次 `BadCredentials`：原子递增失败次数。
- 第 5 次 `BadCredentials`：失败次数达到阈值，仍向客户端返回 `10001`。
- 后续请求在凭据校验前检测到阈值状态，返回 HTTP 429 + `90002`。
- 冷却时间从第 5 次失败写入缓存时开始计算。
- 冷却期间的拦截请求不刷新过期时间，避免持续请求无限延长冷却。
- 冷却到期后缓存条目自动过期，下一次请求从新的失败周期开始。
- 登录成功后立即删除当前键，下一次失败重新从 1 开始。

第 5 次失败和第 6 次拦截的边界是有意设计：失败结果只有在 BCrypt 完成后才能确认，因此本次请求仍按凭据错误处理，下一次请求才应用已经生效的冷却状态。

### 3.3 不累计的结果

以下结果不修改 Caffeine 计数：

- `Authenticated`
- `Locked`
- 参数校验失败
- 数据库、状态审计或其他系统异常
- 被 Caffeine 前置拦截的请求

成功凭据结果清除限流状态。`Locked` 不清除状态，避免已触发双层保护后因为数据库锁定结果提前释放 Caffeine 冷却。

## 4. 组件边界

### 4.1 `LoginRateLimiter`

位置：`identity.domain.auth`

该端口表达登录失败频率限制，不依赖 Caffeine、Spring、Servlet 或 HTTP：

```java
public interface LoginRateLimiter {

    boolean isBlocked(String clientIp, String normalizedUsername);

    void recordFailure(String clientIp, String normalizedUsername);

    void reset(String clientIp, String normalizedUsername);
}
```

领域端口只返回是否被阻止，不抛 `ApiException`。未来登录应用服务负责将阻止结果映射为 `ApiErrorCode.RATE_LIMITED`。

### 4.2 `CaffeineLoginRateLimiter`

位置：`identity.infrastructure.ratelimit`

实现要求：

- 使用 Caffeine `Cache<LoginRateLimitKey, Integer>`。
- 使用 `cache.asMap().compute(...)` 原子累计同一键的失败次数。
- 计数达到阈值后保持在阈值，不允许整数无界增长。
- 使用 `expireAfterWrite(cooldown)`，确保第 5 次失败后的检查不会延长冷却时间。
- 使用 `maximumSize` 限制恶意用户名枚举造成的内存增长。
- 使用 Caffeine `Ticker` 测试时间推进，不使用 `Thread.sleep`。

`LoginRateLimitKey` 是基础设施内部不可变值对象，负责 IP 和用户名的非空规范化，不向其他模块暴露。

### 4.3 `LoginRateLimitProperties`

配置前缀：

```text
myblog.ratelimit
```

配置项：

```yaml
myblog:
  ratelimit:
    login-max-failures: 5
    login-cooldown: 10m
    login-maximum-size: 10000
```

校验规则：

- `login-max-failures >= 1`
- `login-cooldown` 必须为正数
- `login-maximum-size >= 1`

该配置独立于 `myblog.security.password.login-max-attempts` 和 `login-cooldown`。两者当前默认值相同，但职责不同：

- `myblog.ratelimit.*` 控制进程内 IP + username 防刷。
- `myblog.security.password.*` 控制数据库账号失败累计与持久化锁定。

未来可以独立调整任一层，不产生隐式耦合。

### 4.4 错误码

在 `ApiErrorCode` 增加：

```java
RATE_LIMITED("90002", HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁")
```

限流属于 common-infra 错误码空间。领域限流端口不依赖该枚举；未来应用层登录编排在前置检查失败时抛出对应 `ApiException`。

## 5. 登录流程中的位置

未来 `AuthApplicationService` 按以下顺序编排：

1. Controller 完成 Bean Validation，拒绝空用户名、空密码和超长用户名。
2. 使用 `ClientIpResolver` 获取可信客户端 IP。
3. 规范化用户名。
4. 调用 `LoginRateLimiter.isBlocked(...)`。
5. 已阻止：立即返回 HTTP 429 + `90002`，不查询账号、不执行 BCrypt、不累计数据库失败次数。
6. 未阻止：调用 `LoginCredentialVerifier`。
7. `BadCredentials`：调用 `recordFailure(...)`，随后返回 HTTP 401 + `10001`。
8. `Locked`：返回后续登录接口设计确定的账号锁定错误，不修改 Caffeine 状态。
9. `Authenticated`：先调用 `reset(...)`，再执行成功状态更新、refresh token 持久化和 access token 签发。

成功后的 `reset` 不属于数据库事务。即使后续成功状态或 token 事务失败，清除 Caffeine 计数也是可接受的，因为凭据已经被正确证明；数据库账号锁定和 token 事务仍保持各自一致性。

本轮只实现独立限流组件及测试，不提前创建 `AuthApplicationService`。实际调用链在双 token 登录事务编排设计中接入。

## 6. 与持久化锁定的关系

两层保护互不替代：

| 机制 | 维度 | 存储 | 作用 |
|---|---|---|---|
| Caffeine 登录限流 | IP + username | 单实例内存 | 在账号查询和 BCrypt 前阻止高频尝试 |
| 数据库持久化锁定 | userId | MySQL | 真实账号跨重启、跨实例保持锁定 |

对于真实 ADMIN 或 DEMO 账号：

- 第 5 次密码错误同时使 Caffeine 计数达到阈值，并由数据库写入 10 分钟锁定。
- 第 6 次请求优先被 Caffeine 拦截。
- 应用重启导致 Caffeine 状态丢失时，数据库 `locked_until` 仍会拒绝登录。

对于不存在账号或 GUEST：

- 不写数据库失败状态。
- Caffeine 仍按 IP + username 累计，降低用户名枚举和 BCrypt 旁路探测频率。

## 7. 并发与容量边界

- 同一键失败累计使用原子 `compute`，不得通过 `get` 后 `put` 实现。
- 不同键互不阻塞。
- 并发请求可能同时通过 `isBlocked`；后续失败累计必须全部保留，最终计数稳定在阈值。
- 首版不承诺严格阻止“全局时间顺序上的第 6 个并发请求”，这是失败结果后置计数的必然边界。
- `maximumSize=10000` 是个人博客单实例的默认内存保护值；达到容量后 Caffeine 可以淘汰旧条目。
- 容量淘汰可能提前释放某个内存限流键，因此 Caffeine 是尽力型防刷层，数据库锁定仍是账号安全底线。

多实例部署时，每个实例拥有独立计数，不能保证全局阈值。切换多实例前必须将该端口替换为 Redis 等共享实现，并通过独立 ADR 重新确认原子性与过期语义。

## 8. 安全与日志

- 不记录明文密码、密码摘要或完整限流键。
- 正常限流命中属于可预期业务结果，不在限流器内部逐次记录 warn 日志。
- 如后续需要安全观测，只记录脱敏后的聚合指标，不记录用户名。
- 可信代理配置错误会影响 IP 维度准确性，部署时仍必须由反向代理覆盖客户端提供的转发头。
- 限流器异常不得降级为“允许登录”；未预期异常按系统错误处理，避免安全组件静默失效。

## 9. 测试策略

### 9.1 配置测试

- 正确绑定 5 次、10 分钟和最大容量 10000。
- 拒绝非正阈值、非正冷却时间和非正容量。

### 9.2 限流器单元测试

- 初始键不阻止。
- 前 4 次失败不阻止。
- 第 5 次记录后阻止。
- 第 5 次请求本身仍由调用方按 `BadCredentials` 处理。
- 冷却期间的检查不延长过期时间。
- 使用测试 Ticker 推进 10 分钟后自动允许。
- 成功调用 `reset` 后立即允许。
- IP 不同或用户名不同的键互不影响。
- 用户名大小写和首尾空白规范化后命中同一键。
- 空 IP 统一映射到 `<unknown>`。
- 并发累计不丢失，计数不会超过阈值。
- 缓存容量受 `maximumSize` 限制。

### 9.3 架构和回归测试

- domain 不依赖 Caffeine、Spring、Servlet、HTTP 或 `ApiErrorCode`。
- Caffeine 依赖只由基础设施实现使用。
- 不新增 Mapper、SQL 或 Flyway 迁移。
- `ApiErrorCodeTest` 验证 `90002` 和 HTTP 429。
- 全量 `mvn clean test` 通过；现有 Docker 不可用跳过策略保持不变。

## 10. 提交拆分建议

1. `设计后台登录限流边界`
2. `引入登录限流配置与领域端口`
3. `实现Caffeine登录失败限流`
4. `同步登录限流实施结果`

每个代码提交必须先完成对应 RED，再写最小实现转为 GREEN，并执行 ArchUnit、定向测试和必要的全量回归。
