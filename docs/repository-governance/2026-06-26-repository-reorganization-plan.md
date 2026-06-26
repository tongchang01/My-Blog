# MyBlog 本地与远程仓库整理计划

> 日期：2026-06-26  
> 立场：以长期可维护性、启动便利性、远端语义清晰度为优先，不迁就当前临时分支名。  
> 状态：计划文件。本文只描述整理方案，不代表已经执行。

## 1. 结论

现在应该整理仓库，但不应该通过“删除 V1 历史”来整理。Git 历史保留 V1 是正常的；真正需要治理的是：

1. 远端默认主线仍叫 `master`，且语义是 V1。
2. V2 代码分散在多个候选分支，日常启动需要理解分支关系。
3. 本地根目录、worktree、远端分支没有明确职责边界。
4. 前端候选分支 `frontend-v2-clean` 尚未包含后端候选分支 `backend-v2-integration-ready` 的全部提交，不能直接升为主线。

我的建议是：

- 新 V2 主线使用 `main`，不继续使用 `master`。
- 当前 V1 `master` 归档为 `archive/v1-master-2026-06-26`。
- 当前 V2 前后端候选先合并到 `integration/v2-main-prep`。
- 验证通过后，将 GitHub 默认分支切到 `main`。
- 保留少量归档分支，删除过时临时分支。
- 本地长期启动只使用一个稳定工作区，不再围绕功能分支启动。

## 2. 当前审计事实

### 2.1 本地 worktree

当前本地存在 3 个工作区：

| 路径 | 分支 | 用途判断 |
|---|---|---|
| `E:\My-Blog` | `master` | 当前主工作区，仍是 V1 语义；本地比远端 `master` 多 1 个 `chore: ignore local worktrees` 提交 |
| `E:\My-Blog\.worktrees\backend-v2-refactor` | `backend-v2-integration-ready` | 当前后端 V2 候选 |
| `E:\My-Blog\.worktrees\frontend-v2-clean` | `frontend-v2-clean` | 当前前端、前台、后台 V2 候选，最近开发都在这里 |

### 2.2 当前远端分支

| 远端分支 | 最新提交 | 用途判断 |
|---|---:|---|
| `origin/master` | `54054e9` | V1 主线/旧主线，不应继续作为 V2 默认入口 |
| `origin/frontend-v2-clean` | `90caede` | 最新前端 workspace、前台、后台候选 |
| `origin/backend-v2-integration-ready` | `ac7febd` | 最新后端 V2 候选 |
| `origin/backend-v2-refactor` | `775280e` | 旧后端基线，已被两个 V2 候选包含 |
| `origin/frontend-v2-integration` | `710526e` | 旧前端引入/后台基础候选，已明显落后，不应继续作为主线来源 |

### 2.3 分支包含关系

只读审计结果：

| 判断项 | 结果 |
|---|---|
| `frontend-v2-clean` 是否包含 `backend-v2-integration-ready` | 否。`frontend-v2-clean` 独有 76 个提交，`backend-v2-integration-ready` 独有 7 个提交 |
| `frontend-v2-clean` 是否包含 `backend-v2-refactor` | 是 |
| `backend-v2-integration-ready` 是否包含 `backend-v2-refactor` | 是 |
| `origin/master` 是否包含 `frontend-v2-clean` | 否。`frontend-v2-clean` 相对 `origin/master` 独有 368 个提交 |
| `frontend-v2-integration` 是否可直接当主线 | 否。它是旧候选，不是最新开发承载分支 |

结论：V2 主线必须先从 `frontend-v2-clean` 与 `backend-v2-integration-ready` 整合出来，不能直接把任一现有分支当最终主线。

## 3. 目标分支模型

### 3.1 最终远端分支

整理完成后，远端长期保留：

| 分支 | 语义 | 保护策略 |
|---|---|---|
| `main` | V2 默认主线，日常启动和后续开发基线 | 保护，禁止直接提交，功能分支合并 |
| `archive/v1-master-2026-06-26` | V1 最终归档分支 | 只读，不继续开发 |
| `archive/backend-v2-refactor-2026-06-26` | 旧后端 V2 基线归档，可选 | 只读，短期保留 |
| `archive/frontend-v2-integration-2026-06-26` | 旧前端引入分支归档，可选 | 只读，短期保留 |

整理完成后，远端短期保留 1 到 2 周：

| 分支 | 原因 |
|---|---|
| `frontend-v2-clean` | 新 `main` 稳定前的前端候选备份 |
| `backend-v2-integration-ready` | 新 `main` 稳定前的后端候选备份 |

整理完成并确认 `main` 稳定后，删除：

| 分支 | 删除理由 |
|---|---|
| `backend-v2-refactor` | 已被后端候选和前端候选包含，不应继续作为开发入口 |
| `frontend-v2-integration` | 已被 `frontend-v2-clean` 取代，继续存在会误导 |
| `master` | 如果 GitHub 默认分支已切到 `main`，则删除远端 `master`；如果 GitHub 不允许立即删除，则冻结并在 README 标注废弃 |

### 3.2 新功能分支命名

后续开发统一从 `main` 切分支：

| 类型 | 命名 |
|---|---|
| 功能 | `feature/<scope>-<short-name>` |
| 修复 | `fix/<scope>-<short-name>` |
| 重构 | `refactor/<scope>-<short-name>` |
| 文档 | `docs/<scope>-<short-name>` |
| 临时整合 | `integration/<short-name>` |
| 归档 | `archive/<old-name>-YYYY-MM-DD` |

示例：

- `feature/admin-comment-reply`
- `feature/blog-api-integration`
- `fix/backend-local-storage`
- `docs/local-startup-guide`
- `integration/v2-main-prep`
- `archive/v1-master-2026-06-26`

不再使用：

- `*-clean`
- `*-ready`
- `*-integration-ready`
- `codex/*`

这些名字适合临时协作，不适合作为长期主线。

## 4. 本地目录模型

整理完成后，本地建议固定为：

```text
E:\My-Blog\
  main-workspace\          # 日常启动目录，固定 main
  worktrees\
    feature-admin-comment-reply\     # 临时功能 worktree 示例
    fix-backend-local-storage\       # 临时修复 worktree 示例
```

如果不想移动现有目录，也可以采用过渡方案：

```text
E:\My-Blog\                         # 迁到 main 后作为日常启动目录
E:\My-Blog\.worktrees\frontend-v2-clean   # 稳定后删除
E:\My-Blog\.worktrees\backend-v2-refactor # 稳定后删除
```

日常启动只允许使用一个固定目录：

```text
branch: main

后端：MyBlog-springboot-v2
前台：frontend/apps/blog
后台：frontend/apps/admin
```

以后开发新功能时，不在日常启动目录里来回切分支；需要并行开发时再临时创建 worktree。

## 5. 推荐执行阶段

### 阶段 0：冻结窗口

目的：避免整理期间继续产生新分支差异。

规则：

- 暂停功能开发。
- 不再向 `frontend-v2-clean`、`backend-v2-integration-ready` 直接追加业务提交。
- 整理期间所有提交只进入 `integration/v2-main-prep`。

### 阶段 1：创建归档分支和标签

从远端当前状态创建归档。

```powershell
git fetch origin --prune

git branch archive/v1-master-2026-06-26 origin/master
git push origin archive/v1-master-2026-06-26

git tag v1-final-before-v2 origin/master
git push origin v1-final-before-v2
```

如果要保留旧 V2 过程分支，也创建归档：

```powershell
git branch archive/backend-v2-refactor-2026-06-26 origin/backend-v2-refactor
git push origin archive/backend-v2-refactor-2026-06-26

git branch archive/frontend-v2-integration-2026-06-26 origin/frontend-v2-integration
git push origin archive/frontend-v2-integration-2026-06-26
```

### 阶段 2：创建 V2 主线候选

以最新前端候选为基础，因为它包含前台、后台和大部分后端基线。

```powershell
git fetch origin --prune
git switch --create integration/v2-main-prep origin/frontend-v2-clean
git merge --no-ff origin/backend-v2-integration-ready
```

预期：

- 可能出现冲突，尤其是 `MyBlog-springboot-v2` 配置、文档、启动说明。
- 冲突解决原则：以 `backend-v2-integration-ready` 的后端运行配置为准，以 `frontend-v2-clean` 的前端 workspace 为准。

提交信息：

```text
整合V2前后端主线候选
```

### 阶段 3：整理主线启动说明

在 `integration/v2-main-prep` 上整理根目录文档，而不是分散在前端或后端子目录里。

建议新增或重写：

| 文件 | 作用 |
|---|---|
| `README.md` | V2 主线入口，说明这是 V2 主线 |
| `docs/local-development.md` | 本地三端启动指南 |
| `docs/repository-governance/branch-policy.md` | 分支策略 |
| `.env.example` 或 `MyBlog-springboot-v2/.env.example` | 后端本地环境变量样例 |

本地启动文档必须写清：

```text
后端：
  path: MyBlog-springboot-v2
  profile: local
  port: 8080
  database: myblog_v2_dev
  env:
    MYBLOG_DATASOURCE_USERNAME=root
    MYBLOG_DATASOURCE_PASSWORD=<local-mysql-password>
    MYBLOG_JWT_SECRET=<local-jwt-secret-at-least-32-bytes>
    MYBLOG_STATS_HASH_SECRET=<local-stats-hash-secret-at-least-32-bytes>

前台：
  path: frontend/apps/blog
  port: 5173

后台：
  path: frontend/apps/admin
  port: 8848
```

不要把 V1 作为默认启动路径。V1 只在归档文档中说明。

提交信息：

```text
整理V2主线本地启动说明
```

### 阶段 4：验证候选主线

必须在 `integration/v2-main-prep` 上验证。

后端：

```powershell
cd MyBlog-springboot-v2
mvn test
```

后台：

```powershell
cd frontend/apps/admin
pnpm install --frozen-lockfile
npm test
npm run typecheck
npm run build
```

前台：

```powershell
cd frontend/apps/blog
pnpm install --frozen-lockfile
npm run build
```

如果前台目前没有完整测试，以构建和本地启动为最低门槛。

验证结果写入：

```text
docs/repository-governance/v2-main-prep-verification.md
```

提交信息：

```text
记录V2主线候选验证结果
```

### 阶段 5：发布 `main`

候选通过后创建 `main`。

```powershell
git switch integration/v2-main-prep
git branch main
git push origin main
```

然后在 GitHub 仓库设置里：

1. 将默认分支从 `master` 改为 `main`。
2. 给 `main` 开启保护规则。
3. 禁止直接 push，后续通过 PR 或明确的维护流程合并。

如果必须保留 `master` 名称，也不要让它继续指向 V1。可选方案是：

```powershell
git push origin main:master --force-with-lease
```

但我的推荐不是这个。推荐使用 `main`，让 `master` 退役，避免名字继续携带历史歧义。

### 阶段 6：本地工作区收敛

推荐将 `E:\My-Blog` 主工作区切到 `main`，作为唯一日常启动目录。

```powershell
cd E:\My-Blog
git fetch origin
git switch main
git pull --ff-only
```

如果 `E:\My-Blog` 当前有本地-only 提交 `aa7e3b0 chore: ignore local worktrees`，需要先判断是否应 cherry-pick 到 `main`。如果只是本地 worktree ignore，可纳入 `main` 或改成本机全局 exclude，不应阻塞主线整理。

稳定后删除旧 worktree：

```powershell
git worktree remove E:\My-Blog\.worktrees\frontend-v2-clean
git worktree remove E:\My-Blog\.worktrees\backend-v2-refactor
```

如果暂时不删除，也必须标注为过渡目录，不再用于日常启动。

### 阶段 7：远端分支清理

`main` 稳定 1 到 2 周后执行。

建议删除：

```powershell
git push origin --delete frontend-v2-integration
git push origin --delete backend-v2-refactor
```

确认无回退需求后删除：

```powershell
git push origin --delete frontend-v2-clean
git push origin --delete backend-v2-integration-ready
```

如果 GitHub 默认分支已经切到 `main`，且 V1 已归档：

```powershell
git push origin --delete master
```

如果不删除 `master`，必须在根 README 和 GitHub branch description 中标注：

```text
master is deprecated. Use main for V2 development. V1 is archived at archive/v1-master-2026-06-26.
```

## 6. 风险控制

### 6.1 不做历史重写

不使用 `git filter-repo`、`git rebase --root`、`git push --mirror --force`。

原因：

- 当前问题是分支语义混乱，不是 Git 对象历史错误。
- 删除 V1 历史会破坏追溯和回滚。
- 远端和本地已有多个 worktree，历史重写成本高且收益低。

### 6.2 不用直接 `--force`

如果必须覆盖远端分支，只允许：

```powershell
git push --force-with-lease
```

不能用：

```powershell
git push --force
```

### 6.3 每阶段都有可回退点

| 阶段 | 回退点 |
|---|---|
| 归档 V1 后 | `archive/v1-master-2026-06-26` 和 `v1-final-before-v2` |
| 创建候选主线后 | `integration/v2-main-prep` |
| 创建 main 后 | `origin/main` 与归档分支并存 |
| 删除旧分支前 | 先等待 1 到 2 周 |

### 6.4 不把 V1 删除当作第一目标

V1 目录可以后续从 `main` 中删除，但这应作为单独任务处理：

```text
refactor/remove-v1-runtime-from-main
```

是否删除 V1 目录取决于：

- 是否还需要 V1 代码做数据迁移参考。
- 是否还需要 V1 线上热修。
- V2 是否完成基础数据迁移和生产部署。

我的建议：主线切到 `main` 后，先保留 V1 目录一段时间；等数据迁移完成后，再单独删除 V1 运行目录，只保留归档分支。

## 7. 最终目标状态

远端：

```text
main                              # V2 主线，默认分支
archive/v1-master-2026-06-26      # V1 归档
v1-final-before-v2                 # V1 标签
```

本地：

```text
E:\My-Blog                         # 固定 main，日常启动目录
```

日常命令：

```powershell
cd E:\My-Blog
git pull --ff-only
```

启动路径：

```text
后端：E:\My-Blog\MyBlog-springboot-v2
前台：E:\My-Blog\frontend\apps\blog
后台：E:\My-Blog\frontend\apps\admin
```

后续开发：

```powershell
git switch main
git pull --ff-only
git switch -c feature/admin-comment-reply
```

或用临时 worktree：

```powershell
git worktree add E:\My-Blog\worktrees\feature-admin-comment-reply -b feature/admin-comment-reply main
```

## 8. 我建议的下一步

下一步不要直接切主线。先执行：

1. 创建 `archive/v1-master-2026-06-26`。
2. 创建 `v1-final-before-v2` tag。
3. 创建 `integration/v2-main-prep`。
4. 合并 `backend-v2-integration-ready` 到 `frontend-v2-clean`。
5. 解决冲突并完整验证。

完成这一步后，再决定是否马上创建 `main` 并切 GitHub 默认分支。
