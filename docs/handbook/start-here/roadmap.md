# 路线图

> 状态：当前有效
> 适用范围：MyBlog V2 后续开发
> 最后校准：2026-07-07
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：路线图

## 本文档回答什么问题

本文档说明 MyBlog V2 后续按什么顺序推进。已完成的历史阶段只保留摘要；具体当前状态见 `current-status.md`，未完成和争议事项见 `open-issues.md`。

## 当前主线

当前主线是：

1. 第一版发布范围收口。
2. 前台友链简版补齐。
3. 上线部署硬项准备。
4. 手动部署跑通后再设计 CD。

后端六大模块已完成第一版，后续后端工作以联调修正、缺口补齐和上线准备为主。

## 已完成阶段摘要

### S：产品与 Schema 冻结

- V2 范围和不兼容 V1 schema 的方向已确定。
- 业务规则、用例、ER、数据模型已完成。
- 14 张表 schema 已冻结。
- Flyway `V1__init.sql` 已作为初始迁移脚本；后续 schema 变更使用 V2+ 迁移。

### M1：V2 旧代码清理

- 删除旧业务模块实现和旧 Mapper/XML/测试。
- 移除 `hutool-all`。
- 保留 common、安全、ArchUnit、Flyway 基线。
- 建立 M2 前可编译基线。

### M2：基础设施补齐

- 审计列、软删除、Clock、时区、错误响应、安全配置、JWT 密钥校验等已完成。
- MyBatis-Plus、Flyway、Knife4j、ArchUnit、Maven Enforcer 等基础能力已收口。
- CORS、可信代理、Security 探针隔离等安全基线已落地。

### M3：后端模块重建

- identity、content、comment、system、stats、common-infra 六大模块已完成第一版。
- 登录、refresh、logout、改密、文章、分类标签、评论、留言、附件、友链、站点配置和统计均已有后端实现。
- PASSWORD 文章完整解锁仍未完成，见 O-001。

### M4：前后台骨架和首批业务页

- 前台 blog 已完成首批公开接口联调：站点配置、文章列表、文章详情。
- 后台 admin 已完成基础闭环和主要业务页，包括认证会话、文章、分类标签、评论、友链、附件、站点配置、作者资料和统计仪表盘。
- 后台完成度已沉淀到 `../frontend/admin/integration-status.md`；DEMO 字段边界和统计 ID 契约已分别在 O-002、O-012 关闭。

## 近期路线

### R1：完成文档体系收口

目标：让 `docs/handbook/` 成为当前开发可信源。

- [x] 建立文档整理计划。
- [x] 建立文档盘点表。
- [x] 建立 `docs/README.md`、`handbook/README.md`、`open-issues.md`、`glossary.md`、`documentation.md`。
- [x] 迁移并校准 `project-overview.md`、`current-status.md`、`roadmap.md`。
- [x] 迁移并校准 architecture、rules、api、product、ops、workflows 的当前入口和核心规则。
- [x] 提炼 `frontend/apps/admin/docs/` 到 `handbook/frontend/admin/`。
- [x] 归档 `superpowers/` 和旧计划。
- [x] 更新旧入口和内部链接。

### R2：前台读者主链路收口

目标：让 V2 前台成为可浏览、可导航、可上线的博客。

- [x] 分类页。
- [x] 标签页。
- [x] 归档页。
- [x] 关于页。
- [x] 搜索。
- [x] 访问统计前台打点和页脚展示。
- [x] 文章详情评论。
- [ ] 友链简版：接入 `GET /api/public/friend-links`，只展示公开友链列表卡片。
- [ ] 与 V2 API 契约同步校准。

关联 open issues：O-003。

### R3：前台互动能力接入

目标：补齐读者交互，但不阻塞第一版阅读主流程上线。

- [x] 文章评论列表。
- [x] 评论提交。
- [x] 页面访问统计打点。
- [ ] 留言板。
- [ ] PASSWORD 文章完整解锁流程。

关联 open issues：O-001、O-004。

### R4：后台体验边界裁决

目标：在后台主要业务页已完成的基础上，裁决剩余权限和契约边界。

- [x] 校准 ADMIN/DEMO 页面和字段边界。
- [x] 裁决 `topArticles[].articleId` 是否改为 JSON string。
- [x] 若需要代码修改，同步补后端 Controller/OpenAPI 测试和前端 contract。

关联 open issues：O-002、O-012，均已关闭。

### R5：第一版上线准备

目标：让 V2 具备稳定上线的基本条件。

- [x] CI：至少运行后端测试和前端 lint/typecheck/build。
- [ ] 部署文档。
- [ ] 环境变量权威清单。
- [ ] 数据库备份策略。
- [ ] 生产 CORS、trusted proxies、HTTPS、JWT secret 检查。
- [ ] S3 附件存储生产校准。
- [ ] 上线冒烟。
- [ ] CD：手动部署跑通后再设计。

关联 open issues：O-007。

## 中长期增量

- PASSWORD 文章访问 token 完整流程。
- 留言板评论迁移到 V2 自研 API。
- 完整 SEO、canonical、robots、sitemap、RSS / Atom、Open Graph 和结构化数据。
- 文章访问 TOP 10、活动热图等统计增强。
- 图床或对象存储体验增强。
- Umami / Plausible 等第三方统计脚本接入。
- IP 归属地，需单独评估隐私和依赖。
- 多实例限流方案，当前单实例阶段暂缓。

## 不做或暂不做

除非有明确需求并补 ADR，否则不做：

- 微服务拆分。
- 复杂消息中间件。
- 大型前后端框架替换。
- 自建 Newsletter 系统。
- 自建相册系统。
- 自建音乐播放器；音乐展示如有需要优先走 Spotify Embed。

## 文档同步规则

每完成一个里程碑：

1. 更新 `current-status.md`。
2. 若仍有待办或争议，更新 `open-issues.md`。
3. 若接口变化，更新 `handbook/api/`。
4. 若规则变化，更新 `handbook/rules/`，必要时写 ADR。
5. 完成的计划文档提炼后归档，不继续留在入口路径。
