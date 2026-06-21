# 03 · 出口验收

> Step 2 改完后跑这 5 项。全绿才 commit + tag。

---

## 验收 1 · 文件已物理删除

```bash
ls src/components/Dia.vue \
   src/components/Navigator.vue \
   src/components/Link/LinkBoxTitle.vue \
   src/stores/dia.ts \
   src/stores/routers.ts 2>&1
ls src/utils/aurora-dia/ 2>&1
```

**期望**：每行 `No such file or directory`。

---

## 验收 2 · 残留引用为 0

```bash
grep -ri "aurora-dia\|<Dia\b\|useDiaStore\|aurora_bot" src/
# 期望：0 输出

grep -r "LinkBoxTitle" src/
# 期望：0

grep -r "useRoutersStore\|stores/routers" src/
# 期望：0

grep -rn "setOpenNavigator\|toggleOpenNavigator\|openNavigator" src/
# 期望：0（确认 Navigator 弹窗 state/action 没留尾巴）
```

如有任何残留 → 回 [02-cut.md](./02-cut.md) 对应 Step 重新检查。

---

## 验收 3 · TS 检查

```bash
./node_modules/.bin/tsc --noEmit 2>&1 | tail -10
```

**期望**：

```
src/components/LoadingSkeleton/index.ts(6,17): error TS2345: ...
src/components/LoadingSkeleton/index.ts(7,17): error TS2345: ...
```

只剩 **2 个 pre-existing errors**（LoadingSkeleton）。Phase 3b 末的 7 个错误里有 5 个是 `stores/routers.ts` 的，本 phase 删了它们消失了——这是顺手的副作用，不要意外。

| 看到 | 含义 |
|---|---|
| 0 errors | 极佳，但不必须 |
| 2 errors（LoadingSkeleton） | ✅ 正常 |
| > 2 errors | ❌ Step 2 改坏东西了，看错误位置定位 |

---

## 验收 4 · Production build

```bash
./node_modules/.bin/vite build 2>&1 | tail -8
```

**期望**：

- `✓ built in xxx ms`
- 主 bundle 体积有可见下降（沙盒：Phase 3b 末 476 KB → Phase 4 末 **452.92 KB**，**-23 KB / -4.8%**）

**如果 build 失败**：90% 概率是某个文件还在 import 已删掉的东西。Vite 5 错误信息很清楚，按 `Could not load X (imported by Y)` 提示去 Y 文件删 import。

常见漏改场景（沙盒踩过）：
- `MobileMenu.vue` 还在 `navigator.setOpenNavigator(false)` ← Step 2.5 漏改

---

## 验收 5 · Dev server 冒烟

```bash
./node_modules/.bin/vite
```

打开浏览器 `http://localhost:5173/`：

- 首页加载，文章卡片渲染
- F12 Console 无新增报错（注意：旧的 mock 数据相关 warning 不算）
- 看板娘 Dia 不再出现
- 移动端汉堡菜单仍能开合（resize 浏览器窗口 < 1024px 测试）
- 阅读进度条仍随滚动更新

`Ctrl+C` 关掉。

---

## 全过 → commit + tag

```bash
git add src/App.vue src/components/MobileMenu.vue src/models/ThemeConfig.class.ts src/stores/navigator.ts

git commit -m "chore(phase-4): dead code cut (Dia, Navigator, LinkBoxTitle, routers store)"

git tag phase-4-done

git log --oneline -3
# 期望：最上一行是 chore(phase-4)...

git tag | grep phase-4
# 期望：phase-4-done
```

**沙盒实测**：

```
[main 915fbcb] chore(phase-4): dead code cut (Dia, Navigator, LinkBoxTitle, routers store)
 13 files changed, 1768 deletions(-)
```

13 文件 / **1768 行净删**——这就是 Phase 5 `<script setup>` 迁移**少改的工作量**。

---

## 下一步

→ Phase 5 · `<script setup>` 迁移（待撰写）

预告：约 55 个 `.vue` 文件从 `defineComponent({ setup() })` 改写到 `<script setup>`。分 3 子步：

- 5a：`src/pages/**` 10 个文件
- 5b：`src/components/**` ~40 个文件（删完 Phase 4 死代码后）
- 5c：App shell（App.vue + Header + Footer + main.ts）~6 个文件

Phase 3b 残留的 ~22 个 lint errors + 8 warnings 在 Phase 5 顺手清。
