# ADR-0010: SQL 写法分层（BaseMapper / @Select / XML）

- 状态：accepted
- 日期：2026-04
- 决策者：项目负责人

## 背景

MyBatis-Plus 支持三种写 SQL 的方式：
1. `BaseMapper` 通用 CRUD
2. `@Select`/`@Update` 等注解
3. XML mapper

混用易导致：业务复杂查询散落在注解中、修改 SQL 不便维护、调优困难。

## 决定

按场景收敛：

- **BaseMapper 通用方法**：简单按主键、按字段精确匹配查询/写入
- **@Select 注解**：仅限**一句话能写完**的简单查询（单表、无动态拼接）
- **XML mapper**：所有复杂查询（多表 join、动态条件、聚合）必须走 XML

详见 `rules/sql-placement.md`。

## 理由

- 复杂 SQL 走 XML：便于审查、调优、写测试
- 简单查询走 BaseMapper：零代码
- 注解保留少量"一句话查询"场景，避免过度教条

## 后果

正面：SQL 维护性大幅提升，复杂 SQL 集中
负面：需要一份"何时走哪里"的规则约束（已有 `rules/sql-placement.md`）

后续需关注：
- `ContentCatalogMapper` 现有 @Select 查询是典型违例，需迁移到 XML

## 相关

- 相关 rules：`rules/sql-placement.md`
- 已知违例：`ContentCatalogMapper`
