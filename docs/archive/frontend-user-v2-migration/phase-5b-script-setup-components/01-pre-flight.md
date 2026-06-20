# 01 · Pre-flight 检查表

## 入口条件

✅ **Phase 5a 已完成**（pages 全部迁移到 `<script setup>`）

✅ **主分支无 pending 变更**

✅ **node_modules 已安装**

```bash
cd /path/to/My-Blog
npm install  # 或 pnpm install
```

---

## 基线 Tag（沙盒验证后标记）

```bash
# 创建 phase-5b 开始的 baseline tag
git tag -a v5b-start -m "Phase 5b 开始：components script setup 迁移前基线"
git push origin v5b-start

# 迁移完成后
git tag -a v5b-done -m "Phase 5b 完成：42 files 迁移 + TS error fix"
git push origin v5b-done
```

---

## 沙盒方式

在 `/path/to/verify-phase1` 新建沙盒，做所有改动：

```bash
cd /path/to/verify-phase1
git clone <repo> hexo-theme-aurora-main  # 或 cp -r 主项目
cd hexo-theme-aurora-main
git checkout -b phase-5b-test
npm install
```

---

## 验证工具配置

```bash
# 1. TypeScript 检查
npm run type-check  # or: ./node_modules/.bin/tsc --noEmit

# 2. Build 检查
npm run build

# 3. Dev server（测试页面渲染）
npm run dev
```

---

## 预期指标

| 指标 | 5a 完成时 | 5b 完成后 | 变化 |
|---|---|---|---|
| `defineComponent` count | 10 (pages) | 2 (LoadingSkeleton) | ✅ -8 |
| `setup()` count | 10 (pages) | 2 (LoadingSkeleton) | ✅ -8 |
| TS errors | 2 | 0 | ✅ 修复 |
| Bundle size | ~450 KB | ~448 KB | ✅ -0.5% |

---

## 注意事项

1. **LoadingSkeleton 跳过**：两个文件用 `render()` 函数，不兼容 script setup
   - `src/components/LoadingSkeleton/Skeleton.vue`
   - `src/components/LoadingSkeleton/SkeletonTheme.vue`

2. **TS error 修复**：在 index.ts registration 加 `as string` 类型断言（最后一步）

3. **props/emits 转写**：新特性，需逐个核对 TypeScript 签名

4. **inject/provide 链**：Dropdown 组件体系用到，转写时保留顺序

---

## 沙盒验证完成后

1. ✅ TS errors 归零
2. ✅ `npm run build` 通过
3. ✅ `npm run dev` 各页面打开正常
4. ✅ 检查: `grep -rn "defineComponent\|setup()" src/components/ | grep -v LoadingSkeleton` → 0 输出
5. ✅ 所有 commit 整理完毕

---

**下一步**：→ [02-migrate.md](./02-migrate.md) 12 个 commit 批次
