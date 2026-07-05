# 未完成和争议项

> 状态：当前有效
> 适用范围：MyBlog V2 后续开发
> 最后校准：2026-07-05
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

- 状态：未完成 / 方案已定 / 实现待设计
- 优先级：P1
- 影响范围：后端 content、前台 blog、评论模块
- 当前判断：首版已有 PASSWORD 锁定态基础，公开文章详情 VO 已有 `locked` 字段，但当前服务层遇到 PASSWORD 仍直接返回 `403`，完整 unlock API、Article Access Token、前台解锁态和评论授权链路尚未落地。现有 `auth-flow.md` 已给出 Article Access Token 方向，可作为正式方案基础。
- 风险：如果没有统一解锁链路，PASSWORD 文章只能作为列表中的锁定占位，无法形成读者端正文阅读、评论读取和评论提交闭环；如果复用后台 access token 或长期保存文章访问凭证，会混淆身份认证和内容访问授权。
- 下一步：按 `docs/working/plans/2026-06-30-password-article-unlock-implementation-plan.md` 拆分实现。公开详情无 token 时返回锁定元数据：`locked=true`、`body=null`，不直接暴露正文；解锁接口 `POST /api/public/articles/{id}/unlock` 校验文章密码并签发短期 Article Access Token；后续文章详情、文章评论列表和文章评论提交通过 `X-Article-Token` 携带该 token；前台使用 `sessionStorage` 按文章 ID 保存 token，到期或校验失败后清理并回到锁定态。
- 来源：`roadmap.md`、`security-baseline.md`、`auth-flow.md`、当前 `PublicArticleDetailVO.locked` / `PublicArticleQueryService`

## O-002 DEMO 敏感字段裁剪边界

- 状态：已关闭
- 优先级：P1
- 影响范围：后台 admin、后端 application 层、API 契约
- 关闭原因：DEMO 是后台演示账号，允许查看后台整体效果和只读数据，但不能看到不该公开的敏感内容，也不能执行后台写操作。后端 application / VO mapping 层已落实裁剪边界：DEMO 文章详情只可读取 `PUBLISHED` 正文，`DRAFT / PRIVATE / PASSWORD / SCHEDULED` 的 `body` 返回 `null`；后台评论列表对 DEMO 固定裁剪 `authorEmail`、`authorIp`、`authorUserAgent`；附件响应只包含公开 URL、文件名、类型、大小等管理信息，不暴露本地磁盘路径、对象存储内部字段或 hash。统计 dashboard 不裁剪。
- 验证：已补 `AdminArticleControllerTest`、`AdminCommentControllerTest` 的 Web 契约测试；已有 `AdminArticleQueryServiceTest`、`AdminCommentQueryServiceTest` 覆盖 application 层裁剪；`AdminAttachmentControllerTest` 覆盖附件内部字段不暴露；已通过定向测试。
- 后续约束：前端只做只读体验优化，安全边界必须继续由后端返回裁剪字段和 `403 + 10003` 写操作拒绝。
- 来源：`security-baseline.md`、`docs/handbook/product/use-cases.md`、后台业务页设计文档、`handbook/api/article.md`、`handbook/api/comment.md`、`handbook/api/attachment.md`

## O-003 前台读者主链路补齐

- 状态：未完成
- 优先级：P0
- 影响范围：前台 blog、公开 API
- 当前判断：首页、公开文章列表、文章详情、站点配置、分类、标签、归档、关于页和搜索已接入；友链仍待补齐。
- 下一步：友链已下调优先级，后续单独重开讨论；页脚统计归入 O-020。
- 来源：`../frontend/blog/integration-status.md`、`roadmap.md`

## O-004 前台评论、留言和统计接入

- 状态：未完成
- 优先级：P1
- 影响范围：前台 blog、comment、stats
- 当前判断：后端评论、留言板和统计基础能力已经完成，但前台接入仍未形成完整读者交互闭环。
- 下一步：按 `docs/working/plans/2026-06-30-frontend-integration-batch-plan.md` 的 Batch 3 / Batch 4 / Batch 5 推进。统计先接 V2 打点和页脚站点摘要；评论第一批接文章评论，第二批接留言板；PASSWORD 文章评论依赖完整解锁链路；最近评论侧栏已裁决移除。
- 来源：`roadmap.md`、`../frontend/blog/integration-status.md`

## O-005 后台内容生产闭环

- 状态：已关闭
- 优先级：P0
- 影响范围：后台 admin、content、system attachment
- 关闭原因：已对照 `E:\My-Blog\frontend\apps\admin/src/` 校准；文章列表、新建、编辑、Markdown 预览、本地草稿、回收站、附件管理、文章封面选择、站点图片选择均已有实现记录。剩余争议不再放在本条展开。
- 后续跟踪：统计 TOP 文章 ID 类型见 O-012。
- 来源：后台 article/editor/attachment 设计与计划文档

## O-006 后台其他业务页完成度

- 状态：已关闭
- 优先级：P1
- 影响范围：后台 admin、category/tag、comment、friend-link、site-config、stats
- 关闭原因：已建立并校准 `../frontend/admin/integration-status.md`；分类、标签、评论、友链、站点配置、作者资料、附件和统计仪表盘均已有当前状态。
- 后续跟踪：统计 TOP 文章 ID 类型见 O-012。
- 来源：`frontend/apps/admin/docs/`

## O-007 上线前 SEO、发布和运维准备

- 状态：未完成 / 清单已建立 / 实现待设计
- 优先级：P1
- 影响范围：前台 blog、ops、CI/CD
- 当前判断：`docs/handbook/ops/release-checklist.md` 已存在并覆盖测试、生产环境变量、CORS、反向代理、客户端 IP 和附件存储检查；RSS、Sitemap、robots、SEO meta、备份、CI/CD、部署文档和上线冒烟仍需按发布清单补齐。
- 风险：如果上线前只验证接口可用，忽略 SEO、备份、生产环境变量、反向代理和 CI/CD，容易出现可运行但不可收录、不可恢复、限流 IP 错误或部署不可复现的问题。
- 下一步：以 `docs/handbook/ops/release-checklist.md` 作为上线前权威清单继续补实现。前台补 SEO meta、canonical、RSS、Sitemap、robots；运维补生产环境变量核对、备份/恢复演练、反向代理/CORS/IP 验证；CI/CD 至少跑后端测试和前台 lint/typecheck/build；上线前执行公开页和后台登录冒烟。
- 来源：`roadmap.md`、`docs/handbook/ops/release-checklist.md`

## O-008 后台 token 存储方式升级

- 状态：暂缓 / 风险已接受
- 优先级：P2
- 影响范围：后台 admin、后端 auth、安全策略
- 当前判断：后台当前使用 localStorage 保存 access token 和 refresh token。该方案的主要风险是后台发生 XSS 时，脚本可以读取 `myblog-admin-session` 并带走 access/refresh token。考虑当前项目是个人博客后台、后台入口受控，且后端已有 access token 短 TTL、refresh token DB hash 存储、refresh 轮换、旧 token 重放失败、logout/改密撤销等机制，暂不升级到 HttpOnly Cookie。
- 风险：localStorage token 无法抵御 XSS 读取；一旦后台渲染未清洗 HTML、引入不可信第三方脚本或供应链依赖被污染，攻击脚本可读取 token 并在 refresh token 过期或被撤销前冒用后台会话。localStorage 还会跨浏览器重启保留会话，丢失设备或共享设备使用时风险高于纯内存 token。
- 暂缓理由：HttpOnly Cookie 方案不是单纯替换存储位置，会引入 CSRF 防护、SameSite、跨域 credentials、反向代理、refresh cookie 路径、登出清理、本地开发和生产部署一致性等一整套改造；对当前单人后台收益有限，复杂度和回归面高于收益。
- 下一步：按 `docs/working/plans/2026-06-30-admin-token-storage-risk-decision.md` 记录风险接受。近期继续 localStorage 方案，但必须控制 XSS 面：后台不渲染未清洗 HTML，token 不进入 URL/日志/第三方 SDK，减少第三方脚本/CDN，依赖升级走审查。若后台开放多人长期使用、引入大量第三方脚本、部署切到同源 Cookie 架构或安全要求提升，再重开本项设计 HttpOnly Cookie + CSRF 方案。
- 来源：后台 auth 实现、`docs/handbook/architecture/auth-flow.md`、`docs/handbook/rules/security-baseline.md`

## O-009 多实例部署下限流方案

- 状态：暂缓 / 单体单实例前提已确认
- 优先级：P3
- 影响范围：auth、comment、stats、ops
- 当前判断：当前项目按单体、单实例部署进入开发流程，登录、评论和统计限流继续使用进程内 Caffeine。该方案适合当前 V2 单机部署，不为尚未发生的多实例场景提前引入 Redis、数据库或网关限流复杂度。
- 风险：如果未来改为多实例部署，进程内限流不会跨实例共享，同一客户端可绕过单实例阈值打到不同节点。
- 下一步：当前只记录部署前提，不进入近期实现。若未来明确多实例部署，再重开本项，评估 Redis、数据库或网关层限流，并同步更新部署文档和压测验证。
- 来源：`security-baseline.md`

## O-010 公开内容接口 ID 类型不一致

- 状态：已关闭
- 优先级：P1
- 影响范围：后端 content、前台 blog、API 契约
- 关闭原因：已在 `feature/blog-contract-foundation` 落实 HTTP JSON 边界转换。后端 Entity / Domain / Application 继续使用数值 ID，公开文章、公开分类、公开标签响应 VO 和 Web mapping 将前端可见 Snowflake ID 输出为 JSON string；公开文章标签改为 web 层 VO，避免 application result 直接暴露到 HTTP 契约。
- 验证：已补 `PublicArticleControllerTest`、`PublicCategoryTagControllerTest`、`ArticleOpenApiTest`、`CategoryTagOpenApiTest`，并通过定向测试。
- 来源：`handbook/api/article.md`、`handbook/api/category-tag.md`、content web 代码

## O-011 公开评论接口 ID 类型不一致

- 状态：已关闭
- 优先级：P1
- 影响范围：后端 comment、前台 blog、API 契约
- 关闭原因：已在 `feature/blog-contract-foundation` 落实 HTTP JSON 边界转换。公开评论列表和提交响应中的 `id`、`parentId`、`replyToCommentId` 均输出为 JSON string；空父级和空回复目标继续保持 `null` / 不输出。
- 验证：已补 `PublicCommentControllerTest` 和 `CommentOpenApiTest`，覆盖嵌套回复和评论创建响应，并通过定向测试。
- 来源：`handbook/api/comment.md`、comment web 代码

## O-012 统计 TOP 文章 ID 类型不一致

- 状态：已关闭
- 优先级：P2
- 影响范围：后端 stats、后台 admin、API 契约
- 关闭原因：已在 `feature/blog-contract-foundation` 落实 HTTP JSON 边界转换。`StatsDashboardVO.TopArticle.articleId` 输出为 JSON string，统计聚合、查询和 `articleId=0` 的首页/非文章页汇总语义不变。
- 验证：已补 `AdminStatsControllerTest`、`StatsOpenApiTest`、`StatsIntegrationTest`，并通过定向测试。
- 来源：`handbook/api/stats.md`、stats web 代码

## O-013 文章置顶和推荐能力缺失

- 状态：已关闭
- 优先级：P0
- 影响范围：后端 content、后台 admin、前台 blog、API 契约、Schema 迁移
- 关闭原因：已实现独立首页展示槽位 `NONE / PINNED / FEATURED`。后端新增 `homepage_slot`、写入校验和 `GET /api/public/articles/home`；后台文章列表和编辑表单支持维护槽位；前台首页消费聚合接口，不再从普通分页切片推断置顶或推荐语义。
- 验证：后端 Maven 测试、后台 Vitest/typecheck、前台 Vitest/typecheck/build 通过；GitHub Actions `backend-mysql-test` 已纳入主线 CI。
- 来源：`docs/working/plans/2026-06-30-frontend-homepage-slot-implementation-plan.md`、`docs/handbook/api/article.md`、当前 content/admin/blog 代码

## O-014 公开 URL 标识策略与 slug 生命周期

- 状态：已关闭
- 优先级：P0
- 影响范围：前台 blog、后端 content、公开 API、SEO、分享链接、后台分类标签编辑
- 当前判断：公开 URL 采用混合策略。文章继续沿用现有 ID 主导路径 `/:lang/posts/:id/:slug?`，slug 只增强可读性，不作为文章业务唯一标识；分类和标签公开页采用 slug 主导路径 `/:lang/categories/:slug`、`/:lang/tags/:slug`。后台 admin 内部管理接口继续使用 ID。分类和标签更新服务已拒绝修改已创建 slug，后台分类/标签表单已在编辑态禁用 slug 并提示其公开 URL 重要性。
- 风险：如果把文章也改成 slug 主导，需要追加文章 slug 全局唯一、发布后锁定、旧 ID URL 兼容、空 slug/重复 slug 迁移和误填修正工具，当前收益不足；如果分类/标签 slug 允许随意修改，会导致导航入口、外链和搜索收录路径失效。
- 关闭原因：已落实混合策略。后端公开文章列表支持 `categorySlug/tagSlug` 筛选；前台分类和标签侧栏、标签页使用 `/:lang/categories/:slug`、`/:lang/tags/:slug` 路由进入公开文章列表；文章详情继续使用 ID 主导路由。
- 来源：`docs/working/reviews/2026-06-30-frontend-backend-gap-review.md` G-002/G-011、`docs/working/plans/2026-06-30-frontend-url-slug-implementation-plan.md`、`ContentSlug`、当前 category/tag/content 代码

## O-015 公开分类和标签缺少文章数量

- 状态：已关闭
- 优先级：P1
- 影响范围：后端 content、公开 API、前台 blog、后台 admin
- 当前判断：Aurora/Hexo 原始分类和标签 JSON 都包含 `count`，前台分类侧栏、标签侧栏和标签页都会展示该数量。V2 公开分类/标签接口已返回 `id/name/slug/articleCount`。
- 风险：如果不补字段，前台迁移后分类和标签数量展示会丢失；如果把数量落库到 `t_category` / `t_tag`，文章发布、下架、定时发布、删除恢复、改分类、改标签等动作都要维护计数，容易产生不一致。
- 关闭原因：`articleCount` 不新增数据库存储字段，由公开查询聚合得出；统计口径与公开文章列表一致：文章未软删除，状态为 `PUBLISHED` 或 `PASSWORD`，且 `publish_at <= now`。公开读者端只返回 `articleCount > 0` 的分类和标签；前台 taxonomy mapper 已映射为现有组件使用的 `count`。
- 来源：`docs/working/reviews/2026-06-30-frontend-backend-gap-review.md` G-003、`docs/working/plans/2026-06-30-frontend-taxonomy-count-implementation-plan.md`、前台 `Category.count` / `Tag.count`、当前 category/tag/content 代码

## O-016 公开归档时间线接口缺失

- 状态：已关闭
- 优先级：P1
- 影响范围：后端 content、公开 API、前台 blog
- 关闭原因：已新增 `GET /api/public/archives?page=1&size=12&lang=zh`。后端复用公开文章分页口径，按文章数分页后对当前页按年月分组；归档文章项只返回 `id/title/slug/publishedAt/summary`，不返回正文、分类、标签、封面、评论数、状态、锁定标记或密码字段。前台 `archives.vue` 已停止使用旧 `/archives/{page}.json` 和旧 `post-slug` 路由，改用 `useArticleStore().loadArchives(...)` 和 `article-detail` 的 `{ lang, id, slug }` 参数。
- 验证：后端已通过 `PublicArticleQueryServiceTest`、`PublicArchiveControllerTest`、`BackendPropertiesTest`、`ArticleOpenApiTest` 定向验证；前台已通过 `pnpm --dir frontend/apps/blog typecheck` 和 `pnpm --dir frontend/apps/blog test`。
- 来源：`docs/working/reviews/2026-06-30-frontend-backend-gap-review.md` G-004、`docs/working/plans/2026-07-03-blog-archive-timeline-implementation-plan.md`、`handbook/api/article.md`、当前 content/blog 代码

## O-017 搜索实现方式与前后端能力不一致

- 状态：已关闭
- 优先级：P1
- 影响范围：后端 content、公开 API、前台 blog
- 关闭原因：前台搜索弹窗已改用 `GET /api/public/articles?keyword=...`，结果使用公开文章列表口径，只展示标题和摘要；最近搜索、键盘上下选择和回车跳转保留；旧 `/search.json` 和正文高亮逻辑已下线。第一版不做全文搜索或 search index。
- 验证：已通过 `mvn -f MyBlog-springboot-v2/pom.xml -Dtest=ArticleIntegrationTest test`、`pnpm --dir frontend/apps/blog typecheck` 和 `pnpm --dir frontend/apps/blog test`。
- 来源：`docs/working/reviews/2026-06-30-frontend-backend-gap-review.md` G-005、`docs/working/plans/2026-07-05-blog-search-keyword-implementation-plan.md`、前台 `SearchModal.vue` / `Search.class.ts`、当前 public article keyword 查询代码

## O-018 关于页仍依赖旧 page JSON

- 状态：已关闭
- 优先级：P1
- 影响范围：前台 blog、后端 system、后台 admin、Markdown 渲染
- 关闭原因：前台 About 页已读取 `siteSettings.aboutMd`，使用现有 `renderMarkdown` 渲染 Markdown，保留 raw HTML 禁用策略；为空时走现有骨架空态；后端和后台未新增 schema 或入口。
- 验证：已通过 `pnpm --dir frontend/apps/blog typecheck` 和 `pnpm --dir frontend/apps/blog test`。
- 来源：`docs/working/reviews/2026-06-30-frontend-backend-gap-review.md` G-006、`docs/working/plans/2026-07-05-blog-about-page-implementation-plan.md`、前台 `about.vue` / `PageContent.vue`、`PublicSiteConfigVO.aboutMd`、`renderMarkdown`

## O-019 评论和留言迁移到 V2 自研 API

- 状态：未完成 / 独立专题 / 部分方案已定
- 优先级：P1
- 影响范围：后端 comment、后端 content、前台 blog、后台 admin、API 契约
- 当前判断：V2 后端已有文章评论、留言板评论、后台审核、隐藏、删除、恢复和文章 `commentCount` 闭环；前台当前仍绑定 Gitalk / Valine / Twikoo / Waline。评论接入范围较大，涉及 PASSWORD 解锁、公开评论 ID string、最近评论、评论数和第三方插件下线，应作为独立专题推进。
- 风险：继续走第三方插件会让后端自研评论和审核能力无法形成闭环；如果在旧插件适配层上继续叠逻辑，会同时背负第三方插件状态和 V2 自研评论状态，后续难以维护。
- 下一步：后续单独做评论专题设计。方向为前台重写 V2 评论组件，不继续依赖第三方评论插件；第一批优先接文章评论，留言板第二批，友链评论不做；最近评论侧栏直接移除，不保留第三方最新评论，也不规划 V2 公开最近评论接口；PASSWORD 文章评论在完整解锁流程完成前禁用；评论数优先使用文章接口 `commentCount`；公开评论 ID 先按 O-011 修正为 string 后再正式接入。
- 来源：`docs/working/reviews/2026-06-30-frontend-backend-gap-review.md` G-008、`docs/handbook/api/comment.md`、前台 `Comment.vue` / `PostStats.vue` / `RecentComment.vue`、当前 comment/content 代码

## O-020 访问统计前台打点和展示口径

- 状态：未完成 / 方案已定 / 实现待设计
- 优先级：P1
- 影响范围：后端 stats、后端 content、后端 system、前台 blog、后台 admin、API 契约
- 当前判断：V2 后端已经有公开访问打点、日 PV/UV 聚合和后台 dashboard，后台 dashboard 已展示 PV/UV、趋势、TOP 文章和语言分布；后台文章列表已展示文章 `commentCount`。前台原始 `PostStats` 核心展示是阅读时长和字数，但同时依赖 Waline / Twikoo / Valine / LeanCloud 展示文章浏览量和文章评论数；`FooterContainer` 依赖 Waline / Busuanzi 展示全站 PV/UV，并从 `themeConfig.site.started_date` 本地计算建站天数。`PostStats` 第一版不展示文章浏览量和文章评论数；页脚保留展示今日访客、访问量和建站天数，但数据源迁移到 V2 后端。
- 风险：如果前台继续展示第三方统计值，同时又给 V2 后端打点，读者端数字和后台 dashboard 会不一致；如果为了恢复展示过早新增公开统计读取接口，需要先裁决公开范围、缓存、累计/今日/区间口径和近实时要求。
- 下一步：按 `docs/working/plans/2026-06-30-frontend-page-view-stats-implementation-plan.md` 拆分实现。前台接 V2 page-view 打点：文章页传 `articleId + lang`，非文章页固定传 `articleId=0 + lang`，沿用现有 `0=首页/非文章页汇总` 约定；`PostStats` 只保留阅读时长和字数，第三方浏览量和第三方评论数展示注释或删除，并在代码注释中说明后期可通过 V2 `viewCount` / `commentCount` 恢复；`FooterContainer` 的 Waline / Busuanzi PV/UV 展示绑定注释或删除，改用公开站点统计摘要接口展示 `今日访客：todayUv` 和 `访问量：totalPv`。新增 `GET /api/public/stats/site-summary`，`todayUv` 为全站、全语言、所有公开页面今日日 UV 合计，`totalPv` 为全站、全语言、所有公开页面累计 PV；允许使用日聚合表并接受定时聚合延迟。站点配置表新增 `started_date DATE NULL`，后台站点配置维护 `startedDate`，公开站点配置返回 `startedDate`，前台本地计算建站天数；为空时不展示。
- 来源：`docs/working/reviews/2026-06-30-frontend-backend-gap-review.md` G-009、`docs/working/plans/2026-06-30-frontend-page-view-stats-implementation-plan.md`、`docs/handbook/api/stats.md`、前台 `PostStats.vue` / `FooterContainer.vue`、当前 stats/content/system 代码
