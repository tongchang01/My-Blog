# 02 · 改 `vite.config.js`

> 上一步删了 `templates/` 和 `build/scripts/`，现在 vite.config 里**指向这些路径的代码必须配套删掉**，否则 dev 启动会报"找不到 templates/index.html"。

---

## 改动概览

| 删除内容 | 原因 |
|---|---|
| `import { createHtmlPlugin } from 'vite-plugin-html-transformer'` | 插件已不需要（模板都没了） |
| `plugins` 数组里的 `createHtmlPlugin({...})` 调用 | 同上 |
| `filenamePath` / `templatePath` 变量 + 三元判断 | 服务于 createHtmlPlugin，连根 |
| `build.outDir: 'source'` | Hexo 约定，改回 Vite 默认 `dist`（即删掉此行） |
| `server.proxy` 里 `/api`、`/assets` 指向 `localhost:4000` | Hexo dev server 端口；Phase 0 已注释，本步物理删 |

> 注意：这里**不卸载** `vite-plugin-html-transformer` 这个 npm 包，那是 Phase 2 的事。本步只是让 vite.config 不再 import 它。

---

## 最终内容

整个 `vite.config.js` 替换为：

```js
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import Pages from 'vite-plugin-pages'
import path from 'path'

// https://vitejs.dev/config/
export default ({ mode }) => {
  process.env = { ...process.env, ...loadEnv(mode, process.cwd()) }

  return defineConfig({
    build: {
      assetsDir: 'static',
      rollupOptions: {
        output: {
          assetFileNames: assetInfo => {
            let extType = assetInfo.name.split('.').at(1)
            if (/png|jpe?g|svg|gif|tiff|bmp|ico/i.test(extType)) {
              extType = 'img'
            }
            return `static/${extType}/[hash][extname]`
          },
          chunkFileNames: 'static/js/[hash].js',
          entryFileNames: 'static/js/[hash].js'
        },
        plugins: []
      }
    },
    plugins: [
      createSvgIconsPlugin({
        iconDirs: [path.resolve(process.cwd(), 'src/icons')],
        symbolId: 'icon-[dir]-[name]',
        customDomId: '__svg__icons__dom__'
      }),
      vue(),
      Pages({})
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      },
      extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
    },
    server: {}
  })
}
```

---

## 与原始 vite.config.js 的 diff（要点）

| 段落 | 原始 | 改后 |
|---|---|---|
| import 块 | 含 `createHtmlPlugin` | 不含 |
| `VITE_MODE` / `filenamePath` / `templatePath` 三个 const | 在 export 函数顶部 | 完全删掉 |
| `build.outDir` | `'source'` | 不写（默认 `dist`） |
| `build.assetsDir` | `'static'` | 保留 |
| `plugins` 数组 | `createHtmlPlugin({...})` + svg + vue + Pages | 仅 svg + vue + Pages |
| `server.proxy` | `/api`、`/assets` → `http://localhost:4000` | 完全删掉，留 `server: {}` 占位 |

> `server: {}` 留空是为 Phase 7 V2 后端对接时再加 proxy。

---

## 完成后

```bash
# 临时验证，**只检查启动不报"找不到 templates"**
# 期望：能起、能编、首页能开
./node_modules/.bin/vite --port 5180
# Ctrl+C 关掉

# 如果起不来，看 05-troubleshooting.md
```

成功则进入 [03-clean-package-json.md](./03-clean-package-json.md)。
