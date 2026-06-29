# ADR-0005: 以 MyBatis-Plus 作为主 ORM

- 状态：accepted
- 日期：2026-04
- 决策者：项目负责人

## 背景

V1 混用 JdbcTemplate + MyBatis，写法不统一。V2 需要决定持久层方案。

## 备选方案

- 方案 A：纯 JdbcTemplate
- 方案 B：纯 MyBatis（XML 为主）
- 方案 C：MyBatis-Plus（带通用 CRUD 与条件构造）
- 方案 D：Spring Data JPA

## 决定

选 C：MyBatis-Plus 3.5.12 为主，JdbcTemplate 仅过渡期保留，逐步替换。

## 理由

- 与原项目 MyBatis 生态兼容，迁移成本低
- 通用 CRUD 减少样板代码
- 复杂查询仍可走 XML，灵活性保留
- JPA 与现有 SQL 风格差异大，团队学习成本高

## 后果

正面：
- 简单 CRUD 几乎零代码
- 复杂查询不受框架限制
- 与 SQL 直接对应，便于调优

负面：
- 与 JdbcTemplate 并存期间风格不一致（过渡期问题）
- BaseMapper、@Select、XML 三种写法易混乱（见 `rules/sql-placement.md` 收敛规则）

后续需关注：
- 全部业务代码迁离 JdbcTemplate 后，删除相关依赖

## 相关

- 相关 rules：`rules/sql-placement.md`
- 已知违反：`ContentCatalogMapper` 有 @Select 查询需迁 XML
