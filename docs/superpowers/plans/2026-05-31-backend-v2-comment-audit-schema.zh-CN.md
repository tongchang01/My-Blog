# 后端 V2 评论表结构与审计增强实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐个实现。步骤使用 checkbox（`- [ ]`）语法跟踪状态。

**目标：** 在后端 V2 已有评论读取、提交、审核、删除、恢复能力之上，补齐评论创建来源、审核操作者、删除操作者、恢复操作者等审计字段，并给出真实 MySQL 的手工迁移脚本方案。

**架构：** 继续沿用模块化单体和 `modules/comment` 分层，评论业务仍通过 domain port + infrastructure adapter 访问数据库。本期不引入 Redis、MQ、搜索引擎、自动部署，也不在 local profile 启动时自动修改真实 MySQL；真实库结构变更先以 SQL 文档形式审阅，H2 测试迁移用于自动化验证。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring Security、JdbcTemplate、JUnit 5、MockMvc、H2 测试迁移、MySQL 手工 SQL 审阅。

---

## 边界和决策

- 本计划只处理 Java 后端 V2 评论表结构与审计增强，不改前台 Vue，不改后台 Vue，不做部署。
- 本计划继续使用 `JdbcTemplate`，不在本阶段切换 MyBatis Plus。ORM 选型可以单独开文档，但不阻塞本次评论审计字段设计。
- 本计划不让 local profile 自动执行 Flyway。`application-local.yml` 当前 `spring.flyway.enabled=false`，必须保持该行为。
- 真实 MySQL 结构变更先写成 SQL 审阅文档，等你确认后再手工执行，不在普通功能提交里自动跑线上或本地真实库迁移。
- H2 测试结构可以直接更新 `src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`，因为它只服务自动化测试。
- 本期只补评论审计字段，不做敏感词、验证码、限流、通知、站内信、评论举报、内容风控。
- IP 和 User-Agent 属于审计信息，不返回给前台公开接口；后台详情是否展示可以先返回，后续后台页面再决定是否使用。
- 旧数据的审计字段允许为空，不能为了历史数据完整性伪造操作者、IP 或 UA。

## 当前问题

当前测试迁移中的 `t_comment` 结构为：

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

当前实现的限制是：

- `DatabaseCommentWriter` 只能记录评论内容、作者、主题、父评论、审核状态和时间，无法记录提交 IP 和 User-Agent。
- `DatabaseAdminCommentModerator` 审核、删除、恢复时只修改 `is_review`、`is_delete` 和 `update_time`，无法记录是谁操作、什么时候审核、什么时候删除、什么时候恢复。
- `AdminCommentDetail` 只能展示旧字段，后台无法排查评论来源和治理历史。
- IP 解析逻辑当前放在 `modules.identity.api.ClientIpResolver`，如果评论模块直接依赖它，会形成不合理的跨业务模块依赖。

## 目标表结构

本期建议在 `t_comment` 上追加以下字段：

```sql
alter table t_comment
    add column create_ip varchar(45) null comment '评论提交 IP',
    add column user_agent varchar(255) null comment '评论提交 User-Agent',
    add column reviewed_by int null comment '最后审核人用户 ID',
    add column review_time timestamp null comment '最后审核时间',
    add column deleted_by int null comment '最后删除人用户 ID',
    add column delete_time timestamp null comment '最后删除时间',
    add column restored_by int null comment '最后恢复人用户 ID',
    add column restore_time timestamp null comment '最后恢复时间';
```

字段含义：

| 字段 | 写入时机 | 是否允许为空 | 说明 |
| --- | --- | --- | --- |
| `create_ip` | 用户提交评论时 | 是 | 历史评论为空；IPv4/IPv6 都允许 |
| `user_agent` | 用户提交评论时 | 是 | 最长 255，超过则截断 |
| `reviewed_by` | 后台审核或取消审核时 | 是 | 记录最后一次审核操作人 |
| `review_time` | 后台审核或取消审核时 | 是 | 记录最后一次审核操作时间 |
| `deleted_by` | 后台软删除时 | 是 | 记录最后一次删除操作人 |
| `delete_time` | 后台软删除时 | 是 | 记录最后一次删除时间 |
| `restored_by` | 后台恢复软删除时 | 是 | 记录最后一次恢复操作人 |
| `restore_time` | 后台恢复软删除时 | 是 | 记录最后一次恢复时间 |

恢复评论时不清空 `deleted_by` 和 `delete_time`。原因是删除历史属于审计信息，恢复只是新增一条恢复痕迹。

## 文件结构

- Create: `docs/superpowers/specs/2026-05-31-backend-v2-comment-audit-schema.sql`
  - 真实 MySQL 手工审阅迁移脚本，只记录 SQL，不自动执行。
- Modify: `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`
  - 给 H2 测试结构补齐评论审计字段，并更新测试数据。
- Move/Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/ClientIpResolver.java`
  - 把 IP 解析能力从 identity api 包移动到 common web 包。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java`
  - 改用 common web 下的 `ClientIpResolver`。
- Move/Modify: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/web/ClientIpResolverTest.java`
  - 测试文件随包名移动。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentCreateCommand.java`
  - 新增 `clientIp`、`userAgent` 字段。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentController.java`
  - 注入 `HttpServletRequest`，采集 IP 和 UA。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/CommentCommandService.java`
  - 把客户端信息传给 `CommentCreateCommand`。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseCommentWriter.java`
  - 写入 `create_ip`、`user_agent`。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerationCommand.java`
  - 新增 `operatorUserId`。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDeletionCommand.java`
  - 新增 `operatorUserId`。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentRestoreCommand.java`
  - 新增 `operatorUserId`。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentController.java`
  - 使用 `@CurrentUser` 取得后台操作者 ID。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentModerator.java`
  - 审核、删除、恢复时写入对应审计字段。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDetail.java`
  - 增加审计字段。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentDetailResponse.java`
  - 后台详情返回审计字段。
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentReader.java`
  - 后台详情读取审计字段。
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/CommentControllerTest.java`
  - 验证评论提交写入 IP 和 UA。
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentWriterTest.java`
  - 如果已有测试不足，新增数据库写入层测试。
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentModeratorTest.java`
  - 验证审核、删除、恢复写入操作者和时间。
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java`
  - 验证后台详情能读取审计字段。
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java`
  - 验证后台接口能把当前用户作为操作者传入。

---

## Task 1: 写真实 MySQL 手工迁移脚本和 H2 测试结构

**Files:**

- Create: `docs/superpowers/specs/2026-05-31-backend-v2-comment-audit-schema.sql`
- Modify: `MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql`

- [x] **Step 1: 创建真实 MySQL 手工 SQL 文档**

创建 `docs/superpowers/specs/2026-05-31-backend-v2-comment-audit-schema.sql`：

```sql
-- 后端 V2 评论审计字段迁移脚本
-- 执行前置条件：
-- 1. 确认目标库为本地或预发 aurora，不直接在生产库无备份执行。
-- 2. 执行前先备份 t_comment。
-- 3. 执行后使用本文末尾 verification SQL 验证字段存在。

alter table t_comment
    add column create_ip varchar(45) null comment '评论提交 IP',
    add column user_agent varchar(255) null comment '评论提交 User-Agent',
    add column reviewed_by int null comment '最后审核人用户 ID',
    add column review_time timestamp null comment '最后审核时间',
    add column deleted_by int null comment '最后删除人用户 ID',
    add column delete_time timestamp null comment '最后删除时间',
    add column restored_by int null comment '最后恢复人用户 ID',
    add column restore_time timestamp null comment '最后恢复时间';

create index idx_comment_review_delete_time
    on t_comment (is_review, is_delete, create_time);

create index idx_comment_parent_delete_review
    on t_comment (parent_id, is_delete, is_review);

-- verification
select column_name, data_type, is_nullable
from information_schema.columns
where table_schema = database()
  and table_name = 't_comment'
  and column_name in (
      'create_ip', 'user_agent',
      'reviewed_by', 'review_time',
      'deleted_by', 'delete_time',
      'restored_by', 'restore_time'
  )
order by ordinal_position;
```

- [x] **Step 2: 更新 H2 测试建表 SQL**

把 `t_comment` 建表改成：

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
    create_ip varchar(45),
    user_agent varchar(255),
    reviewed_by int,
    review_time timestamp,
    deleted_by int,
    delete_time timestamp,
    restored_by int,
    restore_time timestamp,
    create_time timestamp not null,
    update_time timestamp
);
```

- [x] **Step 3: 更新 H2 测试数据插入列**

把 `insert into t_comment` 的列追加为：

```sql
insert into t_comment (
    id, user_id, reply_user_id, topic_id, comment_content,
    parent_id, type, is_delete, is_review,
    create_ip, user_agent, reviewed_by, review_time,
    deleted_by, delete_time, restored_by, restore_time,
    create_time, update_time
)
```

测试数据建议：

```sql
(1, 2, null, 1, '第一条文章评论', null, 1, 0, 1,
 '203.0.113.1', 'JUnit Browser', 1, timestamp '2026-05-29 10:01:00',
 null, null, null, null,
 timestamp '2026-05-29 10:00:00', timestamp '2026-05-29 10:00:00')
```

其他行可以按需要填 `null`，但已删除评论 `id=4` 应设置 `deleted_by=1`、`delete_time=timestamp '2026-05-29 10:16:00'`。

- [x] **Step 4: 运行测试上下文验证迁移可用**

Run:

```powershell
mvn -pl MyBlog-springboot-v2 "-Dtest=DatabaseCommentReaderTest" test
```

Expected: `BUILD SUCCESS`，且 H2 建表不报字段数量不匹配。

- [x] **Step 5: 提交**

```powershell
git add docs/superpowers/specs/2026-05-31-backend-v2-comment-audit-schema.sql `
        MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql
git commit -m "新增后端V2评论审计表结构计划"
```

---

## Task 2: 抽取通用客户端信息解析能力

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/ClientIpResolver.java`
- Delete: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/ClientIpResolver.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java`
- Create: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/UserAgentResolver.java`
- Move: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/ClientIpResolverTest.java` -> `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/web/ClientIpResolverTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/web/UserAgentResolverTest.java`

- [x] **Step 1: 移动 `ClientIpResolver` 到 common web 包**

新文件内容：

```java
package com.aurora.myblog.v2.common.web;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }
        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return normalize(request.getRemoteAddr());
    }

    private static String firstForwardedIp(String value) {
        if (value == null) {
            return null;
        }
        for (String part : value.split(",")) {
            String ip = normalize(part);
            if (ip != null) {
                return ip;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
```

- [x] **Step 2: 新增 `UserAgentResolver`**

```java
package com.aurora.myblog.v2.common.web;

import jakarta.servlet.http.HttpServletRequest;

public final class UserAgentResolver {

    private static final int MAX_LENGTH = 255;

    private UserAgentResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String normalized = userAgent.trim();
        if (normalized.length() <= MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LENGTH);
    }
}
```

- [x] **Step 3: 更新登录接口 import**

`AuthController` 改用：

```java
import com.aurora.myblog.v2.common.web.ClientIpResolver;
```

- [x] **Step 4: 移动并调整 IP 测试包名**

`ClientIpResolverTest` 的包名改为：

```java
package com.aurora.myblog.v2.common.web;
```

- [x] **Step 5: 新增 UA 截断测试**

```java
package com.aurora.myblog.v2.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UserAgentResolverTest {

    @Test
    void returnsNullWhenUserAgentIsBlank() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", " ");

        assertThat(UserAgentResolver.resolve(request)).isNull();
    }

    @Test
    void truncatesLongUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "a".repeat(300));

        assertThat(UserAgentResolver.resolve(request)).hasSize(255);
    }
}
```

- [x] **Step 6: 运行测试**

Run:

```powershell
mvn -pl MyBlog-springboot-v2 "-Dtest=ClientIpResolverTest,UserAgentResolverTest,AuthControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [x] **Step 7: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/ClientIpResolver.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/UserAgentResolver.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/AuthController.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/web/ClientIpResolverTest.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/web/UserAgentResolverTest.java
git rm MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/identity/api/ClientIpResolver.java `
       MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/identity/ClientIpResolverTest.java
git commit -m "抽取后端V2客户端信息解析能力"
```

---

## Task 3: 评论提交写入 IP 和 User-Agent

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentCreateCommand.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentController.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/CommentCommandService.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseCommentWriter.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/CommentControllerTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentWriterTest.java`

- [x] **Step 1: 更新领域命令**

`CommentCreateCommand` 改为包含：

```java
public record CommentCreateCommand(
        int userId,
        CommentType type,
        Integer topicId,
        Integer parentId,
        Integer replyUserId,
        String content,
        String clientIp,
        String userAgent
) {
}
```

- [x] **Step 2: 控制器采集客户端信息**

`CommentController.saveComment` 增加 `HttpServletRequest servletRequest` 参数，并传入：

```java
ClientIpResolver.resolve(servletRequest),
UserAgentResolver.resolve(servletRequest)
```

- [x] **Step 3: 应用服务传递审计字段**

`CommentCommandService.createComment` 增加 `String clientIp, String userAgent` 参数，并构造：

```java
new CommentCreateCommand(
        userId,
        commentType,
        topicId,
        parentId,
        replyUserId,
        content,
        clientIp,
        userAgent)
```

- [x] **Step 4: 数据库写入审计字段**

`DatabaseCommentWriter` 的 insert 改为：

```sql
insert into t_comment (
    user_id, reply_user_id, topic_id, comment_content,
    parent_id, type, is_delete, is_review,
    create_ip, user_agent,
    create_time, update_time
)
values (?, ?, ?, ?, ?, ?, 0, 0, ?, ?, ?, ?)
```

参数顺序为：

```java
ps.setString(7, command.clientIp());
ps.setString(8, command.userAgent());
ps.setTimestamp(9, Timestamp.valueOf(now));
ps.setTimestamp(10, Timestamp.valueOf(now));
```

- [x] **Step 5: 新增写入层测试**

在 `DatabaseCommentWriterTest` 增加：

```java
@Test
void savesClientIpAndUserAgentWhenCreatingComment() {
    int id = writer.save(new CommentCreateCommand(
            2,
            CommentType.ARTICLE,
            1,
            null,
            null,
            "带审计信息的评论",
            "203.0.113.77",
            "JUnit Browser"));

    Map<String, Object> row = jdbcTemplate.queryForMap(
            "select create_ip, user_agent from t_comment where id = ?",
            id);

    assertThat(row.get("create_ip")).isEqualTo("203.0.113.77");
    assertThat(row.get("user_agent")).isEqualTo("JUnit Browser");
}
```

- [x] **Step 6: 新增控制器测试**

在 `CommentControllerTest` 的提交评论测试中追加：

```java
.header("X-Forwarded-For", "203.0.113.77")
.header("User-Agent", "JUnit Browser")
```

并在请求后用 `JdbcTemplate` 查询新评论的 `create_ip`、`user_agent`。

- [x] **Step 7: 运行测试**

Run:

```powershell
mvn -pl MyBlog-springboot-v2 "-Dtest=CommentControllerTest,DatabaseCommentWriterTest" test
```

Expected: `BUILD SUCCESS`。

- [x] **Step 8: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/CommentCreateCommand.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/CommentController.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/application/CommentCommandService.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseCommentWriter.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/CommentControllerTest.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseCommentWriterTest.java
git commit -m "记录后端V2评论提交审计信息"
```

---

## Task 4: 后台审核删除恢复写入操作者审计

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerationCommand.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDeletionCommand.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentRestoreCommand.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentController.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentModerator.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentModeratorTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java`

- [ ] **Step 1: 更新后台命令对象**

```java
public record AdminCommentModerationCommand(List<Integer> ids, boolean reviewed, int operatorUserId) {
}

public record AdminCommentDeletionCommand(List<Integer> ids, int operatorUserId) {
}

public record AdminCommentRestoreCommand(List<Integer> ids, int operatorUserId) {
}
```

- [ ] **Step 2: 后台控制器读取当前管理员**

`AdminCommentController` 引入：

```java
import com.aurora.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.aurora.myblog.v2.common.auth.CurrentUser;
```

三个写接口分别改为：

```java
ApiResponse<AdminCommentCommandService.Result> review(
        @CurrentUser AuthenticatedPrincipal currentUser,
        @Valid @RequestBody AdminCommentReviewRequest request) {
    return ApiResponse.ok(commandService.review(
            new AdminCommentModerationCommand(request.ids(), request.reviewed(), currentUser.id())));
}
```

删除和恢复同理，把 `currentUser.id()` 传给命令。

- [ ] **Step 3: 审核写入审计字段**

审核 SQL 改为：

```sql
update t_comment
set is_review = ?,
    reviewed_by = ?,
    review_time = ?,
    update_time = ?
where id in (...)
```

- [ ] **Step 4: 删除写入审计字段**

删除 SQL 改为：

```sql
update t_comment
set is_delete = 1,
    deleted_by = ?,
    delete_time = ?,
    update_time = ?
where id in (...)
```

- [ ] **Step 5: 恢复写入审计字段**

恢复 SQL 改为：

```sql
update t_comment
set is_delete = 0,
    restored_by = ?,
    restore_time = ?,
    update_time = ?
where id in (...)
```

- [ ] **Step 6: 写入层测试**

在 `DatabaseAdminCommentModeratorTest` 分别断言：

```java
assertThat(row.get("reviewed_by")).isEqualTo(1);
assertThat(row.get("review_time")).isNotNull();
assertThat(row.get("deleted_by")).isEqualTo(1);
assertThat(row.get("delete_time")).isNotNull();
assertThat(row.get("restored_by")).isEqualTo(1);
assertThat(row.get("restore_time")).isNotNull();
```

- [ ] **Step 7: 控制器测试**

在 `AdminCommentControllerTest` 的 review/delete/restore 请求后查询数据库，确认操作者来自测试登录用户。

- [ ] **Step 8: 运行测试**

Run:

```powershell
mvn -pl MyBlog-springboot-v2 "-Dtest=DatabaseAdminCommentModeratorTest,AdminCommentControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 9: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentModerationCommand.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDeletionCommand.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentRestoreCommand.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentController.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentModerator.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentModeratorTest.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java
git commit -m "记录后端V2评论后台操作审计"
```

---

## Task 5: 后台评论详情返回审计字段

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDetail.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentDetailResponse.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentReader.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java`
- Test: `MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java`

- [ ] **Step 1: 扩展详情领域模型**

`AdminCommentDetail` 追加：

```java
String createIp,
String userAgent,
Integer reviewedBy,
LocalDateTime reviewTime,
Integer deletedBy,
LocalDateTime deleteTime,
Integer restoredBy,
LocalDateTime restoreTime
```

- [ ] **Step 2: 扩展详情响应 DTO**

`AdminCommentDetailResponse` 追加同名字段，并在 `from` 方法中映射。

- [ ] **Step 3: 扩展详情 SQL**

`DatabaseAdminCommentReader.findDetail` 查询增加：

```sql
c.create_ip,
c.user_agent,
c.reviewed_by,
c.review_time,
c.deleted_by,
c.delete_time,
c.restored_by,
c.restore_time
```

- [ ] **Step 4: 读取层测试**

在 `DatabaseAdminCommentReaderTest` 增加断言：

```java
assertThat(detail.get().createIp()).isEqualTo("203.0.113.1");
assertThat(detail.get().userAgent()).isEqualTo("JUnit Browser");
assertThat(detail.get().reviewedBy()).isEqualTo(1);
assertThat(detail.get().reviewTime()).isNotNull();
```

- [ ] **Step 5: 控制器响应测试**

在 `AdminCommentControllerTest` 的详情接口测试中追加：

```java
.andExpect(jsonPath("$.data.createIp").value("203.0.113.1"))
.andExpect(jsonPath("$.data.userAgent").value("JUnit Browser"))
.andExpect(jsonPath("$.data.reviewedBy").value(1))
.andExpect(jsonPath("$.data.reviewTime").exists());
```

- [ ] **Step 6: 运行测试**

Run:

```powershell
mvn -pl MyBlog-springboot-v2 "-Dtest=DatabaseAdminCommentReaderTest,AdminCommentControllerTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 7: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/domain/AdminCommentDetail.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/api/AdminCommentDetailResponse.java `
        MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/comment/infrastructure/DatabaseAdminCommentReader.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/DatabaseAdminCommentReaderTest.java `
        MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/comment/AdminCommentControllerTest.java
git commit -m "展示后端V2评论审计详情"
```

---

## Task 6: 回归验证和计划收尾

**Files:**

- Modify: `docs/superpowers/plans/2026-05-31-backend-v2-comment-audit-schema.zh-CN.md`

- [ ] **Step 1: 运行评论模块测试**

Run:

```powershell
mvn -pl MyBlog-springboot-v2 "-Dtest=CommentControllerTest,CommentCommandServiceTest,DatabaseCommentReaderTest,DatabaseCommentWriterTest,AdminCommentControllerTest,DatabaseAdminCommentReaderTest,DatabaseAdminCommentModeratorTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 运行全量测试**

Run:

```powershell
mvn -pl MyBlog-springboot-v2 test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 3: 打包验证**

Run:

```powershell
mvn -pl MyBlog-springboot-v2 package
```

Expected: `BUILD SUCCESS`，生成 V2 jar。

- [ ] **Step 4: 本地 MySQL 只读结构检查**

如果你已经手工执行过 `docs/superpowers/specs/2026-05-31-backend-v2-comment-audit-schema.sql`，再运行：

```powershell
mysql -h localhost -P 3306 -u root -p -D aurora -e "select column_name from information_schema.columns where table_schema = database() and table_name = 't_comment' and column_name in ('create_ip','user_agent','reviewed_by','review_time','deleted_by','delete_time','restored_by','restore_time') order by ordinal_position;"
```

Expected: 返回 8 个新增字段。

如果没有手工执行 SQL，这一步记录为“跳过，真实 MySQL 结构尚未变更”，不要把它当失败。

- [ ] **Step 5: 更新本计划完成状态**

把已经完成的任务 checkbox 从 `- [ ]` 改为 `- [x]`，并在文末追加实际验证结果：

```markdown
## 实施记录

- 评论模块测试：通过。
- 后端 V2 全量测试：通过。
- 打包验证：通过。
- 真实 MySQL 迁移：未自动执行；如已手工执行，记录执行时间和验证结果。
```

- [ ] **Step 6: 提交**

```powershell
git add docs/superpowers/plans/2026-05-31-backend-v2-comment-audit-schema.zh-CN.md
git commit -m "同步后端V2评论审计计划状态"
```

---

## 验收标准

- 评论提交会记录 `create_ip` 和 `user_agent`。
- 后台审核会记录 `reviewed_by` 和 `review_time`。
- 后台软删除会记录 `deleted_by` 和 `delete_time`。
- 后台恢复会记录 `restored_by` 和 `restore_time`。
- 后台详情能读取并返回审计字段。
- 前台公开评论接口仍然只返回 `is_delete = 0` 且 `is_review = 1` 的评论。
- local profile 仍不自动执行 Flyway。
- 真实 MySQL 迁移脚本存在，但不会被自动执行。
- 评论模块测试、后端 V2 全量测试、打包验证通过。

## 暂不处理

- MyBatis Plus 切换。
- Redis 评论限流。
- MQ 评论通知。
- 敏感词过滤。
- 评论举报。
- 操作审计独立流水表。
- 自动部署。
- 前台和后台 Vue 页面改造。
