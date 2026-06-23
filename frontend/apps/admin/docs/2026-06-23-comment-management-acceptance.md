# 评论管理验收记录

日期：2026-06-23

## 范围

- 后端修正后台评论列表返回契约：评论 ID、目标 ID、父评论 ID、被回复评论 ID 按字符串序列化，避免前端 `number` 精度风险。
- 后端允许后台评论列表返回 `deleted` 状态，用于前端展示删除状态与恢复操作。
- 前端接入后台评论管理：
  - `/comments/list` 菜单与路由。
  - 评论筛选：目标类型、目标 ID、审核状态、关键词、是否包含已删除。
  - 评论列表：内容、作者、目标、审核状态、删除状态、创建时间。
  - ADMIN 操作：通过、隐藏、删除、恢复。
  - DEMO 角色只读。
  - 中文、英文、日文文案。

## 后端验证

工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor\MyBlog-springboot-v2`

```powershell
mvn "-Dtest=AdminCommentControllerTest,CommentOpenApiTest" test
```

结果：通过，4 个测试。

```powershell
mvn "-Dtest=ArticleOpenApiTest,CategoryTagOpenApiTest,AdminCommentControllerTest,CommentOpenApiTest" test
```

结果：通过，8 个测试。

```powershell
mvn clean test
```

结果：通过，641 个测试，0 failures，0 errors，4 skipped。

## 前端验证

工作目录：`E:\My-Blog\.worktrees\frontend-v2-clean`

```powershell
corepack pnpm --dir frontend/apps/admin test -- src/features/comments/query.test.ts src/api/comment.test.ts
```

结果：通过，2 个测试文件，4 个测试。

```powershell
corepack pnpm --dir frontend/apps/admin test -- src/features/comments/useCommentManagement.test.ts
```

结果：通过，1 个测试文件，6 个测试。

```powershell
corepack pnpm --dir frontend/apps/admin test -- src/features/comments/index.test.ts src/features/comments/useCommentManagement.test.ts
```

结果：通过，2 个测试文件，9 个测试。

```powershell
corepack pnpm --dir frontend/apps/admin test -- src/router/static-router.test.ts src/features/comments
```

结果：通过，4 个测试文件，14 个测试。

```powershell
corepack pnpm --dir frontend/apps/admin test
```

结果：通过，28 个测试文件，99 个测试。

```powershell
corepack pnpm --dir frontend/apps/admin typecheck
```

结果：通过。

```powershell
corepack pnpm --dir frontend/apps/admin build
```

结果：通过。构建过程中存在 `baseline-browser-mapping` 与 `Browserslist/caniuse-lite` 数据过期提示，但命令退出码为 0。

## 未覆盖项

- 本轮未做浏览器手工验收。
- 本轮未连接本地 MySQL 做端到端页面操作验收。

这两个验证适合在后续启动后端和后台前端后执行。
