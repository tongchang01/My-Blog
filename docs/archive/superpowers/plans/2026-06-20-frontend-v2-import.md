# 前台 V2 工程引入实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不修改桌面源目录的前提下，把前台工程及其历史文档引入当前仓库，并保留正确的 Git 作者归属。

**Architecture:** 在临时克隆中提交源工作区快照并精确重写 35 个个人提交身份，再以无 squash subtree 导入 `frontend/apps/blog/`。迁移过程文档单独复制到 archive；workspace 规范化留作后续独立任务，避免原始导入和工程调整混在一起。

**Tech Stack:** Git 2.45、git subtree、PowerShell、pnpm、Vue 3、TypeScript、Vite 5

---

### Task 1: 记录源目录不可变基线

**Files:**
- Read: `C:\Users\TYB\OneDrive\Desktop\前台\hexo-theme-aurora-main`
- Create: 临时校验输出，不进入仓库

- [ ] **Step 1: 记录源 HEAD、状态和排除生成物后的文件哈希**

运行 PowerShell，记录 `git rev-parse HEAD`、`git status --porcelain=v1`，并对排除 `.git`、`node_modules`、`dist` 后的文件计算 SHA-256 清单。

- [ ] **Step 2: 确认作者边界**

运行：

```powershell
git -C $source shortlog -sne pristine-clone..HEAD
```

预期：35 个提交，且只出现 `aid_dou <fj2580ij@aa.jp.fujitsu.com>`。

### Task 2: 在临时克隆中生成可导入历史

**Files:**
- Create: `%TEMP%\myblog-frontend-v2-import-*`
- Read: `C:\Users\TYB\OneDrive\Desktop\前台\hexo-theme-aurora-main`

- [ ] **Step 1: 从源仓库创建临时克隆**

使用 `git clone --no-hardlinks`，确保临时历史改写不会影响源 `.git` 对象。

- [ ] **Step 2: 复制当前工作区内容**

使用 `robocopy` 从源目录复制到临时克隆，排除 `.git`、`node_modules` 和 `dist`。随后确认临时克隆的 `git status --short` 与源状态一致。

- [ ] **Step 3: 提交未提交快照**

在临时克隆使用当前本机用户名和邮箱提交 5 个修改文件及 1 个新文件：

```powershell
git add --all
git commit -m "完善前台站点展示配置"
```

- [ ] **Step 4: 精确重写个人提交身份**

仅当 author 或 committer 邮箱等于 `fj2580ij@aa.jp.fujitsu.com` 时，改写为 `TONGYIBIN <Tong-yibin@outlook.com>`。不得改写其他邮箱。

- [ ] **Step 5: 验证历史**

验证 `pristine-clone..HEAD` 中原有 35 个提交和新增快照提交均为当前身份，并抽查 `pristine-clone` 及更早 Aurora 提交作者不变。

### Task 3: 导入前台历史

**Files:**
- Create: `frontend/apps/blog/**`

- [ ] **Step 1: 注册临时仓库 remote**

在目标仓库添加一次性 remote，指向临时克隆。

- [ ] **Step 2: 使用 subtree 无 squash 导入**

运行：

```powershell
git subtree add --prefix=frontend/apps/blog $temporaryRemote main
```

预期：生成一个 subtree 合并提交，当前树出现完整前台工程，源历史可从该合并提交追溯。

- [ ] **Step 3: 删除一次性 remote**

删除临时 remote，不在仓库配置中留下桌面或临时路径。

### Task 4: 归档迁移文档

**Files:**
- Create: `docs/archive/frontend-user-v2-migration/**`
- Read: `C:\Users\TYB\OneDrive\Desktop\前台\feontend-v2-docs/**`

- [ ] **Step 1: 原样复制文档**

复制全部文档和 mock-data，目标目录不保留源目录名中的 `feontend` 拼写错误。

- [ ] **Step 2: 验证文档文件清单和哈希**

对源文档和目标文档分别计算相对路径及 SHA-256，预期完全一致。

- [ ] **Step 3: 单独提交归档**

提交前运行 `git diff --stat` 和 `git status --short`，然后提交：

```powershell
git commit -m "归档前台V2迁移文档"
```

### Task 5: 验证导入结果和源目录不变

**Files:**
- Verify: `frontend/apps/blog/**`
- Verify: `docs/archive/frontend-user-v2-migration/**`

- [ ] **Step 1: 对比前台文件哈希**

排除 `.git`、`node_modules`、`dist` 后，对比源工程与 `frontend/apps/blog` 的相对路径和 SHA-256，预期完全一致。

- [ ] **Step 2: 安装依赖并执行前台质量门禁**

在 `frontend/apps/blog` 运行：

```powershell
pnpm install --frozen-lockfile
pnpm lint
pnpm exec vue-tsc --noEmit
pnpm build
```

预期：安装、lint、类型检查和构建均退出 0；如源工程自身门禁失败，只记录实际结果，不在导入提交中顺带修复。

- [ ] **Step 3: 复核源目录不可变**

重新采集源 HEAD、状态和文件哈希，与 Task 1 基线逐项比较，预期完全一致。

- [ ] **Step 4: 检查目标仓库状态**

运行 `git status --short`、`git log --graph --oneline --decorate -20` 和作者统计，确认无临时文件或 remote 残留。

