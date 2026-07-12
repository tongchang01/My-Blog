# 持续集成、镜像发布与部署边界

> 状态：当前有效；自动部署待首次真实演练
> 适用范围：`.github/workflows/ci.yml`、`.github/workflows/images.yml`
> 最后校准：2026-07-12
> 对应代码：`.github/workflows/`
> 权威程度：CI 说明

CI 在 main 的 pull request、push 和手动 workflow dispatch 时运行，仓库权限为 contents read。main 的 push 和版本 tag 还会触发 GHCR 镜像发布；main 镜像发布成功后，deploy job 使用 GitHub OIDC 与临时 SSH /32 部署同一 SHA 到生产 EC2。

| Job | 当前验证 |
| --- | --- |
| Backend tests | Java 17，JST 时区，排除三个真实 MySQL 专项后的 Maven 测试 |
| Backend MySQL integration tests | MySQL 8.4 Testcontainers：Flyway、改密并发、登录失败并发 |
| Linux PowerShell initialization contract | Ubuntu `pwsh`：本地 MySQL 初始化脚本的凭据、数据库名和非空库安全边界 |
| Admin frontend tests | pnpm 9.15.9、Node 22：typecheck、test、build |
| Blog frontend tests | pnpm 9.15.9、Node 22：typecheck、test、build |
| Publish container images | main/tag：使用提交 SHA 构建并推送 `myblog-api` 与 `myblog-web` 到 GHCR |
| Deploy production | 仅 main、依赖 publish：临时放行当前 Runner 的 SSH /32，调用受限 deploy 用户部署同 SHA，并始终撤销规则 |

后端 MySQL job 预拉镜像并设置 15 分钟上限。只有镜像拉取或容器启动这类基础设施失败可以直接 rerun；迁移、SQL 或断言失败必须先诊断代码。

新增检查必须能在干净 runner 稳定复现，不依赖生产密钥或个人本机状态，并对失败提供可操作反馈。CI 通过不替代真实环境的 CORS、反向代理、客户端 IP、S3、数据库备份和冒烟验证。

CD 不替代生产配置注入、数据库备份、迁移评审、产品冒烟或回滚判断。首次启用前必须完成 [`github-ssh-cd.md`](github-ssh-cd.md) 的 AWS/GitHub/服务器演练；Flyway 或数据问题不会自动回滚。
