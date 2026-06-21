# 04 · 出口验收

> 完成 01 / 02 / 03 三步后，跑这一份验收。**全绿才能 commit + 打 tag。**

---

## 验收 1：Hexo 残留检查（限定到配置层）

```bash
# 在工程根目录
grep -rni "hexo" \
  --include="*.yml" --include="*.yaml" \
  --include="*.config.js" --include="*.config.ts" \
  --include="package.json" --include="index.html" \
  --include="tsconfig.json" \
  --exclude-dir=node_modules --exclude-dir=dist \
  .
```

**期望输出**：

```
./package.json:4:  "description": "MyBlog V2 frontend (forked from auroral-ui/hexo-theme-aurora).",
```

只有这一行 fork 来源说明。其它命中 = 没删干净，回去查。

### ⚠️ src/ 的 Hexo 命名是预期内的

如果你扩大 grep 范围到 `src/**`，会看到：

| 文件 | 命中 |
|---|---|
| `src/api/index.ts` | `HexoConfig`、`fetchHexoConfig` |
| `src/stores/app.ts` | `hexoConfig` 字段、`HexoConfig` 类型 |
| `src/models/HexoConfig.class.ts` | 整个文件 |
| `src/models/ThemeConfig.class.ts` | 注释提到 hexo |
| `src/main.ts` | 注释链接 `auroral-ui/hexo-theme-aurora` |
| `public/api/*.json` | mock 文章里有"Hexo"字样的标题 |

**这些都是业务命名，不在 Phase 1 范围内**。处理时机：

- `HexoConfig` 类 / `fetchHexoConfig` → **Phase 7 V2 后端对接** 时连 adapter 一起改（重命名为 `SiteConfig` / `fetchSiteConfig`，对齐后端 `t_site_config`）
- mock 文章内容 → Phase 7 接真后端时整批替换，本地 mock 改不改无所谓
- `main.ts` 注释链接 → 顺手删，或 Phase 9 发布前打磨时一起清

不要在 Phase 1 动它们——会拖出一长串改动，违背"单一目的 phase"原则。

---

## 验收 2：Hexo 目录全部消失

```bash
ls _config.yml _config.aurora.yml templates layout source data build vue.config.js 2>/dev/null
```

**期望输出**：空（任何一项还在 = 01 步漏删）。

---

## 验收 3：dev 启动

```bash
./node_modules/.bin/vite
```

**期望**：

- 控制台输出 `VITE v4.x.x  ready in xxx ms`
- 给出 Local URL（默认 `http://localhost:5173/`）
- 浏览器打开能看到熟悉的首页（封面 + 文章列表）
- F12 Network：`/api/posts.json`、`/api/site.json` 等返回 **200**，来自 `public/api/`

如果起不来 → 99% 是 vite.config.js 还有残留 import → 看 [05-troubleshooting.md](./05-troubleshooting.md) 案例 A。

---

## 验收 4：production build

```bash
./node_modules/.bin/vite build
```

**期望**：

- 控制台输出 `✓ built in xxx ms`
- 根目录出现 `dist/` 目录（不是 `source/`——若出 `source/` 说明 vite.config.js 还有 `outDir: 'source'`）
- `dist/index.html` 内引用的 JS / CSS 路径都是 `/static/js/[hash].js` 之类

```bash
ls dist/
# 期望看到：index.html + static/
```

可选：起 preview 服务双检：

```bash
./node_modules/.bin/vite preview
# 访问输出的 URL，行为和 dev 一致即通过
```

---

## 收口

四项全绿 → commit + 打 tag：

```bash
git add -A
git commit -m "phase 1: decouple from hexo

- delete _config.yml / templates/ / layout/ / build/ / vue.config.js
- clean vite.config.js: remove createHtmlPlugin + filenamePath + outDir 'source' + proxy
- clean package.json: rebrand to myblog-frontend, remove env:* scripts / postbuild / repository / keywords

src/ business code naming (HexoConfig class, fetchHexoConfig) kept as-is;
will be addressed in Phase 7 V2 adapter."

git tag phase-1-done
```

---

## 下一步

→ [Phase 2 · 删死依赖](../phase-2-prune-deps/)（待撰写）。

阻塞情况：Phase 2 会移除 `vite-plugin-html-transformer` 等依赖包。已经在 Phase 1 把 vite.config 对它的 import 删掉了，所以 Phase 2 直接 `pnpm remove` 即可。
