# 路线图

> 状态：当前有效
> 适用范围：MyBlog V2 后续开发
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：路线图

## 本文档回答什么问题

本文档说明 MyBlog V2 后续按什么顺序推进。已完成的历史阶段只保留摘要；具体当前状态见 `current-status.md`，未完成和争议事项见 `open-issues.md`。

## 当前主线

当前主线是：

1. 文档体系收口，建立可信 handbook。
2. 前台读者主链路补齐。
3. 后台管理端完成度校准和体验收口。
4. 上线前 SEO、部署、备份和 CI/CD 准备。

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
- 后台 admin 已完成基础闭环和多项业务页，包括认证会话、文章、分类标签、友链、附件、站点配置、作者资料和统计仪表盘等实现材料。
- 后台完成度仍需统一文档校准，见 O-005、O-006。

## 近期路线

### R1：完成文档体系收口

目标：让 `docs/handbook/` 成为当前开发可信源。

- [x] 建立文档整理计划。
- [x] 建立文档盘点表。
- [x] 建立 `docs/README.md`、`handbook/README.md`、`open-issues.md`、`glossary.md`、`documentation.md`。
- [ ] 迁移并校准 `project-overview.md`、`current-status.md`、`roadmap.md`。
- [ ] 迁移并校准 architecture、rules、api、product、ops、workflows。
- [ ] 提炼 `frontend/apps/admin/docs/` 到 `handbook/frontend/admin/`。
- [ ] 归档 `superpowers/` 和旧计划。
- [ ] 更新旧入口和内部链接。

### R2：前台读者主链路补齐

目标：让 V2 前台成为可浏览、可导航、可上线的博客。

- [ ] 分类页。
- [ ] 标签页。
- [ ] 归档页。
- [ ] 友链页。
- [ ] 关于页。
- [ ] 搜索。
- [ ] 页面级 loading、empty、error、retry 统一体验。
- [ ] 与 V2 API 契约同步校准。

关联 open issues：O-003。

### R3：前台互动能力接入

目标：补齐读者交互，但不阻塞读者主链路上线。

- [ ] 文章评论列表。
- [ ] 评论提交。
- [ ] 留言板。
- [ ] 页面访问统计打点。
- [ ] PASSWORD 文章完整解锁流程。

关联 open issues：O-001、O-004。

### R4：后台完成度校准和体验收口

目标：把后台当前已完成内容沉淀为可信文档，并补齐关键管理体验。

- [ ] 建立 `handbook/frontend/admin/integration-status.md`。
- [ ] 提炼后台认证会话文档。
- [ ] 提炼文章管理、分类标签、评论、友链、附件、站点配置和统计文档。
- [ ] 校准 ADMIN/DEMO 页面和字段边界。
- [ ] 评估评论回复工作流、友链头像附件选择器、统计图表化等增强项。

关联 open issues：O-002、O-005、O-006。

### R5：上线准备

目标：让 V2 具备稳定上线的基本条件。

- [ ] CI/CD：至少运行后端测试和前端 lint/typecheck/build。
- [ ] 部署文档。
- [ ] 环境变量权威清单。
- [ ] 数据库备份策略。
- [ ] RSS。
- [ ] Sitemap。
- [ ] SEO meta。
- [ ] 生产 CORS、trusted proxies、HTTPS、JWT secret 检查。

关联 open issues：O-007。

## 中长期增量

- PASSWORD 文章访问 token 完整流程。
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
- 自建音乐播放器；音乐展示优先走 Spotify Embed。

## 文档同步规则

每完成一个里程碑：

1. 更新 `current-status.md`。
2. 若仍有待办或争议，更新 `open-issues.md`。
3. 若接口变化，更新 `handbook/api/`。
4. 若规则变化，更新 `handbook/rules/`，必要时写 ADR。
5. 完成的计划文档提炼后归档，不继续留在入口路径。
