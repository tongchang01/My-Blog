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

## 本地 MySQL 与浏览器验收

环境：

- 后端：`spring-boot:run -Dspring-boot.run.profiles=local`，监听 `http://localhost:8080`。
- 后台前端：`corepack pnpm --dir frontend/apps/admin dev --host 127.0.0.1`，监听 `http://127.0.0.1:8848/`。
- 数据库：本地 `myblog_v2_dev`。
- 本地验收账号：`admin` / `demo`，密码已重置为 `local-admin-123`。

测试数据：

- 追加固定 ID 评论 `9007199254742501` 到 `9007199254742506`，内容以 `Codex评论验收` 开头，覆盖：
  - 文章评论 / 留言板评论。
  - 待审核、已通过、已隐藏。
  - 正常、已删除。
  - 回复链路字段展示。

API 验收：

- `POST /api/auth/login`：`admin` 登录成功。
- `GET /api/auth/me`：返回 `ADMIN`。
- `GET /api/admin/comments?keyword=Codex评论验收&includeDeleted=true&page=1&size=20`：
  - 返回 6 条验收评论。
  - 大 ID 字段以字符串返回，例如 `9007199254742501`。
- `demo` 调用 `POST /api/admin/comments/{id}/approve` 返回 403。

浏览器验收：

- ADMIN 登录成功并进入 `/comments/list`。
- 默认列表加载成功，显示真实 MySQL 评论。
- 关键词 `Codex评论验收` 查询后，列表收敛到 5 条未删除验收评论。
- 勾选“包含已删除”后，显示 6 条验收评论，并展示已删除评论的“恢复”按钮。
- ADMIN 操作验证：
  - `9007199254742501`：待审核 -> 点击“通过” -> 状态变为“已通过”。
  - `9007199254742505`：已通过 -> 点击“隐藏” -> 状态变为“已隐藏”。
  - `9007199254742504`：点击“删除” -> 删除状态变为“已删除”，只显示“恢复”。
  - `9007199254742503`：已删除 -> 点击“恢复” -> 删除状态变为“正常”。
- DEMO 登录成功并进入 `/comments/list`：
  - 能读取评论列表。
  - 操作列不显示。
  - 操作按钮数量为 0。

## 当前遗留观察

- 直接进入 `/comments/list` 后页面内容正确，但浏览器标题仍显示为仪表盘标题；这属于后台壳层标题同步问题，不影响本轮评论管理功能链路。
- 构建仍保留上游 `baseline-browser-mapping` 与 `Browserslist/caniuse-lite` 数据过期提示，不阻断构建。
