# 02 · 功能清单与 留/改/删 决议

> **范围**：只看当前前端工程能跑出来的功能，**不考虑后端**。
> **图例**：🟢 留 / 🟡 改 / 🔴 删 / ⚪ 待定
> **填写方式**：每行最后一列「最终决定」由你拍板，可写 `留` / `改：xxx` / `删`。
>
> 路径全部相对 `c:/tyb/hexo-theme-aurora-main/`。

---

## A. 页面（`src/pages/`）

路由由 `vite-plugin-pages` 根据文件名自动生成。

| # | 路由 | 源码 | 做什么 | 主要依赖 | 我的建议 | 最终决定 |
|---|---|---|---|---|---|---|
| A1 | `/` | `src/pages/index.vue` | 首页：分类 Tab + Feature 轮播 + 文章列表 + Sidebar | `usePostStore`、`useCategoryStore`、`useMetaStore`；组件 `Feature`、`ArticleCard`、`Paginator`、`Sidebar` | 🟢 留 | |
| A2 | `/about` | `src/pages/about.vue` | 关于页（fetch 单篇 + Markdown 渲染） | `useArticleStore`、`useCommonStore`；组件 `PageContent`；hook `usePageTitle` | 🟢 留 | |
| A3 | `/archives` | `src/pages/archives.vue` | 时间线归档（按年月分组） | `usePostStore`、`useCommonStore`；组件 `Breadcrumbs`、`Paginator` | 🟢 留 | |
| A4 | `/tags` | `src/pages/tags.vue` | 标签云 | `useTagStore`、`useCommonStore`；组件 `TagList`、`TagItem` | 🟢 留 | |
| A5 | `/categories` | `src/pages/category.vue` | 分类列表（**当前模板很简陋**，只有 Sidebar） | `Sidebar`；hook `usePageTitle` | 🟡 改：v1 有更完整的分类页 UI，可以迁过来 | |
| A6 | `/links` | `src/pages/links.vue` | 友链页：头像墙 + 分类友链列表 + 评论 | `useArticleStore`、`useCommonStore`；组件 `LinkBox`、`LinkList`、`LinkCategoryList`、`PostStats`；hook `useCommentPlugin` | 🟡 改：v1 有友链申请表单，迁入 | |
| A7 | `/post/:slug` | `src/pages/post/[slug].vue` | 文章详情：正文 + TOC + 评论 + 相关推荐 | `useArticleStore`、`useCommonStore`；组件 `Comment`、`PostStats`、`ArticleCard`；hook `useLightBox`、`useCommentPlugin` | 🟢 留 | |
| A8 | `/post/search` | `src/pages/post/search/index.vue` | 搜索结果页：按分类/标签筛选 | `usePostStore`、`useCommonStore`；组件 `ArticleCard`、`Paginator`、`CategoryBox`、`TagBox` | 🟢 留 | |
| A9 | `/page/:slug` | `src/pages/page/[slug].vue` | 自定义页面（如自定义 markdown 页） | `useArticleStore`、`useMetaStore`、`useAppStore`；组件 `PageContent`、`Breadcrumbs`、`Comment` | 🟡 看 V2 是否做"自定义页面"功能 | |
| A10 | `/:all(.*)*` | `src/pages/[...all].vue` | 404 兜底（带动画 SVG） | 纯静态 | 🟢 留 | |

**v1 候选迁入页面**：

| # | v1 功能 | 我的建议 | 最终决定 |
|---|---|---|---|
| A11 | 独立留言板（v1 有，Aurora 没有） | 🟡 看是否用 Waline 替代或迁 v1 实现 | |
| A12 | 后台管理入口 | 🟡 看 V2 后端是否分离 | |

---

## B. 布局 & 全局组件

| # | 组件 | 源码 | 做什么 | 主要依赖 | 我的建议 | 最终决定 |
|---|---|---|---|---|---|---|
| B1 | Header | `src/components/Header/src/Header.vue` 等 5 个文件 | 顶栏（Logo + 导航 + 控件 + 通知） | `useNavigatorStore`、`useAppStore`；`Sticky` 包裹 | 🟢 留 | |
| B2 | Header / Logo | `src/components/Header/src/Logo.vue` | 显示 `site.author`（大字）+ `site.nick`（小字） | `useAppStore` | 🟢 留 | |
| B3 | Header / Navigation | `src/components/Header/src/Navigation.vue` | 主菜单渲染（从 `theme.menu` 配置读取） | `useAppStore` | 🟢 留 | |
| B4 | Header / Controls | `src/components/Header/src/Controls.vue` | 搜索/暗黑/语言切换按钮组 | `useAppStore`、`useSearchStore`、`Dropdown` | 🟡 改：语言选项 → zh/ja/en；切换写 `localStorage` + 跳 `/{lang}/...` | |
| B5 | Header / Notification | `src/components/Header/src/Notification.vue` | 顶部小通知条 | `useCommonStore` | 🟢 留（很少触发但成本低） | |
| B6 | Footer | `src/components/Footer/FooterContainer.vue` | 页脚：版权、备案、运行天数 | `useAppStore` | 🟡 改：替换作者署名和版权 | |
| B7 | FooterLink | `src/components/Footer/FooterLink.vue` | 页脚链接分组 | 纯 props | 🟢 留 | |
| B8 | Sidebar | `src/components/Sidebar/src/Sidebar.vue` 等 5 个 | 右侧栏容器（Profile + TagBox + RecentComment + CategoryBox + Toc） | hook `useCommentPlugin`；`Sticky` | 🟢 留 | |
| B9 | Sidebar / Profile | `src/components/Sidebar/src/Profile.vue` | 作者卡（数据源：`/api/authors/:slug.json`） | axios | 🟢 留 | |
| B10 | Sidebar / RecentComment | `src/components/Sidebar/src/RecentComment.vue` | 最近评论卡（需评论插件支持 `recentComment`） | `useCommentPlugin` | 🟢 留（依 Waline 决定） | |
| B11 | Sidebar / TagBox & CategoryBox | 同上目录 | 侧栏标签/分类小卡 | `useTagStore`、`useCategoryStore` | 🟢 留 | |
| B12 | Sidebar / Toc | `src/components/Sidebar/src/Toc.vue` | 文章目录（带 scroll-spy） | `vue3-scroll-spy`、hook `useJumpToEle` | 🟢 留 | |
| B13 | MobileMenu | `src/components/MobileMenu.vue` | 移动端汉堡抽屉菜单 | `useNavigatorStore`、`useAppStore`、`useRoutersStore` | 🟢 留 | |
| B14 | Breadcrumbs | `src/components/Breadcrumbs.vue` | 面包屑（依路由计算） | `useAppStore`、`useRouter` | 🟢 留 | |
| B15 | Paginator | `src/components/Paginator.vue` | 分页器（事件式） | emit `pageChange` | 🟢 留 | |
| B16 | ProgressBar | `src/components/ProgressBar.vue` | 顶部 nprogress 加载条 | `nprogress` | 🟢 留 | |
| B17 | Navigator | `src/components/Navigator.vue` | 页面内 scroll-spy 浮动导航。**App.vue 里已被注释，当前不渲染** | `useNavigatorStore`、`useAppStore` | 🔴 删（既然上游都关了） | |
| B18 | Sticky | `src/components/Sticky.vue` | 通用吸顶包装器 | emit `activeChange` | 🟢 留 | |
| B19 | Dropdown | `src/components/Dropdown/` 3 个文件 | 通用下拉菜单 | `useDropdownStore` | 🟡 删了语言切换后，仍被 Header Controls / MobileMenu 用 → 留 | |
| B20 | ToggleSwitch | `src/components/ToggleSwitch/` 2 个文件 | 暗黑模式开关 + 通用开关 | `useAppStore` | 🟢 留 | |
| B21 | SvgIcon | `src/components/SvgIcon/index.vue` | SVG 雪碧图渲染（动态染色） | `useAppStore`、`vite-plugin-svg-icons` | 🟢 留 | |
| B22 | LoadingSkeleton | `src/components/LoadingSkeleton/` 2 个文件 | 骨架屏（ob-skeleton 风） | 自包含 | 🟢 留 | |

---

## C. 文章/内容组件

| # | 组件 | 源码 | 做什么 | 主要依赖 | 我的建议 | 最终决定 |
|---|---|---|---|---|---|---|
| C1 | ArticleCard | `src/components/ArticleCard/src/ArticleCard.vue` | 文章列表卡（标准纵向） | `useRouter`、`useAppStore`、`SvgIcon` | 🟢 留 | |
| C2 | HorizontalArticle | `src/components/ArticleCard/src/HorizontalArticle.vue` | 横向卡（首页 Feature 用） | 纯 props | 🟢 留 | |
| C3 | Feature | `src/components/Feature/src/Feature.vue` + `FeatureList.vue` | 首页置顶轮播容器 | 纯 props | 🟢 留 | |
| C4 | Post / PostStats | `src/components/Post/PostStats.vue` | 字数 / 阅读时长 / 评论数 | hook `useCommentPlugin`；`PostStatsType` model | 🟢 留 | |
| C5 | PageContent | `src/components/PageContent.vue` | 渲染 HTML 正文 + 触发 lightbox | hook `useLightBox` | 🟢 留 | |
| C6 | Title | `src/components/Title/src/MainTitle.vue` + `SubTitle.vue` | 渐变样式段落标题 | `useAppStore` 渐变色 | 🟢 留 | |
| C7 | Tag | `src/components/Tag/TagItem.vue` + `TagList.vue` | 标签徽章 + 列表 | `useRouter` | 🟢 留 | |
| C8 | Link | `src/components/Link/*.vue`（共 6 个变体） | 友链相关：卡 / 列表 / 分类列表 / 头像 | `useRouter`、`useAppStore` | 🟢 留 | |
| C9 | Social | `src/components/Social.vue` | 社交图标行 | `useAppStore` socials 配置 | 🟢 留 | |
| C10 | Button | `src/components/Button/` 2 个文件 | 主/次按钮 | 纯样式 | 🟢 留 | |
| C11 | Comment | `src/components/Comment.vue` | 评论容器（按配置分发到 4 个插件之一） | `useAppStore`、`useCommentPlugin`；4 个 init 函数（github/valine/twikoo/waline） | 🟡 改：只保留 Waline 分支，删另外 3 个 | |
| C12 | Dia | `src/components/Dia.vue` | **AI 聊天机器人**（Aurora Dia） | `useDiaStore`、`useAppStore` | 🔴 删（个人博客用不上、还要服务） | |
| C13 | SearchModal | `src/components/SearchModal.vue` | 搜索弹层（Cmd+K） | `useSearchStore`、`useI18n` | 🟡 改：i18n 保留；数据源换 V2（多语索引方案见 05 §1） | |

---

## D. 状态管理（`src/stores/`，14 个 Pinia store）

| # | Store | 持有状态 | 主要 API | 我的建议 | 最终决定 |
|---|---|---|---|---|---|
| D1 | `app.ts` | 主题(暗/亮)、locale、themeConfig、hexoConfig、headerGradient、statistic、appLoading、openSearchModal、configReady | `fetchConfig`、`toggleTheme`、`setLocale`、`initializeTheme` | 🟡 改：locale 默认 `zh`、合法值 `zh/ja/en`；`setDefaultLocale` 用 `navigator.language` + `localStorage.aurora.lang` | |
| D2 | `post.ts` | featurePosts、posts、postTotal、cachePost | `fetchFeaturePosts`、`fetchPostsList`、`fetchPostsByCategory`、`fetchPostsByTag`、`fetchArchives` | 🟢 留 | |
| D3 | `article.ts` | 无状态 | `fetchArticle(source)` | 🟢 留 | |
| D4 | `category.ts` | isLoaded、categories | `fetchCategories` | 🟢 留 | |
| D5 | `tag.ts` | isLoaded、tags | `fetchAllTags`、`fetchTagsByCount` | 🟢 留 | |
| D6 | `author.ts` | 无状态 | `fetchAuthorData(slug)` | 🟢 留 | |
| D7 | `common.ts` | isMobile、headerImage、notificationState、notificationMessage | `changeMobileState`、`sendNotification`、`setHeaderImage` | 🟢 留 | |
| D8 | `meta.ts` | title、description、links、scripts、meta | `setTitle`、`addScripts`、`addLinks`、`addMeta` | 🟢 留 | |
| D9 | `search.ts` | searchIndexes、recentResults、openModal | `fetchSearchIndex`、`search`、`setOpenModal` | 🟢 留 | |
| D10 | `lightbox.ts` | images、index、visible | `addImage`、`openImage`、`hideLightBox` | 🟡 随 `vue-easy-lightbox` 去留 | |
| D11 | `dropdown.ts` | commandName、uid | `setCommand`、`setUid` | 🟢 留 | |
| D12 | `navigator.ts` | openMenu、openNavigator、isDone、progress | `toggleMobileMenu`、`toggleOpenNavigator`、`updateProgress` | 🟡 改：Navigator 组件删了后，相关 action 也可瘦身 | |
| D13 | `routers.ts` | routes | （静态 filter+map） | 🟢 留 | |
| D14 | `dia.ts` | dia（AuroraDia 实例） | `initializeBot` | 🔴 删（随 Dia 组件） | |

---

## E. 钩子（`src/hooks/`）

| # | Hook | 源码 | 做什么 | 调用点 | 我的建议 | 最终决定 |
|---|---|---|---|---|---|---|
| E1 | `usePageTitle.ts` | 同 | 根据路由 + locale 计算页面标题，同步到 `metaStore` | 所有页面 setup 阶段 | 🟢 留（locale 三语沿用） | |
| E2 | `useCommentPlugin.ts` | 同 | 检测启用的评论插件，返回 PV / 评论数 / 最近评论的统一接口 | `Comment.vue`、`PostStats.vue`、`Sidebar/RecentComment.vue` | 🟡 改：简化为只支持 Waline，从 4 路 if-else 砍到 1 路 | |
| E3 | `useLightBox.ts` | 同 | 给 `.post-html` 内的 `<img>` 注册点击事件，触发 `vue-easy-lightbox` 弹层 | `PageContent.vue` onMount、`post/[slug].vue` | 🟡 看是否保留 lightbox（删了节省一个包） | |
| E4 | `useJumpToEle.ts` | 同 | 平滑滚动到元素（考虑 81px 固定头 + 30px 容器留白） | Sidebar `Toc.vue`、文章 TOC | 🟢 留 | |

---

## F. 工具函数（`src/utils/`）

| # | 文件 | 做什么 | 我的建议 | 最终决定 |
|---|---|---|---|---|
| F1 | `index.ts` | 一揽子工具：`RecentComment` 接口、`formatTime`、`filterHTMLContent`、`getDaysTillNow`、`cleanPath`、`shuffleArray`、`throttle`、分页器 | 🟢 留 | |
| F2 | `request.ts` | axios 实例（5s 超时 + 请求/响应拦截器） | 🟡 改：拦截器对接 V2 后端错误格式 | |
| F3 | `localization.ts` | 友链 badge 文案的 i18n key 映射（tech/designer/vip/personal） | 🟢 留（badge 文案要三语就在 zh.json/ja.json/en.json 加键） | |
| F4 | `validate.ts` | URL/mailto/tel 校验、icon 路径校验、用户名校验 | 🟢 留 | |
| F5 | `auth.ts` | 评论插件鉴权 token | 🟡 随评论插件选型决定 | |
| F6 | `external-request.ts` | 跨域外部 HTTP 请求包装 | 🟡 看实际用例（友链信息抓取？） | |
| F7 | `get-page-title.ts` | 页面标题组装（路由名 + 站点名） | 🟢 留 | |
| F8 | `aurora-dia/` | Dia AI 机器人模块 | 🔴 删（随 Dia 组件） | |
| F9 | `comments/github-api.ts` | Gitalk（GitHub Issue 评论） | 🔴 删 | |
| F10 | `comments/valine-api.ts` | Valine（LeanCloud） | 🔴 删 | |
| F11 | `comments/twikoo-api.ts` | Twikoo（无服务器） | 🔴 删 | |
| F12 | `comments/waline-api.ts` | Waline | 🟢 留 | |
| F13 | `comments/gravatar.ts` | Gravatar 头像获取 | 🟡 看 Waline 是否还要 | |

---

## G. 国际化（`src/locales/`）

> **决议已定**（03 Q6=B）：留 vue-i18n，调整为 **zh / ja / en**，删 zh-TW，新增 ja。详细对接方案见 [05-backend-integration.md](./05-backend-integration.md) §1。

| # | 项 | 源码 | 做什么 | 我的建议 | 最终决定 |
|---|---|---|---|---|---|
| G1 | `locales/index.ts` | 用 `import.meta.glob` 加载 `languages/*.json`，创建 vue-i18n 实例 | i18n 框架接入 | 🟡 改：默认 `locale: 'zh'`、`fallbackLocale: 'zh'`、合法 locale = `zh/ja/en` | |
| G2 | `languages/zh-CN.json` | 简体中文翻译（2.7KB） | 文案 | 🟡 改：重命名为 `zh.json` | |
| G3 | `languages/en.json` | 英文翻译（2.6KB） | 文案 | 🟢 留 | |
| G4 | `languages/zh-TW.json` | 繁体中文翻译（2.7KB） | 文案 | 🔴 删（不在目标语言里） | |
| G5 | `languages/ja.json` | 不存在 | 文案 | 🟡 新增：先用 zh 复制，后逐条翻译 | |
| G6 | 语言切换 UI | `Header/Controls.vue` 内某段 | 切换下拉 | 🟡 改：选项改为 zh/ja/en 三项；切换时写 `localStorage.aurora.lang` + 跳转到 `/{lang}/...` | |
| G7 | `vue-i18n` 依赖 | `package.json` | 框架 | 🟢 留 | |
| G8 | 路由语言前缀 | `src/router/index.ts` | 当前无前缀 | 🟡 改：加 `/:lang(zh|ja|en)?` 段 | |
| G9 | 语言检测+记忆 | `src/router/guard.ts` + `src/stores/app.ts` | 当前只读 cookie | 🟡 改：首次访问按 `navigator.language` 匹配，否则 fallback `zh`；选择后写 `localStorage.aurora.lang` | |
| G10 | 业务字段三语 | `src/api/index.ts` + `src/models/*` | 当前单语 | 🟡 改：后端字段 `title_{zh,ja,en}` 等；前端按当前 locale 取（adapter 层） | |
| G11 | 字体按语言切换 | 当前无 | — | 🟡 新增：`styles/i18n-fonts.scss` 用 `:lang(ja)` `:lang(zh)` 切 Noto Sans JP / SC | |
| G12 | `utils/localization.ts` | 友链 badge 文案的 i18n key 映射 | 4 个 badge 类型 | 🟢 留（badge 文案要三语就在这里改键） | |

---

## H. 视觉/主题系统

| # | 功能 | 配置项 / 源码 | 我的建议 | 最终决定 |
|---|---|---|---|---|
| H1 | 暗黑模式自动检测 | `src/stores/app.ts` L20（`prefers-color-scheme`）+ L26-34 | 🟢 留 | |
| H2 | 暗黑模式手动切换 | 同上 L101-107（`toggleTheme`）+ cookie 持久化（`Cookies.set('theme')`） | 🟢 留 | |
| H3 | 主题 3 色渐变 | `App.vue` L219（Header）+ L230-238（CSS 变量 `--text-accent`、`--main-gradient`） | 🟢 留 | |
| H4 | 渐变色用于活动 Tab | `pages/index.vue` L207 | 🟢 留 | |
| H5 | 渐变色用于归档时间线 | `pages/archives.vue` L216 | 🟢 留 | |
| H6 | 渐变色用于段落标题 | `Title/MainTitle.vue` | 🟢 留 | |
| H7 | Header 毛玻璃滤镜 | 配置 `theme.gradient.header_filter_cover` | 🟢 留 | |
| H8 | 首页置顶轮播开关 | 配置 `theme.feature`；组件 `Feature/` | 🟢 留 | |
| H9 | 头像形状 | 配置 `theme.profile_shape`（diamond/circle/square） | 🟢 留 | |
| H10 | 代码块 Mac 风格 | 配置 `theme.code_blocks.macStyle` | 🟢 留 | |
| H11 | Header fixed / auto_hide | 配置 `theme.header.*` | 🟢 留 | |
| H12 | 文章内显示标签 | 配置 `theme.tags_in_post` | 🟢 留 | |
| H13 | 站点状态卡（侧栏） | 配置 `theme.site_state` | 🟢 留 | |
| H14 | UV/PV 计数器 | 配置 `theme.uv_pv_counter` | 🔴 删（需第三方服务） | |
| H15 | 自定义背景图 | 配置 `theme.background` | 🟡 不用就留空 | |
| H16 | `first_screen` 首屏文字 | 配置存在但**源码不读**——伪功能 | 🔴 删配置项 | |

---

## I. 插件集成

| # | 插件 | 配置项 | 集成位置 | 我的建议 | 最终决定 |
|---|---|---|---|---|---|
| I1 | Gitalk | `plugins.gitalk` | `utils/comments/github-api.ts`、`Comment.vue` | 🔴 删 | |
| I2 | Valine | `plugins.valine` | `utils/comments/valine-api.ts`、`Comment.vue` | 🔴 删 | |
| I3 | Twikoo | `plugins.twikoo` | `utils/comments/twikoo-api.ts`、`Comment.vue` | 🔴 删 | |
| I4 | **Waline** | `plugins.waline` | `utils/comments/waline-api.ts`、`Comment.vue` | 🟢 **默认选这个** | |
| I5 | MathJax | `plugins.mathjax` | CDN 注入 | 🔴 删 | |
| I6 | KaTeX | `plugins.katex` | CDN 注入 | 🔴 删 | |
| I7 | Mermaid | `plugins.mermaid` | CDN 注入 | 🟡 想画流程图就留 | |
| I8 | Google Analytics | `plugins.google_analytics.tracking_id` | 主入口 | 🔴 删 | |
| I9 | 百度统计 | `plugins.baidu_analytics.tracking_id` | 主入口 | 🔴 删 | |
| I10 | **Aurora Dia AI 机器人** | 无配置开关（始终条件渲染） | `App.vue` L?? + `Dia.vue` + `stores/dia.ts` + `utils/aurora-dia/` | 🔴 删（4 处全砍） | |
| I11 | Prismjs（代码高亮） | `site_meta.cdn.prismjs` 注入 | `App.vue` L104 `metaStore.addScripts(...)` | 🟢 留（文章代码块要它） | |

---

## J. API 端点（`src/api/index.ts`）

| # | 函数 | 端点 | 用途 | V2 后端要不要兼容 | 最终决定 |
|---|---|---|---|---|---|
| J1 | `fetchHexoConfig()` | `GET /site.json` | 站点 + 主题配置 | 🟡 改：拆 `/api/v2/site` + `/api/v2/theme` | |
| J2 | `fetchStatistic()` | `GET /statistic.json` | 全站统计 | 🟢 留 | |
| J3 | `fetchPostsList(page)` | `GET /posts/{page}.json` | 分页文章列表 | 🟢 留 | |
| J4 | `fetchArchivesList(page)` | `GET /archives/{page}.json` | 归档分页 | 🟢 留 | |
| J5 | `fetchAllTags()` | `GET /tags.json` | 标签云 | 🟢 留 | |
| J6 | `fetchAllCategories()` | `GET /categories.json` | 分类列表 | 🟢 留 | |
| J7 | `fetchPostsListByTag(tag, page, size)` | `GET /tags/{name}.json` | 标签下文章 | 🟢 留 | |
| J8 | `fetchPostsListByCategory(cat, page, size)` | `GET /categories/{name}.json` | 分类下文章 | 🟢 留 | |
| J9 | `fetchPostBySlug(slug)` | `GET /articles/{slug}.json` | 单篇详情 | 🟢 留 | |
| J10 | `fetchImplicitPageBySource(source)` | `GET /pages/{source}/index.json` | 自定义页面（about/links） | 🟡 看 V2 是否做"自定义页" | |
| J11 | `fetchFeature()` | `GET /features.json` | 首页置顶 | 🟢 留 | |
| J12 | `fetchSearchIndexes()` | `GET /search.json` | 搜索索引（lunr.js） | 🟡 改：换 V2 搜索接口 | |
| J13 | `fetchAuthorPost(slug)` | `GET /authors/{slug}.json` | 作者信息 + 其文章列表 | 🟢 留 | |

---

## K. 资源 & 模型

| # | 项 | 路径 | 说明 | 我的建议 | 最终决定 |
|---|---|---|---|---|---|
| K1 | SVG 图标源 | `src/icons/` | 由 `vite-plugin-svg-icons` 打成雪碧图 | 🟢 留，清理用不到的 | |
| K2 | 静态资源 | `src/assets/` | 占位图、内置素材 | 🟡 清理 | |
| K3 | 模型 `Post.class.ts` | `src/models/` | 定义 `NavPost`、`Post`、`PostList`、`FeaturePosts`、`Category`、`Tag` 等 | 🟢 留，V2 字段对接时改 | |
| K4 | 模型 `Article.class.ts` | 同 | `Article`、`Page`、`Link`、`FooterLink`、`Links` | 🟢 留 | |
| K5 | 模型 `HexoConfig.class.ts` | 同 | 站点配置类型 | 🟡 拆站点/主题后改名 | |
| K6 | 模型 `ThemeConfig.class.ts` | 同 | 657 行的主题配置类型 + `Locales` 枚举(en/zh-CN/zh-TW) | 🟡 改：`Locales` 枚举改为 `zh/ja/en`；多语菜单结构保留并改 key | |
| K7 | 模型 `Search.class.ts` / `Statistic.class.ts` | 同 | 搜索 / 统计类型 | 🟢 留 | |
| K8 | `src/settings.ts` | 同 | **只有一个字段** `{ title: env or 'TriDiamond Blog' }` | 🟡 改：换成自己的默认标题 | |

---

## L. 隐性 / 容易遗漏的功能

| # | 项 | 实现位置 | 我的建议 | 最终决定 |
|---|---|---|---|---|
| L1 | 路由切换页面动画 | `App.vue` `<transition>` 包裹 router-view | 🟢 留 | |
| L2 | SEO meta 注入（title/keywords/description） | `usePageTitle` + `meta` store | 🟢 留 | |
| L3 | RSS / Sitemap | 原本由 hexo 生成，剥离 hexo 后**没了** | 🟡 V2 后端补 | |
| L4 | 字数 / 阅读时长 | `count_time` 字段由 mock 给，组件 `PostStats` 展示 | 🟢 留，V2 后端算 | |
| L5 | 文章 TOC 滚动高亮 | `vue3-scroll-spy` 在 `Toc.vue` | 🟢 留 | |
| L6 | 文章图片懒加载 | `vue3-lazyload` 全局注册 | 🟡 改：原生 `loading="lazy"` 替代 | |
| L7 | 文章图片 lightbox | `vue-easy-lightbox` + `useLightBox` + `PageContent` | 🟡 看 E3 决定 | |
| L8 | 备案号 / 公安备案 | 配置项 `beian` / `police_beian`，Footer 渲染 | 🟢 留 | |
| L9 | 运行天数 | `utils/index.ts` 的 `getDaysTillNow` | 🟢 留 | |
| L10 | 通知气泡（Notification） | `useCommonStore.sendNotification` + `Header/Notification.vue` | 🟢 留 | |
| L11 | 移动端检测 | `useCommonStore.changeMobileState`（resize 监听） | 🟢 留 | |
| L12 | Cookie 主题持久化 | `js-cookie` 在 `app.ts` 用 | 🟢 留 | |

---

## 决议汇总（填完上面后回来勾这里）

- [ ] A 表（页面）的最终决定已填
- [ ] B 表（布局组件）的最终决定已填
- [ ] C 表（文章组件）的最终决定已填
- [ ] D 表（store）的最终决定已填
- [ ] E 表（hook）的最终决定已填
- [ ] F 表（工具）的最终决定已填
- [ ] G 表（i18n）的最终决定已填
- [ ] H 表（视觉）的最终决定已填
- [ ] I 表（插件）的最终决定已填
- [ ] J 表（API）的最终决定已填
- [ ] K 表（模型/资源）的最终决定已填
- [ ] L 表（隐性功能）的最终决定已填
- [ ] 决议与 [03-decisions-and-roadmap.md](./03-decisions-and-roadmap.md) 的架构决策无冲突

---

## 附：本次审计修正的旧文档错误

| 旧文档说法 | 实际情况 |
|---|---|
| `vue-class-component` 在用，需要随 `<script setup>` 一起删 | **源码 0 处使用**，是 package.json 里挂着的死依赖，可直接删 |
| `<script setup>` 是"部分老组件"还在用旧风格 | **整套工程 100% 用 `defineComponent` Options API**，0 处 `<script setup>`，迁移工作量 = 全量重写 61 个 .vue |
| 国际化只有 zh-CN + en | 实际还有 **zh-TW 繁体中文**，三套语言包 |
| 没提到 Aurora Dia AI 机器人 | 是个完整子系统：`Dia.vue` + `stores/dia.ts` + `utils/aurora-dia/`，删除时要四处全砍 |
| 没提到 Navigator 组件 | 上游已经在 `App.vue` 里**注释掉了**它的渲染，但代码还在 |
