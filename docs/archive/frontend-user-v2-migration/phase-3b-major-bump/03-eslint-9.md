# 03 · Step 3b.2 · ESLint 8 → 9（flat config 迁移）

> ESLint 9 强制 flat config，旧的 `.eslintrc.js` / `.eslintignore` 完全废弃。要重写 + 升级生态。

---

## 改动总览

| 操作 | 对象 |
|---|---|
| 升级 | `eslint` 8 → 9 |
| 升级 | `@typescript-eslint/*` 6 → 8 |
| 升级 | `eslint-plugin-vue` 9 → 10 |
| 升级 | `@vue/eslint-config-typescript` 11 → 14 |
| 升级 | `@vue/eslint-config-prettier` 8 → 10 |
| 升级 | `eslint-plugin-prettier`、`prettier` 跟最新 |
| 新增 | `globals` 包（flat config 需要） |
| 删 | `.eslintrc.js` |
| 删 | `.eslintignore`（flat config 内部表达 ignore） |
| 新建 | `eslint.config.mjs` |
| 改 | `package.json` 的 `lint` 脚本（v9 没有 `--ext` 了） |
| 改 | `.prettierrc` 加 `endOfLine: 'auto'`（避免 Windows CRLF 误报） |

---

## Step 3.1 · 升级 ESLint 生态

```bash
pnpm up eslint@~9 \
        eslint-plugin-vue@latest \
        @typescript-eslint/eslint-plugin@latest \
        @typescript-eslint/parser@latest \
        @vue/eslint-config-typescript@latest \
        @vue/eslint-config-prettier@latest \
        eslint-plugin-prettier@latest \
        prettier@latest
pnpm add -D globals
```

**沙盒实测输出**（关键片段）：

```
- eslint 8.57.1                          + eslint 9.39.4
- eslint-plugin-vue 9.33.0               + eslint-plugin-vue 10.9.2
- @typescript-eslint/eslint-plugin 6.x   + @typescript-eslint/eslint-plugin 8.60.1
- @typescript-eslint/parser 6.x          + @typescript-eslint/parser 8.60.1
- @vue/eslint-config-typescript 11.0.3   + @vue/eslint-config-typescript 14.8.0
- @vue/eslint-config-prettier 8.0.0      + @vue/eslint-config-prettier 10.2.0
```

---

## Step 3.2 · 删旧配置

```bash
rm -f .eslintrc.js .eslintignore
```

---

## Step 3.3 · 新建 `eslint.config.mjs`

完整内容（沙盒实测可用）：

```js
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'
import prettier from 'eslint-plugin-prettier/recommended'
import globals from 'globals'

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{js,mjs,cjs,ts,tsx,vue}']
  },
  {
    name: 'app/files-to-ignore',
    ignores: [
      'build/**',
      'src/assets/**',
      'public/**',
      'dist/**',
      '**/scripts/**'
    ]
  },
  {
    languageOptions: {
      ecmaVersion: 2020,
      globals: {
        ...globals.node,
        ...globals.browser
      }
    }
  },
  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  skipFormatting,
  prettier,
  {
    rules: {
      '@typescript-eslint/no-explicit-any': 'off',
      'prettier/prettier': ['error', { semi: false }],
      'no-console': 'warn',
      'no-debugger': 'warn'
    }
  }
)
```

**与旧 `.eslintrc.js` 的对应关系**：

| 旧 | 新 |
|---|---|
| `extends: ['plugin:vue/vue3-essential', ...]` | `pluginVue.configs['flat/essential']` + 其它 import |
| `parserOptions: { ecmaVersion: 2020 }` | `languageOptions: { ecmaVersion: 2020 }` |
| `env: { node: true }` | `languageOptions.globals: { ...globals.node }` |
| `rules: { ... }` | 末尾对象的 `rules: { ... }` |
| `.eslintignore` 文件 | `ignores: [...]` 对象 |
| `overrides: [{ files: 'tests/...', env: { jest: true } }]` | **删了**（Phase 2 已删 tests/）|

---

## Step 3.4 · `.prettierrc` 加 `endOfLine: 'auto'`

```json
{
  "semi": false,
  "singleQuote": true,
  "useTabs": false,
  "trailingComma": "none",
  "printWidth": 80,
  "arrowParens": "avoid",
  "endOfLine": "auto"
}
```

**为什么必加**：

沙盒实测，不加这一行 Windows 上跑 ESLint 会报 **13822 个 `Delete '␍'` 错误**，每行都中招。原因是 git 默认开 `core.autocrlf=true`，仓库 LF 文件 checkout 后变 CRLF，而 prettier 默认期望 LF。

加了 `endOfLine: 'auto'` 后只剩 30 个真实业务错误。

---

## Step 3.5 · 改 `package.json` 的 lint 脚本

```diff
- "lint": "eslint --ext .js,.vue .",
+ "lint": "eslint .",
```

ESLint 9 已删 `--ext` 选项，文件类型由 `eslint.config.mjs` 里的 `files: [...]` 控制。

---

## Step 3.6 · 验证

```bash
./node_modules/.bin/eslint . 2>&1 | tail -5
```

**期望**（沙盒实测）：

```
✖ 30 problems (22 errors, 8 warnings)
```

- 22 个 errors：业务代码 `@typescript-eslint/no-unused-vars`、`prettier/prettier`（v14 的 ts-config 比 v11 更严格）
- 8 个 warnings：`no-console`（开发遗留的 `console.log`）

**这些是真实历史问题**，不是迁移引入的。

| 判定 | 怎么办 |
|---|---|
| ≤ 50 个错误，集中在少数文件 | ✅ 通过，留到 Phase 4 `<script setup>` 迁移时一起清 |
| > 100 个或全是 prettier 报错 | ❌ `.prettierrc` 的 `endOfLine: auto` 可能没生效，回 Step 3.4 检查 |

---

## Step 3.7 · 顺手再跑一次 vite 验证 ESLint 升级没影响 build

```bash
./node_modules/.bin/vite build 2>&1 | tail -3
# 期望：✓ built in xxx ms
```

> ESLint 跟 Vite 的 build 链路是分开的——本不该相互影响，但跑一遍图个心安。

---

## Step 3.8 · commit + 子 tag

```bash
git add -A
git status   # 期望：删 2 个 + 新建 1 个 + 改 3 个

git commit -m "phase 3b.2: ESLint 8 -> 9 (flat config) + plugins major bump"

git tag phase-3b-2-done
git tag phase-3b-done    # 整个 Phase 3b 收口
```

---

→ [04-verify.md](./04-verify.md)
