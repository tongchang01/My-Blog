# 后台评论回复工作流设计

> 日期：2026-06-27  
> 分支：`feature/admin-comment-reply`  
> 范围：后台管理端发起站长回复，并复用现有评论模型进入评论列表与前台数据流。

## 1. 目标

补齐后台评论管理的互动闭环：管理员在后台评论列表中选择一条已通过且未删除的评论，输入 Markdown 文本并发布站长回复。回复保存为一条新的评论记录，沿用现有 `parent_id`、`reply_to_comment_id`、`reply_to_nickname`、`created_by` 等字段，不引入新的回复表。

第一版只支持后台管理员回复，不开放访客互相回复的新 UI；前台已有评论查询接口后续可以自然展示这类回复。

## 2. 非目标

- 不做无限层级树形后台管理。
- 不做批量回复。
- 不做评论富文本编辑器。
- 不做邮件通知生产化增强。
- 不做用户中心消息系统。
- 不改变现有游客提交评论接口契约。

## 3. 后端设计

新增后台回复接口：

```http
POST /api/admin/comments/{id}/reply
```

请求体：

```json
{
  "contentMd": "感谢反馈，我已经处理。"
}
```

响应：

```json
{
  "code": 0,
  "message": "OK",
  "data": {
    "id": "2070000000000000001",
    "auditStatus": "PASS"
  }
}
```

服务层新增 `AdminCommentReplyService`，职责只包含后台回复命令：

- 校验当前用户权限沿用 `CommentAuthorization.requireAdmin` 或同等现有权限入口。
- 查询被回复评论，要求存在、未删除、审核状态为 `PASS`。
- 回复目标必须沿用被回复评论的 `targetType` 与 `targetId`。
- `parentId` 使用被回复评论的根评论 ID；如果被回复评论本身已经是回复，则继续挂到同一个根评论下。
- `replyToCommentId` 指向被回复评论 ID。
- `replyToUserId` 和 `replyToNickname` 来自被回复评论作者。
- 作者信息使用站点身份，第一版固定昵称为 `站长`，邮箱和站点为空。
- `createdBy` 写入当前登录用户 ID。
- `auditStatus` 固定为 `PASS`。
- `contentHtml` 复用现有 `CommentMarkdownRenderer`。
- 如果目标是文章评论，插入成功后同步增加文章评论数。
- 插入后发布现有 `CommentNotificationEvent`，但只在回复目标公开可见时发布。

为避免扩大游客创建评论路径，后台回复不复用 `CommentCreateService.createArticleComment` 的限流、重复检测和审核策略入口；它复用底层 `CommentRepository.insert(NewComment)`、Markdown 渲染和通知事件。

## 4. 后台管理端设计

评论列表新增“回复”操作按钮：

- 仅 ADMIN 可见；DEMO 权限沿用后端拒绝策略，前端可按 `isAdmin` 隐藏。
- 仅未删除且审核状态为 `PASS` 的评论可回复。
- 点击后打开弹窗，展示被回复人昵称和评论摘要。
- 输入框使用多行文本，提交字段为 `contentMd`。
- 内容为空或全空白时前端禁止提交。
- 提交成功后关闭弹窗并刷新当前列表。
- 提交失败时在页面顶部现有操作错误区域显示错误。

新增 API 函数：

```ts
replyComment(id: string, contentMd: string): Promise<ApiResponse<CommentReplyResponse>>
```

`useCommentManagement` 新增状态：

- `replyDialogVisible`
- `replyTarget`
- `replyContent`
- `replySubmitting`

新增行为：

- `openReplyDialog(item)`
- `closeReplyDialog()`
- `submitReply()`

## 5. 数据展示

评论列表现有字段已经包含 `replyToNickname`、`parentId`、`replyToCommentId` 时，后台列表展示一行辅助信息：

```text
回复 @昵称
```

如果是根评论则不显示该行。第一版不做树形缩进，避免影响现有分页和筛选逻辑。

## 6. 错误处理

后端错误：

- 被回复评论不存在：`NOT_FOUND`
- 被回复评论已删除、未通过审核或目标不可回复：`CONFLICT`
- 内容为空或超长：沿用评论内容领域校验
- 非管理员：沿用现有权限错误

前端错误：

- 空内容：阻止提交，并给出表单校验提示。
- 接口失败：复用 `operationError`，不关闭弹窗。
- 刷新失败：保留现有 `error` 行为。

## 7. 测试策略

后端：

- `AdminCommentReplyServiceTest` 覆盖成功回复、回复隐藏评论失败、回复删除评论失败、回复文章评论时评论数增加。
- `AdminCommentControllerTest` 覆盖 `POST /api/admin/comments/{id}/reply` 请求与权限入口。
- OpenAPI 测试覆盖新增接口。

后台：

- `api/comment.test.ts` 覆盖请求路径、方法和 body。
- `useCommentManagement.test.ts` 覆盖打开弹窗、空内容拒绝、提交成功刷新、提交失败保留弹窗。
- `comments/index.test.ts` 覆盖回复按钮可见性、弹窗提交、回复对象展示。

## 8. 提交拆分

1. 设计文档提交：`设计后台评论回复工作流`
2. 后端回复服务与接口提交：`接入后台评论回复接口`
3. 后台 API 与状态管理提交：`接入后台评论回复状态`
4. 后台评论回复 UI 提交：`实现后台评论回复弹窗`
5. 阶段验证记录或文档更新提交：`记录评论回复验证结果`
