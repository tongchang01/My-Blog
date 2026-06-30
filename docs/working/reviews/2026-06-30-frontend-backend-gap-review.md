# 前台 Blog 与后端 V2 能力差异盘点

> 状态：初稿
> 适用范围：`frontend/apps/blog/`、`MyBlog-springboot-v2/` 公开接口
> 最后校准：2026-06-30
> 权威程度：整理过程材料

## 本文档回答什么问题

本文档从前台 blog 读者端出发，盘点当前页面、组件、旧 Aurora/Hexo 数据模型与后端 V2 公开接口之间的缺口和冲突。

本文档不是长期权威源。经人工裁决后，应将需要长期跟踪的事项提炼到 `docs/handbook/start-here/open-issues.md`，并同步更新 `docs/handbook/api/`、`docs/handbook/product/` 或对应前端文档。

## 范围

- 只看前台 blog 读者端。
- 不盘点后台 admin 页面。
- 不直接提出代码修改补丁。
- 优先记录有前台代码证据和后端代码/API 证据的问题。

## 盘点原则

- 前台 blog 原本是单前台项目，数据相关能力大量依赖静态 JSON 或第三方 SDK。
- 当前项目已经有自有 V2 后端，数据能力需要逐步从旧静态数据和第三方 SDK 迁移到后端。
- 迁移只代表数据来源和契约要收口到 V2 后端，不代表默认删除前台既有视觉效果或交互能力。
- 前台视觉效果、功能删减、功能保留和语义变化必须逐项讨论后裁决，不能仅因后端当前没有对应能力就直接删改。
- 每个差异项应分开判断“数据来源迁移”“后端能力缺失”“前台体验是否保留”三个问题。

## 总体判断

前台已经开始接入 V2 后端，但当前仍是“新 V2 接入层 + 旧 Aurora/Hexo 页面和插件体系”并存：

- 首页文章列表和文章详情已接入 `/api/public/articles`。
- 分类、标签、归档、关于、友链、搜索仍大量依赖旧 JSON 路径。
- 评论、访问量和最新评论仍依赖 Gitalk / Valine / Twikoo / Waline 插件体系。
- 后端部分公开能力已经存在，但前台未接入，或公开 URL 标识策略与后端查询参数尚未统一。
- 前台暴露出的部分小功能在后端 schema/接口中没有表达，例如文章置顶和推荐。

## 差异清单

### G-001 文章缺少置顶和推荐的后端表达

- 状态：后端缺能力 / 方案已定 / 实现待设计
- 优先级：P0
- 影响范围：content schema、后台文章管理、公开文章列表、前台首页推荐区
- 前台证据：
  - `frontend/apps/blog/src/models/Post.class.ts` 中旧 `Post` 模型包含 `feature` 和 `pinned`。
  - `frontend/apps/blog/src/pages/index.vue` 当前首页用 `records[0]` 作为 `featuredArticle`，用 `records.slice(1, 4)` 作为 `secondaryFeatures`，再从列表中跳过这些记录。
  - 三语文案仍保留 `settings.pinned`、`settings.featured`、`home.recommended`。
- 后端证据：
  - `t_article` / `ArticleEntity` 当前只有标题、摘要、正文、分类、作者、slug、状态、密码、发布时间、封面和评论数，没有 `pinned` / `featured` / 排序权重。
  - `/api/public/articles` 固定按 `publish_at DESC, id DESC` 排序。
- 风险：
  - 站长无法在后台显式控制首页首屏推荐内容。
  - 当前“第一页第一篇就是推荐”的前端临时逻辑会把普通最新文章误当推荐。
  - 如果之后补字段，会牵动 schema、后台编辑表单、公开列表排序和 API 契约。
- 建议：
  - 已确认保留文章置顶与推荐能力，并采用独立首页展示槽位，不并入文章发布状态。
  - 槽位建议为 `NONE / PINNED / FEATURED`：`PINNED` 最多 1 篇，`FEATURED` 最多 2 篇，只有 `PUBLISHED` 文章可进入槽位。
  - 首页接口由后端明确返回 `pinnedArticle`、`featuredArticles`、`articles`，普通文章列表排除已置顶/推荐文章。
  - 前台只有在存在真实槽位数据时才显示置顶或推荐语义标识；`Feature` / `FeatureList` 卡片区域可以由普通文章自然补位，但不能把普通文章伪装成置顶或推荐。
  - 后续按 `docs/working/plans/2026-06-30-frontend-homepage-slot-implementation-plan.md` 拆分实现。

### G-002 公开 URL 标识策略与 slug 生命周期

- 状态：前后端契约不一致 / 已并入 O-014 / 方案已定
- 优先级：P0
- 影响范围：文章详情、分类页、标签页、文章筛选、侧栏分类标签、SEO、分享链接
- 前台证据：
  - `frontend/apps/blog/src/stores/post.ts` 调用 `fetchPostsListByCategory(category)` 和 `fetchPostsListByTag(slug)`。
  - `frontend/apps/blog/src/api/index.ts` 请求 `/categories/{categoryName}.json` 和 `/tags/{tagName}.json`。
  - `frontend/apps/blog/src/components/Sidebar/src/CategoryBox.vue` 点击分类后把 `category.slug` 放入 `post-search` query。
  - `frontend/apps/blog/src/components/Sidebar/src/TagBox.vue` 同样以 tag slug 作为前台交互主键。
  - `router/index.ts` 新文章详情路由是 `/:lang/posts/:id/:slug?`，文章以 ID 主导，slug 只增强可读性。
- 后端证据：
  - `PublicArticleController` 支持 `categoryId`、`tagId`、`keyword`、`archiveMonth`。
  - `ArticleMapper.xml` 公开查询过滤条件是 `a.category_id = #{query.categoryId}` 和 `filter_at.tag_id = #{query.tagId}`。
  - `PublicCategoryVO` / `PublicTagVO` 返回 `id/name/slug`，但文章过滤接口没有 slug 参数。
  - 分类/标签 slug 有格式校验、唯一键和软删除后不可复用约束，但当前仍可编辑，没有旧 slug 的 redirect / alias / history 机制。
- 风险：
  - 如果前台保留 slug URL，需要先通过公开分类/标签列表把 slug 解析成 ID，或后端新增 slug 过滤。
  - 当前页面迁移时容易出现“URL 是 slug，API 要 ID”的隐式转换散落在组件里。
  - 若分类/标签 URL 以 slug 主导，后台修改 slug 会导致旧收藏、外链和搜索引擎收录链接失效。
  - 文章因 URL 以 ID 主导，slug 修改后仍可凭 ID 找到文章；分类/标签若只有 slug，则没有同等兜底。
- 建议：
  - 已裁决公开 URL 采用混合策略：文章继续 ID 主导，slug 只增强可读性；分类和标签采用 slug 主导。
  - 文章继续使用 `/:lang/posts/:id/:slug?`，不在本轮改成 slug 主导，不引入文章 slug 锁定、history、redirect 或 alias。
  - 分类和标签创建后锁定 slug，公开列表、侧栏和筛选页使用 slug URL。
  - 后台分类/标签页面需要提示 slug 的公开 URL 重要性，提醒尽量一次性确定。
  - 后续按 `docs/working/plans/2026-06-30-frontend-url-slug-implementation-plan.md` 拆分实现。
  - 同步更新 `handbook/api/article.md`、`category-tag.md`、业务规则和必要 ADR。

### G-003 公开分类/标签缺少文章数量 count

- 状态：后端缺字段 / 方案已定 / 实现待设计
- 优先级：P1
- 影响范围：后端 content、公开 API、前台 blog、后台 admin
- 前台证据：
  - 旧 `Category` / `Tag` 模型都有 `count`。
  - `CategoryBox.vue`、`TagBox.vue`、`tags.vue` 都展示 `count`。
- 后端证据：
  - `PublicCategoryVO` 当前只返回 `id/name/slug`。
  - `PublicTagVO` 当前只返回 `id/name/slug`。
  - `CategoryMapper.xml` / `TagMapper.xml` 的公开列表只查询 active 分类/标签本体，没有聚合公开文章数量。
- 风险：
  - 如果前台继续展示 count，迁移到 V2 API 后会缺字段。
  - count 语义需要明确：是否只统计当前已公开且未删除的 `PUBLISHED/PASSWORD` 文章，是否按 `publish_at <= now` 过滤。
- 建议：
  - 已确认保留公开分类/标签文章数量，后端公开分类/标签列表补 `articleCount`。
  - `articleCount` 不落库，不在 `t_category` / `t_tag` 增加计数字段；由公开查询 SQL 按当前文章状态实时聚合。
  - 统计口径与公开文章列表一致：文章未软删除，状态为 `PUBLISHED` 或 `PASSWORD`，且 `publish_at <= now`。
  - 公开读者端分类/标签列表只返回 `articleCount > 0` 的项；后台 admin 分类/标签管理仍返回完整 active 列表。
  - 后续按 `docs/working/plans/2026-06-30-frontend-taxonomy-count-implementation-plan.md` 拆分实现。

### G-004 归档页仍依赖旧 `/archives/{page}.json`，后端只有按月份过滤，没有归档聚合接口

- 状态：后端缺聚合接口 / 方案已定 / 实现待设计
- 优先级：P1
- 影响范围：后端 content、公开 API、前台 blog
- 前台证据：
  - `archives.vue` 使用 `postStore.fetchArchives(page)`。
  - `fetchArchivesList` 请求 `/archives/{currentPage}.json`。
  - 旧 `Archives` 模型按文章日期在前端分组为 `month/year/posts`。
- 后端证据：
  - `/api/public/articles` 支持 `archiveMonth=yyyy-MM` 过滤。
  - 当前没有公开“月份列表 + 每月文章数”或“按月分组分页”的归档 API。
- 风险：
  - 仅靠 `archiveMonth` 可以实现“点某个月看文章”，但无法生成归档页面的月份轴。
  - 如果前端自行拉全量文章再分组，会破坏分页和性能边界。
- 建议：
  - 已确认保留原归档时间线体验，不降级为普通文章列表或月份索引页。
  - 新增公开归档时间线接口，建议 `GET /api/public/archives?page=1&size=12&lang=zh`。
  - 分页单位按文章数计算，`total` 表示文章总数；后端返回按年月分组后的 records。
  - 归档文章项只返回 `title`、`slug`、`publishedAt`、`summary`，不返回正文、分类、标签、封面、评论数或阅读时间。
  - 公开口径与公开文章列表一致：文章未软删除，状态为 `PUBLISHED` 或 `PASSWORD`，且 `publish_at <= now`；置顶/推荐不影响归档。
  - 后续按 `docs/working/plans/2026-06-30-frontend-archive-timeline-implementation-plan.md` 拆分实现。

### G-005 搜索仍依赖静态 `/search.json`，后端只有公开文章 keyword 模糊查询

- 状态：实现方式不一致 / 方案已定 / 实现待设计
- 优先级：P1
- 影响范围：搜索弹窗、搜索结果页、SEO/上线体验
- 前台证据：
  - `SearchModal.vue` 初始化时调用 `searchStore.fetchSearchIndex()`。
  - `stores/search.ts` 读取 `SearchIndexes` 并在浏览器内搜索。
  - `fetchSearchIndexes` 请求 `/search.json`。
- 后端证据：
  - `PublicArticleController` 的公开文章分页支持 `keyword`。
  - `ArticleMapper.xml` 只对标题和摘要做 LIKE，不搜索正文。
  - 当前没有 `/api/public/search` 或静态 search index 生成接口。
- 风险：
  - 旧搜索是前端本地索引体验；V2 keyword 是服务端分页搜索，能力和 UX 不等价。
  - 如果继续保留搜索弹窗，需要决定是服务端实时搜索，还是构建期/运行时生成 search index。
- 建议：
  - 第一版不做独立全文搜索系统，也不生成 `/search.json` 静态索引。
  - 前台搜索弹窗保留，数据源改为后端公开文章 keyword 查询。
  - 搜索范围限定为标题和摘要，不搜索正文，不做服务端高亮片段。
  - 搜索结果展示标题和摘要，点击后跳转文章 slug 路由；最近搜索和键盘导航等弹窗交互尽量保留。
  - 公开口径与公开文章列表一致：文章未软删除，状态为 `PUBLISHED` 或 `PASSWORD`，且 `publish_at <= now`。
  - 后续如果需要更强搜索，再单独设计 search index 或全文搜索接口。
  - 后续按 `docs/working/plans/2026-06-30-frontend-search-keyword-implementation-plan.md` 拆分实现。

### G-006 关于页仍取旧 page JSON，后端已有 `aboutMd`

- 状态：后端已有能力 / 方案已定 / 实现待设计
- 优先级：P1
- 影响范围：前台 blog、后端 system、后台 admin、Markdown 渲染
- 前台证据：
  - `about.vue` 使用 `articleStore.fetchArticle('about')`。
  - `fetchImplicitPageBySource` 请求 `/pages/{source}/index.json`。
- 后端证据：
  - `PublicSiteConfigVO` 返回 `aboutMd`。
  - `SiteConfig` 支持 `aboutMdZh/aboutMdJa/aboutMdEn` 并按语言 fallback。
  - 前台 `features/site-settings` 已接收 `aboutMd`，但页面未使用。
- 风险：
  - 关于页仍依赖旧静态 JSON，和 V2 后台站点配置的 About Markdown 脱节。
  - 需要确认前台渲染 About Markdown 时是否禁用 raw HTML 或做清洗。
- 建议：
  - 已确认关于页改为读取 V2 公开站点配置 `aboutMd`，不再请求旧 `/pages/about/index.json`。
  - 前台复用现有 `renderMarkdown(aboutMd)` 渲染，保持 `html: false`，不直接 `v-html` 插入 Markdown 原文。
  - 现阶段尽量保留旧 About 页面视觉和组件，包括 `Profile`、`Toc`、`PostStats`。
  - `aboutMd` 为空时显示空态或骨架，不伪造旧 page。
  - 后端和后台已有 About Markdown 能力，原则上不新增后端 schema 或后台入口。
  - 后续按 `docs/working/plans/2026-06-30-frontend-about-page-implementation-plan.md` 拆分实现。

### G-007 友链页后端已有列表 API，但旧前台需要的页面正文、头像墙、分组和评论不在当前契约中

- 状态：部分已有 / 优先级下调 / 后续重开讨论
- 优先级：P3
- 影响范围：友链页、站点配置、评论/留言
- 前台证据：
  - `links.vue` 使用 `articleStore.fetchArticle('links')` 获取一个旧 `Page`。
  - 旧 `Page` 包含 `content`、`avatarWall`、`categoryMode`、`data`、`comments`。
  - 友链页还会挂 `PostStats` 和第三方 `Comment`。
- 后端证据：
  - `PublicFriendLinkController` 提供 `/api/public/friend-links`。
  - `PublicFriendLinkVO` 只返回 `id/name/url/avatarUrl/description`。
  - `t_friend_link` 只有显示/隐藏、排序等主表字段，不含分组、页面正文或申请说明。
- 风险：
  - 只接后端友链列表可以显示卡片，但旧友链页的顶部说明、头像墙、分组模式和评论区域会丢失。
  - 如果把友链正文塞进 `site_config.aboutMd` 不合适；需要单独页面内容来源。
- 建议：
  - 已判断友链不是当前前台接入主链路，优先级下调，后续单独重开讨论。
  - 第一版不复刻 Aurora 完整友链页，不让头像墙、随机访问、分组、申请说明、评论和统计阻塞主线。
  - 后端已有轻量友链列表能力，后续若需要可先接 `/api/public/friend-links` 做简单列表页。
  - 如未来要恢复完整体验，再单独裁决分组、页面正文、申请说明、评论目标和后台配置入口。

### G-008 评论和留言板后端已有自研 API，但前台仍绑定第三方评论插件

- 状态：实现方式不一致 / 独立专题 / 方向已定
- 优先级：P1
- 影响范围：文章详情、留言板/友链评论、最新评论侧栏
- 前台证据：
  - `Comment.vue` 根据 Gitalk / Valine / Twikoo / Waline 配置初始化第三方评论容器。
  - `PostStats.vue` 的评论数也依赖 Waline / Twikoo / Valine 插件。
  - `RecentComment.vue` 通过 `useCommentPlugin()` 获取第三方最新评论。
- 后端证据：
  - `PublicArticleCommentController` 提供文章评论列表和提交。
  - `PublicGuestbookCommentController` 提供留言板评论列表和提交。
  - `PublicCommentVO` 返回 `contentHtml` 和嵌套 `replies`。
  - 当前公开评论 ID 仍是 JSON number，已在 O-011 登记。
- 风险：
  - 前台继续走第三方插件时，后端自研评论、审核、邮件失败日志和文章 `commentCount` 无法形成闭环。
  - 旧插件支持的头像、最近评论、访客统计等体验与 V2 自研评论不等价。
- 建议：
  - 已确认评论作为独立专题推进，不阻塞当前前台主浏览链路梳理。
  - 正式评论方案不继续依赖 Gitalk / Valine / Twikoo / Waline；前台评论组件应重写为 V2 API 组件，不在旧插件适配层上继续堆逻辑。
  - 第一批优先接文章评论；留言板作为第二批；友链评论不做，随 G-007 下调。
  - 最近评论侧栏直接移除，不保留第三方最新评论，也不规划 V2 公开最近评论接口。
  - PASSWORD 文章评论先禁用，等完整解锁流程完成后再接。
  - 评论数优先使用文章接口已有 `commentCount`，不走第三方插件。
  - 评论 ID 类型先按 O-011 修正为前端可见 string 后再正式接入。

### G-009 访问统计后端已有打点 API，但前台仍依赖第三方访问量

- 状态：实现方式不一致 / 方案已定 / 实现待设计
- 优先级：P1
- 影响范围：文章详情、页脚访问量、统计 dashboard 数据来源
- 前台证据：
  - `PostStats.vue` 原始核心展示是阅读时长和字数。
  - `PostStats.vue` 的页面访问量展示依赖 Waline / Twikoo / Valine。
  - `PostStats.vue` 的评论数展示也会依赖 Waline / Twikoo。
  - `FooterContainer.vue` 通过 Waline / Busuanzi 展示全站 PV / UV。
  - `FooterContainer.vue` 的建站天数来自 `themeConfig.site.started_date`，由前台本地计算。
- 后端证据：
  - `PublicPageViewController` 提供 `POST /api/public/stats/page-views`。
  - `PageViewRecordRequest` 只需要 `articleId` 和 `lang`，referrer/IP/UA 由后端解析。
  - 后台 `StatsDashboardVO` 已返回区间 PV、今日 PV、今日 UV、平均日 UV、趋势、TOP 文章和语言分布；后台 dashboard 页面已经展示这些数据。
  - 公开文章列表和详情已经返回 `commentCount`。
  - 后台文章列表已经展示文章 `commentCount`。
- 风险：
  - 前台不打 V2 统计接口时，后台 dashboard 的 PV/UV 数据不完整。
  - 如果同时保留第三方统计和 V2 统计，前台展示值与后台统计值可能不一致。
  - 当前后端没有公开读取单篇文章浏览量、全站 PV / UV 的接口，不能直接替换原前台第三方展示。
  - 当前后端站点配置没有 `startedDate`，建站天数不能从 V2 配置读取。
- 建议：
  - 前台文章详情和首页/非文章页按路由统一触发 V2 page-view 打点；非文章页固定传 `articleId=0`。
  - `PostStats.vue` 第一版只保留阅读时长和字数；第三方浏览量和第三方评论数展示注释或删除，并在代码注释中说明后期可分别对接 V2 `viewCount` 和 `commentCount`。
  - 前台文章评论数暂不展示；后期如果恢复展示，可直接使用文章接口已有 `commentCount`，不再依赖评论插件计数。
  - 前台文章浏览量暂不展示；后期如果需要展示“本文浏览量”，优先在文章详情 VO 增加 `viewCount`，实现成本不高。
  - `FooterContainer.vue` 移除 Waline / Busuanzi 的 PV / UV 展示绑定，但保留页脚统计展示能力。
  - 页脚展示口径已定为：`今日访客 = 全站、全语言、所有公开页面今日日 UV 合计`，`访问量 = 全站、全语言、所有公开页面累计 PV`。
  - 后端新增公开站点统计摘要接口，建议 `GET /api/public/stats/site-summary`，返回 `todayUv` 和 `totalPv`；允许使用日聚合表，接受定时聚合延迟。
  - 建站天数保留展示；后端站点配置表补 `started_date DATE NULL`，公开站点配置返回 `startedDate`，前台本地计算天数；为空时不展示建站天数。

### G-010 公开 ID 类型仍不一致，会影响前台继续接分类、标签、评论和统计

- 状态：已登记 / 仍阻塞前台接入
- 优先级：P1
- 影响范围：content、comment、stats、前台类型定义
- 前台证据：
  - `features/articles/contract.ts` 已按 string 定义文章、分类、标签 ID。
  - `shared/http/contract.ts` 没有对 ID 做运行时转换，依赖各 feature mapper。
- 后端证据：
  - `PublicArticlePageItemVO` / `PublicArticleDetailVO` 使用 `long` / `Long`。
  - `PublicCategoryVO` / `PublicTagVO` 使用 `long`。
  - `PublicCommentVO` / `PublicCommentCreateVO` 使用 `long` / `Long`。
  - `PageViewRecordRequest.articleId` 是 `Long`。
  - `open-issues.md` 已登记 O-010、O-011、O-012。
- 风险：
  - 分类/标签/评论接入越多，前端 string ID 与后端 number ID 的不一致面越大。
  - Snowflake ID 作为 JSON number 可能有精度风险。
- 建议：
  - O-010/O-011/O-012 已统一口径：后端内部继续使用 `long` / `Long`，HTTP JSON 边界统一把前端可见 Snowflake ID 转成 string。
  - 正式实现时先改后端公开 VO / stats VO 和对应测试，再校准前台 contract。

### G-011 旧文章详情路由和新 ID 主导路由并存，遗留页面会继续跳旧 `post-slug`

- 状态：前台迁移残留 / 已并入 O-014
- 优先级：P2
- 影响范围：归档、搜索、旧 ArticleCard 兼容路径
- 前台证据：
  - `router/index.ts` 新增了 `/:lang/posts/:id/:slug?` 的 `article-detail`。
  - `ArticleCard.vue` 对 legacy 数据仍跳 `post-slug`。
  - `archives.vue` 仍通过 `{ name: 'post-slug', params: { slug: post.slug } }` 跳旧路由。
  - `SearchModal.vue` 搜索结果也跳 `post-slug`。
- 后端证据：
  - `PublicArticleController` 详情接口是 `/api/public/articles/{id}`，只按 ID 查。
  - 业务规则 BR-203 明确文章 URL 以 id 为主，slug 只增强可读性。
- 风险：
  - 迁移完成前，同一页面可能存在 ID 路由和 slug 路由两套路径。
  - 搜索、归档、分类标签迁移时容易继续生成不可用旧链接。
- 建议：
  - 前台迁移页面时统一使用 `article-detail`，旧 `post-slug` 只作为临时兼容路径。
  - 归档和搜索结果必须拿到文章 ID 后再跳转。
  - 公开 URL 标识策略统一在 O-014 中裁决。

## 建议后续处理顺序

1. 先裁决文章 `pinned` / `featured` 是否保留，以及二者语义是否独立。
2. 先处理 O-010/O-011 公开 ID 类型，避免后续前台接入反复改 contract。
3. 前台主链路按页面垂直切片推进：
   - 分类/标签列表与筛选。
   - 归档聚合。
   - 关于页。
   - 友链页。
   - 搜索。
4. 评论、留言和统计作为第二批互动能力接入，不阻塞分类/标签/归档等主浏览链路。
5. 每完成一个裁决，把结果提炼进 `open-issues.md`、`handbook/api/` 或 `handbook/product/`，不要让本 review 文档长期承载权威结论。

## 可提炼到 open issues 的候选

| 候选编号 | 事项 | 建议归属 |
|----------|------|----------|
| O-013 | 文章置顶/推荐能力缺失 | 已提炼 |
| O-014 | 公开 URL 标识策略与 slug 生命周期 | 已提炼 |
| O-015 | 分类/标签公开 articleCount | 已提炼 |
| O-016 | 归档时间线接口缺失 | 已提炼 |
| O-017 | 搜索实现方式裁决 | 已提炼 |
| O-018 | 关于页数据源迁移到 aboutMd | 已提炼 |
| O-019 | 评论/留言迁移到 V2 自研 API | 已提炼 |
| O-020 | 访问统计前台打点和展示口径 | 已提炼 |
| O-004 | 最近评论公开接口是否保留 | 已裁决移除 |
| O-010/O-011/O-012 | 公开 ID 类型统一 | 已有登记 |
