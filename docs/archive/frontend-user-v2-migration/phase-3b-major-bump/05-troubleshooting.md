# 05 · Troubleshooting

> Phase 3b 沙盒里实际踩到的 4 个坑 + 其它常见症状。

---

## A. `vite build` 报 `"@vitejs/plugin-vue" resolved to an ESM file`

**症状**（Step 3b.1 后）：

```
ERROR: [plugin: externalize-deps] "@vitejs/plugin-vue" resolved to an ESM file.
ESM file cannot be loaded by `require`.
failed to load config from vite.config.js
```

**根因**：`vite.config.js` 被 Node 当 CJS 加载（因为 `package.json` 没 `"type": "module"`），但 Vite 5 + plugin-vue v6 都是 ESM-only。

**修复**：

```bash
git mv vite.config.js vite.config.mjs
```

> 这就是 [02-vite-5.md](./02-vite-5.md) Step 2.2 强调的事，但万一漏做就会撞这个。

---

## B. ESLint 报 13822 个 `Delete '␍'` (Windows)

**症状**（Step 3b.2 后第一次跑 `eslint .`）：

```
xxx.ts
  1:23  error  Delete `␍`  prettier/prettier
  2:5   error  Delete `␍`  prettier/prettier
  ... (几千行)
✖ 13822 problems (13815 errors, 7 warnings)
```

**根因**：git 默认 `core.autocrlf=true`，仓库存 LF，checkout 后变 CRLF；prettier 默认要求 LF，每行末尾的 `\r` 都报错。

**修复**：编辑 `.prettierrc` 加一行：

```json
{
  ...
  "endOfLine": "auto"
}
```

再跑 `eslint .`，应降到几十个真实问题。

---

## C. ESLint 报 `Could not find config file` 或类似

**根因**：删了 `.eslintrc.js` 但没建 `eslint.config.mjs`，或者建在了错地方。

**检查**：

```bash
ls eslint.config.* 2>/dev/null
# 期望：eslint.config.mjs
```

**修复**：重新按 [03-eslint-9.md Step 3.3](./03-eslint-9.md) 创建。

---

## D. `pnpm up eslint@~9` 后 `eslint .` 报 `Cannot find package 'globals'`

**根因**：flat config 用到 `globals` 包但忘了安装。

**修复**：

```bash
pnpm add -D globals
```

---

## E. ESLint 报 `defineConfigWithVueTs is not a function`

**根因**：`@vue/eslint-config-typescript` 没升到 14+。v11 没有这个 helper。

**检查**：

```bash
node -e "console.log(require('./package.json').devDependencies['@vue/eslint-config-typescript'])"
# 期望：^14.x.x
```

**修复**：

```bash
pnpm up @vue/eslint-config-typescript@latest
```

---

## F. `pnpm-workspace.yaml` 又冒出来

老朋友。同 Phase 3a 处理：

```bash
rm -f pnpm-workspace.yaml
grep -q pnpm-workspace.yaml .gitignore || echo "pnpm-workspace.yaml" >> .gitignore
```

---

## G. `vite build` 在 Vite 5 下输出文件名都是 `Abc123-XyZ.js`

**不是 bug**。Vite 5 默认用 `base64url` 算 hash，所以文件名风格变了（Vite 4 是 hex）。

如果你想保留 Vite 4 风格：

```js
// vite.config.mjs
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        entryFileNames: 'static/js/[name]-[hash:8].js',
        chunkFileNames: 'static/js/[name]-[hash:8].js',
        assetFileNames: 'static/[ext]/[name]-[hash:8].[ext]'
      }
    }
  }
})
```

> 一般没必要改——除非 CDN 缓存 / SRI 校验依赖固定文件名。

---

## H. 想只回退 ESLint 部分（保留 Vite 5）

```bash
git reset --hard phase-3b-1-done
```

想整个回退 Phase 3b：

```bash
git reset --hard pre-deps-major
pnpm install
```

---

## 没列在这里的报错

逐项跑 [04-verify.md](./04-verify.md) 的 5 项验收，定位失败项后把报错完整复制出来再具体查。
