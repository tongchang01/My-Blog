# 未完成和争议项

> 状态：当前有效
> 适用范围：MyBlog V2 后续开发
> 最后校准：2026-06-29
> 权威程度：未完成事项权威登记表

## 本文档回答什么问题

本文档统一登记 V2 当前尚未完成、存在争议或需要后续裁决的事项。其他文档不应重复展开这些问题，只引用编号。

## 状态说明

| 状态 | 含义 |
|------|------|
| 未完成 | 已纳入范围，但尚未实现或尚未完整联调 |
| 有争议 | 方案未定，需要后续裁决 |
| 暂缓 | 暂不做，但保留后续可能 |
| 待校准 | 需要对照代码或现有文档确认 |
| 已关闭 | 已完成或已决定不做，应说明关闭原因 |

## O-001 PASSWORD 文章完整解锁流程

- 状态：未完成 / 有争议
- 优先级：P1
- 影响范围：后端 content、前台 blog、评论模块
- 当前判断：首版已有 PASSWORD 锁定态，但完整 unlock API、Article Access Token、前台解锁态和评论授权链路仍需统一设计与联调。
- 争议点：文章访问 token 的前端保存方式、评论读取和提交是否复用文章 token、过期后用户体验。
- 下一步：校准现有后端能力，补 API 契约，再按前台页面垂直切片实现。
- 来源：`roadmap.md`、`security-baseline.md`、`auth-flow.md`

## O-002 DEMO 敏感字段裁剪边界

- 状态：待校准 / 有争议
- 优先级：P1
- 影响范围：后台 admin、后端 application 层、API 契约
- 当前判断：DEMO 后台只读规则已经存在，但每个后台 GET 接口哪些字段需要裁剪仍需逐页确认。
- 争议点：未公开文章正文、评论审计字段、附件元数据、统计细节是否允许 DEMO 查看。
- 下一步：按后台页面列字段清单，明确 ADMIN 与 DEMO 响应差异，并回写 API 文档。
- 来源：`security-baseline.md`、后台业务页设计文档

## O-003 前台读者主链路补齐

- 状态：未完成
- 优先级：P0
- 影响范围：前台 blog、公开 API
- 当前判断：首页、公开文章列表、文章详情和站点配置已接入；分类、标签、归档、友链、关于、搜索仍待补齐。
- 下一步：按页面垂直切片推进，每个页面同步校准 API、store、路由、加载态、空态和错误态。
- 来源：`frontend-user/README.md`、`roadmap.md`

## O-004 前台评论、留言和统计接入

- 状态：未完成
- 优先级：P1
- 影响范围：前台 blog、comment、stats
- 当前判断：后端评论、留言板和统计基础能力已经完成，但前台接入仍未形成完整读者交互闭环。
- 下一步：在前台读者主链路稳定后，接入评论列表、评论提交、留言板和访问统计打点。
- 来源：`roadmap.md`、`frontend-user/README.md`

## O-005 后台内容生产闭环

- 状态：待校准
- 优先级：P0
- 影响范围：后台 admin、content、system attachment
- 当前判断：后台基础能力和部分文章管理能力已经完成，但文章详情/编辑、Markdown 编辑器、附件选择和上传的最终完成度需要对照代码确认。
- 下一步：盘点 `frontend/apps/admin/docs/` 与 `frontend/apps/admin/src/`，将已完成内容沉淀到 `handbook/frontend/admin/`，未完成内容继续登记。
- 来源：后台 article/editor/attachment 设计与计划文档

## O-006 后台其他业务页完成度

- 状态：待校准
- 优先级：P1
- 影响范围：后台 admin、category/tag、comment、friend-link、site-config、stats
- 当前判断：后台目录下已有大量设计、计划和验收文档，但哪些页面已完成、哪些只是计划，需要统一校准。
- 下一步：建立 `handbook/frontend/admin/integration-status.md`，按业务页列出已完成、未完成、待验证。
- 来源：`frontend/apps/admin/docs/`

## O-007 上线前 SEO、发布和运维准备

- 状态：未完成
- 优先级：P1
- 影响范围：前台 blog、ops、CI/CD
- 当前判断：RSS、Sitemap、SEO meta、部署文档、备份和 CI/CD 仍属于上线前待办。
- 下一步：建立 `handbook/ops/release-checklist.md` 权威清单，再按清单补实现。
- 来源：`roadmap.md`

## O-008 后台 token 存储方式升级

- 状态：暂缓 / 有争议
- 优先级：P2
- 影响范围：后台 admin、后端 auth、安全策略
- 当前判断：后台当前使用 localStorage 保存 access token 和 refresh token。个人项目可接受，但 XSS 风险高于 HttpOnly Cookie 方案。
- 争议点：是否为了更高安全性改成 refresh token HttpOnly Cookie、access token 内存保存，以及对前后端部署和 CSRF 策略的影响。
- 下一步：上线前至少明确风险；若升级，单独写设计并改 API/前端会话实现。
- 来源：后台 auth 实现、安全评估

## O-009 多实例部署下限流方案

- 状态：暂缓
- 优先级：P3
- 影响范围：auth、comment、stats、ops
- 当前判断：当前登录、评论和统计限流使用单实例 Caffeine，适合 V2 单机部署。
- 下一步：若未来多实例部署，再评估 Redis、数据库或网关层限流。
- 来源：`security-baseline.md`

## O-010 公开内容接口 ID 类型不一致

- 状态：待校准
- 优先级：P1
- 影响范围：后端 content、前台 blog、API 契约
- 当前判断：规则要求前端可见 Snowflake ID 使用 JSON string；后台 content 响应已使用 string，但当前 `PublicArticlePageItemVO`、`PublicArticleDetailVO`、`PublicArticleTagResult`、`PublicCategoryVO` 和 `PublicTagVO` 仍使用 `long` / `Long`，公开接口会返回 JSON number。
- 风险：前台 article contract 已按 string 处理文章、分类和标签 ID，后续接入公开分类/标签页或继续扩展文章筛选时可能出现类型不一致。
- 下一步：决定是修改后端公开 content VO 为 string，还是明确公开 content 接口继续使用 number；若修改代码，同步补 Controller/OpenAPI/前端测试。
- 来源：`handbook/api/article.md`、`handbook/api/category-tag.md`、当前 content web 代码

## O-011 公开评论接口 ID 类型不一致

- 状态：待校准
- 优先级：P1
- 影响范围：后端 comment、前台 blog、API 契约
- 当前判断：后台评论响应使用 string ID，但公开评论列表和提交响应中的 `id`、`parentId`、`replyToCommentId` 仍是 `long` / `Long`，公开接口会返回 JSON number。
- 风险：前台评论尚未接入，若后续统一前端可见 Snowflake ID 为 string，需要在接入前修正公开评论契约或前端类型。
- 下一步：决定是否把公开评论 VO 改为 string ID；若修改代码，同步补公开评论 Controller 测试和前端 contract。
- 来源：`handbook/api/comment.md`、当前 comment web 代码

## O-012 统计 TOP 文章 ID 类型不一致

- 状态：待校准
- 优先级：P2
- 影响范围：后端 stats、后台 admin、API 契约
- 当前判断：后台多数 Snowflake ID 响应已经使用 string，但 `StatsDashboardVO.TopArticle.articleId` 当前仍是 `long`，后台统计接口会返回 JSON number。
- 风险：后台统计图表或跳转文章详情时可能需要把 `articleId` 与其它后台文章 ID 类型对齐。
- 下一步：决定 `topArticles[].articleId` 是否改为 string；若修改代码，同步补 dashboard API 测试和前端类型。
- 来源：`handbook/api/stats.md`、当前 stats web 代码
