# 03 · 出口验收

> 4 项全绿才能 commit + 打 tag。

---

## 验收 1 · 7 个包的版本到位

```bash
node -e "
const p = require('./package.json');
const targets = {
  'vue': '3.5', 'vue-router': '4.5', 'pinia': '2.3',
  'typescript': '5.6', 'axios': '1.7',
  '@vitejs/plugin-vue': '5.', '@types/node': '2'
};
let bad = 0;
for (const [k, prefix] of Object.entries(targets)) {
  const v = (p.dependencies?.[k] || p.devDependencies?.[k] || '').replace(/^[\^~]/, '');
  const ok = v.startsWith(prefix);
  console.log(ok ? '✅' : '❌', k.padEnd(22), v, ok ? '' : '(expected '+prefix+'.x)');
  if (!ok) bad++;
}
process.exit(bad);
"
```

**期望**：7 行 ✅，无 ❌，exit 0。

> `@types/node` 只校验首位 `2`（25.x 或更新都 OK）。

---

## 验收 2 · dev 启动 OK

```bash
./node_modules/.bin/vite
```

**期望**：

- `VITE v4.5.x  ready in xxx ms`（Vite 还是 4，下个 phase 才升）
- 浏览器 `http://localhost:5173/` 首页和 Phase 2 末视觉一致
- F12 Console **无报错**（Vue 警告除外，那是组件层面的）
- F12 Network mock `/api/*.json` 全 200

`Ctrl+C` 关掉再下一步。

---

## 验收 3 · production build OK

```bash
./node_modules/.bin/vite build
```

**期望**：

- `✓ built in xxx ms`
- `dist/` 产物结构与 Phase 2 末几乎一致
- 主 bundle 体积与 Phase 2 末**±10% 内**（Vue 3.5 响应式重写后稍微更小是正常的）
- **不应该**报 `"@vitejs/plugin-vue" resolved to an ESM file` —— 如果报了，说明 plugin-vue 被误升到 v6，参考 [04-troubleshooting.md](./04-troubleshooting.md) A 节回滚

---

## 验收 4 · TypeScript 检查（容忍历史错误）

```bash
npx tsc --noEmit 2>&1 | head -20
```

**预期**：可能仍有 7 处左右历史 TS 错误（沙盒实测）：

- `src/components/LoadingSkeleton/index.ts` 2 处 `'string | undefined' is not assignable to parameter 'string'`
- `src/stores/routers.ts` 5 处（`routersMap` 默认导出 / `any` 推断）

这些**不是 Phase 3a 引入的**，是工程历史问题，在 TS 5.1 下也存在，只是 vite build 不做严格 type check 所以从来没暴露。

**判定**：

- ✅ 只看到上述 7 个左右、且都集中在 `LoadingSkeleton/` 或 `stores/routers.ts` → 通过
- ❌ 新增了别的文件的报错 → 真有 breaking，回 [04-troubleshooting.md](./04-troubleshooting.md) B 节

> 这些历史错误在 **Phase 4 `<script setup>` 迁移** 时顺手清掉，本 phase 不动。

---

## 收口

4 项判定全过 → commit + tag：

```bash
git add -A
git status   # 期望只有 package.json、pnpm-lock.yaml、.gitignore（如果改过）

git commit -m "phase 3a: minor bump (Vue 3.5, router 4.5, pinia 2.3, TS 5.6, axios 1.7, plugin-vue 5, types/node latest)"

git tag phase-3a-done
```

---

## 下一步

→ [Phase 3b · 跨 major 升级](../phase-3b-major-bump/)

Phase 3b 会做：

- Vite 4 → 5（plugin-vue 顺便升到 6）
- ESLint 8 → 9（flat config 迁移）
- 这两个 step 独立 commit，便于单独回退
