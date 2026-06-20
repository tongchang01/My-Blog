# 01 · Pre-flight

> 升级前的"留后路"。

---

## Step 1.1 · 确认 Phase 2 已收口

```bash
git tag | grep phase-2-done
# 期望：phase-2-done

git status
# 期望：working tree clean
```

如果有未提交改动，先回去把 Phase 2 收口。

---

## Step 1.2 · 打 tag `pre-deps-minor`

```bash
git tag pre-deps-minor
```

> 出问题时 `git reset --hard pre-deps-minor` 一键回到升级前。

---

## Step 1.3 · 记录当前版本（升级前快照）

```bash
node -e "
const p = require('./package.json');
const keys = ['vue','vue-router','pinia','typescript','axios','@vitejs/plugin-vue','@types/node'];
keys.forEach(k => console.log(k.padEnd(25), p.dependencies?.[k] || p.devDependencies?.[k] || '(missing)'));
"
```

**沙盒实测输出**：

```
vue                       ^3.3.4
vue-router                ^4.2.4
pinia                     2.1.6
typescript                ^5.1.0
axios                     ^1.5.0
@vitejs/plugin-vue        ^4.3.4
@types/node               ^20.5.7
```

> 如果你这边版本号已经超过我列的，说明你工程已经领先一步，跳过对应包即可（命令里去掉）。如果落后，是正常的，按本 phase 跑就行。

---

## Step 1.4 · 跑一遍 build 留基线

```bash
./node_modules/.bin/vite build 2>&1 | tail -5
```

记下：

- `built in xxx ms` 时间
- 主 bundle (`dist/static/js/xxxxxxx.js`) 体积

升级后再跑一次，对比看有没有意外膨胀 / 速度下降。

---

→ [02-upgrade.md](./02-upgrade.md)
