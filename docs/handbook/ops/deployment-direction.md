# 生产部署方向

> 状态：当前有效；生产已上线，GitHub SSH CD 待首次真实演练
> 适用范围：MyBlog V2 生产部署设计
> 最后校准：2026-07-11
> 对应代码：`MyBlog-springboot-v2/src/main/resources/application-prod.yml`、`frontend/apps/`、`.github/workflows/`
> 权威程度：部署约束

## 信息边界

仓库只记录可公开复用的架构、步骤和验收标准。真实 IP、资源 ID、安全组 ID、Docker 卷 ID、宿主机路径、证书状态、备份位置和环境变量值等敏感生产信息，已保存在仓库外的私有生产台账中，不提交到 Git。

执行部署时必须同时查阅私有生产台账；本文件和 [`production-runbook.md`](production-runbook.md) 不以占位值替代真实环境核对。

## 已确认方向

- 继续使用现有 AWS EC2、Elastic IP、Route 53 和 S3。
- 不重装操作系统；先完成回滚快照，再清理 V1 应用层资源并原地部署 V2。
- V1 业务数据不迁移；V2 使用全新的 MySQL 8 数据卷和空数据库，由 Flyway 初始化。
- AWS 只承担基础设施、S3 最小权限和 GitHub OIDC 的临时 CD SSH 安全组 API；软件交付不依赖 ECR、SSM、App Runner 或 ECS。
- GitHub Actions 负责测试和镜像构建，公共 GHCR 保存标准 Docker/OCI 镜像。
- Docker Compose 是生产运行拓扑的单一声明，保持跨云可迁移。
- GitHub Actions 在 main 镜像发布成功后自动部署同一 SHA；首次启用前必须完成 GitHub OIDC、独立 CD 安全组和受限 deploy 用户演练。

## 目标拓扑

生产 Compose 只包含三个服务：

| 服务 | 职责 | 网络边界 |
| --- | --- | --- |
| `web` | Caddy、blog/admin 静态文件、HTTPS、`/api` 反向代理 | 唯一暴露 80/443 |
| `api` | Java 17、Spring Boot prod profile、业务 API | 仅 Compose 私有网络 |
| `mysql` | MySQL 8、全新 `myblog_v2` 数据卷 | 仅 Compose 私有网络 |

`web` 将完整 `/api/*` 路径代理到 `api:8080`。Compose 显式固定私有子网，`MYBLOG_WEB_TRUSTED_PROXIES` 只信任该代理网段。3306 和 8080 不映射到宿主机公网。

## 镜像与配置

- `myblog-web` 镜像包含 blog/admin 的已验证生产构建和 Caddy 配置。
- `myblog-api` 镜像包含 Java 17 运行时和后端 JAR，以非 root 用户运行。
- 两个镜像使用提交 SHA 标记版本；不以可变的 `latest` 作为唯一发布标识。
- 生产密钥保存在宿主机 root-only 环境文件中，不写入镜像、仓库、日志或运行手册。
- 后端已使用 AWS SDK `DefaultCredentialsProvider`，通过 EC2 IAM Role 获取 S3 临时凭证，无需保存 Access Key。
- EC2 必须启用 IMDSv2，并为容器设置正确的 metadata hop limit；应用禁止回退 IMDSv1。

## 资源与安全基线

- 初始继续使用约 2 GiB 内存，Java 最大堆 512 MiB，并增加 1 GiB swap。
- 80/443 对公网开放；22 仅允许受控管理来源；3306/8080 不开放。
- S3 IAM Role 仅允许 V2 附件前缀所需的读取、写入和删除操作。
- AWS root 只用于必须的账户级操作，启用 MFA，不创建 root Access Key；日常操作使用单独的管理员身份。
- 内存持续不足、OOM、持续 swap 或明显 GC/延迟问题触发升级到 4 GiB。

## 上线阻塞项

清理 V1 前必须完成：

- 服务器只读预检与私有生产台账逐项核对。
- 全新 MySQL 8、Flyway、S3 IAM Role 和 IMDSv2 实机验证。
- EC2 快照、V1 MySQL 保险备份与恢复入口确认。
- 生产环境文件、域名、证书、网络和维护窗口确认。
- Compose 配置校验、镜像匿名拉取、首次部署和生产冒烟。

具体执行顺序见 [`production-runbook.md`](production-runbook.md)，发布验收门槛见 [`release-checklist.md`](release-checklist.md)。
