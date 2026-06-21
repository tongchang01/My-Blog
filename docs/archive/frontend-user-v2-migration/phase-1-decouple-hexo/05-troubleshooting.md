# 05 · Troubleshooting

> Phase 1 沙盒验证时实际踩过 / 容易踩到的坑。按报错形态查。

---

## A. `vite` 启动报 `Cannot find module 'vite-plugin-html-transformer'` 或类似

**原因**：02 步漏改 `vite.config.js`——还留着 `import { createHtmlPlugin } from 'vite-plugin-html-transformer'`。

**修复**：

```bash
grep -n "html-transformer\|createHtmlPlugin\|filenamePath\|templatePath" vite.config.js
# 任何命中都要按 02 文档删掉
```

注意：**别去 `pnpm add vite-plugin-html-transformer` 把它装回来**——Phase 2 还要把它删干净。

---

## B. dev 启动后浏览器空白 / 报 `Failed to fetch /api/xxx.json`

**原因**：Phase 0 的 mock 数据没在 `public/api/` 下。

**确认**：

```bash
ls public/api/
# 期望看到 posts.json / site.json 等
```

如果空 → 回 Phase 0 [04-mock-data.md](../phase-0-setup/04-mock-data.md) 把 mock 文件补上。

---

## C. `vite build` 输出到 `source/` 而不是 `dist/`

**原因**：02 步没删 `vite.config.js` 里 `build.outDir: 'source'` 一行。

**修复**：打开 vite.config.js 找 `outDir` → 直接删那一行（不是改成 `'dist'`——不写就是默认 dist）。

```bash
grep -n "outDir" vite.config.js
# 期望 0 命中
```

---

## D. `pnpm install` 报 `ERR_PNPM_IGNORED_BUILDS`

**原因**：pnpm 10+ 默认严格模式，会拒绝运行未明确允许的 postinstall 脚本（typescript / esbuild / vue-demi 等）。

**Phase 1 不需要解决**——只需绕过去：

```bash
# 方案 1：直接调 vite，跳过 npm scripts 包装
./node_modules/.bin/vite
./node_modules/.bin/vite build

# 方案 2：临时降到 npm
npm install
npm run dev
```

彻底解决放到 Phase 2/3 升级时一起处理（在 `package.json` 加 `pnpm.onlyBuiltDependencies` 白名单）。

---

## E. grep 时 `node_modules` 命中爆屏

**原因**：04-verify.md 的 grep 命令漏加 `--exclude-dir=node_modules`。

**修复**：把验收 grep 完整复制，注意尾部 `--exclude-dir=node_modules --exclude-dir=dist`：

```bash
grep -rni "hexo" \
  --include="*.yml" --include="*.yaml" \
  --include="*.config.js" --include="*.config.ts" \
  --include="package.json" --include="index.html" \
  --include="tsconfig.json" \
  --exclude-dir=node_modules --exclude-dir=dist \
  .
```

---

## F. 验收 grep 命中 `src/api/index.ts:fetchHexoConfig`

**不是 bug**——这是 src/ 业务代码，留给 Phase 7。

确认你的 grep 命令里**没有** `--include="*.ts"` / `--include="*.vue"`。如果是按 04-verify.md 写的命令（限制到 `*.yml` / `*.config.js` / `package.json` / `index.html` / `tsconfig.json`），就不会命中 src/。

---

## G. 根 `index.html` 不存在 / 起 dev 时 404

**原因**：Phase 0 没在根目录建 `index.html`，靠 `vite-plugin-html-transformer` 注入了 `templates/index.html`——现在 templates 删了，没有 fallback。

**修复**：补根 `index.html`（Phase 0 [03-local-modifications.md](../phase-0-setup/03-local-modifications.md) 里的版本）：

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
    <title>MyBlog</title>
  </head>
  <body id="body-container">
    <noscript>This app requires JavaScript.</noscript>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

> `<title>` 改成你的站名（Phase 1 已经在做 rebranding，正好一起改）。

---

## H. commit 时 husky / commitlint 拒绝

**原因**：commit message 不符合 conventional commits 规范。

**修复 1（推荐）**：按规范写：

```bash
git commit -m "phase 1: decouple from hexo"
# type:scope: subject  ← phase 1 也是合法的 type 用法（被 commitlint 默认配置接受）
```

如果还是拒，看具体报错——大概率要求 type 在 `feat/fix/chore/...` 集合里：

```bash
git commit -m "chore: phase 1 decouple from hexo"
```

**修复 2（应急）**：临时跳过 hook：

```bash
git commit --no-verify -m "phase 1: decouple from hexo"
```

> 如果你 Phase 2 决定把 commitlint 删掉，这个问题自动消失。

---

## 没列在这里的报错

回到 [04-verify.md](./04-verify.md) 的四项验收逐项跑，定位是哪一项失败 → 把报错复制出来再具体查。
