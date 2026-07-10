# ADR-0015：统一审计列与软删除

> 状态：当前有效
> 适用范围：V2 业务实体与数据库表
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/infrastructure/persistence/`、`MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql`
> 权威程度：ADR

## 背景

业务数据需要记录创建、修改和删除信息，同时兼容 MyBatis-Plus 的自动填充与逻辑删除能力。

## 决策

标准业务实体使用以下八列：`id`、`created_at`、`created_by`、`updated_at`、`updated_by`、`deleted`、`deleted_at`、`deleted_by`。

- `BaseEntity` 提供雪花主键和七个审计字段。
- `AuditOnlyBase` 用于自行定义主键的实体。
- `AuditFieldHandler` 通过注入的 `Clock` 填充创建和修改时间，并从安全上下文填充操作人。
- `deleted` 使用 `@TableLogic(value = "0", delval = "1")`；删除服务显式写入 `deleted_at` 和 `deleted_by`。
- 游客和系统任务没有系统用户身份时，操作人字段允许为 `NULL`。

例外表：

- `t_user_info` 以 `user_id` 作为主键；
- `t_refresh_token` 用 `revoked` 表示失效，不使用软删除三列；
- `t_article_tag` 使用复合主键，不保留审计列；
- `t_page_view`、`t_mail_log` 使用自增主键并仅保留 `created_at`；
- `t_page_view_daily` 使用复合主键并仅保留 `created_at`。

## 结果

普通业务实体共享一致的审计行为，特殊写入模型保持最小字段集。实际列定义以 Flyway 迁移为准。
