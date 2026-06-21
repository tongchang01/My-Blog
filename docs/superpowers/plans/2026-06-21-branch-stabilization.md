# V2 分支稳定化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不改写现有远端分支、不删除 V1 的前提下，形成后端候选基线和只包含前端增量的干净前端替代分支。

**Architecture:** 保留现有三条分支作为不可变审计来源，新建两个 `codex/` 候选分支。后端候选分支以 `backend-v2-refactor` 为基础吸收浏览器 ID 契约；前端候选分支以该后端候选为基础，按文件所有权重放前端最终状态，并用路径审计和完整测试证明边界正确。

**Tech Stack:** Git linked worktree、PowerShell、Maven、pnpm、Vitest、Vue TypeScript。

---

## 目标引用

- 保留：`backend-v2-refactor`
- 保留：`frontend-v2-integration`
- 保留：`master`
- 新建：`codex/backend-v2-integration-ready`
- 新建：`codex/frontend-v2-clean`
- 记录：`docs/project-handbook/plans/branch-stabilization-manifest.md`

### Task 1：冻结当前引用和提交清单

**Files:**
- Create: `docs/project-handbook/plans/branch-stabilization-manifest.md`

- [ ] **Step 1: 确认工作区和远端状态**

Run:

```powershell
git status --short
git fetch --prune origin
git branch -vv
git worktree list --porcelain
```

Expected: 当前工作区干净；三个现有分支和 worktree 路径可识别。若 fetch 后远端分支前进，停止并重新计算后续 SHA。

- [ ] **Step 2: 记录不可变 SHA**

Run:

```powershell
git rev-parse master
git rev-parse backend-v2-refactor
git rev-parse frontend-v2-integration
git merge-base backend-v2-refactor frontend-v2-integration
```

Expected: merge-base 为后端当前基线；将四个 SHA 写入 manifest，不使用 `latest` 等可漂移描述。

- [ ] **Step 3: 写提交所有权清单**

在 manifest 中列出：

```text
backend-contract:
  5a5fc43 修复后台账号ID前端精度契约
  5003f3d 修复后台文章ID前端精度契约
  3d7bf48 修复后台分类标签ID前端精度契约

frontend-final-state:
  frontend/**
  docs/archive/frontend-user-v2-migration/**
  frontend/apps/admin/docs/**
```

对 `docs/project-handbook/**` 和 `docs/superpowers/**` 逐项列出归属，不允许用整个 `docs/` 覆盖后端候选。

- [ ] **Step 4: 检查并提交 manifest**

Run: `git diff --stat; git status --short; git diff --check`

Commit: `记录V2分支稳定化基线`

### Task 2：建立后端集成候选分支

**Files:**
- Modify only when cherry-pick requires conflict resolution: `MyBlog-springboot-v2/**`
- Modify only when contract ownership requires it: `docs/project-handbook/api-contract/**`

- [ ] **Step 1: 创建隔离 worktree**

从 `backend-v2-refactor` 创建 `codex/backend-v2-integration-ready`，不得在当前前端 worktree 内切换分支。

Run:

```powershell
git worktree add E:\My-Blog\.worktrees\backend-v2-integration-ready -b codex/backend-v2-integration-ready backend-v2-refactor
```

Expected: 新 worktree HEAD 等于 Task 1 记录的 backend SHA。

- [ ] **Step 2: 逐个迁入后端契约提交**

Run:

```powershell
git cherry-pick 5a5fc43
git cherry-pick 5003f3d
git cherry-pick 3d7bf48
```

每次 cherry-pick 后运行 `git show --stat --oneline HEAD`；提交只能修改后端 Web DTO/mapping/tests 或直接对应的接口契约。

- [ ] **Step 3: 验证后端候选**

Run:

```powershell
cd E:\My-Blog\.worktrees\backend-v2-integration-ready\MyBlog-springboot-v2
mvn clean test
```

Expected: 637 tests，0 failures，0 errors，既有 Docker 条件测试允许 skipped。

- [ ] **Step 4: 审计候选差异**

Run:

```powershell
git diff --name-only backend-v2-refactor...codex/backend-v2-integration-ready
```

Expected: 不出现 `frontend/`、V1 目录或前端工程文档。

### Task 3：建立干净前端候选分支

**Files:**
- Create from source branch final state: `frontend/**`
- Create/Modify from manifest whitelist: frontend-specific docs only

- [ ] **Step 1: 从后端候选创建前端 worktree**

Run:

```powershell
git worktree add E:\My-Blog\.worktrees\frontend-v2-clean -b codex/frontend-v2-clean codex/backend-v2-integration-ready
```

- [ ] **Step 2: 按最终状态迁入前端目录**

在新 worktree 中运行：

```powershell
git restore --source=frontend-v2-integration --staged --worktree -- frontend
git restore --source=frontend-v2-integration --staged --worktree -- docs/archive/frontend-user-v2-migration
```

然后只按 manifest 白名单迁入前端规格、计划、状态和 API 契约文档；不得整体 restore `docs/`。

- [ ] **Step 3: 分阶段形成中文提交**

按文件组提交，提交前均运行 `git diff --stat` 与 `git status --short`：

```text
迁入博客前台V2工程
迁入博客后台V2工程
同步前端工程规格与验收文档
```

如果单个提交包含数百文件，manifest 必须说明这是固定上游快照或完整工程迁移，不能继续混入其他目的。

- [ ] **Step 4: 验证不存在后端重复增量**

Run:

```powershell
git diff --name-only codex/backend-v2-integration-ready...codex/frontend-v2-clean | rg "^MyBlog-springboot-v2/"
```

Expected: 无输出。

### Task 4：验证前台和后台候选

**Files:** none

- [ ] **Step 1: 验证前台**

Run:

```powershell
cd E:\My-Blog\.worktrees\frontend-v2-clean\frontend\apps\blog
corepack pnpm install --frozen-lockfile
corepack pnpm test
corepack pnpm typecheck
corepack pnpm build
```

Expected: 所有命令 exit 0。

- [ ] **Step 2: 验证后台**

Run:

```powershell
cd E:\My-Blog\.worktrees\frontend-v2-clean\frontend\apps\admin
corepack pnpm install --frozen-lockfile
corepack pnpm test
corepack pnpm typecheck
corepack pnpm build
```

Expected: 43 tests、typecheck 和生产构建通过。

- [ ] **Step 3: 对比当前前端最终状态**

Run:

```powershell
git diff --name-status frontend-v2-integration codex/frontend-v2-clean -- frontend
```

Expected: 无输出，两个分支中受版本控制的 `frontend/` 最终状态完全一致。

### Task 5：形成集成报告并停止在外部写操作前

**Files:**
- Modify: `docs/project-handbook/plans/branch-stabilization-manifest.md`

- [ ] **Step 1: 记录新旧提交映射和验证结果**

写入候选分支 SHA、目录边界、测试结果和仍保留的旧分支。

- [ ] **Step 2: 最终审计**

Run:

```powershell
git status --short
git log --oneline --decorate -10 codex/backend-v2-integration-ready
git log --oneline --decorate -10 codex/frontend-v2-clean
```

- [ ] **Step 3: 请求集成选择**

停止执行并让用户选择：本地合并、推送并创建 PR、保留候选分支或丢弃。未经选择不得更新 `master`、推送新分支或删除旧分支/worktree。

## 自审结果

- 现有远端分支不重写，旧前端分支保留为回滚点。
- 后端契约先归位，前端候选再以其为基础，避免重复后端 diff。
- V1 目录不参与任何 restore、移动或删除。
- 所有 Git 外部写操作都留到候选分支完整验证之后。
