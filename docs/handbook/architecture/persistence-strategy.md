# 持久化策略

> 状态：当前有效
> 适用范围：MyBlog V2 后端持久化
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`、`MyBlog-springboot-v2/src/main/resources/mapper/`、`MyBlog-springboot-v2/src/main/resources/db/migration/`
> 权威程度：架构权威说明

## 技术与职责

| 能力 | 当前实现 |
| --- | --- |
| 单表 CRUD | MyBatis-Plus 3.5.12 `BaseMapper` 与 Wrapper |
| 复杂查询 | 模块化 MyBatis XML |
| 数据库演进 | Flyway V1–V4 |
| 事务 | Spring `@Transactional`，边界位于 application |
| 审计与软删除 | `BaseEntity`、`AuditOnlyBase`、`AuditFieldHandler`、`@TableLogic` |
| 迁移验证 | H2 常规测试 + Testcontainers MySQL 条件测试 |

生产代码不使用 `JdbcTemplate` 作为 Repository 实现；测试可以使用 `JdbcTemplate` 准备数据和断言数据库状态。

## Repository 边界

业务模块在 domain 或 application 定义具有业务语义的端口，infrastructure 使用 Mapper 实现。Application 只依赖端口，不依赖 Entity、Mapper 或 XML 细节。

```text
application/domain port
        ▲
        │ implements
infrastructure repository
        │
        ├── MyBatis-Plus mapper
        └── XML mapper
```

跨模块调用必须通过目标模块 application 能力，不能跨模块复用 Mapper。

## SQL 放置

- `BaseMapper` / Wrapper：单表、主键、简单等值和更新。
- Mapper 注解：仅用于稳定、短小、无动态拼接的简单 SQL。
- XML：join、动态筛选、聚合、分页 projection、批量和行锁查询。

具体规则见 `../rules/sql-placement.md`。

## Flyway

迁移位于 `src/main/resources/db/migration/`。已经进入主线的版本不可修改语义；Schema 变化只能新增更高版本迁移。H2 测试用于快速反馈，发布前必须运行真实 MySQL 方言验证。

## 时间、审计和删除

- 业务时间使用注入的 Asia/Tokyo `Clock`。
- 标准业务实体使用应用生成 Snowflake ID 和审计列。
- `AuditFieldHandler` 填充创建、更新时刻和当前后台账号。
- 软删除业务必须同时维护 `deleted`、`deleted_at`、`deleted_by`。
- 关联表、refresh token、访问日志、日聚合和邮件日志按 `schema-design.md` 使用显式例外结构。

## 并发

登录失败累计、refresh 轮换、改密、资料更新、分类标签、友链排序、评论状态和计数等并发场景使用行锁、条件更新或事务收敛。并发语义必须由对应数据库测试证明，不能只依赖方法级 `synchronized`。
