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
| `backend-v2-integration-ready` | `backend-v2-refactor` 冻结 SHA | 已建立；已验证实现 tip `239e411bcdb839b16f4958ec8690e6250b5929bb` |
| `frontend-v2-clean` | 后端候选 | 已建立；首次同步后端基线 tip `038ebf63b5600ea0b17a728e6db46d7fd39c9ca5` |

清单提交本身会使分支 tip 前进；上表记录的是进入最终报告前实际执行测试的内容 tip，不把自引用 SHA 伪装为最终分支指针。

## 新旧提交映射

### 后端契约

| 来源提交 | 后端候选提交 |
| --- | --- |
| `5a5fc43` | `cded72e` |
| `5003f3d` | `badab12` |
| `3d7bf48` | `b747c8b` |

账号契约提交中只存在于前端链的旧后台计划文件未在后端候选复活。文章契约冲突只迁入后台 DTO、映射和 OpenAPI 断言，没有隐式带入来源提交的公开文章变更。

### 前端最终状态

| 目的 | 前端候选提交 |
| --- | --- |
| 博客前台固定快照 | `9d364e1` |
| 管理后台固定快照 | `324efb2` |
| 前端规格与归档文档 | `9ebc642` |
| 被 `.gitignore` 隐藏的跟踪文件和可执行位校准 | `3542a89` |

### 本地 MySQL 基线

| 目的 | 后端候选提交 |
| --- | --- |
| local profile 启用 Flyway | `afc1d52` |
| 固定开发种子和验收 SQL | `0607667` |
| 安全初始化与验收脚本 | `3a830f4` |
| 本地 MySQL 工作流文档 | `3a872e6` |
| 自动化验收状态 | `239e411` |

执行期间发生过一次无对应命令的外部 checkout，导致脚本提交 `2304fe3` 先落在前端候选；未改写该提交，而是将同一内容迁入后端候选为 `3a830f4`，随后用合并恢复候选分支祖先关系。

## 验证结果

- 后端契约迁入后：`mvn clean test`，637 tests，0 failures，0 errors，4 skipped。
- MySQL 自动化基线完成后：`mvn clean test`，638 tests，0 failures，0 errors，4 skipped。
- `initialize.contract-test.ps1`：通过；覆盖缺失凭据、错误数据库名、非空数据库拒绝和未传 `-Reset` 不执行 DROP。
- 博客前台：30 tests 通过，typecheck 通过，生产构建通过。
- 管理后台：43 tests 通过，typecheck 通过，生产构建通过。
- `frontend/` 与冻结的 `frontend-v2-integration` 最终状态一致。
- 前端候选相对后端候选不包含额外的 `MyBlog-springboot-v2/` 变更。
- 前端构建仍输出既有 Sass legacy/import 和浏览器数据过期警告，不影响退出码。

## 尚未完成的真实环境验收

当前终端未设置以下五个变量，因此未执行 `initialize.ps1 -Reset`，也未进行 ADMIN/DEMO 登录及真实接口联调：

- `MYBLOG_DATASOURCE_URL`
- `MYBLOG_DATASOURCE_USERNAME`
- `MYBLOG_DATASOURCE_PASSWORD`
- `MYBLOG_JWT_SECRET`
- `MYBLOG_STATS_HASH_SECRET`

未修改 `master`，未推送候选分支，未删除旧分支或 V1 文件。真实 MySQL 验收完成前，不得把本地 MySQL 基线标记为完全通过。
