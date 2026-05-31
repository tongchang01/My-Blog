# 后端 V2 本地评论审计表结构迁移执行计划

> **给执行该计划的代理：** 必须使用 `superpowers:executing-plans` 按任务逐个执行。步骤使用 checkbox（`- [ ]`）语法跟踪状态。

**目标：** 把后端 V2 已经依赖的评论审计字段落到本地 MySQL `aurora.t_comment`，并验证 V2 local profile 连接真实库后评论相关 SQL 不再因为字段缺失失败。

**架构：** 本次只操作本地 MySQL 的 `aurora.t_comment`，不改线上库，不改 Java 业务代码，不启用 local Flyway 自动迁移。SQL 来源为 `docs/superpowers/specs/2026-05-31-backend-v2-comment-audit-schema.sql`，执行前后都做只读结构检查。

**Tech Stack:** MySQL 8、本地 `aurora` 数据库、Spring Boot local profile、JdbcTemplate、手工 SQL、PowerShell、Maven。

---

## 边界和决策

- 本计划会真实修改本地 MySQL `aurora.t_comment`。
- 用户已确认本地 MySQL 可以大胆修改，不会影响线上。
- 本计划不修改线上数据库。
- 本计划不修改 `application-local.yml` 的 `spring.flyway.enabled=false`。
- 本计划不把数据库密码写入 Git。
- 本计划不新增 Java 功能，只做本地库结构落地和验证。
- 如果字段或索引已经存在，执行时应跳过对应 SQL，避免重复执行失败。
- 如果 `t_comment` 不存在，立即停止，不创建新表，因为这说明当前连接的库不是预期的旧库。

## 预期新增字段

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `create_ip` | `varchar(45)` | 评论提交 IP |
| `user_agent` | `varchar(255)` | 评论提交 User-Agent |
| `reviewed_by` | `int` | 最后审核人用户 ID |
| `review_time` | `timestamp` | 最后审核时间 |
| `deleted_by` | `int` | 最后删除人用户 ID |
| `delete_time` | `timestamp` | 最后删除时间 |
| `restored_by` | `int` | 最后恢复人用户 ID |
| `restore_time` | `timestamp` | 最后恢复时间 |

## 预期新增索引

| 索引 | 字段 |
| --- | --- |
| `idx_comment_review_delete_time` | `is_review, is_delete, create_time` |
| `idx_comment_parent_delete_review` | `parent_id, is_delete, is_review` |

---

## Task 1: 执行前只读检查

**Files:**

- Read: `docs/superpowers/specs/2026-05-31-backend-v2-comment-audit-schema.sql`

- [ ] **Step 1: 确认当前工作区干净**

Run:

```powershell
git status --short
```

Expected: 无输出。

- [ ] **Step 2: 确认本地 MySQL 可连接**

Run:

```powershell
mysql -h localhost -P 3306 -u root -p -D aurora -e "select database();"
```

Expected: 返回 `aurora`。

- [ ] **Step 3: 确认 `t_comment` 存在**

Run:

```powershell
mysql -h localhost -P 3306 -u root -p -D aurora -e "show tables like 't_comment';"
```

Expected: 返回 `t_comment`。如果没有返回，停止执行。

- [ ] **Step 4: 检查目标字段是否已存在**

Run:

```powershell
mysql -h localhost -P 3306 -u root -p -D aurora -e "select column_name, column_type, is_nullable from information_schema.columns where table_schema = database() and table_name = 't_comment' and column_name in ('create_ip','user_agent','reviewed_by','review_time','deleted_by','delete_time','restored_by','restore_time') order by ordinal_position;"
```

Expected: 如果未迁移，应返回 0 行；如果部分字段存在，记录已存在字段，后续只补缺失字段。

- [ ] **Step 5: 检查目标索引是否已存在**

Run:

```powershell
mysql -h localhost -P 3306 -u root -p -D aurora -e "show index from t_comment where Key_name in ('idx_comment_review_delete_time','idx_comment_parent_delete_review');"
```

Expected: 如果未迁移，应返回 0 行；如果部分索引存在，记录已存在索引，后续只补缺失索引。

---

## Task 2: 执行本地 MySQL 迁移

**Files:**

- Read: `docs/superpowers/specs/2026-05-31-backend-v2-comment-audit-schema.sql`

- [ ] **Step 1: 执行字段新增 SQL**

如果 8 个字段都不存在，执行：

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

如果只有部分字段缺失，只对缺失字段执行 `alter table ... add column ...`。

- [ ] **Step 2: 执行索引新增 SQL**

如果索引不存在，执行：

```sql
create index idx_comment_review_delete_time
    on t_comment (is_review, is_delete, create_time);

create index idx_comment_parent_delete_review
    on t_comment (parent_id, is_delete, is_review);
```

如果只有部分索引缺失，只创建缺失索引。

---

## Task 3: 迁移后只读验证

**Files:**

- Modify: `docs/superpowers/plans/2026-05-31-backend-v2-local-comment-audit-migration.zh-CN.md`

- [ ] **Step 1: 验证 8 个字段存在**

Run:

```powershell
mysql -h localhost -P 3306 -u root -p -D aurora -e "select column_name, column_type, is_nullable from information_schema.columns where table_schema = database() and table_name = 't_comment' and column_name in ('create_ip','user_agent','reviewed_by','review_time','deleted_by','delete_time','restored_by','restore_time') order by ordinal_position;"
```

Expected: 返回 8 行。

- [ ] **Step 2: 验证 2 个索引存在**

Run:

```powershell
mysql -h localhost -P 3306 -u root -p -D aurora -e "show index from t_comment where Key_name in ('idx_comment_review_delete_time','idx_comment_parent_delete_review');"
```

Expected: 返回两个索引的字段明细。

- [ ] **Step 3: 验证现有评论数据量未丢失**

Run:

```powershell
mysql -h localhost -P 3306 -u root -p -D aurora -e "select count(*) as comment_count from t_comment;"
```

Expected: 返回执行前后相同或符合预期的数据量。

---

## Task 4: V2 local profile 冒烟验证

**Files:**

- Read: `MyBlog-springboot-v2/src/main/resources/application-local.yml`

- [ ] **Step 1: 运行后端 V2 评论相关测试**

Run:

```powershell
mvn "-Dtest=CommentControllerTest,DatabaseCommentWriterTest,AdminCommentControllerTest,DatabaseAdminCommentReaderTest,DatabaseAdminCommentModeratorTest" test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 如果本地服务可启动，执行 local profile 冒烟**

Run:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Expected: 服务启动成功，不因 `t_comment` 审计字段缺失报 SQL 错误。

如果本地管理员登录数据不可用，记录为“服务启动验证通过，接口写入冒烟跳过”。

---

## Task 5: 记录结果并提交

**Files:**

- Modify: `docs/superpowers/plans/2026-05-31-backend-v2-local-comment-audit-migration.zh-CN.md`

- [ ] **Step 1: 更新 checkbox 和实施记录**

在本文末尾追加：

```markdown
## 实施记录

- 执行时间：
- 目标库：
- 字段迁移：
- 索引迁移：
- 评论数据量：
- V2 测试结果：
- local profile 冒烟：
```

- [ ] **Step 2: 提交文档状态**

Run:

```powershell
git add docs/superpowers/plans/2026-05-31-backend-v2-local-comment-audit-migration.zh-CN.md
git commit -m "新增后端V2本地评论审计迁移计划"
```

## 回滚方案

如果本地迁移后需要回滚，可以在确认没有新代码依赖本地审计字段数据后执行：

```sql
drop index idx_comment_review_delete_time on t_comment;
drop index idx_comment_parent_delete_review on t_comment;

alter table t_comment
    drop column create_ip,
    drop column user_agent,
    drop column reviewed_by,
    drop column review_time,
    drop column deleted_by,
    drop column delete_time,
    drop column restored_by,
    drop column restore_time;
```

注意：回滚会删除本地新增审计数据。生产或预发环境不得直接套用本回滚 SQL，必须先备份和评估。
