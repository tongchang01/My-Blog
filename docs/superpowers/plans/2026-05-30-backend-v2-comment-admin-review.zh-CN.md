# 后端 V2 评论审核管理实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐个实现。步骤使用 checkbox（`- [ ]`）语法跟踪状态。

**目标：** 在后端 V2 中补齐后台评论管理闭环：管理员分页查询评论、按审核状态筛选、审核/取消审核评论、软删除评论。

**架构：** 继续沿用当前 V2 的模块化单体结构，把后台评论管理放在 `modules/comment` 内，不新建微服务，不引入 Redis、MQ 或搜索引擎。后台接口走 `/api/admin/**`，复用现有 `SecurityConfig` 的 `hasRole("ADMIN")` 保护；数据访问继续使用 `JdbcTemplate`，先兼容现有 `t_comment` 表结构。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring Security、JdbcTemplate、JUnit 5、MockMvc、H2 测试迁移、MySQL 本地冒烟。

---

## 边界和决策

- 本计划只做后端 V2 的后台评论审核管理，不改前台 Vue，不改后台 Vue，不做部署。
- 本计划不改真实 MySQL 表结构；当前 `t_comment.is_review` 和 `t_comment.is_delete` 已能支撑审核和软删除。
- 本计划不发送邮件通知，不做站内通知，不接入 Redis 缓存，不接入 RabbitMQ。
- 后台查询默认只展示未删除评论；如果要恢复已删除评论，另开计划处理。
- 审核语义：`reviewed=true` 表示 `is_review = 1`；`reviewed=false` 表示 `is_review = 0`。
- 删除语义：只软删除，更新 `is_delete = 1` 和 `update_time`，不物理删除行。
- 管理员权限依赖现有规则：`/api/admin/**` 必须是 `ROLE_ADMIN`，普通登录用户返回 `403`，未登录返回 `401`。

## 文件结构

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentQuery.java`
  - 后台评论查询条件，负责分页边界和筛选条件归一化。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentItem.java`
  - 后台评论列表项，包含评论状态、作者、目标文章和回复关系。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentReader.java`
  - 后台评论读取端口。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerationCommand.java`
  - 审核/取消审核批量命令。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDeletionCommand.java`
  - 软删除批量命令。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerator.java`
  - 后台评论审核和软删除端口。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentReader.java`
  - 使用 `JdbcTemplate` 查询后台评论列表。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentModerator.java`
  - 使用 `JdbcTemplate` 批量更新 `is_review`、`is_delete`。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentQueryService.java`
  - 后台评论查询应用服务。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentCommandService.java`
  - 后台评论审核/删除应用服务。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentController.java`
  - 暴露 `/api/admin/comments` 后台接口。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentResponse.java`
  - 后台评论列表响应 DTO。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentReviewRequest.java`
  - 审核/取消审核请求 DTO。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentDeleteRequest.java`
  - 批量软删除请求 DTO。
- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java`
  - 数据库后台查询测试。
- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentModeratorTest.java`
  - 数据库审核/删除测试。
- Create: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java`
  - 后台接口权限和业务测试。

---

## Task 1: 后台评论查询领域模型和失败测试

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentQuery.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentItem.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentReader.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java`

- [x] **Step 1: 写失败测试**

Create `DatabaseAdminCommentReaderTest.java`:

```java
package com.aurora.myblog.v2.modules.comment;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentItem;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentQuery;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import com.aurora.myblog.v2.modules.comment.infrastructure.DatabaseAdminCommentReader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase
class DatabaseAdminCommentReaderTest {

    @Autowired
    private DatabaseAdminCommentReader reader;

    @Test
    void listsNonDeletedCommentsForAdmin() {
        PageResponse<AdminCommentItem> page = reader.list(new AdminCommentQuery(null, null, null, null, 1, 10));

        assertThat(page.total()).isEqualTo(5);
        assertThat(page.records()).extracting(AdminCommentItem::id).containsExactly(6, 5, 3, 2, 1);
        assertThat(page.records().get(2).reviewed()).isFalse();
        assertThat(page.records().get(2).deleted()).isFalse();
    }

    @Test
    void filtersByTypeTopicAndReviewStatus() {
        PageResponse<AdminCommentItem> page = reader.list(new AdminCommentQuery(CommentType.ARTICLE, 1, false, null, 1, 10));

        assertThat(page.total()).isEqualTo(1);
        AdminCommentItem item = page.records().get(0);
        assertThat(item.id()).isEqualTo(3);
        assertThat(item.type()).isEqualTo(CommentType.ARTICLE);
        assertThat(item.topicId()).isEqualTo(1);
        assertThat(item.topicTitle()).isEqualTo("后端V2第一篇");
        assertThat(item.reviewed()).isFalse();
    }

    @Test
    void searchesByCommentContentAndAuthorNickname() {
        PageResponse<AdminCommentItem> byContent = reader.list(new AdminCommentQuery(null, null, null, "待审核", 1, 10));
        PageResponse<AdminCommentItem> byAuthor = reader.list(new AdminCommentQuery(null, null, null, "普通用户", 1, 10));

        assertThat(byContent.records()).extracting(AdminCommentItem::id).containsExactly(3);
        assertThat(byAuthor.total()).isGreaterThanOrEqualTo(1);
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseAdminCommentReaderTest'
```

Expected:

```text
Compilation failure: cannot find symbol AdminCommentItem
```

- [x] **Step 3: 新增后台查询领域类型**

Create `AdminCommentQuery.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

public record AdminCommentQuery(
        CommentType type,
        Integer topicId,
        Boolean reviewed,
        String keyword,
        int page,
        int size
) {
    public AdminCommentQuery {
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    public int offset() {
        return (page - 1) * size;
    }
}
```

Create `AdminCommentItem.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

import java.time.LocalDateTime;

public record AdminCommentItem(
        int id,
        CommentType type,
        Integer topicId,
        String topicTitle,
        Integer parentId,
        Integer replyUserId,
        int userId,
        String nickname,
        String avatar,
        String content,
        boolean reviewed,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
```

Create `AdminCommentReader.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

import com.aurora.myblog.v2.common.web.PageResponse;

public interface AdminCommentReader {

    PageResponse<AdminCommentItem> list(AdminCommentQuery query);
}
```

---

## Task 2: 后台评论数据库查询实现

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentReader.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java`

- [x] **Step 1: 实现数据库查询**

Create `DatabaseAdminCommentReader.java`:

```java
package com.aurora.myblog.v2.modules.comment.infrastructure;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentItem;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentQuery;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentReader;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseAdminCommentReader implements AdminCommentReader {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseAdminCommentReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PageResponse<AdminCommentItem> list(AdminCommentQuery query) {
        SqlParts parts = buildWhere(query);
        Long total = jdbcTemplate.queryForObject("select count(*) from t_comment c join t_user_info u on u.id = c.user_id left join t_article a on a.id = c.topic_id " + parts.where(), Long.class, parts.args().toArray());
        List<Object> args = new ArrayList<>(parts.args());
        args.add(query.size());
        args.add(query.offset());
        List<AdminCommentItem> records = jdbcTemplate.query("""
                        select c.id,
                               c.type,
                               c.topic_id,
                               a.article_title as topic_title,
                               c.parent_id,
                               c.reply_user_id,
                               c.user_id,
                               u.nickname,
                               u.avatar,
                               c.comment_content,
                               c.is_review,
                               c.is_delete,
                               c.create_time,
                               c.update_time
                        from t_comment c
                        join t_user_info u on u.id = c.user_id
                        left join t_article a on a.id = c.topic_id
                        %s
                        order by c.id desc
                        limit ? offset ?
                        """.formatted(parts.where()),
                (rs, rowNum) -> new AdminCommentItem(
                        rs.getInt("id"),
                        CommentType.fromCode(rs.getInt("type")),
                        (Integer) rs.getObject("topic_id"),
                        rs.getString("topic_title"),
                        (Integer) rs.getObject("parent_id"),
                        (Integer) rs.getObject("reply_user_id"),
                        rs.getInt("user_id"),
                        rs.getString("nickname"),
                        rs.getString("avatar"),
                        rs.getString("comment_content"),
                        rs.getInt("is_review") == 1,
                        rs.getInt("is_delete") == 1,
                        toLocalDateTime(rs.getTimestamp("create_time")),
                        toLocalDateTime(rs.getTimestamp("update_time"))),
                args.toArray());
        return new PageResponse<>(records, total == null ? 0 : total, query.page(), query.size());
    }

    private SqlParts buildWhere(AdminCommentQuery query) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        clauses.add("c.is_delete = 0");
        if (query.type() != null) {
            clauses.add("c.type = ?");
            args.add(query.type().code());
        }
        if (query.topicId() != null) {
            clauses.add("c.topic_id = ?");
            args.add(query.topicId());
        }
        if (query.reviewed() != null) {
            clauses.add("c.is_review = ?");
            args.add(query.reviewed() ? 1 : 0);
        }
        if (query.keyword() != null) {
            clauses.add("(c.comment_content like ? or u.nickname like ?)");
            String keyword = "%" + query.keyword() + "%";
            args.add(keyword);
            args.add(keyword);
        }
        return new SqlParts("where " + String.join(" and ", clauses), args);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record SqlParts(String where, List<Object> args) {
    }
}
```

- [x] **Step 2: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseAdminCommentReaderTest'
```

Expected:

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [x] **Step 3: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentQuery.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentItem.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentReader.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentReader.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java
git commit -m "实现后端V2后台评论查询"
```

---

## Task 3: 后台评论审核和软删除写入能力

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerationCommand.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDeletionCommand.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerator.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentModerator.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentModeratorTest.java`

- [x] **Step 1: 写失败测试**

Create `DatabaseAdminCommentModeratorTest.java`:

```java
package com.aurora.myblog.v2.modules.comment;

import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDeletionCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerationCommand;
import com.aurora.myblog.v2.modules.comment.infrastructure.DatabaseAdminCommentModerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class DatabaseAdminCommentModeratorTest {

    @Autowired
    private DatabaseAdminCommentModerator moderator;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reviewsCommentsInBatch() {
        moderator.review(new AdminCommentModerationCommand(List.of(3), true));

        Integer reviewed = jdbcTemplate.queryForObject("select is_review from t_comment where id = 3", Integer.class);
        assertThat(reviewed).isEqualTo(1);
    }

    @Test
    void cancelsReviewInBatch() {
        moderator.review(new AdminCommentModerationCommand(List.of(1, 2), false));

        Integer first = jdbcTemplate.queryForObject("select is_review from t_comment where id = 1", Integer.class);
        Integer second = jdbcTemplate.queryForObject("select is_review from t_comment where id = 2", Integer.class);
        assertThat(first).isZero();
        assertThat(second).isZero();
    }

    @Test
    void softDeletesCommentsInBatch() {
        moderator.delete(new AdminCommentDeletionCommand(List.of(1, 2)));

        Integer first = jdbcTemplate.queryForObject("select is_delete from t_comment where id = 1", Integer.class);
        Integer second = jdbcTemplate.queryForObject("select is_delete from t_comment where id = 2", Integer.class);
        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(1);
    }

    @Test
    void rejectsEmptyIds() {
        assertThatThrownBy(() -> moderator.review(new AdminCommentModerationCommand(List.of(), true)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("评论 ID 不能为空");
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseAdminCommentModeratorTest'
```

Expected:

```text
Compilation failure: cannot find symbol AdminCommentModerationCommand
```

- [x] **Step 3: 新增写入领域端口**

Create `AdminCommentModerationCommand.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

import java.util.List;

public record AdminCommentModerationCommand(List<Integer> ids, boolean reviewed) {
}
```

Create `AdminCommentDeletionCommand.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

import java.util.List;

public record AdminCommentDeletionCommand(List<Integer> ids) {
}
```

Create `AdminCommentModerator.java`:

```java
package com.aurora.myblog.v2.modules.comment.domain;

public interface AdminCommentModerator {

    int review(AdminCommentModerationCommand command);

    int delete(AdminCommentDeletionCommand command);
}
```

- [x] **Step 4: 实现数据库写入**

Create `DatabaseAdminCommentModerator.java`:

```java
package com.aurora.myblog.v2.modules.comment.infrastructure;

import com.aurora.myblog.v2.common.error.ApiErrorCode;
import com.aurora.myblog.v2.common.error.ApiException;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDeletionCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerationCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DatabaseAdminCommentModerator implements AdminCommentModerator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseAdminCommentModerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public int review(AdminCommentModerationCommand command) {
        List<Integer> ids = normalizeIds(command.ids());
        return update(ids, "is_review", command.reviewed() ? 1 : 0);
    }

    @Override
    public int delete(AdminCommentDeletionCommand command) {
        List<Integer> ids = normalizeIds(command.ids());
        return update(ids, "is_delete", 1);
    }

    private int update(List<Integer> ids, String column, int value) {
        String placeholders = String.join(",", ids.stream().map(id -> "?").toList());
        Object[] args = new Object[ids.size() + 2];
        args[0] = value;
        args[1] = Timestamp.valueOf(LocalDateTime.now());
        for (int i = 0; i < ids.size(); i++) {
            args[i + 2] = ids.get(i);
        }
        return jdbcTemplate.update("""
                update t_comment
                set %s = ?, update_time = ?
                where id in (%s)
                """.formatted(column, placeholders), args);
    }

    private List<Integer> normalizeIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论 ID 不能为空");
        }
        List<Integer> normalized = ids.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "评论 ID 不能为空");
        }
        if (normalized.size() > 100) {
            throw new ApiException(ApiErrorCode.VALIDATION_ERROR, "单次最多处理 100 条评论");
        }
        return normalized;
    }
}
```

- [x] **Step 5: 运行测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseAdminCommentModeratorTest'
```

Expected:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [x] **Step 6: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerationCommand.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDeletionCommand.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerator.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentModerator.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentModeratorTest.java
git commit -m "新增后端V2后台评论审核写入能力"
```

---

## Task 4: 后台评论管理接口

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentQueryService.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentCommandService.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentController.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentResponse.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentReviewRequest.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentDeleteRequest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java`

- [ ] **Step 1: 写失败测试**

Create `AdminCommentControllerTest.java`:

```java
package com.aurora.myblog.v2.modules.comment;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsAnonymousAdminCommentList() throws Exception {
        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsNonAdminUser() throws Exception {
        String token = loginAndToken("user@163.com");

        mockMvc.perform(get("/api/admin/comments").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listsCommentsForAdmin() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(get("/api/admin/comments")
                        .param("reviewed", "false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(3))
                .andExpect(jsonPath("$.data.records[0].reviewed").value(false));
    }

    @Test
    void reviewsCommentsForAdmin() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(put("/api/admin/comments/review")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":[3],"reviewed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affected").value(1));
    }

    @Test
    void deletesCommentsForAdmin() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(delete("/api/admin/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":[5]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affected").value(1));
    }

    private String loginAndToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.accessToken");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=AdminCommentControllerTest'
```

Expected:

```text
Status expected:<200> but was:<404>
```

- [ ] **Step 3: 新增应用服务**

Create `AdminCommentQueryService.java`:

```java
package com.aurora.myblog.v2.modules.comment.application;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentItem;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentQuery;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentReader;
import org.springframework.stereotype.Service;

@Service
public class AdminCommentQueryService {

    private final AdminCommentReader reader;

    public AdminCommentQueryService(AdminCommentReader reader) {
        this.reader = reader;
    }

    public PageResponse<AdminCommentItem> list(AdminCommentQuery query) {
        return reader.list(query);
    }
}
```

Create `AdminCommentCommandService.java`:

```java
package com.aurora.myblog.v2.modules.comment.application;

import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDeletionCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerationCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerator;
import org.springframework.stereotype.Service;

@Service
public class AdminCommentCommandService {

    private final AdminCommentModerator moderator;

    public AdminCommentCommandService(AdminCommentModerator moderator) {
        this.moderator = moderator;
    }

    public Result review(AdminCommentModerationCommand command) {
        return new Result(moderator.review(command));
    }

    public Result delete(AdminCommentDeletionCommand command) {
        return new Result(moderator.delete(command));
    }

    public record Result(int affected) {
    }
}
```

- [ ] **Step 4: 新增请求和响应 DTO**

Create `AdminCommentResponse.java`:

```java
package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.modules.comment.domain.AdminCommentItem;

import java.time.LocalDateTime;

public record AdminCommentResponse(
        int id,
        int type,
        Integer topicId,
        String topicTitle,
        Integer parentId,
        Integer replyUserId,
        int userId,
        String nickname,
        String avatar,
        String content,
        boolean reviewed,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminCommentResponse from(AdminCommentItem item) {
        return new AdminCommentResponse(
                item.id(),
                item.type().code(),
                item.topicId(),
                item.topicTitle(),
                item.parentId(),
                item.replyUserId(),
                item.userId(),
                item.nickname(),
                item.avatar(),
                item.content(),
                item.reviewed(),
                item.deleted(),
                item.createdAt(),
                item.updatedAt());
    }
}
```

Create `AdminCommentReviewRequest.java`:

```java
package com.aurora.myblog.v2.modules.comment.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminCommentReviewRequest(
        @NotEmpty(message = "评论 ID 不能为空")
        List<Integer> ids,

        @NotNull(message = "审核状态不能为空")
        Boolean reviewed
) {
}
```

Create `AdminCommentDeleteRequest.java`:

```java
package com.aurora.myblog.v2.modules.comment.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminCommentDeleteRequest(
        @NotEmpty(message = "评论 ID 不能为空")
        List<Integer> ids
) {
}
```

- [ ] **Step 5: 新增 Controller**

Create `AdminCommentController.java`:

```java
package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.application.AdminCommentCommandService;
import com.aurora.myblog.v2.modules.comment.application.AdminCommentQueryService;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDeletionCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentItem;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerationCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentQuery;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/comments")
public class AdminCommentController {

    private final AdminCommentQueryService queryService;
    private final AdminCommentCommandService commandService;

    public AdminCommentController(AdminCommentQueryService queryService,
                                  AdminCommentCommandService commandService) {
        this.queryService = queryService;
        this.commandService = commandService;
    }

    @GetMapping
    ApiResponse<PageResponse<AdminCommentResponse>> list(@RequestParam(required = false) Integer type,
                                                         @RequestParam(required = false) Integer topicId,
                                                         @RequestParam(required = false) Boolean reviewed,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        CommentType commentType = type == null ? null : CommentType.fromCode(type);
        PageResponse<AdminCommentItem> result = queryService.list(new AdminCommentQuery(commentType, topicId, reviewed, keyword, page, size));
        return ApiResponse.ok(new PageResponse<>(
                result.records().stream().map(AdminCommentResponse::from).toList(),
                result.total(),
                result.page(),
                result.size()));
    }

    @PutMapping("/review")
    ApiResponse<AdminCommentCommandService.Result> review(@Valid @RequestBody AdminCommentReviewRequest request) {
        return ApiResponse.ok(commandService.review(new AdminCommentModerationCommand(request.ids(), request.reviewed())));
    }

    @DeleteMapping
    ApiResponse<AdminCommentCommandService.Result> delete(@Valid @RequestBody AdminCommentDeleteRequest request) {
        return ApiResponse.ok(commandService.delete(new AdminCommentDeletionCommand(request.ids())));
    }
}
```

- [ ] **Step 6: 运行接口测试确认通过**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=AdminCommentControllerTest,DatabaseAdminCommentReaderTest,DatabaseAdminCommentModeratorTest'
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 7: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentQueryService.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentCommandService.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentController.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentResponse.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentReviewRequest.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentDeleteRequest.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java
git commit -m "新增后端V2后台评论管理接口"
```

---

## Task 5: 全量验证和计划状态同步

**Files:**

- Modify: `docs/superpowers/plans/2026-05-30-backend-v2-comment-admin-review.zh-CN.md`

- [ ] **Step 1: 全量测试**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml test
```

Expected:

```text
Failures: 0, Errors: 0
BUILD SUCCESS
```

- [ ] **Step 2: 打包**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: 本地 MySQL 只读检查**

不要把本地密码写进文件。命令只读取当前 PowerShell 会话环境变量：

```powershell
if (-not $env:MYSQL_PWD) { throw 'MYSQL_PWD is required' }
mysql -h localhost -P 3306 -u root -N -e "select count(*) from aurora.t_comment where is_delete = 0;"
mysql -h localhost -P 3306 -u root -N -e "select is_review, count(*) from aurora.t_comment where is_delete = 0 group by is_review;"
```

Expected:

```text
SQL 执行成功；如果本地库有评论，应看到未删除评论总数和审核状态分布。
```

- [ ] **Step 4: 本地 API 冒烟**

启动服务时通过环境变量注入数据库密码：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
if (-not $env:MYBLOG_DATASOURCE_PASSWORD) { throw 'MYBLOG_DATASOURCE_PASSWORD is required' }
if (-not $env:MYBLOG_JWT_SECRET) { throw 'MYBLOG_JWT_SECRET is required' }
mvn -f MyBlog-springboot-v2/pom.xml spring-boot:run
```

调用后台接口需要真实管理员账号。没有可确认管理员账号时，只记录“本地库管理员登录账号不确定，跳过真实后台接口冒烟”，不能伪造通过。

如果本地库可使用真实管理员账号，执行：

```powershell
$login = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/auth/login' -ContentType 'application/json' -Body '{"username":"admin@163.com","password":"password123"}'
$token = $login.data.accessToken
Invoke-WebRequest -Uri 'http://localhost:8080/api/admin/comments?page=1&size=10' -Headers @{ Authorization = "Bearer $token" } -UseBasicParsing
Invoke-WebRequest -Method Put -Uri 'http://localhost:8080/api/admin/comments/review' -Headers @{ Authorization = "Bearer $token" } -ContentType 'application/json' -Body '{"ids":[3],"reviewed":true}' -UseBasicParsing
```

Expected:

```text
后台列表返回 200；审核接口返回 200 和 affected 数量。没有管理员账号时跳过真实后台接口冒烟，并说明原因。
```

- [ ] **Step 5: 更新本计划的执行结果**

先读取实际提交记录：

```powershell
git log --oneline -8
```

再在本文档末尾追加真实执行结果。提交记录必须复制实际短 SHA 和提交信息。

- [ ] **Step 6: 提交**

```powershell
git add docs/superpowers/plans/2026-05-30-backend-v2-comment-admin-review.zh-CN.md
git commit -m "同步后端V2评论审核计划状态"
```

---

## 自检结果

- 覆盖范围：计划覆盖后台评论分页查询、类型筛选、主题筛选、审核状态筛选、关键词搜索、批量审核、批量取消审核、批量软删除和管理员接口鉴权。
- 边界控制：不改真实表结构，不引入 Redis、RabbitMQ、邮件通知、搜索引擎或部署改造。
- 类型一致性：`AdminCommentQuery`、`AdminCommentItem`、`AdminCommentReader`、`AdminCommentModerator`、应用服务和 Controller DTO 命名一致。
- 安全控制：接口全部位于 `/api/admin/comments`，由现有 `/api/admin/**` 规则保护；未登录为 `401`，非管理员为 `403`。
- 数据控制：删除只更新 `is_delete = 1`；审核只更新 `is_review`；单次批量最多 100 条，避免一次请求更新过多数据。
- 占位扫描：文档没有待替换占位词；本地数据库密码只通过环境变量读取，不能写入代码、文档或 Git。
