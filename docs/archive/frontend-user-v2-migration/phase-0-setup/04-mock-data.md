# 04 — Mock 数据

> 本阶段没有真实后端。Aurora 启动时会请求一堆 `/api/*.json`，我们把准备好的 JSON 文件丢到 `public/api/` 下，vite 会当静态文件返回，前端就以为后端还活着。
>
> **重要**：干净 clone 下来的工程**没有** `public/api/` 目录（`public/` 下只有 `favicon.ico` 和 `icons/`），需要你自己建。

## 拷贝

从本文档的同级目录 [`mock-data/`](./mock-data/) 拷贝所有内容到工程的 `public/api/`：

```bash
# 假设你在工程根目录 hexo-theme-aurora-main/
mkdir -p public/api
cp -r <DOCS_ROOT>/phase-0-setup/mock-data/* public/api/
```

把 `<DOCS_ROOT>` 替换成本文档集所在路径，例如 `C:/tyb/aurora-v2-docs`。

## 完整文件清单

拷完后 `public/api/` 应该是这样的：

```
public/api/
├── site.json              # 站点配置（主题色、菜单、社交链接、SEO 等）
├── statistic.json         # 统计数字（文章数、分类数、标签数）
├── categories.json        # 分类列表
├── tags.json              # 标签列表
├── features.json          # 首页推荐文章
├── search.json            # 搜索索引（mock 是空数组）
├── posts/
│   └── 1.json             # 文章列表第 1 页
├── archives/
│   └── 1.json             # 归档列表第 1 页
└── authors/
    └── blog-author.json   # 默认作者信息
```

共 9 个文件。

## 各文件作用对照

| URL | 文件 | 渲染哪个页面 |
|---|---|---|
| `/api/site.json` | `site.json` | 全局（菜单、配色、站点信息） |
| `/api/statistic.json` | `statistic.json` | 侧边栏数字徽章 |
| `/api/categories.json` | `categories.json` | 分类页 |
| `/api/tags.json` | `tags.json` | 标签云 |
| `/api/features.json` | `features.json` | 首页"特色文章" |
| `/api/posts/1.json` | `posts/1.json` | 首页文章流 / 列表页 |
| `/api/archives/1.json` | `archives/1.json` | 归档页 |
| `/api/authors/blog-author.json` | `authors/blog-author.json` | 作者卡片 |
| `/api/search.json` | `search.json` | 搜索弹窗 |

## 关键固定值

`site.json` 里有几个值是**主题正常运行必须的**，改之前先想清楚：

| 字段 | 当前值 | 改了会怎样 |
|---|---|---|
| `theme.primary_color_1/2/3` | `#06b6d4 / #6366f1 / #8b5cf6` | 全站渐变色，改了配色全变 |
| `theme.dark_mode` | `auto` | 改成 `on`/`off` 强制单一模式 |
| `theme.language` | `en` | 切默认语言（zh/en/ja 待 Phase 5 补） |
| `site.subtitle.enable` | `true` | 首页大标题下的副标题 |
| `socials` | GitHub/Twitter/... | 头部右侧图标，空数组会让顶栏右侧空荡 |

## 改 mock 数据

预览中想测试不同数据，直接编辑 `public/api/xxx.json`：

- **加文章**：在 `posts/1.json` 的 `data` 数组里加一项（参考已有项的字段结构）
- **改菜单**：编辑 `site.json` 里 `menu` 数组
- **换头像**：改 `authors/blog-author.json` 里的 `avatar` URL

⚠️ **改完要刷新浏览器**（vite 不会自动 reload `public/` 下的 JSON）。

## 不要在 mock 阶段做的事

| 别做 | 原因 |
|---|---|
| 给文章正文加复杂 markdown | mock 数据是简化版，正文渲染规则在 Phase 4 才理顺 |
| 加新的 API endpoint | 没改源码，加了文件前端也不知道去请求 |
| 把 JSON 改成数组以外的结构 | 主题对每个文件的 schema 有期望，改坏了就白屏 |

如果想看真实的 schema 字段含义，去工程源码 `src/models/` 下找 TypeScript 类型定义。

## 检查点

```bash
ls public/api/                  # 应该看到 9 个条目（含子目录）
ls public/api/posts/            # 应该看到 1.json
ls public/api/archives/         # 应该看到 1.json
ls public/api/authors/          # 应该看到 blog-author.json
```

## 下一步

[05-start-and-verify.md](./05-start-and-verify.md) — 启动 + 验收
