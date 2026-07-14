# 运行、验证与发布

> 状态：当前有效
> 适用范围：MyBlog V2 本地开发、CI 与生产运行
> 最后校准：2026-07-14
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/`、`.github/workflows/ci.yml`
> 权威程度：运维导航

| 文档 | 用途 |
| --- | --- |
| [本地启动](local-development.md) | 后端、博客端和管理端启动顺序 |
| [本地 MySQL](local-mysql-development.md) | 安全初始化、种子和验收脚本 |
| [环境变量](environment.md) | local/prod 配置来源与必填项 |
| [构建与测试](build-and-test.md) | 本地和阶段验证命令 |
| [持续集成](ci-cd.md) | 当前 CI job 与 CD 边界 |
| [GitHub SSH CD](github-ssh-cd.md) | OIDC、临时 SSH 放行、部署用户与首次演练 |
| [部署方向](deployment-direction.md) | 已确认的生产架构、供应商边界与持续验证项 |
| [生产运行手册](production-runbook.md) | 生产核对、故障恢复、验收与回滚顺序 |
| [发布检查](release-checklist.md) | 每次发布或恢复的真实环境门槛 |

CI、测试和本地开发中可复用的故障经验统一记录在 [`../start-here/pitfalls.md`](../start-here/pitfalls.md)，完整过程由 Git 历史追溯。
