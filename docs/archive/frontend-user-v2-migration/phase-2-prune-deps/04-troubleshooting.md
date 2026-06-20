# 04 · Troubleshooting

> Phase 2 沙盒验证时可能遇到的坑 + 应对。

---

## A. dev 启动报 `Cannot find module 'vue-class-component'`（或其它已删的包）

**原因**：业务代码里还有 `import` 它，grep 时漏看了。

**修复**：

```bash
# 1. 精确定位
grep -rn "vue-class-component" --include="*.ts" --include="*.vue" \
  --exclude-dir=node_modules src/

# 2. 把那个包临时加回去
pnpm add vue-class-component

# 3. 重启 dev 验证
./node_modules/.bin/vite
```

> 然后**记录到 phase-2 文档"实际不能删的包"清单**，下次反向验证时跳过它。如果只是注释或文档里提到，则去掉注释后再 `pnpm remove`。

---

## B. `pnpm remove` 报 `lockfile is not up to date`

**原因**：手动改过 `package.json` 删了字段但没同步 lock。

**修复**：

```bash
# 不带 --frozen-lockfile，让 pnpm 自动修复
pnpm install

# 然后再 remove
pnpm remove <pkg>...
```

---

## C. `vite build` 突然报 `[plugin:vite:resolve] Failed to resolve import`

**原因**：同 A，业务代码引用了被删的包，**dev 因为懒加载没触发，build 才一次性全编译爆出来**。

**修复**：

```bash
# build 输出会告诉你具体是哪个文件 import 哪个包
# 例：src/main.ts:5:0: Failed to resolve import "vue-class-component"

# 把那个包加回来
pnpm add <pkg>
```

---

## D. 删完 `tests/` 后 lint 报 `no such file`

**原因**：`.eslintrc.cjs` 里可能有 `overrides` 段专门给 `tests/**` 配 rule。

**检查**：

```bash
grep -n "tests" .eslintrc.cjs 2>/dev/null
grep -n "tests" .eslintignore 2>/dev/null
```

**修复**：把指向 `tests/` 的 overrides / ignore 行删掉。这不影响 lint 行为（被指向的目录不存在了，规则就是空操作）。

---

## E. `pnpm-workspace.yaml` 自动生成

**症状**：`pnpm install` 或 `pnpm remove` 之后冒出一个 `pnpm-workspace.yaml`，内容像：

```yaml
allowBuilds:
  '@parcel/watcher': set this to true or false
  core-js: set this to true or false
  ...
```

**原因**：pnpm 11 严格 build-script 模式，希望你逐个声明哪些子包允许跑 install 脚本。

**Phase 2 处理**：直接删，不要 commit 进仓库：

```bash
rm -f pnpm-workspace.yaml
echo "pnpm-workspace.yaml" >> .gitignore     # 防它再次冒出来时被 add
```

> 彻底关掉这个机制留到 Phase 3：在 `package.json` 加 `"pnpm": { "onlyBuiltDependencies": [...] }`。

---

## F. 删完后 commit 触发 husky / commitlint 失败

**原因**：commit message 不符合 conventional commits。

**修复**：用规范格式：

```bash
git commit -m "phase 2: prune dead dependencies"
# 或 chore: 前缀
git commit -m "chore: prune dead dependencies"
```

应急绕过：`git commit --no-verify -m "..."`（不推荐常态用）。

---

## G. 想反悔，把某个包加回去

```bash
# 单包回滚
pnpm add <pkg>           # 加回 dependencies
pnpm add -D <pkg>        # 加回 devDependencies

# 整 phase 回滚
git reset --hard pre-prune-deps
pnpm install             # 让 node_modules 跟 lockfile 对齐
```

---

## 没列在这里的报错

逐项跑 [03-verify.md](./03-verify.md) 的 5 项验收，定位失败项后把报错复制出来再具体查。
