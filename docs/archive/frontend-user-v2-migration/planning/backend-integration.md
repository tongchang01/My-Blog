# 05 · 后台对接（i18n + 配置 DB 化）

> 范围：把 Aurora 前端**和自研 V2 后台**对齐的两条主线 ——
> 1. 三语（zh / ja / en）方案；
> 2. 可配置项从前端硬编码 / mock JSON 迁到后台管理页（DB 持久化）。
>
> 后端参考：`c:/tyb/My-Blog/docs/project-handbook/` —— 重点 `arch/schema-design.md`、`product/decisions-draft.md` #14–19、`product/feature-inventory.md`、`api-contract/README.md`、`frontend-user/README.md`。
>
> **写法**：本文档以**前端视角**写"我需要后台提供什么"，每条尽量对齐后台已有的表 / ADR。等真到了对接阶段（Phase 7 V2 后端对接）再带着这份清单和后端同步、补 schema。

---

## 0. 技术栈口径（⚠️ 重要修订）

后台文档 `frontend-user/README.md` 原文：
> **技术栈**：Vue 3 + Element Plus + Pinia + TypeScript + vue-i18n（前后台统一）

**本次重构定调**：
- **用户前台** = Aurora（Vue 3 + Tailwind + 自研组件）—— 不引入 Element Plus
- **后台管理页** = Vue 3 + Element Plus + Pinia + TS + vue-i18n（按后台原计划）
- 前后台**共享数据契约**（API、字段、i18n 键、`t_site_config` 列），**不共享 UI 框架**

→ 真要对接时，需要让后台同学把 `frontend-user/README.md` 那句"前后台统一"改成"数据契约统一，UI 各自"。

---

## 1. 国际化（zh / ja / en）

### 1.1 决策（与后台 R2 #14–19 完全对齐）

| 维度 | 方案 | 来源 |
|---|---|---|
| 目标语言 | `zh` / `ja` / `en` | R2 #14 |
| URL 形式 | `/{lang}/posts/{id}` 路径前缀 | R2 #14 |
| 默认语言 | 首次：`navigator.language` → 自动跳；后续：`localStorage` 记忆 | #16，**前端独占**，后端 0 介入 |
| Fallback | `ja → zh`、`en → zh`（取不到对应字段时回落 `_zh`） | R8'、R2 #15 |
| 正文 | **单中文不翻译**（`t_article.body` 单字段） | R7a |
| UI 文案 | vue-i18n 构建期打包 JSON `src/locales/languages/{zh,ja,en}.json` | R2 #15 |
| 业务文案 | DB 三语副本字段：`title_{zh,ja,en}` / `summary_{zh,ja,en}` / `name_{zh,ja,en}` 等 | R2 #7a/#7b/#7c |
| 字体 | CSS `:lang()` + Noto Sans SC / JP / Sans | #18 |
| 切换 UI | Header Controls 内 Dropdown 3 选 1 | 前端 |

### 1.2 前端路由

`src/router/index.ts`：

```ts
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: routes.map(r => ({
    ...r,
    path: r.path === '/' ? '/:lang(zh|ja|en)?' : `/:lang(zh|ja|en)?${r.path}`
  })),
  scrollBehavior(/* 不变 */)
})
```

`src/router/guard.ts` 的 `beforeEach`：

```ts
const lang = (to.params.lang as string) || detectLang()
if (!to.params.lang) {
  return next({ path: `/${lang}${to.path}`, query: to.query, hash: to.hash })
}
i18n.global.locale.value = lang as 'zh' | 'ja' | 'en'
localStorage.setItem('aurora.lang', lang)
document.documentElement.lang = lang
next()
```

`detectLang()`（`utils/index.ts`）：

```ts
export function detectLang(): 'zh' | 'ja' | 'en' {
  const stored = localStorage.getItem('aurora.lang')
  if (stored === 'zh' || stored === 'ja' || stored === 'en') return stored
  const nav = (navigator.language || 'zh').toLowerCase()
  if (nav.startsWith('ja')) return 'ja'
  if (nav.startsWith('en')) return 'en'
  return 'zh'
}
```

### 1.3 业务字段三语取值（adapter 层）

后端约定（来自 `api-contract/README.md`）：
- **JSON camelCase**（snake_case 列名 → JSON 中是 `titleZh / titleJa / titleEn`）
- **ApiResponse 包络** `{ code: "00000", msg, data }`
- 时间统一 ISO 8601 本地 JST（如 `2026-06-01T12:34:56`，无 `Z`）
- 分页 `?page=1&size=20`，响应 `{ total, list, page, size }`

文章详情示例返回：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "id": 42,
    "slug": "hello-aurora",
    "titleZh": "你好 Aurora",
    "titleJa": null,
    "titleEn": null,
    "summaryZh": "...",
    "summaryJa": null,
    "summaryEn": null,
    "body": "（单中文 markdown）",
    "publishAt": "2026-06-01T10:00:00"
  }
}
```

前端 adapter（`src/api/adapters/post.ts`）：

```ts
function pickLang(raw: any, base: string, lang: string): string {
  const Cap = lang.charAt(0).toUpperCase() + lang.slice(1)  // 'Zh' / 'Ja' / 'En'
  return raw[`${base}${Cap}`] || raw[`${base}Zh`] || ''
}

export function adaptV2Post(raw: any, lang: 'zh' | 'ja' | 'en'): Post {
  return {
    uid: raw.id,
    slug: raw.slug,
    title: pickLang(raw, 'title', lang),
    excerpt: pickLang(raw, 'summary', lang),
    content: raw.body,
    date: raw.publishAt
  }
}
```

> 切语言后 store 里缓存的 `posts` 需要失效——`app.ts` 的 `setLocale` 调用各 store `reset()` + 重新 fetch。

非中文 locale 进中文正文时（R2 R7a 提示）："本文主要以中文写作"——在 `PageContent.vue` 顶部条件渲染一行小提示。

### 1.4 字体（`src/styles/i18n-fonts.scss`，main.ts 之前 import）

```scss
:lang(zh) { font-family: 'Noto Sans SC', 'PingFang SC', system-ui, sans-serif; }
:lang(ja) { font-family: 'Noto Sans JP', 'Hiragino Sans', system-ui, sans-serif; }
:lang(en) { font-family: 'Noto Sans', system-ui, sans-serif; }
```

CDN：`https://fonts.loli.net/css?family=Noto+Sans+SC|Noto+Sans+JP|Noto+Sans`，替换当前 `index.html` 里的 Rubik。

### 1.5 文件清单一览

| 文件 | 改动 |
|---|---|
| `src/locales/languages/zh.json` | 由 `zh-CN.json` 重命名 |
| `src/locales/languages/ja.json` | 新增（先 copy zh） |
| `src/locales/languages/zh-TW.json` | 删 |
| `src/locales/index.ts` | locale 列表 `['zh','ja','en']`、`fallbackLocale: 'zh'` |
| `src/router/index.ts` | 加 `:lang` 段 |
| `src/router/guard.ts` | 加 detect + redirect + setLocale |
| `src/utils/index.ts` | 新增 `detectLang()` |
| `src/stores/app.ts` | `locale` 默认 `'zh'`；`setLocale` 触发数据 refetch |
| `src/components/Header/src/Controls.vue` | Dropdown 改 3 项；点击写 localStorage + push 路由 |
| `src/api/adapters/*.ts` | 新增（按 locale 选字段） |
| `src/styles/i18n-fonts.scss` | 新增 |
| `src/models/ThemeConfig.class.ts` | `Locales` 枚举改 `zh/ja/en` |
| `src/utils/localization.ts` | 保留；badge 文案在三个 json 都加键 |
| `index.html` | 字体 link 改为 Noto Sans 三套 |

---

## 2. 配置 DB 化（站点配置进后台管理页）

### 2.1 后端已有的相关表（schema-design.md §3.x）

**直接相关的 3 张表 + 1 张关联表**：

| 表 | 节 | 作用 | 三语 |
|---|---|---|---|
| `t_site_config` | §3.9 | 站点配置（宽表，单行 id=1） | 部分字段三语 |
| `t_user_info` | §3.2 | 用户资料（1:1 t_user_auth） | bio 三语 |
| `t_friend_link` | §3.11 | 友链 | 单中文 description |
| `t_attachment` | §3.10 | 附件（logo / favicon / 头像走 URL 引用，不直接关联）| — |

### 2.2 `t_site_config` 当前列（后端已建）

```
id                      BIGINT          固定 1
site_title_zh / ja / en VARCHAR(128)    站点标题三语
site_subtitle_zh / ja / en VARCHAR(255) 副标题三语
about_md_zh / ja / en   MEDIUMTEXT      关于我三语
logo_url                VARCHAR(255)    站点 Logo URL
favicon_url             VARCHAR(255)    favicon URL
icp_no                  VARCHAR(64)     ICP 备案号
spotify_playlist_id     VARCHAR(64)     Spotify 播放列表 ID
+ 7 列审计
```

### 2.3 `t_user_info` 当前列（后端已建）

```
user_id         BIGINT          PK + 逻辑引用 t_user_auth.id
nickname        VARCHAR(64)     展示昵称
avatar_url      VARCHAR(255)    头像 URL
bio_zh/ja/en    TEXT            个人简介三语
location        VARCHAR(64)     所在地（Tokyo）
website         VARCHAR(255)    个人主页
email_public    VARCHAR(128)    公开邮箱
github_url      VARCHAR(255)
twitter_url     VARCHAR(255)
linkedin_url    VARCHAR(255)
zhihu_url       VARCHAR(255)
qiita_url       VARCHAR(255)
juejin_url      VARCHAR(255)
+ 7 列审计
```

→ **作者卡 / 社交链接 → 直接读 `t_user_info`**，不需要再造 `t_social_link` 表。

### 2.4 `t_friend_link` 当前列（后端已建）

```
id              BIGINT
name            VARCHAR(64)
url             VARCHAR(255)
avatar_url      VARCHAR(255)
description     VARCHAR(255)    一句话（单中文，按后端决定不三语）
sort_order      INT
status          TINYINT         1=显示 / 2=隐藏（推测，需后端确认枚举值）
+ 8 列审计
```

### 2.5 Aurora `site.json` 字段 → 后端表映射

#### ✅ 后端已有，前端直接消费

| Aurora 字段（site.json） | 后端字段 / 表 |
|---|---|
| `site.title` | `t_site_config.site_title_{zh,ja,en}` |
| `site.subtitle` | `t_site_config.site_subtitle_{zh,ja,en}` |
| `site.author` | `t_user_info.nickname` |
| `site.avatar` | `t_user_info.avatar_url` |
| `site.social[]` | `t_user_info.{github_url, twitter_url, linkedin_url, zhihu_url, qiita_url, juejin_url}` |
| `site.beian` | `t_site_config.icp_no` |
| 关于页正文 | `t_site_config.about_md_{zh,ja,en}` |
| logo / favicon | `t_site_config.logo_url / favicon_url` |
| Spotify | `t_site_config.spotify_playlist_id` |
| 友链 | `t_friend_link` 整表 |

#### 🆕 后端目前没有，**前端需要后端补**

| Aurora 字段 | 当前 mock 值 | 建议后端补到 | 优先级 |
|---|---|---|---|
| `theme.gradient.color_1` | `#06b6d4` | `t_site_config.theme_gradient_color_1 VARCHAR(16)` | 🔴 高（影响视觉） |
| `theme.gradient.color_2` | `#6366f1` | `theme_gradient_color_2 VARCHAR(16)` | 🔴 高 |
| `theme.gradient.color_3` | `#8b5cf6` | `theme_gradient_color_3 VARCHAR(16)` | 🔴 高 |
| `theme.gradient.header_filter_cover` | bool | `theme_header_filter_cover TINYINT` | 🟡 中 |
| `theme.profile_shape` | `diamond` | `theme_profile_shape VARCHAR(16)` | 🟡 中 |
| `theme.feature`（首页轮播开关） | bool | `theme_feature_enabled TINYINT` | 🟡 中 |
| `theme.code_blocks.macStyle` | bool | `theme_code_mac_style TINYINT` | 🟢 低 |
| `theme.tags_in_post` | bool | `theme_tags_in_post TINYINT` | 🟢 低 |
| `theme.site_state`（侧栏统计） | bool | `theme_site_state TINYINT` | 🟢 低 |
| `site.police_beian` | 公安备案号 | `t_site_config.police_beian_no VARCHAR(64)` | 🟡 中（中国大陆部署用） |
| `site.description` | SEO meta | `t_site_config.site_desc_{zh,ja,en} VARCHAR(255)` | 🟡 中 |
| `plugins.waline.serverURL` | Waline 服务 URL | `t_site_config.waline_server_url VARCHAR(255)` | 🟡 中 |
| `plugins.prismjs.theme` | `tomorrow` | `t_site_config.prism_theme VARCHAR(32)` | 🟢 低 |
| `plugins.mermaid.enable` | bool | `t_site_config.mermaid_enabled TINYINT` | 🟢 低 |

> 替代方案：把所有 `theme_*` 字段合成一列 `theme_json JSON`，前端读完反序列化。**后端可酌情选择**——前端 adapter 层两种都能吃。

#### 🤷 后端已明确"不进 DB"的项

| 项 | 后端决定 | 前端怎么办 |
|---|---|---|
| 导航菜单 `theme.menu[]` | feature-inventory ⑩：**前端静态路由更简单** | 写死在 `src/router/index.ts` 或 `App.vue` 配置常量 |
| 页脚链接 `footer.navigation` | 暂未设计；可比照菜单走前端静态 | 同上 |
| 圆角 / 动画时长 / Sticky 头高 81px | 视觉细节 | 写在 Tailwind config / SCSS 变量 |

### 2.6 前端要消费的接口（按后端 `api-contract` 约定）

→ 这部分接口**目前后端 `api-contract/endpoints-*.md` 都还是骨架**，下面是**前端这边需要的契约，等真到对接阶段把这段拷给后端**。

#### 2.6.1 公开站点配置

```
GET /api/public/site/config
Auth: 无
Response: {
  "code": "00000",
  "msg": "success",
  "data": {
    "siteTitleZh": "...", "siteTitleJa": null, "siteTitleEn": null,
    "siteSubtitleZh": "...", "siteSubtitleJa": null, "siteSubtitleEn": null,
    "siteDescZh": "...", "siteDescJa": null, "siteDescEn": null,   // 🆕
    "aboutMdZh": "...", "aboutMdJa": null, "aboutMdEn": null,
    "logoUrl": "...", "faviconUrl": "...",
    "icpNo": "...", "policeBeianNo": null,                          // 🆕
    "spotifyPlaylistId": "...",
    "themeGradientColor1": "#06b6d4",                               // 🆕
    "themeGradientColor2": "#6366f1",                               // 🆕
    "themeGradientColor3": "#8b5cf6",                               // 🆕
    "themeHeaderFilterCover": true,                                 // 🆕
    "themeProfileShape": "diamond",                                 // 🆕
    "themeFeatureEnabled": true,                                    // 🆕
    "themeCodeMacStyle": true,                                      // 🆕
    "themeTagsInPost": true,                                        // 🆕
    "themeSiteState": true,                                         // 🆕
    "walineServerUrl": "...",                                       // 🆕
    "prismTheme": "tomorrow",                                       // 🆕
    "mermaidEnabled": false                                         // 🆕
  }
}
```

期望：一次性返回全量；前端 `useAppStore.fetchConfig` 在 `configReady` 之前阻塞渲染（已有逻辑）。

#### 2.6.2 公开作者卡

```
GET /api/public/users/{userId}/profile
Auth: 无
Response.data: {
  "userId": 1,
  "nickname": "...",
  "avatarUrl": "...",
  "bioZh": "...", "bioJa": null, "bioEn": null,
  "location": "Tokyo",
  "website": "...",
  "emailPublic": "...",  // 可选，前端按 null 判断显不显示
  "socials": {            // 后端可以把 6 个 *_url 收进 socials 对象，或者直接平铺
    "githubUrl": "...",
    "twitterUrl": "...",
    "linkedinUrl": "...",
    "zhihuUrl": null,
    "qiitaUrl": null,
    "juejinUrl": null
  }
}
```

> Aurora 现在的 mock 是 `/api/authors/blog-author.json`，对应改成 `GET /api/public/users/1/profile`（单用户博客 userId=1，admin 注册时建）。

#### 2.6.3 公开友链

```
GET /api/public/friend-links
Auth: 无
Response.data: [
  { "id": 1, "name": "...", "url": "...", "avatarUrl": "...", "description": "...", "sortOrder": 0 }
]
（后端按 status=1 过滤，按 sortOrder 升序）
```

#### 2.6.4 公开文章列表 / 详情（已有规划，等后端落契约）

```
GET /api/public/articles?page=1&size=20&categoryId=&tagId=&keyword=
GET /api/public/articles/{id}
GET /api/public/articles/by-slug/{slug}
```

字段：见 §1.3，三语副本字段 + 单中文 body。

#### 2.6.5 公开分类 / 标签

```
GET /api/public/categories     → 返回 [{id, nameZh, nameJa, nameEn, slug, sortOrder}]
GET /api/public/tags           → 同结构
```

#### 2.6.6 后台管理（admin 端用，与本前端无关但列在这里给参考）

```
GET  /api/admin/site/config              ADMIN+DEMO
PUT  /api/admin/site/config              ADMIN（替换式更新）
GET  /api/admin/users/current            ADMIN+DEMO
PUT  /api/admin/users/current            ADMIN+DEMO（改自己资料）
GET  /api/admin/friend-links             ADMIN+DEMO（含隐藏的）
POST /api/admin/friend-links             ADMIN
PUT  /api/admin/friend-links/{id}        ADMIN
DELETE /api/admin/friend-links/{id}      ADMIN
```

### 2.7 缓存 / 失效

- 站点配置：前端首屏 fetch 一次进 Pinia，整个 session 不再拉
- 主题色变更：后台改完后**用户下次开页刷新即生效**（不做 push）
- 多语言：路径前缀已经天然区分 CDN 缓存键，**后端不需要发 `Vary: Accept-Language`**
- 业务列表/详情：前端不主动缓存，依赖浏览器 HTTP 缓存

---

## 3. 落地节奏（衔接 decisions-and-roadmap.md 的 phase 表）

| Phase | 本文档对应工作 | 阻塞 |
|---|---|---|
| Phase 1 脱钩 Hexo | — | — |
| Phase 2 删死依赖 | — | — |
| Phase 3 依赖升级 | — | — |
| Phase 4 `<script setup>` 迁移 | — | — |
| Phase 5 功能裁剪 | — | — |
| Phase 6 i18n 改造 | i18n 文件层调整（重命名 + 加 ja.json + 删 zh-TW） + 路由 + guard | 无 |
| **Phase 7 V2 后端对接** | adapter；`/site.json` → `/api/public/site/config`；接口逐个换；业务字段三语切换 | **需后端先按 §4 完成 schema 扩 + 接口落 contract** |
| Phase 8 v1 功能迁入 | 友链管理（已规划在后台 admin 端） | 无 |

---

## 4. 本前端对后台的需求清单（真到对接时拷给后端）

> 当前先记着，**不主动找后端**。等 Phase 7 V2 后端对接启动前提一次。

### 4.1 schema 扩列（往 `t_site_config` 加字段，全部 NULL 允许）

```sql
ALTER TABLE t_site_config
  ADD COLUMN site_desc_zh           VARCHAR(255)  NULL  COMMENT 'SEO 描述（中）',
  ADD COLUMN site_desc_ja           VARCHAR(255)  NULL  COMMENT 'SEO 描述（日）',
  ADD COLUMN site_desc_en           VARCHAR(255)  NULL  COMMENT 'SEO 描述（英）',
  ADD COLUMN police_beian_no        VARCHAR(64)   NULL  COMMENT '公安备案号',
  ADD COLUMN theme_gradient_color_1 VARCHAR(16)   NULL  COMMENT '主题渐变色 1（如 #06b6d4）',
  ADD COLUMN theme_gradient_color_2 VARCHAR(16)   NULL  COMMENT '主题渐变色 2',
  ADD COLUMN theme_gradient_color_3 VARCHAR(16)   NULL  COMMENT '主题渐变色 3',
  ADD COLUMN theme_header_filter_cover TINYINT    NOT NULL DEFAULT 1 COMMENT 'Header 毛玻璃 0/1',
  ADD COLUMN theme_profile_shape    VARCHAR(16)   NOT NULL DEFAULT 'diamond' COMMENT 'diamond/circle/square',
  ADD COLUMN theme_feature_enabled  TINYINT       NOT NULL DEFAULT 1 COMMENT '首页轮播开关',
  ADD COLUMN theme_code_mac_style   TINYINT       NOT NULL DEFAULT 1 COMMENT '代码块 Mac 风格',
  ADD COLUMN theme_tags_in_post     TINYINT       NOT NULL DEFAULT 1 COMMENT '文章内显示标签',
  ADD COLUMN theme_site_state       TINYINT       NOT NULL DEFAULT 1 COMMENT '侧栏站点状态卡',
  ADD COLUMN waline_server_url      VARCHAR(255)  NULL  COMMENT 'Waline 评论服务地址',
  ADD COLUMN prism_theme            VARCHAR(32)   NULL DEFAULT 'tomorrow' COMMENT 'PrismJS 主题',
  ADD COLUMN mermaid_enabled        TINYINT       NOT NULL DEFAULT 0 COMMENT '是否启用 Mermaid';
```

→ 让后端走他们的 schema migration 流程（`docs/sql/` 下的 V2 迁移脚本）。

### 4.2 `api-contract` 落 6 个公开接口契约

按 §2.6.1–§2.6.5 的样子写到 `api-contract/endpoints-public.md`：
- `GET /api/public/site/config`
- `GET /api/public/users/{userId}/profile`
- `GET /api/public/friend-links`
- `GET /api/public/articles` + `/{id}` + `/by-slug/{slug}`
- `GET /api/public/categories`
- `GET /api/public/tags`

业务字段**三语副本要在一次响应里全返回**，不要按 `?lang=` 单语返回——前端切语言不刷接口。

### 4.3 `frontend-user/README.md` 修订

把"前后台统一"那句改成"数据契约统一，UI 各自（前台 Aurora + Tailwind，后台 Element Plus）"。

### 4.4 评论 / 友链关联 admin 端

友链 admin CRUD 接口（§2.6.6 后 4 个）落到 `api-contract/endpoints-admin.md`。

### 4.5 单用户博客的约定

本博客是**单 admin** 模式（feature-inventory ⑤可见）。需要后端确认：
- 站点作者 = `t_user_auth` 表里 role=ADMIN 的那一行（如果只有一个，userId 应固定 1）
- 公开作者卡接口建议加 `GET /api/public/users/owner/profile` 别名，避免前端硬编码 userId

---

## 5. 一句话总结

**前端继续 Aurora（Vue + Tailwind）；i18n 留着扩成 zh/ja/en + 路径前缀；可配置内容全部走 `t_site_config` + `t_user_info` + `t_friend_link`，由后台 Element Plus 管理页编辑——前端只读，不维护配置 UI。后端要补 16 个字段 + 落 6 个公开接口契约，到 Phase 7 V2 后端对接前提一次。**

---

## 6. 与后台已知冲突清单

> 真到 Phase 7 V2 后端对接时**逐条对账**。每条标了**处理方向**：吃掉（adapter 解决）/ 协商（要后端配合）/ 取舍（前端单方面决策）。

### 6.1 🔴 架构级冲突（影响重构方向）

| # | Aurora 现状 | 后端文档定调 | 来源 | 处理方向 |
|---|---|---|---|---|
| C1 | 4 套评论插件（gitalk/valine/twikoo/waline），已选 Waline | 自研评论系统（`t_comment` 带审计状态/树形回复/计数冗余） | schema §3.8、feature-inventory ④ | **取舍**：保持 Waline，后端 `t_comment` 表闲置或仅做留言板用；个人博客避免重造审计/反垃圾 |
| C2 | 路由全用 slug：`/post/hello-world` | id-led，slug 是可选后缀：`/{lang}/posts/{id}` 或 `/{lang}/posts/{id}-{slug}` | ADR-0016、R2 #14 | **协商 + 改前端**：Phase 7 V2 后端对接时改 router 为 id-led，slug 留 SEO 后缀；slug 后端不强制唯一 |
| C3 | Aurora = Vue + Tailwind + 自研组件 | `frontend-user/README.md`：Vue + Element Plus（前后台统一） | frontend-user/README.md | **协商**：让后端把"前后台统一"改成"数据契约统一，UI 各自"（见 §0、§4.3） |

### 6.2 🟡 数据结构 / 字段冲突

| # | Aurora 现状 | 后端文档定调 | 处理方向 |
|---|---|---|---|
| C4 | i18n locale = `zh-CN / en / zh-TW` | `zh / ja / en` | **改前端**（Phase 6 i18n 改造） |
| C5 | `/api/authors/:slug.json` 多作者设计 | 单 ADMIN 模式（`t_user_auth` role=ADMIN 通常一行） | **改前端**：作者卡固定读单用户（§2.6.2 建议 `owner` 别名） |
| C6 | "关于"页走 `/api/pages/about/index.json` 自定义页机制 | `t_site_config.about_md_{zh,ja,en}` 三语字段直存 | **改前端**：about.vue 换取数路径，走 `/api/public/site/config` |
| C7 | UV/PV 走第三方 `theme.uv_pv_counter`（不蒜子之类） | 自研 `t_page_view` + `t_page_view_daily` | **改前端**：删第三方计数器（02-H14 已标 🔴），改读后端统计接口 |
| C8 | 时间显示按浏览器本地时区 | ADR-0018：**Asia/Tokyo**；后端返回 ISO 字符串无 `Z` | **吃掉**：`utils/index.ts` 的 `formatTime` 统一按 JST 字面量解析；新建 `parseJST(s)` 辅助 |
| C9 | 友链描述支持多行 / 富文本（mock 里随意） | `t_friend_link.description` VARCHAR(255) 单中文 | **改前端**：友链 UI 限制 255 字单行；不三语 |
| C10 | 作者卡两字段：`site.author`（大字）+ `site.nick`（小字） | `t_user_info.nickname` 单字段 | **改前端**：Logo.vue / Profile.vue 合并成一行显示 |

### 6.3 🟢 小调整 / 取舍

| # | Aurora 现状 | 后端文档定调 | 处理方向 |
|---|---|---|---|
| C11 | 搜索：前端 lunr.js + `search.json` 索引 | 后端有 `t_article` 全文索引能力，未明确暴露搜索 API | **取舍**：Phase 7 前继续前端搜索；接后端后 search.json 改成 `?lang=` 分语种生成，或换后端 `/api/public/articles?keyword=` |
| C12 | 文章 cover 走 mock 静态路径 | `t_attachment` 表 + storage_type 抽象 | **吃掉**：adapter 把 `coverAttachmentId` → `t_attachment.public_url`；前端字段名不变 |
| C13 | 导航菜单读 `theme.menu` 配置 | feature-inventory ⑩：前端静态路由更简单（后端不存菜单） | **改前端**：菜单写死在 `src/router/index.ts` 或常量文件；ThemeConfig 里删 `menu` |
| C14 | `first_screen` 配置（伪功能，源码 0 引用） | 后端无 | **吃掉**：配置项直接删（已在 02-H16 标 🔴） |

### 6.4 处理时机一览

| Phase | 要解决的冲突 |
|---|---|
| Phase 5 功能裁剪 | C7（UV/PV）、C9（友链 UI） |
| Phase 6 i18n 改造 | C4、C13、C14 |
| **Phase 7 V2 后端对接** | C2、C5、C6、C8、C10、C11、C12 一起搞 |
| 跨阶段（先拍板）| **C1、C3 现在就要定**——影响 Phase 5 删评论插件的范围、是否引入 EP |

→ 真开工前把这张表拷给后端 review 一遍，免得双方理解错位。
