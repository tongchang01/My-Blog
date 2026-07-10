# ADR-0005：MyBatis-Plus 作为主要持久化框架

> 状态：当前有效
> 适用范围：V2 后端持久化
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/pom.xml`、`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`、`MyBlog-springboot-v2/src/main/resources/mapper/`
> 权威程度：ADR

## 背景

项目需要保留 SQL 可见性，同时减少单表 CRUD 样板。JPA 与现有 SQL 模型差异较大，纯 XML 会增加简单操作成本。

## 决策

生产持久化使用 MyBatis-Plus 3.5.12：简单单表操作使用 `BaseMapper` 或 Wrapper，复杂查询使用 MyBatis XML。Repository 端口与 Mapper 实现保持分离。

## 结果

- 生产 Repository 不使用 `JdbcTemplate`；测试可用其准备和检查数据。
- SQL 放置规则由 ADR-0010 和 `../rules/sql-placement.md` 约束。
- Flyway 独立负责 Schema 演进。
