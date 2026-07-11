# MyBlog V2 生产部署产物设计

## 目标

把当前已经通过 CI 的 V2 代码包装为可跨云运行的 Docker Compose 产物：`api` 运行 Java 17 Spring Boot，`web` 运行 Caddy 并提供 blog 与独立管理域名，`mysql` 提供只在 Compose 私网可达的 MySQL 8.4。AWS 只提供 EC2、S3、Route 53 和 IAM Role；镜像交付使用 GitHub Actions 与 GHCR。

## 关键决策

- 管理端使用独立域名，不挂载到 blog 的 `/admin` 路径；保留现有 admin hash 路由和 `/` 构建基座。
- 根目录 `compose.yaml` 是生产拓扑唯一声明；宿主机只暴露 `web` 的 80/443。
- API 与 web 使用两个独立镜像，并用同一 Git 提交 SHA 作为镜像标签；不使用 `latest` 作为唯一发布标识。
- API 镜像使用 Maven 构建阶段和 Java 17 JRE 运行阶段，以非 root 用户运行；健康检查访问 `/actuator/health`。
- web 镜像使用 Node 22 + pnpm 9.15.9 构建 blog/admin，运行阶段使用 Caddy；Caddy 自动申请和续期证书，证书数据写入持久卷。
- Compose 通过显式 `172.28.0.0/16` 私网连接三个服务，API 的 trusted proxy 只信任该网段。
- 生产密码、JWT、数据库和 S3 配置只由 `/etc/myblog-v2/runtime.env` 或一次性 `bootstrap.env` 注入，不进入仓库、镜像或日志。

## 数据流

```text
blog.example / admin.example
        │ HTTPS :443
        ▼
      web (Caddy)
        │ /api/* → api:8080
        ├───────────────┐
        ▼               ▼
  static blog/admin   api (Java 17)
                            │ 私网
                            ├── mysql:3306
                            └── S3 via EC2 IAM Role
```

## 验证边界

- Dockerfile 必须能在无本地源码依赖的干净上下文构建。
- `docker compose config --quiet` 必须在占位运行环境文件下通过。
- CI 只在 `main` 或版本 tag 构建并推送 GHCR；Pull Request 只运行已有 CI，不推送镜像。
- 本阶段不连接真实 EC2、不删除 V1 容器或卷、不把真实域名和凭据写入仓库。
