# 后端 V2 MyBatis-Plus 分模块迁移实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:executing-plans` 按任务逐个执行。步骤使用 checkbox（`- [ ]`）语法跟踪状态。用户要求“一个任务一个任务来”，每个任务完成并提交后应停下汇报，不要一次性把所有任务做完。

**目标：** 将后端 V2 剩余生产代码中的 `JdbcTemplate` 按模块逐步替换为 MyBatis-Plus，保持现有业务接口、Controller 响应和测试断言不退化。

**架构：** 后端 V2 保持模块化单体，业务模块仍按 `web / application / domain / infrastructure` 分层。MyBatis-Plus 的 Entity 放在各模块 `infrastructure.persistence.entity`，Mapper 放在 `infrastructure.persistence.mapper`，业务实现类继续实现 domain 端口，避免 application 和 web 层直接依赖 Mapper。

**Tech Stack:** Java 17、Spring Boot 3.5.x、MyBatis-Plus 3.5.12、MySQL、H2、Flyway Test Migration、JUnit 5、ArchUnit。

---

## 1. 当前盘点结果

执行盘点命令：

```powershell
rg "JdbcTemplate" MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java -n
```

生产代码剩余使用点：

| 模块 | 文件 | 职责 | 迁移风险 |
| --- | --- | --- | --- |
| identity | `DatabaseCurrentUserProfileReader` | 当前登录用户资料读取 | 低 |
| identity | `DatabaseUserCredentialReader` | 登录凭证和角色读取 | 中 |
| identity | `DatabaseUserMenuReader` | 后台菜单树读取 | 中 |
| identity | `DatabaseLoginAuditRecorder` | 登录成功审计写入 | 低 |
| content | `DatabaseArticleReader` | 文章列表、详情、归档、访问状态读取 | 高 |
| comment | `DatabaseCommentReader` | 前台评论和回复读取 | 中 |
| comment | `DatabaseCommentWriter` | 前台评论写入和提交校验 | 高 |
| comment | `DatabaseAdminCommentReader` | 后台评论列表和详情读取 | 高 |
| comment | `DatabaseAdminCommentModerator` | 后台审核、删除、恢复 | 中 |

测试代码中的 `JdbcTemplate` 主要用于准备测试数据、验证数据库状态或验证已迁移实现不再依赖 `JdbcTemplate`。测试侧不要求一次性删除，只有迁移对应测试时才按需调整。

已完成试迁移：

- `DatabaseContentCatalogReader` 已迁移到 `ContentCatalogMapper`。
- `CategoryEntity`、`TagEntity` 已放入 `content.infrastructure.persistence.entity`。
- `ContentCatalogMapper` 已放入 `content.infrastructure.persistence.mapper`。

---

## 2. 迁移规则

- 每个任务单独提交，提交信息必须使用中文。
- 每个迁移任务先写或补测试，确认旧实现会失败或能暴露迁移边界，再改生产代码。
- 不改 domain 端口方法签名，除非单独开设计讨论。
- 不改 Controller 响应结构。
- 不弱化已有测试断言。
- Entity 字段必须使用中文 Javadoc，写明对应表字段和中文含义。
- Mapper 必须继承 MyBatis-Plus `BaseMapper`。
- 多表聚合、动态条件、批量更新优先使用 Mapper 注解或 XML，不在 application 层拼 SQL。
- 迁移后对应生产实现类不得持有 `JdbcTemplate` 字段。
- `JdbcTemplate` 全量删除不作为单个任务目标，避免测试辅助代码和生产迁移混在一起。

---

## 3. 推荐迁移顺序

1. identity 当前用户资料读取。
2. identity 登录凭证和角色读取。
3. identity 菜单树读取。
4. identity 登录审计写入。
5. content 文章访问状态和文章详情读取。
6. content 文章列表、分类/标签列表、归档和推荐读取。
7. comment 前台评论读取。
8. comment 后台评论读取。
9. comment 后台审核、删除、恢复。
10. comment 前台评论写入。
11. 全量清理生产代码 `JdbcTemplate` 残留。

该顺序的理由：

- identity 表结构简单，适合继续沉淀 Entity/Mapper 规范。
- content 文章读取 SQL 多、聚合多，放在 identity 之后可以复用更稳定的 Mapper 模式。
- comment 写入和后台审核涉及状态流转、审计字段、批量更新，风险最高，放到最后。

---

## Task 1: 迁移当前用户资料读取

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/DatabaseCurrentUserProfileReader.java`
- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/UserAuthEntity.java`
- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/UserInfoEntity.java`
- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/IdentityUserMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/DatabaseCurrentUserProfileReaderTest.java`

- [ ] **Step 1: 补充迁移边界测试**

在 `DatabaseCurrentUserProfileReaderTest` 中增加断言：`DatabaseCurrentUserProfileReader` 不再持有 `JdbcTemplate`，并持有 `IdentityUserMapper`。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseCurrentUserProfileReaderTest" test
```

Expected: 新增测试先失败，失败原因是当前实现仍持有 `JdbcTemplate`。

- [ ] **Step 2: 新增用户认证和用户资料 Entity**

新增或复用：

- `UserAuthEntity` 对应 `t_user_auth`
- `UserInfoEntity` 对应 `t_user_info`

字段至少覆盖当前查询使用字段：`id`、`user_info_id`、`username`、`password`、`ip_address`、`last_login_time`、`nickname`、`avatar`、`email`、`is_disable`。

- [ ] **Step 3: 新增 `IdentityUserMapper`**

Mapper 放在：

```text
com.tyb.myblog.v2.identity.infrastructure.persistence.mapper
```

方法：

```java
Optional<CurrentUserProfile> findCurrentUserProfileByAuthId(@Param("authId") String authId);
```

- [ ] **Step 4: 替换 `DatabaseCurrentUserProfileReader`**

构造器改为注入 `IdentityUserMapper`，保留 `findByAuthId(String authId)` 的空值、空白字符串处理逻辑。

- [ ] **Step 5: 运行目标测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseCurrentUserProfileReaderTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/DatabaseCurrentUserProfileReaderTest.java
git commit -m "迁移当前用户资料读取到MyBatis-Plus"
```

---

## Task 2: 迁移登录凭证和角色读取

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/DatabaseUserCredentialReader.java`
- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/RoleEntity.java`
- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/UserRoleEntity.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/IdentityUserMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/DatabaseUserCredentialReaderTest.java`

- [ ] **Step 1: 补充迁移边界测试**

断言 `DatabaseUserCredentialReader` 不再持有 `JdbcTemplate`，并持有 `IdentityUserMapper`。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseUserCredentialReaderTest" test
```

Expected: 新增测试先失败。

- [ ] **Step 2: 新增角色相关 Entity**

新增或复用：

- `RoleEntity` 对应 `t_role`
- `UserRoleEntity` 对应 `t_user_role`

字段中文 Javadoc 必须写清 `role_name`、`is_disable`、`user_id`、`role_id` 的旧库含义。

- [ ] **Step 3: 扩展 `IdentityUserMapper`**

新增方法：

```java
Optional<UserCredentialRow> findCredentialByUsername(@Param("username") String username);
List<String> listEnabledRoleNamesByAuthId(@Param("authId") String authId);
```

`UserCredentialRow` 可以放在 mapper 包下，也可以作为 infrastructure 私有 record，不能进入 domain。

- [ ] **Step 4: 替换 `DatabaseUserCredentialReader`**

保留：

- username 空值返回 `Optional.empty()`
- 用户资料禁用时不可登录
- 禁用角色不参与权限计算
- `RoleNameMapper` 继续负责旧角色名到 V2 角色枚举的映射

- [ ] **Step 5: 运行目标测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseUserCredentialReaderTest,AuthServiceTest,AuthControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity
git commit -m "迁移登录凭证读取到MyBatis-Plus"
```

---

## Task 3: 迁移后台菜单读取

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/DatabaseUserMenuReader.java`
- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/MenuEntity.java`
- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/entity/RoleMenuEntity.java`
- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/IdentityMenuMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/DatabaseUserMenuReaderTest.java`

- [ ] **Step 1: 补充迁移边界测试**

断言 `DatabaseUserMenuReader` 不再持有 `JdbcTemplate`，并持有 `IdentityMenuMapper`。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseUserMenuReaderTest" test
```

Expected: 新增测试先失败。

- [ ] **Step 2: 新增菜单相关 Entity**

新增或复用：

- `MenuEntity` 对应 `t_menu`
- `RoleMenuEntity` 对应 `t_role_menu`

字段中文 Javadoc 必须覆盖 `path`、`component`、`icon`、`order_num`、`parent_id`、`is_hidden`。

- [ ] **Step 3: 新增 `IdentityMenuMapper`**

新增方法：

```java
List<MenuRow> listVisibleMenusByAuthId(@Param("authId") String authId);
```

SQL 保留原有 `distinct`、禁用角色过滤和排序规则。

- [ ] **Step 4: 替换 `DatabaseUserMenuReader`**

保留菜单树组装逻辑和 `Layout` 包裹叶子菜单的旧前端兼容规则。

- [ ] **Step 5: 运行目标测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseUserMenuReaderTest,AdminIdentityControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity
git commit -m "迁移后台菜单读取到MyBatis-Plus"
```

---

## Task 4: 迁移登录审计写入

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/DatabaseLoginAuditRecorder.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/infrastructure/persistence/mapper/IdentityUserMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity/DatabaseLoginAuditRecorderTest.java`

- [ ] **Step 1: 补充迁移边界测试**

断言 `DatabaseLoginAuditRecorder` 不再持有 `JdbcTemplate`，并持有 `IdentityUserMapper`。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseLoginAuditRecorderTest" test
```

Expected: 新增测试先失败。

- [ ] **Step 2: 扩展 `IdentityUserMapper`**

新增方法：

```java
int updateSuccessfulLoginAudit(@Param("authId") String authId, @Param("clientIp") String clientIp);
```

- [ ] **Step 3: 替换 `DatabaseLoginAuditRecorder`**

保留更新字段：

- `t_user_auth.last_login_time`
- `t_user_auth.ip_address`

- [ ] **Step 4: 运行目标测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseLoginAuditRecorderTest,AuthControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/identity
git commit -m "迁移登录审计写入到MyBatis-Plus"
```

---

## Task 5: 迁移文章访问状态和详情读取

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/DatabaseArticleReader.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/entity/ArticleEntity.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/entity/ArticleTagEntity.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/ArticleMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/DatabaseArticleReaderTest.java`

- [ ] **Step 1: 补充迁移边界测试**

断言 `DatabaseArticleReader` 不再持有 `JdbcTemplate`，并持有 `ArticleMapper`。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseArticleReaderTest" test
```

Expected: 新增测试先失败。

- [ ] **Step 2: 新增文章相关 Entity**

新增：

- `ArticleEntity` 对应 `t_article`
- `ArticleTagEntity` 对应 `t_article_tag`

字段中文 Javadoc 必须覆盖 `is_top`、`is_featured`、`is_delete`、`status`、`type`、`password`。

- [ ] **Step 3: 新增 `ArticleMapper` 基础读取方法**

先迁移：

```java
Optional<ArticleAccessCheck> findArticleAccessCheckById(@Param("articleId") int articleId);
List<ArticleDetailRow> listArticleDetailRows(@Param("articleId") int articleId, @Param("statuses") List<Integer> statuses);
```

- [ ] **Step 4: 替换详情相关读取**

只替换：

- `findArticleAccessCheckById`
- `findPublishedArticleById`
- `findAccessibleArticleById`

列表、归档、推荐先不动。

- [ ] **Step 5: 运行目标测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content
git commit -m "迁移文章详情读取到MyBatis-Plus"
```

---

## Task 6: 迁移文章列表、归档和推荐读取

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/DatabaseArticleReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/ArticleMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/DatabaseArticleReaderTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/ContentArticleControllerTest.java`

- [ ] **Step 1: 扩展 `ArticleMapper` 列表查询方法**

新增：

- 公开文章总数和 ID 分页查询
- 分类文章总数和 ID 分页查询
- 标签文章总数和 ID 分页查询
- 首页置顶和推荐文章 ID 查询
- 归档文章 ID 查询
- 按文章 ID 批量加载摘要行

- [ ] **Step 2: 替换列表、归档和推荐读取**

替换：

- `listPublishedArticles`
- `listPublishedArticlesByCategory`
- `listPublishedArticlesByTag`
- `findFeaturedArticles`
- `listPublishedArchives`
- `loadArticleSummaries`
- `loadArchiveArticles`

- [ ] **Step 3: 确认 `DatabaseArticleReader` 不再依赖 `JdbcTemplate`**

保留测试断言：`DatabaseArticleReader` 没有 `JdbcTemplate` 字段。

- [ ] **Step 4: 运行 content 全量测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=ContentArticleControllerTest,ContentCatalogControllerTest,DatabaseArticleReaderTest,DatabaseContentCatalogReaderTest,SignedArticleAccessTokenServiceTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content
git commit -m "迁移文章列表读取到MyBatis-Plus"
```

---

## Task 7: 迁移前台评论读取

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/DatabaseCommentReader.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/persistence/entity/CommentEntity.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/persistence/mapper/CommentMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/DatabaseCommentReaderTest.java`

- [ ] **Step 1: 补充迁移边界测试**

断言 `DatabaseCommentReader` 不再持有 `JdbcTemplate`，并持有 `CommentMapper`。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseCommentReaderTest" test
```

Expected: 新增测试先失败。

- [ ] **Step 2: 新增评论 Entity**

`CommentEntity` 对应 `t_comment`，字段中文 Javadoc 必须覆盖：

- `topic_id`
- `comment_content`
- `parent_id`
- `type`
- `is_delete`
- `is_review`
- `create_ip`
- `user_agent`
- `reviewed_by`
- `review_time`
- `deleted_by`
- `delete_time`
- `restored_by`
- `restore_time`

- [ ] **Step 3: 新增 `CommentMapper` 前台读取方法**

新增：

- 一级评论总数查询
- 一级评论分页查询
- 批量回复查询
- 热门评论查询

- [ ] **Step 4: 替换 `DatabaseCommentReader`**

保留前台过滤条件：

- `is_delete = 0`
- `is_review = 1`
- 一级评论 `parent_id is null`
- 留言板 `topic_id is null`

- [ ] **Step 5: 运行目标测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseCommentReaderTest,CommentControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment
git commit -m "迁移前台评论读取到MyBatis-Plus"
```

---

## Task 8: 迁移后台评论读取

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/DatabaseAdminCommentReader.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/persistence/mapper/CommentMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/DatabaseAdminCommentReaderTest.java`

- [ ] **Step 1: 补充迁移边界测试**

断言 `DatabaseAdminCommentReader` 不再持有 `JdbcTemplate`，并持有 `CommentMapper`。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseAdminCommentReaderTest" test
```

Expected: 新增测试先失败。

- [ ] **Step 2: 扩展 `CommentMapper` 后台读取方法**

新增：

- 后台评论总数动态查询
- 后台评论分页动态查询
- 后台评论详情查询

动态条件可以先使用 MyBatis 注解 `<script>`，如果 SQL 可读性下降，再迁移到 XML。

- [ ] **Step 3: 替换 `DatabaseAdminCommentReader`**

保留筛选条件：

- 删除状态
- 类型
- 主题 ID
- 审核状态
- 评论内容或昵称关键词

- [ ] **Step 4: 运行目标测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseAdminCommentReaderTest,AdminCommentControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment
git commit -m "迁移后台评论读取到MyBatis-Plus"
```

---

## Task 9: 迁移后台评论审核、删除、恢复

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/DatabaseAdminCommentModerator.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/persistence/mapper/CommentMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/DatabaseAdminCommentModeratorTest.java`

- [ ] **Step 1: 补充迁移边界测试**

断言 `DatabaseAdminCommentModerator` 不再持有 `JdbcTemplate`，并持有 `CommentMapper`。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseAdminCommentModeratorTest" test
```

Expected: 新增测试先失败。

- [ ] **Step 2: 扩展 `CommentMapper` 状态更新方法**

新增：

- 批量更新审核状态
- 批量软删除
- 批量恢复

批量 ID 最大数量和空 ID 校验仍保留在 `DatabaseAdminCommentModerator`。

- [ ] **Step 3: 替换 `DatabaseAdminCommentModerator`**

保留审计字段写入：

- `reviewed_by`
- `review_time`
- `deleted_by`
- `delete_time`
- `restored_by`
- `restore_time`
- `update_time`

- [ ] **Step 4: 运行目标测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseAdminCommentModeratorTest,AdminCommentControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment
git commit -m "迁移评论审核状态变更到MyBatis-Plus"
```

---

## Task 10: 迁移前台评论写入

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/DatabaseCommentWriter.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/infrastructure/persistence/mapper/CommentMapper.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/ArticleMapper.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/DatabaseCommentWriterTest.java`

- [ ] **Step 1: 补充迁移边界测试**

断言 `DatabaseCommentWriter` 不再持有 `JdbcTemplate`，并持有 `CommentMapper`。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseCommentWriterTest" test
```

Expected: 新增测试先失败。

- [ ] **Step 2: 扩展校验查询 Mapper**

需要 Mapper 支持：

- 校验文章是否存在且可评论
- 查询父评论
- 校验回复用户是否存在且未禁用

- [ ] **Step 3: 使用 MyBatis-Plus 插入评论**

可以通过 `CommentMapper.insert(CommentEntity entity)` 获取自增 ID。写入字段必须保留：

- `user_id`
- `reply_user_id`
- `topic_id`
- `comment_content`
- `parent_id`
- `type`
- `is_delete = 0`
- `is_review = 0`
- `create_ip`
- `user_agent`
- `create_time`
- `update_time`

- [ ] **Step 4: 替换 `DatabaseCommentWriter`**

保留全部业务校验：

- 说说评论暂不支持
- 评论内容不能为空
- 文章评论必须指定文章
- 文章不存在时报 `NOT_FOUND`
- 留言板不能指定主题
- 根评论不能指定回复用户
- 回复必须指定回复用户
- 只能回复根评论
- 回复类型和主题必须与父评论一致

- [ ] **Step 5: 运行目标测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseCommentWriterTest,CommentControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment
git commit -m "迁移前台评论写入到MyBatis-Plus"
```

---

## Task 11: 清理生产代码 JdbcTemplate 残留并验收

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/**`
- Modify: `MyBlog-springboot-v2/src/test/java/**`

- [ ] **Step 1: 搜索生产代码残留**

Run:

```powershell
rg "JdbcTemplate" MyBlog-springboot-v2/src/main/java -n
```

Expected: 只允许出现在文档注释中；生产实现类不得再持有 `JdbcTemplate` 字段。

- [ ] **Step 2: 搜索 Mapper 越层依赖**

Run:

```powershell
rg "infrastructure\\.persistence\\.mapper" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/*/web MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/*/application -n
```

Expected: 无输出。

- [ ] **Step 3: 运行 ArchUnit 测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=ArchitectureRulesTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 4: 运行全量测试**

Run:

```powershell
cd MyBlog-springboot-v2
mvn test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 5: 提交验收**

```powershell
git add MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java
git commit -m "完成后端V2生产持久层MyBatis-Plus迁移"
```

---

## 4. 每个模块的测试命令汇总

identity：

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=AuthControllerTest,AuthServiceTest,DatabaseCurrentUserProfileReaderTest,AdminIdentityControllerTest,DatabaseUserCredentialReaderTest,ConfiguredUserCredentialReaderTest,RoleNameMapperTest,DatabaseLoginAuditRecorderTest,DatabaseUserMenuReaderTest" test
```

content：

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=ContentArticleControllerTest,ContentCatalogControllerTest,DatabaseArticleReaderTest,DatabaseContentCatalogReaderTest,SignedArticleAccessTokenServiceTest" test
```

comment：

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=CommentControllerTest,AdminCommentControllerTest,DatabaseAdminCommentModeratorTest,DatabaseCommentWriterTest,DatabaseCommentReaderTest,DatabaseAdminCommentReaderTest" test
```

architecture：

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=ArchitectureRulesTest" test
```

full：

```powershell
cd MyBlog-springboot-v2
mvn test
```

所有命令预期结果均为 `BUILD SUCCESS`。
