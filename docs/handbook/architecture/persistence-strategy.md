# 持久化策略

> 本文档回答："V2 当前如何读写数据？"
> 适用范围：V2 所有持久层代码。
> 相关 ADR：ADR-0005（MyBatis-Plus 为主）、ADR-0010（SQL 分层）

## 1. 当前持久化技术

| 技术 | 用途 | 状态 |
|-----|------|------|
| **MyBatis-Plus 3.5.12** | 单表访问、Mapper 基础能力 | ✅ 当前标准 |
| **MyBatis XML** | join、动态条件、聚合、分页和 projection | ✅ 当前标准 |

## 2. MyBatis-Plus 使用规范

三种写法分场景：

| 写法 | 何时用 |
|------|--------|
| `BaseMapper` 通用方法 | 单表、按主键/字段精确查询/写入 |
| `@Select` 等注解 | 一句话写得完的简单查询，不含动态拼接 |
| XML mapper | 多表 join、动态条件、聚合等复杂查询 |

详见 `../rules/sql-placement.md`。

## 3. 仓储抽象层

每个业务模块在 `domain` 层定义 Repository 接口（如 `CommentRepository`），实现放 `infrastructure.persistence`：

```
domain.repository.CommentRepository  (接口，定义业务语义)
       ▲
       │ implements
       │
infrastructure.persistence.CommentRepositoryImpl  (用 Mapper 实现)
       │
       └── infrastructure.persistence.mapper.CommentMapper  (MyBatis-Plus)
```

应用层只调 Repository 接口，不直接接触 Mapper。ArchUnit 规则 #3 守护。

## 4. Reader / Writer 拆分（可选）

复杂读场景（多表聚合、自定义 SQL）可拆出独立 `DatabaseXxxReader`，与 Writer 分离：

- `DatabaseXxxReader` — 复杂查询读
- `DatabaseXxxWriter` — 写入与简单读

测试命名：`DatabaseXxxReaderTest` / `DatabaseXxxWriterTest`。

## 5. 数据库迁移

- 使用 **Flyway**
- 迁移脚本位置：`src/main/resources/db/migration`
- 命名：`V{version}__{description}.sql`
- 启动时自动执行
- 测试 profile 用 H2 内存库，Flyway 在启动时跑同一套迁移脚本验证

## 6. 多环境策略

| Profile | 数据库 | Flyway | 用途 |
|---------|--------|--------|------|
| `local` | V2 开发 MySQL | 关闭 | 本地开发，必须显式激活 |
| `test` | H2 内存 | 启动时迁移 | 自动化测试 |
| `prod` | 生产 MySQL | 启动时迁移 | 生产环境，发布前审视迁移脚本 |

应用不提供默认 profile。`local` 与 `prod` 的连接信息统一通过
`MYBLOG_DATASOURCE_URL`、`MYBLOG_DATASOURCE_USERNAME`、`MYBLOG_DATASOURCE_PASSWORD`
注入，不提供 root、空密码或旧数据库名兜底。

## 7. V1 数据迁移边界

V2 运行时不依赖 V1 schema。一次性数据导入见 `../migration/`，决策原因见 ADR-0013；
当前 Entity、SQL 和测试只表达 V2 schema。

## 8. 相关文档

- ADR：`../adr/0005-mybatis-plus-as-primary-orm.md`、`../adr/0010-sql-placement-strategy.md`
- 规则：`../rules/sql-placement.md`
- pitfalls：`../pitfalls.md`
