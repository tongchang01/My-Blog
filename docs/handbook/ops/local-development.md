# MyBlog V2 本地开发与三端启动指南

本文档面向本机开发环境。默认使用本地 MySQL 测试库 `myblog_v2_dev`，不要用于生产环境。

## 1. 前置条件

- Java 17
- Maven 3.9+
- Node.js 24 或满足前端 `package.json` 中 `engines` 要求的版本
- Corepack / pnpm
- MySQL 8，`localhost:3306` 上存在 `myblog_v2_dev`

## 2. 后端环境变量

后端本地 profile 为 `local`，配置文件位于：

```text
MyBlog-springboot-v2/src/main/resources/application-local.yml
```

必须提供以下环境变量：

```powershell
$env:MYBLOG_DATASOURCE_USERNAME = "root"
$env:MYBLOG_DATASOURCE_PASSWORD = "<your-local-mysql-password>"
$env:MYBLOG_JWT_SECRET = "<local-dev-jwt-secret-at-least-32-bytes>"
$env:MYBLOG_STATS_HASH_SECRET = "<local-dev-stats-secret-at-least-32-bytes>"
```

可选覆盖数据库 URL：

```powershell
$env:MYBLOG_DATASOURCE_URL = "jdbc:mysql://localhost:3306/myblog_v2_dev?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&connectionTimeZone=Asia/Tokyo&forceConnectionTimeToSession=true&sessionVariables=time_zone='%2B09:00'"
```

如果使用 IntelliJ IDEA，可以在 Spring Boot 启动配置中指定环境变量文件，但仍要确认启动 profile 是 `local`。

## 3. 启动后端

```powershell
cd MyBlog-springboot-v2
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

默认地址：

```text
http://localhost:8080
```

## 4. 启动博客前台

```powershell
cd frontend/apps/blog
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

默认地址：

```text
http://localhost:5173
```

## 5. 启动管理后台

```powershell
cd frontend/apps/admin
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

默认地址：

```text
http://localhost:5174
```

## 6. 常见问题

### 后端提示没有 datasource url

说明 `local` profile 没有生效。检查：

- Maven 命令是否带了 `-Dspring-boot.run.profiles=local`
- IDE 启动配置是否设置了 `SPRING_PROFILES_ACTIVE=local`

### MySQL 报 `Public Key Retrieval is not allowed`

确认 JDBC URL 中包含：

```text
allowPublicKeyRetrieval=true
```

当前 `application-local.yml` 的默认 URL 已包含该参数；如果你通过 `MYBLOG_DATASOURCE_URL` 覆盖了默认值，也要手动保留它。

### Windows 下后台启动提示 `NODE_OPTIONS` 不是命令

后台 `package.json` 已改为跨平台的 Node 启动方式。优先使用：

```powershell
corepack pnpm dev
```

不要手工执行旧模板中的 Unix 风格 `NODE_OPTIONS=... vite`。

## 7. 本地回归

后端：

```powershell
cd MyBlog-springboot-v2
mvn test
```

博客前台：

```powershell
cd frontend/apps/blog
corepack pnpm run build
```

管理后台：

```powershell
cd frontend/apps/admin
corepack pnpm test
corepack pnpm run typecheck
corepack pnpm run build
```
