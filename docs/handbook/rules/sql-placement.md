# SQL 与持久化代码放置规则

> 状态：当前有效
> 适用范围：V2 MyBatis-Plus、Mapper XML 与 Flyway
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/`、`MyBlog-springboot-v2/src/main/resources/mapper/`、`MyBlog-springboot-v2/src/main/resources/db/migration/`
> 权威程度：规则

| 场景 | 位置 |
| --- | --- |
| 主键 CRUD、简单单表条件 | infrastructure 中的 BaseMapper / Wrapper |
| 极短、固定、无动态条件的单表 SQL | Mapper 注解；当前生产代码没有此用法 |
| join、动态条件、批量、聚合、分页、排序、projection、行锁 | `src/main/resources/mapper/{module}/XxxMapper.xml` |
| schema 变更 | `src/main/resources/db/migration/V{n}__*.sql` |

XML 文件名、namespace 和 statement ID 必须与 Mapper 接口及方法一致。复杂 SQL 使用中文注释说明筛选、聚合、排序、软删除和锁定口径。

禁止在 web、application 或 domain 拼 SQL；禁止字符串拼接动态条件和 IN 列表；禁止生产代码用 `JdbcTemplate` 绕过现有 Repository；禁止跨模块访问 Mapper；禁止把 Entity 或 projection 作为 Web 响应。

排序字段由服务端白名单选择。分页从 1 开始，空结果返回空 records。复杂 XML 需覆盖动态条件、排序、分页、聚合、软删除和并发关键路径。

Flyway 已执行迁移不可改写。新增迁移不得包含数据库外键，索引和约束名必须全局唯一，并通过 H2 迁移测试；发布前涉及 MySQL 方言的变更还需 Testcontainers 或目标 MySQL 验证。
