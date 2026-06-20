# 02 · 升级

> 一条 `pnpm up` 搞定 7 个包。

---

## Step 2.1 · 执行升级

```bash
pnpm up \
  vue@~3.5 \
  vue-router@~4.5 \
  pinia@~2.3 \
  typescript@~5.6 \
  axios@~1.7 \
  @vitejs/plugin-vue@~5 \
  @types/node@latest
```

> **版本号说明**：
> - `~3.5` 表示升到 3.5.x 最新 patch（不跨 minor）。**故意**不用 `@latest` 避免顺带升到不在评估范围的版本（比如 Pinia 3、vue-router 5）。
> - `@vitejs/plugin-vue` 固定 `~5` —— **不要写 `@latest`！** v6 是 ESM-only，Vite 4 加载不了（详见 README 的"特别注意"）。
> - `@types/node` 跟着 Node LTS 走，可以 `@latest`。

**沙盒实测输出**（关键片段）：

```
dependencies:
- axios 1.x
+ axios 1.7.9
- pinia 2.1.6
+ pinia 2.3.1
- vue-router 4.x
+ vue-router 4.5.1

devDependencies:
- @types/node 20.x
+ @types/node 25.9.2
- @vitejs/plugin-vue 4.x
+ @vitejs/plugin-vue 5.2.4
- typescript 5.x
+ typescript 5.6.3

Packages: +38 -39
Done in xxs using pnpm vXX
```

> 看到 `Packages: +38 -39` 是正常的——新版包带来的子依赖与老版略有差异。**绝对值不重要**，重点是命令没报错退出。

---

## Step 2.2 · 升级后版本核对

```bash
node -e "
const p = require('./package.json');
const keys = ['vue','vue-router','pinia','typescript','axios','@vitejs/plugin-vue','@types/node'];
keys.forEach(k => console.log(k.padEnd(25), p.dependencies?.[k] || p.devDependencies?.[k] || '(missing)'));
"
```

**期望**：

```
vue                       ^3.5.xx
vue-router                ^4.5.x
pinia                     2.3.x
typescript                ^5.6.x
axios                     ^1.7.x
@vitejs/plugin-vue        ^5.2.x
@types/node               ^25.x.x（或更新）
```

任何一项跟期望偏差大，回 [04-troubleshooting.md](./04-troubleshooting.md) 查。

---

## Step 2.3 · 处理 pnpm-workspace.yaml（如果冒出来）

pnpm 11 严格 build-script 模式可能再次生成 `pnpm-workspace.yaml`（即使 Phase 1/2 已 gitignore 过，沙盒里也复现过）：

```bash
ls pnpm-workspace.yaml 2>/dev/null && rm -f pnpm-workspace.yaml
grep -q pnpm-workspace.yaml .gitignore || echo "pnpm-workspace.yaml" >> .gitignore
```

> 彻底关掉这个机制的方案留到将来某次 phase：在 `package.json` 加 `"pnpm": { "onlyBuiltDependencies": [...] }`。本 phase 不动。

---

→ [03-verify.md](./03-verify.md)
