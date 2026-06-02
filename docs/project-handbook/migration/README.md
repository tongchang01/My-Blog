# migration/ — V1 → V2 数据迁移

> 本目录回答："V1 线上库的数据怎么搬到 V2 新 schema？"
> 性质：一次性脚本 + 操作手册，**不是工程级 ETL**（V1 数据量 <20 篇文章）
> 当前状态：骨架待填，等 `arch/schema-design.md` 定稿后再写。

## 背景

- ADR-0013：V2 不兼容 V1 数据结构
- ADR-0014：V2 schema 重新设计
- V1 仍在线上运行，V2 本地重构完成后**一次性导入**当时的 V1 数据快照

## 计划包含的文件

| 文件 | 内容 | 状态 |
|------|------|------|
| `v1-data-import.md` | 导入总流程：导出 V1 快照 → 转换 → 导入 V2 → 校验 | ⏳ |
| `table-mapping.md` | V1 表/字段 → V2 表/字段 的逐项映射 | ⏳ |
| `data-cleanup.md` | 导入过程中要清洗的数据（脏字段、未审核评论等） | ⏳ |
| `dropped-data.md` | 不迁移的数据及理由（操作日志、定时任务记录等） | ⏳ |

## 工作前置依赖

```
product/feature-inventory.md  ← 决定哪些功能保留 → 哪些表迁移
       ↓
product/data-model.md         ← 业务实体重设计
       ↓
arch/schema-design.md         ← V2 表 DDL 定稿
       ↓
migration/table-mapping.md    ← 才能开始写映射
```

## 写作约定

- 一次性脚本写成 `sql/` 或 `scripts/`（位置后定），不进 Flyway
- 每张表映射都要标"丢字段"和"新字段默认值"
- 导入后必须有 row count 校验报告
