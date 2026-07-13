# MyBlog V2 开发手册

> 状态：当前有效
> 适用范围：MyBlog V2
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：开发手册入口

## 目录职责

| 目录 | 唯一职责 |
| --- | --- |
| [`start-here/`](start-here/project-overview.md) | 项目概览、当前状态、路线图、开放问题、踩坑记录和术语 |
| [`architecture/`](architecture/README.md) | 当前系统结构和关键数据流 |
| [`adr/`](adr/README.md) | 仍影响当前实现的架构决策及理由 |
| [`rules/`](rules/README.md) | 开发、测试、安全和文档维护约束 |
| [`api/`](api/README.md) | 前后端联调所需的 HTTP 契约 |
| [`product/`](product/README.md) | 稳定业务范围、规则和数据语义 |
| [`content/`](content/markdown-authoring.md) | 文章 Markdown 写作、预览与历史恢复规范 |
| [`frontend/`](frontend/README.md) | blog 与 admin 的当前实现边界 |
| [`ops/`](ops/README.md) | 本地运行、环境变量、测试、CI、部署和发布 |
| [`workflows/`](workflows/README.md) | 可重复执行且仍会使用的开发流程 |

## 使用原则

1. `current-status.md` 只记录已实现能力、当前发布阻塞项和最近验证状态。
2. `open-issues.md` 只保留尚未解决的事项，关闭项从正文删除并由 Git 追溯。
3. `roadmap.md` 只记录未来顺序，不复制已完成历史。
4. API、架构、产品和前端文档分别维护各自事实，不复制整段内容。
5. 代码、配置、迁移或测试发生变化时，同一提交必须更新受影响的权威文档。

详细规则见 [`rules/documentation.md`](rules/documentation.md)。
