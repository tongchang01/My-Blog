# 02 · 卸包 + 删配套文件

> 一次性删 9 个 npm 包 + 3 个无主文件/目录。

---

## Step 2.1 · 卸 9 个 npm 包

```bash
pnpm remove \
  vite-plugin-html-transformer \
  script-ext-html-webpack-plugin \
  esm \
  semantic-release \
  vue-class-component \
  vue-jest \
  @vue/test-utils \
  @types/jest \
  runjs
```

> 一条命令一起删，避免 `pnpm-lock.yaml` 多次抖动。

**期望输出**（沙盒实测）：

```
dependencies:
- vue-class-component 8.0.0-rc.1

devDependencies:
- @types/jest 29.5.14
- @vue/test-utils 2.4.11
- esm 3.2.25
- runjs 4.4.2
- script-ext-html-webpack-plugin 2.1.5
- semantic-release 21.1.2
- vite-plugin-html-transformer 4.0.0
- vue-jest 3.0.7

Packages: -488
Done in 4.5s using pnpm v11.5.1
```

> `Packages: -488` 是传递依赖（这 9 个包牵出来的子依赖）净减少数。沙盒里 `node_modules/` 体积明显瘦下来。

---

## Step 2.2 · 删 3 个无主文件 / 目录

```bash
rm -f  release.config.js
rm -f  jest.config.js
rm -rf tests/
```

**验证**：

```bash
ls release.config.js jest.config.js tests/ 2>/dev/null
# 期望输出：空
```

---

## Step 2.3 · 同步清理 `tsconfig.json`

`tsconfig.json` 里有两处引用了刚删的东西，不清理 Phase 3a 跑 `tsc --noEmit` 会爆 `Cannot find type definition file for 'jest'`。

打开 `tsconfig.json`，做两处删除：

1. `compilerOptions.types` 数组里删掉 `"jest"`（`@types/jest` 已卸）
2. `include` 数组里删掉 `"tests/**/*.ts"` 和 `"tests/**/*.tsx"`（`tests/` 已删）

改完应该长这样（关键片段）：

```jsonc
{
  "compilerOptions": {
    "types": [
      "vite/client",
      // ❌ 删掉了 "jest"
      "node",
      "vite-plugin-svg-icons/client",
      "vite-plugin-pages/client"
    ]
  },
  "include": [
    "src/**/*.ts",
    "src/**/*.tsx",
    "src/**/*.vue",
    // ❌ 删掉了 "tests/**/*.ts" 和 "tests/**/*.tsx"
    "typings/*.d.ts",
    "vite.config.js"
  ]
}
```

**验证**：

```bash
npx tsc --noEmit 2>&1 | head -5
# 不应有 "Cannot find type definition file for 'jest'"
# 工程里其它历史 TS 错误可能仍在（Phase 4 处理），暂时无视
```

---

## Step 2.4 · 检查 `package.json` 现状

打开 `package.json`，**`dependencies` 应剩 10 个**：

```json
"dependencies": {
  "axios": "^1.5.0",
  "js-cookie": "^3.0.5",
  "normalize.css": "^8.0.1",
  "nprogress": "^0.2.0",
  "pinia": "2.1.6",
  "vue": "^3.3.4",
  "vue-i18n": "^9.2.2",
  "vue-router": "^4.2.4",
  "vue3-click-away": "^1.2.4",
  "vue3-lazyload": "^0.3.8"
}
```

`devDependencies` 应剩 22 个，**不再包含**：

- `vite-plugin-html-transformer`
- `script-ext-html-webpack-plugin`
- `esm`
- `semantic-release`
- `vue-jest`
- `@vue/test-utils`
- `@types/jest`
- `runjs`

---

## ⚠️ 不要再 `pnpm install`

`pnpm remove` 已经把 `node_modules/` 和 `pnpm-lock.yaml` 同步更新好了。这时**不需要**再跑一次 `pnpm install`——多此一举，且 pnpm 11 严格模式可能弹 `ERR_PNPM_IGNORED_BUILDS` 干扰判断。

直接进 [03-verify.md](./03-verify.md)。

---

## 如果 `pnpm remove` 报错

| 报错关键字 | 原因 + 修复 |
|---|---|
| `not installed` | 该包本来就不在 `dependencies` / `devDependencies` 里——把它从命令里去掉重试 |
| `lockfile out of sync` | 先 `pnpm install`（不带 `--frozen-lockfile`）刷新 lock，再 `pnpm remove` |
| Windows `EPERM` / `EBUSY` | 关掉占用 `node_modules` 的进程（vite dev / 编辑器 watcher），重试 |
