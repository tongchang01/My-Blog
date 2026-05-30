# 后端 V2 评论治理增强实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐个实现。步骤使用 checkbox（`- [ ]`）语法跟踪状态。

**目标：** 在后端 V2 已有后台评论审核能力之上，补齐评论详情、已删除评论筛选、软删除恢复和前台可见性回归，形成更完整的评论治理闭环。

**架构：** 继续沿用模块化单体，把能力放在 `modules/comment` 内；后台接口继续挂在 `/api/admin/comments` 下，复用现有 `/api/admin/**` 管理员鉴权。数据访问继续使用 `JdbcTemplate` 和现有 `t_comment` 表，本期不改真实 MySQL 表结构，不引入 Redis、MQ、搜索引擎或部署改造。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring Security、JdbcTemplate、JUnit 5、MockMvc、H2 测试迁移、MySQL 本地冒烟。

---

## 边界和决策

- 本计划只做 Java 后端 V2 评论治理增强，不改前台 Vue，不改后台 Vue，不做部署。
- 本计划不改真实表结构。现有 `t_comment` 没有 IP、UA、审核人、审核时间、删除人、删除时间字段，所以本期不能伪造这些数据。
- 本期后台详情只返回现有可可靠读取的字段：评论、作者、回复目标、所属文章或留言板、审核状态、删除状态、创建时间、更新时间。
- 本期支持后台查看已删除评论，并支持把软删除评论恢复为 `is_delete = 0`。
- 前台接口必须继续只展示 `is_review = 1` 且 `is_delete = 0` 的评论；后台增强不能放宽前台可见性。
- 审核记录、操作审计、IP/UA 采集、敏感词、通知、站内信、缓存和消息队列后续另开表结构与业务增强计划。
- 管理员权限继续依赖现有规则：未登录访问 `/api/admin/**` 返回 `401`，普通用户返回 `403`，管理员允许访问。

## 表结构评估

当前测试迁移里的 `t_comment` 结构为：

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

本期判断：

- `is_delete` 已足够支撑后台回收站和恢复能力。
- `is_review` 已足够支撑审核和取消审核能力。
- `parent_id`、`reply_user_id`、`topic_id`、`type` 已足够支撑详情里的评论关系展示。
- 不足字段是 IP、UA、审核人、审核时间、删除人、删除时间、操作备注；这些字段需要真实表结构升级和历史数据兼容策略，不适合夹在本期接口增强里顺手改。

## 文件结构

- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentQuery.java`
  - 新增 `Boolean deleted` 筛选条件，默认仍只查未删除评论。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDetail.java`
  - 后台评论详情领域模型，承载评论正文、作者、回复对象、所属主题、审核状态和删除状态。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentReader.java`
  - 新增 `Optional<AdminCommentDetail> findDetail(int id)`。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentRestoreCommand.java`
  - 后台批量恢复软删除评论命令。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerator.java`
  - 新增 `int restore(AdminCommentRestoreCommand command)`。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentReader.java`
  - 支持 deleted 筛选和详情查询。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentModerator.java`
  - 支持把 `is_delete` 恢复为 `0`。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentQueryService.java`
  - 暴露后台详情查询应用方法。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentCommandService.java`
  - 暴露恢复软删除应用方法。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentDetailResponse.java`
  - 后台评论详情响应 DTO。
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentRestoreRequest.java`
  - 后台批量恢复请求 DTO。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentController.java`
  - 新增 `GET /api/admin/comments/{id}` 和 `PUT /api/admin/comments/restore`，列表新增 `deleted` 参数。
- Modify: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java`
  - 增加已删除筛选和详情查询测试。
- Modify: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentModeratorTest.java`
  - 增加恢复软删除测试。
- Modify: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java`
  - 增加详情、已删除筛选、恢复和权限测试。
- Modify: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentReaderTest.java`
  - 增加前台可见性回归测试，防止后台增强污染前台读取规则。

---

## Task 1: 后台评论详情和已删除筛选领域计划

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentQuery.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDetail.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentReader.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java`

- [ ] **Step 1: 写失败测试**

在 `DatabaseAdminCommentReaderTest` 增加测试：

```java
@Test
void listsDeletedCommentsWhenDeletedFilterIsTrue() {
    PageResponse<AdminCommentItem> page = reader.list(new AdminCommentQuery(null, null, null, null, true, 1, 10));

    assertThat(page.total()).isEqualTo(1);
    assertThat(page.records()).extracting(AdminCommentItem::id).containsExactly(4);
    assertThat(page.records().get(0).deleted()).isTrue();
}

@Test
void findsCommentDetailIncludingDeletedComment() {
    Optional<AdminCommentDetail> detail = reader.findDetail(4);

    assertThat(detail).isPresent();
    assertThat(detail.get().id()).isEqualTo(4);
    assertThat(detail.get().content()).isEqualTo("已删除评论");
    assertThat(detail.get().type()).isEqualTo(CommentType.ARTICLE);
    assertThat(detail.get().topicId()).isEqualTo(1);
    assertThat(detail.get().topicTitle()).isEqualTo("后端V2第一篇");
    assertThat(detail.get().deleted()).isTrue();
    assertThat(detail.get().reviewed()).isTrue();
}

@Test
void returnsEmptyWhenCommentDetailDoesNotExist() {
    assertThat(reader.findDetail(9999)).isEmpty();
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=DatabaseAdminCommentReaderTest test
```

Expected:

```text
Compilation failure
constructor AdminCommentQuery cannot be applied to given types
cannot find symbol: class AdminCommentDetail
cannot find symbol: method findDetail(int)
```

- [ ] **Step 3: 新增领域模型和端口**

把 `AdminCommentQuery` 改成：

```java
package com.aurora.myblog.v2.modules.comment.domain;

public record AdminCommentQuery(
        CommentType type,
        Integer topicId,
        Boolean reviewed,
        String keyword,
        Boolean deleted,
        int page,
        int size
) {
    public AdminCommentQuery(CommentType type, Integer topicId, Boolean reviewed, String keyword, int page, int size) {
        this(type, topicId, reviewed, keyword, false, page, size);
    }

    public AdminCommentQuery {
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        deleted = deleted == null ? false : deleted;
    }

    public int offset() {
        return (page - 1) * size;
    }
}
```

新增 `AdminCommentDetail.java`：

```java
package com.aurora.myblog.v2.modules.comment.domain;

import java.time.LocalDateTime;

public record AdminCommentDetail(
        int id,
        CommentType type,
        Integer topicId,
        String topicTitle,
        Integer parentId,
        Integer replyUserId,
        String replyNickname,
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

把 `AdminCommentReader` 改成：

```java
package com.aurora.myblog.v2.modules.comment.domain;

import com.aurora.myblog.v2.common.web.PageResponse;

import java.util.Optional;

public interface AdminCommentReader {

    PageResponse<AdminCommentItem> list(AdminCommentQuery query);

    Optional<AdminCommentDetail> findDetail(int id);
}
```

- [ ] **Step 4: 补齐测试 import**

在 `DatabaseAdminCommentReaderTest` 增加：

```java
import java.util.Optional;
```

如果测试文件还没有静态导入 `assertThat`，保持现有 `org.assertj.core.api.Assertions.assertThat` 静态导入不变。

- [ ] **Step 5: 运行测试确认仍失败到数据库实现**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=DatabaseAdminCommentReaderTest test
```

Expected:

```text
DatabaseAdminCommentReader is not abstract and does not override abstract method findDetail(int)
```

- [ ] **Step 6: 不提交失败状态**

当前状态是有意制造的红灯测试，不提交。继续执行 Task 2，把数据库实现补齐并在测试通过后一起提交。

---

## Task 2: 后台评论详情和已删除筛选数据库实现

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentReader.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java`

- [ ] **Step 1: 实现 deleted 筛选**

把 `buildWhere` 中固定的：

```java
clauses.add("c.is_delete = 0");
```

改成：

```java
clauses.add("c.is_delete = ?");
args.add(query.deleted() ? 1 : 0);
```

- [ ] **Step 2: 实现详情查询**

在 `DatabaseAdminCommentReader` 增加：

```java
@Override
public Optional<AdminCommentDetail> findDetail(int id) {
    List<AdminCommentDetail> records = jdbcTemplate.query("""
                    select c.id,
                           c.type,
                           c.topic_id,
                           a.article_title as topic_title,
                           c.parent_id,
                           c.reply_user_id,
                           reply_user.nickname as reply_nickname,
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
                    left join t_user_info reply_user on reply_user.id = c.reply_user_id
                    left join t_article a on a.id = c.topic_id
                    where c.id = ?
                    """,
            (rs, rowNum) -> new AdminCommentDetail(
                    rs.getInt("id"),
                    CommentType.fromCode(rs.getInt("type")),
                    (Integer) rs.getObject("topic_id"),
                    rs.getString("topic_title"),
                    (Integer) rs.getObject("parent_id"),
                    (Integer) rs.getObject("reply_user_id"),
                    rs.getString("reply_nickname"),
                    rs.getInt("user_id"),
                    rs.getString("nickname"),
                    rs.getString("avatar"),
                    rs.getString("comment_content"),
                    rs.getInt("is_review") == 1,
                    rs.getInt("is_delete") == 1,
                    toLocalDateTime(rs.getTimestamp("create_time")),
                    toLocalDateTime(rs.getTimestamp("update_time"))),
            id);
    return records.stream().findFirst();
}
```

同时补齐 import：

```java
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDetail;
import java.util.Optional;
```

- [ ] **Step 3: 运行数据库查询测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=DatabaseAdminCommentReaderTest test
```

Expected:

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 4: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentQuery.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDetail.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentReader.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentReader.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java
git commit -m "实现后端V2后台评论详情查询"
```

---

## Task 3: 后台恢复软删除评论

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentRestoreCommand.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerator.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentModerator.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentCommandService.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentModeratorTest.java`

- [ ] **Step 1: 写失败测试**

在 `DatabaseAdminCommentModeratorTest` 增加：

```java
@Test
void restoresDeletedCommentsInBatch() {
    int affected = moderator.restore(new AdminCommentRestoreCommand(List.of(4)));

    Integer deleted = jdbcTemplate.queryForObject("select is_delete from t_comment where id = 4", Integer.class);
    assertThat(affected).isEqualTo(1);
    assertThat(deleted).isZero();
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=DatabaseAdminCommentModeratorTest test
```

Expected:

```text
cannot find symbol: class AdminCommentRestoreCommand
cannot find symbol: method restore(AdminCommentRestoreCommand)
```

- [ ] **Step 3: 新增恢复命令和端口**

新增 `AdminCommentRestoreCommand.java`：

```java
package com.aurora.myblog.v2.modules.comment.domain;

import java.util.List;

public record AdminCommentRestoreCommand(List<Integer> ids) {

    public AdminCommentRestoreCommand {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids must not be empty");
        }
        if (ids.size() > 100) {
            throw new IllegalArgumentException("ids size must not exceed 100");
        }
    }
}
```

把 `AdminCommentModerator` 改成：

```java
package com.aurora.myblog.v2.modules.comment.domain;

public interface AdminCommentModerator {

    int review(AdminCommentModerationCommand command);

    int delete(AdminCommentDeletionCommand command);

    int restore(AdminCommentRestoreCommand command);
}
```

- [ ] **Step 4: 实现数据库恢复**

在 `DatabaseAdminCommentModerator` 增加：

```java
@Override
public int restore(AdminCommentRestoreCommand command) {
    return update(command.ids(), "is_delete", 0);
}
```

同时补齐 import：

```java
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentRestoreCommand;
```

- [ ] **Step 5: 暴露应用服务方法**

在 `AdminCommentCommandService` 增加：

```java
public Result restore(AdminCommentRestoreCommand command) {
    return new Result(moderator.restore(command));
}
```

同时补齐 import：

```java
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentRestoreCommand;
```

- [ ] **Step 6: 运行恢复测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=DatabaseAdminCommentModeratorTest test
```

Expected:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 7: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentRestoreCommand.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerator.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentModerator.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentCommandService.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentModeratorTest.java
git commit -m "新增后端V2后台评论恢复能力"
```

---

## Task 4: 后台详情和恢复接口

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentDetailResponse.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentRestoreRequest.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentQueryService.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentController.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java`

- [ ] **Step 1: 写失败接口测试**

在 `AdminCommentControllerTest` 增加：

```java
@Test
void getsCommentDetailForAdmin() throws Exception {
    mockMvc.perform(get("/api/admin/comments/{id}", 4)
                    .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(4))
            .andExpect(jsonPath("$.data.content").value("已删除评论"))
            .andExpect(jsonPath("$.data.deleted").value(true));
}

@Test
void listsDeletedCommentsForAdmin() throws Exception {
    mockMvc.perform(get("/api/admin/comments")
                    .param("deleted", "true")
                    .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(1))
            .andExpect(jsonPath("$.data.records[0].id").value(4))
            .andExpect(jsonPath("$.data.records[0].deleted").value(true));
}

@Test
void restoresCommentsForAdmin() throws Exception {
    mockMvc.perform(put("/api/admin/comments/restore")
                    .with(user("admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"ids":[4]}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.affected").value(1));
}
```

- [ ] **Step 2: 运行测试确认失败**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=AdminCommentControllerTest test
```

Expected:

```text
Status expected:<200> but was:<404>
```

- [ ] **Step 3: 新增详情响应 DTO**

新增 `AdminCommentDetailResponse.java`：

```java
package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDetail;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;

import java.time.LocalDateTime;

public record AdminCommentDetailResponse(
        int id,
        CommentType type,
        Integer topicId,
        String topicTitle,
        Integer parentId,
        Integer replyUserId,
        String replyNickname,
        int userId,
        String nickname,
        String avatar,
        String content,
        boolean reviewed,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    static AdminCommentDetailResponse from(AdminCommentDetail detail) {
        return new AdminCommentDetailResponse(
                detail.id(),
                detail.type(),
                detail.topicId(),
                detail.topicTitle(),
                detail.parentId(),
                detail.replyUserId(),
                detail.replyNickname(),
                detail.userId(),
                detail.nickname(),
                detail.avatar(),
                detail.content(),
                detail.reviewed(),
                detail.deleted(),
                detail.createdAt(),
                detail.updatedAt());
    }
}
```

- [ ] **Step 4: 新增恢复请求 DTO**

新增 `AdminCommentRestoreRequest.java`：

```java
package com.aurora.myblog.v2.modules.comment.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminCommentRestoreRequest(
        @NotEmpty
        @Size(max = 100)
        List<Integer> ids
) {
}
```

- [ ] **Step 5: 暴露详情应用服务**

在 `AdminCommentQueryService` 增加：

```java
public AdminCommentDetail detail(int id) {
    return reader.findDetail(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));
}
```

同时补齐 import：

```java
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDetail;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
```

- [ ] **Step 6: 修改 Controller**

把列表构造查询改为：

```java
@RequestParam(required = false) Boolean deleted,

new AdminCommentQuery(
        type == null ? null : CommentType.fromCode(type),
        topicId,
        reviewed,
        keyword,
        deleted,
        page,
        size)
```

并新增接口：

```java
@GetMapping("/{id}")
ApiResponse<AdminCommentDetailResponse> detail(@PathVariable int id) {
    return ApiResponse.ok(AdminCommentDetailResponse.from(queryService.detail(id)));
}

@PutMapping("/restore")
ApiResponse<AdminCommentCommandService.Result> restore(@Valid @RequestBody AdminCommentRestoreRequest request) {
    return ApiResponse.ok(commandService.restore(new AdminCommentRestoreCommand(request.ids())));
}
```

同时补齐 import：

```java
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentRestoreCommand;
import org.springframework.web.bind.annotation.PathVariable;
```

- [ ] **Step 7: 运行接口测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=AdminCommentControllerTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 8: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentDetailResponse.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentRestoreRequest.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/AdminCommentQueryService.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentController.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java
git commit -m "新增后端V2后台评论详情和恢复接口"
```

---

## Task 5: 前台可见性回归和全量验证

**Files:**

- Modify: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentReaderTest.java`
- Modify: `docs/superpowers/plans/2026-05-30-backend-v2-comment-governance-enhancement.zh-CN.md`

- [ ] **Step 1: 写前台可见性回归测试**

在 `DatabaseCommentReaderTest` 增加：

```java
@Test
void publicCommentReaderHidesPendingAndDeletedComments() {
    PageResponse<CommentThread> page = reader.list(new CommentPageQuery(1, 20), CommentType.ARTICLE, 1);

    assertThat(page.records()).extracting(CommentThread::id).contains(1);
    assertThat(page.records()).extracting(CommentThread::id).doesNotContain(3, 4);
}
```

- [ ] **Step 2: 运行前台可见性测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=DatabaseCommentReaderTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 3: 运行评论模块测试**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml -Dtest=DatabaseCommentReaderTest,DatabaseCommentWriterTest,DatabaseAdminCommentReaderTest,DatabaseAdminCommentModeratorTest,CommentControllerTest,AdminCommentControllerTest test
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 4: 运行全量测试**

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

- [ ] **Step 5: 打包**

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

Expected:

```text
BUILD SUCCESS
```

- [ ] **Step 6: 本地 MySQL 只读检查**

不要把本地密码写进文件。命令只读取当前 PowerShell 会话环境变量：

```powershell
if (-not $env:MYSQL_PWD) { throw 'MYSQL_PWD is required' }
mysql -h localhost -P 3306 -u root -N -e "select count(*) from aurora.t_comment;"
mysql -h localhost -P 3306 -u root -N -e "select is_delete, is_review, count(*) from aurora.t_comment group by is_delete, is_review;"
```

Expected:

```text
SQL 执行成功；如果本地库没有评论，也要如实记录为 0，不伪造数据。
```

- [ ] **Step 7: 更新本计划执行结果**

读取实际提交记录：

```powershell
git log --oneline -8
```

在本文档末尾追加真实执行结果，必须包含测试数量、打包结果、本地 MySQL 只读结果和 API 冒烟结果。

- [ ] **Step 8: 提交**

```powershell
git add MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentReaderTest.java `
        docs/superpowers/plans/2026-05-30-backend-v2-comment-governance-enhancement.zh-CN.md
git commit -m "同步后端V2评论治理增强计划状态"
```

---

## 自检结果

- 覆盖范围：计划覆盖后台评论详情、已删除筛选、软删除恢复、接口鉴权、前台可见性回归和最终验证。
- 边界控制：没有要求修改真实 MySQL 表结构，没有引入 Redis、RabbitMQ、搜索引擎、通知或部署改造。
- 数据诚实性：明确现有表没有 IP、UA、审核人、审核时间等字段，本期不伪造这些数据。
- 类型一致性：`AdminCommentDetail`、`AdminCommentRestoreCommand`、`findDetail`、`restore`、`deleted` 在领域、基础设施、应用服务和接口层命名一致。
- 风险控制：后台恢复只把 `is_delete` 改回 `0`；前台读取规则继续要求 `is_delete = 0` 且 `is_review = 1`。
- 占位扫描：本文档没有待替换占位内容。
