# 持久化策略

> 本文档回答："数据怎么读写？现在的混用状况如何？目标是什么？"
> 适用范围：V2 所有持久层代码。
> 相关 ADR：ADR-0005（MyBatis-Plus 为主）、ADR-0010（SQL 分层）

## 1. 当前并存的两种 ORM

| ORM | 用途 | 状态 |
|-----|------|------|
| **MyBatis-Plus 3.5.12** | V2 新写代码的主 ORM | ✅ 主推 |
| **JdbcTemplate** | V1 遗留代码、少量过渡期使用 | ⚠️ 逐步替换 |

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
| `local` | 真实 MySQL | 关闭 | 本地开发，避免改坏开发库 |
| `test` | H2 内存 | 启动时迁移 | 自动化测试 |
| `prod` | 生产 MySQL | 手动审视后启用 | 生产环境 |

## 7. 与 V1 的关系（已转向全量重设计）

⚠️ **本节策略已变更**。早期 V2 重构假设"兼容 V1 库结构"，现已确定走**全量重设计**：

- V1 数据库结构仅作**参考**，不作兼容目标
- 新 schema 在 `arch/schema-design.md` 单独定稿
- V1 数据量极小（不到 20 篇文章），用一次性脚本导入即可，见 `migration/v1-data-import.md`
- 取代该假设的决策见 ADR-0013（V2 不再兼容 V1 数据结构）

历史遗留：已写的 Entity 中带有"旧库兼容"注释（如 `is_review` Integer 包装），在业务清空重写时一并清理。

## 8. 旧实现盘点（不作为当前行动项）

> ⚠️ **不是 TODO**。按 `status.md` / `roadmap.md` 当前主线，DDL 冻结前**停止修旧实现**，下表只用于"DDL 冻结后清理 / 重建"时回顾。

| 项 | 描述 | 处置时机 |
|----|------|------|
| `ContentCatalogMapper` 含 @Select 长查询 | 应放到 XML mapper（rules/sql-placement.md） | M3 模块重建时按新规则重写 |
| JdbcTemplate 与 MyBatis-Plus 并存 | V1 遗留 JdbcTemplate 残留 | M1 清理时随业务层一起删除 |
| 尚无 `src/main/resources/mapper/` 目录 | 首个 XML mapper 落地时同步创建 | M3 第一个复杂查询出现时建 |
| `TokenRevocationStore` 是内存实现 | V2 单实例部署，进程内 Caffeine 已够用（R7 D6） | 不计划迁 Redis；多实例部署是 V3 议题 |

## 9. 相关文档

- ADR：`../decisions/0005-mybatis-plus-as-primary-orm.md`、`../decisions/0010-sql-placement-strategy.md`
- 规则：`../rules/sql-placement.md`
- pitfalls：`../pitfalls.md`
