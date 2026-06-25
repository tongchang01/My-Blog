# 文章封面附件选择器设计

## 目标

把后台附件库接入文章编辑器，让 ADMIN 在新建或编辑文章时可以从已有附件中选择封面。选择后写入现有 `coverAttachmentId` 字段，保存文章时继续复用当前 `POST/PUT /api/admin/articles` 契约。

## 边界

- 不新增后端接口。
- 不在选择器内上传附件；上传仍通过 `/settings/attachments` 完成。
- 不做删除、裁剪、批量选择或媒体分类。
- 不改变文章保存 payload，只填充已有 `coverAttachmentId`。

## 交互

文章编辑器发布设置区增加封面区域：

- 未选择时显示“未选择封面”。
- 已选择时展示附件图片预览、文件名、尺寸和 ID。
- “选择封面”打开附件选择弹窗。
- “清除封面”把 `coverAttachmentId` 置空并清除本地预览。

弹窗复用附件分页接口：

- `GET /api/admin/attachments?page=1&size=20`
- 展示附件图片网格。
- 点击“选择”后关闭弹窗，并把 `attachment.id` 写入文章表单。

编辑已有文章时：

- 如果详情返回 `coverAttachmentId` 和 `coverUrl`，页面先显示已有封面预览。
- 打开弹窗选择新附件后，用新附件替换预览。

## 结构

新增可复用组件：

- `src/features/attachments/AttachmentPickerDialog.vue`

修改文章编辑器：

- `src/features/articles/editor/form.ts`：表单增加只读展示字段 `coverUrl`。
- `src/features/articles/editor/index.vue`：接入选择器与封面预览。
- `src/features/articles/editor/index.test.ts`：覆盖选择封面、清除封面、编辑态已有封面。

## 验证

- 附件选择器组件测试：分页加载、选择附件。
- 文章编辑器测试：选择后保存 payload 包含 `coverAttachmentId`，清除后 payload 为 `null`。
- 完整前端验证：`npm test`、`npm run typecheck`、`npm run build`。
