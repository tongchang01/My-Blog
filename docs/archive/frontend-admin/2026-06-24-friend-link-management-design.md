# 后台友链管理设计

## 目标

在 Pure Admin Thin 后台中新增友链管理页面，复用 V2 后端现有友链管理接口。ADMIN 可以新增、编辑、显示/隐藏、调整排序和删除友链；DEMO 可以查看列表但不能执行写操作。

本页面只做后台管理能力，不扩展前台友链展示，也不新建后端接口。

## 后端契约

后端现有接口：

- `GET /api/admin/friend-links?page=&size=`
- `GET /api/admin/friend-links/{id}`
- `POST /api/admin/friend-links`
- `PUT /api/admin/friend-links/{id}`
- `PATCH /api/admin/friend-links/{id}/status`
- `PUT /api/admin/friend-links/sort-orders`
- `DELETE /api/admin/friend-links/{id}`

响应模型：

- `id`：字符串，避免浏览器端 Snowflake ID 精度损失。
- `name`：友链名称。
- `url`：站点地址。
- `avatarUrl`：头像或站点图标，可为空。
- `description`：说明，可为空。
- `sortOrder`：排序值，数值越小越靠前。
- `status`：`VISIBLE` / `HIDDEN`。
- `createdAt` / `updatedAt`。
- `createdBy` / `updatedBy`：字符串或空值。

写入模型要求提交完整字段：`name`、`url`、`avatarUrl`、`description`、`sortOrder`、`status`。

## 页面与菜单

友链管理作为内容管理下的独立菜单项：

- 路由：`/friend-links/list`
- 组件：`src/features/friend-links/index.vue`
- 菜单标题：`menus.friendLinkManagement`
- 角色：`ADMIN`、`DEMO`

页面结构复用现有后台风格：独立筛选卡片 + 结果表格卡片。

筛选卡片：

- 关键词：前端本地过滤名称、URL、描述。
- 状态：全部 / 显示 / 隐藏。

结果表格：

- 名称：名称、URL、头像预览。
- 描述。
- 状态。
- 排序值：ADMIN 可直接调整后批量保存。
- 更新时间。
- 操作：ADMIN 可见，DEMO 不渲染操作列。

## 操作规则

ADMIN 操作：

- 新增：打开表单弹窗，提交完整友链字段。
- 编辑：读取当前行数据填充表单，提交完整字段。
- 显示/隐藏：通过状态接口切换。
- 保存排序：只提交排序值发生变化的记录。
- 删除：二次确认后删除。

所有写操作成功后刷新当前列表。写操作失败保留页面状态并显示错误提示。DEMO 不渲染新增、编辑、状态切换、排序保存和删除入口；最终权限仍以后端为准。

## 前端结构

新增文件：

- `src/features/friend-links/model.ts`：友链类型、筛选、分页、写入模型。
- `src/features/friend-links/query.ts`：分页查询参数构造。
- `src/features/friend-links/form.ts`：表单创建、填充、校验、payload 构造。
- `src/api/friend-link.ts`：后台友链 API。
- `src/features/friend-links/useFriendLinkManagement.ts`：页面状态、筛选、分页、写操作。
- `src/features/friend-links/index.vue`：友链管理页面。

修改文件：

- `src/router/modules/articles.ts`：内容管理菜单增加友链管理。
- `src/router/static-router.test.ts`：锁定菜单路径与三语文案。
- `locales/*.yaml`：补充友链管理三语文案。
- `docs/README.md`：补充后台友链管理说明。

## 错误处理

- `10003`：无权限。
- `90001`：表单或请求字段不合法。
- `90003`：友链不存在或已删除。
- `90004`：状态冲突或重复操作。
- 其他错误：显示通用失败提示并保留当前页面状态。

## 测试与验收

- API 测试：路径、方法、分页参数、完整写入 payload、字符串 ID。
- 查询测试：分页默认值、状态 ALL 去除、关键词去空格。
- 表单测试：必填、URL、排序值、可空字段、payload 完整性。
- 状态测试：加载、筛选、分页、保存、状态切换、删除、失败保留状态。
- 页面测试：筛选卡片、表格、ADMIN 操作、DEMO 只读、确认框。
- 路由测试：菜单路径、角色、三语 key。
- 阶段验证：前端全量 test、typecheck、build；后端友链局部测试和阶段收尾完整测试。

## 暂不实现

- 后台批量删除。
- 拖拽排序。
- 图片上传选择器。
- 访问友链时的可用性检测。
- 前台友链展示页。
