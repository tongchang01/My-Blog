# V2 分支稳定化清单

记录时间：2026-06-21（Asia/Tokyo）

## 冻结引用

| 引用 | 冻结 SHA | 说明 |
| --- | --- | --- |
| `master` | `aa7e3b0c1cea1d814d067fb8faed7fdd4a8d6d3f` | 本地主工作区；包含未推送的 worktree ignore 提交 |
| `backend-v2-refactor` | `775280e34baaea6518b8b57409b4781b97913739` | 后端 V2 已验证基线 |
| `frontend-v2-integration` | `ce23e98e521471edcde1fef48e351dbf099f9899` | 当前前后台工程最终状态及稳定化计划 |
| 后端/前端 merge-base | `775280e34baaea6518b8b57409b4781b97913739` | 前端分支直接建立在后端基线上 |

抓取远端后的冻结引用：

| 远端引用 | 冻结 SHA |
| --- | --- |
| `origin/master` | `54054e9e38d2cba0920ac55fc20a42fe239b5a15` |
| `origin/backend-v2-refactor` | `775280e34baaea6518b8b57409b4781b97913739` |
| `origin/frontend-v2-integration` | `710526e10ff21657be617a039726ca69ea8dfad6` |

本次稳定化不改写上述引用，不删除现有分支，不移动或删除 V1 文件。

## 提交所有权

### 后端契约

按顺序迁入后端候选分支：

1. `5a5fc43a224f078f3612a44cde1b0e7dcbcc0564` `修复后台账号ID前端精度契约`
2. `5003f3d530fe254521bcb87e8e2c1c29b59b3586` `修复后台文章ID前端精度契约`
3. `3d7bf489263c3eeea00c5e64bddb9e1b6684f5f9` `修复后台分类标签ID前端精度契约`

允许范围：

- `MyBlog-springboot-v2/src/main/**/web/**`
- `MyBlog-springboot-v2/src/test/**/web/**`
- `docs/project-handbook/api-contract/auth.md`
- 与上述契约直接对应的既有后端计划文档

### 前端最终状态

从冻结的 `frontend-v2-integration` 恢复以下完整目录：

- `frontend/**`
- `docs/archive/frontend-user-v2-migration/**`

其中 `frontend/**` 是两个固定工程快照，文件量大，必须拆为博客前台、管理后台两个独立提交；不得在工程迁移提交中混入后端文件。

### 前端专属文档白名单

- `frontend/apps/admin/docs/**`
- `docs/project-handbook/frontend-user/README.md`
- `docs/superpowers/specs/2026-06-20-frontend-v2-import-design.md`
- `docs/superpowers/plans/2026-06-20-frontend-v2-import.md`
- `docs/superpowers/specs/2026-06-20-frontend-blog-backend-integration-design.md`
- `docs/superpowers/plans/2026-06-20-frontend-blog-backend-integration.md`

### 前后端共享文档白名单

以下文件只从冻结前端分支逐文件恢复，不允许整体恢复 `docs/`：

- `docs/project-handbook/CLAUDE.md`
- `docs/project-handbook/INDEX.md`
- `docs/project-handbook/api-contract/article.md`
- `docs/project-handbook/api-contract/auth.md`
- `docs/project-handbook/product/decisions-draft.md`
- `docs/project-handbook/roadmap.md`
- `docs/project-handbook/status.md`

`docs/project-handbook/frontend-admin/README.md` 的删除属于后台文档迁入 `frontend/apps/admin/docs/**` 后的归档调整，随前端文档提交重放。

### 稳定化文档

以下文档属于分支与本地开发基础设施，不归入前端工程提交；后端候选分支需保留：

- `docs/superpowers/specs/2026-06-21-branch-and-mysql-stabilization-design.md`
- `docs/superpowers/plans/2026-06-21-branch-stabilization.md`
- `docs/superpowers/plans/2026-06-21-local-mysql-baseline.md`
- `docs/project-handbook/plans/branch-stabilization-manifest.md`

## 候选分支

| 分支 | 基线 | 状态 |
| --- | --- | --- |
| `codex/backend-v2-integration-ready` | `backend-v2-refactor` 冻结 SHA | 待建立 |
| `codex/frontend-v2-clean` | 后端候选最终 SHA | 待建立 |

候选分支验证结果和新旧提交映射在执行完成后补充。
