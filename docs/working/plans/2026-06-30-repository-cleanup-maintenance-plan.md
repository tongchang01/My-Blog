# 仓库清理维护计划

> 日期：2026-06-30
> 状态：已执行记录
> 适用范围：远端分支与仓库治理清理
> 依据：`origin` 远端分支扫描、`docs/governance/branch-policy.md`、`docs/governance/2026-06-26-repository-reorganization-plan.md`

## 1. 目的

当前远端分支数量较多，且存在旧候选分支、归档分支、已合入功能分支和仍待裁决分支混在一起的问题。本文件记录本次仓库清理维护任务的现状、执行顺序、裁决依据和最终结果。

本计划只处理远端分支与仓库治理相关事项，不处理业务代码、功能实现、文档重组或本地工作区清理。

## 2. 当前事实

远端仓库：

```text
origin  https://github.com/tongchang01/My-Blog.git
```

当前远端默认入口：

```text
origin/HEAD -> origin/main
```

执行时所在本地工作区：

```text
docs/sync-frontend-integration-docs...origin/docs/sync-frontend-integration-docs
```

执行前后均通过 `git status --short --branch` 确认没有未提交变更。

## 3. 长期保留分支

| 分支 | 用途 | 处理 |
| --- | --- | --- |
| `origin/main` | V2 当前主线 | 保留 |
| `origin/archive/v1-master-2026-06-26` | V1 master 归档 | 保留 |
| `origin/archive/backend-v2-refactor-2026-06-26` | 旧后端 V2 过程分支归档 | 保留 |
| `origin/archive/frontend-v2-integration-2026-06-26` | 旧前端引入过程分支归档 | 保留 |

## 4. 第一批删除结果

以下分支已并入 `origin/main`，或已有对应归档分支，已完成远端删除。

| 分支 | 判断 | 结果 |
| --- | --- | --- |
| `origin/backend-v2-refactor` | 与 `archive/backend-v2-refactor-2026-06-26` 同提交，且已并入 `main` | 已删除 |
| `origin/frontend-v2-clean` | 已并入 `main`，名称不再符合长期分支约定 | 已删除 |
| `origin/integration/v2-main-prep` | 已并入 `main`，主线候选任务已结束 | 已删除 |
| `origin/feature/admin-comment-reply` | 已并入 `main` | 已删除 |
| `origin/feature/admin-phase-2` | 已并入 `main` | 已删除 |
| `origin/frontend-v2-integration` | 与 `archive/frontend-v2-integration-2026-06-26` 同提交，属于旧过程分支 | 已删除 |

已执行命令：

```powershell
git push origin --delete backend-v2-refactor
git push origin --delete frontend-v2-clean
git push origin --delete integration/v2-main-prep
git push origin --delete feature/admin-comment-reply
git push origin --delete feature/admin-phase-2
git push origin --delete frontend-v2-integration
```

## 5. 裁决后删除结果

以下分支相对 `origin/main` 曾有独有提交，已逐个确认是否吸收、替代或废弃。

| 分支 | 独有内容 | 裁决与结果 |
| --- | --- | --- |
| `origin/backend-v2-integration-ready` | `39b9f2a 修复本地 MySQL 公钥检索配置` | 当前 `application-local.yml` 和本地 MySQL 手册已包含 `allowPublicKeyRetrieval=true`，已删除 |
| `origin/feature/frontend-v2-backend-integration` | `e0310fb 补充前台数据源盘点文档` | 当前前台差异盘点和批次计划已覆盖有效结论，旧单文件盘点不再保留远端分支，已删除 |
| `origin/docs-reorganization` | 文档整理平行线 | 17 个提交与当前文档分支 patch 等价；剩余非等价内容已由当前分支覆盖或属于旧树回退，已删除 |

## 6. `master` 处理结果

当前：

```text
origin/master == origin/archive/v1-master-2026-06-26
```

`master` 不再是远端默认分支。已确认：

1. 确认 GitHub 默认分支仍为 `main`。
2. 确认 `archive/v1-master-2026-06-26` 和 `v1-final-before-v2` tag 均存在。
3. `origin/master`、`origin/archive/v1-master-2026-06-26` 和 `v1-final-before-v2` 指向同一提交 `54054e9e38d2cba0920ac55fc20a42fe239b5a15`。

处理结果：`origin/master` 已删除。

已执行命令：

```powershell
git push origin --delete master
```

## 7. 执行检查

清理前后运行：

```powershell
git fetch origin --prune
git branch -r --sort=-committerdate -vv
git status --short --branch
```

删除前均明确列出本批次要删除的远端分支，未使用通配符删除。

## 8. 实际保留远端分支

```text
origin/archive/backend-v2-refactor-2026-06-26
origin/archive/frontend-v2-integration-2026-06-26
origin/archive/v1-master-2026-06-26
origin/docs/sync-frontend-integration-docs
origin/main
```

## 9. 风险控制

- 不使用 `git push --force`。
- 不改写历史。
- 不删除 `archive/*` 分支。
- 不删除仍有独有提交且未裁决的分支。
- 不把本地未提交文档变更混入远端清理任务。
- 删除前后都保留命令输出或分支列表，方便回溯。

## 10. 已关闭确认项

1. 第一批删除候选已删除。
2. `backend-v2-integration-ready` 的 MySQL 配置修复已由当前分支覆盖，分支已删除。
3. `feature/frontend-v2-backend-integration` 的有效内容已由当前前台差异盘点和批次计划覆盖，分支已删除。
4. `docs-reorganization` 已完成 patch 等价和树级差异确认，分支已删除。
5. `master` 已确认由 `main` 取代，且 V1 归档分支和 tag 均存在，分支已删除。
