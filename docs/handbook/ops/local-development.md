# 本地三端启动

> 状态：当前有效
> 适用范围：Windows PowerShell 本地开发
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/resources/application-local.yml`、`frontend/apps/blog/.env`、`frontend/apps/admin/.env.development`
> 权威程度：运行手册

## 前置条件

- JDK 17、Maven 3.9.x。
- Node `^20.19.0` 或满足应用 engines 的 22+ 版本，Corepack 与 pnpm 9。
- MySQL 8，默认数据库 `myblog_v2_dev`。
- JVM 默认时区 `Asia/Tokyo`；其他系统时区需加 `-Duser.timezone=Asia/Tokyo`。

## 后端

```powershell
$env:MYBLOG_DATASOURCE_USERNAME = "root"
$env:MYBLOG_DATASOURCE_PASSWORD = "<local-password>"
$env:MYBLOG_JWT_SECRET = "<at-least-32-byte-local-secret>"
$env:MYBLOG_STATS_HASH_SECRET = "<local-stats-secret>"

cd MyBlog-springboot-v2
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

`application.yml` 当前默认 profile 也是 local，但命令显式声明可避免 IDE 或环境覆盖。local 启动时 Flyway 会迁移到最新版本。后端地址为 `http://localhost:8080`，健康检查为 `/actuator/health`，本地 API 文档为 `/doc.html`。

## 博客端

```powershell
cd frontend/apps/blog
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

默认地址 `http://localhost:5173`，`/api` 代理到 `VITE_API_PROXY_TARGET`，未设置时使用 `http://localhost:8080`。

## 管理端

```powershell
cd frontend/apps/admin
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

默认地址 `http://localhost:8848`。开发环境 `VITE_API_BASE_URL` 留空，`/api` 由 Vite 代理到 `VITE_BACKEND_PROXY_TARGET=http://localhost:8080`。

## 常见失败

- Enforcer 拒绝启动：确认 Java 17 和 Maven 3.9.x。
- JVM 时区错误：为 Maven/JVM 设置 `-Duser.timezone=Asia/Tokyo`。
- 数据库认证或公钥错误：检查用户名、密码和 local JDBC URL；默认 URL 已包含 `allowPublicKeyRetrieval=true` 与 JST session 参数。
- 管理端请求直连错误地址：确认 `.env.development` 的 API base 为空、代理目标指向后端。
- 端口冲突：博客端可由 Vite 自动选择其他端口；管理端端口来自 `VITE_PORT`。

验证命令见 `build-and-test.md`，安全初始化数据库见 `local-mysql-development.md`。
