# 可重复开发流程

> 状态：当前有效
> 适用范围：MyBlog V2 开发流程
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/`
> 权威程度：流程入口

本目录只保留会重复执行的标准操作流程。一次性任务计划、阶段评审和已完成迁移说明不进入长期文档。

| 文档 | 用途 |
| --- | --- |
| [`add-new-module.md`](add-new-module.md) | 新增后端业务模块 |
| [`add-new-api.md`](add-new-api.md) | 在现有模块新增 REST API |
| [`add-new-table.md`](add-new-table.md) | 新增表、迁移、持久化模型和验证 |
| [`write-adr.md`](write-adr.md) | 记录新的架构决策 |

流程文档只描述操作顺序和验证门槛。具体约束链接到 `../rules/`，架构事实链接到 `../architecture/`，构建和发布命令链接到 `../ops/`。
