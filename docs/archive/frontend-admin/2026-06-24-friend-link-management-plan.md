# 后台友链管理实施计划

**Goal:** 在后台工程接入 V2 友链管理能力，让 ADMIN 能新增、编辑、显示/隐藏、排序和删除友链，DEMO 只读。

**Architecture:** 复用后端现有 `/api/admin/friend-links` 契约；前端按 `api -> query/model/form -> composable -> page -> route/locale/docs` 分层实现。页面状态独立于文章、评论、分类标签模块。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Axios、Vitest、Vue Test Utils。

---

### Task 1：文档与范围冻结

**Files:**

- Create: `frontend/apps/admin/docs/2026-06-24-friend-link-management-design.md`
- Create: `frontend/apps/admin/docs/2026-06-24-friend-link-management-plan.md`

- [x] 写明后端复用契约、菜单位置、页面结构、操作规则、错误处理和暂不实现项。
- [ ] 运行 `git diff --stat` 和 `git status --short`。
- [ ] 提交：`git commit -m "规划后台友链管理页面"`

### Task 2：友链 API、模型、查询与表单

**Files:**

- Create: `frontend/apps/admin/src/features/friend-links/model.ts`
- Create: `frontend/apps/admin/src/features/friend-links/query.ts`
- Create: `frontend/apps/admin/src/features/friend-links/query.test.ts`
- Create: `frontend/apps/admin/src/features/friend-links/form.ts`
- Create: `frontend/apps/admin/src/features/friend-links/form.test.ts`
- Create: `frontend/apps/admin/src/api/friend-link.ts`
- Create: `frontend/apps/admin/src/api/friend-link.test.ts`

- [ ] 先写失败测试：查询参数应保留分页、去掉空关键词和 ALL 状态。
- [ ] 先写失败测试：表单应校验名称、URL、排序值，并输出完整 payload。
- [ ] 先写失败测试：API 应调用列表、详情、新增、更新、状态、排序、删除接口。
- [ ] 实现模型、查询构造、表单和 API。
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin test -- src/features/friend-links/query.test.ts src/features/friend-links/form.test.ts src/api/friend-link.test.ts`
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 提交：`git commit -m "接入后台友链管理接口"`

### Task 3：友链管理状态组合函数

**Files:**

- Create: `frontend/apps/admin/src/features/friend-links/useFriendLinkManagement.ts`
- Create: `frontend/apps/admin/src/features/friend-links/useFriendLinkManagement.test.ts`

- [ ] 先写失败测试：初始化加载列表。
- [ ] 先写失败测试：搜索重置页码，重置恢复默认筛选。
- [ ] 先写失败测试：新增、编辑、状态切换、删除成功后刷新列表。
- [ ] 先写失败测试：排序值变化后只提交 dirty sort items。
- [ ] 先写失败测试：操作失败时保留列表并设置 `operationError`。
- [ ] 实现 composable。
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin test -- src/features/friend-links/useFriendLinkManagement.test.ts`
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 提交：`git commit -m "实现友链管理状态控制"`

### Task 4：友链管理页面

**Files:**

- Create: `frontend/apps/admin/src/features/friend-links/index.vue`
- Create: `frontend/apps/admin/src/features/friend-links/index.test.ts`

- [ ] 先写失败测试：渲染筛选卡片、结果表格、分页和空状态。
- [ ] 先写失败测试：ADMIN 显示操作按钮并通过确认框触发状态/删除方法。
- [ ] 先写失败测试：DEMO 不显示写操作。
- [ ] 实现页面。
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin test -- src/features/friend-links/index.test.ts src/features/friend-links/useFriendLinkManagement.test.ts`
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 提交：`git commit -m "实现后台友链管理页面"`

### Task 5：路由、三语文案与文档

**Files:**

- Modify: `frontend/apps/admin/src/router/modules/articles.ts`
- Modify: `frontend/apps/admin/src/router/static-router.test.ts`
- Modify: `frontend/apps/admin/locales/zh-CN.yaml`
- Modify: `frontend/apps/admin/locales/ja.yaml`
- Modify: `frontend/apps/admin/locales/en.yaml`
- Modify: `frontend/apps/admin/docs/README.md`
- Modify: `frontend/apps/admin/docs/2026-06-24-friend-link-management-plan.md`

- [ ] 先写失败测试：内容管理菜单包含 `/friend-links/list`，ADMIN/DEMO 可访问。
- [ ] 补充三语菜单、筛选、表格、状态、操作、确认框和错误文案。
- [ ] 更新后台文档。
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin test -- src/router/static-router.test.ts src/features/friend-links`
- [ ] 运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 提交：`git commit -m "接入友链管理菜单与三语文案"`

### Task 6：阶段验证、本地联调与推送

- [ ] 后端运行：`mvn "-Dtest=AdminFriendLinkControllerTest,FriendLinkOpenApiTest,PublicFriendLinkControllerTest,FriendLinkIntegrationTest" test`
- [ ] 后端阶段结束运行：`mvn clean test`
- [ ] 前端运行：`corepack pnpm --dir frontend/apps/admin test`
- [ ] 前端运行：`corepack pnpm --dir frontend/apps/admin typecheck`
- [ ] 前端运行：`corepack pnpm --dir frontend/apps/admin build`
- [ ] 本地 MySQL `myblog_v2_dev` 准备友链测试数据。
- [ ] 浏览器联调 ADMIN 可写、DEMO 只读。
- [ ] 记录验收结果。
- [ ] 检查两个 worktree 的 `git status --short`。
- [ ] 推送 `backend-v2-integration-ready` 与 `frontend-v2-clean`。
