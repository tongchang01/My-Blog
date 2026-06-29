# 后台附件管理实施计划

## 拆分

1. 后端附件响应 ID 字符串契约。
2. 前端附件 API 与状态模型。
3. 前端附件管理页面。
4. 路由、三语文案与文档验收。

## 小任务

### 1. 后端附件响应契约

- 修改 `AttachmentVO`：`id`、`createdBy` 改为 `String`，`from` 中显式 `Long.toString`。
- 更新 `AdminAttachmentControllerTest`，断言 JSON 字符串 ID。
- 更新 `AttachmentOpenApiTest`，断言 `id`、`createdBy` schema 为 string/int64。
- 更新 `docs/project-handbook/api-contract/attachment.md` 示例。
- 验证：`mvn -Dtest=AdminAttachmentControllerTest,AttachmentOpenApiTest test`。
- 提交：`修复附件响应ID字符串契约`。

### 2. 前端附件 API 与状态

- 新增 `src/features/attachments/model.ts`。
- 新增 `src/api/attachment.ts`。
- 新增 `src/api/attachment.test.ts`。
- 新增 `src/features/attachments/useAttachmentManagement.ts` 与测试。
- 验证：`npm test -- attachment useAttachmentManagement`。
- 提交：`实现附件接口与管理状态`。

### 3. 前端附件管理页面

- 新增 `src/features/attachments/index.vue`。
- 新增 `src/features/attachments/index.test.ts`。
- ADMIN 渲染上传入口，DEMO 只读。
- 展示附件图片、文件名、尺寸、大小、MIME、上传时间、ID、URL。
- 验证：`npm test -- attachments/index useAttachmentManagement`。
- 提交：`实现附件管理页面`。

### 4. 路由文案文档与完整验证

- 修改 `src/router/modules/settings.ts`。
- 修改 `src/router/static-router.test.ts`。
- 修改 `locales/en.yaml`、`locales/zh-CN.yaml`、`locales/ja.yaml`。
- 修改 `frontend/apps/admin/docs/README.md`。
- 验证：
  - `npm test`
  - `npm run typecheck`
  - `npm run build`
  - 后端附件局部测试。
- 提交：`接入附件管理菜单与验收文档`。
