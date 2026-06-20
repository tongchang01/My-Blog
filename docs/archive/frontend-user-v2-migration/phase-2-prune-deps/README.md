# Phase 2 · 删死依赖

> **目标**：把 `package.json` 里**确认无引用**的依赖卸掉，配套删 3 个无主的 config 文件 + 1 个 stub 测试目录。
>
> **范围**：只删确认零业务引用的包；**不升级版本**（Phase 3）、**不改业务代码**。
>
> **预计耗时**：15–30 分钟（含验收）。
> **回退点**：进入前打 tag `pre-prune-deps`。

---

## 入口条件

- [x] Phase 1 已完成（`phase-1-done` tag 存在；vite dev/build 能跑）
- [x] 工作目录干净（`git status` 无未提交改动）

```bash
git tag pre-prune-deps            # 给 Phase 2 准备回退点
```

---

## 本 phase 文件清单

| # | 文档 | 内容 |
|---|---|---|
| 01 | [01-confirm-dead-deps.md](./01-confirm-dead-deps.md) | grep 验证 9 个候选包零业务引用 |
| 02 | [02-remove-packages.md](./02-remove-packages.md) | `pnpm remove` + 删配套 config / tests |
| 03 | [03-verify.md](./03-verify.md) | 出口验收：grep / dev / build |
| 04 | [04-troubleshooting.md](./04-troubleshooting.md) | 常见坑（误删依赖、build 报 missing） |

按 01 → 02 → 03 顺序执行。

---

## 删除清单（沙盒已验证）

### 9 个 npm 包

| 包 | 字段 | 删因 |
|---|---|---|
| `vite-plugin-html-transformer` | devDeps | Phase 1 已删 vite.config 引用 |
| `script-ext-html-webpack-plugin` | devDeps | Webpack 时代遗物，工程是 Vite |
| `esm` | devDeps | 老 CJS 时代产物；唯一 grep 命中是 `vue-easy-lightbox.esm.min.js` 文件名误报 |
| `semantic-release` | devDeps | 你不发 npm 包 |
| `vue-class-component` | deps | 源码 0 引用 |
| `vue-jest` | devDeps | 配 `jest.config.js`，但 `package.json` 无 `test` script，跑不起来 |
| `@vue/test-utils` | devDeps | 仅 `tests/unit/components/` 下 2 个 stub spec 用 |
| `@types/jest` | devDeps | jest 一起删 |
| `runjs` | devDeps | 服务于 `build/scripts/`，Phase 1 已删该目录 |

### 3 个配套文件 / 目录

| 路径 | 删因 |
|---|---|
| `release.config.js` | semantic-release 用 |
| `jest.config.js` | jest 用 |
| `tests/` 整目录 | 仅 2 个 stub spec，未被任何 npm script 调用 |

---

## 这个 phase 不做的事

| ❌ 不做 | 留到哪个 phase |
|---|---|
| 升级 Vue 3.3 → 3.5、Vite 4 → 5、Pinia 2.1 → 2.2 等 | Phase 3 依赖升级 |
| 删 `vue-i18n`（重构成 zh/ja/en） | Phase 6 i18n 改造 |
| 删 `vue3-click-away`、`vue3-lazyload`（评估能否原生替代） | Phase 5 功能裁剪 |
| 装 Vitest（取代删掉的 jest 测试栈） | Phase 4 之后视情况 |
| 删 `husky` / `commitlint`（工具链审视） | Phase 9 发布前打磨；现在保留 |

---

## 出口验收（速查）

```bash
# 1. 9 个包从 package.json 里消失
node -e "const p=require('./package.json');['vite-plugin-html-transformer','script-ext-html-webpack-plugin','esm','semantic-release','vue-class-component','vue-jest','@vue/test-utils','@types/jest','runjs'].forEach(k=>{if(p.dependencies?.[k]||p.devDependencies?.[k])console.log('STILL THERE:',k);else console.log('OK gone:',k)})"

# 2. 3 个配套文件 / 目录消失
ls release.config.js jest.config.js tests/ 2>/dev/null

# 3. dev/build 仍 OK
./node_modules/.bin/vite                # 期望首页 200
./node_modules/.bin/vite build          # 期望 dist/ 产物
```

详细见 [03-verify.md](./03-verify.md)。

---

## 收口

```bash
git add -A
git commit -m "phase 2: prune dead dependencies"
git tag phase-2-done
```

**沙盒实测数据**（参考）：

| 维度 | Phase 1 末 | Phase 2 末 |
|---|---|---|
| `dependencies` 条数 | 11 | 10 |
| `devDependencies` 条数 | 30 | 22 |
| 传递依赖减少 | — | **-488 个**（pnpm 报告） |
| `dist/` 主 bundle | ~489 kB | ~489 kB（业务代码不变，体积不变）|

> ⚠️ Phase 2 删的都是**未被业务代码引用**的包，所以 bundle 体积**不会**有明显下降——主要收益是：**install 更快**、`package.json` 更清爽、Phase 3 升级时少 9 条噪音、Phase 4 迁移 `<script setup>` 时少一份混乱来源。

下一站：[Phase 3 · 依赖升级](../phase-3-upgrade-deps/)（待撰写）。
