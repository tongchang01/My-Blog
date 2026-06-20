# V2 设计钩子决定（R1–R7）

> **本文档作用**：记录"一旦上代码就难改"的早期设计决定。所有 V2 schema 与业务代码必须遵循。
>
> **当前状态**：
> - R1–R7：**业务方向 + schema 字段 + 框架约定 + 横切基础设施全部已定**
> - 审计列规范：原则已定
> - **DDL：已冻结**（14 张表已落入 Flyway `V1__init.sql`；后续结构变更新增 V2+ 迁移，不修改已执行的 V1）
>
> ⚠️ 部分细节（前端 UI 框架 / Markdown 渲染器 / 部署 CI 细节等）后置到前端启动时定，不阻塞后端动代码。

---

## 决定速查表

| 轮次 | 主题 | 状态 |
|---|---|---|
| R1 | 用户 / 认证 | ✅ 已定 |
| R2 | 多语言数据结构 | ✅ 已定 |
| R3 | 文章内容模型 | ✅ 已定 |
| R4 | 评论 + 软删除 | ✅ 已定 |
| —  | 审计列规范（横切） | ✅ 已定 |
| R5 | schema 补漏（user_info / attachment / cover / dict / 模块边界） | ✅ 已定 |
| R6 | 框架约定（API 格式 / 分页 / 错误码 / DTO 分层 / JWT 撤销 / 统计） | ✅ 已定 |
| R7 | 横切基础设施（部署 / DB FK / 配置 / MyBatis-Plus / 邮件 / 限流 / CORS / 日志 / Knife4j / 清理任务 / 时区） | ✅ 已定 |
| R8 | schema 缺口补齐（t_user_auth / t_friend_link / t_mail_log 字段） | ✅ 已定 |

| 钩子 | 决定一句话 |
|---|---|
| #1 评论作者 | 游客模式（昵称 + 邮箱 + 站点），管理员评论挂 `author_user_id` |
| #2 文章作者 | 存 `author_id`，关联 `t_user_auth.id`，NOT NULL |
| #3 管理员账号 | 进库，不写死配置 |
| #4 账号类型 | 三态：ADMIN / DEMO / GUEST（GUEST schema 留位不开放） |
| #5 审计字段 | 业务表统一 `created_by` + `updated_by` |
| #6 文章 URL | id 主导 + slug 可读性增强：`/{lang}/posts/{id}-{slug}`；slug 不强制唯一、可改、不维护历史表 |
| #7a 文章 i18n | 标题 + 摘要三语，正文单中文 |
| #7b 分类标签 i18n | name_zh / name_ja / name_en |
| #7c 站点配置 i18n | 展示型字段三语，非展示单值（**宽表，非 KV**） |
| #8 文章状态 | 5 态：DRAFT / PUBLISHED / PRIVATE / PASSWORD / SCHEDULED |
| #9 分类 | 平铺，不做多级树；每篇文章只挂一个分类 |
| #10 标签 | 管理员预定义白名单，写文章时只能选 |
| #11 评论嵌套 | 两层（顶层 + 一级回复，再回复仍挂顶层） |
| #12 评论审核 | 先发后审 + 关键词黑名单 + **P0 安全基线**（见 R4） |
| #13 评论身份 | 昵称必填 + 邮箱必填 + 网站选填 |
| #14 多语言 URL | 路径前缀 `/zh/...` `/ja/...` `/en/...` |
| #15 翻译存储 | UI 文案前端 JSON，业务内容后端 DB 三语副本 |
| #16 默认语言 | `navigator.language` 自动跳 + localStorage 记忆 |
| #17 软删除 | 业务表统一软删，物理删走运维 |
| #18 字体策略 | CSS `:lang()` 分层 + Noto Sans 全家桶 |
| #19 翻译工作流 | DeepL 打底 + Qiita 术语 + 人工校对 + 术语表 |
| 鉴权 | `/api/public/**` permitAll；`/api/admin/**` 读 ADMIN+DEMO，写仅 ADMIN |
| DEMO 视角 | **DEMO 是公开演示账号 + 后台只读**；后台 GET 保持可读，由 application 层裁剪未公开正文和评论审计字段 |

---

## R1 用户 / 认证

### #1 评论作者怎么存

**游客模式**：评论无需注册。

- `t_comment` 字段：
  - `author_user_id BIGINT NULL` —— 管理员评论时非空，游客评论时 NULL
  - `author_nickname VARCHAR(64) NOT NULL`
  - `author_email VARCHAR(128) NOT NULL`（不公开展示）
  - `author_site VARCHAR(255) NULL`（选填）
  - `author_ip VARCHAR(45)`（审计）
- 邮箱用途：① Gravatar 头像哈希 ② 被 @ 时邮件通知
- 管理员评论时 `author_user_id` 非空 → 前端渲染"博主"badge

### #2 文章作者怎么存

**存 `author_id`**。

- `t_article.author_id BIGINT NOT NULL`，关联 `t_user_auth.id`
- 当前只有一个管理员写，但留字段为"客座作者"零成本铺路

### #3 管理员账号放哪

**进库**（`t_user_auth` + `t_user_info`），不写死配置。

- 改密码不重启
- 能挂"最后登录时间 / 登录失败次数"等审计字段

### #4 账号类型：三态枚举

`t_user_auth.type TINYINT NOT NULL`：

| 值 | 名 | 用途 | V2 初期 |
|---|---|---|---|
| 1 | `ADMIN` | 管理员（全权） | ✅ 激活 |
| 2 | `DEMO` | 只读演示账号（作品集求职用） | ✅ 激活 |
| 3 | `GUEST` | 长期访客账号（未来积分/收藏） | ⏳ schema 留位，接口不开放 |

### #5 审计字段

业务表统一加 `created_by BIGINT` + `updated_by BIGINT`，值是 `t_user_auth.id`。详见底部"审计列规范"。

### #5' t_user_auth 关键字段补充

R6 C1 JWT 撤销设计依赖：

- `token_version INT NOT NULL DEFAULT 0` —— 改密 / 主动登出 / 强制下线 时 +1，使所有未过期 access token 失效
- 其他基础字段（username / password_hash / type / last_login_at / login_fail_count / locked_until 等）DDL 阶段定

### R1 衍生设计

- **Spring Security 角色**：`ROLE_ADMIN` / `ROLE_DEMO`（GUEST 暂不映射）
- **API 鉴权边界（三段式）**：

  | 路径 | 鉴权 | 说明 |
  |---|---|---|
  | `/api/public/**` | `permitAll()` | 前台游客读公开内容（文章列表/详情/分类/标签/评论读取/友链等） |
  | `/api/auth/**` | `permitAll()` | 登录 / 刷新 / 登出 |
  | `/api/admin/** GET` | `hasAnyRole('ADMIN','DEMO')`；application 层按角色裁剪敏感字段 | 后台读 |
  | `/api/admin/** POST/PUT/DELETE/PATCH` | `hasRole('ADMIN')` | 后台写 |

- **写接口红线**：DEMO 视为"普通访客 + 后台只读"，写操作不能只靠前端隐藏按钮，后端注解级强制拒绝（`@PreAuthorize("hasRole('ADMIN')")` 是写接口唯一闸门，DEMO 调到必返 403）
- **敏感读红线**：公开 DEMO 账号不得获得未公开文章正文或评论审计字段。允许 DEMO 读取的后台 GET 必须由 application 层返回字段级裁剪结果，Controller、Web mapping 和 Repository 不得各自重复判断角色。
- **DEMO 权限原则**：DEMO 是公开演示账号，后台 GET 端点保持可读，但响应必须按角色裁剪：
  - PUBLISHED 文章正文可见；DRAFT、PRIVATE、PASSWORD、SCHEDULED 的 `body` 返回 `null`
  - 评论管理列表可见；`authorEmail`、`authorIp`、`authorUserAgent` 返回 `null`
  - ADMIN 保持完整读取；同一端点不为 DEMO 复制 Controller、Repository 或 SQL
  - 用户管理 / 站点配置如新增敏感字段，必须单独定义字段级规则
- **前端**：DEMO 登录时禁用/隐藏写按钮 + 顶部 banner "Demo 只读模式"（防呆）
- **README**：明示 demo 凭证（如 `demo / demo123`），作为作品集演示入口
- **关于"RBAC"叙事**：README / Showcase 不要包装成"完整 RBAC"。准确说法："**基于账号类型的最小权限控制**"。V2 不实现动态 RBAC；未来需要多角色时，再拆 `t_role / t_user_role / t_permission`。

---

## R2 多语言数据结构

**前置约束**：UI 三语（中/日/英），文章正文只用中文写。
**核心场景**：日本 SES 求职作品集，日本主管是首要访客。

### #7a 文章标题 + 摘要三语

- `t_article` 加：`title_zh / title_ja / title_en` + `summary_zh / summary_ja / summary_en`
- 正文字段保持单列（只中文）
- 日本访客首页列表看到日语标题 + 日语摘要 → 能判断"这篇讲什么"再决定是否点开

### #7b 分类 / 标签名三语

- `t_category` 和 `t_tag` 加：`name_zh / name_ja / name_en`
- **`name_zh` 必填**；`name_ja` / `name_en` 可空，前台展示时缺失自动 fallback 到 `name_zh`（与文章 i18n 规则一致）
- 表很小（撑死 ~20 分类 + ~50 标签），加几列零负担

### #7c 站点配置三语（仅展示型字段）

- `t_site_config` **采用宽表**（不是 KV 表）。理由：配置项数量少（<20），强类型优先，KV 表对个人博客是过度设计
- 展示型字段（站点标题、副标题、关于我 Markdown）每条带三语：
  - `site_title_zh / site_title_ja / site_title_en`
  - `site_subtitle_zh / site_subtitle_ja / site_subtitle_en`
  - `about_md_zh / about_md_ja / about_md_en`
- 非展示字段保持单值：`logo_url / favicon_url / icp_no / spotify_playlist_id / ...`
  - `spotify_playlist_id`：V1 自建音乐播放器替换方案，前端读此值渲染 Spotify 官方 iframe（见 `feature-inventory.md` ⑬）
- 表本身只有一行数据（id=1），后台编辑即更新

### #14 URL 策略：路径前缀

- 路径形式：`/zh/posts/xxx` / `/ja/posts/xxx` / `/en/posts/xxx`
- Vue Router 用 `path: '/:lang(zh|ja|en)/...'` 统一处理
- 后端响应 SEO 元数据时配合输出 `<link rel="alternate" hreflang="...">`

### #15 翻译存储：混合

| 类型 | 存哪 |
|---|---|
| UI 文案（按钮/菜单/label） | 前端 `locales/{zh,ja,en}.json`，vue-i18n 构建期打包 |
| 业务内容（站点标题、关于我、分类标签） | 后端 DB 三语副本 |

切换语言时：UI 切 vue-i18n locale；业务内容由各接口返回对应语言字段（按请求路径前缀判断）。

### #16 默认语言：自动跳 + 记忆

- 首次访问 `/` → 前端读 `navigator.language` → 跳到对应 `/{lang}/`
- 用户手动切换 → 写 `localStorage`，下次直接跳记住的语言
- 不做 IP 归属地猜测（许可证 + 数据更新成本不值）

### #18 字体策略：CSS `:lang()` 分层

- HTML 分层 `lang`：`<html lang="ja">` 但 `<article lang="zh-CN">`
- CSS 用 `:lang(zh-CN)` / `:lang(ja)` / `:lang(en)` 分别配字体栈
- 优先用 Noto Sans SC / Noto Sans JP / Noto Sans（Google Fonts 免费，子集化）
- 代码块独立字体栈：JetBrains Mono / Fira Code
- README 写明 → Showcase 点

### #19 翻译工作流

- UI 文案 → DeepL 打底，对照 Qiita / Zenn / note 调整术语
- 关键文案（首页副标题、关于我、错误提示）→ 必须人工校对
- 分类 / 标签译名 → 优先抄 Qiita 现成译法保证地道
- 新建 `frontend-user/i18n-glossary.md` 术语表（中/日/英对照）

### R2 衍生设计

- **数据层**：文章 title + summary 三语，正文单中文；分类/标签/站点配置展示字段三语
- **请求层**：路径前缀 `/{lang}/` 决定后端返回哪个语言字段
- **前端层**：vue-i18n 管 UI / 业务内容直接渲染接口返回值 / 全站字体走 `:lang()`
- **流程层**：i18n 术语表 + DeepL+校对流水线
- **关联记录**：`pitfalls.md` P-009（V1 i18n CJK 字体混排 + 机翻教训）

### R2 产品定位声明（必须在三处明示，不藏）

> 本博客主要以中文写作。日语 / 英语标题与摘要用于帮助非中文访客快速判断文章主题，**不承诺全文翻译**。重点文章未来可单独追加日语版正文。

展示位置：
1. 首页简介区
2. About 页
3. 文章详情页（非中文 UI 进入中文文章时，标题附近的小提示）

理由：作品集场景下坦诚比假装"全三语博客"更专业，避免日本访客点开正文产生落差。

---

## R3 文章内容模型

### #6 文章 URL：id 主导 + slug 可读性增强

V2 不采用纯 slug URL，不维护 slug 历史表，也不做 slug 全局永久占用。**理由**：本项目是作品集，不是 SEO 流量站；纯 slug 方案带来的 slug 唯一性 / 软删占用 / 历史 301 / 跨表命名空间等一连串复杂度，对当前阶段是过度设计。

**公开 URL 形态**：

```
/{lang}/posts/{id}
/{lang}/posts/{id}-{slug}
```

示例：
- `/zh/posts/123`
- `/zh/posts/123-spring-security-jwt-pitfalls`
- `/ja/posts/123-spring-security-jwt-pitfalls`

**字段定位**：

| 字段 | 角色 |
|---|---|
| `id` | 文章**唯一定位键**，后端查询只依赖 id |
| `slug` | URL 可读性字段，**不承担全局唯一身份**，允许为空，允许修改 |

**表设计**：

- `t_article.slug VARCHAR(160) NULL`
- 不强制唯一（不加 UNIQUE 约束）
- **不维护** `t_article_slug_history`
- **不引入** `t_slug_registry`

**slug 输入规则**：

- 只允许 `a-z 0-9 -`
- 长度上限 160
- 可为空
- 可任意修改

**访问 / canonical 规则**：

1. `/posts/{id}-{slug}` 先解析 id 查文章；若 URL 中 slug **与当前 slug 一致** → 200；**不一致** → 301 到 canonical
2. `/posts/{id}`（无 slug 段）：文章**有 slug** → 301 到 `/{lang}/posts/{id}-{currentSlug}`；文章**无 slug** → 200
3. canonical URL：
   - 有 slug → `/{lang}/posts/{id}-{currentSlug}`
   - 无 slug → `/{lang}/posts/{id}`
4. 同一篇文章公开只有一个 200 地址，避免重复内容
5. 改 slug 不破坏旧 URL 访问（id 仍能命中）；浏览器/爬虫到 301 后会更新

**基础 SEO（保留）**：

- 每页 `<title>` / `meta description`
- `<link rel="canonical">`
- Open Graph / Twitter Card
- 语义化 HTML / 合理 h1/h2
- `sitemap.xml` / `robots.txt`
- `<link rel="alternate" hreflang>`（配合多语言路径前缀）

**V2 SEO 范围边界（重要）**：

- 前台是 Vue SPA，V2 **不上 SSR / 预渲染**
- 站点级 + SPA 内基础 meta 全做（浏览器内 title / description / canonical 由 vue-router 动态注入正常）
- **文章级 Open Graph / Twitter Card 不保证**：很多分享平台爬虫不执行 JS，抓的是初始 HTML，分享卡片可能拿不到正确数据
- 如需保证文章分享卡片，后置到 V3 或部署阶段评估（vite-ssg 预渲染 / Nuxt SSR / 部署层中间件注入 meta）

**sitemap.xml 收录规则**：

- 收录：`status = PUBLISHED AND deleted = 0` 的文章；分类页；标签页；首页；About 页
- 可选收录：PASSWORD 文章入口页（标题/摘要本来公开，正文受密码保护）；当前阶段**默认收录**
- 不收录：DRAFT / PRIVATE / SCHEDULED（未到 publish_at）/ deleted = 1
- 每篇文章发 3 条 URL（zh/ja/en），用 `<xhtml:link rel="alternate" hreflang>` 互链

**robots.txt 内容**（静态文件 / 不动态生成）：

```
User-agent: *
Disallow: /api/
Disallow: /admin/
Disallow: /_static/
Allow: /

Sitemap: https://blog.example.com/sitemap.xml
```

- 禁爬 API、后台路径
- 允许爬前台所有页面（hreflang 会引导多语言）

**不做的"高级 SEO"**：

- slug history / registry
- 多级 301 迁移策略
- 完整 hreflang 工程
- Schema.org 结构化数据
- 搜索关键词策略 / 内容增长策略

### #8 文章状态：5 态

`t_article.status TINYINT NOT NULL`：

| 值 | 名 | 含义 | 谁能看到 |
|---|---|---|---|
| 1 | DRAFT | 草稿 | 仅作者后台 |
| 2 | PUBLISHED | 已发布 | 所有人 |
| 3 | PRIVATE | 私密 | 仅登录 ADMIN |
| 4 | PASSWORD | 密码保护 | 列表显示标题/摘要，详情需输密码 |
| 5 | SCHEDULED | 定时发布 | 到 `publish_at` 自动转 PUBLISHED |

配套字段：
- `t_article.access_password VARCHAR(255) NULL`（BCrypt 哈希，PASSWORD 用）
- `t_article.publish_at DATETIME NULL`（公开发布时间；PUBLISHED / PASSWORD 为首次公开时间，SCHEDULED 为预定公开时间）

**PASSWORD 校验流程**（关键）：

> 以下为上线后目标方案；首版不提供 `/unlock`、article access token 或 PASSWORD 评论授权，当前边界以 BR-204A 和 API 契约为准。

1. 用户在前端输入明文密码
2. 前端通过 HTTPS 把明文密码传到后端
3. 后端用 `BCrypt.matches(rawPassword, storedHash)` 校验
4. 通过则签发 **article access token**（见 R6 PASSWORD 文章会话策略）
5. **失败限流**：同 IP + 同文章连续失败 5 次后冷却 10 分钟

> 注意：前端**不要**把密码哈希后传给后端。HTTPS 已经保护传输；BCrypt 必须用明文校验。

**SCHEDULED 实现**：

- Spring `@Scheduled(cron = "0 * * * * *")` 每分钟扫一次
- 时区统一 `Asia/Tokyo`（应用层 + 数据库 DATETIME 字段语义都按 JST）
- 部署时强制 MySQL session `time_zone = '+09:00'`
- 单实例部署，不引分布式锁
- 必须**幂等单条 UPDATE**，且 `now` 由应用层生成 JST 时间作为参数传入（避免 DB `NOW()` 受 session 时区影响）：

```sql
UPDATE t_article
SET status = 2, updated_at = #{now}, updated_by = NULL
WHERE status = 5
  AND publish_at <= #{now}
  AND deleted = 0;
```

> 系统任务触发的更新统一 `updated_by = NULL`，与既有"无登录态填 NULL"规则一致；任务执行记录走业务日志。

### #8' 文章状态字段约束矩阵（application 层强制）

后端保存时按状态校验，违反返 400：

| 状态 | title_zh / summary_zh | title_ja/en / summary_ja/en | body | category_id | slug | access_password | publish_at |
|---|---|---|---|---|---|---|---|
| DRAFT | 可空 | 可空 | 可空 | 可空 | 可空 | 忽略 | 忽略 |
| PUBLISHED | **必填** | 可空（缺失 fallback 到 zh） | 必填 | 必填 | 可空 | 忽略并清空 | **必填；为空时保存为当前 JST 时间** |
| PRIVATE | **必填** | 可空（fallback） | 必填 | 必填 | 可空 | 忽略并清空 | 忽略并清空 |
| PASSWORD | **必填** | 可空（fallback） | 必填 | 必填 | 可空 | **必填**（BCrypt 哈希后存） | **必填；为空时保存为当前 JST 时间** |
| SCHEDULED | **必填** | 可空（fallback） | 必填 | 必填 | 可空 | 忽略并清空 | **必填且 > 当前 JST 时间** |

**i18n fallback 规则**：

- 前端按访问路径 `/ja/` 或 `/en/` 取对应字段，**取不到则 fallback 到 zh**
- 非中文页面 fallback 到中文标题/摘要时，前端在标题附近显示小提示："本文主要以中文写作"（与 R2 产品定位声明一致）
- 重点文章作者可手动补三语，普通文章中文一把过即可，降低发布门槛

`publish_at` 不参与 PUBLISHED / PASSWORD 的可见性判断，但用于归档、Sitemap、RSS、后台"本月发布"等统计口径。SCHEDULED 到点转 PUBLISHED 后保留原 `publish_at`，不改成任务执行时间。

特别防止：PASSWORD 没密码 / SCHEDULED 没 publish_at / PUBLISHED 中文标题或正文为空 / publish_at 已过期还存 SCHEDULED。

### #9 分类：平铺

- `t_category` 字段：`id / name_zh / name_ja / name_en / slug / sort_order` + 审计列
- **不加** `parent_id`
- 粗粒度（"后端/前端/职场/生活" 4-5 个），细分用标签
- **每篇文章只能挂一个分类**（`t_article.category_id BIGINT NOT NULL`），跨主题表达走标签，不引入多分类
- `t_category.slug` **UNIQUE**（分类是小表+白名单管理，唯一性保证后台数据质量；软删后不复用，新建时若 slug 与已删除项冲突，后台提示"已存在同 slug 的分类，包括已删除项"）

### #10 标签：白名单

- 后台单独"标签管理"页面，标签建好才能用
- 写文章时只能从已有标签下拉选，不能现场新建
- `t_tag` 字段：`id / name_zh / name_ja / name_en / slug` + 审计列
- 多对多通过 `t_article_tag(article_id, tag_id)` 关联
- `t_tag.slug` **UNIQUE**（同分类规则：标签白名单管理，唯一性杜绝同义泛滥；软删后不复用）
- **为什么**：杜绝同义标签泛滥（Spring Boot / spring-boot / SpringBoot）；三语场景下强制建标签时填三语，避免漏翻

### R3 衍生设计

- **后端**：
  - `ArticleQueryController.getById(id)` 直接以 id 为唯一定位键查询
  - 路径形如 `/posts/{id}-{slug}` 时，解析出 id 查文章；若 URL 中 slug 与当前 `t_article.slug` 不一致，301 到 canonical URL
  - canonical URL：有 slug → `/{lang}/posts/{id}-{currentSlug}`；无 slug → `/{lang}/posts/{id}`
  - `ArticleScheduledPublisher` 每分钟扫定时发布（单条幂等 UPDATE，时区 Asia/Tokyo，`now` 应用层传参）
  - PASSWORD 详情接口接收明文密码，`BCrypt.matches` 校验 + 失败限流（5 次/10 分钟冷却）
- **前端**:
  - 列表/卡片对 PASSWORD 文章显示"🔒"标识
  - 详情页 PASSWORD 先渲染密码输入框，输对再请求正文（明文 POST，HTTPS 保护）
  - PRIVATE 在公开列表中后端层完全隐藏
- **后台编辑器**:
  - "URL 别名（slug）"输入框，校验只允许 `a-z 0-9 -`，长度 ≤160，可为空，不校验唯一
  - 状态下拉 5 选 1：选 PASSWORD 时显示密码输入框，选 SCHEDULED 时显示时间选择器（默认 Asia/Tokyo）

---

## R4 评论 + 软删除

### #10.5 评论挂载目标：`target_type + target_id` 二元定位

> **背景**：feature-inventory ④ 决定留言板复用 `t_comment`。为避免 `article_id=0` 这类 magic value（0 不是合法的 ASSIGN_ID 雪花值，破坏 id 语义），引入显式 target 定位。

- `t_comment.target_type TINYINT NOT NULL` —— `1=ARTICLE / 2=GUESTBOOK`（未来可扩展 `3=ABOUT` 等）
- `t_comment.target_id BIGINT NOT NULL` —— ARTICLE 时是 `t_article.id`；GUESTBOOK 时固定 `0`
- **不再单独保留** `article_id` 列
- 索引：`(target_type, target_id, deleted, audit_status, created_at)` 覆盖所有列表查询
- **application 层强制**：
  - ARTICLE：`target_id` 必须命中 `t_article.id` 且文章状态允许评论
  - GUESTBOOK：`target_id` 固定 `0`，service 层写入时强制赋值
  - 跨目标回复禁止：`reply_to_comment_id` 所属 `(target_type, target_id)` 必须等于当前评论的 `(target_type, target_id)`（替代 R4 #11 原"跨文章回复禁止"规则）
- `t_article.comment_count` 语义同步收窄为"`target_type=ARTICLE AND target_id=本文 AND deleted=0 AND audit_status=PASS` 的评论数"
- 留言板**不做** `guestbook_count` 冗余列（留言板就一页，count(*) 直查即可）

### #11 评论嵌套：两层

- `t_comment.parent_id BIGINT NULL` —— 指向**顶层评论 id**（不是上一条），渲染层挂楼用
- `t_comment.reply_to_comment_id BIGINT NULL` —— 指向**本次实际回复的目标评论**，通知用（必须，游客无 user_id 时是唯一定位依据）
- `t_comment.reply_to_user_id BIGINT NULL` —— 系统用户辅助字段，仅当被回复者是 ADMIN/DEMO 时有值
- `t_comment.reply_to_nickname VARCHAR(64) NULL` —— 展示快照（被回复人后续改名不影响历史显示）
- 回复别人的回复 → `parent_id` 仍挂同一个顶层评论；`reply_to_comment_id` 指向直接被回复的那条
- **通知定位**：基于 `reply_to_comment_id` 找目标评论 → 取其 `author_email`（游客）或登录用户邮箱（系统用户）
- **回复业务约束**（application 层强制）：
  - 顶层评论：`parent_id = NULL`，`reply_to_comment_id = NULL`
  - 回复顶层评论：`parent_id = 被回复评论 id`，`reply_to_comment_id = 被回复评论 id`
  - 回复子评论：`parent_id = 被回复评论的 parent_id`（保持挂同一顶层），`reply_to_comment_id = 被回复评论 id`
  - 不允许回复 `deleted=1` / `audit_status IN (HIDDEN, PENDING)` 的评论
  - 不允许跨文章回复：`reply_to_comment_id` 所属 `article_id` 必须等于当前 `article_id`
- **为什么**：无限嵌套在窄屏左缩进顶死；两层是 GitHub Issues / 知乎 / V2EX 通行做法

### #12 评论审核：先发后审 + 关键词黑名单

- `t_comment.audit_status TINYINT NOT NULL`：1=PASS / 2=PENDING / 3=HIDDEN
- 提交流程：用户提交 → 关键词扫描 → 命中则入库为 PENDING，未命中则入库为 PASS
- 管理员评论直接 PASS（不过黑名单）
- 管理员可随时手动改 HIDDEN
- 后台"评论管理"3 个 tab：待审 / 通过 / 隐藏

**可见性矩阵**：

| 状态 | 前台公开列表 | 后台管理 |
|---|---|---|
| PASS | 显示 | 显示 |
| PENDING | **不显示** | 仅 ADMIN 可见（"待审" tab） |
| HIDDEN | 不显示 | 仅 ADMIN 可见（"隐藏" tab） |

"先发后审"语义精确化：**未命中关键词的评论立即可见；命中关键词的评论入 PENDING 不公开，需 ADMIN 审核通过转 PASS 后才公开。**

### #12-P0 评论安全基线（动代码必须实现）

> 评论是匿名写入口。匿名写接口没有限流 = 上线第一天被脚本刷库。这不是优化，是基础防线。

| 能力 | 要求 |
|---|---|
| **Markdown 清洗 + 存储** | 评论原文按 Markdown 子集解析为 HTML；解析时**禁用原始 HTML**；解析后的 HTML 再经 Sanitizer（Jsoup / OWASP HTML Sanitizer）白名单清洗。`t_comment` 同时保存 `content_md TEXT`（用户提交原文）+ `content_html TEXT`（清洗后安全 HTML）；**前台只渲染 `content_html`**，不直接渲染 `content_md` |
| **字段长度限制** | `nickname` ≤ 64 / `email` ≤ 128 / `site` ≤ 255 / **`content_md` ≤ 5000**（用户输入边界），前后端都校验；`content_html` 不强限（由 `content_md` 解析+清洗产生，正常情况下不会显著超出） |
| **author_site 协议白名单** | 只允许 `http://` / `https://`；禁止 `javascript:` / `data:` / `vbscript:` 等；前端展示锚标签必须加 `rel="nofollow noopener noreferrer"` |
| **同 IP 频率限制** | 同 IP 1 分钟内最多 5 条评论，超出 429 |
| **同 IP+同文章重复内容拒绝** | 同 IP 在同一文章下 5 分钟内提交相同 content（hash 比较）→ 直接拒绝 |

可后置到 V2 后期增强：

- Honeypot（前端隐藏字段，机器人填写则拒绝）
- 外链数量超阈值（如 ≥3）自动进 PENDING
- 管理员可配置黑名单（当前阶段走配置文件）
- 更复杂的语义反垃圾

### #13 评论身份字段：昵称 + 邮箱必填，网站选填

- `author_nickname VARCHAR(64) NOT NULL`
- `author_email VARCHAR(128) NOT NULL`（后端格式校验，前端不展示）
- `author_site VARCHAR(255) NULL`
- **为什么**：邮箱选填等于 Gravatar 和通知白做

### #13' PASSWORD 文章评论可见性

- PASSWORD 文章的**正文 + 评论列表 + 评论提交入口**都需要密码校验通过后才能访问
- 公开列表只显示标题 / 摘要 / 🔒 标识 / 评论数量；**不展示评论内容**
- 防止评论泄漏正文上下文

### #17 软删除策略：业务表统一软删

- 所有业务表带软删三件套：`deleted` + `deleted_at` + `deleted_by`（详见审计列规范）
- MyBatis-Plus `@TableLogic` 自动过滤 `deleted=1`
- 后台"回收站"页面**只支持查看 / 恢复**，不提供彻底删除按钮
- 评论软删后保留楼层占位（显示"该评论已删除"）
- 物理删除走运维手动 SQL，不开后台入口

### R4 衍生设计

- **评论数据流**：用户提交 → 关键词扫描 → PASS 或 PENDING → 入库
- **通知数据流**（精确化）：
  - 评论入库为 PASS 时 → 异步发邮件给被回复者（基于 `reply_to_comment_id` 定位目标邮箱）
  - 评论入库为 PENDING 时 → **不发通知**；后续 ADMIN 审核通过转 PASS 时再触发通知
  - HIDDEN / deleted 评论永不触发通知
  - 异步用 Spring `@Async`，不引 MQ
- **回收站**：所有业务表后台统一"回收站"入口，按 `deleted_at desc` 排；**只支持查看 / 恢复，不提供后台彻底删除按钮**。物理删除仅限运维手动 SQL，执行前必须确认关联数据（评论、附件、标签关联等）
- **评论审计字段**：`t_comment` 带 `author_ip VARCHAR(45)` + `author_user_agent VARCHAR(512)`，审计用不展示
- **评论审计字段权限**：评论管理页对 ADMIN / DEMO 均可读；ADMIN 获得完整 `author_email` / `author_ip` / `author_user_agent`，DEMO 对应响应字段为 `null`
- **评论数量缓存**：`t_article.comment_count INT NOT NULL DEFAULT 0` 冗余列；评论入库为 PASS / 审核 PENDING→PASS / 软删 / 隐藏 / 恢复 时，service 层在同事务内 +1 或 -1（不靠触发器）；只统计 `deleted=0 AND audit_status=PASS` 的评论；列表/卡片直接读这列，避免 N 次 count(*)

---

## R5 schema 补漏

### A1 `t_user_info` 字段

```sql
t_user_info:
  user_id        BIGINT PRIMARY KEY          -- 同时逻辑引用 t_user_auth.id，1:1（无独立 AUTO_INCREMENT id，不建 DB FOREIGN KEY，ADR-0017）
  nickname       VARCHAR(64)  NOT NULL
  avatar_url     VARCHAR(255) NULL
  bio_zh         TEXT         NULL
  bio_ja         TEXT         NULL
  bio_en         TEXT         NULL
  location       VARCHAR(64)  NULL           -- "Tokyo" 之类
  website        VARCHAR(255) NULL
  email_public   VARCHAR(128) NULL           -- 可选公开邮箱
  -- 社交链接（拆列）
  github_url     VARCHAR(255) NULL
  twitter_url    VARCHAR(255) NULL
  linkedin_url   VARCHAR(255) NULL
  zhihu_url      VARCHAR(255) NULL
  qiita_url      VARCHAR(255) NULL
  juejin_url     VARCHAR(255) NULL
  -- 审计 7 列（无独立 id，其余继承）：created_at / created_by / updated_at / updated_by / deleted / deleted_at / deleted_by
```

- **不继承 `BaseEntity`**：t_user_info 与 t_user_auth 是 1:1 强绑定，主键即逻辑引用列（不建 DB FOREIGN KEY，ADR-0017），无需独立 id（属于"审计列规范"例外表，见底部例外清单）
- 实现层：单独继承 `AuditOnlyBase`（只带 7 列审计字段，不带 id），由 MyBatis-Plus 字段填充器统一处理

- bio 三语；缺失语言 fallback 到中文，与文章 i18n 规则一致
- 社交链接拆列：强类型，前端各平台图标独立渲染；个人博客平台不会频繁变
- 未来加新平台（如 Mastodon）→ 加列，schema 变更可控

### A3 `t_attachment` 表

```sql
t_attachment:
  id                BIGINT PK             -- ASSIGN_ID（MyBatis-Plus 雪花），不带 AUTO_INCREMENT
  storage_type      VARCHAR(16)  NOT NULL    -- LOCAL / S3 / OSS（V2 起点 LOCAL）
  bucket            VARCHAR(64)  NULL        -- LOCAL 时为本地根目录别名
  object_key        VARCHAR(255) NOT NULL    -- 存储后端的对象键 / 相对路径
  public_url        VARCHAR(512) NOT NULL    -- 对外可访问 URL（CDN/直链）
  content_type      VARCHAR(64)  NOT NULL    -- image/png / image/webp 等
  file_size         BIGINT       NOT NULL    -- 字节
  width             INT          NULL        -- 仅图片
  height            INT          NULL        -- 仅图片
  original_filename VARCHAR(255) NULL
  hash_sha256       VARCHAR(64)  NOT NULL    -- 去重 + 完整性校验
  -- 审计 8 列
  UNIQUE KEY uk_hash (hash_sha256)
  INDEX idx_storage_key (storage_type, object_key)
```

- `storage_type / object_key / public_url` 三件套是抽象层；切 OSS 不动 schema 不动业务代码，只换 `StorageService` 实现
- `hash_sha256` 用于上传去重（同图重复上传命中已有记录），DB 层用 `UNIQUE KEY uk_hash` 防并发重复写
- 不引用计数列；删除前由 application 层只扫描**结构化引用**（`t_article.cover_attachment_id`）；正文 Markdown / 评论 Markdown 中的图片 URL 引用走弱审计（见下"删除策略"）

**上传去重命中策略**：

- 上传时先算 `sha256(file)` → 查 `t_attachment` 同 hash 的记录（不按 `deleted` 过滤）
- 命中 `deleted=0` → **不写新行**，直接返回已有 attachment 的 `id` / `public_url`（前端无感）
- 命中 `deleted=1` → 恢复该行（`deleted=0`，清空 `deleted_at/deleted_by`），直接返回原 `id` / `public_url`，避免软删保留期内重复上传触发唯一键冲突
- 未命中 → 写新行 + 实际写存储后端

**删除策略（两段式 + 弱审计，A3' 方案）**：

| 阶段 | 触发 | 行为 |
|---|---|---|
| 软删 | 后台"删除附件"按钮 | 只查**结构化引用**：若 `t_article.cover_attachment_id` 命中 → 拒绝软删（提示"被 N 篇文章用作封面"）；正文 Markdown 引用**不挡删**。否则 `deleted=1` + `deleted_at/by`，**物理文件保留** |
| 物理清理 | `AttachmentCleanupJob` 每周一 5:00 JST | 扫 `deleted=1 AND deleted_at < now-90d` 的记录 → 再查 cover 引用兜底确认 → 物理删文件 + 物理删行 |
| 弱审计 | `AttachmentReferenceAuditJob` 每周日 6:00 JST | 全表扫 `t_article.body` 和 `t_comment.content_md` 中的 `attachment` URL，对照 `t_attachment` 表，生成"失效引用报告"（哪些文章引用了已 deleted=1 的 attachment）+ "孤儿报告"（哪些 attachment 没有任何引用），写入运维日志，不自动处理 |

**为什么不强校验正文 Markdown 引用**：
- 个人博客文章数量有限，正文里的图片引用总数可控
- `LIKE '%url%'` 在 MEDIUMTEXT 上无索引可用，文章一多就慢
- 删除附件本就低频，"秒删 + 后置审计"比"经常因引用挡删 + 强一致"对管理员更友好
- 短期内若有旧文章出现图片 404，访客容忍度高，管理员看 audit 报告再决定（手动改文章 / 上传新图 / 容忍）

### A3 文章封面

`t_article.cover_attachment_id BIGINT NULL` 逻辑引用 `t_attachment.id`（**不建 DB FOREIGN KEY**，ADR-0017 / R-012）。

- 上传流程：后台编辑器"封面"区域 → 调附件库上传 → 选中 → 写 `cover_attachment_id`
- 删除 attachment 前查此列引用，命中则禁止软删

### A3' Markdown 正文图片

- 编辑器"插入图片"按钮 → 走附件上传 → 自动插入 `![](attachment.public_url)`
- **外链 URL 允许直接粘贴**（不入库），用户自负责任（图片源站宕机风险）
- 删除 attachment 时**只扫描结构化引用（cover_attachment_id）**；正文 Markdown 中的 attachment URL 引用走弱审计（每周 `AttachmentReferenceAuditJob` 报告）

### A5 字典表 `t_dict`：**不引**

- 所有 enum（用户类型、文章状态、评论审核状态、附件 storage_type 等）走 Java enum class
- 前端 enum 文案走 vue-i18n 资源文件（与 UI 文案同管）
- 理由：字典表价值在"运营人员动态加字典项"，个人博客没这需求；引入只会多一张表 + 一套 CRUD + 启动加载缓存的工程负担

### B1 模块边界

V2 维持 6 模块：

| 模块 | 职责 |
|---|---|
| `identity` | t_user_auth / t_user_info / 登录 / JWT |
| `content` | t_article / t_category / t_tag / t_article_tag（内部分子包 article / category / tag） |
| `comment` | t_comment |
| `system` | t_site_config / t_attachment / t_friend_link（**不含 V1 的字典/菜单/操作日志**，详见 ADR-0004 R5 修订） |
| `stats` | t_page_view_daily 等统计聚合 |
| `common-infra` | 基础设施：异常、响应包装、审计 handler、storage 抽象、限流等 |

- content 不拆 article/category/tag 独立模块；分类标签与文章强耦合，独立成模块只会增加跨模块调用成本
- `t_attachment` 归属 `system` 模块（资源型基础设施，content/comment 通过 system 暴露的 application 接口使用）

### R5 衍生

- **新增表清单**：`t_user_info`、`t_attachment`
- **t_article 新增列**：`cover_attachment_id BIGINT NULL`（逻辑引用 t_attachment.id，不建 DB FOREIGN KEY，ADR-0017 / R-012）
- **存储抽象**：`common-infra` 提供 `StorageService` 接口，V2 实现 `LocalStorageService`（写到本地 `${myblog.storage.local.root}` 目录），未来加 `S3StorageService` / `OssStorageService` 不改业务代码
- **附件上传接口**：`/api/admin/attachments POST` 仅 ADMIN（DEMO 只读上传记录列表，不能上传）

---

## R6 框架约定

### B2 DTO / VO / Entity 分层

| 类型 | 后缀 | 位置 | 用途 |
|---|---|---|---|
| Entity | `*Entity` | infrastructure / dal | 与 DB 表 1:1，MyBatis-Plus 映射 |
| Request | `*Request` | interfaces (controller) | 接收前端入参（带 Bean Validation 注解） |
| Command | `*Command` | application | Service 写操作入参 |
| Query | `*Query` | application | Service 读操作入参 |
| VO | `*VO` | interfaces | 返回给前端 |

- 层间转换走 **MapStruct**（编译期生成，性能好且类型安全）
- **Entity 永不出 application 层**，Controller 直接拿到的必须是 VO
- DTO 之间不互相继承，扁平定义

### B3 API 响应包装格式

沿用 `rules/api-response.md` 既定方案：

```json
{
  "code": "00000",
  "msg": "success",
  "data": { ... }
}
```

- **HTTP 状态码与业务结果同步**（R-007）：200 成功 / 400 参数错 / 401 未登录 / 403 无权限 / 404 不存在 / 409 冲突 / 500 系统错
- `code` 字段同时承载业务子码（见 B5），便于前端分支处理
- 异常统一由 `GlobalExceptionHandler` 兜底（R-006）

### B4 分页规约

入参：

```
?page=1&size=10           // 1-based，与 MyBatis-Plus IPage 一致
```

返回结构：

```json
{
  "records": [ ... ],
  "total": 123,
  "page": 1,
  "size": 10,
  "pages": 13
}
```

- 默认 `size=10`，上限 `size=100`（超出 400）
- `page < 1` 或 `size < 1` 一律 400

### B5 错误码空间

**类型**：`String`（固定 5 位字符串，前导零保留；不用 int 是为了支持「保留段」如 `99XXX` 不被前导零吃掉，前端 `switch` 也更明确）

5 位 `MMSSS`：

| MM | 模块 |
|---|---|
| 00 | 通用成功（`00000` = success） |
| 10 | identity |
| 20 | content |
| 30 | comment |
| 40 | system |
| 50 | stats |
| 90 | common-infra（参数校验、限流、系统错等） |

示例：

| code | 含义 |
|---|---|
| `00000` | success |
| `10001` | 用户名或密码错误 |
| `10002` | token 已失效 |
| `10003` | 已认证但无权限 |
| `20001` | 文章不存在 |
| `20002` | 文章密码错误 |
| `20003` | 文章定时发布时间必须晚于当前时间 |
| `30001` | 评论命中关键词进入待审 |
| `30002` | 评论频率超限 |
| `90001` | 参数校验失败 |
| `90002` | 请求过于频繁（限流） |
| `99999` | 系统错误（兜底，HTTP 500） |

模块内 SSS 从 001 起递增，按业务追加；不复用编号。**`9` 开头预留给基础设施**（90 = common，99 = 系统兜底），业务模块不占。

### C1 JWT 撤销策略

**双 token + token_version 递增**：

- **access token**：JWT，TTL 15 分钟，无状态校验
  - 标准 claim：`sub`（user_id）/ `exp` / `iat` / `iss`
  - 自定义 claim：**`ver`**（int，对应 `t_user_auth.token_version`）/ `typ`（固定 `"access"`）
- **refresh token**：随机字符串（不是 JWT），TTL 7 天，存 `t_refresh_token` 表
- **`t_user_auth.token_version INT NOT NULL DEFAULT 0`**（R1 schema 补列）：改密 / 主动登出 / 强制下线 时 +1
- access token 校验时除标准校验外还校验 `token.ver == user.token_version`，不一致即失效
- refresh 接口：拿 refresh token 换新 access token，同时再次校验 user.token_version

PASSWORD 文章 token 用同样 JWT 结构但 `typ="article_access"` + 自定义 claim `aid`（article_id），与登录 token **靠 typ 字段强区分，互不互通**。

`t_refresh_token` 表：

```sql
id            BIGINT PK             -- ASSIGN_ID（MyBatis-Plus 雪花），不带 AUTO_INCREMENT
user_id       BIGINT NOT NULL
token_hash    VARCHAR(64) NOT NULL UNIQUE  -- SHA-256(refresh token)，不存明文
expires_at    DATETIME NOT NULL
revoked       TINYINT NOT NULL DEFAULT 0
created_at / created_by / ...  -- 审计
KEY idx_user (user_id)
```

- **不引 Redis**（与 R-003 + P-001 教训一致）
- 多实例部署时，token_version + refresh token 都在 DB，撤销跨实例生效

### C4 统计方案：自建

- **明细表 `t_page_view`**：每次访问写一条，含 `article_id / lang / visitor_hash(ip+ua) / referrer / created_at`，**不带审计 8 列**（高写入量，单纯 append-only）
- **每日聚合表 `t_page_view_daily`**：`(article_id, lang, date)` 三元组主键，列：`pv / uv`
- 每天凌晨 `@Scheduled` 任务把昨日明细聚合到 daily 表
- 明细表保留 90 天，定期清理
- 后台"数据总览"页面查 daily 表绘图（文章浏览趋势、热门文章 Top 10、语言分布等）
- **不引第三方**（Umami / GA 后置 V3 评估）

### PASSWORD 文章会话策略

> 本节是上线后目标方案。首版不提供 `/unlock`、article access token 或 PASSWORD 评论授权；公开详情和评论固定返回 `403 + 10003`。

**签发短期专用 token**：

- `/api/public/articles/{id}/unlock POST` body 含明文密码
- 验证通过 → 后端签发 **article access token**（JWT 结构与登录 access token 一致，TTL 30 分钟，`typ="article_access"` + 自定义 claim `aid`=article_id）
- 后续获取该文章正文 / 评论列表 / 提交评论的接口检查 header `X-Article-Token`，校验 token `typ` 必须为 `article_access` 且 `aid` 与当前文章一致
- token 失效 → 重新输密码
- **与登录 token 强隔离**：article access token 不含 `sub`（user_id），不能用作身份凭证；登录 access token `typ="access"`，不能用作文章解锁凭证。两类 token 在过滤器层按 typ 分发到不同处理链

---

## R7 横切基础设施

### D1 部署形态：V2 单实例

- Docker Compose 起步（app 容器 + MySQL 容器 + 可选 Nginx 反代）
- **明确假设**：V2 不支持多实例水平扩展；SCHEDULED 任务不加分布式锁；评论 IP 限流可用进程内 `Caffeine` 计数器
- token_version + refresh token 表设计**仍保留**，跨重启撤销有效；多实例水平扩展后置 V3 评估
- 部署相关具体（CI / 镜像构建 / 反代配置）前端启动期定

### D2 DB 外键策略：不用 DB FK

- 所有 `*_id`（`category_id` / `parent_id` / `reply_to_comment_id` / `cover_attachment_id` / `user_id` 等）只在 application 层维护引用完整性
- DB 层只建普通索引（`KEY idx_xxx`），不建 `FOREIGN KEY` 约束
- 软删时不级联：删父表只把父表 `deleted=1`，子表引用保留，application 层查询时按需过滤
- 物理删除走运维 SQL，执行前手动验关联（与 R4 #17 一致）
- 理由：阿里规范推荐；改表 / 模块迁移 / 软删语义灵活

### D3 配置命名空间 `myblog.*`

| 配置前缀 | 用途 | 示例 key |
|---|---|---|
| `myblog.security.jwt.*` | JWT 签名 / TTL | `secret` / `access-token-ttl` / `refresh-token-ttl` / `issuer` |
| `myblog.security.password.*` | 登录限流 / BCrypt 强度 | `login-max-attempts` / `login-cooldown` / `bcrypt-strength` |
| `myblog.cors.*` | CORS | `allowed-origins` / `allow-credentials` |
| `myblog.storage.*` | 附件存储 | `type`(LOCAL/S3) / `local.root` / `local.public-base-url` |
| `myblog.mail.*` | Resend 配置 | `provider`(resend) / `api-key` / `from` / `from-name` |
| `myblog.ratelimit.*` | 限流参数 | `comment-per-minute` / `unlock-per-window` |
| `myblog.i18n.*` | 默认语言 / 支持语言列表 | `default-lang` / `supported-langs` |
| `myblog.scheduler.*` | 定时任务开关 + cron | `publish.enabled` / `publish.cron` / `stats-aggregate.cron` / `cleanup.cron` |

- **敏感配置**（jwt.secret / mail.api-key / DB password）必须通过环境变量注入，**无默认值**（缺失即启动失败，R-001 红线）
- 启动时 `MyBlogConfigStartupValidator` 一次性校验所有必填项

### D4 MyBatis-Plus 配置

- **逻辑删除**：`@TableLogic(value="0", delval="1")`（与审计列 `deleted TINYINT` 一致）
- **ID 生成**：`IdType.ASSIGN_ID`（雪花算法，避免单调暴露）；t_user_info / t_article_tag 等无独立 id 的表不用
- **自动填充**：`AuditFieldHandler implements MetaObjectHandler`
  - INSERT 填 `created_at` / `created_by` / `updated_at` / `updated_by`
  - UPDATE 填 `updated_at` / `updated_by`
  - 软删走 application 层 service 显式设置 `deleted_at` / `deleted_by`（不靠 handler，避免误填）
- **SQL 注入器**：用 `DefaultSqlInjector`，不自定义
- **分页插件**：`MybatisPlusInterceptor` + `PaginationInnerInterceptor(DbType.MYSQL)`

### D5 邮件方案：Resend

- 用 Resend HTTP API（不走 SMTP）
- 通过域名 DNS 配置 SPF / DKIM / DMARC 验证发件人（部署期完成）
- `MailService` 接口 + `ResendMailService` 实现；未来换 SendGrid 不动业务代码
- 模板用 Thymeleaf（评论被回复通知 / 评论 PASS 通知 / 后续可扩展）
- 模板三语：用收件人语言（评论 author 默认从评论提交时的请求路径判断）
- 失败重试：`@Retryable` 3 次，间隔 2s/5s/10s；3 次失败写 `t_mail_log` 表（结构 R7 后落 DDL 时定）

### D6 限流策略统一

| 接口 | 限制 |
|---|---|
| 登录 `/api/auth/login` | 同 IP + 同 username 5 次/10 分钟冷却（pitfalls U-004） |
| PASSWORD 解锁 `/api/public/articles/{id}/unlock`（上线后目标） | 同 IP + 同 article 5 次/10 分钟冷却（R3 #8 已定） |
| 评论提交 `/api/public/articles/{id}/comments` | 同 IP 1 分钟 5 条 + 5 分钟内同 IP+同文章重复 content 拒绝（R4 #12-P0 已定） |
| 附件上传 | 仅 ADMIN，不限流 |

- 实现：进程内 `Caffeine` 计数器（与 D1 单实例一致）；多实例时需换 Redis（后置 V3）
- 超限统一返 HTTP 429 + 业务码 `90002` "请求过于频繁"

### D7 CORS

```yaml
myblog.cors:
  allowed-origins:
    - https://blog.example.com
    - https://admin.example.com
  allow-credentials: true
  allowed-methods: [GET, POST, PUT, DELETE, PATCH, OPTIONS]
  allowed-headers: ["*"]
  exposed-headers: [Authorization, X-Article-Token]
  max-age: 3600
```

- 开发环境通过 profile 覆盖加 `http://localhost:5173` / `http://localhost:5174`
- 生产配置走环境变量

### D8 日志规范

- **框架**：Logback（Spring Boot 默认）
- **格式**：JSON（用 `logstash-logback-encoder`，便于将来接 ELK / Loki）；开发环境保留普通格式
- **级别**：默认 INFO；业务模块可配 DEBUG（`logging.level.com.myblog.content=DEBUG`）
- **MDC**：每请求注入 `traceId`（UUID）+ `userId`（登录态时）；过滤器层统一注入清理
- **敏感字段脱敏**：自定义 `SensitiveDataMaskingConverter`，对 `password / token / authorization / email / ip / api_key` 字段日志输出时自动脱敏（如 `pa**word`、`13****@gmail.com`）
- **异常日志**：`GlobalExceptionHandler` 业务异常 WARN，系统异常 ERROR（含完整堆栈）

### D9 API 文档：Knife4j

- 引入 `knife4j-openapi3-jakarta-spring-boot-starter`
- 仅 `dev` / `test` profile 启用，`prod` 关闭（避免暴露接口结构）
- 接口分组：按模块（identity / content / comment / system / stats）
- DTO 用 `@Schema` 注解描述

### D10 清理任务

| 任务 | cron | 内容 |
|---|---|---|
| `RefreshTokenCleanupJob` | `0 30 3 * * *`（每日 3:30 JST） | 删除 `t_refresh_token` 中 `expires_at < now OR revoked=1 AND created_at < now-30d` |
| `PageViewCleanupJob` | `0 0 4 * * *`（每日 4:00 JST） | 删除 `t_page_view` 中 `created_at < now-90d` |
| `MailLogCleanupJob` | `0 30 4 * * *`（每日 4:30 JST） | 删除 `t_mail_log` 中 `created_at < now-90d`（R8 E3 已定） |
| `PageViewAggregateJob` | `0 10 0 * * *`（每日 00:10 JST） | 把昨日 `t_page_view` 聚合到 `t_page_view_daily`（覆写） |
| `ScheduledArticlePublishJob` | `0 * * * * *`（每分钟） | R3 #8 已定 |

- 所有任务 `updated_by = NULL` 标识系统任务
- 任务执行日志走业务日志（不单建任务执行记录表）

### D11 时区统一（横切）

V2 所有时间相关处理统一 **Asia/Tokyo（UTC+9）**：

| 层 | 配置 / 做法 |
|---|---|
| JVM | 启动参数 `-Duser.timezone=Asia/Tokyo` |
| MySQL session | `time_zone='+09:00'`（DataSource URL 通过 `sessionVariables` 强制设置） |
| 应用层 | `Clock.system(ZoneId.of("Asia/Tokyo"))` 注入；禁止散落 `LocalDateTime.now()` 直接调用，统一走 `Clock` |
| SCHEDULED SQL | 不用 `NOW()`；`#{now}` 应用层传入 JST `LocalDateTime`（R3 #8 已定） |
| API 返回 | `LocalDateTime` 序列化为 ISO-8601 不带时区后缀（`2026-06-03T14:30:00`），前端按 JST 展示 |
| 前端展示 | 统一按 JST 渲染；日/英 UI 显示日期格式分别按 ja-JP / en-US 区域设置 |

理由：作品集场景目标受众在日本，单时区比双时区简单；用户不分布全球，无需 UTC 中性存储。

---

## 横切：审计列规范

适用于所有"业务实体表"。关联表 / 历史表 / 聚合表按例外处理。

### 标准 8 列

| 列名 | 类型 | NULL | 默认 | 含义 |
|---|---|---|---|---|
| `id` | BIGINT | NOT NULL | （MyBatis-Plus `IdType.ASSIGN_ID` 雪花生成；日志型例外可保留 DB AUTO_INCREMENT） | 主键 |
| `created_at` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| `created_by` | BIGINT | NULL | NULL | 创建者 `t_user_auth.id`（游客 NULL） |
| `updated_at` | DATETIME | NOT NULL | CURRENT_TIMESTAMP | 最后修改时间；由 `AuditFieldHandler` 在应用层更新，不使用 DB `ON UPDATE` |
| `updated_by` | BIGINT | NULL | NULL | 最后修改者 |
| `deleted` | TINYINT | NOT NULL | 0 | 软删标记（0=正常 1=已删） |
| `deleted_at` | DATETIME | NULL | NULL | 删除时间 |
| `deleted_by` | BIGINT | NULL | NULL | 删除者 |

### 落地

- `BaseEntity` 抽象基类承载 8 列；业务实体继承
- `AuditFieldHandler implements MetaObjectHandler` 自动填 created/updated/deleted 元数据
- `current_user_id` 从 SecurityContext 取（无登录态时填 NULL）
- 高频查询表（`t_article`、`t_comment`）的 `deleted` 列建索引

### 语义说明（重要）

- `created_by` / `updated_by` **只记录系统用户**（`t_user_auth.id`），不记录游客身份
- 游客评论时 `created_by = NULL` 是正常情况，不是审计缺失
- 游客身份信息存在评论表自己的 `author_nickname / author_email / author_ip / author_user_agent` 字段里

### 例外（不带审计列或只带部分）

| 表 | 不带 | 理由 |
|---|---|---|
| `t_user_info` | 独立 `id` 列（用 `user_id` 同时作 PK + 逻辑引用 t_user_auth.id） | 与 `t_user_auth` 1:1 强绑定，无需独立主键（不建 DB FOREIGN KEY，ADR-0017）。继承 `AuditOnlyBase`（7 列审计） |
| `t_article_tag`（关联表） | 全部 8 列 | 中间表，靠主表审计 |
| `t_refresh_token` | `deleted` 三件套 | 用 `revoked` 表示失效，过期 / 撤销由清理任务物理删 |
| `t_page_view`（明细，append-only） | 全部 8 列 | 高写入量，仅 `created_at` + 业务字段，append-only |
| `t_page_view_daily`（统计聚合） | 大半 | 复合 PK `(article_id, lang, stat_date)`，无独立 `id`、无软删 |
| `t_mail_log`（append-only 日志） | 全部 8 列 | 邮件发送失败日志，仅 `id + created_at` + 业务字段；`id` 用 `AUTO_INCREMENT`（R8 E3） |

例外表手动定义实体，不继承 `BaseEntity`。

---

## R8 Schema 缺口补齐（DDL 冻结前）

> R1–R7 拼合后剩余 3 张表字段未定。本节补齐，使 14 张表全部可直接产出 DDL。

### E1 `t_user_auth` 完整字段

R1 #3/#4/#5' 与 R6 C1 已定语义，此处明确 DDL：

```sql
t_user_auth:
  id                BIGINT       NOT NULL      -- ASSIGN_ID（雪花），PK
  username          VARCHAR(64)  NOT NULL
  password_hash     VARCHAR(72)  NOT NULL      -- BCrypt 60 字符 + 余量
  type              TINYINT      NOT NULL      -- 1=ADMIN / 2=DEMO / 3=GUEST（R1 #4）
  token_version     INT          NOT NULL DEFAULT 0   -- R6 C1 JWT 撤销
  last_login_at     DATETIME     NULL
  last_login_ip     VARCHAR(45)  NULL          -- IPv6 兼容长度
  login_fail_count  INT          NOT NULL DEFAULT 0
  locked_until      DATETIME     NULL          -- 锁定截止时间
  -- 审计 8 列
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_auth_username (username)
```

**关键决定**：

- `locked_until` 入库（不仅靠 Caffeine）—— 进程重启不丢锁定状态，与 D6 登录限流配套（5 次失败锁 10 分钟）
- 不存 `ip_source`（地理位置）—— V1 遗留，作品集场景无价值
- `password_hash` 长度 72：BCrypt 输出固定 60 字符，留余量以防换算法

### E2 `t_friend_link` 字段

R5 B1 归 `system` 模块，此处明确字段：

```sql
t_friend_link:
  id           BIGINT       NOT NULL      -- ASSIGN_ID，PK
  name         VARCHAR(64)  NOT NULL      -- 友链名称
  url          VARCHAR(255) NOT NULL      -- 站点地址
  avatar_url   VARCHAR(255) NULL          -- 头像 / logo
  description  VARCHAR(255) NULL          -- 一句话介绍（单中文）
  sort_order   INT          NOT NULL DEFAULT 0  -- 后台拖拽排序
  status       TINYINT      NOT NULL DEFAULT 1  -- 1=显示 / 2=隐藏
  -- 审计 8 列
  PRIMARY KEY (id),
  KEY idx_friend_link_status_sort (status, sort_order)
```

**关键决定**：

- `description` **单中文**，不做三语 —— 友链总量小（撑死 ~30 条），三语过度设计；日本访客通过 url 自行判断
- 保留 `status`（显示/隐藏），不靠 `deleted` 兼任 —— "朋友站临时宕机暂时下架但不删"是常见诉求，语义比软删清晰
- **不加申请审核字段** —— 友链申请走邮件 / GitHub issue，进库即可见；未来若做申请表单，单开 `t_friend_link_application` 表，不污染当前表

### E3 `t_mail_log` 字段

R7 D5 留口"3 次失败写 `t_mail_log` 表，结构 R7 后落 DDL 时定"，此处补齐：

```sql
t_mail_log:
  id                   BIGINT       NOT NULL AUTO_INCREMENT  -- 例外：append-only 日志型用自增（ADR-0015 §6 例外），与 t_page_view 一致；雪花 id 对永不被引用的日志表是过度设计
  to_email             VARCHAR(128) NOT NULL
  template             VARCHAR(64)  NOT NULL      -- comment_reply / comment_passed 等模板名
  subject              VARCHAR(255) NOT NULL
  status               TINYINT      NOT NULL      -- 1=SUCCESS / 2=FAILED（V2 起点只写 FAILED）
  retry_count          INT          NOT NULL DEFAULT 0
  error_message        VARCHAR(512) NULL          -- 失败原因（脱敏后）
  provider_message_id  VARCHAR(64)  NULL          -- Resend 返回的 message id
  params_json          VARCHAR(512) NULL          -- 模板渲染关键参数（comment_id 等）
  created_at           DATETIME     NOT NULL
  -- 不带完整审计 8 列（append-only 日志型，例外表，参照 t_page_view 规则）
  PRIMARY KEY (id),
  KEY idx_mail_log_status_created (status, created_at)
```

**关键决定**：

- **只写失败记录**（D5 原意） —— 表小、语义清晰（日志=异常）；评论一多每天几十封邮件全记会涨表
- **不做独立重试 job** —— `@Retryable` 已三次重试，再失败大概率是配置/账号问题，人工看日志处理
- 列入 D10 清理任务，保留 90 天
- 例外表：参照 `t_page_view` 规则，不带审计 8 列（仅 `created_at`）

### R8 衍生

- **审计列规范"例外表"清单**追加：`t_mail_log`（理由：append-only 日志型，仅 `created_at` + 业务字段）
- **D10 清理任务表**追加：`MailLogCleanupJob` 每日 4:30 JST 删除 `created_at < now-90d` 的记录
- **模块归属**：`t_friend_link` 归 `system`（R5 B1 已定）；`t_user_auth` 归 `identity`；`t_mail_log` 归 `common-infra`（基础设施日志）

---

## 后续待开

### 延后（前端启动 / 部署期定）

- FE-1 邮件模板细化（HTML 设计、退订链接等）
- FE-2 图床后端**实现切换**（已在 R5 决定起点为 LOCAL，未来切 S3/OSS 不动 schema）
- FE-3 部署 CI / 镜像构建 / 反代具体配置（部署形态 V2 单实例已在 R7 定）
- FE-4 前台主题风格
- FE-5 ~~后台 UI 框架~~ → **已定：Element Plus**（见 `frontend/apps/admin/docs/README.md` 已决策项）
- FE-6 Markdown 渲染器（Vditor / Bytemd / md-editor-v3）
- FE-7 CSS 方案（Tailwind / 原生 / UnoCSS）

---

## 全部封轮后

整理本文档为：

- `product/data-model.md`（业务实体 + ER 图）
- `arch/schema-design.md`（具体表 DDL，符合 ADR-0014 规范）

然后开始动 V2 代码。
