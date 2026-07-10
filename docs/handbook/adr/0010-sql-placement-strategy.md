# ADR-0010：按复杂度分配 MyBatis SQL

> 状态：当前有效
> 适用范围：V2 后端持久化层
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/`、`MyBlog-springboot-v2/src/main/resources/mapper/`
> 权威程度：ADR

## 背景

MyBatis-Plus 同时提供通用 CRUD、条件构造器、注解 SQL 和 XML Mapper。缺少统一边界会让复杂 SQL 分散在 Java 代码中，增加审查与调优成本。

## 决策

- 单表通用 CRUD 与简单条件查询使用 `BaseMapper` 和 Wrapper。
- 稳定、短小、无动态拼接的单表 SQL 可以使用 Mapper 注解，但当前生产代码没有此类用法。
- 多表关联、聚合、批量更新和动态条件统一放在 `src/main/resources/mapper/` 下的 XML。
- Mapper 接口与 XML 使用相同的业务模块目录和方法名。

## 结果

复杂 SQL 有统一审查位置，Java 代码保留类型和调用关系。详细判定规则见 `../rules/sql-placement.md`。
