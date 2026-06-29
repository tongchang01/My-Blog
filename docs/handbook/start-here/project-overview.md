# 项目概览

> 状态：当前有效
> 适用范围：全项目、MyBlog V2 开发
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：项目总览

## 本文档回答什么问题

本文档说明 MyBlog 是什么、V1 与 V2 的边界、当前主要代码目录、V2 技术栈和本地开发入口。

## 项目简介

MyBlog 是一个个人博客系统。当前仓库同时保留 V1 历史代码和 V2 重构代码；后续开发主线只关注 V2。

| 目录 | 角色 | 当前状态 |
|------|------|----------|
| `MyBlog-springboot/` | 后端 V1 | 只读历史参考，不再修改 |
| `MyBlog-vue/` | 前端 V1 | 只读历史参考，不作为新功能开发目标 |
| `MyBlog-springboot-v2/` | 后端 V2 | 当前后端主线，六大业务模块已完成第一版 |
| `frontend/apps/blog/` | 前台 V2 | 当前前台主线，已完成首批公开接口联调 |
| `frontend/apps/admin/` | 后台 V2 | 当前后台主线，基础闭环和多项业务页已完成 |
| `docs/` | 开发文档 | 正在从旧结构整理为 handbook / working / archive 三层 |

## V2 技术栈

### 后端

| 项 | 选型 |
|----|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.x |
| 持久层 | MyBatis-Plus + MyBatis XML + Flyway |
| 数据库 | MySQL 8，统一 Asia/Tokyo 时区 |
| 认证 | Spring Security + JWT access token + DB refresh token |
| 密码 | BCrypt |
| API 文档 | Knife4j 4.x / springdoc-openapi，本地和测试环境启用 |
| 邮件 | Resend HTTP API，默认关闭 |
| 限流 | Caffeine 进程内限流，面向 V2 单实例部署 |
| 测试 | JUnit、Spring Boot Test、ArchUnit、Testcontainers 条件测试 |
| 构建 | Maven |

### 前端

| 应用 | 技术栈 | 说明 |
|------|--------|------|
| 前台 blog | Vue 3、TypeScript、Vite、Pinia、vue-i18n、Vitest | 保留 Aurora 视觉基础，逐步改为 V2 API 数据源 |
| 后台 admin | Vue 3、TypeScript、Vite、Pure Admin Thin、Element Plus、Pinia、vue-i18n、Vitest | 静态路由，ADMIN/DEMO 权限，真实 V2 API 联调 |

## V2 后端模块

基础包：`com.tyb.myblog.v2`

| 模块 | 职责 | 主要表 | 错误码段 |
|------|------|--------|----------|
| `identity` | 用户、登录、JWT、refresh token、当前用户资料 | `t_user_auth`、`t_user_info`、`t_refresh_token` | 10xxx |
| `content` | 文章、分类、标签 | `t_article`、`t_category`、`t_tag`、`t_article_tag` | 20xxx |
| `comment` | 评论、留言板、审核、回复通知 | `t_comment` | 30xxx |
| `system` | 站点配置、附件、友链 | `t_site_config`、`t_attachment`、`t_friend_link` | 40xxx |
| `stats` | 访问打点、日聚合、后台统计 | `t_page_view`、`t_page_view_daily` | 50xxx |
| `common-infra` | 响应、异常、安全、配置、时间、存储、邮件、架构守护 | 无独立业务表 | 90xxx / 99999 |

每个业务模块内部遵循四层结构：

```text
<module>/
├── web/             Controller、请求/响应模型
├── application/     应用服务、命令、查询、事务编排
├── domain/          领域模型和领域规则
└── infrastructure/  持久化、外部服务和框架适配
```

## V2 前端应用

### 前台 blog

代码目录：`frontend/apps/blog/`

当前定位：从旧 Aurora/Hexo 数据源逐步迁移到 MyBlog V2 后端。已接入站点配置、公开文章列表和公开文章详情；分类、标签、归档、友链、关于、搜索、评论、留言、统计和 PASSWORD 解锁仍在后续批次。

### 后台 admin

代码目录：`frontend/apps/admin/`

当前定位：V2 管理后台。已具备登录、会话刷新、静态路由、ADMIN/DEMO 权限、文章管理、分类标签、评论、友链、附件、站点配置、作者资料和统计仪表盘等页面的基础实现；仍需继续校准文档和补齐后续体验边界。

## 本地开发入口

后端：

```powershell
cd MyBlog-springboot-v2
mvn clean test
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

前台：

```powershell
cd frontend/apps/blog
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

后台：

```powershell
cd frontend/apps/admin
corepack pnpm install --frozen-lockfile
corepack pnpm dev
```

完整环境变量、local/prod/test 差异和发布检查后续统一维护在 `docs/handbook/ops/`。

## 文档入口

| 主题 | 位置 |
|------|------|
| 文档总入口 | `docs/README.md` |
| V2 开发手册 | `docs/handbook/README.md` |
| 当前状态 | `docs/handbook/start-here/current-status.md` |
| 未完成和争议项 | `docs/handbook/start-here/open-issues.md` |
| 术语表 | `docs/handbook/start-here/glossary.md` |
| 文档维护规则 | `docs/handbook/rules/documentation.md` |
