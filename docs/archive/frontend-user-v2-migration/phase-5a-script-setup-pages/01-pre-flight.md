# 01 · Pre-flight

> 入口条件 + 基线 tag。

---

## 入口条件

- [x] Phase 4 已收口（tag `phase-4-done` 存在）
- [x] 工作目录干净（`git status` 全空）
- [x] 当前 main bundle 体积已知（Phase 4 末沙盒 = **452.92 KB**，用于对比 5a 末）
- [x] Vue ≥ 3.5（Phase 3a 已升到 3.5；`<script setup>` 自 3.2 起稳定支持）

---

## Step 1.1 · 确认前置

```bash
git tag | grep phase-4
# 期望：phase-4-done

git status
# 期望：nothing to commit, working tree clean

git log --oneline -1
# 期望：最上一行是 chore(phase-4): dead code cut ...

cat package.json | grep '"vue"'
# 期望：^3.5.x
```

---

## Step 1.2 · 打基线 tag

```bash
git tag pre-script-setup
git tag | grep -E "pre-script|phase-5a"
# 期望：pre-script-setup
```

---

## Step 1.3 · 清点 10 个目标文件

```bash
find src/pages -name "*.vue" | xargs wc -l | sort -n
```

**期望输出**（10 个文件 + 1 个 total 行）：

```
   38 src/pages/category.vue
   50 src/pages/about.vue
   76 src/pages/tags.vue
   88 src/pages/page/[slug].vue
  175 src/pages/links.vue
  192 src/pages/post/search/index.vue
  280 src/pages/index.vue
  289 src/pages/[...all].vue
  308 src/pages/post/[slug].vue
  375 src/pages/archives.vue
 1871 total
```

---

## Step 1.4 · 确认这 10 个文件还是 Options API 写法

```bash
grep -l "defineComponent" src/pages/**/*.vue src/pages/*.vue 2>/dev/null
```

**期望**：10 个文件全部列出（说明都还没迁）。

---

## Step 1.5 · 当前 TS 错误数基线

```bash
./node_modules/.bin/tsc --noEmit 2>&1 | grep "error TS" | wc -l
# 期望：2（pre-existing LoadingSkeleton errors）
```

5a 全程要保持 = 2，超过就是改坏了。

---

→ [02-migrate.md](./02-migrate.md)
