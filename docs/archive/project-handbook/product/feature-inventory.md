# V1 功能清单（人类决策版）

> 这份文档帮你**快速决定 V2 留什么、砍什么、加什么**。
> 读法：从上到下扫一遍，看到 AI 推荐不同意就在【你的决定】栏写一句覆盖。完全同意可以不写。
> 详细的接口逐条清单沉到文末附录，只在需要细究时打开。

---

## 一句话现状

V1 是套**企业级博客系统**（原作者按公司项目套路写的），24 张表、100+ 接口、19 个后台模块。对"你一个人写 20 篇文章"来说严重过度设计。

**AI 的总建议**：砍掉企业级冗余（Quartz / 动态菜单 / 复杂 RBAC / 操作日志入库 / 在线用户管理），砍掉沉重内容载体（相册 / 音乐 / 说说），保留博客核心三件套（内容 / 互动 / 身份），补齐现代博客标配（暗黑模式 / RSS / Sitemap / 评论邮件通知 / 图床抽象 / Vditor 编辑器）。

**净结果**：

| 维度            | V1        | V2（如全采纳推荐） |
| ------------- | --------- | ---------- |
| 数据库表          | 24 + 音乐 1 | ~10        |
| 后端 Controller | 20        | ~8         |
| 接口数           | 100+      | ~40        |
| 前台页面          | 12        | 8          |
| 后台模块          | 19        | 8          |

---

## 二、按功能模块逐项决策

每个模块一张卡，**只看 AI 推荐 + 理由**，不同意就在最后一行写。

### ① 文章

- **V1 现状**：完整 CRUD、置顶推荐、密码保护、归档、搜索、按分类/标签筛选、导入、导出、上传配图
- **AI 推荐**：**留**（核心功能）。微调三处：
  - URL 用 slug（`/posts/hello-world`）替代 id，对 SEO 友好
  - 砍批量导入（个人写作不需要）
  - 单篇导出 .md 保留
- **你的决定**：**同意 + 已扩展（R3 / R7 / ADR-0016）**。5 态状态机（DRAFT / PUBLISHED / PRIVATE / PASSWORD / SCHEDULED）；URL `/{lang}/posts/{id}` 或 `/{lang}/posts/{id}-{slug}`，slug 非唯一可空可改；title + summary 三语，正文单中文（R2）；PASSWORD 文章独立 access token（R6 `typ="article_access"` + `aid` claim）。砍批量导入；单篇 .md 导出保留

### ② 分类 / 标签

- **V1 现状**：分类和标签都是基础 CRUD
- **AI 推荐**：**留**
- **你的决定**：**同意（R5 B1 + R7 D11 + ADR-0016）**。归 `content` 模块；`t_category.slug` / `t_tag.slug` UNIQUE（ADR-0016 例外条款）；分类/标签 name 三语

### ③ 评论

- **V1 现状**：一张表用 type=1~5 承载 5 种评论（文章 / 留言 / 关于我 / 友链 / 说说）
- **AI 推荐**：**改**。简化为两种：文章评论 + 留言板（留言板 = topic_id=0 的特殊评论），其他全砍。同时补：
  - 评论 Markdown 支持（链接 / 代码 / 换行）
  - **@ 回复时邮件通知被回复者**（现代博客必备，体验提升大）
  - Gravatar 头像
  - 评论列表显示 OS / 浏览器
- **你的决定**：**同意 + 扩展（R4 / R5）**。归 `comment` 模块；t_comment 三态（NORMAL / PENDING / HIDDEN）；`content_md` ≤5000 + `content_html`（后端 sanitize 入库），**前端只渲染 content_html**（pitfalls R-013 红线）；@ 回复走 Resend HTTP API 异步邮件；Gravatar；UA 解析后台展示

### ④ 留言板

- **V1 现状**：复用评论的 type=2
- **AI 推荐**：**留**，继续复用评论表（topic_id=0）。不必为留言板拆独立实体
- **你的决定**：**同意（R4）**。复用 t_comment，`target_type=GUESTBOOK` + `target_id=0`（字段决策已落进 `decisions-draft.md` R4 #10.5）

### ⑤ 友链

- **V1 现状**：管理员后台 CRUD
- **AI 推荐**：**留**，并补一个"申请友链"表单（访客在线提交，管理员审核）
- **你的决定**：**同意（R5 B1）**。归 `system` 模块；`t_friend_link` 含审核态（PENDING / APPROVED / REJECTED）；访客表单走限流（参考 R6 评论限流策略）
  - ⚠️ **已被 R8 E2 修订**：`t_friend_link` **不含审核态字段**，仅 `status` 显示/隐藏；友链申请走邮件 / GitHub issue。未来若做在线申请表单，单开 `t_friend_link_application` 表，不污染主表。

### ⑥ 关于我

- **V1 现状**：独占一张 `t_about` 表
- **AI 推荐**：**改**。不专占一张表，并入 `t_site_config` 作为一个 key（`about_markdown`）
- **你的决定**：**同意 + 升级（R2 #7c）**。并入 `t_site_config`，三语字段 `about_md_zh / about_md_ja / about_md_en`（非 `about_markdown` 单值）

### ⑦ 网站配置

- **V1 现状**：`t_website_config`（标题 / Logo / 备案号等）
- **AI 推荐**：**留**，改名为更短的 `t_site_config`
- **你的决定**：**同意（R2 #7c）**。`t_site_config` 采用**宽表**（非 KV），单行（id=1）；展示字段三语，非展示单值（含 `spotify_playlist_id`）

### ⑧ 用户 / 登录

- **V1 现状**：访客可注册、QQ OAuth 登录、邮箱验证码、后台用户管理、在线用户管理、强制下线
- **AI 推荐**：**大改**。简化为只有"管理员"一个账号：
  - 砍掉访客注册（评论用游客模式：昵称 + 邮箱 + 站点）
  - 砍掉 QQ OAuth
  - 砍掉邮箱验证码
  - 砍掉后台用户管理（你一个人管理员，没人可管）
  - 砍掉在线用户 / 强制下线（不需要 Redis 也省了）
  - 保留：管理员登录 / 登出 / 改密码 / 改资料
- **你的决定**：**部分调整（R5 / R6 / ADR-0007 R6 C1 补充）**。改为 **ADMIN / DEMO / GUEST 三态枚举**（非"只有 ADMIN"）：ADMIN = 你自己；DEMO = 给 SES 主管演示用的只读账号；GUEST = 评论游客模式。砍访客注册 / QQ OAuth / 邮箱验证码 / 在线用户 / 强制下线。**双 token 机制**：access JWT 15min + refresh 7d（SHA-256 hash 存 `t_refresh_token`），靠 `token_version` 实现"改密即吊销"

### ⑨ 角色 / 权限

- **V1 现状**：完整 RBAC — 角色 / 资源（接口路径）/ 菜单 / 三张关联表
- **AI 推荐**：**全删**。简化为枚举 `Role.ADMIN`，接口用 `@PreAuthorize("hasRole('ADMIN')")` 注解控制即可
- **你的决定**：**同意 + 微调（R5 / R6）**。枚举改为 **`Role.ADMIN / DEMO / GUEST` 三态**；接口 `@PreAuthorize("hasRole('ADMIN')")` 控制；删 4 张 RBAC 表（`t_resource` / `t_role` / `t_role_menu` / `t_role_resource` / `t_user_role`）

### ⑩ 后台菜单

- **V1 现状**：后端动态下发（`/admin/user/menus`）
- **AI 推荐**：**删**。前端静态路由更简单、更稳、更易调试
- **你的决定**：**同意**。前端 Vue Router 静态定义，删 `t_menu` + `/admin/user/menus` 接口

### ⑪ 说说（短动态）

- **V1 现状**：完整模块（列表 / 详情 / 评论 / 后台管理 / 表 + 7 接口）
- **AI 推荐**：**全删**。短动态发微博 / X 更顺手，博客主战场是长文章
- **你的决定**：**同意**。全删 `t_talk` + 相关接口/前后台页面

### ⑫ 相册 / 照片

- **V1 现状**：完整相册系统（相册 + 照片两张表 + 14 接口）
- **AI 推荐**：**全删**。自建相册维护成本高；真想展示照片，写一篇"图片集合"文章即可
- **你的决定**：**同意**。全删 `t_photo` / `t_photo_album`。需要展示照片就写"图片集合"文章

### ⑬ 音乐播放器

- **V1 现状**：前台播放器 + 后台管理（6 接口 + 表）
- **AI 推荐**：**全删**。多数访客嫌打扰；非要保留可嵌一个网易云分享 iframe，不入库
- **你的决定**：**留 / Spotify Embed 方案**。不进 V2 模块边界，不建 `t_music`，仅在 `t_site_config` 加 1 个 key `spotify_playlist_id`，前端读配置渲染 Spotify 官方 iframe。理由：版权干净、访客免费可听全曲、零后端成本、SES 日本场景受众接受度高

### ⑭ 定时任务（Quartz）

- **V1 现状**：完整后台管理 + 日志查看（13 接口 + 两张表）
- **AI 推荐**：**全删**。个人博客没"业务定时任务"需求；技术性的用 Spring `@Scheduled` 写死
- **你的决定**：**同意（ADR-0018）**。删 `t_job` / `t_job_log`。SCHEDULED 文章定时发布走 Spring `@Scheduled`，时间从注入的 `Clock`（JST）取，SQL 由应用层传 `#{now}` 参数（不用 DB `NOW()`）

### ⑮ 操作日志 / 异常日志

- **V1 现状**：每个后台请求记一行，每个异常记一行，后台可查
- **AI 推荐**：**全删**。走 logback 日志文件即可，需要时 grep；后台不开日志查看页
- **你的决定**：**同意**。删 `t_operation_log` / `t_exception_log`；走 logback 文件 + 异常通过统一 `GlobalExceptionHandler` 落日志（rules/error-handling.md）

### ⑯ 仪表盘

- **V1 现状**：echarts 全家桶 + GitHub 风格活动热图
- **AI 推荐**：**改**。简化为几张数字卡（总文章数 / 总评论数 / 本月发布 / 最新评论列表 / 文章访问 TOP 10）；活动热图搬到前台 about 页（这个有意思，留着）
- **你的决定**：**同意**。后台仪表盘 = 数字卡 + 最新评论 + 文章访问 TOP 10；活动热图迁前台 about 页

### ⑰ 访客统计（PV / UV）

- **V1 现状**：`/report` 接口 + `t_unique_view` 表
- **AI 推荐**：两选一
  - **选 A（推荐）**：自建只留一张日聚合表 `t_page_view_daily`，复杂统计接 **Umami / Plausible**（开源自托管，免费）
  - **选 B**：全砍自建，纯接 Umami
- **你的决定**：**自研为主，不引第三方（V2 范围内）**。归 `stats` 模块（R5 B1）；`t_page_view`（原始打点，AuditOnlyBase 7 列例外，ADR-0015）+ `t_page_view_daily`（日聚合）；后台"数据总览"页面查 daily 表绘图（文章浏览趋势、热门文章 TOP 10、语言分布等）；**Umami / GA 后置评估**，需要时前端嵌 `<script>` 即可加，不预留抽象层（YAGNI）

### ⑱ 国际化（vue-i18n）

- **V1 现状**：前台带 vue-i18n
- **AI 推荐**：**删**。你不会真运营多语言，i18n 增加大量重复维护
- **你的决定**：多语言需要留着且V1版本有大问题，本次需要讨论着重新搞。**已重设计（R2 #7 - #19）**：三语 zh / ja / en；路径前缀 `/{lang}/posts/...`；UI 文案 vue-i18n 构建期打包，业务内容 DB 三语副本（文章 title+summary、分类/标签 name、站点配置展示字段、关于我）；正文单中文（不翻译）；字体 CSS `:lang()` + Noto Sans SC/JP/Sans；默认语言 `navigator.language` 自动跳 + `localStorage` 记忆；翻译走 DeepL 打底 + 人工校对 + `frontend-user/i18n-glossary.md` 术语表

### ⑲ 头像裁剪 / 富头像管理

- **V1 现状**：vue-avatar-cropper 组件
- **AI 推荐**：**删**。管理员只有你一个，直接传图即可
- **你的决定**：**同意**。删头像裁剪组件，直接传图

### ⑳ 后台技术栈

- **V1 现状**：Vue 2.6 + Element UI + mavon-editor
- **AI 推荐**：**大改**。升级到 Vue 3 + Element Plus + Pinia + TS（与前台统一）；编辑器换 **Vditor** 或 **Bytemd**（更现代，对图床 / 数学公式 / Mermaid 友好）
- **你的决定**：**同意（R7 D11 / FE-1~7）**。Vue 3 + Element Plus + Pinia + TypeScript + vue-i18n（前后台统一栈）；编辑器选 **Vditor**（候选 Vditor / Bytemd，最终在前端实施前敲定）；API 文档用 Knife4j 4.x（基于 springdoc-openapi）

---

## 三、新增功能候选（业界博客借鉴）

调研了 **Halo / Ghost / Hexo Butterfly** 和个人技术博客圈通行做法。我标了三档优先级：

### 🟢 强烈推荐（现代博客标配，几乎零异议）

- **暗黑模式**（跟随系统 + 手动切换）
- **RSS / Atom 订阅**（V1 居然没有）
- **Sitemap.xml 自动生成**（搜索引擎收录）
- **文章 SEO 元数据**（自定义 description / keywords / og:image）
- **文章 slug**（URL 友好别名）
- **评论 @ 回复邮件通知**
- **图床抽象层**（本地 / 七牛 / OSS / GitHub 仓库 任选一个 backend）
- **编辑器图片粘贴 / 拖拽自动上传图床**
- **评论 XSS 清洗**（已在 pitfalls U-002 跟踪）
- **上传文件 MIME + 大小校验**（已在 U-003 跟踪）

### 🟡 推荐（性价比高，写一次终身受益）

- 文章字数 / 阅读时长估算
- 代码块"复制按钮"+ 行号 + 语言标签
- 文章版权声明（默认 CC BY-NC-SA 4.0）
- 文章上一篇 / 下一篇导航
- 相关文章推荐（按标签匹配）
- 阅读进度条（顶部细条）
- 图片懒加载
- 文章点赞（无 IP 去重）
- 评论 Markdown 支持
- Gravatar 评论头像
- 友链申请表单
- 草稿自动保存
- API 限流（评论 / 搜索 接口防爬）
- 评论简单防刷（同 IP 同内容短时间拒绝）
- 登录失败次数限制
- 自动备份（DB dump + 文章 .md 双备份）
- 接 **Umami / Plausible** 第三方统计
- 文章访问 TOP 10 排行
- 前台 about 页"写作热力图"（GitHub 风格）

### 🤔 待你决定（不是必须，看你个人偏好）

- 文章修订历史 / 版本回滚（实现成本中，使用频率低）
- 文章发布定时（指定时间自动发布）
- Newsletter 邮件订阅（要 SMTP 配置 + 持续运营）
- 评论表情面板
- 评论 IP 归属地显示（需要第三方 IP 库）
- "正在听 / 正在读 / 现在做什么"侧栏（NowPage 风格）
- JSON Feed
- PWA（离线浏览）

**填法**：在上面三组里勾你想要的（或直接在卡片下面写"以上 🟢 全要，🟡 除 X 外都要，🤔 都不要"这种粗粒度回复即可）。

---

## 四、AI 给的 V2 形态草图（如全采纳推荐）

- **后端**：1 个 SpringBoot 服务，6 模块，~13 张表（article / article_tag / category / tag / comment / friend_link / user_auth / user_info / refresh_token / site_config / attachment / page_view / page_view_daily）
- **前台**：Vue 3 + Element Plus + Pinia，8 个页面（首页 / 文章详情 / 分类页 / 标签页 / 归档 / 留言 / 友链 / 关于）+ 暗黑模式 + RSS + Sitemap
- **后台**：Vue 3 + Element Plus + Pinia（统一前台技术栈），8 个页面（登录 / 仪表盘 / 文章 / 分类 / 标签 / 评论 / 友链 / 设置）+ Vditor 编辑器 + 图床抽象
- **角色**：单一管理员，无访客账号体系，评论用游客模式
- **统计**：自研只做日聚合 PV/UV，详细统计接 Umami

---

## 下一步

1. **你扫一遍上面二、三两节，写下不同意的地方**（不急，可分多次）
2. 填完叫我，下一步：把"留 + 改 + 新增"转成 `product/use-cases.md`（用户故事） + `product/data-model.md`（领域实体）
3. 再下一步：`arch/schema-design.md`（具体表 DDL）
4. 最后才动 V2 代码

---

## 附录 A：V1 全部 24 张表速查

> 仅供你想细究"具体哪张表去哪了"时翻阅。结论已浓缩进上面的"二、按功能模块逐项决策"。

| #   | 表名                 | AI 推荐               | 归属决策   |
| --- | ------------------ | ------------------- | ------ |
| 1   | `t_about`          | 改                   | ⑥ 关于我  |
| 2   | `t_article`        | 留                   | ① 文章   |
| 3   | `t_article_tag`    | 留                   | ① 文章   |
| 4   | `t_category`       | 留                   | ② 分类   |
| 5   | `t_comment`        | 改                   | ③ 评论   |
| 6   | `t_friend_link`    | 留                   | ⑤ 友链   |
| 7   | `t_job`            | 删                   | ⑭ 定时任务 |
| 8   | `t_job_log`        | 删                   | ⑭ 定时任务 |
| 9   | `t_menu`           | 删                   | ⑩ 后台菜单 |
| 10  | `t_operation_log`  | 删                   | ⑮ 操作日志 |
| 11  | `t_photo`          | 删                   | ⑫ 相册   |
| 12  | `t_photo_album`    | 删                   | ⑫ 相册   |
| 13  | `t_resource`       | 删                   | ⑨ RBAC |
| 14  | `t_role`           | 改（→枚举）              | ⑨ RBAC |
| 15  | `t_role_menu`      | 删                   | ⑨ RBAC |
| 16  | `t_role_resource`  | 删                   | ⑨ RBAC |
| 17  | `t_tag`            | 留                   | ② 标签   |
| 18  | `t_talk`           | 删                   | ⑪ 说说   |
| 19  | `t_unique_view`    | 改                   | ⑰ 访客统计 |
| 20  | `t_user_auth`      | 留                   | ⑧ 用户登录 |
| 21  | `t_user_info`      | 留                   | ⑧ 用户登录 |
| 22  | `t_user_role`      | 删                   | ⑨ RBAC |
| 23  | `t_website_config` | 留（→`t_site_config`） | ⑦ 网站配置 |
| 24  | `t_exception_log`  | 删                   | ⑮ 异常日志 |
| +   | music-player.sql   | 删                   | ⑬ 音乐   |

## 附录 B：V1 接口 / 页面汇总

- V1 后端 Controller 共 20 个，接口 100+
- V1 前台页面 12 个（Home / Article / ArticleList / Archives / Tags / About / Message / FriendLink / TalkList / Talk / Photos / 404）
- V1 后台模块 19 个（about / album / article / category / comment / friendLink / home / log / login / menu / music / quartz / resource / role / setting / tag / talk / user / website）

如需逐接口、逐页面的精细决策（哪个接口要保留、哪个 URL 要改路径），告诉我，我再展开成详细矩阵。
