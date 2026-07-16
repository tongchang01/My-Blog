# 项目概览

> 状态：当前有效
> 适用范围：MyBlog V2 全项目
> 最后校准：2026-07-14
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：项目总览

MyBlog 是包含公开博客、管理后台和 Spring Boot API 的个人博客系统。当前开发主线是 V2；V1 源码已从主线移除，由只读分支 `archive/v1-master-2026-06-26` 保存。

| 目录 | 角色 |
| --- | --- |
| `MyBlog-springboot-v2/` | Java 17、Spring Boot 3.5、MyBatis-Plus、Flyway 后端 |
| `frontend/apps/blog/` | Vue 3 公开读者端 |
| `frontend/apps/admin/` | Vue 3 管理端 |
| `docs/` | 当前文档、治理规则和展示材料 |

## 后端

基础包为 `com.tyb.myblog.v2`。identity、content、comment、system、stats 五个业务模块采用 web/application/domain/infrastructure 四层；common 提供响应、异常、安全、时间、存储、邮件和持久化配置。

数据库为 MySQL 8，当前 Flyway V1–V5 建立 15 张表。运行时统一 `Asia/Tokyo`。认证使用 Spring Security、JWT access token、数据库 refresh token、token version 与 BCrypt。

## 前端

博客端提供三语首页、文章、分类、标签、归档、搜索、关于、友链、留言板、评论、作者资料和访问统计。管理端提供会话、仪表盘、文章、首页槽位、分类标签、评论、友链、附件、站点配置和资料管理。

详细能力与缺口见 `../product/feature-inventory.md`，接口见 `../api/`。

## 开发入口

- 本地启动：`../ops/local-development.md`
- 环境变量：`../ops/environment.md`
- 构建测试：`../ops/build-and-test.md`
- 当前状态：`current-status.md`
- 路线图：`roadmap.md`
- 开放问题：`open-issues.md`
