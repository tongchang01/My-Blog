# 设置图片附件选择器实施计划

## 小任务

### 1. 文档

- 新增设计与计划文档。
- 提交：`规划设置图片附件选择器`。

### 2. 作者资料头像选择

- 修改 `profile/index.test.ts`，先写失败测试：
  - ADMIN 点击头像选择按钮后，从附件弹窗选中图片。
  - 保存时 `PATCH /api/auth/me/profile` payload 包含 `avatarUrl = attachment.publicUrl`。
  - DEMO 不显示头像选择按钮。
- 修改 `profile/index.vue`，接入 `AttachmentPickerDialog`。
- 验证：`npm test -- profile/index AttachmentPickerDialog`。
- 提交：`接入作者头像附件选择`。

### 3. 站点 Logo/Favicon 选择

- 修改 `site-config/index.test.ts`，先写失败测试：
  - ADMIN 可选择 Logo 和 Favicon。
  - 保存时 `PUT /api/admin/site-config` payload 包含对应 URL。
  - DEMO 不显示选择按钮。
- 修改 `site-config/index.vue`，接入 `AttachmentPickerDialog`。
- 验证：`npm test -- site-config/index AttachmentPickerDialog`。
- 提交：`接入站点图片附件选择`。

### 4. 文案、README 与完整验证

- 补齐中/英/日文案。
- 更新 `frontend/apps/admin/docs/README.md`。
- 运行：
  - `npm test`
  - `npm run typecheck`
  - `npm run build`
- 提交：`记录设置图片选择器验收结果`。
