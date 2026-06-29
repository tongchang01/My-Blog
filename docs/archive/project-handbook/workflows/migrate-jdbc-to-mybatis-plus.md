# 将 JdbcTemplate 代码迁移到 MyBatis-Plus（SOP）

> 目标：把 V1 遗留的 JdbcTemplate 代码逐步替换为 MyBatis-Plus + Repository 抽象。
> 范围：每次迁移一个明确的查询或一个明确的小模块，**不**做大批量重写。

## 1. 前置评估

- 目标 SQL 是否复杂？（决定走 BaseMapper / @Select / XML，参考 `../rules/sql-placement.md`）
- 该查询是否已有调用方测试？无测试 → 先补测试，再迁移
- 该查询是否涉及事务、批处理特殊场景？

## 2. 步骤

### 步骤 1：在 domain 层定义/复用 Repository 接口

把"业务想要做什么"用接口表达。Repository 接口的方法名是业务语义（如 `findUserByUsername`），不是 SQL 语义（如 `selectByUsernameWithLimit1`）。

### 步骤 2：在 infrastructure 写 Mapper + Repository 实现

- 简单查询 → `BaseMapper` 通用方法 / `@Select`
- 复杂查询 → XML mapper（路径 `src/main/resources/mapper/{module}/`）

### 步骤 3：先写测试

- `DatabaseXxxReaderTest` / `DatabaseXxxWriterTest` 用 H2 验证
- 验证新实现行为与旧 JdbcTemplate 一致（字段、排序、null 处理、边界）

### 步骤 4：替换调用点

- 把 application 层中调用 JdbcTemplate 的位置改为调用 Repository 接口
- 若调用点跨模块，确认通过对方 application 层暴露

### 步骤 5：删除 JdbcTemplate 代码

- 旧 Dao 或工具类删除
- 检查是否还有其它调用方，避免误删

### 步骤 6：跑全量测试

`mvn test`，关注 ArchUnit 与业务行为。

## 3. 风险点

- **null 处理差异**：JdbcTemplate 的 ResultSetExtractor 与 MyBatis 的类型处理可能不同（如 Integer 字段为 null 时）
- **排序行为**：MyBatis-Plus 通用方法默认不带 ORDER BY，需显式补
- **大小写**：列名大小写在 MySQL/H2 行为不同，注意写迁移测试时校验
- **批处理**：JdbcTemplate.batchUpdate → MyBatis-Plus 用 `saveBatch` 或自定义批量 XML

## 4. 不要做

- ❌ 一次迁完所有 JdbcTemplate 代码
- ❌ 没测试就直接替换
- ❌ 迁移过程中顺便重构业务逻辑（拆开做）
- ❌ 在 Repository 实现里加事务（事务在 application 层）
