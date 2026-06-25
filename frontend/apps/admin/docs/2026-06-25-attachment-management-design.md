# 后台附件管理设计

## 目标

在现有 V2 附件后端能力基础上，为 Pure Admin Thin 后台增加附件库页面。ADMIN 可以上传图片并浏览附件，DEMO 只能浏览。附件库先作为独立后台能力落地，后续再接入文章封面、作者头像、站点 Logo、Favicon 和友链头像选择器。

## 后端契约

复用现有接口：

- `POST /api/admin/attachments`：上传图片，`multipart/form-data`，文件字段为 `file`。
- `GET /api/admin/attachments?page=1&size=20`：分页查询 active 附件。
- `GET /api/admin/attachments/{id}`：查询单个 active 附件。

本轮不新增删除、回收站、批量操作或物理清理接口。

前置修复：附件响应里的 `id` 和 `createdBy` 改为字符串输出，并在 OpenAPI 上标注 `int64` 格式。原因是附件 ID 可能是 Snowflake 大整数，浏览器端不能按 number 可靠承载；文章、评论、友链后台契约已经采用同类处理。

## 页面与菜单

附件管理放在系统管理下：

- 路由：`/settings/attachments`
- 组件：`src/features/attachments/index.vue`
- 菜单标题：`menus.attachmentManagement`
- 角色：`ADMIN`、`DEMO`

页面结构沿用现有后台风格：

- 顶部上传卡片：ADMIN 显示上传入口，DEMO 显示只读提示。
- 结果卡片：展示附件网格/卡片列表、刷新、分页。
- 附件卡片展示图片预览、原始文件名、尺寸、大小、MIME、上传时间、ID 和公开 URL。
- 操作：复制公开 URL、打开公开 URL。复制失败时回退到选中文本或显示错误提示。

## 前端结构

新增文件：

- `src/features/attachments/model.ts`：附件响应和分页类型。
- `src/api/attachment.ts`：附件后台 API 封装。
- `src/features/attachments/useAttachmentManagement.ts`：页面状态、分页、上传和错误状态。
- `src/features/attachments/index.vue`：附件管理页面。

修改文件：

- `src/router/modules/settings.ts`：增加附件管理路由。
- `src/router/static-router.test.ts`：锁定菜单与三语文案。
- `locales/*.yaml`：补齐三语文案。
- `docs/README.md`：补充附件管理验收记录。

## 权限与错误处理

- DEMO 不渲染上传入口；后端仍是最终权限边界。
- 上传失败保持当前列表和分页状态，显示上传错误。
- 列表加载失败显示错误与重试入口。
- 上传成功后刷新第一页，确保新附件可见。

## 测试

- 后端：附件 Controller 与 OpenAPI 测试锁定 `id/createdBy` 字符串契约。
- 前端 API 测试：锁定 list/detail/upload 路径、分页参数和 multipart 字段名。
- 前端状态测试：覆盖初始化、分页、上传成功刷新第一页、上传失败保留状态。
- 页面测试：覆盖 ADMIN 上传入口、DEMO 只读、列表卡片、复制 URL。
- 路由测试：覆盖系统管理下的附件菜单和三语文案。
