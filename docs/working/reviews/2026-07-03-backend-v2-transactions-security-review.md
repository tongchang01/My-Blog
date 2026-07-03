# 后端 V2 事务、安全与资源管理盘点

> 状态：初稿 / 整改延后
> 适用范围：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`、`src/main/resources/`
> 最后校准：2026-07-03
> 权威程度：整理过程材料
> 姐妹文档：[2026-07-03-backend-v2-code-duplication-review.md](2026-07-03-backend-v2-code-duplication-review.md)、[2026-07-03-backend-v2-code-style-review.md](2026-07-03-backend-v2-code-style-review.md)

> **整改延后**：本文档中的现象与待调查问题统一延后到 V2 第一版上线、CD 流程走通并稳定观察后再启动。若期间发现新的一档问题（安全 / 数据一致性 / 资源泄漏），照常处理并回补登记。详见 [2026-07-03-backend-v2-review-dimensions.md](2026-07-03-backend-v2-review-dimensions.md)。

## 本文档回答什么问题

前两份 review 覆盖了"重复代码"和"代码风格"。这一份继续同一次代码审查，换到之前没盘的维度：

- 事务边界与并发原语
- 认证、密码、令牌、上传等安全相关代码卫生
- 资源管理（IO 关闭、临时文件生命周期、错误路径下的补偿）
- MyBatis 参数绑定与 SQL 注入面
- 现代 Java 特性（sealed / pattern matching / record）使用一致性

同样只列现象，不下整改结论。业务逻辑正确性依旧排除在外。

## 大盘结论先说

本次扫描没有找到明显的安全/事务缺陷。以下位置的实现已经到位，值得单独说明（避免下面的盘点看起来在唱衰）：

- 所有 MyBatis XML 都使用 `#{...}` 参数占位符，没有 `${...}` 字符串拼接，SQL 注入面基本为零。
- `LocalStorageService.resolve` 有完整的 path traversal 防御：拒空、拒反斜杠、拒盘符前缀、拒绝对路径，再 `normalize()` + `startsWith(root)` 双重兜底。
- `ImageInspector` 完整解码首帧并校验尺寸/像素/GIF 帧数，防御 zip bomb / decompression bomb。
- `UploadSpooler` 采用"落临时 → 校验 → 原子 move → 补偿删除"的完整生命周期，`AttachmentUploadService` 又叠了一层 compensation。
- `RefreshTokenService` 使用 32 字节 `SecureRandom` 明文 + SHA-256 落库摘要，正确的 token 存储姿势（不用 BCrypt，因为熵已足够）。
- `JwtSecretStartupValidator` 会在启动阶段拒空密钥、默认开发密钥、以及短于 32 字节的密钥。
- `AuthApplicationService.login` 有意不加 `@Transactional`，把事务边界推到内部的 `LoginSuccessTransactionService`，避免 Spring 代理自调用失效。
- 无 `@Autowired` 字段注入，无 `System.out`/`printStackTrace`/`catch (Exception)` 兜底，无 TODO/FIXME。

以下现象都是在此基线之上的"能更整齐"或"值得复核"层面的问题。

## 现象清单

### 现象 P：所有 `*QueryService` 都没有 `@Transactional(readOnly = true)`

`grep @Transactional\(readOnly` 全项目 0 命中。所有查询服务（`ArticleQueryService` / `CategoryQueryService` / `TagQueryService` / `AdminCommentQueryService` / `CurrentUserProfileQueryService` / `AttachmentQueryService` 等）在无事务下执行，多语句查询没有一致性快照保护。

- 单语句 SELECT 无所谓（MySQL InnoDB 默认 REPEATABLE READ 是语句级）。
- 但 [CurrentUserProfileQueryService#L34-L37](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileQueryService.java#L34) 明明同时查了 `CurrentAccount` 和 `UserProfile` 两次；[PublicArticleQueryService](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/article/PublicArticleQueryService.java) 里也存在多语句读取。这类情况下加 `@Transactional(readOnly = true)` 才能保证两次查询看到同一 MVCC 视图，同时也给 JDBC 驱动 read-only 提示，允许跳过 flush 检测。
- 收益取决于是否真的有跨语句一致性要求。需要确认。

### 现象 Q：`AttachmentUploadService` 无类级或方法级 `@Transactional`（有意为之）

[AttachmentUploadService](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java) 的 `upload(...)` 没有 `@Transactional`，注释里明确写：

> 该服务故意不加事务，物理存储写入后只调用独立短事务服务；数据库失败时可以在事务回滚之后补偿删除本次随机对象。

这是正确的选择（外部资源和数据库不能包在同一事务里）。列出来是为了标记"这是有意的、不要在整改中顺手加上 `@Transactional`"。

### 现象 R：SHA-256 十六进制摘要工具重复三份

模式完全一致：`MessageDigest.getInstance("SHA-256")` → `.digest(bytes)` → `HexFormat.of().formatHex(...)`，`NoSuchAlgorithmException` 转 `IllegalStateException`。

- [RefreshTokenService#L83](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/token/RefreshTokenService.java#L83) `hash(rawToken)`
- [UploadSpooler#L60](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/image/UploadSpooler.java#L60) `sha256()`（返回 `MessageDigest` 实例，用法略不同）
- [CaffeineDuplicateCommentGuard#L52](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/protection/CaffeineDuplicateCommentGuard.java#L52) `sha256(value)`

属于第一份 review 的"重复代码"范畴，作为补充追加。

### 现象 S：`AuthApplicationService.login` 里 `instanceof` 用法不齐

[AuthApplicationService#L44-L55](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/AuthApplicationService.java#L44) 目前写法：

```java
if (credentialResult instanceof LoginCredentialResult.BadCredentials) {
    rateLimiter.recordFailure(command.clientIp(), username);
    throw new ApiException(ApiErrorCode.BAD_CREDENTIALS);
}
if (credentialResult instanceof LoginCredentialResult.Locked) {
    throw new ApiException(ApiErrorCode.BAD_CREDENTIALS);
}
LoginCredentialResult.Authenticated authenticated =
        (LoginCredentialResult.Authenticated) credentialResult;
```

`instanceof` 无 pattern 绑定，第三分支还要显式 cast。Java 17 起 `LoginCredentialResult` 若是 sealed，可以用：

```java
return switch (credentialResult) {
    case LoginCredentialResult.BadCredentials b -> ...;
    case LoginCredentialResult.Locked l -> ...;
    case LoginCredentialResult.Authenticated a -> ...;
};
```

对照：同一个项目里 [JwtTokenService#L173-L177](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/auth/JwtTokenService.java#L173) 已经用了 `if (!(claim instanceof Number number))` 的 pattern binding。两种写法混用。

需要确认 `LoginCredentialResult` 是否是 sealed（大概率是），以及是否有意保留 if 链风格。

### 现象 T：`AuthApplicationService.login` 中 `Locked` 与 `RATE_LIMITED` 语义

同一个方法里的三种"拒绝":

- IP + username 命中限流 → `RATE_LIMITED`（明确告知客户端"过频"）
- 账户被锁 → `BAD_CREDENTIALS`（不告知）
- 密码错 → `BAD_CREDENTIALS`（不告知）

"锁定"隐藏为"账号密码错误"是"不泄漏账号存在与否"的常见姿势；但 `RATE_LIMITED` 又已经泄漏了"这个 IP+username 有历史失败次数"。两个策略混用是否有意（例如"IP 层限流 vs 账号层锁"分开对待）需要确认。

### 现象 U：BCrypt 强度默认 10

[application.yml#L77](../../MyBlog-springboot-v2/src/main/resources/application.yml#L77) `bcrypt-strength: 10`。Spring Security 默认值。OWASP 2024 建议 BCrypt cost `≥ 12`（约 250 ms / hash on modern CPU）。

配置可覆盖，不是代码问题；但当前生产 profile 没有 override，等于跑默认 10。是否要在 [application-prod.yml](../../MyBlog-springboot-v2/src/main/resources/application-prod.yml) 里显式提到 12，需要确认。

### 现象 V：并发原语几乎没有

全项目搜索 `synchronized|Atomic*|ConcurrentHashMap|volatile` 只命中 2 个 `@Scheduled`：

- [ArticlePublishScheduler](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/scheduling/ArticlePublishScheduler.java)
- [PageViewMaintenanceScheduler](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats/infrastructure/scheduling/PageViewMaintenanceScheduler.java)

也无 `@Async`。系统基本是"每个请求跑在自己线程里 + 定时任务"。这带来两件事需要确认：

1. `@Scheduled` 在多实例部署下会各自触发（`ArticlePublishScheduler` 每分钟发布定时文章、`PageViewMaintenanceScheduler` cron 聚合）。是否已经用数据库唯一约束或行锁保证幂等？如果没有 leader-election 或 shedlock 类保护，多实例时会重复发布/重复聚合。
2. `CaffeineDuplicateCommentGuard` / `CaffeineCommentRateLimitService` / `LoginRateLimiter`（推测同类）都是 in-memory Caffeine。多实例部署下限流窗口会按实例数放大 N 倍。这可能是"当前只单机部署"的显式取舍，也可能是欠账。

### 现象 W：`RefreshTokenService.secureRandom` 是实例字段

[RefreshTokenService#L28](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/token/RefreshTokenService.java#L28)：

```java
private final SecureRandom secureRandom = new SecureRandom();
```

Spring 单例 + `SecureRandom` 线程安全，运行没问题。可讨论点是：

- 默认 `new SecureRandom()` 在 Linux 上取 `NativePRNG`，读 `/dev/urandom`，锁竞争在极高并发下可能被观测到。生产敏感场景常改用 `SecureRandom.getInstanceStrong()`（可能阻塞 `/dev/random`）或按线程一个 `ThreadLocal<SecureRandom>`。当前 QPS 完全用不到，标出来仅为了备忘。

### 现象 X：`ApiException` 是 `RuntimeException` 与 `AuthApplicationService` 的 `catch (RuntimeException)` 陷阱

第二份 review 现象 L 已提过 [AttachmentUploadService](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java#L74) 的 `catch (ApiException) { throw; } catch (RuntimeException) {...}` 模式。此处在 [ImageInspector#L48-L52](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/image/ImageInspector.java#L48) 出现另一处同款：

```java
} catch (IllegalArgumentException exception) {
    throw exception;
} catch (IOException | RuntimeException exception) {
    throw new IllegalArgumentException("图片文件已损坏", exception);
}
```

同一模式在项目里出现≥2 次。属于第一份 review 的"重复"范畴 + 第二份 review 现象 L 的延伸。

### 现象 Y：`AttachmentUploadService` 的 compensate 存在 short window

[AttachmentUploadService.storeAndRegister](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java#L108) 流程：

1. `storage.store(...)` 写物理对象成功。
2. `registrationService.register(attachment)` 数据库登记。
3. 若 (2) 抛异常 → `compensate(...)` 删物理对象。

窗口：进程在步骤 (1) 与 (2) 之间被强杀，或步骤 (2) 抛了非 `RuntimeException` 的 `Error`（`OutOfMemoryError` 之类），会留下无引用的物理对象。当前有 `catch (RuntimeException)` 兜底 + `catch (DuplicateKeyException)` 特殊路径，正常情况覆盖完整。是否要另加对账清理任务需要确认。

### 现象 Z：`application.yml` 中 `MYBLOG_STATS_HASH_SECRET` 默认空值

[application.yml#L52](../../MyBlog-springboot-v2/src/main/resources/application.yml#L52)：

```yaml
myblog:
  stats:
    hash-secret: ${MYBLOG_STATS_HASH_SECRET:}
```

对比 `MYBLOG_JWT_SECRET` 完全没有默认（缺少时 Spring 直接拒启动），`hash-secret` 允许空。得确认：

- 空 secret 时统计模块是否有启动校验拒绝空值？
- 还是空值有兜底策略（例如"若为空则不做 hash，直接明文存 IP"）？

如果没有启动校验，一份 `.env` 忘设置就悄悄退化。

### 现象 AA：`SecurityJwtProperties.secret` 泄漏面

[JwtTokenService#L74](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/auth/JwtTokenService.java#L74) 直接 `properties.secret().getBytes(...)` 生成 `SecretKeySpec`。`properties` 作为 record 存活整个进程，`secret()` 会返回 String。理论上 `String` 长期驻留堆里，被 heap dump 可以捞到明文。可讨论的强化：

- 用 `char[]` 或 `byte[]` 承接（record 与 Spring `@ConfigurationProperties` 生态支持有限，会引入复杂度）。
- 或至少在 `SecurityJwtProperties` 上加 `@ToString.Exclude` 之类保护，避免被日志/序列化误写出去（当前 record 的默认 `toString()` 会打全部字段包含 secret）。

## 待调查的问题

1. 现象 P：查询服务是否统一加 `@Transactional(readOnly = true)`？收益是"多语句读一致性 + 驱动优化"，成本几乎零。
2. 现象 S：`AuthApplicationService.login` 是否切成 `switch` on sealed `LoginCredentialResult`？以及 `LoginCredentialResult` 是否已经声明为 sealed。
3. 现象 T：`Locked` 返回 `BAD_CREDENTIALS` 是有意的账号级不透明策略，还是欠账？
4. 现象 U：生产 profile 是否要显式提高 BCrypt strength？影响登录延迟。
5. 现象 V：`@Scheduled` 任务在多实例下的幂等性是否已经用别的手段保护？Caffeine 限流是否有意接受"多实例放大"？
6. 现象 X + 第二份 review 现象 L：`catch (ApiException) { throw }` 模式已出现 2 次。是否要通过 `ApiException` 不继承 `RuntimeException`（或引入 `NotBusinessException` 之类基类）来根除？影响 Spring 事务回滚默认策略。
7. 现象 Y：附件对账清理任务是否已列入 backlog？
8. 现象 Z：`MYBLOG_STATS_HASH_SECRET` 空值时的行为是否已有启动校验？
9. 现象 AA：`SecurityJwtProperties` 的 `toString()` 是否需要遮蔽 `secret` 字段？以及配置类打日志的口径统一到什么程度。

## 下一步

- 与前两份 review 汇总一起，把"有意 / 欠账"分类完成后再合并成整改计划。
