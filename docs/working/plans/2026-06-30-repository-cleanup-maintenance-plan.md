# 仓库清理维护计划

> 日期：2026-06-30
> 状态：临时维护计划
> 适用范围：远端分支与仓库治理清理
> 依据：`origin` 远端分支扫描、`docs/governance/branch-policy.md`、`docs/governance/2026-06-26-repository-reorganization-plan.md`

## 1. 目的

当前远端分支数量较多，且存在旧候选分支、归档分支、已合入功能分支和仍待裁决分支混在一起的问题。本计划用于记录本次仓库清理维护任务的现状、执行顺序和确认点。

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

当前本地工作区：

```text
main...origin/main
```

工作区存在未提交文档变更和新增计划/review 文件。本次分支清理前不得混入这些本地变更。

## 3. 长期保留分支

| 分支 | 用途 | 处理 |
| --- | --- | --- |
| `origin/main` | V2 当前主线 | 保留 |
| `origin/archive/v1-master-2026-06-26` | V1 master 归档 | 保留 |
| `origin/archive/backend-v2-refactor-2026-06-26` | 旧后端 V2 过程分支归档 | 保留 |
| `origin/archive/frontend-v2-integration-2026-06-26` | 旧前端引入过程分支归档 | 保留 |

## 4. 第一批删除候选

以下分支已并入 `origin/main`，或已有对应归档分支，适合作为第一批清理对象。

| 分支 | 判断 | 建议 |
| --- | --- | --- |
| `origin/backend-v2-refactor` | 与 `archive/backend-v2-refactor-2026-06-26` 同提交，且已并入 `main` | 删除 |
| `origin/frontend-v2-clean` | 已并入 `main`，名称不再符合长期分支约定 | 删除 |
| `origin/integration/v2-main-prep` | 已并入 `main`，主线候选任务已结束 | 删除 |
| `origin/feature/admin-comment-reply` | 已并入 `main` | 删除 |
| `origin/feature/admin-phase-2` | 已并入 `main` | 删除 |
| `origin/frontend-v2-integration` | 与 `archive/frontend-v2-integration-2026-06-26` 同提交，属于旧过程分支 | 删除 |

建议命令：

```powershell
git push origin --delete backend-v2-refactor
git push origin --delete frontend-v2-clean
git push origin --delete integration/v2-main-prep
git push origin --delete feature/admin-comment-reply
git push origin --delete feature/admin-phase-2
git push origin --delete frontend-v2-integration
```

## 5. 暂缓裁决分支

以下分支相对 `origin/main` 仍有独有提交，删除前必须先确认是否吸收、重做或废弃。

| 分支 | 独有内容 | 暂缓原因 |
| --- | --- | --- |
| `origin/backend-v2-integration-ready` | `39b9f2a 修复本地 MySQL 公钥检索配置` | 涉及本地 MySQL 配置，需确认是否已由其他提交替代 |
| `origin/feature/frontend-v2-backend-integration` | `e0310fb 补充前台数据源盘点文档` | 涉及前台数据源盘点文档，需确认是否要并入当前文档体系 |
| `origin/docs-reorganization` | 文档整理平行线，仍显示相对 `main` 有独有提交 | 需要比对是否只是重复迁移提交，不能只按分支名删除 |

## 6. `master` 处理

当前：

```text
origin/master == origin/archive/v1-master-2026-06-26
```

`master` 不再是远端默认分支。根据分支治理策略，GitHub 默认分支切到 `main` 后，`master` 应冻结或删除。

建议作为单独确认项处理：

1. 确认 GitHub 默认分支仍为 `main`。
2. 确认 `archive/v1-master-2026-06-26` 和 `v1-final-before-v2` tag 均存在。
3. 决定删除 `origin/master`，或保留但标注废弃。

如果确认删除：

```powershell
git push origin --delete master
```

## 7. 执行前检查

每次清理前运行：

```powershell
git fetch origin --prune
git branch -r --sort=-committerdate -vv
git status --short --branch
```

删除前必须再次明确列出本批次要删除的远端分支，不使用通配符删除。

## 8. 执行顺序

1. 再次刷新远端引用。
2. 确认本批次只删除第一批删除候选。
3. 执行逐条 `git push origin --delete <branch>`。
4. 再次运行 `git fetch origin --prune`。
5. 记录清理后的远端分支列表。
6. 对暂缓裁决分支分别做差异审查。
7. 单独确认是否删除或冻结 `master`。

## 9. 风险控制

- 不使用 `git push --force`。
- 不改写历史。
- 不删除 `archive/*` 分支。
- 不删除仍有独有提交且未裁决的分支。
- 不把本地未提交文档变更混入远端清理任务。
- 删除前后都保留命令输出或分支列表，方便回溯。

## 10. 待确认问题

1. 第一批删除候选是否可以直接执行删除。
2. `backend-v2-integration-ready` 的 MySQL 配置修复是否需要并入 `main`。
3. `feature/frontend-v2-backend-integration` 的数据源盘点文档是否需要并入当前 `docs/working/` 或 `docs/handbook/`。
4. `docs-reorganization` 是否仍有当前有效内容，还是可以在比对后删除。
5. `master` 是删除，还是保留并在仓库说明中标注废弃。
