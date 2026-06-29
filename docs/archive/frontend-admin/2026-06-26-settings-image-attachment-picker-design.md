# 设置图片附件选择器设计

## 目标

把现有附件选择器接入后台设置页面中的图片 URL 字段：

- 作者资料：`avatarUrl`
- 站点配置：`logoUrl`
- 站点配置：`faviconUrl`

选择附件后写入附件的 `publicUrl`，继续复用现有资料和站点配置保存接口。

## 边界

- 不改后端接口。
- 不新增数据库字段。
- 不把这些字段改成附件 ID；当前后端表结构只支持 URL。
- 不在设置页面内上传附件；上传仍通过 `/settings/attachments`。
- DEMO 仍只读，不渲染选择/清除入口。

## 页面交互

每个图片 URL 字段保留手动输入框，并增加：

- “选择附件”：打开附件选择弹窗。
- “清除”：清空当前 URL。
- 有 URL 时显示图片预览和当前 URL。

选择附件后：

- `avatarUrl/logoUrl/faviconUrl = attachment.publicUrl`
- 表单保存流程不变。

## 验证

- 作者资料页面测试：ADMIN 可选择头像附件并保存 URL；DEMO 不显示选择入口。
- 站点配置页面测试：ADMIN 可分别选择 Logo/Favicon 附件并保存 URL；DEMO 不显示选择入口。
- 完整验证：`npm test`、`npm run typecheck`、`npm run build`。
