# 03 · 出口验收

> 7 个 commit 全部完成后跑这 4 项。全绿才打 `phase-5a-done` tag。

---

## 验收 1 · `defineComponent` 在 pages 已清零

```bash
grep -rn "defineComponent" src/pages/
# 期望：0 输出
```

如有残留 → 漏改的文件回 [02-migrate.md](./02-migrate.md) 对应 commit 补改。

---

## 验收 2 · TS 检查仍只剩 2 个 pre-existing errors

```bash
./node_modules/.bin/tsc --noEmit 2>&1 | grep "error TS" | wc -l
# 期望：2
```

```bash
./node_modules/.bin/tsc --noEmit 2>&1 | grep "error TS"
# 期望：
# src/components/LoadingSkeleton/index.ts(6,17): error TS2345: ...
# src/components/LoadingSkeleton/index.ts(7,17): error TS2345: ...
```

| 看到 | 含义 |
|---|---|
| 2 errors（都在 LoadingSkeleton） | ✅ 正常，与 Phase 4 末持平 |
| > 2 errors | ❌ 5a 改坏了，错误位置必在 `src/pages/**` 内 |
| < 2 errors | 👌 极佳但不必须（说明顺手修了 pre-existing 问题，本 phase 不该发生，复核一下） |

---

## 验收 3 · Production build

```bash
./node_modules/.bin/vite build 2>&1 | tail -15
```

**期望**：

- `✓ built in xxx ms`
- 主 bundle 体积有小幅下降（沙盒：Phase 4 末 **452.92 KB** → Phase 5a 末 **450.92 KB**，**-2 KB**）

下降幅度不大是正常的——`<script setup>` 主要省的是 `defineComponent({ setup(){...} })` 那层包装的元数据，不是大块逻辑。Phase 5b（~40 个 components）才会看到更显著的减少。

**如果 build 失败**：

| 报错关键词 | 大概率根因 |
|---|---|
| `Cannot access 'X' before initialization` | 函数声明顺序问题（[04-troubleshooting.md case B](./04-troubleshooting.md)） |
| `X is not defined` in template | 顶层漏 export（其实是漏写 `const`）或者拼写 |
| `defineComponent is not a function` | 某文件改一半，import 删了但 `defineComponent({})` 还在 |

---

## 验收 4 · Dev server 冒烟（10 个页面）

```bash
./node_modules/.bin/vite
```

打开浏览器 `http://localhost:5173/`，**10 个路由都点一遍**：

| 路由 | 文件 | 检查点 |
|---|---|---|
| `/` | `index.vue` | 首页文章列表、分类卡片渲染 |
| `/about` | `about.vue` | About 页面 markdown 内容显示 |
| `/archives` | `archives.vue` | 归档时间线渲染、月份分组 |
| `/tags` | `tags.vue` | 标签云显示，数字为空时也不崩 |
| `/categories` | `category.vue` | 分类侧栏 |
| `/links` | `links.vue` | 友链卡片 + PostStats 区块 |
| `/page/友情链接`（任意 menu 项） | `page/[slug].vue` | 页面标题随 locale 切换更新 |
| `/post/<某篇 slug>` | `post/[slug].vue` | 文章正文、prev/next、评论区 |
| `/post/search?tag=xxx` | `post/search/index.vue` | 搜索结果列表 |
| `/__nonexistent__` | `[...all].vue` | 404 SVG 灯柱 |

**Console**：F12 无新增报错（旧的 mock 数据 warning 不算）。

**特别注意 `post/[slug].vue`**：这是唯一一个改了生命周期钩子的文件。重点验证：
- 进文章页能正常 fetch 内容（说明 `onMounted` 触发）
- 离开文章页 header 背景图恢复默认（说明 `onBeforeUnmount` 触发）

`Ctrl+C` 关掉。

---

## 全过 → tag

```bash
git log --oneline | head -8
# 期望：最上 7 行都是 chore(phase-5a) 开头

git tag phase-5a-done
git tag | grep phase-5
# 期望：phase-5a-done（pre-script-setup 也在更前面）
```

**沙盒实测**：tag `phase-5a-done` 落在 commit `472a56a`，7 个 commit 净 -167 行。

---

## 下一步

→ Phase 5b · `<script setup>` 迁移 components 目录（待撰写）

预告：~40 个 `.vue` 文件，分批改。届时会遇到 5a 没遇到的：
- `props: { ... }` → `defineProps<{ ... }>()`
- `emits: [...]` → `defineEmits<{ ... }>()`
- 大量未用 imports（5a 都留着没清，5b 集中清）
