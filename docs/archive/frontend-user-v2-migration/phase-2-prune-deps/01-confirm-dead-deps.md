# 01 · 确认 9 个候选包零业务引用

> 不要看到 planning 文档列了就直接删——**亲自 grep 一遍**。包名一字之差可能误删活的依赖。

---

## 一键 grep 命令

进入工程根目录，执行：

```bash
for pkg in \
  "vite-plugin-html-transformer" \
  "script-ext-html-webpack-plugin" \
  "esm" \
  "semantic-release" \
  "vue-class-component" \
  "vue-jest" \
  "@vue/test-utils" \
  "@types/jest" \
  "runjs"; do
  echo "=== $pkg ==="
  grep -rn "$pkg" \
    --include="*.ts" --include="*.tsx" \
    --include="*.js" --include="*.jsx" \
    --include="*.vue" --include="*.json" \
    --include="*.cjs" --include="*.mjs" \
    --include="*.config.*" \
    --exclude-dir=node_modules --exclude-dir=dist \
    --exclude="pnpm-lock.yaml" --exclude="package-lock.json" \
    . 2>/dev/null | head -10
done
```

---

## 沙盒实测输出 + 判定

| 包 | grep 命中 | 判定 |
|---|---|---|
| `vite-plugin-html-transformer` | 仅 `package.json` | ✅ 纯死 → 删 |
| `script-ext-html-webpack-plugin` | 仅 `package.json` | ✅ 纯死 → 删 |
| `vue-class-component` | 仅 `package.json` | ✅ 纯死 → 删 |
| `@types/jest` | 仅 `package.json` | ✅ 纯死 → 删 |
| `runjs` | 仅 `package.json` | ✅ 纯死（Phase 1 已删 `build/scripts/`）→ 删 |
| `esm` | `package.json` + `src/shims-vue.d.ts:11` | ⚠️ src 命中是文件名 `vue-easy-lightbox.esm.min.js` 里的 `.esm.` 子串，不是 `esm` 包 → 删 |
| `semantic-release` | `package.json` + `release.config.js` 6 行 | ⚠️ release.config.js 是配套——**连文件一起删** |
| `vue-jest` | `package.json` + `jest.config.js:4` | ⚠️ jest.config.js 是配套——**连文件一起删** |
| `@vue/test-utils` | `package.json` + `tests/unit/components/Dropdown.spec.ts` + `tests/unit/components/Toggle.spec.ts` | ⚠️ 2 个 stub spec；`package.json` 也没 `test` script 调它们 → **连 `tests/` 一起删** |

---

## ⚠️ 判定原则

- **只在 `package.json` 命中** → 直接删
- **在配置文件命中**（`release.config.js` / `jest.config.js`）→ 删包 + 删该配置文件
- **在 `src/**` 命中实质 `import` / `require`** → ⛔ **不能删**，停下来重新评估
- **在 `src/**` 命中但只是文件名 / 注释 / 字符串字面量** → 可删（如本例的 `esm` 字符串）

> 看到 grep 输出有 src 命中时，要**逐行读上下文**判断是不是真依赖。`grep --include="*.ts"` 默认输出含行号，肉眼几秒就能分辨。

---

## 你不动的 22 个 devDeps（参考）

| 包 | 用途 |
|---|---|
| `@vitejs/plugin-vue` | Vue SFC 编译——核心 |
| `vite` / `vite-plugin-pages` / `vite-plugin-svg-icons` | 构建工具 |
| `vue-easy-lightbox` | 图片灯箱组件（Phase 5 视情况删）|
| `vue3-scroll-spy` | TOC 滚动高亮 |
| `tailwindcss` / `postcss` / `autoprefixer` / `sass` | 样式工具链 |
| `eslint` 全家桶 + `prettier` | Lint / 格式化 |
| `typescript` + `@types/*`（node / js-cookie / nprogress）| 类型 |
| `husky` / `@commitlint/*` | Git hook + commit 规范（Phase 9 评估）|

> 暂时**全部保留**。Phase 5 / Phase 9 各阶段再决定是否再清。

---

## 完成后

确认 9 个包的 grep 输出都符合上表判定 → 进入 [02-remove-packages.md](./02-remove-packages.md)。
