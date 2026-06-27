# MyBlog V2

MyBlog V2 是当前仓库的主线工程，包含三个日常开发与启动入口：

| 工程 | 路径 | 说明 |
| --- | --- | --- |
| 后端 | `MyBlog-springboot-v2` | Spring Boot 3 / Java 17 后端服务 |
| 博客前台 | `frontend/apps/blog` | 面向访客的博客前台 |
| 管理后台 | `frontend/apps/admin` | 面向站长的后台管理端 |

旧版 V1 代码仍保留在仓库历史和旧目录中，用于回溯、对照和数据迁移参考；新开发默认不再从 V1 目录启动。

## 本地启动

推荐先阅读：

- [本地开发与三端启动指南](./docs/local-development.md)
- [仓库分支治理策略](./docs/repository-governance/branch-policy.md)
- [仓库整理计划](./docs/repository-governance/2026-06-26-repository-reorganization-plan.md)

最小启动顺序：

```powershell
# 1. 后端
cd MyBlog-springboot-v2
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 2. 博客前台
cd frontend/apps/blog
corepack pnpm install --frozen-lockfile
corepack pnpm dev

# 3. 管理后台
cd frontend/apps/admin
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

默认地址：

- 后端：`http://localhost:8080`
- 博客前台：`http://localhost:5173`
- 管理后台：`http://localhost:5174`

## 本地环境变量

后端本地运行需要提供数据库账号和密钥。示例见：

- [`MyBlog-springboot-v2/.env.example`](./MyBlog-springboot-v2/.env.example)

真实密码和密钥只放在本机环境变量或 IDE 运行配置中，不提交到仓库。

## 验证命令

```powershell
cd MyBlog-springboot-v2
mvn test

cd frontend/apps/blog
corepack pnpm install --frozen-lockfile
corepack pnpm run build

cd frontend/apps/admin
corepack pnpm install --frozen-lockfile
corepack pnpm test
corepack pnpm run typecheck
corepack pnpm run build
```

## 分支约定

整理后的长期目标：

- `main`：V2 默认主线。
- `archive/v1-master-2026-06-26`：V1 master 归档。
- `feature/*`、`fix/*`、`refactor/*`、`docs/*`：后续功能、修复、重构和文档分支。

不再把 `*-clean`、`*-ready`、`*-integration-ready` 作为长期主线名称。
