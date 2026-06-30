# 前台对接阶段实施批次计划

> 状态：方案已定 / 实施排序。本文是前台 Blog 对接 V2 后端的总批次计划，负责排序、依赖和验收边界；每个专题的细节以对应 `docs/working/plans/` 子计划为准。

## 目标

把已裁决的前台 Blog 与 V2 后端差异，整理成可独立实现、独立验证、独立回滚的阶段批次。

## 排序原则

1. 先处理跨页面基础契约，再做具体页面。
2. 先修会影响所有后续接入的 ID、URL、slug、公开数据字段。
3. 读者主浏览链路优先于互动能力。
4. 后台配置能力要早于依赖该配置的前台展示。
5. 评论、PASSWORD、统计属于互动/增强能力，不阻塞分类、标签、归档、搜索、关于页。
6. 每个批次完成后运行该批次风险范围内测试；阶段结束再运行完整验证。

## 总批次

### Batch 0：契约地基与文档校准

状态：已完成后端与后台管理侧地基，前台页面接入按后续批次继续。

目标：让后续前台接入不再反复处理 ID 类型、slug 生命周期和已裁决文档漂移。

包含事项：

- O-010 公开内容接口 ID 类型统一。
- O-011 公开评论接口 ID 类型统一。
- O-012 统计 TOP 文章 ID 类型统一。
- O-014 slug URL 生命周期规则。

实施要点：

- 后端内部继续使用 `long` / `Long`。
- HTTP JSON 边界把前端可见 Snowflake ID 统一转成 string。
- 文章公开 URL 继续 ID 主导，slug 只增强可读性。
- 分类和标签公开 URL 使用 slug 主导，创建后锁定 slug。
- 后台补 slug 重要性提示。

主要文档：

- `docs/working/plans/2026-06-30-frontend-url-slug-implementation-plan.md`
- `docs/handbook/start-here/open-issues.md` O-010 / O-011 / O-012 / O-014

验证：

- 已通过后端公开 content/comment/stats VO 测试，确认 ID 为 JSON string。
- 已通过分类/标签 slug 锁定规则的后端应用层与集成测试。
- 已通过后台 admin taxonomy 页面测试和 typecheck，确认编辑态 slug 禁用并显示提示。
- 前台 blog 的 slug 路由接入不在 Batch 0 代码范围内，继续跟随后续分类/标签页面批次验证。

建议提交拆分：

- 已提交 1：公开 content ID string。
- 已提交 2：公开 comment ID string。
- 已提交 3：stats dashboard top article ID string。
- 已提交 4：分类/标签 slug 锁定规则。
- 已提交 5：后台分类/标签 slug 锁定提示。

### Batch 1：首页首屏与公开文章主列表

目标：先把首页首屏语义补完整，避免前台继续用“列表前三篇”冒充置顶/推荐。

包含事项：

- O-013 文章置顶和推荐能力。

实施要点：

- 新增首页展示槽位：`NONE / PINNED / FEATURED`。
- `PINNED` 最多 1 篇，`FEATURED` 最多 2 篇。
- 只有 `PUBLISHED` 文章可进入槽位。
- 首页接口明确返回 `pinnedArticle`、`featuredArticles`、`articles`。
- 普通列表排除已进入槽位的文章，避免重复。
- 前台卡片区域可由普通文章自然补位，但置顶/推荐语义只在真实槽位存在时显示。

主要文档：

- `docs/working/plans/2026-06-30-frontend-homepage-slot-implementation-plan.md`
- `docs/handbook/start-here/open-issues.md` O-013

验证：

- 后端槽位数量约束测试。
- 首页接口返回结构测试。
- 前台首页无槽位、1 个置顶、1/2 个推荐、普通文章补位场景测试。

建议提交拆分：

- 提交 1：后端 schema / domain / API 槽位能力。
- 提交 2：后台文章编辑槽位选择和提示。
- 提交 3：前台首页接入新首页接口。

### Batch 2：分类、标签、归档和搜索

目标：补齐读者主浏览链路，先完成无需登录、无需评论、无需 PASSWORD 解锁的页面。

包含事项：

- O-015 公开分类和标签 `articleCount`。
- O-016 公开归档时间线接口。
- O-017 搜索数据源迁移到后端 keyword。

实施要点：

- 分类/标签公开列表返回 `articleCount`。
- `articleCount` 不落库，由公开查询聚合。
- 公开分类/标签只返回 `articleCount > 0` 的项。
- 新增公开归档时间线接口：`GET /api/public/archives?page=1&size=12&lang=zh`。
- 归档分页单位按文章数计算，后端返回年月分组。
- 搜索弹窗保留，数据源改为 `/api/public/articles?keyword=...`。
- 搜索范围限定标题和摘要，不搜索正文，不做服务端高亮。

主要文档：

- `docs/working/plans/2026-06-30-frontend-taxonomy-count-implementation-plan.md`
- `docs/working/plans/2026-06-30-frontend-archive-timeline-implementation-plan.md`
- `docs/working/plans/2026-06-30-frontend-search-keyword-implementation-plan.md`
- `docs/handbook/start-here/open-issues.md` O-015 / O-016 / O-017

验证：

- 分类/标签数量 SQL 聚合测试。
- 归档接口按文章数分页和年月分组测试。
- 搜索 keyword 只匹配标题和摘要的测试。
- 前台分类页、标签页、归档页、搜索弹窗组件测试。

建议提交拆分：

- 提交 1：分类/标签 `articleCount`。
- 提交 2：归档时间线后端接口。
- 提交 3：归档前台页面接入。
- 提交 4：搜索弹窗改接后端 keyword。

### Batch 3：关于页、页脚统计和站点配置扩展

目标：完成站点级内容和页脚展示，避免继续读取旧 page JSON 或第三方统计插件。

包含事项：

- O-018 关于页迁移到 `aboutMd`。
- O-020 访问统计前台打点和页脚统计。

实施要点：

- 关于页读取公开站点配置 `aboutMd`。
- 前台复用 `renderMarkdown(aboutMd)`，保持禁用 raw HTML。
- `aboutMd` 为空时显示空态或骨架。
- 前台所有公开路由触发 V2 page-view 打点。
- 文章页打点传 `articleId + lang`，非文章页固定传 `articleId=0 + lang`。
- `PostStats` 第一版只展示阅读时长和字数。
- 移除或注释第三方浏览量、第三方评论数展示。
- 新增公开站点统计摘要接口：`GET /api/public/stats/site-summary`。
- 页脚展示 `今日访客：todayUv`、`访问量：totalPv`、`建站天数`。
- 站点配置表新增 `started_date DATE NULL`，公开站点配置返回 `startedDate`。

主要文档：

- `docs/working/plans/2026-06-30-frontend-about-page-implementation-plan.md`
- `docs/working/plans/2026-06-30-frontend-page-view-stats-implementation-plan.md`
- `docs/handbook/start-here/open-issues.md` O-018 / O-020

验证：

- 关于页 Markdown 安全渲染测试。
- 公开路由打点测试。
- 站点统计摘要接口测试。
- `FooterContainer` 展示 todayUv、totalPv、startedDate 计算测试。
- `PostStats` 不渲染第三方浏览量/评论数 DOM 的测试。

建议提交拆分：

- 提交 1：关于页接 `aboutMd`。
- 提交 2：公开访问打点前台接入。
- 提交 3：站点统计摘要后端接口。
- 提交 4：站点配置 `startedDate` 后端和后台维护。
- 提交 5：页脚统计展示。

### Batch 4：评论与留言

目标：移除第三方评论体系，接入 V2 自研评论和留言板评论。

包含事项：

- O-019 评论和留言迁移到 V2 自研 API。
- O-011 公开评论 ID string 需已在 Batch 0 完成。

实施要点：

- 不继续依赖 Gitalk / Valine / Twikoo / Waline。
- 第一批接文章评论。
- 第二批接留言板。
- 友链评论不做。
- 最近评论侧栏直接移除，不规划 V2 最近评论接口。
- PASSWORD 文章评论在 O-001 完整解锁前禁用。
- 评论数后期如需要展示，使用文章接口 `commentCount`。

主要文档：

- `docs/handbook/start-here/open-issues.md` O-019
- `docs/handbook/api/comment.md`

验证：

- 文章评论列表、提交、回复关系测试。
- 留言板评论列表、提交测试。
- 前台评论组件加载、提交成功、待审、错误态测试。
- 第三方评论插件 DOM 和初始化逻辑不再进入主路径。

建议提交拆分：

- 提交 1：前台文章评论 V2 组件。
- 提交 2：留言板评论 V2 组件。
- 提交 3：移除第三方评论插件主路径和最新评论侧栏。

### Batch 5：PASSWORD 文章完整解锁

目标：补齐 PASSWORD 文章正文阅读和评论授权闭环。

包含事项：

- O-001 PASSWORD 文章完整解锁流程。

实施要点：

- 公开详情无 token 时返回锁定元数据：`locked=true`、`body=null`。
- 新增 `POST /api/public/articles/{id}/unlock`。
- 解锁成功签发独立 Article Access Token。
- 前台用 `sessionStorage` 按文章 ID 保存 token。
- 文章详情、文章评论列表、文章评论提交通过 `X-Article-Token` 携带。
- token 过期或无效时清理缓存并回到锁定态。

主要文档：

- `docs/working/plans/2026-06-30-password-article-unlock-implementation-plan.md`
- `docs/handbook/start-here/open-issues.md` O-001

验证：

- PASSWORD 无 token 详情锁定态测试。
- 解锁成功、密码错误、限流测试。
- token 不能跨文章使用测试。
- article token 和后台 access token 互不通用测试。
- 前台锁定态、解锁态、过期清理测试。

建议提交拆分：

- 提交 1：后端 Article Access Token 和解锁接口。
- 提交 2：公开详情支持 token 与锁定元数据。
- 提交 3：PASSWORD 评论授权。
- 提交 4：前台解锁 UI 和 sessionStorage。

### Batch 6：后台安全边界和上线准备

目标：把不属于读者主链路、但影响上线和演示安全的事项收口。

包含事项：

- O-002 DEMO 敏感字段裁剪。
- O-007 上线前 SEO、发布和运维准备。
- O-008 后台 token 存储风险接受记录。
- O-009 单体单实例限流部署前提。

实施要点：

- DEMO 只允许看 `PUBLISHED` 正文，其他状态文章详情 `body=null`。
- DEMO 评论审计字段固定裁剪为 `null`。
- DEMO 写接口后端返回 `403 + 10003`。
- 发布清单补齐 SEO、RSS、Sitemap、robots、备份、CI/CD 和上线冒烟。
- O-008/O-009 当前不进入功能实现，只作为风险和部署前提记录。

主要文档：

- `docs/working/plans/2026-06-30-demo-sensitive-field-trimming-plan.md`
- `docs/working/plans/2026-06-30-admin-token-storage-risk-decision.md`
- `docs/handbook/ops/release-checklist.md`
- `docs/handbook/start-here/open-issues.md` O-002 / O-007 / O-008 / O-009

验证：

- DEMO 文章详情裁剪测试。
- DEMO 评论审计字段裁剪测试。
- DEMO 写接口 403 测试。
- 发布清单按实际部署拓扑执行。

建议提交拆分：

- 提交 1：DEMO 后端字段裁剪和测试。
- 提交 2：后台前端 DEMO 裁剪显示兼容。
- 提交 3：SEO/RSS/Sitemap/robots。
- 提交 4：部署、备份、CI/CD 清单落地。

## 阶段验收

每个 Batch 完成时：

- 检查 `git diff --stat` 和 `git status --short`。
- 运行对应后端或前端局部测试。
- 更新相关 API 文档或 open issue 状态。
- 以单一目的提交，提交信息使用中文。

全部 Batch 完成时：

- 后端运行 `mvn clean test`。
- 前台 blog 运行 lint、typecheck、unit test、production build。
- 后台 admin 运行受影响测试。
- 按 `docs/handbook/ops/release-checklist.md` 做上线前检查。

## 不进入本阶段

- 完整 Aurora 友链页复刻：G-007 已下调优先级，后续单独重开讨论。
- 最近评论侧栏：已裁决直接移除。
- HttpOnly Cookie token 存储升级：O-008 已风险接受，暂缓。
- 多实例限流：O-009 已确认当前单体单实例前提，暂缓。
