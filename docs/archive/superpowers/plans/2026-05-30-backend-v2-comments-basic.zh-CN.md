# 后端 V2 评论与留言基础能力实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐个实现。步骤使用 checkbox（`- [ ]`）语法跟踪状态。

**目标：** 在后端 V2 中迁移前台评论基础能力：读取已审核评论、读取回复、读取最新评论、登录用户提交评论。

**架构：** 新增 `comment` 业务域，继续使用当前 V2 的 API / application / domain / infrastructure 分层。读取侧只返回 `is_review = 1` 且 `is_delete = 0` 的评论；写入侧只插入评论，不接入 Redis 限流、RabbitMQ 邮件通知、后台审核管理和图片表情扩展。

**技术栈：** Java 17, Spring Boot 3.5, JdbcTemplate, Spring Security, JUnit 5, AssertJ, MockMvc, H2, MySQL local smoke.

---

## 背景与边界

旧后端评论接口：

```text
POST /comments/save
GET /comments
GET /comments/{commentId}/replies
GET /comments/topSix
GET /admin/comments
PUT /admin/comments/review
DELETE /admin/comments
```

V2 本计划只迁移前台基础能力：

```text
GET /api/comments?type=1&topicId=1&page=1&size=10
GET /api/comments/{commentId}/replies
GET /api/comments/top
POST /api/comments
```

本计划不做：

- 不引入 Redis 限流；旧系统的 `@AccessLimit` 后续和限流能力一起规划。
- 不引入 RabbitMQ 和邮件通知；评论通知后续和消息/邮件基础设施一起规划。
- 不做后台评论审核接口；后台端会单独规划。
- 不迁移说说评论 `type = 5`；说说模块还没有迁移，当前提交说说评论返回参数错误。
- 不改真实 MySQL 表结构；只在 H2 测试迁移中补 `t_comment` 表和测试数据。

评论类型定义：

```text
1 = ARTICLE
2 = MESSAGE
3 = ABOUT
4 = LINK
5 = TALK
```

V2 当前支持提交目标：

- `ARTICLE`：必须传 `topicId`，目标文章必须存在且未删除，状态允许 `1` 公开文章或 `2` 密码文章。
- `MESSAGE`、`ABOUT`、`LINK`：不能传 `topicId`。
- `TALK`：本计划暂不支持，返回 `VALIDATION_ERROR`。

回复规则：

- 根评论：`parentId = null`，`replyUserId = null`。
- 回复：`parentId` 必须指向一条根评论，`replyUserId` 必须存在。
- 回复的 `type` 必须和根评论一致。
- 如果根评论是文章评论，回复的 `topicId` 必须和根评论一致。

## 文件结构

### 修改测试迁移

- `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`
  - 新增 `t_comment` 表。
  - 新增文章评论、留言评论、待审核评论、删除评论和回复测试数据。

### 新增生产代码

- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentType.java`
  - 评论类型枚举和数字编码转换。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentAuthor.java`
  - 评论作者展示信息。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentReply.java`
  - 回复展示模型。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentThread.java`
  - 根评论及其内嵌回复。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentPageQuery.java`
  - 评论分页与查询参数规范化。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentCreateCommand.java`
  - 提交评论命令。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentReader.java`
  - 评论读取端口。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentWriter.java`
  - 评论写入端口。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseCommentReader.java`
  - 基于旧表 `t_comment`、`t_user_info` 的读取实现。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseCommentWriter.java`
  - 基于旧表的写入实现和目标校验查询。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/CommentQueryService.java`
  - 公开读取编排。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/CommentCommandService.java`
  - 登录用户提交评论编排。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentController.java`
  - 评论 HTTP 接口。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentCreateRequest.java`
  - 提交评论请求 DTO。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentResponse.java`
  - 根评论响应 DTO。
- `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentReplyResponse.java`
  - 回复响应 DTO。

### 修改配置

- `MyBlog-springboot-v2/src/main/resources/application.yml`
  - 新增公开端点：`/api/comments`、`/api/comments/*/replies`、`/api/comments/top`。
- `MyBlog-springboot-v2/src/main/resources/application-local.yml`
  - 同步公开端点。
- `MyBlog-springboot-v2/src/test/resources/application-test.yml`
  - 同步公开端点。

### 新增测试代码

- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentReaderTest.java`
  - 覆盖评论列表、回复列表、最新评论读取。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentWriterTest.java`
  - 覆盖提交评论和参数拒绝。
- `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/CommentControllerTest.java`
  - 覆盖公开读取、登录提交、未登录拒绝。

## Task 1: 补充评论测试表和夹具

**Files:**

- Modify: `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/infrastructure/persistence/FlywayMigrationTest.java`

- [x] **Step 1: 新增失败检查**

在 `FlywayMigrationTest` 增加一个测试，确认测试库包含评论表和评论数据：

```java
@Test
void migratesLegacyCommentTablesForTests() {
    Integer count = jdbcTemplate.queryForObject("select count(*) from t_comment", Integer.class);

    assertThat(count).isNotNull();
    assertThat(count).isGreaterThanOrEqualTo(6);
}
```

- [x] **Step 2: 运行迁移测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=FlywayMigrationTest'
```

Expected:

```text
Table "T_COMMENT" not found
```

- [x] **Step 3: 新增 H2 测试表**

在 `V2__create_legacy_identity_tables_for_tests.sql` 的 `t_article_tag` 建表后追加：

```sql
create table t_comment (
    id int auto_increment primary key,
    user_id int not null,
    reply_user_id int,
    topic_id int,
    comment_content varchar(1024) not null,
    parent_id int,
    type tinyint not null,
    is_delete tinyint not null default 0,
    is_review tinyint not null default 1,
    create_time timestamp not null,
    update_time timestamp
);
```

- [x] **Step 4: 新增 H2 评论数据**

在 `t_article_tag` 测试数据后追加：

```sql
insert into t_comment (
    id, user_id, reply_user_id, topic_id, comment_content,
    parent_id, type, is_delete, is_review, create_time, update_time
)
values
    (1, 2, null, 1, '第一条文章评论', null, 1, 0, 1, timestamp '2026-05-29 10:00:00', timestamp '2026-05-29 10:00:00'),
    (2, 1, 2, 1, '管理员回复普通用户', 1, 1, 0, 1, timestamp '2026-05-29 10:05:00', timestamp '2026-05-29 10:05:00'),
    (3, 2, null, 1, '待审核评论', null, 1, 0, 0, timestamp '2026-05-29 10:10:00', timestamp '2026-05-29 10:10:00'),
    (4, 2, null, 1, '已删除评论', null, 1, 1, 1, timestamp '2026-05-29 10:15:00', timestamp '2026-05-29 10:15:00'),
    (5, 2, null, null, '留言板第一条', null, 2, 0, 1, timestamp '2026-05-29 11:00:00', timestamp '2026-05-29 11:00:00'),
    (6, 1, 2, null, '留言板回复', 5, 2, 0, 1, timestamp '2026-05-29 11:05:00', timestamp '2026-05-29 11:05:00');
```

- [x] **Step 5: 运行迁移测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=FlywayMigrationTest'
```

Expected:

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [x] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/infrastructure/persistence/FlywayMigrationTest.java
git commit -m "补充后端V2评论测试数据"
```

## Task 2: 新增评论读取领域和数据库实现

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentType.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentAuthor.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentReply.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentThread.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentPageQuery.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentReader.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseCommentReader.java`
- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentReaderTest.java`

- [x] **Step 1: 写失败测试**

Create `DatabaseCommentReaderTest.java`:

```java
package com.aurora.myblog.v2.modules.comment;

import com.aurora.myblog.v2.modules.comment.domain.CommentPageQuery;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import com.aurora.myblog.v2.modules.comment.infrastructure.DatabaseCommentReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseCommentReader.class)
class DatabaseCommentReaderTest {

    @Autowired
    private DatabaseCommentReader reader;

    @Test
    void listsApprovedArticleCommentsWithReplies() {
        var page = reader.listComments(CommentType.ARTICLE, 1, new CommentPageQuery(1, 10));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).extracting("id").containsExactly(1);
        assertThat(page.records().get(0).content()).isEqualTo("第一条文章评论");
        assertThat(page.records().get(0).author().nickname()).isEqualTo("普通用户");
        assertThat(page.records().get(0).replies()).extracting("id").containsExactly(2);
        assertThat(page.records().get(0).replies().get(0).replyUser().nickname()).isEqualTo("普通用户");
    }

    @Test
    void listsMessageCommentsWithoutTopicId() {
        var page = reader.listComments(CommentType.MESSAGE, null, new CommentPageQuery(1, 10));

        assertThat(page.total()).isEqualTo(1);
        assertThat(page.records()).extracting("id").containsExactly(5);
        assertThat(page.records().get(0).replies()).extracting("id").containsExactly(6);
    }

    @Test
    void listsRepliesByRootCommentId() {
        var replies = reader.listRepliesByCommentId(1);

        assertThat(replies).extracting("id").containsExactly(2);
        assertThat(replies.get(0).content()).isEqualTo("管理员回复普通用户");
    }

    @Test
    void listsLatestApprovedRootComments() {
        var comments = reader.listTopComments(6);

        assertThat(comments).extracting("id").containsExactly(5, 1);
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseCommentReaderTest'
```

Expected:

```text
Compilation failure: package com.aurora.myblog.v2.modules.comment does not exist
```

- [x] **Step 3: 新增领域类型**

Create `CommentType.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;

public enum CommentType {
    ARTICLE(1),
    MESSAGE(2),
    ABOUT(3),
    LINK(4),
    TALK(5);

    private final int code;

    CommentType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static CommentType fromCode(Integer code) {
        if (code == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论类型不能为空");
        }
        for (CommentType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论类型不支持");
    }

    public boolean requiresTopic() {
        return this == ARTICLE || this == TALK;
    }

    public boolean forbidsTopic() {
        return this == MESSAGE || this == ABOUT || this == LINK;
    }
}
```

Create `CommentAuthor.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

public record CommentAuthor(int id, String nickname, String avatar, String website) {
}
```

Create `CommentReply.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

import java.time.LocalDateTime;

public record CommentReply(
        int id,
        int parentId,
        CommentAuthor author,
        CommentAuthor replyUser,
        String content,
        LocalDateTime createdAt
) {
}
```

Create `CommentThread.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

import java.time.LocalDateTime;
import java.util.List;

public record CommentThread(
        int id,
        CommentType type,
        Integer topicId,
        CommentAuthor author,
        String content,
        LocalDateTime createdAt,
        List<CommentReply> replies
) {
    public CommentThread {
        replies = replies == null ? List.of() : List.copyOf(replies);
    }
}
```

Create `CommentPageQuery.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

public record CommentPageQuery(int page, int size) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    public static CommentPageQuery of(Integer page, Integer size) {
        int safePage = page == null ? DEFAULT_PAGE : Math.max(page, 1);
        int safeSize = size == null ? DEFAULT_SIZE : Math.max(size, 1);
        return new CommentPageQuery(safePage, Math.min(safeSize, MAX_SIZE));
    }

    public int offset() {
        return (page - 1) * size;
    }
}
```

Create `CommentReader.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

import com.aurora.myblog.v2.common.web.PageResponse;

import java.util.List;

public interface CommentReader {

    PageResponse<CommentThread> listComments(CommentType type, Integer topicId, CommentPageQuery query);

    List<CommentReply> listRepliesByCommentId(int commentId);

    List<CommentThread> listTopComments(int limit);
}
```

- [x] **Step 4: 实现数据库读取**

Create `DatabaseCommentReader.java`:

```java
package com.aurora.myblog.v2.modules.comment.infrastructure;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.domain.CommentAuthor;
import com.aurora.myblog.v2.modules.comment.domain.CommentPageQuery;
import com.aurora.myblog.v2.modules.comment.domain.CommentReader;
import com.aurora.myblog.v2.modules.comment.domain.CommentReply;
import com.aurora.myblog.v2.modules.comment.domain.CommentThread;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class DatabaseCommentReader implements CommentReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCommentReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResponse<CommentThread> listComments(CommentType type, Integer topicId, CommentPageQuery query) {
        Long total = jdbcTemplate.queryForObject("""
                select count(*)
                from t_comment c
                where c.is_delete = 0
                  and c.is_review = 1
                  and c.parent_id is null
                  and c.type = ?
                  and ((? is null and c.topic_id is null) or c.topic_id = ?)
                """, Long.class, type.code(), topicId, topicId);
        List<CommentThread> roots = loadRootComments(type, topicId, query);
        return new PageResponse<>(attachReplies(roots), total == null ? 0 : total, query.page(), query.size());
    }

    @Override
    public List<CommentReply> listRepliesByCommentId(int commentId) {
        return loadReplies(List.of(commentId));
    }

    @Override
    public List<CommentThread> listTopComments(int limit) {
        List<CommentThread> roots = jdbcTemplate.query("""
                        select c.id,
                               c.type,
                               c.topic_id,
                               c.comment_content,
                               c.create_time,
                               u.id as user_id,
                               u.nickname,
                               u.avatar,
                               u.website
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        where c.is_delete = 0
                          and c.is_review = 1
                          and c.parent_id is null
                        order by c.id desc
                        limit ?
                        """,
                (rs, rowNum) -> toThread(new RootRow(
                        rs.getInt("id"),
                        CommentType.fromCode(rs.getInt("type")),
                        (Integer) rs.getObject("topic_id"),
                        rs.getString("comment_content"),
                        toLocalDateTime(rs.getTimestamp("create_time")),
                        rs.getInt("user_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("website"))),
                Math.max(1, Math.min(limit, 20)));
        return attachReplies(roots);
    }

    private List<CommentThread> loadRootComments(CommentType type, Integer topicId, CommentPageQuery query) {
        return jdbcTemplate.query("""
                        select c.id,
                               c.type,
                               c.topic_id,
                               c.comment_content,
                               c.create_time,
                               u.id as user_id,
                               u.nickname,
                               u.avatar,
                               u.website
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        where c.is_delete = 0
                          and c.is_review = 1
                          and c.parent_id is null
                          and c.type = ?
                          and ((? is null and c.topic_id is null) or c.topic_id = ?)
                        order by c.id desc
                        limit ? offset ?
                        """,
                (rs, rowNum) -> toThread(new RootRow(
                        rs.getInt("id"),
                        CommentType.fromCode(rs.getInt("type")),
                        (Integer) rs.getObject("topic_id"),
                        rs.getString("comment_content"),
                        toLocalDateTime(rs.getTimestamp("create_time")),
                        rs.getInt("user_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("website"))),
                type.code(), topicId, topicId, query.size(), query.offset());
    }

    private List<CommentThread> attachReplies(List<CommentThread> roots) {
        if (roots.isEmpty()) {
            return List.of();
        }
        List<Integer> rootIds = roots.stream().map(CommentThread::id).toList();
        Map<Integer, List<CommentReply>> repliesByRootId = loadReplies(rootIds).stream()
                .collect(Collectors.groupingBy(CommentReply::parentId, LinkedHashMap::new, Collectors.toList()));
        return roots.stream()
                .map(root -> new CommentThread(
                        root.id(),
                        root.type(),
                        root.topicId(),
                        root.author(),
                        root.content(),
                        root.createdAt(),
                        repliesByRootId.getOrDefault(root.id(), List.of())))
                .toList();
    }

    private List<CommentReply> loadReplies(List<Integer> parentIds) {
        if (parentIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", parentIds.stream().map(id -> "?").toList());
        return jdbcTemplate.query("""
                        select c.id,
                               c.parent_id,
                               c.comment_content,
                               c.create_time,
                               u.id as user_id,
                               u.nickname,
                               u.avatar,
                               u.website,
                               r.id as reply_user_id,
                               r.nickname as reply_nickname,
                               r.avatar as reply_avatar,
                               r.website as reply_website
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        join t_user_info r on r.id = c.reply_user_id
                        where c.is_delete = 0
                          and c.is_review = 1
                          and c.parent_id in (%s)
                        order by c.create_time asc, c.id asc
                        """.formatted(placeholders),
                (rs, rowNum) -> new CommentReply(
                        rs.getInt("id"),
                        rs.getInt("parent_id"),
                        new CommentAuthor(
                                rs.getInt("user_id"),
                                rs.getString("nickname"),
                                rs.getString("avatar"),
                                rs.getString("website")),
                        new CommentAuthor(
                                rs.getInt("reply_user_id"),
                                rs.getString("reply_nickname"),
                                rs.getString("reply_avatar"),
                                rs.getString("reply_website")),
                        rs.getString("comment_content"),
                        toLocalDateTime(rs.getTimestamp("create_time"))),
                parentIds.toArray());
    }

    private CommentThread toThread(RootRow row) {
        return new CommentThread(
                row.id(),
                row.type(),
                row.topicId(),
                new CommentAuthor(row.userId(), row.nickname(), row.avatar(), row.website()),
                row.content(),
                row.createdAt(),
                List.of());
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record RootRow(
            int id,
            CommentType type,
            Integer topicId,
            String content,
            LocalDateTime createdAt,
            int userId,
            String nickname,
            String avatar,
            String website
    ) {
    }
}
```

- [x] **Step 5: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseCommentReaderTest'
```

Expected:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [x] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentReaderTest.java
git commit -m "新增后端V2评论读取能力"
```

## Task 3: 新增评论公开读取 API

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/CommentQueryService.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentController.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentResponse.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentReplyResponse.java`
- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`
- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`
- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/CommentControllerTest.java`

- [x] **Step 1: 写失败测试**

Create `CommentControllerTest.java`:

```java
package com.aurora.myblog.v2.modules.comment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsArticleCommentsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/comments?type=1&topicId=1&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].content").value("第一条文章评论"))
                .andExpect(jsonPath("$.data.records[0].replies[0].id").value(2));
    }

    @Test
    void returnsMessageCommentsWithoutTopicId() throws Exception {
        mockMvc.perform(get("/api/comments?type=2&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(5));
    }

    @Test
    void returnsRepliesByCommentIdWithoutToken() throws Exception {
        mockMvc.perform(get("/api/comments/1/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].replyUser.nickname").value("普通用户"));
    }

    @Test
    void returnsTopCommentsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/comments/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(5))
                .andExpect(jsonPath("$.data[1].id").value(1));
    }

    @Test
    void rejectsUnsupportedCommentType() throws Exception {
        mockMvc.perform(get("/api/comments?type=99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=CommentControllerTest'
```

Expected:

```text
Status expected:<200> but was:<404>
```

- [x] **Step 3: 新增应用查询服务**

Create `CommentQueryService.java`:

```java
package com.aurora.myblog.v2.modules.comment.application;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.domain.CommentPageQuery;
import com.aurora.myblog.v2.modules.comment.domain.CommentReader;
import com.aurora.myblog.v2.modules.comment.domain.CommentReply;
import com.aurora.myblog.v2.modules.comment.domain.CommentThread;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentQueryService {

    private final CommentReader commentReader;

    public CommentQueryService(CommentReader commentReader) {
        this.commentReader = commentReader;
    }

    public PageResponse<CommentThread> listComments(Integer type, Integer topicId, Integer page, Integer size) {
        CommentType commentType = CommentType.fromCode(type);
        return commentReader.listComments(commentType, topicId, CommentPageQuery.of(page, size));
    }

    public List<CommentReply> listRepliesByCommentId(int commentId) {
        return commentReader.listRepliesByCommentId(commentId);
    }

    public List<CommentThread> listTopComments() {
        return commentReader.listTopComments(6);
    }
}
```

- [x] **Step 4: 新增响应 DTO**

Create `CommentReplyResponse.java`:

```java
package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.modules.comment.domain.CommentAuthor;
import com.aurora.myblog.v2.modules.comment.domain.CommentReply;

import java.time.LocalDateTime;

public record CommentReplyResponse(
        int id,
        int parentId,
        Author author,
        Author replyUser,
        String content,
        LocalDateTime createdAt
) {
    static CommentReplyResponse from(CommentReply reply) {
        return new CommentReplyResponse(
                reply.id(),
                reply.parentId(),
                Author.from(reply.author()),
                Author.from(reply.replyUser()),
                reply.content(),
                reply.createdAt());
    }

    public record Author(int id, String nickname, String avatar, String website) {
        static Author from(CommentAuthor author) {
            return new Author(author.id(), author.nickname(), author.avatar(), author.website());
        }
    }
}
```

Create `CommentResponse.java`:

```java
package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.modules.comment.domain.CommentAuthor;
import com.aurora.myblog.v2.modules.comment.domain.CommentThread;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        int id,
        int type,
        Integer topicId,
        Author author,
        String content,
        LocalDateTime createdAt,
        List<CommentReplyResponse> replies
) {
    static CommentResponse from(CommentThread comment) {
        return new CommentResponse(
                comment.id(),
                comment.type().code(),
                comment.topicId(),
                Author.from(comment.author()),
                comment.content(),
                comment.createdAt(),
                comment.replies().stream().map(CommentReplyResponse::from).toList());
    }

    public record Author(int id, String nickname, String avatar, String website) {
        static Author from(CommentAuthor author) {
            return new Author(author.id(), author.nickname(), author.avatar(), author.website());
        }
    }
}
```

- [x] **Step 5: 新增 Controller**

Create `CommentController.java`:

```java
package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.application.CommentQueryService;
import com.aurora.myblog.v2.modules.comment.domain.CommentThread;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CommentController {

    private final CommentQueryService commentQueryService;

    public CommentController(CommentQueryService commentQueryService) {
        this.commentQueryService = commentQueryService;
    }

    @GetMapping("/api/comments")
    public ApiResponse<PageResponse<CommentResponse>> listComments(
            @RequestParam Integer type,
            @RequestParam(required = false) Integer topicId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(mapPage(commentQueryService.listComments(type, topicId, page, size)));
    }

    @GetMapping("/api/comments/{commentId}/replies")
    public ApiResponse<List<CommentReplyResponse>> listReplies(@PathVariable int commentId) {
        return ApiResponse.ok(commentQueryService.listRepliesByCommentId(commentId).stream()
                .map(CommentReplyResponse::from)
                .toList());
    }

    @GetMapping("/api/comments/top")
    public ApiResponse<List<CommentResponse>> listTopComments() {
        return ApiResponse.ok(commentQueryService.listTopComments().stream()
                .map(CommentResponse::from)
                .toList());
    }

    private PageResponse<CommentResponse> mapPage(PageResponse<CommentThread> page) {
        return new PageResponse<>(
                page.records().stream().map(CommentResponse::from).toList(),
                page.total(),
                page.page(),
                page.size());
    }
}
```

- [x] **Step 6: 新增公开端点配置**

在三个配置文件的 `myblog.security.public-endpoints` 中追加：

```yaml
      - /api/comments
      - /api/comments/*/replies
      - /api/comments/top
```

- [x] **Step 7: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=CommentControllerTest,DatabaseCommentReaderTest'
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [x] **Step 8: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment MyBlog-springboot-v2/src/main/resources/application.yml MyBlog-springboot-v2/src/main/resources/application-local.yml MyBlog-springboot-v2/src/test/resources/application-test.yml MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/CommentControllerTest.java
git commit -m "新增后端V2评论公开读取接口"
```

## Task 4: 支持登录用户提交评论

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentCreateCommand.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentWriter.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseCommentWriter.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/CommentCommandService.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentCreateRequest.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentController.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentWriterTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/CommentControllerTest.java`

- [x] **Step 1: 写失败测试**

Create `DatabaseCommentWriterTest.java`:

```java
package com.aurora.myblog.v2.modules.comment;

import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.comment.domain.CommentCreateCommand;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import com.aurora.myblog.v2.modules.comment.infrastructure.DatabaseCommentWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@JdbcTest
@Import(DatabaseCommentWriter.class)
class DatabaseCommentWriterTest {

    @Autowired
    private DatabaseCommentWriter writer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesArticleRootCommentAsPendingReview() {
        int id = writer.save(new CommentCreateCommand(
                2,
                CommentType.ARTICLE,
                1,
                null,
                null,
                "新的文章评论"));

        Integer isReview = jdbcTemplate.queryForObject("select is_review from t_comment where id = ?", Integer.class, id);
        String content = jdbcTemplate.queryForObject("select comment_content from t_comment where id = ?", String.class, id);

        assertThat(isReview).isZero();
        assertThat(content).isEqualTo("新的文章评论");
    }

    @Test
    void rejectsArticleCommentWithoutTopicId() {
        assertThatThrownBy(() -> writer.save(new CommentCreateCommand(
                2,
                CommentType.ARTICLE,
                null,
                null,
                null,
                "缺少文章")))
                .isInstanceOf(ApiException.class)
                .hasMessage("文章评论必须指定文章");
    }

    @Test
    void rejectsReplyToNonRootComment() {
        assertThatThrownBy(() -> writer.save(new CommentCreateCommand(
                2,
                CommentType.ARTICLE,
                1,
                2,
                1,
                "不能回复回复")))
                .isInstanceOf(ApiException.class)
                .hasMessage("只能回复根评论");
    }
}
```

在 `CommentControllerTest` 追加：

```java
@Test
void saveCommentRequiresLogin() throws Exception {
    mockMvc.perform(post("/api/comments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"type":1,"topicId":1,"content":"新的文章评论"}
                            """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
}

@Test
void loggedInUserCanSaveArticleComment() throws Exception {
    String response = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"username":"user@163.com","password":"password123"}
                            """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(response, "$.data.accessToken");

    mockMvc.perform(post("/api/comments")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"type":1,"topicId":1,"content":"新的文章评论"}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").isNumber())
            .andExpect(jsonPath("$.data.review").value(false));
}
```

同时给 `CommentControllerTest` 增加 import：

```java
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
```

- [x] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseCommentWriterTest,CommentControllerTest'
```

Expected:

```text
Compilation failure: cannot find symbol CommentCreateCommand
```

- [x] **Step 3: 新增写入领域端口**

Create `CommentCreateCommand.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

public record CommentCreateCommand(
        int userId,
        CommentType type,
        Integer topicId,
        Integer parentId,
        Integer replyUserId,
        String content
) {
}
```

Create `CommentWriter.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

public interface CommentWriter {

    int save(CommentCreateCommand command);
}
```

- [x] **Step 4: 实现数据库写入**

Create `DatabaseCommentWriter.java`:

```java
package com.aurora.myblog.v2.modules.comment.infrastructure;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.comment.domain.CommentCreateCommand;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import com.aurora.myblog.v2.modules.comment.domain.CommentWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
public class DatabaseCommentWriter implements CommentWriter {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseCommentWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int save(CommentCreateCommand command) {
        validate(command);
        LocalDateTime now = LocalDateTime.now();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    insert into t_comment (
                        user_id, reply_user_id, topic_id, comment_content,
                        parent_id, type, is_delete, is_review, create_time, update_time
                    )
                    values (?, ?, ?, ?, ?, ?, 0, 0, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, command.userId());
            setNullableInt(ps, 2, command.replyUserId());
            setNullableInt(ps, 3, command.topicId());
            ps.setString(4, command.content().trim());
            setNullableInt(ps, 5, command.parentId());
            ps.setInt(6, command.type().code());
            ps.setTimestamp(7, Timestamp.valueOf(now));
            ps.setTimestamp(8, Timestamp.valueOf(now));
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR, "评论保存失败");
        }
        return key.intValue();
    }

    private void validate(CommentCreateCommand command) {
        if (command.type() == CommentType.TALK) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "说说评论暂未支持");
        }
        if (command.content() == null || command.content().isBlank()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论内容不能为空");
        }
        if (command.type() == CommentType.ARTICLE) {
            validateArticleTarget(command.topicId());
        }
        if (command.type().forbidsTopic() && command.topicId() != null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "该评论类型不能指定主题");
        }
        if (command.parentId() == null && command.replyUserId() != null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "根评论不能指定回复用户");
        }
        if (command.parentId() != null) {
            validateReply(command);
        }
    }

    private void validateArticleTarget(Integer topicId) {
        if (topicId == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "文章评论必须指定文章");
        }
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from t_article
                where id = ?
                  and is_delete = 0
                  and status in (1, 2)
                """, Integer.class, topicId);
        if (count == null || count == 0) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在");
        }
    }

    private void validateReply(CommentCreateCommand command) {
        if (command.replyUserId() == null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "回复评论必须指定回复用户");
        }
        List<ParentComment> parents = jdbcTemplate.query("""
                        select id, parent_id, topic_id, type
                        from t_comment
                        where id = ?
                          and is_delete = 0
                        """,
                (rs, rowNum) -> new ParentComment(
                        rs.getInt("id"),
                        (Integer) rs.getObject("parent_id"),
                        (Integer) rs.getObject("topic_id"),
                        rs.getInt("type")),
                command.parentId());
        ParentComment parent = parents.stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "父评论不存在"));
        if (parent.parentId() != null) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "只能回复根评论");
        }
        if (parent.type() != command.type().code()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "回复类型必须和父评论一致");
        }
        if (!Objects.equals(parent.topicId(), command.topicId())) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "回复主题必须和父评论一致");
        }
        Integer userCount = jdbcTemplate.queryForObject("""
                select count(*)
                from t_user_info
                where id = ?
                  and is_disable = 0
                """, Integer.class, command.replyUserId());
        if (userCount == null || userCount == 0) {
            throw new ApiException(ApiErrorCode.NOT_FOUND, "回复用户不存在");
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws java.sql.SQLException {
        if (value == null) {
            ps.setObject(index, null);
        } else {
            ps.setInt(index, value);
        }
    }

    private record ParentComment(int id, Integer parentId, Integer topicId, int type) {
    }
}
```

- [x] **Step 5: 新增应用服务和请求 DTO**

Create `CommentCommandService.java`:

```java
package com.aurora.myblog.v2.modules.comment.application;

import com.aurora.myblog.v2.modules.comment.domain.CommentCreateCommand;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import com.aurora.myblog.v2.modules.comment.domain.CommentWriter;
import org.springframework.stereotype.Service;

@Service
public class CommentCommandService {

    private final CommentWriter commentWriter;

    public CommentCommandService(CommentWriter commentWriter) {
        this.commentWriter = commentWriter;
    }

    public CommentCreateResult createComment(String userId,
                                             Integer type,
                                             Integer topicId,
                                             Integer parentId,
                                             Integer replyUserId,
                                             String content) {
        int id = commentWriter.save(new CommentCreateCommand(
                Integer.parseInt(userId),
                CommentType.fromCode(type),
                topicId,
                parentId,
                replyUserId,
                content));
        return new CommentCreateResult(id, false);
    }

    public record CommentCreateResult(int id, boolean review) {
    }
}
```

Create `CommentCreateRequest.java`:

```java
package com.aurora.myblog.v2.modules.comment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequest(
        @NotNull(message = "评论类型不能为空")
        Integer type,
        Integer topicId,
        Integer parentId,
        Integer replyUserId,
        @NotBlank(message = "评论内容不能为空")
        String content
) {
}
```

- [x] **Step 6: 修改 Controller 支持提交**

修改 `CommentController` 构造参数和字段：

```java
private final CommentQueryService commentQueryService;
private final CommentCommandService commentCommandService;

public CommentController(CommentQueryService commentQueryService,
                         CommentCommandService commentCommandService) {
    this.commentQueryService = commentQueryService;
    this.commentCommandService = commentCommandService;
}
```

新增 import：

```java
import com.aurora.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.aurora.myblog.v2.common.auth.CurrentUser;
import com.aurora.myblog.v2.modules.comment.application.CommentCommandService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
```

新增接口：

```java
@PostMapping("/api/comments")
public ApiResponse<CommentCommandService.CommentCreateResult> saveComment(
        @CurrentUser AuthenticatedPrincipal currentUser,
        @Valid @RequestBody CommentCreateRequest request) {
    return ApiResponse.ok(commentCommandService.createComment(
            currentUser.id(),
            request.type(),
            request.topicId(),
            request.parentId(),
            request.replyUserId(),
            request.content()));
}
```

- [x] **Step 7: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseCommentWriterTest,CommentControllerTest'
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [x] **Step 8: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment
git commit -m "支持后端V2登录用户提交评论"
```

## Task 5: 全量验证和计划状态同步

**Files:**

- Modify: `docs/superpowers/plans/2026-05-30-backend-v2-comments-basic.zh-CN.md`

- [x] **Step 1: 全量测试**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [x] **Step 2: 打包**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

Expected:

```text
BUILD SUCCESS
```

- [x] **Step 3: 本地 MySQL 只读检查**

不要把本地密码写进文件。命令只读取当前 PowerShell 会话环境变量：

```powershell
if (-not $env:MYSQL_PWD) { throw 'MYSQL_PWD is required' }
mysql -h localhost -P 3306 -u root -N -e "select count(*) from aurora.t_comment;"
mysql -h localhost -P 3306 -u root -N -e "select id, type, topic_id, parent_id, is_review, is_delete from aurora.t_comment order by id desc limit 5;"
```

Expected:

```text
SQL 执行成功；如果本地库已有评论，应看到评论总数和最近评论行。
```

- [x] **Step 4: 本地 API 冒烟**

启动服务时通过环境变量注入数据库密码：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
if (-not $env:MYBLOG_DATASOURCE_PASSWORD) { throw 'MYBLOG_DATASOURCE_PASSWORD is required' }
mvn -f MyBlog-springboot-v2/pom.xml spring-boot:run
```

调用公开读取接口：

```powershell
Invoke-WebRequest -Uri 'http://localhost:8080/actuator/health' -UseBasicParsing
Invoke-WebRequest -Uri 'http://localhost:8080/api/comments/top' -UseBasicParsing
Invoke-WebRequest -Uri 'http://localhost:8080/api/comments?type=2&page=1&size=10' -UseBasicParsing
```

Expected:

```text
health 返回 200；评论读取接口返回 200 或空分页，不能返回 401/403/500。
```

提交评论接口需要真实可登录账号；如果本地库密码哈希和测试账号不一致，API 冒烟只记录“本地库登录账号不确定，跳过真实提交冒烟”，不能伪造通过。

- [x] **Step 5: 更新本计划的执行结果**

先读取实际提交记录：

```powershell
git log --oneline -8
```

再在本文档末尾追加真实执行结果。提交记录必须复制实际短 SHA 和提交信息。

- [x] **Step 6: 提交**

```powershell
git add docs/superpowers/plans/2026-05-30-backend-v2-comments-basic.zh-CN.md
git commit -m "同步后端V2评论计划状态"
```

## 自检结果

- 覆盖范围：计划覆盖测试数据、评论公开读取、回复读取、最新评论、登录提交评论和完整验证。
- 边界控制：不引入 Redis、RabbitMQ、邮件通知、后台审核接口，不改真实表结构。
- 类型一致性：`CommentType`、`CommentThread`、`CommentReply`、`CommentReader`、`CommentWriter`、DTO 和 Controller 方法命名一致。
- 安全控制：公开读取接口加入 public endpoints；提交接口不加入 public endpoints，必须依赖 `@CurrentUser`。
- 占位扫描：文档没有待替换占位词；本地数据库密码只通过环境变量读取，不能写入代码、文档或 Git。

## 执行结果

- Task 1：已完成。提交 `33fe938 补充后端V2评论测试数据`，补充 `t_comment` 测试表、评论测试数据和 Flyway 迁移验证。
- Task 2：已完成。提交 `7308db0 新增后端V2评论读取能力`，新增评论领域模型、读取接口和数据库实现。
- Task 3：已完成。提交 `e4914a2 新增后端V2评论公开读取接口`，新增评论公开读取 Controller、DTO 和接口测试。
- Task 4：已完成。提交 `8960b0d 支持后端V2登录用户提交评论`，新增登录用户提交评论能力和写入校验。
- Task 5：已完成。`mvn -f MyBlog-springboot-v2/pom.xml test` 通过，结果为 `Tests run: 99, Failures: 0, Errors: 0, Skipped: 0`。
- Task 5：已完成。`mvn -f MyBlog-springboot-v2/pom.xml clean package` 通过，已生成 `MyBlog-springboot-v2/target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar`。
- Task 5：已完成。本地 MySQL 只读检查通过，`aurora.t_comment` 当前为 `0` 行；最近评论查询因表为空没有返回行。
- Task 5：已完成。本地 API 冒烟通过：`/actuator/health`、`/api/comments/top`、`/api/comments?type=2&page=1&size=10` 均返回 `200`；评论接口因本地表为空返回空数组/空分页。
- Task 5：真实提交评论 API 冒烟未执行。本地库没有可确认的真实登录账号和评论业务数据，避免伪造通过；该能力已由 `CommentControllerTest` 和 `DatabaseCommentWriterTest` 覆盖。
