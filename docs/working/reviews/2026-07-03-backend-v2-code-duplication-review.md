# 后端 V2 应用层重复代码盘点

> 状态：初稿 / 整改延后
> 适用范围：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 最后校准：2026-07-03
> 权威程度：整理过程材料

> **整改延后**：本文档中的现象与待调查问题统一延后到 V2 第一版上线、CD 流程走通并稳定观察后再启动。详见 [2026-07-03-backend-v2-review-dimensions.md](2026-07-03-backend-v2-review-dimensions.md)。

## 本文档回答什么问题

本文档只做一件事：从 `AttachmentDeleteService` 里一处 `requireAdmin` 私有方法出发，横向盘点后端 V2 应用层是否存在同类"看起来应当抽公共方法却各处独立实现"的重复代码。

本文档不做整改决定，只列现象和代码位置。后续调查再逐项裁决：

- 哪些是有意为之（例如刻意的模块隔离、语义差异、性能取舍），保留原状。
- 哪些是无差别复制，应当抽公共实现。
- 抽公共实现时，抽到 `common/` 的哪一层、以什么形态（组件 / 静态工具 / 常量类）。

## 范围

- 只看 `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`。
- 只看应用层（`application/`）和与应用层直接相关的 `common/auth`、`common/error` 使用。
- 不看 domain 层的不变量校验（例如 `Comment` / `Category` 构造器里的 `IllegalArgumentException`），那属于领域对象自身职责，不在本次盘点范围。
- 不看基础设施、Web 层、SQL 映射。

## 触发点

初次发现于 [AttachmentDeleteService.requireAdmin](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentDeleteService.java#L68)：

```java
private long requireAdmin(AuthenticatedPrincipal principal) {
    if (principal == null) {
        throw new ApiException(ApiErrorCode.INVALID_TOKEN);
    }
    if (!principal.roles().contains("ADMIN")) {
        throw new ApiException(ApiErrorCode.FORBIDDEN);
    }
    try {
        long id = Long.parseLong(principal.id());
        if (id <= 0) {
            throw new NumberFormatException();
        }
        return id;
    } catch (NumberFormatException exception) {
        throw new ApiException(ApiErrorCode.INVALID_TOKEN);
    }
}
```

该方法承担三件事：`principal` 非空校验、ADMIN 角色校验、把 `principal.id()` 解析为正数 long。同一段模板下方的搜索结果显示，它在应用层被多处以不同变体独立实现。

## 现象清单

以下每一项只描述"在哪些位置写了几乎相同的代码"，不判断是否应当整改。

### 现象 A：`requireAdmin` / `requireReadable` 存在多套并存实现

**A.1 模块级 `*Authorization` 组件（四份，方法签名与主体高度相似）**

- [ContentAuthorization](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/ContentAuthorization.java) — `requireAdmin` + `requireReadable`
- [CommentAuthorization](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/CommentAuthorization.java) — `requireAdmin` + `requireReadable`
- [FriendLinkAuthorization](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/friendlink/FriendLinkAuthorization.java) — `requireAdmin` + `requireReadable`
- [StatsAuthorization](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats/application/StatsAuthorization.java) — 仅 `requireReadable`

**A.2 服务内私有 `requireAdmin`（未使用任何 `*Authorization` 组件）**

- [AttachmentDeleteService#L68](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentDeleteService.java#L68)
- [AttachmentUploadService#L167](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java#L167)
- [SiteConfigUpdateService#L64](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/SiteConfigUpdateService.java#L64)
- [CurrentUserProfileUpdateService#L60](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileUpdateService.java#L60)
- [ChangePasswordApplicationService#L84](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/ChangePasswordApplicationService.java#L84)（同文件内还有第二处独立写法）

**A.3 服务内直接内联 `principal.roles().anyMatch(...)`（等价于 `requireReadable`）**

- [AttachmentQueryService#L83](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentQueryService.java#L83)
- [AdminSiteConfigQueryService#L36](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/AdminSiteConfigQueryService.java#L36)

**A.4 观察到的实现差异（不判断对错，只记录）**

- 校验顺序：`CommentAuthorization.requireAdmin` 先调用 `requireReadable` 再判断 ADMIN；其余三个 `*Authorization` 是各自独立的两段判断。
- 错误码选择：`ContentAuthorization` 中"解析出的 ID `<=0`"抛 `FORBIDDEN`；`AttachmentDeleteService` / `AttachmentUploadService` / `FriendLinkAuthorization` / `SiteConfigUpdateService` 等处走 `catch NumberFormatException` 分支抛 `INVALID_TOKEN`。
- 访问修饰符：`FriendLinkAuthorization` 是 package-private；`ContentAuthorization` / `CommentAuthorization` / `StatsAuthorization` 是 `public`。

### 现象 B：`parsePositiveUserId(String)` 有五份完全相同的私有方法

方法体几乎逐字符一致（差异仅在异常变量名 `ex` / `exception`）：

- [ChangePasswordApplicationService#L89](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/ChangePasswordApplicationService.java#L89)
- [LogoutApplicationService#L28](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/auth/LogoutApplicationService.java#L28)
- [CurrentUserProfileQueryService#L56](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileQueryService.java#L56)
- [CurrentUserProfileUpdateService#L69](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/application/profile/CurrentUserProfileUpdateService.java#L69)
- [SiteConfigUpdateService#L73](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/SiteConfigUpdateService.java#L73)

同一段"把 `principal.id()` 解析为正数 long"的逻辑，也内嵌在现象 A 的所有 `requireAdmin` 后半段里。

### 现象 C：`id <= 0` + `VALIDATION_ERROR` + "XXX ID 必须为正数" 服务层样板

服务层 14+ 处使用同一模板，差异仅在名词（"文章" / "分类" / "标签" / "附件"）：

```java
if (id <= 0) {
    throw new ApiException(
            ApiErrorCode.VALIDATION_ERROR,
            "文章 ID 必须为正数");
}
```

分布：`content/application/article/`、`content/application/category/`、`content/application/tag/`、`system/application/attachment/` 下的 delete / update / restore / query 服务。完整位置列表在附录 1。

> 说明：domain 层构造器里也有一批"XXX ID 必须为正数"抛 `IllegalArgumentException` 的写法（`Comment.java`、`Category.java`、`NewArticle.java` 等），属于领域不变量，与本条现象不是同一件事，不列入。

### 现象 D："更新受影响行数为 0 → `log.error(...行数异常)` → `throw INTERNAL_ERROR`"三段式

10 处使用同一模板：

```java
if (!repository.softDelete(id, now, actorId)) {
    log.error("XXX 软删除行数异常，xxxId={}", id);
    throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
}
```

分布：`Category` / `Tag` / `Attachment` / `FriendLink` / `SiteConfig` / `Profile` / `Password` 的写服务。完整位置列表在附录 2。

差异点：日志前缀名词（"分类软删除" / "标签更新" 等）、日志字段名（`categoryId` / `tagId` / `attachmentId` / `friendLinkId` / `userId` 等）、有无 `siteConfigId` 之类附加字段。

### 现象 E：角色字符串字面量散落各处

- `"ADMIN".equals(role) || "DEMO".equals(role)` 内联写法 4 处：[AttachmentQueryService](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentQueryService.java#L83)、[AdminSiteConfigQueryService](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/AdminSiteConfigQueryService.java#L36)、[CommentAuthorization](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/CommentAuthorization.java#L16)、[FriendLinkAuthorization](../../MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/friendlink/FriendLinkAuthorization.java#L20)（另有 `ContentAuthorization` / `StatsAuthorization` 中的等价形式）。
- `principal.roles().contains("ADMIN")` 内联写法 8+ 处。

字面量 `"ADMIN"` / `"DEMO"` 未以枚举或常量集中。

## 待调查的问题

后续逐项确认，才能决定是否整改：

1. 现象 A.1 的四份模块级 `*Authorization` 组件是否有意保留？例如是否用于日后按模块扩展角色维度、模块内额外校验、限制跨模块引用？还是当初复制粘贴留下的？
2. 现象 A.2 中 `system/attachment`、`system/siteconfig`、`identity/profile`、`identity/auth` 为什么没有沿用同目录/模块的 `*Authorization` 组件而各自私有实现？是模块历史顺序问题，还是有意区分？
3. 现象 A.4 中"解析出的 ID `<=0`"应当抛 `FORBIDDEN` 还是 `INVALID_TOKEN`？两者是否代表不同的语义假设（"token 里的 subject 非法" vs "身份合法但不允许访问"），需要统一后才能抽公共实现。
4. 现象 B 的 `parsePositiveUserId` 与现象 A 的 `requireAdmin` 后半段是否应当是同一段代码，还是"仅解析"和"鉴权+解析"应当分开对外提供两个方法？
5. 现象 C 的错误消息文案（"文章 ID 必须为正数"等）是否属于对外契约的一部分，抽公共方法后是否需要保留各名词、还是允许统一为"资源 ID 必须为正数"之类通用文案？影响前端是否依赖这些具体消息。
6. 现象 D 的三段式是否值得抽公共工具？收益是"少写三行"，成本是引入新抽象；需要衡量。日志字段名 `xxxId` 语义要不要统一。
7. 现象 E 的角色常量应放在 `common/auth` 下的什么位置？以什么形态（枚举、常量类、`Role` 值对象）？会不会与未来接入 Spring Security 的 `Authority` 前缀策略冲突？

## 下一步

- 逐项走访相关模块的历史提交，确认哪些差异是有意为之。
- 对确认为无差别复制的部分，另起整改计划文档，落到 `docs/working/plans/`。

## 附录 1：现象 C 的位置清单

- `content/application/article/ArticleDeleteService`
- `content/application/article/ArticleQueryService`
- `content/application/article/ArticleRestoreService`
- `content/application/article/PublicArticleQueryService`
- `content/application/category/CategoryDeleteService`
- `content/application/category/CategoryQueryService`
- `content/application/category/CategoryUpdateService`
- `content/application/tag/TagDeleteService`
- `content/application/tag/TagQueryService`
- `content/application/tag/TagUpdateService`
- `system/application/attachment/AttachmentDeleteService`（`delete` 与 `restore` 两处）

## 附录 2：现象 D 的位置清单

- `content/application/category/CategoryDeleteService`
- `content/application/category/CategoryUpdateService`
- `content/application/tag/TagDeleteService`
- `content/application/tag/TagUpdateService`
- `system/application/attachment/AttachmentDeleteService`
- `system/application/friendlink/FriendLinkDeleteService`
- `system/application/friendlink/FriendLinkStatusService`
- `system/application/friendlink/FriendLinkUpdateService`
- `system/application/siteconfig/SiteConfigUpdateService`
- `identity/application/auth/ChangePasswordApplicationService`
- `identity/application/profile/CurrentUserProfileUpdateService`
