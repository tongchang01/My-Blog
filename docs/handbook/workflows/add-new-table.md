# 新增数据库表

> 状态：当前有效
> 适用范围：V2 Flyway、领域模型和持久化实现
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/resources/db/migration/`、`MyBlog-springboot-v2/src/main/java/`
> 权威程度：标准流程

1. 确认表归属模块、业务生命周期、引用关系、主键方式和是否属于审计列例外。
2. 新建下一个版本的 `V{n}__*.sql`；已应用迁移不得修改。
3. 使用 snake_case、`utf8mb4`、中文 COMMENT、`DATETIME` 和全局唯一的索引/约束名。
4. 普通业务表使用 Snowflake `BIGINT` 主键与七个审计字段；日志、关联或复合主键表必须明确例外原因。
5. 不创建 `FOREIGN KEY`；为逻辑引用建立普通索引，并在应用服务校验引用与删除语义。
6. 在 domain 定义业务模型和 Repository 端口，在 infrastructure 定义 Entity、Mapper XML、映射和 Repository 实现。
7. 标准实体继承 `BaseEntity`；自定义主键且保留审计字段的实体继承 `AuditOnlyBase`。软删除服务显式填写删除时间和删除人。
8. 更新 `../architecture/schema-design.md` 和 `../product/data-model.md`。
9. 运行 `FlywayMigrationTest`、相关持久化测试和 ArchUnit；涉及 MySQL 特性时运行 `MySqlFlywayMigrationTest`。

详细 schema、审计和 SQL 规则见 ADR-0014、ADR-0015、ADR-0017、ADR-0018 与 `../rules/sql-placement.md`。
