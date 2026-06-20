# site.json 字段说明

这个文件是本地开发用的静态配置，模拟 Hexo 生成的 `public/api/site.json`。
前端实际读取的是 `theme_config` 下的内容，顶层字段大多是 Hexo 遗留，前端基本不用。

---

## 顶层字段（Hexo 遗留，前端基本忽略）

| 字段 | 说明 |
|------|------|
| `title` | 站点标题，Hexo 用，前端不读 |
| `subtitle` | 站点副标题，Hexo 用，前端不读 |
| `description` | 站点描述，Hexo 用，前端不读 |
| `author` | 作者名，Hexo 用，前端不读 |
| `language` | Hexo 默认语言，前端语言走 `theme_config.site_meta.language` |
| `timezone` | Hexo 时区，前端不读 |
| `url` | 站点根 URL，Hexo 用于生成链接 |
| `root` | 站点根路径，Hexo 用 |
| `permalink` | 文章 URL 格式，Hexo 用 |
| `source_dir` / `public_dir` | Hexo 源文件和输出目录 |
| `tag_dir` / `archive_dir` / `category_dir` | Hexo 各页面输出路径 |
| `highlight` / `prismjs` | 代码高亮配置，Hexo 生成时用 |
| `per_page` | 每页文章数，Hexo 分页用 |
| `theme` | Hexo 主题名，固定 `aurora` |
| `deploy` | Hexo 部署配置，本地开发无用 |

---

## theme_config（前端真正读取的部分）

### site — 作者与站点基本信息

| 字段 | 说明 |
|------|------|
| `subtitle` | 显示在首屏的副标题 |
| `author` | 作者名，显示在侧边栏和文章署名 |
| `nick` | 昵称，显示在 header logo 处 |
| `description` | 个人简介，显示在侧边栏头像下方 |
| `avatar` | 头像图片 URL |
| `beian.number` | ICP 备案号，留空不显示 |
| `beian.link` | ICP 备案链接 |
| `police_beian.number` | 公安备案号，留空不显示 |
| `police_beian.link` | 公安备案链接 |

### site_meta — 站点元信息

| 字段 | 说明 |
|------|------|
| `cdn.locale_link` | i18n CDN 地址，留空用本地文件 |
| `cdn.main_color` | 主色调（部分组件用），默认绿色 |
| `favicon` | 网站图标路径 |
| `domain` | 站点域名，用于分享链接等 |
| `language` | 前端默认语言，`zh-CN` / `en` / `ja` |
| `multi_language` | 是否开启多语言切换按钮，`false` 时 header 不显示语言切换 |

### menu — 导航菜单

控制哪些菜单项显示，存在即显示，删掉则隐藏。
显示文字由代码 `ThemeConfig.class.ts` 的 `extract` 决定，这里的 `i18n` 对默认菜单无效。

### avatar — 头像样式

| 字段 | 说明 |
|------|------|
| `url` | 头像图片地址（与 `site.avatar` 作用相同） |
| `rounded` | 是否圆形裁切 |
| `opacity` | 透明度，1 为不透明 |

### theme — 主题视觉配置

| 字段 | 说明 |
|------|------|
| `profile_shape` | 侧边栏头像形状：`circle` / `diamond` / `rounded` |
| `feature` | 是否显示首页特色文章区域 |
| `gradient.color_1/2/3` | 渐变色三个节点，影响 header、进度条、强调色 |
| `header_filter_cover` | header 遮罩颜色（暂未使用） |
| `header.fixed` | header 是否固定在顶部 |
| `header.auto_hide` | 向下滚动时是否自动隐藏 header |
| `background` | 全局背景图 URL，留空用默认深色背景 |
| `code_blocks.transparent` | 代码块是否透明背景 |
| `code_blocks.macStyle` | 代码块是否显示 macOS 风格红绿灯按钮 |
| `dark_mode` | 深色模式：`auto`（跟随系统）/ `true` / `false` |
| `first_screen.background_image` | 首屏背景图 URL，留空用渐变色 |
| `first_screen.description` | 首屏描述文字 |
| `first_screen.anime_text` | 首屏左侧打字动画文字数组 |
| `first_screen.type_text` | 首屏右侧轮播文字数组 |
| `site_state` | 是否显示侧边栏文章数/分类数/标签数统计 |
| `uv_pv_counter` | 是否显示访问量计数（需配合插件） |
| `tags_in_post` | 文章页是否显示标签 |

### socials — 社交链接

各平台链接，留空则不显示对应图标。
支持：`github` / `twitter` / `stackoverflow` / `wechat` / `qq` / `weibo` / `csdn` / `zhihu` / `juejin` / `email`

### plugins — 插件开关

| 插件 | 说明 |
|------|------|
| `google_analytics` | Google 统计，填 tracking_id 启用 |
| `baidu_analytics` | 百度统计，填 tracking_id 启用 |
| `gitalk` | Gitalk 评论，`enable: true` 后需填 clientID 等 |
| `valine` | Valine 评论 |
| `twikoo` | Twikoo 评论 |
| `waline` | Waline 评论，`serverURL` 填部署地址 |
| `mathjax` | LaTeX 数学公式渲染 |
| `katex` | KaTeX 数学公式渲染（比 mathjax 快） |
| `mermaid` | Mermaid 流程图渲染 |

### footer_links — 页脚友链

数组，留空则不显示页脚友链区域。
每项格式：`{ "name": "名称", "url": "链接", "logo": "图标URL" }`
