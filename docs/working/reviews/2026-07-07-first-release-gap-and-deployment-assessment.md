# V2 第一版缺口与部署评估

> 状态：当前评估
> 适用范围：MyBlog V2 第一版发布前范围、前台缺口、上线准备
> 日期：2026-07-07
> 对应代码：`frontend/apps/blog/`、`frontend/apps/admin/`、`MyBlog-springboot-v2/`

## 结论

第一版还不能只说“补友链 + 部署”。前台仍有几类旧 Aurora/Hexo 数据源和 V2 API 未对齐点，尤其是作者卡片、移动菜单、友链页、页脚友链和通用 page 评论入口。部署侧也缺少服务器现状、环境变量清单、S3 实战校准、反向代理拓扑和 CD 方案。

第一版前建议优先处理：

1. 前台残留旧数据源清理第一批：作者卡片 / 移动菜单 / 友链页 / 页脚友链。
2. 删除或停用旧第三方评论和旧 page 入口的活跃消费者。
3. S3 生产模式校准：后端配置、凭证链、Bucket Policy 或 CDN、附件上传/读取/删除冒烟。
4. 部署硬项文档：服务器现状、反向代理、TLS、环境变量、备份恢复、手动部署步骤。

第一版后置：

- 留言板评论。
- PASSWORD 文章完整解锁。
- 完整 SEO / RSS / Sitemap / Open Graph。
- 后台筛选组件扩展。
- CD 自动化深水区。
- 邮件能力和邮件模板细化。

## 前台残留数据源

| 位置 | 当前数据源 | 现状判断 | 第一版建议 |
|------|------------|----------|------------|
| `components/Sidebar/src/Profile.vue` | `useAuthorStore().fetchAuthorData('blog-author')` -> `/authors/blog-author.json`，并调用旧 `/statistic.json` | 作者卡片依赖旧静态作者和旧统计接口。后端当前没有公开作者聚合 API。 | 优先改为 V2 数据组合或最小公开作者 API；不要继续读 `public/api/authors`。 |
| `components/MobileMenu.vue` | `useAuthorStore().fetchAuthorData('blog-author')` -> `/authors/blog-author.json` | 移动菜单同样会出现作者信息旧数据或空态。 | 与作者卡片共用同一份 V2 作者/站点资料模型。 |
| `pages/links.vue` | `useArticleStore().fetchArticle('links')` -> `/pages/links/index.json` | 友链页仍是旧 page 结构，并挂旧第三方评论入口。 | 改为 `GET /api/public/friend-links` 简版卡片；不做友链评论、头像墙、分组、申请说明。 |
| `components/Footer/FooterLink.vue` | `useArticleStore().fetchArticle('links')` -> 旧 `avatarWall` | 页脚随机友链仍依赖旧 page 数据。 | 要么接 `GET /api/public/friend-links`，要么第一版直接隐藏随机友链块。 |
| `pages/page/[slug].vue` | `useArticleStore().fetchArticle(slug)` -> `/pages/{slug}/index.json` | 通用 page 仍依赖旧静态 page，并用旧第三方评论开关。 | 第一版只保留显式 `about.vue` / `links.vue`，通用 page 评论删除；未知 slug 走 404 或后置。 |
| `stores/app.ts#fetchStat` | `fetchStatistic()` -> `/statistic.json` | 旧统计接口仍被作者卡片间接调用；V2 统计已有 `features/stats`。 | 删除旧 `fetchStat` 或改用 V2 stats store。 |
| `stores/post.ts` / `api/index.ts` 部分函数 | `/archives/*.json`、`/tags/*.json`、`/categories/*.json` | 当前主页面已迁到 V2 features；这些大概率是死代码或旧兼容残留。 | 在上述消费者迁完后用 `rg` 再确认，统一删除旧 API helper 和旧 store。 |
| `public/api/*` | mock JSON | Phase 0 mock 数据，部分仍被旧消费者读取。 | 代码无消费者后再决定删除、归档或仅保留为历史 fixture。 |

## 后端公开 API 与前台使用差异

| 后端公开能力 | 前台状态 | 判断 |
|--------------|----------|------|
| `GET /api/public/site-config` | 已用于站点配置、关于页、页脚建站日期 | 仍缺作者卡片需要的公开作者资料或社交信息来源。 |
| `GET /api/public/articles` | 已用于列表、分类/标签筛选、搜索 | 可复用 `total` 给作者卡片文章数，避免为文章数新建接口。 |
| `GET /api/public/articles/home` | 已用于首页槽位 | 已接入。 |
| `GET /api/public/archives` | 已用于归档页 | 已接入。 |
| `GET /api/public/categories` / `tags` | 已用于侧栏和标签页 | 可复用数量给作者卡片 categories/tags。 |
| `GET /api/public/friend-links` | 后端已有，前台未接 | 第一版必须接，替换友链页和页脚旧数据。 |
| `GET/POST /api/public/articles/{id}/comments` | 已用于文章详情评论 | 已接入；PASSWORD 评论等 O-001。 |
| `GET/POST /api/public/guestbook/comments` | 前台未接 | 留言板后置，不阻塞第一版。 |
| `POST /api/public/stats/page-views` / `GET /api/public/stats/site-summary` | 已用于打点和页脚统计 | 作者卡片旧 `/statistic.json` 还没迁完。 |

可能遗留的后端能力：

- `SecurityProbeController` 只在 `local/test` profile 启用，不是生产遗留 API。
- `PublicGuestbookCommentController` 是后端已完成、前台后置的能力，不应删除。
- 后台 API 中文章列表和评论列表的筛选能力比当前 UI 更完整，属于后台体验后置，不影响第一版前台。

## 作者卡片方案

优先按少接口方案处理：

1. 作者名、描述、头像优先来自现有站点配置或前台 typed defaults。
2. 文章数用 `GET /api/public/articles?page=1&size=1` 的 `total`。
3. 分类数、标签数用公开分类/标签数组长度。
4. 字数统计如果后端没有现成口径，第一版隐藏，不新增统计接口。
5. 社交链接如果后端没有现成配置，第一版先沿用前台 defaults；需要后台可维护时再扩展站点配置。

如果站点配置字段不足以承载作者资料，第二选择是补一个最小 `GET /api/public/profile` 或扩展 `site-config` 公开响应。不要同时做 profile API、站点配置扩表和社交系统。

## 后台后置缺口

后台目前不是第一版阻塞项，但有一个明确差异：

- 后端 `GET /api/admin/articles` 支持 `categoryId`、`tagId`、`createdFrom`、`createdTo`、`publishFrom`、`publishTo`。
- 前端文章列表当前只提交 `titleKeyword`、`status`、`page`、`size`。

这属于后台筛选体验扩展，第一版发布后再做。不要为了补这个影响前台主线和部署。

## S3 与附件

代码和文档现状：

- `application-prod.yml` 默认 `MYBLOG_STORAGE_TYPE:S3`。
- 生产 S3 需要 `MYBLOG_STORAGE_S3_REGION`、`MYBLOG_STORAGE_S3_BUCKET`、`MYBLOG_STORAGE_S3_PUBLIC_BASE_URL`。
- S3 使用 AWS Default Credentials Provider Chain，不在配置文件写 access key / secret key。
- `/media/**` 是 LOCAL 模式历史兼容路径，S3 模式应确认未注册或不会被误用。

第一版必须实战验证：

1. 后台上传图片成功。
2. 文章封面引用 S3 URL 后前台能显示。
3. 删除/恢复附件的数据库状态与对象存储行为符合预期。
4. 公开 URL 不暴露本地路径、桶内部私密凭证或临时签名误配置。
5. Bucket Policy / CDN / CORS 与前台访问方式一致。

## 部署与服务器未知数

当前仓库没有可信服务器信息文档。上线前至少要确认：

- 服务器厂商、系统版本、CPU/内存/磁盘、是否已有 Docker。
- 域名、DNS 托管方、是否走 Cloudflare 或其它 CDN。
- 是否已有 Nginx / Caddy，当前 80/443/8080/3306 端口占用。
- MySQL 是本机、容器还是托管服务；版本、字符集、备份方式。
- Java 版本、Node/pnpm 是否需要在服务器安装。
- S3 provider、bucket、region、public base URL、凭证注入方式。
- 是否允许 GitHub Actions 通过 SSH 访问服务器。

粗方向：

1. 第一版优先单机单实例。
2. Nginx 终止 HTTPS，静态托管 blog/admin dist，`/api/**` 反代到 `127.0.0.1:8080`。
3. 后端用 systemd 跑 Spring Boot jar，环境变量从独立 env 文件注入。
4. MySQL 先用现有服务器或已配置实例，不为了第一版引入编排平台。
5. 附件走 S3，不把生产附件写本机磁盘。
6. 先手动部署跑通，再设计 CD。

CD 方向先保持轻量：

- GitHub Actions 继续做 CI。
- 发布工作流先用 `workflow_dispatch` 手动触发。
- 构建后端 jar 和前端 dist，上传到服务器 release 目录。
- 服务器端切换 `current` symlink，重启 systemd 服务，Nginx 指向静态 current。
- 部署前做数据库备份；部署后跑 `/actuator/health` 和公开页/后台登录冒烟。
- 回滚先做文件级回滚，不先引入 Kubernetes、蓝绿发布或复杂镜像仓库。

## 建议顺序

1. 文档收口：补服务器待确认清单和部署方向。
2. 前台缺口第一批：作者卡片 / 移动菜单 / 友链页 / 页脚友链。
3. 前台旧代码删除：旧 page 评论、旧第三方评论、旧 JSON API helper、旧 mock 消费者。
4. S3 生产模式本地或预生产校准。
5. 手动部署文档和冒烟清单。
6. 手动部署成功后再设计 CD。
