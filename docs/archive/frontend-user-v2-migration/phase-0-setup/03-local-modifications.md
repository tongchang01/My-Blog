# 03 — 本地代码改造

> 这一步只改两个文件，目的是让 vite dev server **不依赖 Hexo 预览环境**就能起来。

## 改动 1：在工程根目录**新建** `index.html`

### 背景

干净仓库的根目录**没有** `index.html`。它的位置在 `templates/index.html`，由 `vite-plugin-html-transformer` 在 vite 启动时动态注入（见 `vite.config.js` 里的 `createHtmlPlugin` 调用）。

这套机制服务于 Hexo 集成场景（开发用 `templates/index.html`、生产用 `templates/index_prod.html` → 输出到 `../layout/index.ejs`）。在我们这种"脱离 Hexo 的纯 SPA 预览"下，这条链很容易踩坑——而且 `templates/index.html` 引了一堆评论插件（gitalk/valine/twikoo/waline 等）CDN，我们都不用。

最简单的破解：**在工程根目录新建一个 `index.html`**。vite 的入口解析优先级是先看根目录 `index.html`，找到就直接用，不走 `createHtmlPlugin` 那一套。

### 操作

在工程根目录创建 `index.html`，内容：

```html
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta name="viewport" content="width=device-width,initial-scale=1.0" />
    <link rel="icon" href="/favicon.ico" />
    <script src="https://unpkg.com/blueimp-md5@^2.19.0/js/md5.min.js"></script>
    <script src="https://unpkg.com/lodash@^4.17.21/lodash.min.js"></script>
    <link rel="stylesheet" href="https://fonts.loli.net/css?family=Rubik" />
    <title>Aurora Dev</title>
  </head>
  <body id="body-container">
    <noscript>This app requires JavaScript.</noscript>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

### `templates/` 下的两个文件不要动

| 文件 | 不要动的原因 |
|---|---|
| `templates/index.html` | 是上游主题保留功能，将来如果你想接回 Hexo 还会用 |
| `templates/index_prod.html` | 同上，生产构建路径依赖 |

我们只是用"根 index.html 优先"绕开了它们，没有删它们。

### 关键点

| 元素 | 作用 | 能不能删 |
|---|---|---|
| `md5.min.js` (CDN) | gravatar 头像哈希需要 | 不能（运行时会报 `md5 is not defined`） |
| `lodash.min.js` (CDN) | 部分组件用 `_.xxx` 直接挂 window | 不能 |
| `fonts.loli.net` | Rubik 字体 | 可以删，但 UI 会退回系统字体 |
| `#body-container` | 主题 SCSS 选择器锚点 | 不能删，删了背景渐变会消失 |
| `<div id="app">` | Vue 挂载点 | 不能删 |

> ⚠️ 三个 CDN 走的是公网。家里电脑如果科学上网不稳，可以换成国内镜像或下载到本地 `public/vendor/`——后续 phase 视需要再处理。本阶段先用 CDN。

## 改动 2：`vite.config.js`

### 现状

原配置里 `server.proxy` 把 `/api` 和 `/assets` 转发到 `http://localhost:4000`（指向 Hexo dev server）。我们没有 Hexo，所以要把整块 `proxy` 注释掉。

### 改成

打开 `vite.config.js`，找到 `server: { ... }` 块，**把 `proxy` 整块注释掉**：

```js
server: {
  // Proxy disabled for local preview — mock JSON served from public/api/
  // proxy: {
  //   '/api': {
  //     target: 'http://localhost:4000/api',
  //     changeOrigin: true,
  //     rewrite: path => path.replace(/^\/api/, '')
  //   },
  //   '/assets': {
  //     target: 'http://localhost:4000/assets',
  //     changeOrigin: true,
  //     rewrite: path => path.replace(/^\/assets/, '')
  //   }
  // }
}
```

### 为什么

- `/api/*.json` 的请求会优先匹配 `public/api/*.json`（vite 默认行为）
- 只有当 `public/api/` 里**没有**对应文件时，才会走 proxy
- 如果 proxy 配着，但 4000 端口没人，会偶发的连接超时报错

所以注释掉是更干净的做法。

## 不要动的文件

| 文件 | 原因 |
|---|---|
| `package.json` | scripts、deps 都不改 |
| `tsconfig.json` | 不改 |
| `src/**` 任何文件 | Phase 1 才动业务代码 |
| `_config.*.yml` | 是 Hexo 端的配置，跟 vite 预览无关 |

## 检查点

```bash
# 验证 index.html 已改
grep -c 'Aurora Dev' index.html   # 应该输出 1

# 验证 vite.config.js 已注释 proxy
grep -c '// proxy:' vite.config.js   # 应该输出 1
```

## 下一步

[04-mock-data.md](./04-mock-data.md) — 拷 mock JSON
