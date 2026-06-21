# 01 · Pre-flight

> 入口条件确认 + 打基线 tag。

---

## 入口条件

- [x] Phase 3b 已收口（tag `phase-3b-done` 存在）
- [x] 工作目录干净（`git status` 全空）
- [x] 当前 main bundle 体积已知（Phase 3b 沙盒末 = 476 KB，便于对比下降幅度）

---

## Step 1.1 · 打基线 tag

```bash
git tag pre-dead-code-cut
git tag | grep -E "phase-3b|pre-dead"
# 期望：phase-3b-1-done / phase-3b-2-done / phase-3b-done / pre-dead-code-cut
```

---

## Step 1.2 · 确认 6 个目标文件存在

```bash
ls src/components/Dia.vue \
   src/components/Navigator.vue \
   src/components/Link/LinkBoxTitle.vue \
   src/stores/dia.ts \
   src/stores/navigator.ts \
   src/stores/routers.ts
ls src/utils/aurora-dia/ src/utils/comments/
```

**期望**：全部存在，每行无报错。

---

## Step 1.3 · 0 引用确认（防止误删）

```bash
# routers store 必须 0 外部引用
grep -r "useRoutersStore\|stores/routers\|from.*routers" src/ --include="*.ts" --include="*.vue"
# 期望：只有 src/stores/routers.ts 自己出现（即 1 行 = "import 自身定义所在"），其它 = 0

# LinkBoxTitle 必须 0 引用
grep -r "LinkBoxTitle" src/
# 期望：0

# aurora_bot 仅在 ThemeConfig.class.ts + Dia.vue 出现（Dia.vue 整个删，ThemeConfig 改）
grep -rn "aurora_bot" src/
# 期望：仅 2 文件
```

如有任何意外引用 → **停下来调查**，不要继续。

---

## Step 1.4 · ⚠️ navigator store 不能直接删

**沙盒踩坑结果**：`stores/navigator.ts` 表面上是 Navigator.vue 的 store，实际它同时存了 3 件事：

| state / action | 用途 | 消费者 |
|---|---|---|
| `openMenu` / `toggleMobileMenu()` / `isDone` | 移动端汉堡菜单 | **MobileMenu.vue**（活的） |
| `progress` / `updateProgress(p)` | 阅读进度条 | **Sticky.vue** + **Header.vue**（活的） |
| `openNavigator` / `toggleOpenNavigator()` / `setOpenNavigator()` | Navigator 弹窗开关 | **仅 Navigator.vue + MobileMenu.vue 一处调用**（死的） |

**结论**：navigator store **保留文件**，本 phase 只切 Navigator 弹窗相关的 3 个 state/action（详见 [02-cut.md Step 2.4](./02-cut.md)）。

确认命令：

```bash
grep -rn "navigatorStore\|useNavigatorStore" src/ --include="*.vue" --include="*.ts" | grep -v "stores/navigator.ts"
# 期望：3 文件 MobileMenu.vue / Sticky.vue / Header.vue（确认还有活的消费者）
```

---

## Step 1.5 · 当前 bundle 体积基线

```bash
./node_modules/.bin/vite build 2>&1 | grep "static/js/" | sort -k2 -h | tail -3
# 沙盒实测 Phase 3b 末：主 bundle 476 KB
# 记下你的数值，[03-verify.md] 末尾对比
```

---

→ [02-cut.md](./02-cut.md)
