# MyBlog V2：可持续维护的三端个人博客

MyBlog V2 是一个由公开博客、管理后台和 Spring Boot API 组成的全栈个人博客系统。项目重点不在堆叠中间件，而在清晰业务边界、可执行架构规则、完整内容工作流和可验证的安全约束。

## 产品能力

公开博客支持中文、日文和英文界面，包含首页编排、文章详情、分类、标签、归档、搜索、关于、友链、留言板、文章评论、作者资料和访问统计。文章 URL 以 ID 作为稳定定位依据，并附带可读 slug。

管理后台覆盖登录会话、统计仪表盘、文章发布、定时发布、首页置顶与精选、分类标签、评论审核、友链、附件、站点配置和作者资料。ADMIN 可以读写，DEMO 只读且敏感字段由后端裁剪。

## 工程设计

后端采用 Java 17、Spring Boot 3.5、Spring Security、MyBatis-Plus、Flyway 和 MySQL 8。identity、content、comment、system、stats 五个业务模块使用四层结构，跨模块只通过 application 能力协作，ArchUnit 自动阻止依赖越界。

认证链路包含短期 JWT access token、数据库 refresh token、并发安全轮换和 token version 撤销。评论 Markdown 在后端转换并清洗，附件支持本地和 S3 存储，访问统计使用不可逆访客哈希与日聚合。

两个前端均使用 Vue 3、TypeScript、Pinia、Vite 和 Vitest。博客端由 Aurora 视觉基础演进而来，管理端基于 pure-admin-thin 框架；两者的数据源和业务交互已经接入 V2 REST API。

## 当前边界

PASSWORD 文章目前只展示锁定元数据，尚无公开解锁流程。完整 SEO/feed、Spotify Embed 和多实例协调均属于按实际需求触发的后续扩展。

生产环境运行在 AWS EC2：Docker Compose 承载 MySQL、API 和 Caddy，S3 承载附件；GitHub Actions 构建 GHCR 镜像，并通过 GitHub OIDC 与受限 SSH 自动部署同一提交 SHA。公开 HTTPS 健康端点已纳入部署后的公网冒烟；数据库恢复、S3 全链路和回滚演练仍需持续验证。

## 体现的能力

- 从产品状态、权限和数据模型出发组织全栈功能。
- 用模块边界、事务、测试和迁移脚本维护长期一致性。
- 同时处理公开体验、后台运营、安全、数据审计和部署准备。
- 明确区分已实现能力、已知风险和触发式扩展，避免把计划写成现状。
