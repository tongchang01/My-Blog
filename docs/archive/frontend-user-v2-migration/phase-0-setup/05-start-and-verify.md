# 05 — 启动 + 验收

## 启动

**用 `vite` 二进制直接启动，不要用 `pnpm serve`**：

```bash
# 在工程根目录
./node_modules/.bin/vite
```

Windows 下 Git Bash 也用这个命令（forward slash）。

### 为什么不用 `pnpm serve`

`package.json` 里的 `serve` 脚本会触发 pnpm 的依赖完整性检查。pnpm 11 严格模式下，因为 [02](./02-clone-and-install.md) 提到的 ignored builds 问题，会直接报错退出。

`./node_modules/.bin/vite` 绕过 pnpm，直接调 vite 二进制。等同于 `npx vite`，但不会触发额外的依赖解析。

### 预期输出

```
  VITE v4.4.9  ready in xxx ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

端口是 **5173**（vite 默认）。

> 老文档里有写 4173 的，那是预览模式（`vite preview`）的端口，开发模式是 5173。看到 5173 是对的。

## 浏览器验收

打开 http://localhost:5173/，按顺序检查：

### 1. 首页（`/`）

- [ ] 背景是紫色/青色渐变（不是白屏，不是黑屏）
- [ ] 顶部有 logo + 菜单（Home / Categories / Tags / Archives 等）
- [ ] 中间能看到至少 1 篇文章卡片
- [ ] 右下角有"返回顶部"按钮（滚动后出现）

### 2. 文章页（`/posts/xxx`）

- [ ] 点首页任意一篇文章 → 跳到详情页
- [ ] 正文可读，markdown 排版正常
- [ ] 侧边有目录（TOC）

### 3. 分类页（`/categories`）

- [ ] 能看到分类列表
- [ ] 点击某个分类能跳进去看到该分类的文章

### 4. 标签页（`/tags`）

- [ ] 能看到标签云
- [ ] 点击标签能进入对应列表

### 5. 归档页（`/archives`）

- [ ] 按年份/月份分组显示文章

### 6. 语言切换

- [ ] 顶部有语言切换图标
- [ ] 切到中文：菜单文字变中文，不报错
- [ ] 切回英文：恢复原样

### 7. 深色模式

- [ ] 顶部有日/月图标
- [ ] 切换后整站配色反转，无白屏闪烁

### 8. 控制台

按 F12 打开 DevTools：

- [ ] **Console 没有红色 Error**（黄色 Warning 可以无视）
- [ ] **Network 里 `/api/*.json` 全部 200**（没有 404 / 500）
- [ ] 字体加载 200 或 from cache

## 页面 ↔ API 对照

如果某个页面白屏或报错，先去 Network 看是哪个请求挂了：

| 页面 | 必需 API |
|---|---|
| 首页 | `site.json` + `posts/1.json` + `features.json` + `statistic.json` |
| 文章详情 | `site.json` + `posts/{id}.json`（mock 只到 1） |
| 分类列表 | `categories.json` |
| 标签列表 | `tags.json` |
| 归档 | `archives/1.json` |
| 搜索 | `search.json` |
| 作者卡片 | `authors/blog-author.json` |

某个 404 → 回 [04](./04-mock-data.md) 确认拷贝完整。

## 停止 dev server

终端里 `Ctrl + C`。

Windows 下如果端口卡住没释放：

```bash
# Git Bash / WSL
netstat -ano | grep 5173
# 看到 PID 后
taskkill //PID <pid> //F
```

## 检查点

走到这里如果 8 项验收全过，**Phase 0 完成**。

任何一项不过，去 [06-troubleshooting.md](./06-troubleshooting.md)。

## 下一步

Phase 0 终点。后续阶段（Phase 1+）的文档由后续目录承载，本目录到此为止。
