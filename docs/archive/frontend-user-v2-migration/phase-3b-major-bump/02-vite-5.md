# 02 · Step 3b.1 · Vite 4 → 5

> Vite + 两个相关插件一起升。**关键陷阱**：必须把 `vite.config.js` 改名为 `vite.config.mjs`，否则 build 直接挂。

---

## 为什么要改名

工程 `package.json` 没设 `"type": "module"`，所以 `.js` 文件默认按 CJS 加载。
但 Vite 5 + `@vitejs/plugin-vue` v6 都是 **ESM-only**，CJS 不能 `require()` ESM 包。

**沙盒实测**：第一次直接 `pnpm up vite@~5` 后 `vite build` 报错：

```
ERROR: [plugin: externalize-deps] "@vitejs/plugin-vue" resolved to an ESM file.
ESM file cannot be loaded by `require`.
failed to load config from vite.config.js
```

修复 = 一行 `git mv`。

---

## Step 2.1 · 升级 Vite 套件

```bash
pnpm up vite@~5 @vitejs/plugin-vue@~6 vite-plugin-pages@latest vite-plugin-svg-icons@latest
```

> 这次 `@vitejs/plugin-vue` 才能 `~6`——Vite 5 终于支持 ESM 配置加载，前提是配置文件用 `.mjs`。

**沙盒实测输出**（关键片段）：

```
devDependencies:
- @vitejs/plugin-vue 5.2.4
+ @vitejs/plugin-vue 6.0.7
- vite 4.5.14
+ vite 5.4.21
- vite-plugin-pages 0.31.0
+ vite-plugin-pages 0.33.3

Packages: +22 -12
```

`vite-plugin-svg-icons` 没列出 = 它已经是最新。

---

## Step 2.2 · 改名 `vite.config.js` → `vite.config.mjs`

```bash
git mv vite.config.js vite.config.mjs
```

> 用 `git mv` 而不是 `mv`，git 历史能识别为 rename（diff 才好读）。

**不需要改内容**：原文件就是 ESM `import` 语法，只是扩展名让 Node 误判为 CJS。

---

## Step 2.3 · 验证 Vite 5

```bash
# build
./node_modules/.bin/vite build 2>&1 | tail -5
# 期望：✓ built in xxx ms

# dev
./node_modules/.bin/vite
# 期望：VITE v5.4.x  ready in xxx ms
# 浏览器 http://localhost:5173/ 首页正常
# Ctrl+C
```

> **常见症状**：build 还报 ESM 错误 → 检查 `ls vite.config.*` 看是不是 `.mjs`，可能 `git mv` 后又被工具改回去了。

---

## Step 2.4 · commit + 子 tag

```bash
git add -A
git status   # 期望：package.json / pnpm-lock.yaml / vite.config.js → vite.config.mjs

git commit -m "phase 3b.1: Vite 4 -> 5 (+ plugin-vue 6, vite-plugin-pages 0.33, rename vite.config.js -> .mjs)"

git tag phase-3b-1-done
```

---

→ [03-eslint-9.md](./03-eslint-9.md)
