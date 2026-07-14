# MyBlog 文档入口

> 状态：当前有效
> 适用范围：全项目文档
> 最后校准：2026-07-10
> 对应代码：`README.md`、`AGENTS.md`
> 权威程度：文档总入口

## 目录

| 目录 | 内容 | 使用方式 |
| --- | --- | --- |
| [`handbook/`](handbook/README.md) | 当前架构、规则、API、产品、前端和运维说明 | 开发与部署的权威文档 |
| [`governance/`](governance/README.md) | 分支、提交和仓库协作规则 | 仓库治理的权威文档 |
| [`showcase/`](showcase/README.md) | 中文、英文、日文项目展示 | 对外介绍，不作为开发依据 |

`handbook/` 和 `governance/` 是当前事实来源；`superpowers/` 中的计划与设计稿只用于追溯当时的决策，不得作为当前状态依据。其他历史过程由 Git 追溯。

## 阅读路径

- 了解项目：[`handbook/start-here/project-overview.md`](handbook/start-here/project-overview.md)
- 查看当前进度：[`handbook/start-here/current-status.md`](handbook/start-here/current-status.md)
- 查看遗留事项：[`handbook/start-here/open-issues.md`](handbook/start-here/open-issues.md)
- 查看项目踩坑：[`handbook/start-here/pitfalls.md`](handbook/start-here/pitfalls.md)
- 编写或恢复文章：[`handbook/content/markdown-authoring.md`](handbook/content/markdown-authoring.md)
- 开始开发：[`handbook/rules/README.md`](handbook/rules/README.md) 与对应模块文档
- 本地运行：[`handbook/ops/local-development.md`](handbook/ops/local-development.md)
- 规划生产部署：[`handbook/ops/deployment-direction.md`](handbook/ops/deployment-direction.md)
- 执行生产部署：[`handbook/ops/production-runbook.md`](handbook/ops/production-runbook.md) 与 [`handbook/ops/release-checklist.md`](handbook/ops/release-checklist.md)

## 事实优先级

发生冲突时按以下顺序处理：

1. 当前代码、数据库迁移、配置和可执行测试。
2. `handbook/` 中已标记“当前有效”的权威说明。
3. 已采纳且仍适用的 ADR。
4. `governance/` 中的仓库协作规则。

发现不一致时必须同步修正文档或登记到 `handbook/start-here/open-issues.md`，不能继续复制冲突内容。
