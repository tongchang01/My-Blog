# 持续集成、镜像发布与部署边界

> 状态：当前有效；自动部署已于 2026-07-12 完成首次真实演练
> 适用范围：`.github/workflows/ci.yml`、`.github/workflows/images.yml`
> 最后校准：2026-08-09
> 对应代码：`.github/workflows/`、`deploy/cd/test/workflow-contract-test.sh`、`deploy/web/Caddyfile`、`deploy/web/test/security-headers-integration-test.sh`
> 权威程度：CI 说明

CI 在 main 的 pull request、push 和手动 workflow dispatch 时运行，仓库权限为 contents read。main 的 push 和版本 tag 还会触发 GHCR 镜像发布；main 镜像发布成功后，deploy job 使用 GitHub OIDC 与临时 SSH /32 部署同一 SHA 到生产 EC2。

| Job | 当前验证 |
| --- | --- |
| Backend tests | Java 17，JST 时区：H2 快测及随机端口运行中 API 契约；排除五个真实 MySQL 专项 |
| Backend MySQL integration tests | MySQL 8.4 Testcontainers：Flyway、改密、登录失败、评论审核和首页槽位并发 |
| Linux MySQL initialization | Ubuntu `pwsh` + MySQL 8.4：脚本安全边界，以及基础初始化、重置、跳过种子、重复执行拒绝和最终种子 |
| Deployment workflow contract | Ubuntu `bash` + Caddy 2.11.4：镜像发布、OIDC、临时 SSH 放行、同 SHA 部署、公网冒烟，以及 Caddy 配置语法和静态/代理响应头运行行为 |
| Admin frontend tests | pnpm 9.15.9、Node 24：typecheck、test、build |
| Blog frontend tests | pnpm 9.15.9、Node 24：lint、typecheck、test、build |
| Publish container images | main/tag：使用提交 SHA 构建并推送 `myblog-api` 与 `myblog-web` 到 GHCR |
| Deploy production | 仅 main、依赖 publish：临时放行当前 Runner 的 SSH /32，调用受限 deploy 用户部署同 SHA，校验三个公网 `/healthz`、三域静态响应头和公开 API 的 `nosniff`，并始终撤销规则 |

后端 MySQL job 预拉镜像并设置 15 分钟上限。只有镜像拉取或容器启动这类基础设施失败可以直接 rerun；迁移、SQL 或断言失败必须先诊断代码。

运行中 API 契约由 `RunningApiContractTest` 在随机端口启动后端，按独立测试方法覆盖登录/refresh/DEMO、文章完整写入、multipart 附件和站点配置完整 PUT；它不依赖生产账号或外部服务。新增检查必须能在干净 runner 稳定复现，不依赖生产密钥或个人本机状态，并对失败提供可操作反馈。CI 通过不替代真实环境的 CORS、反向代理、客户端 IP、S3、数据库备份和冒烟验证。

Caddy 明确拥有两类响应头：三个站点的 HTTPS 应用响应统一设置不含 `includeSubDomains` 和 `preload` 的一年期 HSTS；静态站点路由直接设置 `nosniff`、Referrer Policy、`X-Frame-Options: DENY` 和最小 Permissions Policy。`/api` 不再由 Caddy 猜测并补齐页面策略，Spring Security 继续负责 API 的 `nosniff` 等安全默认值。CI 使用 `deploy/web/Dockerfile` 固定的 Caddy 镜像启动静态站点和模拟上游，实际请求三域与代理路径，同时独立验证配置语法；CD 在新镜像启动后读取公网响应头，避免把配置存在或 CI 模拟误判为生产已生效。响应头取值依据见 [Caddy `header`](https://caddyserver.com/docs/caddyfile/directives/header)、[MDN HSTS](https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Strict-Transport-Security) 与 [OWASP HTTP Headers Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Headers_Cheat_Sheet.html)。

CD 不替代生产配置注入、数据库备份、迁移评审、产品冒烟或回滚判断。首次演练已通过；后续仍按 [`github-ssh-cd.md`](github-ssh-cd.md) 核对 AWS/GitHub/服务器边界。Flyway 或数据问题不会自动回滚。

CI、镜像发布与部署使用的官方 Action 已升级到声明 Node 24 运行时的主版本；主线 CI、Docker 镜像构建、AWS OIDC、同 SHA 部署和公网冒烟均已真实通过，不使用 Node 20 回退开关。
