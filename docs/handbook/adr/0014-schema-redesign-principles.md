# ADR-0014：V2 数据库结构设计原则

> 状态：当前有效
> 适用范围：V2 MySQL schema 与 Flyway 迁移
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/resources/db/migration/`
> 权威程度：ADR

## 背景

V2 不继承 V1 schema，需要稳定的命名、类型、索引和迁移约束，避免实体、SQL 和文档各自定义规则。

## 决策

- 表名使用 `t_` 前缀和单数 snake_case，列名使用 snake_case。
- 业务主键通常使用 `BIGINT`，由应用通过 `IdType.ASSIGN_ID` 生成；日志表和复合主键表按用途例外处理。
- 时间列使用 `DATETIME`，应用时区统一为 `Asia/Tokyo`。
- 布尔状态在 MySQL 中使用带明确语义的 `TINYINT`；业务多状态使用字符串枚举值。
- 字符列使用 `utf8mb4`，表和列必须有中文 `COMMENT`。
- 逻辑关联不建立数据库外键，但必须为查询路径配置普通索引，并由应用服务保证引用有效性。
- 已应用的迁移文件不可改写；结构演进通过新的 `V{n}__*.sql` 完成。
- 索引和约束名称在整个 schema 中保持唯一，保证 Flyway 在目标数据库和测试数据库上均可执行。

## 结果

Flyway 脚本是结构事实来源，完整表清单与关键不变量见 `../architecture/schema-design.md`。审计列和例外表由 ADR-0015 进一步规定。
