# Phase 0 — 从零搭建本地预览环境

> **目标**：在一台干净的电脑上，从 GitHub 拉下原始 Aurora 主题代码，按本目录文档操作，最终在浏览器里看到一个能跑的本地预览站。
>
> **不做什么**：本阶段**不改任何业务代码**、**不接后端**、**不删功能**。只解决"能跑"的问题。

---

## 阅读顺序

| # | 文档 | 做什么 |
|---|---|---|
| 1 | [01-environment.md](./01-environment.md) | 装 Node / pnpm，对齐版本基线 |
| 2 | [02-clone-and-install.md](./02-clone-and-install.md) | 克隆原始仓库 + 安装依赖 + 处理已知坑 |
| 3 | [03-local-modifications.md](./03-local-modifications.md) | 改 `index.html` 和 `vite.config.js`（脱离 Hexo 预览模式） |
| 4 | [04-mock-data.md](./04-mock-data.md) | 把 `mock-data/` 下的 JSON 拷到工程 `public/api/` |
| 5 | [05-start-and-verify.md](./05-start-and-verify.md) | 启动 vite + 逐项验收 |
| 6 | [06-troubleshooting.md](./06-troubleshooting.md) | 常见报错对照表 |

附属：[mock-data/](./mock-data/) — 9 个 JSON 文件，是本阶段唯一的"数据"。

---

## 验收标准

走完本目录后，应满足：

- [ ] `./node_modules/.bin/vite` 能起来，端口 `5173`
- [ ] http://localhost:5173 打开是 Aurora 首页（紫色渐变背景，有文章列表）
- [ ] 控制台**无红色报错**（黄色警告可忽略）
- [ ] 点击文章详情、分类、标签、归档页都能渲染
- [ ] 切换 中/英 语言不报错（日语在本阶段尚未加）

任何一项不达标，**回到 [06-troubleshooting.md](./06-troubleshooting.md)** 对照排查，不要跳到下一阶段。

---

## 关于"本地修改"

原始 Aurora 是为 Hexo 静态生成场景设计的——它假设有一个 Hexo 服务在 `localhost:4000` 提供 API，并且通过 `vite-plugin-html-transformer` 从 `templates/index.html` 动态注入入口。我们不跑 Hexo，所以要做两件事：

1. 在工程根目录**新建** `index.html`（绕开 `templates/` 那条链，[03](./03-local-modifications.md)）
2. 自己建 `public/api/` 把 mock JSON 丢进去，让 vite dev server 当静态文件返回（[04](./04-mock-data.md)）

这两步是**临时桥**——`index.html` 在 **Phase 1 脱钩 Hexo** 时就会"转正"（templates 删了它就是唯一入口），mock 数据在 **Phase 7 V2 后端对接** 时换成真实 API。但本阶段必须有它们，否则首页空白。

---

## 检查点

走完本阶段，应在心里能回答：

1. 为什么不直接用 `pnpm serve`？（→ 02）
2. 为什么要在根目录新建 `index.html` 而不是改 `templates/index.html`？（→ 03）
3. 改 mock 数据后页面没变化怎么办？（→ 04）

回答不出来就再读一遍对应文档。
