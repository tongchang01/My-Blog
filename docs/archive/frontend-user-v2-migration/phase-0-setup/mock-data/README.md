# mock-data/

> Phase 0 用的 9 个 mock JSON 文件，覆盖到工程的 `public/api/` 下即可让 Aurora 主题本地跑起来，不依赖后端。

## 怎么用

见上一级 [`../04-mock-data.md`](../04-mock-data.md)。一句话：

```bash
cp -r ./* <工程根>/public/api/
```

## 文件清单

| 文件 | 大小 | 内容 |
|---|---|---|
| `site.json` | ~4 KB | 站点级配置（主题色、菜单、社交链接、SEO） |
| `statistic.json` | ~70 B | 文章/分类/标签计数 |
| `categories.json` | ~330 B | 分类列表 |
| `tags.json` | ~700 B | 标签列表 |
| `features.json` | ~3 KB | 首页推荐文章 |
| `search.json` | 空数组 | 搜索索引（mock 为空） |
| `posts/1.json` | ~7 KB | 文章列表第 1 页（含 1 篇示例文章） |
| `archives/1.json` | ~7 KB | 归档第 1 页 |
| `authors/blog-author.json` | ~1.6 KB | 默认作者信息 |

## 来源

这些 JSON 是用 Aurora 上游仓库的 `npm run mock`（或类似命令）从默认示例数据生成，再手动调整过：

- 主题色锁定为 `#06b6d4 / #6366f1 / #8b5cf6`
- 站点名、菜单调整为占位文字
- 文章内容简化为最小可渲染样例

## 想改

直接在 `<工程根>/public/api/xxx.json` 改，不要回头改本目录的副本（除非你想把改动沉淀到文档里）。改完浏览器 `Ctrl + Shift + R` 硬刷新。

## 不要做

- ❌ 改 schema 结构（key 名、嵌套层级），主题代码硬编码了这些字段
- ❌ 加新的 endpoint 文件，没用——没人去请求
- ❌ 把 mock 数据当成真实数据维护，Phase 1 之后会被真实后端 API 替代
