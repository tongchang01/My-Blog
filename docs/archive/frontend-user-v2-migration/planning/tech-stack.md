# 01 · 技术栈与依赖基线

> 上游：`hexo-theme-aurora` v2.5.3（最后发布 2023-11）
> 本文档锁定的是**当前工程的现状事实**。升级目标版本见 [03-decisions-and-roadmap.md](./03-decisions-and-roadmap.md) Q1/Q3。

---

## 1. 运行时与构建工具链

| 组件 | 当前版本 | 状态 | 升级建议 | 备注 |
|---|---|---|---|---|
| Node.js | 实测 v24.16.0 | ✅ 可用 | LTS（v20 / v22）都行 | 上游未声明 `engines.node` |
| 包管理器 | `pnpm`（推荐）/ `npm` | ✅ | — | husky `prepare` 在无 `.git` 时会卡 |
| Vite | **4.4.9** | ⚠️ 偏旧 | → 5.4（Q1=B） | Vite 5 在 2023-11 发布；Vite 6 在 2024-11 |
| Vue | **3.3.4** | ⚠️ | → 3.5.x | 3.5 响应式重写 + `useTemplateRef`、`defineModel` 稳定 |
| TypeScript | **5.1.x** | ⚠️ | → 5.5.x | `const` 类型参数稳定、`satisfies` 完善 |
| Pinia | **2.1.6** | ⚠️ | → 2.2.x | — |
| vue-router | **4.2.4** | ⚠️ | → 4.4.x | — |
| Tailwind CSS | **3.3.3** | ⚠️ | 暂不升 4 | 见 Q2 |
| Sass | 1.66.1 | ✅ | 自然升 | — |

---

## 2. 工程目录结构与依赖关系

### 2.1 顶层结构

```
hexo-theme-aurora-main/
├── public/                  # 静态资源 + mock API
│   ├── api/                 # mock JSON（我们手动加的）
│   └── favicon.ico
├── src/                     # 115 个 .ts/.vue 文件
├── templates/               # hexo 模式专用 HTML 模板（重构要删）
├── data/, source/, layout/  # hexo 产物目录（重构要删）
├── build/                   # 自研脚本：index.js + scripts/config-script.js
├── index.html               # 我们手写的根入口（绕过 html-transformer）
├── vite.config.js           # Vite 配置（已注释 proxy）
├── tailwind.config.js
├── postcss.config.js
├── tsconfig.json
├── .env                     # VITE_APP_PROJECT_TITLE, VITE_APP_BASE_API, VITE_APP_PUBLIC_PATH
├── .env.production
└── package.json
```

### 2.2 `src/` 子目录速查

| 目录 | 内容 | 文件数（粗估） |
|---|---|---|
| `src/api/` | `index.ts` — 13 个 API 函数 | 1 |
| `src/assets/` | 默认封面图、内置素材 | 少量 |
| `src/components/` | 18 个组件目录 + 散文件，**全部 `defineComponent` Options API** | 60+ .vue |
| `src/hooks/` | 4 个 hook：useCommentPlugin / useJumpToEle / useLightBox / usePageTitle | 4 |
| `src/icons/` | SVG 雪碧图源，由 `vite-plugin-svg-icons` 打包 | 一堆 |
| `src/locales/` | i18n：`index.ts` + `languages/{en,zh-CN,zh-TW}.json` | 4 |
| `src/models/` | 7 个 TS 类/类型文件 | 7 |
| `src/pages/` | 文件路由：8 个顶层 + `post/`、`page/` 子目录 | 10+ |
| `src/router/` | `index.ts` + `guard.ts` | 2 |
| `src/stores/` | 14 个 Pinia store | 14 |
| `src/styles/` | 全局 SCSS + 组件样式 | — |
| `src/utils/` | 工具 + `aurora-dia/`（AI 机器人）+ `comments/`（4 套评论 API） | 10+ |
| `src/App.vue` | 根组件，挂 Header / Footer / router-view / MobileMenu / Dia / Lightbox | 1 |
| `src/main.ts` | 应用入口 | 1 |
| `src/settings.ts` | **只有一行有效内容**：`title: env or 'TriDiamond Blog'` | 1 |

### 2.3 启动链路（看清楚谁先谁后）

```
index.html
  └─ <script type="module" src="/src/main.ts"></script>
        │
        ▼
src/main.ts                            # 见 2.4
  ├─ createApp(App)
  ├─ .use(createPinia())               # 14 个 store 注册
  ├─ .use(router)                      # 见 src/router/index.ts
  ├─ .use(i18n)                        # 见 src/locales/index.ts（重构 Q6 删）
  ├─ .use(VueClickAway)
  ├─ .use(VueLazyLoad, {loading, error: defaultCover})
  ├─ import './router/guard'           # 路由钩子注册副作用
  ├─ import 'virtual:svg-icons-register' # 由 vite-plugin-svg-icons 提供
  ├─ registerObSkeleton(app)
  └─ registerScrollSpy(app)
        │
        ▼
App.vue                                # 渲染入口
  ├─ HeaderMain                        # Logo + Navigation + Controls + Notification
  ├─ <router-view>                     # 由 vite-plugin-pages 自动路由
  ├─ FooterContainer + FooterLink
  ├─ (isMobile)  MobileMenu
  ├─ (!isMobile && configReady)  Dia    # AI 机器人，重构删
  ├─ VueEasyLightbox                   # 图片灯箱（看是否保留）
  └─ teleport(<title>) → <head>
```

### 2.4 `src/main.ts` 关键依赖（共 13 个 import）

| 来源 | 用途 | 重构后还要不要 |
|---|---|---|
| `vue` `createApp` | 创建应用 | 留 |
| `pinia` `createPinia` | 状态管理 | 留 |
| `@/styles/index.scss` | 全局样式入口 | 留 |
| `./App.vue` | 根组件 | 留 |
| `./router` | 路由实例 | 留 |
| `./locales` | i18n 实例 | **Q6 = B 时删** |
| `vue3-click-away` | 点击元素外 | 看 Dropdown 是否要 |
| `vue3-lazyload` | 图片懒加载 | 可换原生 `loading="lazy"` |
| `./router/guard` | 路由钩子副作用 | 留（但要改写） |
| `virtual:svg-icons-register` | SVG 雪碧图运行时 | 留 |
| `@/components/LoadingSkeleton` 的 `registerObSkeleton` | 骨架屏注册 | 留 |
| `vue3-scroll-spy` 的 `registerScrollSpy` | TOC 滚动高亮 | 留 |
| `@/assets/default-cover.jpg` | 懒加载占位图 | 留 |

---

## 3. 运行时依赖（`dependencies`）逐项

| 包 | 版本 | 实际用法 | 建议 |
|---|---|---|---|
| `vue` | ^3.3.4 | 核心 | 🟢 升 3.5 |
| `vue-router` | ^4.2.4 | 路由 | 🟢 升 4.4 |
| `pinia` | 2.1.6 | 14 个 store | 🟢 升 2.2 |
| `vue-i18n` | ^9.2.2 | 全局 i18n 框架 | 🔴 **删**（Q6 = B） |
| `axios` | ^1.5.0 | `src/utils/request.ts` 实例化 | 🟢 升 1.7 |
| `vue-class-component` | ^8.0.0-rc.1 | **源码 0 引用** | 🔴 **死依赖，直接删** |
| `js-cookie` | ^3.0.5 | `src/stores/app.ts` 持久化暗黑模式 | 🟢 留 |
| `nprogress` | ^0.2.0 | `src/components/ProgressBar.vue` + `stores/app.ts` 的 `startLoading`/`endLoading` | 🟢 留 |
| `normalize.css` | ^8.0.1 | 全局 reset | 🟡 Tailwind preflight 可代替（但要测视觉差异） |
| `vue3-click-away` | ^1.2.4 | Dropdown 点击外触发 | 🟡 原生 `@click.outside` 或自写指令可替代 |
| `vue3-lazyload` | ^0.3.8 | 图片懒加载（main.ts 全局注册） | 🟡 原生 `loading="lazy"` 替代 |

---

## 4. 开发依赖（`devDependencies`）

### 4.1 必留（构建/类型/lint 核心）

| 包 | 版本 | 用途 |
|---|---|---|
| `@vitejs/plugin-vue` | ^4.3.4 | Vue SFC 编译 |
| `vite-plugin-pages` | ^0.31.0 | 文件路由（扫描 `src/pages/**`） |
| `vite-plugin-svg-icons` | ^2.0.1 | SVG 雪碧图，提供 `virtual:svg-icons-register` |
| `tailwindcss` `postcss` `autoprefixer` | — | CSS 工具链 |
| `sass` | ^1.66.1 | SCSS 预处理 |
| `typescript` | ^5.1.0 | TS 编译 |
| `@types/*` | — | 类型声明 |
| `eslint` 8 + 一堆 plugin | — | Lint |
| `prettier` | ^3.0.3 | 格式化 |

### 4.2 应删（确认无用 / 有更好替代）

| 包 | 删除理由 |
|---|---|
| `vite-plugin-html-transformer` | 当前 Vite 4 下行为异常（导致根 `index.html` 404），已用手写 `index.html` 绕过 |
| `script-ext-html-webpack-plugin` | Webpack 时代插件，工程是 Vite，**完全用不到** |
| `esm` | 老 CJS 时代产物，无引用 |
| `semantic-release` | npm 包发版工具，你不发包 |
| `vue-jest` / `@vue/test-utils` / `@types/jest` | 0 个测试文件；改 Vitest（Q4=B）则全删 |
| `vue-easy-lightbox` | 文章图片 lightbox；看 02-L7 决定 |

### 4.3 可疑（待查）

| 包 | 问题 |
|---|---|
| `runjs` | 看 `build/scripts/config-script.js` 是否还跑 |
| `@commitlint/cli` `@commitlint/config-conventional` `husky` | 提交规范；个人项目按需 |
| `vue3-scroll-spy` | TOC 用，留 |
| `vue3-click-away` | Dropdown 用，可自写替代 |

---

## 5. 配置文件清单

| 文件 | 当前作用 | 重构后 |
|---|---|---|
| `vite.config.js` | Vite 主配置；已注释 `/api` proxy；用了 `createHtmlPlugin`、`vite-plugin-pages`、`vite-plugin-svg-icons` | 🟡 改成 `vite.config.ts`；删 `createHtmlPlugin` 段；恢复 proxy 指向 V2 后端 |
| `tailwind.config.js` | Tailwind 主题/路径 | 🟢 留 |
| `postcss.config.js` | PostCSS 插件 | 🟢 留 |
| `tsconfig.json` | TS 编译选项 | 🟡 升级后 `moduleResolution: "bundler"`、`target: "ES2022"` |
| `.eslintrc.cjs` / `.prettierrc` | 代码规范 | 🟢 留 |
| `commitlint.config.js` | 提交信息规则 | 🟡 不要就删 |
| `index.html`（我新建在根目录） | Vite 入口 HTML | 🟢 留 |
| `templates/index.html` `templates/index_prod.html` | `vite-plugin-html-transformer` 用 | 🔴 删（连带删插件） |
| `_config.aurora.yml`（hexo 模式） | 主题配置；mock 模式不用 | 🔴 删 |
| `build/index.js`、`build/scripts/config-script.js` | 切换 local/prod/pub 环境的脚本 | 🔴 删（用 `.env.*` 替代） |
| `.env`、`.env.production` | Vite 环境变量 | 🟢 留，内容改 |

---

## 6. 环境变量

文件 `.env`（当前内容）：

```
VITE_APP_PROJECT_TITLE = 'Aurora Blog'
VITE_APP_BASE_API = 'api'
VITE_APP_PUBLIC_PATH = '/'
```

| 变量 | 用途 | 用在哪 | V2 改造 |
|---|---|---|---|
| `VITE_APP_PROJECT_TITLE` | HTML 标题 fallback | `src/settings.ts` | 改成你的站点名 |
| `VITE_APP_BASE_API` | API 路径前缀 | `src/utils/request.ts` L6 | 接 V2 时改 |
| `VITE_APP_PUBLIC_PATH` | 静态资源前缀 | `vite.config.js` `base`、`request.ts` L6 | 改为部署路径 |
| `VITE_APP_I18N_LOCALE` / `VITE_APP_I18N_FALLBACK_LOCALE` | i18n 默认语言 | `src/locales/index.ts` | 🔴 Q6=B 则删 |

> `request.ts` 当前 baseURL 计算：`VITE_APP_PUBLIC_PATH + VITE_APP_BASE_API` = `/` + `api` = `/api`。

---

## 7. 路由系统

### 7.1 路由实例（`src/router/index.ts`）

```ts
import { createRouter, createWebHistory } from 'vue-router'
import routes from '~pages'   // ← vite-plugin-pages 的虚拟模块

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // hash → 1.5s 后滚动到锚点 top: 81
    // savedPosition → 恢复
    // 其它 → top: 0
  }
})
```

要点：
- `routes` 是 `vite-plugin-pages` 根据 `src/pages/**` **自动生成**的，没有手写表
- `scrollBehavior` 的 `top: 81` 对应 Header 高度（写死，和 `useJumpToEle` 一致）
- `1500ms setTimeout` 是为了等异步内容渲染后再滚动到锚点

### 7.2 路由守卫（`src/router/guard.ts`）

| 阶段 | 做了什么 | 重构注意 |
|---|---|---|
| `beforeEach` | 1) 启动 nprogress; 2) 用 i18n 计算页面标题写到 metaStore; 3) 设置 i18n.global.locale | 🟡 Q6=B 删 i18n 逻辑；title 改纯字符串 |
| `afterEach` | 1) 结束 nprogress; 2) `#App-Container` focus | 🟢 留 |

---

## 8. HTTP 客户端（`src/utils/request.ts`）

```ts
const service = axios.create({
  baseURL: import.meta.env.VITE_APP_PUBLIC_PATH + import.meta.env.VITE_APP_BASE_API,
  timeout: 5000
})

// 请求拦截器：占位，没有实际逻辑
// 响应拦截器：成功直接 return；失败 console.log + console.error + Promise.reject
```

**事实**：当前**响应拦截器形同虚设**，错误处理只有 `console.log`。接 V2 后端时必须重写。

V2 改造建议：
- 适配后端统一响应格式 `{ code, data, message }`：业务码非 0 转 `Promise.reject(new Error(message))`
- 401/403 → 跳登录页（如果做后台）
- 全局错误 Toast（接 `useCommonStore.sendNotification`）

---

## 9. 暗黑模式机制

| 步骤 | 文件 / 行号 | 说明 |
|---|---|---|
| 系统偏好检测 | `src/stores/app.ts` L20 | `window.matchMedia('(prefers-color-scheme: dark)')` |
| 应用主题 | `src/stores/app.ts` L26-34（`setTheme` 函数） | 给 `document.body` 加 `theme-dark` / `theme-light` 类 |
| 手动切换 | `src/stores/app.ts` L101-107（`toggleTheme` action） | 切换 class + `Cookies.set('theme', ...)` |
| UI 入口 | `src/components/ToggleSwitch/ThemeToggle.vue` | 调 `appStore.toggleTheme` |
| CSS 变量 | `src/App.vue` L230-238 | 两套 CSS 变量根据 body class 切换 |

---

## 10. 主题渐变色机制

| 步骤 | 文件 / 行号 |
|---|---|
| 配置源 | `site.json` → `theme_config.theme.gradient.{color_1, color_2, color_3, header_filter_cover}` |
| 计算 | `src/stores/app.ts` 的 `headerGradient` 派生 + `themeConfig.theme.header_gradient_css` |
| Header 应用 | `src/App.vue` L219（背景）+ L230-238（CSS 变量 `--text-accent`、`--main-gradient`） |
| 活动 Tab | `src/pages/index.vue` L207 |
| 归档时间线 | `src/pages/archives.vue` L216 |
| 段落标题 | `src/components/Title/src/MainTitle.vue` |

**当前锁定**：`#06b6d4 → #6366f1 → #8b5cf6`（青绿 → 蓝紫 → 紫罗兰），明/暗模式都搭。

---

## 11. CDN / 外部脚本注入机制

注入点有两处：

### 11.1 HTML 静态 CDN（`index.html`）

```html
<script src="https://unpkg.com/blueimp-md5@^2.19.0/js/md5.min.js"></script>
<script src="https://unpkg.com/lodash@^4.17.21/lodash.min.js"></script>
<link rel="stylesheet" href="https://fonts.loli.net/css?family=Rubik" />
```

- `blueimp-md5`：Gravatar 头像 hash 用
- `lodash`：评论插件依赖
- `fonts.loli.net`：Google Fonts 镜像

> 重构后：md5 / lodash 改用 npm 包按需引入，字体可走本地或更稳的 CDN。

### 11.2 运行时按配置注入（`src/stores/meta.ts` 的 `addScripts`）

| 触发位置 | 文件 / 行号 | 注入什么 |
|---|---|---|
| 主入口 | `src/App.vue` L104 `metaStore.addScripts(themeConfig.site_meta.cdn.prismjs)` | Prism.js 代码高亮 |
| 评论容器 | `src/components/Comment.vue` L21-24 | 按 `plugins.{gitalk/valine/twikoo/waline}.enable` 注入对应 CSS/JS |
| MathJax / KaTeX / Mermaid | 同上模式 | 按 `plugins.*.enable` 注入 |

**机制**：`meta` store 维护 `scripts: []` 数组，App.vue 用 `<teleport to="head">` 把 `<script>` 标签喂到 `<head>`。

**重构注意**：Phase 5 功能裁剪时，删插件 = 删 store 里的注入调用 + 删 `Comment.vue` 对应 case 分支。

---

## 12. 模型类型层（`src/models/`）

| 文件 | 主要类型 | 说明 |
|---|---|---|
| `Post.class.ts` | `NavPost`, `Post`, `PostList`, `FeaturePosts`, `Category`, `Tag`, `Tags`, `Categories`, `Archives`, `AuthorPosts`, `SpecificPostsList`, `SpecificPostListRaw` | 文章/列表/分页等核心数据 |
| `Article.class.ts` | `Article`, `Page`, `Link`, `FooterLink`, `Links` | 详情页 + 友链 |
| `HexoConfig.class.ts` | `HexoConfig` | 站点配置（顶层） |
| `ThemeConfig.class.ts` | `ThemeConfig`, `ThemeMenu`, `Menu`, `Avatar`, `Theme`, `Site`, `Social`, `SiteMeta`, `Plugins`, `FooterLinks`, **`Locales` 枚举** | 657 行；i18n 删了后 `Locales` 也删 |
| `Search.class.ts` | `SearchIndexes`, `RecentSearchResults` | 搜索 |
| `Statistic.class.ts` | `Statistic` | 全站统计 |

---

## 13. 测试

**事实**：工程里**0 个测试文件**。`vue-jest` / `@vue/test-utils` / `@types/jest` 三个依赖空挂着。

- 不写测试 → 全删
- 想写 → Q4=B，全删后装 Vitest（和 Vite 同源、零配置）

---

## 14. 已知技术债速查

| # | 问题 | 表现 | 解决方案 |
|---|---|---|---|
| TD1 | `vite-plugin-html-transformer` 异常 | 根目录访问 404 | 已用手写 `index.html` 绕过；**Phase 1 脱钩 Hexo** 时彻底删插件 |
| TD2 | `vue-class-component` 是死依赖 | 源码 0 引用，纯负重 | **Phase 2 删死依赖** |
| TD3 | i18n 抽象层冗余 | 个人中文博客，三套语言包 + 框架开销 | Q6=B 走 **Phase 6 i18n 改造** |
| TD4 | 100% Options API | 与社区主流 `<script setup>` 风格脱节 | Q8=C → **Phase 4 集中迁移** |
| TD5 | CDN 注入逻辑写死在 utils + meta store | 加 / 删插件牵扯多处 | **Phase 5 功能裁剪**时连锁清理；长远改 Vite 插件 |
| TD6 | 自研 env 切换脚本（`build/scripts/config-script.js`） | 重复造轮子 | **Phase 1 脱钩 Hexo** 时删，用 `.env.*` 替代 |
| TD7 | `templates/index.html` + html transformer 机制 | 为支持 hexo 注入变量而存在，剥离 hexo 后没意义 | **Phase 1 脱钩 Hexo** |
| TD8 | `request.ts` 响应拦截器只有 `console.log` | 接 V2 后端会"沉默失败" | **Phase 7 V2 后端对接**时改写 |
| TD9 | `Navigator` 组件已被 App.vue 注释但代码还在 | 死代码 | **Phase 5 功能裁剪** |
| TD10 | Aurora Dia AI 机器人 | 不需要这个功能 + 增加加载体积 | **Phase 5 功能裁剪**（Vue + store + utils 三处） |

---

## 15. 一句话总结

**一个 2023 年中冷冻的 Vue 3 + Vite 4 项目，主要技术债集中在「hexo 耦合」「未启用的测试栈」「Options API 全量未升 `<script setup>`」三块。** 升级路径清晰，没有不可逾越的硬骨头。
