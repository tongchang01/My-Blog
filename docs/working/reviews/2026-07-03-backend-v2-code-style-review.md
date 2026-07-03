# 后端 V2 应用层代码风格与实现细节盘点

> 状态：初稿 / 整改延后
> 适用范围：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 最后校准：2026-07-03
> 权威程度：整理过程材料
> 姐妹文档：[2026-07-03-backend-v2-code-duplication-review.md](2026-07-03-backend-v2-code-duplication-review.md)

> **整改延后**：本文档中的现象与待调查问题统一延后到 V2 第一版上线、CD 流程走通并稳定观察后再启动。详见 [2026-07-03-backend-v2-review-dimensions.md](2026-07-03-backend-v2-review-dimensions.md)。

## 本文档回答什么问题

姐妹文档只盘点"重复代码"这一类问题。本文档继续同一次代码审查，扩到风格、实现选择、标准库使用、可读性等更广的维度。

本文档不做整改决定，只列现象、给出代码位置和判断需要的背景。是否整改、如何整改留到调查完成后再决定。

## 范围与前提

- 只看 `MyBlog-springboot-v2/`。
- 用户明确排除业务逻辑正确性；本文档只谈"写法"与"实现细节"。
- 只列有代码证据的问题，主观口味不列入。
- 大盘上，本项目质量基线已经很高：全量构造器注入、无 `System.out` / `printStackTrace`、无 `catch (Exception)` 兜底、无 `@Autowired` 字段注入、无 TODO/FIXME 遗留、有 ArchUnit、有 MapStruct、有 `Clock` 抽象、绝大多数领域对象是 `record` + 静态 `create` 工厂。以下现象都是在此基线之上的"能更整齐"层面的问题。

## 现象清单

### 现象 F：日志声明风格混用

同一项目里同时存在两种声明日志字段的方式：

**用 Lombok `@Slf4j`（13 处）：** 分布在 `content` / `identity` / `system/friendlink` 等应用服务。

**手写 `LoggerFactory.getLogger(...)`（6 处）：**
- [MyBlogConfigStartupValidator#L16](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/MyBlogConfigStartupValidator.java#L16) — 字段名 `LOGGER`（唯一大写变体）
- [GlobalExceptionHandler#L26](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/error/GlobalExceptionHandler.java#L26)
- [LocalStorageService#L25](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/infrastructure/storage/local/LocalStorageService.java#L25)
- [S3StorageService#L24](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/infrastructure/storage/s3/S3StorageService.java#L24)
- [AttachmentRestoreService#L24](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentRestoreService.java#L24)
- [AttachmentUploadService#L38](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java#L38)

其中 5 处字段名 `log`，1 处 `LOGGER`。

### 现象 G：`Optional` 的"典型反模式"用法

**`isPresent()` / `isEmpty()` + `get()` 组合（Java 官方文档明确不推荐）：**

- [RefreshSessionTransactionService](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/RefreshSessionTransactionService.java#L38)：`tokenOptional.isEmpty()` → `return`；下一行 `tokenOptional.get()`；`accountOptional` 同样处理。
- [AttachmentUploadService#L65](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java#L65)：`duplicate.isPresent() ? reuseOrRestore(duplicate.get(), ...) : ...`。
- [CurrentUserProfileQueryService#L34-L48](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileQueryService.java#L34)：`isEmpty()` 判断 → 记日志 → 抛异常，之后又 `account.orElseThrow()` 再取一次。

> 说明：`LocalStorageWebCondition` 中 `explicit.get()` 是 Spring `BindResult.get()`，与 `Optional` 无关，不属于本条。

### 现象 H：`LocalDateTime` + 注入 `Clock` 作为全局时间口径

40+ 处使用 `LocalDateTime.now(clock)`，跨越 domain / application / persistence / VO / Controller。项目里没有出现 `Instant` / `OffsetDateTime` / `ZonedDateTime`（除 `TokenPair` 的一处 `Instant`）。

现状：`LocalDateTime` 丢弃时区信息；数据库列（`datetime` 与 `timestamp`）在存与取时依赖 `Clock` 的 zone 与 JVM 时区一致。是否有意为之需要确认。参考位置：[CurrentUserProfileUpdateService#L54](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileUpdateService.java#L54)、[AttachmentDeleteService#L44](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentDeleteService.java#L44)、[Article.java](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/article/Article.java) 各 `LocalDateTime` 字段。

### 现象 I：`ApiException` 缺 `(code, Throwable)` 构造函数，使用方绕开

[ApiException](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/error/ApiException.java) 只暴露 `(code)` 和 `(code, message)` 两个构造函数，无 cause 参数。

[AttachmentUploadService#L192](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java#L192) 因此写了一段变通：

```java
private ApiException internal(Throwable cause) {
    ApiException exception = new ApiException(ApiErrorCode.INTERNAL_ERROR);
    exception.initCause(cause);
    return exception;
}
```

其他服务里如果想让 `INTERNAL_ERROR` 带原始 `cause` 传给日志/APM，需要各自重复这段变通，或者索性丢掉 cause。

### 现象 J：出现完全限定名（应当 import）

[AttachmentUploadService#L120](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java#L120)：

```java
StoredObject object = storage.store(
        new com.tyb.myblog.v2.common.storage.StoreObjectCommand(
                upload.path(),
                objectKey,
                ...));
```

`StoreObjectCommand` 未 import，直接以完整包名书写。同文件顶部已有多个 `common.storage.*` 的 import，独此一处未加。

### 现象 K：`Stream.peek(...)` 承担校验副作用

[ArticleValidation#L145-L155](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/domain/article/ArticleValidation.java#L145)：

```java
List<Long> normalized = raw.stream()
        .peek(id -> {
            if (id == null || id <= 0) {
                throw new IllegalArgumentException(
                        "文章标签 ID 必须为正数");
            }
        })
        .distinct()
        .sorted()
        .toList();
```

`Stream.peek` 的 JDK Javadoc 明确写"chiefly to support debugging"，且非保证执行（惰性/短路条件下）。此处终止操作是 `toList()`，行为上可以工作，但语义上校验应放在 `.map` / `.filter`（负校验）或前置 `forEach` 里，避免读者误解。

### 现象 L：`catch (ApiException) { throw; }` 只为让下面的 `catch (RuntimeException)` 不吃掉自己

[AttachmentUploadService#L74-L86](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java#L74)：

```java
} catch (ApiException exception) {
    throw exception;
} catch (IllegalArgumentException exception) {
    throw new ApiException(
            ApiErrorCode.VALIDATION_ERROR,
            exception.getMessage());
} catch (IOException exception) {
    log.error(...);
    throw internal(exception);
} catch (RuntimeException exception) {
    log.error(...);
    throw internal(exception);
}
```

`catch (RuntimeException)` 兜底就会捕获 `ApiException`，所以上面被迫先写一段"catch-and-rethrow"。可读性一般，也容易被 IDE / SonarLint 标为 "identity catch block"。可以换写法（例如把 `ApiException` 从 `RuntimeException` 拆开、或把兜底 catch 收窄），但会影响别的地方，需要评估。

### 现象 M：Bean Validation（`@Valid` / `@NotBlank` 等）覆盖不均衡

**用了 Bean Validation 的 Controller / Request（9 处）：**
- `identity`：`AuthController` + `LoginRequest` + `RefreshTokenRequest`、`CurrentUserController` + `ChangePasswordRequest`
- `comment`：`AdminCommentController` + `AdminCommentReplyRequest`
- `stats`：`PublicPageViewController` + `PageViewRecordRequest`

**没有用 Bean Validation 的 Controller / Request：**
- `content` 全部写接口（`AdminArticleController`、`AdminCategoryController`、`AdminTagController`）以及公共查询接口。请求体 record 内部完全没有 `@NotBlank` / `@Size` / `@Valid`。
- `system` 全部写接口（`AttachmentController`、`FriendLinkController`、`SiteConfigController`）同上。

不用 Bean Validation 的这一批，校验全部推给 domain（`ArticleValidation` / `SiteConfig.create` / `AttachmentValidation` 等），走 `IllegalArgumentException` 再由服务层转 `VALIDATION_ERROR`。业务上能兜住，但两套风格并行需要判断是有意为之（domain 已经足够严格，Controller 层校验冗余）还是欠账。

### 现象 N：命名与结构上的小不一致

以下每一项单独看都是小事，累计起来是"约定不明"的信号：

- 异常变量名：既有 `NumberFormatException ex`，也有 `NumberFormatException exception`（`ex` 只出现在 [CommentAuthorization](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/CommentAuthorization.java#L33) 与 [PersistentAccessTokenVerifier](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/token/PersistentAccessTokenVerifier.java#L50)）。
- `Optional` 变量命名：既有 `Optional<X> xOptional`（[RefreshSessionTransactionService](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/RefreshSessionTransactionService.java#L38)），也有 `Optional<X> x`（[CurrentUserProfileQueryService](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileQueryService.java#L34)）。
- 错误消息文案：`"文章 ID 必须为正数"`（有空格）、`"站点配置 ID 必须固定为1"`（"1" 前后无空格）、`"分类不能超过 X 个字符"`（有空格）、`"不能超过X个字符"`（无空格）等混排。
- `*Authorization` 类的访问修饰符：`public` 三处，package-private 一处，语义无差别。
- `NumberFormatException("non-positive")` 与不带 message 版本混用；消息本身也会被下一层 catch 丢弃，属于死消息。
- 分类词典：`domain/article` 下同时存在 `AdminArticleQueryRepository` 与 `PublicArticleQueryRepository`；但 `content/domain/category` / `content/domain/tag` 只有一个 `CategoryRepository` / `TagRepository`。命名维度不统一（视角前缀 vs 无前缀），是有意区分（前后台读写模型分离）还是历史差异，需要确认。

### 现象 O：细节实现选择

以下几处不影响正确性，但存在"可以更清晰或更贴近标准库"的余地：

- [ClientIpResolver.firstForwardedIp](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/web/ClientIpResolver.java#L54)：用 `value.split(",")` 全量切分再取第 1 个有效。链路长时会分配整段数组。可以用 `indexOf(',')` + `substring` 或提前 `break`。收益极小，仅在极长转发链有意义。
- [SiteConfig#L106-L113](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/siteconfig/SiteConfig.java#L106) `localized(...)` 是三分支 `switch`。语义上是"按语言取字段"；如果日后语言维度扩展或字段扩展，用 `EnumMap<SiteLanguage, String>` 存本地化文本可能更利于扩展。当前 3 语言 × 3 字段规模下，`switch` 是合理选择。
- [CommentAuthorization#L15](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/CommentAuthorization.java#L15) 与 [ContentAuthorization#L19](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/ContentAuthorization.java#L19) 都用 `roles.stream().anyMatch(role -> "ADMIN".equals(role) || "DEMO".equals(role))`。同一个判断用 `Set.of("ADMIN","DEMO")::contains` 更短，或者定义好角色常量后一次搞定（与姐妹文档现象 E 关联）。

## 待调查的问题

以下问题需要与你/历史提交确认后，才能决定哪些整改、哪些保留：

1. 现象 F 的日志声明风格：是否统一到 `@Slf4j`？其中 `GlobalExceptionHandler` 等在 `common` 下的类是否有意避开 Lombok 依赖污染？
2. 现象 G 的 `isEmpty()`+`get()` 反模式：是当前团队接受的写法（可读性优先），还是打算收敛？特别是 `RefreshSessionTransactionService` 里两处，改成 `flatMap` 会不会牺牲事务边界的线性可读性？
3. 现象 H 的 `LocalDateTime` 全局口径：是否有意与 MyBatis-Plus / MySQL `datetime` 列的默认映射保持一致？如果日后接入国际化时间显示或跨机房，是否需要迁到 `Instant`？此处影响面很大，属于设计决策而非风格。
4. 现象 I 的 `ApiException` 缺 cause 构造函数：是否直接补 `(code, message, Throwable)` 与 `(code, Throwable)`？会影响 `GlobalExceptionHandler` 是否需要打 cause 到日志。
5. 现象 J 的完全限定名：确认是遗漏还是刻意（例如避免和别处同名冲突）——本项目内没有别的 `StoreObjectCommand`，倾向遗漏。
6. 现象 K 的 `Stream.peek`：是否统一换成先 `forEach` 校验或 `map` 断言的写法？
7. 现象 L 的 `catch (ApiException) { throw }`：是否值得让 `ApiException` 不继承 `RuntimeException`（引入自定义标记 / 抽象超类）？收益是消除该模式；成本是全局改动、且违反 Spring 事务默认按 `RuntimeException` 回滚的假设，需要评估事务影响。
8. 现象 M 的 Bean Validation 覆盖：`content` / `system` 写接口是有意"domain 一票否决、Controller 不重复校验"，还是欠账？如果统一，前端会不会收到"请求过大在 Controller 就被拦"与"到 domain 才发现"两类不同错误消息？
9. 现象 N 里 `*QueryRepository` 命名维度：是否统一为"按视角前缀 (Admin/Public) + 领域名"？涉及 category / tag 是否要拆读模型。
10. 现象 O 里 `SiteConfig.localized` 与角色 `anyMatch` 是否想引入 `EnumMap` / `Set.of(...)::contains` 之类小写法调整。

## 下一步

- 与姐妹文档现象一起，按"有意 / 欠账"分类。
- 分类完成后，把"欠账"部分合并成一份整改计划文档，落到 `docs/working/plans/`。
