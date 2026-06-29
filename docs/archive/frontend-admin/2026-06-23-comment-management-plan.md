# 后台评论管理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在后台工程接入 V2 评论管理能力，让 ADMIN 能审核、隐藏、删除、恢复评论，DEMO 只读。

**Architecture:** 复用后端现有 `/api/admin/comments` 契约；前端按 `api -> query/model -> composable -> page -> route/locale/docs` 分层实现。页面状态独立于文章列表和文章回收站，避免不同分页与错误状态互相耦合。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Axios、Vitest、Vue Test Utils。

---

### Task 1：文档与范围冻结

**Files:**
- Create: `frontend/apps/admin/docs/2026-06-23-comment-management-design.md`
- Create: `frontend/apps/admin/docs/2026-06-23-comment-management-plan.md`

- [x] 写明后端复用契约、菜单位置、页面结构、操作规则、错误处理和暂不实现项。
- [ ] 运行 `git diff --stat` 和 `git status --short`。
- [ ] 提交：`git commit -m "规划后台评论管理页面"`

### Task 2：评论 API、模型与查询参数

**Files:**
- Create: `frontend/apps/admin/src/features/comments/model.ts`
- Create: `frontend/apps/admin/src/features/comments/query.ts`
- Create: `frontend/apps/admin/src/features/comments/query.test.ts`
- Create: `frontend/apps/admin/src/api/comment.ts`
- Create: `frontend/apps/admin/src/api/comment.test.ts`

- [ ] 先写失败测试：`buildCommentListParams` 应去掉空关键词和 ALL 状态，保留 `includeDeleted=true`。
- [ ] 先写失败测试：API 应调用 `/api/admin/comments`、`/approve`、`/hide`、`/restore` 和 `DELETE /{id}`。
- [ ] 实现模型、查询构造和 API。
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin test -- src/features/comments/query.test.ts src/api/comment.test.ts`
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 提交：`git commit -m "接入后台评论管理接口"`

### Task 3：评论管理状态组合函数

**Files:**
- Create: `frontend/apps/admin/src/features/comments/useCommentManagement.ts`
- Create: `frontend/apps/admin/src/features/comments/useCommentManagement.test.ts`

- [ ] 先写失败测试：初始化加载列表。
- [ ] 先写失败测试：搜索重置页码，重置恢复默认筛选。
- [ ] 先写失败测试：approve/hide/delete/restore 成功刷新当前页。
- [ ] 先写失败测试：尾页操作后为空时回退上一页。
- [ ] 先写失败测试：操作失败时保留列表并设置 `operationError`。
- [ ] 实现 composable。
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin test -- src/features/comments/useCommentManagement.test.ts`
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 提交：`git commit -m "实现评论管理状态控制"`

### Task 4：评论管理页面

**Files:**
- Create: `frontend/apps/admin/src/features/comments/index.vue`
- Create: `frontend/apps/admin/src/features/comments/index.test.ts`

- [ ] 先写失败测试：渲染筛选卡片、结果表格、分页和空状态。
- [ ] 先写失败测试：ADMIN 显示操作按钮并通过确认框触发状态方法。
- [ ] 先写失败测试：DEMO 不显示操作列。
- [ ] 实现页面。
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin test -- src/features/comments/index.test.ts src/features/comments/useCommentManagement.test.ts`
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 提交：`git commit -m "实现后台评论管理页面"`

### Task 5：路由、三语文案与文档

**Files:**
- Modify: `frontend/apps/admin/src/router/modules/articles.ts`
- Modify: `frontend/apps/admin/src/router/static-router.test.ts`
- Modify: `frontend/apps/admin/locales/zh-CN.yaml`
- Modify: `frontend/apps/admin/locales/ja.yaml`
- Modify: `frontend/apps/admin/locales/en.yaml`
- Modify: `frontend/apps/admin/docs/README.md`
- Modify: `frontend/apps/admin/docs/2026-06-23-comment-management-plan.md`

- [ ] 先写失败测试：内容管理菜单包含 `/comments/list`，ADMIN/DEMO 可访问。
- [ ] 补充三语菜单、筛选、表格、状态、操作、确认框和错误文案。
- [ ] 更新后台文档。
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin test -- src/router/static-router.test.ts src/features/comments`
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 提交：`git commit -m "接入评论管理菜单与三语文案"`

### Task 6：阶段验证与推送

- [ ] 后端运行：`mvn "-Dtest=AdminCommentControllerTest,CommentOpenApiTest" test`
- [ ] 后端阶段结束运行：`mvn clean test`
- [ ] 前端运行：`corepack pnpm --dir frontend/apps/admin test`
- [ ] 前端运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 前端运行：`corepack pnpm --dir frontend/apps/admin build`
- [ ] 检查两个工作区 `git status --short`。
- [ ] 推送 `backend-v2-integration-ready` 与 `frontend-v2-clean`。
