# Phase 1 · 脱钩 Hexo

> **目标**：把工程从「Hexo 主题」剥成「普通 Vite SPA」。
>
> **范围**：只删 / 改 Hexo 相关的 **配置 / 模板 / 产物 / 脚本**。
> **不动**：业务源码（`src/**`）、`node_modules` 依赖列表本身（依赖统一在 Phase 2 删）、`<script setup>` 写法迁移（Phase 4）。
>
> **预计耗时**：30–60 分钟（含验收）。
> **回退点**：进入前打 tag `pre-decouple-hexo`。

---

## 入口条件

- [x] Phase 0 已完成：`./node_modules/.bin/vite` 能跑通本地预览，首页加载 mock 数据
- [x] 工作目录干净（`git status` 无未提交改动）
- [x] 已打 tag `phase-0-done` 标记 Phase 0 收口

```bash
git tag pre-decouple-hexo            # 给 Phase 1 准备回退点
```

---

## 本 phase 文件清单

| # | 文档 | 内容 |
|---|---|---|
| 01 | [01-delete-hexo-files.md](./01-delete-hexo-files.md) | 删 Hexo 产物 / 模板 / 配置文件 |
| 02 | [02-clean-vite-config.md](./02-clean-vite-config.md) | 改 `vite.config.js`，移除 hexo 注入逻辑 |
| 03 | [03-clean-package-json.md](./03-clean-package-json.md) | 改 `package.json`，去掉 hexo 相关 metadata + 脚本 |
| 04 | [04-verify.md](./04-verify.md) | 出口验收：grep / dev / build 三件套 |
| 05 | [05-troubleshooting.md](./05-troubleshooting.md) | 常见坑（grep 误报、root index.html、proxy 残留） |

按 01 → 02 → 03 → 04 顺序执行。05 是参考，遇到具体报错再翻。

---

## 这个 phase 不做的事

明确写出来防止越界（这是单一目的 phase 的精髓）：

| ❌ 不做 | 留到哪个 phase |
|---|---|
| 删 `vite-plugin-html-transformer` 等依赖包本身 | Phase 2 删死依赖 |
| 删 `vue-class-component`、`esm`、`semantic-release` | Phase 2 |
| 升 Vite 4 → 5 / Vue 3.3 → 3.5 | Phase 3 依赖升级 |
| 把 `src/models/HexoConfig.class.ts` 改名 / 重构 | Phase 7 V2 后端对接（adapter 层处理） |
| 把 `src/api/index.ts` 的 `fetchHexoConfig` 重命名 | Phase 7 |
| 删 mock 数据里"Hexo"字样的文章标题 | Phase 7（接真后端时 mock 整批换） |
| 删评论插件 / Dia 机器人 / UV-PV 计数 | Phase 5 功能裁剪 |

→ 你在 Phase 1 验收时如果 grep 到 `src/` 下还有 `hexo` / `Hexo`，那是**预期内的**，下面 [04-verify.md](./04-verify.md) 会教你把 grep 缩到只看 config 文件。

---

## 出口验收（速查）

```bash
# 1. Hexo 残留只在 src/ 业务命名里（其它位置应当 0 命中）
grep -ri "hexo" --include="*.yml" --include="*.config.js" --include="package.json" --include="index.html" . | grep -v node_modules

# 2. 目录已清
ls templates/ layout/ source/ build/scripts/ 2>/dev/null

# 3. dev 启动
./node_modules/.bin/vite

# 4. build 产物
./node_modules/.bin/vite build
ls dist/
```

详细判断标准见 [04-verify.md](./04-verify.md)。

---

## 收口

```bash
git add -A
git commit -m "phase 1: decouple from hexo"
git tag phase-1-done
```

下一站：[Phase 2 · 删死依赖](../phase-2-prune-deps/)（暂未撰写，待 Phase 1 落地反馈后再开）。
