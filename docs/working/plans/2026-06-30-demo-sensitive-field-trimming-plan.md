# DEMO 敏感字段裁剪实施思路

> 状态：方案已定 / 实现待设计。本文记录 O-002 的实施边界。

## 目标

DEMO 是后台演示账号，可以查看后台整体效果和只读数据，但不能读取敏感内容，也不能执行任何写操作。

前端可以隐藏按钮、禁用表单来改善体验，但安全边界必须在后端完成。

## 已定规则

### DEMO 可以查看

- Dashboard 统计：PV / UV、趋势、TOP 文章、语言分布。
- 文章列表：标题、摘要、分类、标签、状态、发布时间、评论数等列表信息。
- 分类、标签、友链、站点配置、作者资料、附件列表。
- 评论管理列表：评论内容、审核状态、目标类型、目标 ID、创建时间等管理视图。

### DEMO 必须裁剪

- 后台文章详情：
  - `PUBLISHED` 文章：允许返回 `body`。
  - `DRAFT / PRIVATE / PASSWORD / SCHEDULED` 文章：`body` 固定返回 `null`。
- 后台评论列表：
  - `authorEmail` 固定返回 `null`。
  - `authorIp` 固定返回 `null`。
  - `authorUserAgent` 固定返回 `null`。
- 密码相关：
  - 文章密码明文、密码 hash 不得在任何查询响应返回；ADMIN 也不返回。
- 附件：
  - DEMO 可以查看公开 URL、文件名、内容类型、大小、图片尺寸等管理信息。
  - 不得暴露本地磁盘路径、对象存储密钥、bucket 私密配置或内部存储凭证。

### DEMO 不允许执行

- 新增、修改、删除、恢复、审核、隐藏、回复、上传、发布等所有后台写操作。
- 后端写接口必须返回 `403 + 10003`，不能只依赖前端隐藏按钮。

## 三端变更范围

### 后端

- 在后台文章详情查询结果映射处根据当前用户角色裁剪 `body`。
- 在后台评论查询结果映射处根据当前用户角色裁剪 `authorEmail`、`authorIp`、`authorUserAgent`。
- 确认后台附件响应不包含本地磁盘路径、对象存储密钥、bucket 私密配置。
- 保持所有后台写接口 ADMIN-only，DEMO 请求返回 `403 + 10003`。
- API 文档同步标注 ADMIN / DEMO 字段差异。

### 后台前端

- 继续保留 DEMO 只读体验：隐藏或禁用写按钮、上传入口和保存入口。
- 不把前端只读控制当作安全边界。
- 对 `body = null`、评论审计字段为 `null` 的情况保持可用显示。

### 文档

- 更新 `docs/handbook/api/article.md` 的后台文章详情 DEMO 裁剪说明。
- 更新 `docs/handbook/api/comment.md` 的后台评论列表 DEMO 裁剪说明。
- 如附件 API 文档列出存储字段，同步确认 DEMO 不暴露内部存储信息。

## 验证建议

- 后端测试：DEMO 读取 `PUBLISHED` 文章详情时返回正文。
- 后端测试：DEMO 读取 `DRAFT / PRIVATE / PASSWORD / SCHEDULED` 文章详情时 `body = null`。
- 后端测试：DEMO 查询后台评论列表时 `authorEmail`、`authorIp`、`authorUserAgent` 均为 `null`。
- 后端测试：ADMIN 查询后台评论列表时审计字段按实际数据返回。
- 后端测试：DEMO 调用后台写接口返回 `403 + 10003`。
- 前端测试：DEMO 页面保持只读，且能渲染被裁剪字段。
