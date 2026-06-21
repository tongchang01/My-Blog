# 01 · Pre-flight

> 升级前快照 + 留 rollback tag。

---

## Step 1.1 · 确认 Phase 3a 已收口

```bash
git tag | grep phase-3a-done
# 期望：phase-3a-done

git status
# 期望：working tree clean
```

---

## Step 1.2 · 打 tag `pre-deps-major`

```bash
git tag pre-deps-major
```

> 出大问题可一键 `git reset --hard pre-deps-major` 回到 Phase 3a 末状态。

> 此外两个 step 各自再打 `phase-3b-1-done` / `phase-3b-2-done` 子 tag，便于只回退某一步。

---

## Step 1.3 · 当前版本快照

```bash
node -e "
const p = require('./package.json');
const keys = ['vite','vite-plugin-pages','vite-plugin-svg-icons','@vitejs/plugin-vue','eslint','eslint-plugin-vue','@typescript-eslint/eslint-plugin','@typescript-eslint/parser','@vue/eslint-config-typescript','@vue/eslint-config-prettier','eslint-plugin-prettier','prettier'];
keys.forEach(k => console.log(k.padEnd(36), p.dependencies?.[k] || p.devDependencies?.[k] || '(missing)'));
"
```

**沙盒实测（Phase 3a 末）**：

```
vite                                 ^4.4.9
vite-plugin-pages                    ^0.31.0
vite-plugin-svg-icons                ^2.0.1
@vitejs/plugin-vue                   ^5.2.4
eslint                               8
eslint-plugin-vue                    9
@typescript-eslint/eslint-plugin     ^6.5.0
@typescript-eslint/parser            ^6.5.0
@vue/eslint-config-typescript        ^11.0.3
@vue/eslint-config-prettier          ^8.0.0
eslint-plugin-prettier               ^5.0.0
prettier                             ^3.0.3
```

---

## Step 1.4 · 跑一遍 build 留基线

```bash
./node_modules/.bin/vite build 2>&1 | tail -5
```

记下 `built in xxx ms` + 主 bundle 体积，升级后对比。

---

→ [02-vite-5.md](./02-vite-5.md)
