# 03 · 出口验收

> 完成 01 / 02 两步后跑这一份。全绿才能 commit + 打 tag。

---

## 验收 1 · 9 个包从 package.json 里消失

```bash
node -e "
const p = require('./package.json');
const dead = [
  'vite-plugin-html-transformer', 'script-ext-html-webpack-plugin',
  'esm', 'semantic-release', 'vue-class-component',
  'vue-jest', '@vue/test-utils', '@types/jest', 'runjs'
];
let bad = 0;
dead.forEach(k => {
  if (p.dependencies?.[k] || p.devDependencies?.[k]) {
    console.log('❌ STILL THERE:', k); bad++;
  } else {
    console.log('✅ gone:', k);
  }
});
process.exit(bad);
"
```

**期望**：9 行 ✅，无 ❌，exit code 0。

---

## 验收 2 · 3 个配套文件 / 目录消失

```bash
ls release.config.js jest.config.js tests/ 2>/dev/null
# 期望：空输出
```

---

## 验收 2.5 · `tsconfig.json` 不再引用已删的东西

```bash
grep -nE '"jest"|tests/\*\*' tsconfig.json
# 期望：无输出
```

> 如果还匹配到，回 [02-remove-packages.md](./02-remove-packages.md) Step 2.3 补做。

---

## 验收 3 · `node_modules/` 真的瘦了

```bash
# 简化检查：看 node_modules/.pnpm/ 下顶级目录数（pnpm 隔离布局）
ls node_modules/.pnpm/ | wc -l
# Phase 1 末沙盒：~700+；Phase 2 末：~220 左右
```

> 数字会随宿主机 pnpm 版本 / OS 略有偏差，**绝对值不重要**，关键是看 `pnpm remove` 报了 `Packages: -488`（或差不多的负数）。

---

## 验收 4 · dev 启动 OK

```bash
./node_modules/.bin/vite
```

**期望**：

- `VITE v4.x.x  ready in xxx ms`
- 浏览器开 `http://localhost:5173/`，首页和 Phase 1 末**视觉完全一致**
- F12 Network：mock `/api/*.json` 全 200
- F12 Console：**无 `Cannot find module 'xxx'` 报错**

如果报 `Cannot find module 'vue-class-component'` / 类似 → 业务代码居然真在用某个被你删的包。`Ctrl+C` 关掉 vite，参考 [04-troubleshooting.md](./04-troubleshooting.md) A 节回滚单个包。

---

## 验收 5 · production build OK

```bash
./node_modules/.bin/vite build
```

**期望**：

- `✓ built in xxx ms`
- `dist/` 出来，主 bundle (`dist/static/js/xxxxx.js`) 大小和 Phase 1 末**几乎相同**（差几 KB 是正常波动）
- **不应该**报 `[plugin:vite:resolve] Failed to resolve import`

> Bundle 体积没显著下降是**正常的**——Phase 2 删的都是没被业务代码引用的包，本就不在 bundle 里。Phase 2 的收益在工程清爽度，不在产物体积。

---

## 收口

5 项全绿 → commit + tag：

```bash
git add -A
git status

git commit -m "phase 2: prune 9 dead deps + 3 orphan files

removed packages: vite-plugin-html-transformer, script-ext-html-webpack-plugin,
esm, semantic-release, vue-class-component, vue-jest, @vue/test-utils,
@types/jest, runjs (-488 transitive deps)

removed files: release.config.js, jest.config.js, tests/"

git tag phase-2-done
```

---

## 下一步

→ [Phase 3 · 依赖升级](../phase-3-upgrade-deps/)（待撰写）

预告：Phase 3 会把 Vue 3.3→3.5、Vite 4→5、Pinia 2.1→2.2、TS 5.1→5.5、vue-router 4.2→4.4 这一批升级。Phase 2 的清理让 Phase 3 升级时少 9 条版本兼容噪音。
