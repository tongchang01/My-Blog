# 本地 MySQL 开发工作流

本文档说明如何用空的 MySQL 8 数据库初始化 V2 schema、导入固定开发种子并启动前后台联调。它只适用于本机 `myblog_v2_dev`，不负责 V1 数据迁移，也不得用于生产数据库。

## 一、前置条件

- MySQL 8.0，`mysql` 命令已加入 `PATH`。
- Java 17、Maven 3.9、Node.js 24 和 Corepack 可用。
- 从仓库根目录执行命令。
- 本机 3306、8080、5173、5174 端口未被无关进程占用。

当前本地开发约定：`localhost:3306` 已存在 `myblog_v2_dev`，可直接作为 V2 联调测试库。本机允许使用 `root` 连接该库，但数据库密码只在运行时输入，不记录在本文档或仓库文件中。

如果后续需要降低本机账号权限，建议创建仅管理本地 V2 数据库的账号。以下密码必须替换为你自己的本机密码，不得提交真实值：

```sql
CREATE USER 'myblog_dev'@'localhost'
IDENTIFIED BY '<LOCAL_ONLY_DATABASE_PASSWORD>';
GRANT ALL PRIVILEGES ON myblog_v2_dev.*
TO 'myblog_dev'@'localhost';
```

## 二、设置当前终端环境变量

PowerShell 示例：

```powershell
$env:MYBLOG_DATASOURCE_URL = "jdbc:mysql://localhost:3306/myblog_v2_dev?useUnicode=true&characterEncoding=utf8&useSSL=false&connectionTimeZone=Asia/Tokyo&forceConnectionTimeToSession=true&sessionVariables=time_zone='%2B09:00'"
$env:MYBLOG_DATASOURCE_USERNAME = "root"
$databasePassword = Read-Host "请输入本机 MySQL root 密码" -AsSecureString
$env:MYBLOG_DATASOURCE_PASSWORD = [Net.NetworkCredential]::new("", $databasePassword).Password
$env:MYBLOG_JWT_SECRET = "<LOCAL_ONLY_JWT_SECRET_AT_LEAST_32_BYTES>"
$env:MYBLOG_STATS_HASH_SECRET = "<LOCAL_ONLY_STATS_HASH_SECRET>"
```

不要把这些值写入 `.env`、YAML、脚本、终端截图、聊天记录或 Git 提交。初始化脚本使用 `MYSQL_PWD` 临时传递密码，并在退出前恢复原值。完成联调后可用 `Remove-Item Env:MYBLOG_DATASOURCE_PASSWORD` 清理当前终端中的数据库密码。

## 三、初始化或重建数据库

首次建立空库，或确认现有 `myblog_v2_dev` 只含可丢弃测试数据时：

```powershell
cd MyBlog-springboot-v2
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\dev\mysql\initialize.ps1 -Reset
```

`-Reset` 会且只会删除并重建 `myblog_v2_dev`。当前本地数据库已明确作为测试库时可以使用该参数；执行前仍须确认其中没有需要保留的数据。脚本拒绝其他数据库名，随后隐藏启动 Spring Boot，让 Flyway 应用到版本 2，再导入 `seed.sql` 并执行验收查询。

不带 `-Reset` 时，脚本不会执行 `DROP DATABASE`。如果目标表已有 active 用户或文章，它会直接拒绝导入，避免覆盖已有开发数据：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\dev\mysql\initialize.ps1
```

只运行 Flyway、不导入种子：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\dev\mysql\initialize.ps1 -Reset -SkipSeed
```

## 四、开发种子账号与数据

固定种子只用于本地开发：

| 用户名 | 角色 | 本地开发密码 |
| --- | --- | --- |
| `admin` | ADMIN | `MyBlogDev!2026` |
| `demo` | DEMO | `MyBlogDev!2026` |

种子包含 2 个分类、3 个标签，以及 DRAFT、PUBLISHED、PRIVATE、PASSWORD、SCHEDULED 各一篇文章。文章和账号 ID 超过 JavaScript 安全整数，用于持续验证前后端字符串 ID 契约。

这些账号和密码禁止部署到公网、测试共享环境或生产环境。

## 五、只执行数据库验收

```powershell
cd MyBlog-springboot-v2
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\dev\mysql\verify.ps1
```

验收检查：Flyway 版本为 2、当前会话时区为 `+09:00`、关键表数量满足种子基线，且已发布文章的冗余评论数与 PASS 评论一致。

## 六、启动后端与前端

后端：

```powershell
cd MyBlog-springboot-v2
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

博客前台：

```powershell
cd frontend/apps/blog
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

管理后台：

```powershell
cd frontend/apps/admin
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

默认访问地址：后端 `http://localhost:8080`、博客前台 `http://localhost:5173`、管理后台 `http://localhost:5174`。实际端口以 Vite 输出为准。

## 七、回归与故障处理

H2 自动化测试继续作为完整回归门禁：

```powershell
cd MyBlog-springboot-v2
mvn clean test
```

常见失败：

- `Missing required environment variable`：只在当前终端补齐变量，不要写入仓库。
- `only 'myblog_v2_dev' is allowed`：检查 JDBC URL，不要绕过数据库名守卫。
- `active user/article rows`：现有库不是空基线；确认数据可丢弃后才使用 `-Reset`。
- 健康检查超时：检查 8080 端口、MySQL 服务和临时日志 `%TEMP%\myblog-v2-local-mysql.*.log`。
- 前端仍显示旧数据：确认 API base URL 指向 8080，并检查浏览器 Network 中是否仍请求静态 JSON。

正式 V1 数据迁移必须使用 `migration/` 下的独立映射与校验流程，不能修改或扩充本开发种子代替迁移。
