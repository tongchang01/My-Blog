# 04 · Troubleshooting

> Phase 3a 沙盒验证时实际踩到 + 容易踩到的坑。

---

## A. `vite build` 报 `"@vitejs/plugin-vue" resolved to an ESM file`

**症状**：

```
failed to load config from vite.config.js
error during build:
Error: Build failed with 1 error:
ERROR: [plugin: externalize-deps] "@vitejs/plugin-vue" resolved to an ESM file.
ESM file cannot be loaded by `require`.
```

**根因**：`@vitejs/plugin-vue` 升到了 v6.x。v6 是 ESM-only，要求 Vite 5+。Vite 4 加载 `vite.config.js` 时走 CJS 路径，加载不了 ESM 模块。

**沙盒实测**：第一次升级时我用了 `@vitejs/plugin-vue@latest`，被升到 6.0.7，build 直接挂。

**修复**：

```bash
pnpm add -D @vitejs/plugin-vue@~5
```

**验证**：再跑 `vite build`，应通过。

> 这是为什么 README 和 02-upgrade.md 都写"必须固定 `~5`，不要 `@latest`"。

---

## B. `npx tsc --noEmit` 报新文件的错（不是 LoadingSkeleton / routers.ts）

**判定**：

```bash
npx tsc --noEmit 2>&1 | grep -E "^[a-z]" | head
```

如果错误集中在 `LoadingSkeleton/index.ts` 和 `stores/routers.ts` → 历史问题，忽略，进 Phase 4 处理。

如果出现**别的**文件路径，且 Phase 2 末 tsc 时没报 → 真是升级导致的 breaking。

**典型修法**：

| 报错关键字 | 来源 | 修法 |
|---|---|---|
| `Cannot find type definition file for 'jest'` | tsconfig 还引用 jest | 回 Phase 2 [02-remove-packages.md](../phase-2-prune-deps/02-remove-packages.md) Step 2.3 补做 |
| `ComponentPublicInstance` 类型变化 | Vue 3.5 类型重写 | 改用 `InstanceType<typeof MyComp>` |
| `RouteRecordRaw` 字段名变化 | vue-router 4.5 微调 | 对照 [vue-router changelog](https://github.com/vuejs/router/blob/main/packages/router/CHANGELOG.md) |
| `defineStore` 推断变化 | pinia 2.3 类型推断改进 | 通常自动好转；少数手写类型的地方需调整 |

应急：单包回滚

```bash
pnpm add vue@3.3 vue-router@4.2  # 等
```

---

## C. `pnpm-workspace.yaml` 又冒出来

**现象**：升级后根目录多了 `pnpm-workspace.yaml`，内容像：

```yaml
allowBuilds:
  vue-demi: set this to true or false
```

**原因**：pnpm 11 严格 build-script 模式。

**修复**：参考 [02-upgrade.md Step 2.3](./02-upgrade.md)。删掉 + gitignore。

---

## D. `pnpm up` 报 `peer dependency` warning 一大堆

**示例**：

```
[WARN] Issues with peer dependencies found. Run "pnpm peers check" to list them.
```

**判定**：先**忽略**，跑完验证（dev / build / tsc）。如果三项都过，这些 warning 99% 是历史包对 Vue 3.5 / TS 5.6 的 peer range 还没更新，但实际能用。

**严重例外**：dev 或 build 真挂了，再去 `pnpm peers check` 看清单决定要不要锁某个版本。

---

## E. `pnpm up` 报 `ERR_PNPM_IGNORED_BUILDS`

**示例**：

```
[ERR_PNPM_IGNORED_BUILDS] Ignored build scripts: vue-demi@0.12.5, vue-demi@0.14.10
Run "pnpm approve-builds" to pick which dependencies should be allowed to run scripts.
```

**判定**：这只是**警告**（虽然标了 ERR），不影响 install 结果。

**修法**：本 phase 直接忽略，留到将来在 `package.json` 加：

```jsonc
{
  "pnpm": {
    "onlyBuiltDependencies": ["esbuild", "vue-demi"]
  }
}
```

---

## F. dev 报 `Failed to fetch dynamically imported module`

**原因**：浏览器缓存了旧版 chunk。升级后 chunk hash 变了，旧 tab 还指向旧文件。

**修复**：硬刷新（Ctrl+Shift+R）或关掉旧 tab 重开。

---

## G. 想反悔整个 Phase 3a

```bash
git reset --hard pre-deps-minor
pnpm install   # 让 node_modules 跟旧 lockfile 对齐
```

---

## 没列在这里的报错

逐项跑 [03-verify.md](./03-verify.md) 的 4 项验收，定位失败项后把报错完整复制出来再具体查。
