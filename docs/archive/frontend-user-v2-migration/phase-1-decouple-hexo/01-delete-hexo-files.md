# 01 · 删 Hexo 产物 / 模板 / 配置

> 本步只**删文件**。验证靠下一步的 dev 启动——这里不跑。
> 全部 `rm` 都是物理删除，不可逆，所以确认在 `pre-decouple-hexo` tag 之后再操作。

---

## 待删清单

按"在原始 clone 里**一定存在**"的判断（基于 `auroral-ui/hexo-theme-aurora` v2.5.3 验证）：

| # | 路径 | 原始角色 | 删除理由 |
|---|---|---|---|
| 1 | `_config.yml` | Hexo 站点配置（如果 clone 里有） | 工程不再是 Hexo 主题 |
| 2 | `_config.aurora.yml` | Hexo 主题级配置 | 同上 |
| 3 | `templates/` | 装着 `index.html` / `index_prod.html`，给 `vite-plugin-html-transformer` 注入 hexo 变量用 | Phase 0 已用根 `index.html` 取而代之 |
| 4 | `layout/` | Hexo EJS 模板目录 | 工程是 SPA，不渲染 EJS |
| 5 | `build/scripts/` 整目录 + `build/index.js` | 自研环境切换脚本（`config-script.js` 切 local/prod/pub） | 改用 Vite 原生 `.env.*`，Phase 0 已经验证 `.env` 足够 |
| 6 | `source/`（如果存在）| Hexo 产物输出目录约定 | 不再使用 Hexo 产物管线 |
| 7 | `data/`（如果存在）| Hexo data files 约定 | 同上 |
| 8 | `vue.config.js`（如果存在）| 残留 Vue CLI 配置 | 工程是 Vite，不用 Vue CLI |

> ⚠️ **不删** `_config.yml.example` / `README` / `LICENSE` / `CHANGELOG.md`——这些是仓库元信息。

---

## 执行

进入工程根目录后：

```bash
# 在 hexo-theme-aurora-main/（或你 fork 后的目录名）下
rm -f  _config.yml
rm -f  _config.aurora.yml
rm -rf templates/
rm -rf layout/
rm -rf build/
rm -rf source/
rm -rf data/
rm -f  vue.config.js
```

> `rm` 对不存在的文件不会报错（`-f` 静默忽略）。所以即使 clone 里有的没有，全部执行一遍也安全。

---

## 验证（仅看目录）

```bash
ls _config.yml _config.aurora.yml templates layout build source data vue.config.js 2>/dev/null
# 期望输出：空（全部不存在）
```

---

## ⚠️ 不删的边缘项

| 路径 | 看上去像 Hexo 却要保留 |
|---|---|
| `public/` | Vite 的静态资源目录，里面有 `favicon.ico` 和你 Phase 0 加的 `public/api/*.json` mock |
| `src/icons/` | SVG 雪碧图源，由 `vite-plugin-svg-icons` 使用 |
| `previews/` / `preivews/`（注意上游有这个拼写错的目录）| README 截图，删不删都行——保留为安全选择 |
| `tests/` | 空目录或仅含一两个 stub 文件；Phase 2 决定测试方向时再处理 |
| `commitlint.config.js` / `release.config.js` | 提交规范 + semantic-release 配置，Phase 2 决定是否保留 |
| `jest.config.js` | Jest 配置，Phase 2 决定测试方向时处理（删 jest 或换 vitest） |
| `.yo-rc.json` | Yeoman generator 记号，与 Hexo 无关 |

---

## 完成后

不要 commit，继续做 [02-clean-vite-config.md](./02-clean-vite-config.md)——下一步会改 `vite.config.js`，**必须配套**完成才能跑通 dev（删了 `templates/` 后，`vite.config.js` 里残留的 `createHtmlPlugin` 会去找不存在的模板而报错）。

把 01 + 02 + 03 三步做完，再统一 commit。
