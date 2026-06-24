# 后台友链管理验收记录

日期：2026-06-24

## 自动化验证

后端：

- `mvn "-Dtest=AdminFriendLinkControllerTest,FriendLinkOpenApiTest,PublicFriendLinkControllerTest,FriendLinkIntegrationTest" test`
  - 11 tests，0 failures，0 errors。
- `mvn clean test`
  - 641 tests，0 failures，0 errors，4 skipped。

前端：

- `corepack pnpm --dir frontend/apps/admin test`
  - 33 files，115 tests，全部通过。
- `corepack pnpm --dir frontend/apps/admin typecheck`
  - 通过。
- `corepack pnpm --dir frontend/apps/admin build`
  - 通过。
  - 保留既有 `baseline-browser-mapping` 与 Browserslist 数据过期提示，不阻断构建。

## 本地 MySQL 数据

测试库：`myblog_v2_dev`

保留了三条友链验收数据，便于本机继续打开页面验证：

- `9007199254742601`：`Codex Friend Link Visible`，`VISIBLE`，排序 `5`。
- `9007199254742602`：`Codex Friend Link Hidden`，`HIDDEN`，排序 `15`。
- `9007199254742603`：`Codex Friend Link Sort`，`VISIBLE`，排序 `25`。

说明：MySQL CLI 写入中文时出现本机控制台编码问题，因此验收数据名称和描述使用 ASCII，避免页面显示乱码。

## 浏览器联调

本地服务：

- 后端：`http://127.0.0.1:8080`
- 后台前端：`http://127.0.0.1:8848`

DEMO 验证：

- 使用 `demo` 登录后可进入 `/friend-links/list`。
- 菜单显示 `友链管理`。
- 列表能看到 `Codex Friend Link` 验收数据。
- 不显示 `新增友链`、`保存排序`、删除等写操作入口。

ADMIN 验证：

- 使用 `admin` 登录后可进入 `/friend-links/list`。
- 显示 `新增友链`、`保存排序`、编辑、显示/隐藏、删除等写操作入口。
- 对 `Codex Friend Link Visible` 执行一次“隐藏”操作，确认弹窗后接口成功，页面状态从“显示”变为“隐藏”，按钮从“隐藏”变为“显示”。
- 联调结束后已将该记录恢复为 `VISIBLE`，排序恢复为 `5`。

## 服务清理

联调结束后已停止本次启动的 8080 与 8848 监听进程。
