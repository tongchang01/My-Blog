# 当前状态

> 状态：当前有效
> 适用范围：MyBlog V2 开发
> 最后校准：2026-07-03
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：当前进度权威源

## 本文档回答什么问题

本文档统一记录 MyBlog V2 当前做到哪里、哪些已经完成、哪些仍在后续批次。历史计划和阶段 review 不再作为当前进度依据；未完成和争议事项统一登记在 `open-issues.md`。

## 总体结论

当前主线已经从“后端重建”进入“前台读者链路补齐 + 后台管理体验收口 + 上线准备”。

| 方向 | 当前状态 | 说明 |
|------|----------|------|
| V1 | 只读历史参考 | 不再作为新功能开发目标 |
| V2 后端 | 第一版主体完成 | 六大模块已完成，后续以联调修正、上线准备和增量能力为主 |
| V2 前台 blog | 首批联调完成，主链路未完整 | 站点配置、首页槽位、文章列表、文章详情已接入；分类、标签、归档、友链、关于、搜索等仍待补齐 |
| V2 后台 admin | 主要业务页已校准 | 登录、会话、文章、分类标签、评论、友链、附件、站点配置、作者资料、统计仪表盘已有实现记录，DEMO 字段边界和统计 ID 契约已收口 |
| 文档体系 | 主要迁移完成，仍在收口 | `project-handbook/` 已降级为跳转入口，`superpowers/` 和后台过程材料已归档；剩余为入口状态、前台 blog、产品/运维细节校准 |

## 已完成：产品、Schema 与后端基础

- V2 不兼容 V1 schema 的方向已确定。
- 产品范围、业务规则、ER、数据模型和 14 张表 schema 已完成。
- Flyway `V1__init.sql` 已作为冻结起点；后续 schema 变更应走 `V2__xxx.sql` / `V3__xxx.sql`。
- M1 旧代码清理完成。
- M2 基础设施完成，包括审计列、软删除、Clock、错误响应、安全配置、MyBatis-Plus、Flyway、ArchUnit、Maven Enforcer 等。
- M3 后端模块重建完成。

## 已完成：V2 后端模块

| 模块 | 完成状态 | 摘要 |
|------|----------|------|
| identity | 已完成第一版 | 登录、BCrypt、限流、双 token、refresh 行锁轮换、logout、当前用户、资料更新、改密和 token_version 失效 |
| content | 已完成第一版 | 分类、标签、文章核心、公开文章查询、详情、回收站、恢复校验、定时发布、PASSWORD 首版锁定态 |
| comment | 已完成第一版 | 公开评论、留言板、后台审核、Markdown 清洗、文章评论计数、回复通知和邮件失败日志 |
| system | 已完成第一版 | 站点配置、附件上传/查询、友链公开和后台管理 |
| stats | 已完成第一版 | 公开打点、HMAC 访客标识、日 PV/UV 聚合、补算、校准、清理和后台 dashboard |
| common-infra | 已完成第一版 | 响应、异常、安全、JWT、邮件端口、存储端口、时间、测试和架构守护 |

## 已完成：V2 前台 blog 首批

代码目录：`frontend/apps/blog/`

已完成：

- 三语入口和语言保存。
- `/:lang` 首页和 `/:lang/posts/:id/:slug?` ID 主导文章路由。
- `GET /api/public/site-config` 接入，失败时使用 typed defaults 降级。
- `GET /api/public/articles/home` 首页聚合接口接入，后台文章编辑可维护 `homepageSlot`。
- 公开文章列表、分页、loading、empty、error、retry。
- 公开文章详情、canonical slug、PASSWORD 锁定态、404、网络错误、retry。
- 分类和标签公开列表、文章数量展示、slug 路由筛选。
- 公开 content/comment/stats 的前端可见 Snowflake ID 已在 HTTP JSON 边界输出为 string；后端内部仍使用数值 ID。
- 正文通过 `markdown-it` 渲染，禁用原始 HTML，并补充外链安全属性。
- 首页已停止请求旧 Hexo/Aurora 的 `site.json`、`posts/1.json`、`features.json`、旧搜索索引和旧友链页面数据。

仍待补齐：

- 归档、友链、关于、搜索。
- 评论、留言、访问统计前台接入。
- PASSWORD 文章完整解锁流程。
- Spotify Embed。
- Markdown chunk 分包和 Sass 旧 API/`@import` 清理。

未完成项以 `open-issues.md` 的 O-001、O-003、O-004 为准。

## 已完成：V2 后台 admin 当前能力

代码目录：`frontend/apps/admin/`

已完成：

- 后台基础工程、三语、静态路由、Pinia、Axios、统一错误模型。
- 登录：`/api/auth/login` -> `/api/auth/me` 原子编排。
- 会话：access token 自动刷新、单飞 refresh、原请求最多重放一次、退出清理。
- ADMIN/DEMO 权限：前端改善体验，后端仍是最终安全边界。
- 仪表盘：复用 `/api/admin/stats/dashboard` 展示真实统计，包含 PV/UV 指标、趋势折线图、TOP 文章柱状图和语言分布饼图。
- 文章管理：列表、新建、编辑、状态、Markdown 预览、本地草稿、回收站、软删除和恢复入口。
- 分类/标签管理：列表、筛选、新增、编辑、删除，分类支持排序，标签不排序。
- 评论管理：列表、目标类型/目标 ID/审核状态/关键词/已删除筛选，ADMIN 可审核通过、隐藏、软删除、恢复和回复，DEMO 只读。
- 友链管理：列表、筛选、新增、编辑、显示/隐藏、排序、删除和头像附件选择器。
- 站点配置与作者资料：站点配置、About Markdown、Logo/Favicon、Spotify playlist ID、当前用户资料维护。
- 附件管理：图片上传、列表、详情、复制 URL、打开公开地址；文章封面和站点图片可复用附件选择器。
- DEMO 敏感字段裁剪：文章非公开正文、评论邮箱/IP/UA、附件内部存储字段已由后端裁剪；写操作继续后端拒绝。

仍需跟踪：

- 前端可继续优化 DEMO 只读体验，但不得作为安全边界。

未完成项以 `open-issues.md` 为准。

## 验证状态

已有历史记录显示：

- 后端多轮 `mvn clean test` 通过，跳过项主要为 Docker 不可用时的 Testcontainers 条件测试。
- CI 已补 `backend-mysql-test`，在 GitHub Actions 上真实运行 `MySql*Test`。
- 前台首批联调完成 Vitest、lint、typecheck、build。
- 后台基础闭环和多项业务页完成前端测试、typecheck、build，并有部分本地 MySQL 浏览器联调记录。

当前文档整理批次未重新运行全量测试；后续涉及代码或接口校准时，应按 `handbook/ops/build-and-test.md` 重新验证。

## 当前优先级

1. 补齐前台读者主链路：分类、标签、归档、友链、关于、搜索。
2. 接入前台评论、留言和访问统计。
3. 上线准备：SEO、RSS、Sitemap、部署、备份和 CI/CD。
4. 继续收口文档入口状态、产品规格和运维细节。

## 相关文档

- 项目概览：`project-overview.md`
- 路线图：`roadmap.md`
- 未完成和争议项：`open-issues.md`
- 术语表：`glossary.md`
- 红线与历史踩坑：`pitfalls.md`
- 文档维护规则：`../rules/documentation.md`
