# 后台文章生命周期管理验收记录

验收日期：2026-06-22

## 自动化验证

- 后端局部验证：`AdminArticleControllerTest`、`ArticleOpenApiTest`、`ArticleIntegrationTest` 共 8 项通过。
- 后端完整验证：`mvn clean test` 共 641 项，0 failure、0 error、4 skipped。
- 前端完整验证：24 个测试文件、86 项测试全部通过。
- 前端 `typecheck` 通过。
- 前端生产构建通过，产物写入 `frontend/apps/admin/dist/`。
- 构建仅保留既有的 Browserslist / baseline-browser-mapping 数据过期提示，不阻断产物生成。

## 本地 MySQL 浏览器联调

环境：本地 `myblog_v2_dev`、Spring Boot local profile、Admin 开发服务器。

使用唯一临时 slug `codex-lifecycle-20260622-1915` 验证：

1. ADMIN 新建临时草稿，活动文章数由 5 变为 6。
2. 在文章列表二次确认删除，临时文章从活动列表消失。
3. 回收站显示临时文章及删除时间、删除人，并提供恢复操作。
4. 二次确认恢复后，临时文章重新出现在活动文章列表。
5. 再次软删除临时文章。
6. DEMO 可访问 `/articles/recycle-bin`，页面不渲染恢复按钮。

## 数据清理

联调完成后按临时 slug 定位并清理该文章及其可能的文章标签、评论和访问统计记录。清理后：

- 临时 slug 记录数为 0。
- 原有活动文章仍为 5 篇。
- 原有已删除文章仍为 1 篇。

未修改其他文章、分类、标签或既有回收站记录。
