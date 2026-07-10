# 持续集成与部署边界

> 状态：当前有效
> 适用范围：`.github/workflows/ci.yml`
> 最后校准：2026-07-10
> 对应代码：`.github/workflows/ci.yml`
> 权威程度：CI 说明

CI 在 main 的 pull request、push 和手动 workflow dispatch 时运行，仓库权限为 contents read。当前没有 CD、制品发布或服务器操作。

| Job | 当前验证 |
| --- | --- |
| Backend tests | Java 17，JST 时区，排除三个真实 MySQL 专项后的 Maven 测试 |
| Backend MySQL integration tests | MySQL 8.4 Testcontainers：Flyway、改密并发、登录失败并发 |
| Admin frontend tests | pnpm 9.15.9、Node 22：typecheck、test、build |
| Blog frontend tests | pnpm 9.15.9、Node 22：typecheck、test、build |

后端 MySQL job 预拉镜像并设置 15 分钟上限。只有镜像拉取或容器启动这类基础设施失败可以直接 rerun；迁移、SQL 或断言失败必须先诊断代码。

新增检查必须能在干净 runner 稳定复现，不依赖生产密钥或个人本机状态，并对失败提供可操作反馈。CI 通过不替代真实环境的 CORS、反向代理、客户端 IP、S3、数据库备份和冒烟验证。

CD 只能在服务器拓扑、密钥注入、迁移、回滚和审批方式明确后单独设计。
