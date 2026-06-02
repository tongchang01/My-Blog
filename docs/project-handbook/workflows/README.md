# workflows/ — 可重复的标准操作流程

> 本目录回答："我要做 X 这件事，按什么步骤来？"
> 性质：长期有效的 SOP，面向重复性任务。

## 与 plans/ 的区别

- 旧 `docs/superpowers/plans/` 里的 plan 是**一次性任务清单**（做完就过期）
- 本目录的 workflow 是**可复用的步骤模板**（每次都能照着做）

## 写作约定

- 每个文件描述一类可重复操作
- 结构：`目的` → `前置条件` → `步骤（编号列表）` → `验证方法` → `常见问题`
- 步骤要可执行，含具体命令、文件路径、检查点
- 如果某 workflow 长期没人用、或被自动化取代，删除而非保留

## 计划包含的文件

| 文件 | 用途 |
|------|------|
| `add-new-module.md` | 在 V2 中新增一个业务模块的完整步骤 |
| `add-new-api.md` | 在已有模块中新增一个 REST 接口的步骤 |
| `migrate-jdbc-to-mybatis-plus.md` | 把一个 Repository 从 JdbcTemplate 迁到 MP 的步骤 |
| `add-new-table.md` | 新增数据库表 + 对应 Entity/Mapper 的步骤 |
| `build-and-test.md` | 构建、测试、本地启动、常见问题 |
| `write-adr.md` | 新增一份 ADR 的步骤（含编号查找） |

## 与其它目录的关系

- workflow 中涉及的规则细节链接到 `../rules/`
- workflow 中涉及的设计选择链接到 `../decisions/`
