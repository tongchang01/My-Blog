# MyBlog V2 生产部署产物实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付可在当前 AWS EC2 或其他云主机运行的 API、web、MySQL Docker Compose 生产拓扑，并由 GitHub Actions 发布 GHCR 镜像。

**Architecture:** 使用独立管理域名；Caddy 统一承载 blog/admin 静态文件、HTTPS 和 `/api` 反向代理。API 使用 Java 17 非 root 镜像，MySQL 使用官方 8.4 镜像，三个服务仅加入固定私有网络。

**Tech Stack:** Docker BuildKit、Docker Compose、Caddy、Java 17、Spring Boot、Node 22、pnpm 9.15.9、GitHub Actions、GHCR。

## Global Constraints

- 不接入 ECR、ECS、SSM、App Runner 或其他 AWS 专属交付服务。
- 不在仓库提交真实域名、数据库密码、JWT、S3 凭据或管理员生产密码。
- API 使用 `SPRING_PROFILES_ACTIVE=prod`，bootstrap 默认关闭。
- 生产常驻服务不映射 MySQL 3306 或 API 8080 到宿主机。
- 每个任务完成后运行局部验证并创建单一目的的中文提交。

---

### Task 1：API 镜像

**Files:**

- Create: `MyBlog-springboot-v2/Dockerfile`
- Create: `MyBlog-springboot-v2/.dockerignore`
- Test: `MyBlog-springboot-v2/target/myblog-springboot-v2-0.1.0-SNAPSHOT.jar`

- [ ] 使用 Maven 3.9 + Temurin 17 构建 JAR，运行阶段使用 Temurin 17 JRE。
- [ ] 运行用户 UID 10001，暴露 8080，健康检查访问 `/actuator/health`。
- [ ] 运行 `mvn -DskipTests package` 和 Docker build；确认镜像默认命令为 JAR 启动而非开发 profile。
- [ ] 提交 `功能：增加后端生产镜像`。

### Task 2：统一 web 镜像与 Caddy

**Files:**

- Create: `deploy/web/Dockerfile`
- Create: `deploy/web/Caddyfile`
- Delete: `frontend/apps/admin/Dockerfile`
- Create: `.dockerignore`

- [ ] 分别用 Node 22 + pnpm 9.15.9 构建 `frontend/apps/blog` 和 `frontend/apps/admin`。
- [ ] 将两个 dist 放入 Caddy 运行镜像的独立目录；blog 使用主域名和 www，admin 使用独立管理域名。
- [ ] Caddy 为两个站点提供 history fallback、`/api/*` 到 `api:8080` 的反代和 gzip；证书数据写入 `/data` 与 `/config`。
- [ ] 提交 `功能：增加 blog 与 admin 生产 web 镜像`。

### Task 3：Compose 拓扑

**Files:**

- Create: `compose.yaml`
- Create: `.env.compose.example`

- [ ] 定义 `mysql`、`api`、`web` 三个服务，固定 `172.28.0.0/16` 私网和 MySQL/Caddy 持久卷。
- [ ] API 显式传入 prod、数据库、JWT、统计密钥、S3、CORS、trusted proxy 与邮件变量；bootstrap 仍默认关闭。
- [ ] 为 MySQL、API、web 添加健康检查、资源限制和 `restart: unless-stopped`。
- [ ] 运行 `docker compose --env-file .env.compose.example config --quiet`，确认没有宿主机公网端口暴露给 MySQL/API。
- [ ] 提交 `部署：增加生产 Compose 拓扑`。

### Task 4：GHCR 镜像发布工作流

**Files:**

- Create: `.github/workflows/images.yml`
- Modify: `docs/handbook/ops/production-runbook.md`
- Modify: `docs/handbook/ops/environment.md`

- [ ] 只在 `main` 和版本 tag 推送镜像，PR 不推送；两个镜像都使用提交 SHA 标签。
- [ ] 使用 `GITHUB_TOKEN` 的 packages write 权限，镜像名为 `ghcr.io/<owner>/myblog-api` 与 `ghcr.io/<owner>/myblog-web`。
- [ ] 文档写明服务器使用匿名 GHCR 拉取、`IMAGE_TAG` 固定为完整 SHA、Compose 校验和上线顺序。
- [ ] 使用 YAML 解析和静态检查验证 workflow；提交 `CI：增加 GHCR 镜像发布工作流`。

### Task 5：阶段验证

- [ ] 运行 `git diff --check`。
- [ ] 运行 API 测试、admin/blog 测试、typecheck 和 build。
- [ ] 在有 Docker 的环境运行两个镜像 build 与 `docker compose config --quiet`；当前主机无 Docker 时记录为环境限制，不修改代码绕过。
- [ ] 检查 `git status --short`，推送 `feature/production-deployment`。
