# 运行、验证与发布

> 状态：当前有效
> 适用范围：MyBlog V2 本地开发、CI 和部署准备
> 最后校准：2026-07-10
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
| [部署方向](deployment-direction.md) | 已确认的生产架构、供应商边界与上线阻塞项 |
| [生产运行手册](production-runbook.md) | 当前 EC2 原地重建、验收、回滚和七天收尾顺序 |
| [发布检查](release-checklist.md) | 真实环境上线与回滚门槛 |

CI、测试和本地开发中可复用的故障经验统一记录在 [`../start-here/pitfalls.md`](../start-here/pitfalls.md)，完整过程由 Git 历史追溯。
