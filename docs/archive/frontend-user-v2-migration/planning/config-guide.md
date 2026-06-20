# Aurora 主题可配置项速查

> **生效位置**：`c:\tyb\hexo-theme-aurora-main\public\api\site.json`（本地预览用 mock）
> 改完保存后浏览器手动刷新即可。正式接 hexo 时这些字段对应 `_config.aurora.yml`。
>
> **官方文档**：https://aurora.tridiamond.tech/cn/config/theme-config.html

---

## 1. 站点基本信息（`theme_config.site`）

| 字段 | 作用 | 示例 |
|---|---|---|
| `author` | 作者全名 | `"张三"` |
| `nick` | 昵称（首页大头像下显示） | `"小张"` |
| `description` | 站点描述（meta + 首屏使用） | `"一个安静的角落"` |
| `avatar` | 头像 URL（备份/兼容） | 图片地址 |
| `logo` | 站点 logo | 图片地址，空就用文字标题 |
| `started_date` | 建站日期（页脚"已运行 X 天"用） | `"2020-01-01"` |
| `beian.number` / `beian.link` | ICP 备案号及链接 | `""` 留空隐藏 |
| `police_beian.number` / `police_beian.link` | 公安备案号 | 同上 |

---

## 2. 头像（`theme_config.avatar`）

| 字段 | 作用 | 可选值 |
|---|---|---|
| `url` | 首页大头像图片 URL | 图片地址 |
| `rounded` | 是否圆角 | `true` / `false` |
| `opacity` | 透明度 | `0` ~ `1` |

---

## 3. 主题视觉（`theme_config.theme`）—— 最常调

### 3.1 整体风格
| 字段 | 作用 | 可选值 |
|---|---|---|
| `dark_mode` | 暗黑模式策略 | `"auto"` 跟随系统 / `true` 强制暗 / `false` 强制亮 |
| `profile_shape` | 头像形状 | `"circle"` / `"diamond"` / `"rounded"` |
| `background` | 全站背景图 | 图片 URL，空 = 默认渐变 |
| `feature` | 是否显示首页"特色文章"区 | `true` / `false` |
| `site_state` | 显示文章/分类/标签计数 | `true` / `false` |
| `tags_in_post` | 文章卡片上是否显示标签 | `true` / `false` |
| `uv_pv_counter` | 显示访客统计（需配合 busuanzi） | `true` / `false` |

### 3.2 主题三色渐变（决定整站氛围色）
```json
"gradient": {
  "color_1": "#24c6dc",
  "color_2": "#5433ff",
  "color_3": "#ff0099",
  "header_filter_cover": "rgba(0,0,0,0.5)"
}
```
> 改这三个色就能彻底换肤。`header_filter_cover` 控制 header 渐变上的遮罩深浅。

### 3.3 顶栏行为
```json
"header": {
  "fixed": true,        // 顶栏是否吸顶
  "auto_hide": false    // 向下滚动时是否自动隐藏
}
```

### 3.4 代码块样式
```json
"code_blocks": {
  "transparent": false, // 透明背景
  "macStyle": true      // 是否显示 Mac 风格三色圆点
}
```

---

## 4. 文案显示位置说明（重要）

Aurora 2.5.x **没有独立的首屏 Hero/Banner 区**，可见文字只在这几处：

| 位置 | 字段 | 说明 |
|---|---|---|
| 顶部 Header 左上角 | `theme_config.site.author`（大字） + `theme_config.site.nick`（小字） | Logo 旁的署名 |
| 右侧 Sidebar Profile 卡 | 来自 `/api/authors/{slug}.json` 的 `name` / `avatar` / `description` / 统计数据 | 主页/列表页右侧的作者卡 |
| 浏览器标签标题 | `theme_config.site.subtitle` 或 `slogan` | Tab 上的文字 |
| HTML meta 标签（不可见） | `theme_config.site.description` | SEO 用 |

> ⚠️ 网上有些老文档提到 `first_screen` / `anime_text` / `type_text` —— 这是 Aurora 1.x 的字段，2.5+ 已经移除。

---

## 5. 顶部菜单（`theme_config.menu`）

每个菜单结构：
```json
"Home": {
  "name": "Home",
  "path": "/",
  "i18n": { "zh-CN": "首页", "zh-TW": "首頁", "en": "Home" }
}
```

**内置 6 个**：`Home` / `About` / `Archives` / `Tags` / `Categories` / `Links` —— 不想要某个直接删 key 即可。

**自定义菜单**：放到 `theme_config.custom_menu`，结构同上。支持外链 path。

---

## 6. 社交链接（`theme_config.socials`）

填地址就显示对应图标，留空就隐藏。

内置：`github` / `twitter` / `stackoverflow` / `wechat` / `qq` / `weibo` / `csdn` / `juejin` / `zhihu` / `email`

**自定义社交图标**（`theme_config.custom_socials`）：
```json
"custom_socials": {
  "bilibili": {
    "icon": "icon-bilibili",       // iconfont 名称 或 图片 URL
    "link": "https://space.bilibili.com/xxx"
  }
}
```

---

## 7. 站点元数据（`theme_config.site_meta`）

| 字段 | 作用 | 示例 |
|---|---|---|
| `favicon` | 浏览器标签图标 | `"/favicon.ico"` |
| `domain` | 站点域名（SEO） | `"https://example.com"` |
| `language` | 默认语言 | `"zh-CN"` / `"zh-TW"` / `"en"` |
| `multi_language` | 是否显示语言切换按钮 | `true` / `false` |
| `cdn.locale_link` | i18n 资源 CDN | 通常留空 |
| `cdn.main_color` | 主题主色（部分 CDN 资源用） | hex 颜色 |

---

## 8. 插件开关（`theme_config.plugins`）

### 8.1 评论系统（四选一，互斥）

```json
"gitalk": { "enable": false, "clientID": "", "clientSecret": "", "repo": "", "owner": "", "admin": [] },
"valine": { "enable": false, "app_id": "", "app_key": "" },
"twikoo": { "enable": false, "envId": "" },
"waline": { "enable": false, "serverURL": "", "pageSize": 10 }
```

| 评论方案 | 适合 | 注意 |
|---|---|---|
| **Gitalk** | 个人技术博客，登录用 GitHub | 需到 github 注册 OAuth App |
| **Valine** | 简单匿名评论 | 依赖 LeanCloud |
| **Twikoo** | 国内访问友好 | 需自部署 |
| **Waline** | 功能最全（点赞、表情、登录） | 推荐，自部署 |

> 如果接你自己的 V2 后端评论系统，**全部关掉**，自己改组件。

### 8.2 其他插件

| 字段 | 作用 |
|---|---|
| `busuanzi.enable` | 不蒜子访问统计（PV/UV） |
| `recent_comments` | 侧边栏显示最新评论 |
| `copy_protection.enable` | 复制内容时追加版权信息 |
| `copy_protection.author/link/license` | 自定义版权文案（中英文） |
| `aurora_bot.enable` | 右下角对话机器人 |
| `aurora_bot.bot_type` | 机器人形象，目前支持 `"dia"` |
| `mathjax.enable` / `katex.enable` | 数学公式渲染 |
| `mermaid.enable` | 流程图渲染 |

---

## 9. 页脚链接（`theme_config.footer_links`）

数组，每项是一列：
```json
"footer_links": [
  {
    "title": "About",
    "links": [
      { "title": "GitHub", "url": "https://github.com/xxx" },
      { "title": "Email", "url": "mailto:xxx@xxx.com" }
    ]
  },
  {
    "title": "Friends",
    "links": [
      { "title": "Friend A", "url": "https://example.com" }
    ]
  }
]
```

---

## 10. 站点级（hexo 原生字段，非主题）

这些在 `site.json` 根级，对应 hexo 的 `_config.yml`：

| 字段 | 作用 |
|---|---|
| `title` | 站点标题（浏览器 tab 用） |
| `subtitle` | 副标题 |
| `description` | 站点描述（SEO meta） |
| `language` | 站点语言 |
| `timezone` | 时区，如 `"Asia/Shanghai"` |
| `url` | 站点 URL |
| `per_page` | 列表每页文章数 |
| `pagination_dir` | 分页 URL 前缀，默认 `"page"` |

---

## 11. 推荐改动顺序

1. **配色** → `theme.gradient` 三色 + `theme.dark_mode`
2. **品牌** → `site.author` / `site.nick` / `site.description` / `avatar.url`
3. **首屏** → `theme.first_screen.description` + `anime_text` + `type_text`
4. **菜单瘦身** → 删掉 `menu` 里用不到的项
5. **关插件** → `plugins.*` 全部 `enable: false`（接你自己后端后再决定）
6. **社交链接** → `socials` 留你实际用的几个
7. **页脚** → `footer_links` 按需配

---

## 12. 几组流行配色参考

```json
// 默认极光（青-紫-粉）
"gradient": { "color_1": "#24c6dc", "color_2": "#5433ff", "color_3": "#ff0099" }

// 海洋蓝
"gradient": { "color_1": "#43cea2", "color_2": "#185a9d", "color_3": "#0f2027" }

// 日落橙
"gradient": { "color_1": "#ff9966", "color_2": "#ff5e62", "color_3": "#c2185b" }

// 黑金商务
"gradient": { "color_1": "#bf953f", "color_2": "#fcf6ba", "color_3": "#aa771c" }

// 莫兰迪
"gradient": { "color_1": "#d4a5a5", "color_2": "#a8b2c7", "color_3": "#9bb7a8" }

// 赛博朋克
"gradient": { "color_1": "#ff00ff", "color_2": "#00ffff", "color_3": "#ffff00" }
```
