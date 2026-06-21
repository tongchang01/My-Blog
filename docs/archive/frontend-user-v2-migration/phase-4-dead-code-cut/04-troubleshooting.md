# 04 · Troubleshooting

> 沙盒实际踩到的 2 个坑。

---

## A. `vite build` 报 `Could not load src/stores/navigator`

**症状**（Step 2.1 把 `src/stores/navigator.ts` 也删掉之后）：

```
[vite:load-fallback] Could not load .../src/stores/navigator
  (imported by src/components/MobileMenu.vue?vue&type=script&lang.ts):
  ENOENT: no such file or directory
```

**根因**：本来盘点写"Navigator + stores/navigator 一起删"，但实际 `stores/navigator` 是个**三合一存储**（Navigator 弹窗 + 移动菜单 + 阅读进度），后两半还在用。

**确认命令**：

```bash
grep -rn "useNavigatorStore" src/ --include="*.vue" --include="*.ts" | grep -v "stores/navigator.ts"
# 沙盒：3 个活跃消费者
#   src/components/MobileMenu.vue
#   src/components/Sticky.vue
#   src/components/Header/src/Header.vue
```

**修复**：把 `stores/navigator.ts` 改回去（只删 Navigator 弹窗相关的 3 个 state/action），见 [02-cut.md Step 2.4](./02-cut.md)。MobileMenu.vue 也跟着改一行（[Step 2.5](./02-cut.md)）。

**预防**：删 store / 公共 hook 前永远先跑：

```bash
grep -rn "use<StoreName>Store" src/ --include="*.vue" --include="*.ts"
```

确认所有调用方都"愿意"被删，再动手。

---

## B. `git commit` 报 `subject may not be empty / type may not be empty`

**症状**：

```bash
$ git commit -m "phase 4: dead code cut ..."
⧗   input: phase 4: dead code cut ...
✖   subject may not be empty [subject-empty]
✖   type may not be empty [type-empty]
✖   found 2 problems, 0 warnings
husky - commit-msg hook exited with code 1
```

**根因**：本仓库 `commitlint.config.js` 启用了 `@commitlint/config-conventional`，要求 commit subject 必须是 conventional 格式：

```
<type>(<scope>)?: <description>
```

合法 `type` 仅这些：`feat` / `fix` / `chore` / `refactor` / `docs` / `style` / `test` / `perf` / `build` / `ci` / `revert`。**`phase` 不在其中**——commitlint 解析失败，整个 header 被判为"既无 type 也无 subject"。

**为什么之前 Phase 0-3b 的 `phase 1:` / `phase 3a:` 都过了**：husky hook 在 Phase 3a 之后才生效（pnpm install 时 `prepare` script 重装）。Phase 4 是第一个被 hook 拦住的。

**修复 1**：用 `chore(phase-N)` 作为类型 + scope：

```bash
git commit -m "chore(phase-4): dead code cut (Dia, Navigator, LinkBoxTitle, routers store)"
```

**修复 2**：header 长度 ≤ 100 字。沙盒第一版写法是：

```
chore(phase-4): dead code cut - Dia, Navigator widget, LinkBoxTitle, stores/routers, aurora_bot config
```

102 字，超 2 字也会被拦。**简化用括号 + 短词**。

**预防**：Phase 5 起所有 commit 用 `chore(phase-N)` / `chore(phase-Nx)` 格式（如 `chore(phase-5a)`、`chore(phase-7g)`）。

---

## C. 没列在这里的报错

逐项跑 [03-verify.md](./03-verify.md) 的 5 项验收，定位失败项后把报错完整复制再具体查。

---

## 想回退

**只回到 Phase 4 起点**：

```bash
git reset --hard pre-dead-code-cut
```

**整个 Phase 4 都不要了**（罕见——通常 Phase 4 是 Phase 5 的前置）：

```bash
git tag -d phase-4-done
git reset --hard pre-dead-code-cut
```
