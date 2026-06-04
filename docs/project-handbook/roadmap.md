# 路线图

> 本文档回答："接下来要干什么？短期 / 中期 / 长期分别做什么？"
> 性质：滚动更新，每完成一个里程碑就调整。
> 当前日期：2026-06

## ⚠️ 主线：完成产品规格 + Schema 设计 → DDL 冻结 → 重建 V2 代码

R5-R7 多处与既有 V2 实现冲突（详见 `status.md` § 2）。继续修旧实现 = 返工。**DDL 冻结前停止写实体 / Mapper / Controller / Flyway DDL**。

## 1. 短期（已完成，DDL 已冻结）

### S1：新功能候选范围敲定

- [x] 过 `product/feature-inventory.md` 末尾 🟢🟡🤔 三组候选，决定 V2 范围（已由 R1-R8 + 14 表 schema 收敛）
- [x] 新增可能引入表的候选写进 schema 范围或明确后置：点赞 / Newsletter 不进 V2 起点 schema

### S2：领域建模

- [x] `product/er-diagram.md`：ER / 领域关系图（Mermaid）
- [x] `product/use-cases.md`：用户故事按角色 ADMIN / DEMO / GUEST / 系统任务分组
- [x] `product/business-rules.md`：状态机 / 校验规则 / 业务不变量
- [x] `product/data-model.md`：聚合根 / 实体 / 值对象 / 边界

### S3：Schema 设计

- [x] `arch/schema-design.md`：14 张表 DDL（按 ADR-0014 / 0015 / 0017 / 0018 + R1-R8）
- [x] Flyway `V1__init.sql`：从 schema-design.md 转换为可执行 SQL，并通过迁移烟测
- [x] 种子数据策略：`t_site_config` 默认行入 Flyway；管理员 / DEMO 账号由首次部署运维 SQL 插入
- [x] **DDL 冻结里程碑**

## 2. 中期（DDL 冻结后，进入代码阶段）

### M1：V2 代码清理

- [ ] 删除 `content/` `comment/` `identity/` 三个业务模块的 `domain/` `application/` `web/`
- [ ] 删除 CategoryEntity / TagEntity（按旧规则写，不可复用）
- [x] 删除 V1 marker Flyway 脚本，替换为 V1__init.sql
- [ ] 保留 `common/`、`infrastructure/security/`、ArchUnit、Spring Security 链路

### M2：基础设施补齐

- [ ] BaseEntity（8 列审计） / AuditOnlyBase（7 列例外）
- [ ] AuditFieldHandler（MyBatis-Plus MetaObjectHandler）
- [ ] Clock Bean（Asia/Tokyo）+ 启动校验
- [ ] i18n 配置（LocaleResolver / MessageSource）
- [ ] 更新 `application.yml`（serverTimezone=Asia/Tokyo / Jackson 时区 / Knife4j 4.x 配置）
- [ ] ArchUnit 规则按新 6 模块（identity / content / comment / system / stats / common-infra）调整

### M3：模块重建（按新 schema）

- [ ] identity：t_user_auth / t_user_info / t_refresh_token + 双 token 流程
- [ ] content：t_article / t_article_tag / t_category / t_tag + slug + 5 态状态机
- [ ] comment：t_comment（含 content_md / content_html 双存）+ Resend 邮件
- [ ] system：t_site_config / t_attachment / t_friend_link
- [ ] stats：t_page_view / t_page_view_daily（AuditOnlyBase 例外）
- [ ] common-infra：跨模块公共能力

### M4：前端骨架

- [ ] frontend-user：Vue 3 + Element Plus + Pinia + TS + vue-i18n + 三语路由
- [ ] frontend-admin：同栈 + Vditor 编辑器 + 数字卡仪表盘
- [ ] Spotify Embed 接入（读 `t_site_config.spotify_playlist_id`）

## 3. 长期（上线及之后）

### L1：上线准备

- [ ] CI/CD（GitHub Actions 跑 mvn test + ArchUnit）
- [ ] 部署文档（`ops/deployment.md`）
- [ ] 自动备份（DB dump + 文章 .md 双备份）
- [ ] Sitemap.xml + RSS 自动生成
- [ ] Resend / Spotify playlist 等环境变量配置

### L2：上线后增量

- [ ] 富文本编辑器图床抽象（本地 / 七牛 / OSS）
- [ ] 评论 @ 回复邮件通知
- [ ] 文章访问 TOP 10 / 活动热图
- [ ] PASSWORD 文章访问 token 完整流程

### L3：可选第三方接入（任何时刻可加，零架构债）

- [ ] Umami / Plausible 自托管（嵌前端 `<script>`，不进 V2 schema）
- [ ] IP 归属地（需第三方库 + 隐私权衡）

## 4. 永远不做（除非有强需求 + ADR 论证）

- 拆微服务
- 引入复杂消息中间件
- 大型前后端框架替换（Vue 3 + SpringBoot 3 稳定栈不动）
- 自建 Newsletter 系统（如需走 Buttondown / Substack 嵌入）
- 自建相册系统（V1 已删）
- 自建音乐播放器（V1 已删，改 Spotify Embed）

## 5. 与文档的同步

每完成一个里程碑：

- 更新 `status.md`
- 涉及决策的写 ADR
- 涉及规则变更的改 `rules/`
- 涉及架构变更的改 `arch/`

## 6. 相关文档

- 当前进度：`status.md`
- 已知问题：`pitfalls.md`
- V1 vs V2：`v1-vs-v2.md`
- 文档索引：`INDEX.md`
