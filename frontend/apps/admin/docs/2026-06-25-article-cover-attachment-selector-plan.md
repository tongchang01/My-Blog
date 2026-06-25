# 文章封面附件选择器实施计划

## 小任务

### 1. 文档

- 新增设计与计划文档。
- 提交：`规划文章封面附件选择器`。

### 2. 表单模型

- 修改 `ArticleForm`，增加 `coverUrl: string | null`。
- `createEmptyArticleForm` 默认 `coverUrl = null`。
- `articleDetailToForm` 从文章详情写入 `coverUrl`。
- `articleFormToPayload` 不输出 `coverUrl`。
- 先补 `form.test.ts`，验证编辑态保留封面预览 URL、payload 只发送附件 ID。
- 提交：`扩展文章表单封面预览模型`。

### 3. 附件选择器组件

- 新增 `AttachmentPickerDialog.vue`。
- 复用 `useAttachmentManagement`。
- props：`modelValue: boolean`。
- emits：`update:modelValue`、`select`。
- 选择时发出完整 `AttachmentItem`。
- 测试覆盖加载附件、选择附件、分页变更。
- 提交：`实现附件选择器组件`。

### 4. 接入文章编辑器

- 在发布设置卡片增加封面预览区。
- 打开 `AttachmentPickerDialog`。
- 选择附件后设置 `form.coverAttachmentId` 和 `form.coverUrl`。
- 清除封面时两个字段都清空。
- 更新文章编辑器测试。
- 提交：`接入文章封面附件选择`。

### 5. 文案、文档和完整验证

- 补齐三语文案。
- 更新 `frontend/apps/admin/docs/README.md`。
- 验证：
  - `npm test`
  - `npm run typecheck`
  - `npm run build`
- 提交：`记录文章封面选择器验收结果`。
