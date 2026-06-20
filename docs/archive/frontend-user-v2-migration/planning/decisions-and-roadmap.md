# 架构决策与路线图（v2）

> **本版基于代码现状盘点 + 用户裁决重写**（2026-06-09）。Phase 0-3b 已实战完成（沙盒 + 文档双通），Phase 4-9 重新规划。
>
> **新核心顺序原则**：**先删后改**。
> 之前的版本把 `<script setup>` 迁移（61 个文件）排在功能裁剪前面，但盘点发现有相当多组件/页面注定要删——先迁移再删就是白干。

---

## 一、决议状态总览

| # | 决策 | 状态 | 备注 |
|---|---|---|---|
| Q1 | Vite 4 → 5 | ✅ 已做 | Phase 3b.1 完成 |
| Q2 | Tailwind 3 → 4 | ⏸ 保留 v3 | Phase 9a 可选 |
| Q3 | Vue / TS / Pinia / router 小升级 | ✅ 已做 | Phase 3a 完成 |
| Q4 | 引入 Vitest | ❌ 不做 | 个人博客，肉眼回归 |
| Q5 | 评论方案 | ✅ **自研对接 V2 后端** | 删 6 个第三方插件，重写 Comment.vue |
| Q6 | i18n 改 zh/ja/en + 路径前缀 | ✅ 已定 | Phase 6 落地 |
| Q7 | 路由：保持文件路由 | ✅ 已定 | `vite-plugin-pages` 继续用 |
| Q8 | `<script setup>` 集中迁移 | ✅ 已定 | **改放 Phase 5**（删完之后） |

---

## 二、代码现状盘点裁决（2026-06-09）

### 删除清单

| 项 | 理由 | 后续 |
|---|---|---|
| `src/components/Dia.vue` + `src/stores/dia.ts` + `src/utils/aurora-dia/` | Aurora 主题看板娘，v2 不要二次元元素 | ✅ Phase 4 已删 |
| `src/components/Navigator.vue` | 已在 App.vue 注释，0 实际引用 | ✅ Phase 4 已删 |
| `src/stores/navigator.ts`（**仅 Navigator 弹窗那一半**） | 三合一 store（弹窗 + 移动菜单 + 阅读进度），只切弹窗 3 个 state/action | ✅ Phase 4 已切 |
| `src/utils/comments/` 6 文件 + `useCommentPlugin.ts` + `Comment.vue` | v2 自建评论 API；删除会炸 7 个消费者文件 | **推迟到 Phase 7g**（与 Comment.vue 重写一起做） |
| `src/components/Link/LinkBoxTitle.vue` | 全仓 0 引用 | ✅ Phase 4 已删 |
| `src/stores/routers.ts` | 0 引用 + TS 5 个报错 | ✅ Phase 4 已删 |
| `aurora_bot` 字段（`ThemeConfig.class.ts`） | Dia 配置容器 | ✅ Phase 4 已删 |

### 保留 + 对接清单

| 项 | v2 后端契约 |
|---|---|
| `pages/about.vue` | `GET /api/site` → 读 `about_md_zh/ja/en`，前端 markdown 渲染 |
| `pages/links.vue` + `components/Link/*`（去 LinkBoxTitle） | `GET /api/friend-links`（v2 `t_friend_link`） |
| `pages/category.vue`（当前空壳） | **Phase 7 补全**，参考 tags 页结构。对接 `GET /api/categories` + `GET /api/categories/{id}/articles` |
| `pages/tags.vue` | `GET /api/tags` + `GET /api/tags/{id}/articles` |
| `pages/archives.vue` / `index.vue` / `post/[slug].vue` / `page/[slug].vue` / `post/search/index.vue` / `[...all].vue` | 文章相关接口 |
| Lightbox（`vue-easy-lightbox` + `useLightBox.ts` + `stores/lightbox`） | 文章图片点击放大，开箱用 |
| Dropdown（`components/Dropdown/*` + `stores/dropdown.ts`） | 语言切换器在用，必须保留 |

### v2 后端实现状态参考（来自 `c:\tyb\My-Blog\docs\`）

| 模块 | SQL DDL | Java 代码 |
|---|---|---|
| `t_article` / `t_article_tag` / `t_category` / `t_tag` | ✅ 已冻结 | ❌ 待开发 |
| `t_friend_link` / `t_friend_link_application` | ✅ 已冻结 | ❌ 待开发 |
| `t_site_config`（含 `about_md_*`） | ✅ 已冻结 | ❌ 待开发 |
| Comment 模块 | ✅ 已规划 | ❌ 待开发 |

⚠️ **Phase 7 启动前，后端对应接口需联调可用**。具体字段以 `c:\tyb\My-Blog\docs\sql\V1__init.sql` 为准。

---

## 三、路线图（每个 phase 只做一件事）

| Phase | 目标 | 预估 | 状态 |
|---|---|---|---|
| **0** | 本地预览环境 | 半天 | ✅ 完成 |
| **1** | 脱钩 Hexo | 1 天 | ✅ 完成 |
| **2** | 删死依赖 | 半天 | ✅ 完成 |
| **3a** | 小升级（同 major） | 半天 | ✅ 完成 |
| **3b** | 大升级（Vite 5 / ESLint 9） | 1 天 | ✅ 完成 |
| **4** | **死代码一刀切**（Dia / Navigator 弹窗 / LinkBoxTitle / routers store / aurora_bot） | 半天 | ✅ 完成（沙盒 -23 KB / -4.8%） |
| **5a** | `<script setup>` 迁移：pages | 半天 | ⏳ 下一步 |
| **5b** | `<script setup>` 迁移：components | 1-2 天 | |
| **5c** | `<script setup>` 迁移：app shell | 半天 | |
| **6a** | i18n 结构改造（zh-CN/zh-TW/en → zh/ja/en + 路径前缀 + 字体） | 1 天 | |
| **6b** | vue-i18n 9 → 11（可选） | 半天 | |
| **7a** | request.ts 重写 + adapter 基础设施 + `/api/site` 打通 | 半天 | |
| **7b** | 路由改 id-led（`/{lang}/posts/{id}`） | 半天 | |
| **7c** | 文章列表 / 详情接口对接 + 三语字段 | 1 天 | |
| **7d** | 分类 + 标签接口对接 | 半天 | |
| **7e** | 友链接口对接 | 半天 | |
| **7f** | About 页对接 `site_config.about_md_*` | 半天 | |
| **7g** | Comment.vue 重写 + 对接 v2 评论 API | 1 天 | |
| **7h** | category 页从空壳补全（仿 tags 页） | 半天 | |
| **7i** | `HexoConfig` → `SiteConfig` 类型重命名 | 1 小时 | |
| **7j** | JST 时区处理（`parseJST`） | 1 小时 | |
| **8** | v1 ↔ V2 功能对照盘点（元 phase） | — | |
| **9a** | Tailwind 3 → 4（可选） | 1-2 天 | |
| **9b** | SEO（meta / OG / sitemap.xml） | 半天 | |
| **9c** | 性能（Lighthouse ≥ 90） | 半天 | |
| **9d** | 真机测试（iOS / Android / 微信） | 半天 | |
| **9e** | 部署（域名 / HTTPS / CI/CD） | 1 天 | |
| **9f** | 备份机制 | 半天 | |

**总顺序逻辑**：
- **删 → 迁 → i18n → 对接 → 收尾**
- Phase 4 删完，Phase 5 迁移的工作量直接缩 ~15-20%（少改 Dia / Navigator / Comments 等注定要删的文件）
- Phase 6 在对接前做，因为 Phase 7 的 adapter 要按 locale 取 `*_zh/*_ja/*_en` 字段
- Phase 7 是"接口替换"集中战场，子步多但每步小
- Phase 8 是 v1 → v2 的"功能搬迁"元规划，等数据链路稳了再启动
- Phase 9 上线前打磨，子步顺序灵活

---

## 四、Phase 4（死代码一刀切）✅ 已完成

**实际范围**（沙盒已验证，详见 [`phase-4-dead-code-cut/`](../phase-4-dead-code-cut/README.md)）：

| 操作 | 文件 / 目录 |
|---|---|
| 删 | `src/components/Dia.vue` |
| 删 | `src/stores/dia.ts` |
| 删整目录 | `src/utils/aurora-dia/`（index.ts + 3 个 messages json） |
| 删 | `src/components/Navigator.vue` |
| 删 | `src/components/Link/LinkBoxTitle.vue` |
| 删 | `src/stores/routers.ts` |
| 改 | `src/App.vue`：删 `<Dia>` 标签 + `<!-- <Navigator /> -->` 注释 + Dia/Navigator import + components 注册 |
| 改 | `src/models/ThemeConfig.class.ts`：删 `aurora_bot`（interface + class 字段两处） |
| 改 | `src/stores/navigator.ts`：**保留文件**，只切 Navigator 弹窗的 3 个 state/action（`openNavigator` / `toggleOpenNavigator` / `setOpenNavigator`），保留移动菜单 + 阅读进度两半 |
| 改 | `src/components/MobileMenu.vue`：删 `navigatorStore.setOpenNavigator(false)` 一行 |

**为何 `utils/comments/` 没在本 phase 删**：6 个第三方插件经 `useCommentPlugin.ts` 被 7 个文件引用（Comment.vue / post / page / links / RecentComment / PostStats / PageContent / FooterContainer）；物理删会炸整个 build 链路。**推迟到 Phase 7g**（与 Comment.vue 重写一起做）。

**实际成果**：13 文件 / 1768 行净删；main bundle 476 → 452.92 KB（-23 KB / -4.8%）；TS 错误 7 → 2（剩 2 个 pre-existing LoadingSkeleton）。

**沙盒 tag**：`pre-dead-code-cut` / `phase-4-done`。

**踩坑记录**：见 [`phase-4-dead-code-cut/04-troubleshooting.md`](../phase-4-dead-code-cut/04-troubleshooting.md) — Case A（navigator store 三合一误判，差点全删导致 build 失败）+ Case B（commitlint conventional 格式拦截）。后者影响后续所有 phase：**commit subject 必须是 `chore(phase-Nx): xxx` 格式且 header ≤ 100 字**。

---

## 五、Phase 5（`<script setup>` 迁移）

**目标**：61 个 `.vue` 文件（Phase 4 删完后约 **55 个**）从 `defineComponent({ setup() })` 改写到 `<script setup>`。

**入口条件**：Phase 4 收口；Vue 已 ≥ 3.5（Phase 3a 已满足）；打 tag `pre-script-setup`。

**模式化转换规则**：

原写法：

```vue
<script lang="ts">
import { defineComponent } from 'vue'
export default defineComponent({
  components: { ... },
  props: { ... },
  emits: [ ... ],
  setup(props, { emit }) {
    // body
    return { foo, bar }
  }
})
</script>
```

新写法：

```vue
<script setup lang="ts">
const props = defineProps<{ ... }>()
const emit = defineEmits<{ ... }>()
// body（不需要 return）
</script>
```

**子步**：

| Step | 范围 | 文件数 | 节奏 |
|---|---|---|---|
| **5a** | `src/pages/**` | ~10 | 一页一 commit，浏览器回归 |
| **5b** | `src/components/**`（除 Header / Footer） | ~40（删后） | 按子目录批次，每目录 commit + 冒烟 |
| **5c** | App shell：App.vue + Header/* + Footer/* + main.ts | ~6 | 最后做，最敏感 |

**5b 建议顺序**：叶子组件（Card / Tag / Toggle / Skeleton）→ 中间层（Sidebar 子组件 / Statistic）→ 容器（PageContent / Comment 壳 / SearchModal）。

**Phase 3b 残留 22 个 lint errors + 8 warnings 在 Phase 5 顺手清掉**（多数是 `no-unused-vars` / `no-console`，迁移期间天然就改了）。

**总出口验收**（5c 完成后）：

```bash
grep -r 'defineComponent' src/                  # 应为空
./node_modules/.bin/vite                        # 所有页面正常
./node_modules/.bin/vite build                  # 通过
./node_modules/.bin/eslint .                    # 错误数显著下降
```

**回退**：5a/5b/5c 各自 tag（`phase-5a-done` / `phase-5b-done` / `phase-5c-done`）。

---

## 六、Phase 6（i18n 改造）

### 6a · 结构改造（1 天）

**入口条件**：Phase 5 收口；打 tag `pre-i18n`。

| 操作 | 文件 |
|---|---|
| 删 | `src/locales/languages/zh-TW.json` |
| 重命名 | `zh-CN.json` → `zh.json` |
| 新增 | `ja.json`（先复制 zh，逐条翻译） |
| 改 | `src/locales/index.ts`：`locale: 'zh'`、`fallbackLocale: 'zh'`、合法值 `['zh','ja','en']` |
| 改 | `src/stores/app.ts`：默认 `'zh'`；`setDefaultLocale` 用 `navigator.language` + `localStorage.aurora.lang` |
| 改 | `src/models/ThemeConfig.class.ts`：`Locales` 枚举改 `zh/ja/en` |
| 改 | `src/components/Header/src/Controls.vue`：Dropdown 三项 |
| 改 | `src/router/index.ts`：加 `/:lang(zh|ja|en)?` 段 |
| 改 | `src/router/guard.ts`：beforeEach 同步 `i18n.global.locale` |
| 新增 | `src/styles/i18n-fonts.scss`：`:lang(ja) { font-family: 'Noto Sans JP', ... }` |

> ⚠️ 业务字段（标题/分类名/友链等）按 locale 取 `*_zh/*_ja/*_en` 留到 Phase 7c-7f 跟接口对接一起做，本子步只完成 UI 文案 + 路由 + 字体。

**出口验收**：

```bash
grep -r "zh-TW" src/                            # 应为空
# 浏览器三语切换正常，URL 前缀正确，日语下字体切换正确
```

### 6b · vue-i18n 9 → 11（可选，半天）

可推迟到 Phase 9 前任意时机；不做就停在 v9。

```bash
pnpm up vue-i18n@~11
# 改 createI18n({ legacy: false, ... })
# 把模板里散用的 $t 在 setup 内改用 useI18n()
```

---

## 七、Phase 7（V2 后端对接）

**总入口条件**：Phase 6a 收口；V2 后端对应接口已上联调；打 tag `pre-v2-api`。

### 7a · 基础设施 + `/api/site` 打通（半天）

1. `.env.development` 设 `VITE_APP_BASE_API=/api`
2. `vite.config.mjs` 启用 proxy 指向 V2 后端
3. `src/utils/request.ts`：
   - baseURL 改 `VITE_APP_BASE_API`
   - 响应拦截器适配 V2 `{ code, msg, data }`
   - 401/403 跳转
4. 新增 `src/api/adapters/` 目录
5. `/api/site` 接口打通：编写 `adaptV2SiteConfig`，删 `public/api/site.json`

**验收**：站点配置走真后端，首页 Header / Footer 渲染正常。

### 7b · 路由改 id-led（半天）

| 改 | 内容 |
|---|---|
| `src/router` | `/post/:slug` → `/:lang/posts/:id`；`/page/:slug` → `/:lang/pages/:id` |
| `src/pages/post/[slug].vue` | 改名 `[id].vue`，读 `route.params.id` |
| 所有 `<router-link>` | 改用 `{ name: 'post', params: { id, lang } }` |

### 7c · 文章接口（列表 / 详情 / 分页） + 三语（1 天）

```ts
// src/api/adapters/article.ts
export function adaptV2Article(raw: any, lang: 'zh'|'ja'|'en'): Article {
  return {
    uid: raw.id,
    title: raw[`title_${lang}`] ?? raw.title_zh,
    slug: raw.slug,
    date: raw.publishedAt,
    excerpt: raw[`summary_${lang}`] ?? raw.summary_zh,
    // ...
  }
}
```

- 删 `public/api/posts*.json` mock
- 调用方传入当前 i18n locale
- Fallback：日/英缺失 → 显示中文原文

### 7d · 分类 + 标签接口（半天）

- `GET /api/categories` + `GET /api/categories/{id}/articles`
- `GET /api/tags` + `GET /api/tags/{id}/articles`
- adapters：`adaptV2Category` / `adaptV2Tag`（注意 `name_zh/ja/en` 三语）
- 删 `public/api/tag.json` / `category.json`

### 7e · 友链接口（半天）

- `GET /api/friend-links` → `adaptV2FriendLink`
- 字段：name / url / avatar / description / sort_order / status
- 删 `public/api/links.json`

### 7f · About 页接 site_config.about_md_*（半天）

- About 页改读 site config 的 `about_md_{lang}`
- markdown 渲染（沿用现有 markdown 渲染链路）
- 删 `public/api/about.json`

### 7g · Comment.vue 重写 + 对接 v2 评论 API（1 天）

- v2 后端评论 API 上线后启动（具体 endpoint 待 v2 backend 实现）
- 写一个全新的 Comment.vue：列表 / 提交表单 / 头像（Gravatar 邮箱哈希自己实现，~10 行）
- 删 Phase 4 留下的"空壳"
- 删 `src/models/ThemeConfig.class.ts` 残留的评论插件配置项

### 7h · category 页从空壳补全（半天）

- 当前 `src/pages/category.vue` 是占位
- 仿 `pages/tags.vue` 结构：列分类卡片 → 点击进入 `/:lang/categories/:id`
- 配套新增分类详情页（或复用列表组件）

### 7i · `HexoConfig` → `SiteConfig` 类型重命名（1 小时）

```bash
grep -rl "HexoConfig" src/ | xargs sed -i 's/HexoConfig/SiteConfig/g'
git mv src/models/HexoConfig.class.ts src/models/SiteConfig.class.ts
```

验收：`grep -r "Hexo" src/` 0 实质引用；`tsc --noEmit` 通过。

### 7j · JST 时区处理（1 小时）

- `src/utils/index.ts` 新增 `parseJST(iso: string): Date`
- `formatTime` 内部用 `parseJST`
- 验收：任意时区浏览器显示同一文章发布时间一致（JST）

**Phase 7 总出口验收**：
- [ ] Network 面板所有请求 2xx
- [ ] 错误态 / 空态 / 加载态 三类 UI 验过
- [ ] 文章详情、列表分页、标签筛选、搜索、评论、友链、关于我 全链路打通
- [ ] 三语切换业务文案跟着切
- [ ] `public/api/` 已空（或仅留 README）

---

## 八、Phase 8（v1 ↔ V2 元盘点）

**性质**：元 phase——只产出 `planning/v1-to-v2-migration.md`，把 v1 (`c:\tyb\My-Blog\MyBlog-vue\MyBlog-blog\`) 里值得迁的功能拆成 8.1 / 8.2 / 8.3 ... 子任务，再各自单 phase。

**入口条件**：Phase 7 收口（V2 数据链路稳定）。

| 列 | 含义 |
|---|---|
| v1 功能 | `MyBlog-blog/...` 里的功能名 |
| 价值评估 | 必留 / 锦上添花 / 弃用 |
| Aurora 现状 | 已有等价 / 需自建 / 概念冲突 |
| 工作量 | 半天 / 1 天 / 3 天 |
| 子任务 ID | 8.1 / 8.2 / ... |

⚠️ **UI 不可直接搬**：v1 = Vue CLI + Element-Plus，v2 = Vite + Aurora + Tailwind。组件必须重写，视觉以 Aurora 为准。

典型候选（清单等盘点产出）：
- 友链申请表单
- 留言板
- 后台管理入口
- 自定义动效 / 小组件

---

## 九、Phase 9（发布前打磨）

子步顺序灵活，按上线节奏选。

### 9a · Tailwind 3 → 4（1-2 天，可跳过）

```bash
pnpm remove tailwindcss postcss autoprefixer
pnpm add -D tailwindcss@~4 @tailwindcss/postcss
# 改 postcss.config / 全局 CSS 入口 / 删 tailwind.config.js
```

CSS-first 配置（`@import "tailwindcss"` + `@theme {}`），`@apply` 行为略变。

### 9b · SEO（半天）

- meta / Open Graph 全检查
- `sitemap.xml` 生成（三语路径）
- `robots.txt`

### 9c · 性能（Lighthouse ≥ 90，半天）

- 路由级 code splitting（已内置）
- 图片懒加载（`vue3-lazyload` 已装）
- 字体子集化（中/日字体只加载使用字符）
- 关键 CSS 内联

### 9d · 真机测试（半天）

- iOS Safari（最近两个大版本）
- Android Chrome
- 微信内嵌浏览器（重点：分享、字体回退、安全区域）

### 9e · 部署（1 天）

- `dist/` 部署方案
- 域名 + HTTPS
- CI/CD（GitHub Actions：push master → 自动 build + 部署）

### 9f · 备份（半天）

- V2 后端 DB 定期备份
- 静态资源镜像
- 恢复演练

---

## 十、风险与依赖

| 风险 | 触发 phase | 应对 |
|---|---|---|
| Phase 4 误删活代码 | Phase 4 | ✅ 已规避（沙盒踩到 navigator 三合一陷阱，文档已记录；comments 推迟到 7g） |
| 55 个文件 `<script setup>` 迁移引入隐 bug | Phase 5a/5b/5c | 子步分批 commit；5a 先在 pages 验证模式 |
| V2 后端接口字段与前端 model 不匹配 | Phase 7a-7g | 7a 先打通 site 一个接口验证 adapter 模式；后续复用 |
| V2 评论 API 没就绪卡住 Phase 7g | Phase 7g | 不阻塞其它子步；7g 可推迟到 v2 后端模块上线后做 |
| Tailwind 4 升级与已有 utility 冲突 | Phase 9a | 工程稳定后做；不做就停在 v3 |
| vue-i18n 9 → 11 Legacy API 移除 | Phase 6b | 6a 完成后可选；不做就停在 v9 |

---

## 十一、不在本次重构内的事

- ❌ Tailwind 4（除非 9a 决定做）
- ❌ SSR / Nuxt
- ❌ monorepo / Turborepo
- ❌ 重写文章正文 markdown 渲染
- ❌ 埋点 / A/B 测试
- ❌ PWA
- ❌ 多主题色用户切换（固定 3 色渐变）
