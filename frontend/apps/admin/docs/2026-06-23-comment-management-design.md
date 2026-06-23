# 后台评论管理设计

## 目标

在现有 V2 评论后端能力基础上，为 Pure Admin Thin 后台增加评论管理页。ADMIN 可以审核通过、隐藏、软删除和恢复评论；DEMO 只能读取。后端继续作为权限和状态流转的最终边界，前端不重新定义业务规则。

## 现有后端契约

后端已经提供：

- `GET /api/admin/comments`
- `POST /api/admin/comments/{id}/approve`
- `POST /api/admin/comments/{id}/hide`
- `POST /api/admin/comments/{id}/restore`
- `DELETE /api/admin/comments/{id}`

查询参数包括 `targetType`、`targetId`、`auditStatus`、`keyword`、`includeDeleted`、`page`、`size`。接口返回 `PageResponse<AdminCommentPageItemVO>`。

本批前置修复：后台评论列表的 `id`、`targetId`、`parentId`、`replyToCommentId` 以 JSON string 输出，避免浏览器端 Snowflake ID 精度损失；同时补出 `deleted` 字段，让前端能区分软删除记录。

## 页面与菜单

评论管理作为内容管理下的独立菜单项：

- 路由：`/comments/list`
- 组件：`src/features/comments/index.vue`
- 菜单标题：`menus.commentManagement`
- 角色：`ADMIN`、`DEMO`

页面沿用当前后台风格：独立筛选卡片 + 结果表格卡片。筛选项为：

- 目标类型：全部 / 文章 / 留言板
- 目标 ID
- 审核状态：全部 / 已通过 / 待审核 / 已隐藏
- 关键词：匹配作者昵称与 Markdown 原文
- 包含已删除

表格展示：

- 内容摘要：Markdown 原文截断展示
- 作者：昵称、邮箱、站点
- 目标：文章或留言板 + 目标 ID
- 状态：审核状态标签
- 删除状态：正常 / 已删除
- 时间：创建时间
- 操作：ADMIN 可见

## 操作规则

ADMIN 操作：

- 待审核或已隐藏：可审核通过
- 已通过或待审核：可隐藏
- 未删除：可删除
- 已删除：可恢复

所有写操作都需要二次确认。操作成功后刷新当前页；如果当前页为空且页码大于 1，则回退上一页再刷新。DEMO 不渲染操作列。

## 前端结构

新增文件：

- `src/features/comments/model.ts`：评论类型、筛选类型、分页类型。
- `src/features/comments/query.ts`：筛选条件序列化。
- `src/api/comment.ts`：评论后台接口。
- `src/features/comments/useCommentManagement.ts`：页面状态、加载、筛选、分页、操作。
- `src/features/comments/index.vue`：评论管理页面。

修改文件：

- `src/router/modules/articles.ts`：继续复用内容管理菜单组，增加评论管理子路由。
- `src/router/static-router.test.ts`：锁定菜单与三语文案。
- `locales/*.yaml`：补充评论管理三语文案。
- `docs/README.md`：补充后台能力说明。

## 错误处理

- `10003`：无权限。
- `90003`：评论不存在或状态已变化。
- `90004`：状态冲突或重复操作。
- 其他错误：保留当前页面状态并显示通用失败提示。

## 测试与验收

- API 测试：路径、方法、参数与 ID 字符串模型。
- 查询测试：筛选序列化。
- 状态测试：加载、筛选、分页、尾页回退、写操作成功与失败。
- 页面测试：筛选卡片、表格、ADMIN 操作、DEMO 只读、确认框。
- 路由测试：菜单可见性和三语键存在。
- 阶段验证：前端全量 test、typecheck、build；后端评论局部测试与阶段结束 `mvn clean test`。

## 暂不实现

- 批量审核、批量删除、批量恢复。
- 评论详情抽屉。
- 按文章标题反查评论。
- 前台评论提交页面。
