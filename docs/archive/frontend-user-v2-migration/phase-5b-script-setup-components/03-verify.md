# 03 · 出口验收（4 项检查清单）

## 验收前准备

完成所有 12 个 commit + 1 个 TS error fix 后，在主分支（或本地沙盒）执行以下 4 项检查。

---

## 1️⃣ TypeScript 无错误

```bash
npm run type-check
# or
./node_modules/.bin/tsc --noEmit
```

**期望结果**：
```
0 errors
```

**检查清单**：
- ✅ LoadingSkeleton TS error 已修复（`as string` 类型断言）
- ✅ Props 类型推导正确（`defineProps<{}>()` 语法）
- ✅ Emits 类型推导正确（`defineEmits<{}>()` 语法）
- ✅ inject/provide 键值对齐（Dropdown → DropdownItem）
- ✅ 无多余的 `declare` 声明（除了 lodash `_: any` 和特定 CDN）

**常见遗漏**：
- [ ] 忘记 `as string` 类型断言在 LoadingSkeleton/index.ts
- [ ] Props 中可选字段用了 `required: true`（应该是 `foo?: Type`）
- [ ] defineEmits 签名不完整（缺少 `: void` 返回类型）

---

## 2️⃣ Build 通过

```bash
npm run build
```

**期望结果**：
```
✓ dist/ 生成成功
Bundle size: ~448 KB (从 5a 的 ~450 KB 缩小)
```

**检查清单**：
- ✅ 无 build 报错
- ✅ dist/ 文件夹完整生成
- ✅ 静态资源正常复制（CSS、SVG、字体等）
- ✅ 代码混淆/Tree-shaking 正常工作

**常见问题**：
- [ ] import 路径错误（vue component 相对路径）
- [ ] 模板中错误的 ref 绑定（`:ref` vs `ref`）
- [ ] 缺失 provide/inject 导致运行时错误

---

## 3️⃣ Dev 服务启动 + 页面渲染

```bash
npm run dev
```

**期望结果**：
```
➜  Local:   http://localhost:5173/
✓ 所有页面打开、导航、组件交互正常
```

**页面检查清单**（功能性验证）：

### 首页 / 分类 / 标签
- [ ] Header 导航正常渲染
- [ ] Sidebar 分类、标签、最新评论正常
- [ ] Footer 信息完整
- [ ] 主题切换（ThemeToggle）工作正常
- [ ] 搜索框打开/关闭动画正常
- [ ] MobileMenu 在移动端显示正常

### 文章详情页
- [ ] 面包屑正常显示
- [ ] 文章卡片列表加载正常
- [ ] 评论组件（Comment）初始化正常
- [ ] 底部分页（Paginator）正常
- [ ] 侧栏目录（Toc）展开/收起正常

### 功能验证
- [ ] 日/夜间主题切换无闪烁
- [ ] 搜索功能正常（输入关键词 → 显示结果）
- [ ] 分页导航正常（上一页/下一页）
- [ ] 链接页面打开正常（Link 组件链接列表）
- [ ] 评论插件加载成功（Waline/Gitalk/等）

**常见问题**：
- [ ] Header/Sidebar 组件因 provide/inject 失配导致显示为空
- [ ] SearchModal 搜索输入框无法聚焦（debounce 问题）
- [ ] Sticky 组件滚动粘性失效（window resize 事件未绑定）
- [ ] 组件 props 传值错误导致类型不匹配

---

## 4️⃣ defineComponent 计数确认

```bash
grep -rn "defineComponent\|setup()" src/components/ | grep -v LoadingSkeleton
```

**期望结果**：
```
(empty output)
```

**详细检查**：

```bash
# 应该返回 0（已全部迁移到 script setup）
grep -rn "defineComponent" src/components/ | grep -v LoadingSkeleton | wc -l

# 应该返回 2（仅 LoadingSkeleton 保留 defineComponent）
grep -rn "defineComponent" src/components/LoadingSkeleton/ | wc -l

# 应该返回 0（script setup 中不再有 setup() 函数）
grep -rn "setup()" src/components/ | grep -v LoadingSkeleton | wc -l
```

**预期指标**：

| 指标 | Phase 5a 后 | Phase 5b 后 | 验收标准 |
|---|---|---|---|
| `defineComponent` in components | 10 | 2 | ✅ 8 个已迁移 |
| `setup()` functions | 10 | 2 | ✅ 8 个已迁移 |
| TS errors | 2 | 0 | ✅ 全部修复 |
| Bundle size | ~450 KB | ~448 KB | ✅ 缩小 0.5% |
| 组件文件总数 | 42 | 42 | ✅ 无变化 |
| 净行数变化 | N/A | -442 lines | ✅ 代码简化 |

---

## 完成标志

全部 4 项通过 ✅ 时，出口验收完毕。

```
✅ TypeScript 无错误（tsc --noEmit）
✅ Build 成功（npm run build）
✅ Dev 服务运行 + 所有页面正常
✅ defineComponent 计数确认（仅剩 LoadingSkeleton 2 个）
```

---

## 验收失败排查

如果验收失败，参考 [04-troubleshooting.md](./04-troubleshooting.md) 常见坑与解决方案。

**关键联系**：
- TypeScript 错误 → 检查 Props/Emits 类型签名、inject 键名拼写
- Build 错误 → 检查导入路径、模板 ref 绑定
- 页面渲染问题 → 检查 provide/inject 链、组件 props 传值、事件 emit 调用

---

**下一步**：→ [04-troubleshooting.md](./04-troubleshooting.md) 常见坑排查
