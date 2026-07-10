# 环境变量与 profile

> 状态：当前有效
> 适用范围：V2 后端 local 与 prod、两套前端构建
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/resources/application.yml`、`MyBlog-springboot-v2/src/main/resources/application-local.yml`、`MyBlog-springboot-v2/src/main/resources/application-prod.yml`、`frontend/apps/blog/.env`、`frontend/apps/blog/.env.production`、`frontend/apps/admin/.env.development`、`frontend/apps/admin/.env.production`
> 权威程度：配置手册

## 后端 profile

| Profile | 数据库 | 存储 | API 文档 | Flyway |
| --- | --- | --- | --- | --- |
| local | 默认 `myblog_v2_dev` MySQL | LOCAL | 开启 | 开启 |
| test | H2，专项使用 MySQL Testcontainers | 测试配置 | 测试按需 | 开启 |
| prod | 环境提供的 MySQL | 默认 S3 | 关闭 | 开启 |

`application.yml` 默认 profile 为 local。生产必须显式激活 prod，且 JVM 默认时区必须为 `Asia/Tokyo`。

## 后端变量

| 变量 | local | prod | 用途 |
| --- | --- | --- | --- |
| `MYBLOG_DATASOURCE_URL` | 可选，已有 JST 默认 URL | 必填 | MySQL JDBC URL |
| `MYBLOG_DATASOURCE_USERNAME` | 必填 | 必填 | 数据库账号 |
| `MYBLOG_DATASOURCE_PASSWORD` | 必填 | 必填 | 数据库密码 |
| `MYBLOG_JWT_SECRET` | 必填，至少 32 字节 | 必填，至少 32 字节 | JWT HS256 密钥 |
| `MYBLOG_STATS_HASH_SECRET` | 必填 | 必填 | 访客哈希 HMAC 密钥 |
| `MYBLOG_STORAGE_LOCAL_ROOT` | 可选 | 不使用 | 本地附件根目录 |
| `MYBLOG_STORAGE_LOCAL_PUBLIC_BASE_URL` | 可选 | 不使用 | 本地附件公开 URL |
| `MYBLOG_STORAGE_TYPE` | local 固定 LOCAL | 可选，默认 S3 | 存储类型 |
| `MYBLOG_STORAGE_S3_REGION` | 不使用 | S3 必填 | S3 region |
| `MYBLOG_STORAGE_S3_BUCKET` | 不使用 | S3 必填 | S3 bucket |
| `MYBLOG_STORAGE_S3_PUBLIC_BASE_URL` | 不使用 | S3 必填 | 公开资源前缀 |
| `MYBLOG_CORS_ALLOWED_ORIGINS` | 配置中含本地端口 | 跨域时必填 | 逗号分隔的明确 origin |
| `MYBLOG_WEB_TRUSTED_PROXIES` | 可选 | 使用代理时必填 | 可信代理 IP/CIDR 列表 |
| `MYBLOG_RESEND_ENABLED` | 可选，默认 false | 可选，默认 false | 评论邮件通知 |
| `MYBLOG_RESEND_API_KEY` | 启用邮件时必填 | 启用邮件时必填 | Resend API key |
| `MYBLOG_RESEND_FROM_EMAIL` | 启用邮件时必填 | 启用邮件时必填 | 发件地址 |

Spring Boot relaxed binding 还允许通过大写下划线变量覆盖 `myblog.ratelimit.*`、`myblog.stats.*`、`myblog.web.*` 和 `myblog.comment.audit.*`。调整阈值前需同时更新测试与安全说明。

## 前端变量

博客端：`VITE_API_BASE_URL` 控制 Axios base，`VITE_API_PROXY_TARGET` 控制开发代理，`VITE_APP_PUBLIC_PATH` 控制部署 base。

管理端：`VITE_PORT` 控制开发端口；`VITE_API_BASE_URL` 控制 Axios base；`VITE_BACKEND_PROXY_TARGET` 控制开发代理；`VITE_PUBLIC_PATH` 控制部署 base。

真实密钥、密码和云凭据不得写入 `.env`、YAML、脚本或文档示例。
