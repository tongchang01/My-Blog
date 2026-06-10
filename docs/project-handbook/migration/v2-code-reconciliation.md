---
name: V2 代码留删计划
description: schema 冻结后，对 MyBlog-springboot-v2 现有源码逐文件给出留 / 改 / 删决策
type: project
status: completed
date: 2026-06-04
---

# V2 代码留删计划

## 执行结果（2026-06-06）

M1 清理已经完成。逐文件审计后发现，现有业务 domain 端口虽然名称仍有参考价值，但方法签名和依赖类型绑定旧 schema：使用 `String/int` ID、旧文章置顶/推荐字段、旧评论五类型、`is_review/is_delete` 等。原样保留会把旧模型带入重建阶段，因此本次删除 identity / content / comment 的全部旧业务源码，后续按冻结模型重新定义端口。

已保留：

- `common/` 横切能力
- Spring Security / JWT 访问 token 链路
- ArchUnit 架构测试
- Flyway `V1__init.sql` 与迁移测试

已删除：

- identity / content / comment 的 domain / application / web / infrastructure
- 依赖旧 identity application 的 `JwtAuthTokenServiceAdapter`
- 旧 Mapper XML 和全部旧业务测试
- `hutool-all`
- 已不存在业务端点的公开白名单

验证：`mvn clean test` 通过，46 tests，0 failures。

> **触发原因**：`arch/schema-design.md` + `MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql` 已形成 14 张表的新 schema 草案，与现有 v2 工程基于旧 schema 假设写的代码字段级冲突。
> **决策原则**：
> 1. **横切组件（common/）默认留**，与 schema 解耦；只在与 R6 C1 双 token 等新决策不一致处增量改
> 2. **业务模块 domain 端口只保留概念，不保留旧源码**：逐文件审计确认旧端口签名已绑定旧 schema，后续按冻结模型重新定义
> 3. **违反 pitfalls 红线的依赖**（hutool-all）无条件删
> 4. **测试代码跟随被测对象**：被测对象重写则测试重写
> 5. ArchUnit 等架构测试与 schema 无关，**全留**
>
> **状态字段含义**：
> - **留**：保留，不动
> - **微调**：保留主体，加/改少量字段或方法
> - **重写**：删除现有实现，按新 schema 重新实现（接口/类名可保留）
> - **删**：完全删除，新 schema 下不再需要

---

## 一、总览

| 维度 | 留 | 微调 | 重写 | 删 |
|---|---|---|---|---|
| common/ | ~20 个文件 | 5 | 0 | 0 |
| identity/ | 8（domain 端口） | 1（AuthRole 加 DEMO/GUEST） | ~15 | 2（旧角色映射） |
| content/ | ~12（domain 端口） | 0 | ~10 | 0 |
| comment/ | ~10（domain 端口） | 1（CommentType 改） | ~10 | 0 |
| infrastructure/（顶级） | 1 | 0 | 1 | 0 |
| db/migration | 0 | 0 | 1（V1 marker → 真 init） | 0 |
| 测试 | ArchUnit + JWT/异常等 common 测试 | 0 | 几乎所有业务测试 | 1（旧 schema fixture） |
| 依赖 | 全部 starter + MP + Flyway 等 | 0 | 0 | **hutool-all（红线）** |

---

## 二、common/ 横切组件

| 路径 | 决策 | 理由 / 操作 |
|---|---|---|
| `common/auth/AuthenticatedPrincipal` | 留 | 与 schema 无关 |
| `common/auth/BearerTokenResolver` | 留 | 标准 Bearer 解析 |
| `common/auth/CurrentUser` `CurrentUserArgumentResolver` | 留 | 注解 + 解析器，与 schema 无关 |
| `common/security/auth/JwtTokenService` | **微调** | 当前是单 token；按 R6 C1 加 refresh token 签发 + 校验 + `token_version` claim |
| `common/security/auth/TokenClaims` | **微调** | 加 `tokenVersion` 字段 |
| `common/security/auth/TokenPair` | **微调** | 拆 `accessToken / refreshToken / accessExpiresAt / refreshExpiresAt` |
| `common/security/auth/TokenRevocationStore` | 留 | 端口定义不变 |
| `common/security/support/InMemoryTokenRevocationStore` | 留 | 用于 access token 黑名单仍需要；refresh token 走 DB（新 `t_refresh_token`） |
| `common/security/JwtAuthenticationFilter` | **微调** | 取出 `tokenVersion` 与 DB `t_user_auth.token_version` 比对，不一致 401 |
| `common/security/SecurityConfig` | **微调** | refresh token 端点白名单；DEMO 角色权限矩阵 |
| `common/security/SecurityProblemSupport` `SecurityProbeController` | 留 | 与 schema 无关；探针仅限 `local/test` profile |
| `common/config/SecurityJwtProperties` | **微调** | 加 `refreshTokenTtl`（默认 7d）配置项 |
| `common/config/SecurityPublicEndpointProperties` | 留 | 配置类不变，白名单内容由 yml 调 |
| `common/config/ApiCorsProperties` | 留 | |
| `common/config/MyBatisPlusConfig` | 留 | 分页插件配置不变 |
| `common/config/OpenApiConfig` | 留 | Knife4j / springdoc 配置不变（R7 D11） |
| `common/error/ApiErrorCode` | 留 | 已对齐 ADR-0007 R6 C1 错误码语义 |
| `common/error/ApiException` | 留 | |
| `common/error/GlobalExceptionHandler` | 留 | |
| `common/web/ApiResponse` `PageResponse` | 留 | 统一响应契约 |
| `common/web/ClientIpResolver` `UserAgentResolver` | 留 | 评论审计需要继续用 |

**common/ 总结**：5 个文件做小幅扩展（围绕双 token），其他不动。

---

## 三、identity/ 模块

### domain（端口大多保留，1 个微调）

| 路径 | 决策 | 理由 |
|---|---|---|
| `domain/AuthRole` | **微调** | 由 `USER / ADMIN` → `ADMIN / DEMO / GUEST`（R5） |
| `domain/AuthenticatedUser` | 留 | record (id, username, roles) 结构不变 |
| `domain/LoginCommand` | 留 | (username, password) |
| `domain/CurrentUserProfile` | 留 | record 结构与 schema 字段一致即可（重写 reader 实现时对齐） |
| `domain/UserCredentialReader` | 留 | 端口接口 |
| `domain/CurrentUserProfileReader` | 留 | 端口接口 |
| `domain/LoginAuditRecorder` | 留 | 端口接口；落地实现重写时改写 `last_login_at / last_login_ip / login_fail_count / locked_until` |
| `domain/UserMenu` `UserMenuReader` | **删** | feature-inventory ⑩：后台菜单走前端静态路由，删除 `/admin/user/menus` 接口 |

### application

| 路径 | 决策 | 理由 |
|---|---|---|
| `application/AuthService` | **重写** | 字段调整：`token_version` 校验、登录失败次数 / 锁定逻辑、双 token 签发 |
| `application/AuthTokenService` | **重写** | 双 token 签发；refresh token SHA-256 hash 落 `t_refresh_token` |
| `application/IdentityQueryService` | **重写** | 字段重对齐 |

### infrastructure

| 路径 | 决策 | 理由 |
|---|---|---|
| `infrastructure/ConfiguredUserCredentialReader` | **删** | R5/R6：管理员账号靠 DB（首次部署运维 SQL 插入），不再支持 yml 配置账号 |
| `infrastructure/ConfiguredIdentityProperties` | **删** | 同上 |
| `infrastructure/DatabaseUserCredentialReader` | **重写** | 字段全变（`type / token_version / login_fail_count / locked_until`），SQL 重写 |
| `infrastructure/DatabaseCurrentUserProfileReader` | **重写** | t_user_info 三语 bio + 多个社交 URL，字段全变 |
| `infrastructure/DatabaseLoginAuditRecorder` | **重写** | 改写 `last_login_at / last_login_ip / login_fail_count / locked_until` |
| `infrastructure/DatabaseUserMenuReader` | **删** | 同 UserMenu 端口删除 |
| `infrastructure/RoleNameMapper` | **删** | 旧库角色名映射，新 schema 用 `type` 直接映射枚举，无需中转 |
| 新增：RefreshTokenRepository / RefreshTokenEntity / RefreshTokenMapper | **新增** | 落地 `t_refresh_token` 表的 CRUD（含 SHA-256 hash 索引查询、撤销标记、cleanup job） |

### web

| 路径 | 决策 | 理由 |
|---|---|---|
| `web/AuthController` | **重写** | 接口签名扩：`/auth/refresh` 端点；`/auth/logout` 撤销 refresh token + 自增 `token_version` |
| `web/AdminIdentityController` | **重写** | 字段对齐；菜单接口删除 |
| `web/LoginRequest` | 留 | (username, password) |
| `web/LoginResponse` | **微调** | 加 `refreshToken / refreshExpiresAt` |
| `web/MeResponse` | **重写** | 字段对齐（三语 bio、社交链接） |
| `web/UserMenuResponse` | **删** | |

---

## 四、content/ 模块

### domain

| 路径 | 决策 | 理由 |
|---|---|---|
| `domain/ArticleReader` | 留 | 端口接口 |
| `domain/ArticlePageQuery` | **微调** | 加 `lang` 维度（三语过滤） |
| `domain/ArticleSummary` | **重写** | 字段：title/summary 三语、slug、5 态状态 |
| `domain/ArticleDetail` | **重写** | 同上 + cover_attachment_id、access_password、publish_at |
| `domain/ArticleAccessToken` `ArticleAccessTokenService` `ArticleAccessCheck` | **重写** | R6 C1：从签名 token 改成 JWT `typ="article_access"` + `aid` claim |
| `domain/CategorySummary` `TagSummary` | **重写** | name 三语 + slug |
| `domain/ArticleTagSummary` | 留 | 简单关联结构 |
| `domain/ArchiveMonth` `FeaturedArticles` `AuthorSummary` | 留 | 维度对象结构不变 |
| `domain/ContentCatalogReader` | 留 | 端口接口 |

### application

| 路径 | 决策 | 理由 |
|---|---|---|
| `application/ContentQueryService` | **重写** | 字段重对齐（三语切换、状态机过滤） |

### infrastructure

| 路径 | 决策 | 理由 |
|---|---|---|
| `infrastructure/persistence/entity/CategoryEntity` | **重写** | 字段全变（name 三语 + slug + sort_order + 8 列审计） |
| `infrastructure/persistence/entity/TagEntity` | **重写** | 字段全变 |
| `infrastructure/persistence/mapper/ContentCatalogMapper` | **重写** | XML SQL 跟随字段重写 |
| `resources/mapper/content/ContentCatalogMapper.xml` | **重写** | 同上 |
| `infrastructure/DatabaseArticleReader` | **重写** | 跟随实体 |
| `infrastructure/DatabaseContentCatalogReader` | **重写** | 跟随实体 |
| `infrastructure/SignedArticleAccessTokenService` | **重写** | 改 JWT 形式 |
| 新增：ArticleEntity / ArticleMapper / ArticleMapper.xml | **新增** | 现在缺文章实体和 Mapper（infra 只有 reader 接口，没看到 ArticleEntity）；重写时补齐 |
| 新增：AttachmentEntity / FriendLinkEntity / SiteConfigEntity 等 | **新增** | 新表对应实体（content 模块只负责 article/category/tag；attachment/friend_link/site_config 归 system 模块） |

### web

| 路径 | 决策 | 理由 |
|---|---|---|
| `web/ContentArticleController` | **重写** | URL 改为 `/{lang}/posts/{id}` 或 `/{lang}/posts/{id}-{slug}`（ADR-0016） |
| `web/ContentCatalogController` | **重写** | 字段对齐 |
| `web/ArticleDetailResponse` `ArticleSummaryResponse` | **重写** | 三语字段 |
| `web/ArticleAccessRequest` `ArticleAccessResponse` | **重写** | JWT 形式 |
| `web/CategoryResponse` `TagResponse` `ArchiveMonthResponse` | **重写** | 三语 + slug |

---

## 五、comment/ 模块

### domain

| 路径 | 决策 | 理由 |
|---|---|---|
| `domain/CommentType` | **微调** | 由 `ARTICLE/MESSAGE/ABOUT/LINK/TALK`（5 种）→ `ARTICLE/GUESTBOOK`（2 种）；ABOUT/LINK/TALK 全删（feature-inventory ④/⑤/⑪决策） |
| `domain/CommentThread` | **重写** | 字段对齐：target_type/target_id、content_md/content_html、audit_status |
| `domain/CommentReply` | **重写** | 同上 |
| `domain/CommentAuthor` | **重写** | 加 author_user_id（管理员评论时）+ author_email/site/ip/ua |
| `domain/CommentCreateCommand` | **重写** | (targetType, targetId, parentId, replyToCommentId, replyToUserId, replyToNickname, authorNickname, authorEmail, authorSite, contentMd, clientIp, ua) |
| `domain/CommentPageQuery` | **微调** | 改 `(targetType, targetId)` 维度 |
| `domain/CommentReader` `CommentWriter` | 留 | 端口接口 |
| `domain/AdminCommentReader` `AdminCommentModerator` | 留 | 端口接口 |
| `domain/AdminCommentQuery / AdminCommentDetail / AdminCommentItem` | **重写** | 字段对齐 |
| `domain/AdminComment*Command` 系列 | **重写** | 字段对齐 |

### application / infrastructure / web

| 路径 | 决策 | 理由 |
|---|---|---|
| `application/CommentCommandService` | **重写** | sanitize content_md → content_html；@ 回复邮件触发（Resend HTTP API） |
| `application/CommentQueryService` | **重写** | 字段对齐 |
| `application/AdminCommentCommandService` `AdminCommentQueryService` | **重写** | 字段对齐 |
| `infrastructure/DatabaseComment*` 4 个 | **重写** | 跟随实体 + 新表 |
| 新增：CommentEntity / CommentMapper / CommentMapper.xml | **新增** | 落地 t_comment（target_type/target_id 二元定位、复合索引） |
| `web/CommentController` | **重写** | 接口路径改 `/articles/{id}/comments`、`/guestbook/comments` 两套（target_type 由路径决定，不用前端传） |
| `web/AdminCommentController` | **重写** | 字段对齐 |
| `web/CommentCreateRequest / CommentResponse / CommentReplyResponse` | **重写** | 字段对齐 |
| `web/AdminComment*Request/Response` 系列 | **重写** | 字段对齐 |

---

## 六、infrastructure/（顶级）

| 路径 | 决策 | 理由 |
|---|---|---|
| `infrastructure/persistence/package-info` | 留 | 包标记 |
| `infrastructure/security/JwtAuthTokenServiceAdapter` | **重写** | 跟随双 token 改造 |

---

## 七、db/migration

| 路径 | 决策 | 理由 / 操作 |
|---|---|---|
| `src/main/resources/db/migration/V1__create_v2_schema_marker.sql` | **重写** | 用 `docs/sql/V1__init.sql` 内容覆盖；删 DROP TABLE IF EXISTS 段；保留为单一 V1，无需新增版本 |
| `src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql` | **删** | 旧 schema fixture 全废；测试改为依赖新 V1__init.sql 自动加载（H2 + Flyway 已开启） |

**集成时同步处理 codex P1.1**：进 migration 时统一索引命名（`uk_category_slug` / `uk_tag_slug` / `idx_page_view_created` / `idx_mail_log_created`）以兼容 H2 `MODE=MySQL`。

---

## 八、测试代码

> 原则：被测对象重写则测试重写；架构 / 横切测试不动。

| 测试 | 决策 |
|---|---|
| `ArchitectureRulesTest` | **留** |
| `MyBlogV2ApplicationTest` | 留（启动测试与 schema 无关，但需要新 V1__init 跑通） |
| `common/security/JwtTokenServiceTest` | **微调**（覆盖 refresh token 路径） |
| `common/security/JwtAuthenticationFilterTest` | **微调**（`token_version` 校验） |
| `common/security/SecurityConfigTest` | **微调**（DEMO 角色） |
| `common/error/GlobalExceptionHandlerTest` | 留 |
| `common/auth/BearerTokenResolverTest` | 留 |
| `common/config/BackendPropertiesTest` | **微调**（`refreshTokenTtl`） |
| `identity/AuthServiceTest` | **重写** |
| `identity/AuthControllerTest` | **重写** |
| `identity/AdminIdentityControllerTest` | **重写** |
| `identity/Database*ReaderTest`（4 个） | **重写** |
| `identity/ConfiguredUserCredentialReaderTest` | **删**（端口本身删了） |
| `identity/RoleNameMapperTest` | **删** |
| `content/*ControllerTest`（2 个） | **重写** |
| `content/Database*Test`（3 个） | **重写** |
| `comment/*ControllerTest`（2 个） | **重写** |
| `comment/Database*Test`（4 个） | **重写** |
| `infrastructure/persistence/FlywayMigrationTest` | **微调**（断言新 14 张表 + 检查约束名全局唯一） |

---

## 九、依赖与配置

### pom.xml

| 依赖 | 决策 | 理由 |
|---|---|---|
| `mybatis-plus-spring-boot3-starter` 3.5.12 | 留 | |
| `mybatis-plus-jsqlparser` 3.5.12 | 留 | |
| **`hutool-all` 5.8.36** | **删** | 🔴 **pitfalls 红线**："不引入 hutool-all"；改用具体子模块（如 `hutool-crypto` `hutool-core`）按需引入 |
| `springdoc-openapi-starter-webmvc-ui` 2.8.8 | 留 | |
| `spring-security-oauth2-jose` | 留 | Nimbus JWT |
| `flyway-core` | 留 | |
| `archunit-junit5` 1.4.1 | 留 | |
| `mysql-connector-j` | 留 | |
| `h2` | 留 | 测试用；注意 P1.1 索引命名兼容 |
| 新增：`resend-java` 或自实现 HTTP 客户端 | **新增** | R7 D5 Resend HTTP API 邮件发送 |
| 新增：`caffeine` | **按需新增** | identity 实现登录限流时引入，不在基础设施阶段提前占位 |
| 新增：`commonmark-java` + `OWASP html-sanitizer` | **按需新增** | comment 实现 Markdown 渲染 + HTML 清洗时引入（R-013 红线："前端只渲染 content_html"） |
| 新增：`mapstruct` | **按需新增** | 首个 DTO / Entity 转换落地时引入，统一使用编译期类型安全映射 |
| 新增：`testcontainers-mysql` | **按需新增** | 首个 Mapper 集成测试落地时引入，用真实 MySQL 补充 H2 方言验证；执行环境需要 Docker |
| 新增：`maven-enforcer-plugin` 3.6.3 | **已启用** | 锁定 Java 17 / Maven 3.9.x，并用 `dependencyConvergence` 检查依赖收敛 |

### 配置文件

| 文件 | 决策 |
|---|---|
| `application.yml` | **微调**：`myblog.security.jwt.refreshTokenTtl: 7d`；公开端点白名单更新（`/auth/refresh`、新评论路径）；新模块属性（mail / rate-limit / 站点 i18n 默认值） |
| `application-local.yml` | **微调**：同上 |
| `application-test.yml` | **微调**：同上；保留 H2 + Flyway 启用 |

---

## 十、执行顺序建议

1. **第 1 步：基础设施先行**（解锁后续）
   - 修复 P1.1（索引命名）→ 把 `docs/sql/V1__init.sql` 拷成 `src/main/resources/db/migration/V1__create_v2_schema_marker.sql`（删 DROP），删 test V2 fixture
   - 删 `hutool-all`；第三方工具依赖在首个实际使用任务中单独引入、测试和提交
   - `application*.yml` 配置项扩展
   - 跑 `MyBlogV2ApplicationTest` + `FlywayMigrationTest` 确保启动 & schema 加载

2. **第 2 步：common/security 双 token 改造**
   - `JwtTokenService / TokenClaims / TokenPair / JwtAuthenticationFilter`
   - 新增 `RefreshTokenEntity / Mapper / Repository`
   - 单测（含 JwtTokenServiceTest 扩展）

3. **第 3 步：按模块串行重写**（每个模块内顺序：domain → infra entity/mapper → infra impl → application → web → 测试）
   - identity（依赖最少）
   - system（attachment / site_config / friend_link）— 当前 v2 没这个模块，新增
   - content
   - comment
   - stats / common-infra（page_view / page_view_daily / mail_log）— 当前 v2 没，新增

4. **第 4 步：跨模块联调**
   - 文章发布 → 评论邮件触发 → 邮件日志落库
   - 文章访问 → page_view 打点 → daily 聚合 job

5. **第 5 步：文档同步**
   - 更新 `status.md`、`v1-vs-v2.md`、`arch/persistence-strategy.md`
   - 写 ADR-0019（如本次出现新决策）

---

## 十一、未决问题清单

> 以下需要在执行前明确，否则会在某模块卡住。

1. **stats / system / common-infra 模块的包结构** — 当前 v2 工程只有 identity / content / comment 三个业务模块；schema 涉及的 attachment / friend_link / site_config / page_view / page_view_daily / mail_log 该归到哪些新模块？建议：
   - 新建 `system/` 容纳 attachment / friend_link / site_config
   - 新建 `stats/` 容纳 page_view / page_view_daily
   - `common-infra/` 容纳 mail_log（或归到 `common/mail/`，需对齐 ADR-0001~0006 模块边界）

2. **DEMO 账号语义** — 是真账号（DB 行）还是配置式只读身份？建议真账号，但需要明确"只读"如何在权限矩阵实现（`@PreAuthorize` 还是 Spring Security ACL？）

3. **i18n 路径前缀** — `/{lang}/posts/{id}` 是 Controller 路径变量还是 Spring 拦截器统一注入？影响 SecurityConfig 白名单写法

4. **t_attachment 软删恢复策略** — schema-design.md §3.10 注明"命中软删行需恢复"，application 层具体实现需要细化（事务里 update deleted=0 还是 delete 后 insert？）

---

## 相关文档

- 权威 schema：`arch/schema-design.md`
- 决策草案：`product/decisions-draft.md` R1–R8
- ADR：`decisions/0014` / `0015` / `0016` / `0017` / `0018`
- 红线：`pitfalls.md`（hutool-all、HTML 渲染、Flyway 已 apply 不改）
- 路线：`roadmap.md` S3 / M1
