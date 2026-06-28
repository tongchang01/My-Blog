# 后台评论回复工作流审阅与补齐计划

> 日期：2026-06-28  
> 分支：`feature/admin-phase-2`  
> 状态：待人工审阅  
> 结论：评论回复基础能力已经存在，不建议从零重做。下一步只做复核和必要小修。

## 1. 当前已存在的能力

当前分支已经包含评论回复工作流的主干代码：

- 后端接口：`POST /api/admin/comments/{id}/reply`
- 后端服务：`AdminCommentCommandService.reply(...)`
- 后端请求/响应对象：`AdminCommentReplyRequest`、`AdminCommentReplyVO`
- 后台 API：`replyComment(id, contentMd)`
- 后台状态：回复弹窗、回复目标、回复内容、提交状态
- 后台 UI：评论列表“回复”按钮、回复弹窗、`回复 @昵称` 辅助展示
- 验证记录：`docs/superpowers/reviews/2026-06-27-admin-comment-reply-verification.md`

所以这里不再规划“大开发”，只规划一次小范围审阅。

## 2. 建议审阅目标

这次审阅只确认四件事：

1. 管理员能回复已通过、未删除的评论。
2. 回复作为正式评论写入，不新建回复表。
3. 前台评论查询能自然展示后台回复。
4. DEMO 或非管理员不能发回复。

不做：

- 不做无限层级后台树形管理。
- 不做访客之间互相回复的新 UI。
- 不做邮件通知生产化。
- 不做站内消息系统。
- 不做富文本编辑器。

## 3. 已发现的待确认点

### 3.1 站长身份字段

旧设计写的是：站长回复昵称固定为 `站长`，邮箱和站点为空。

当前实现是：

- 昵称：`站长`
- 邮箱：`admin@myblog.local`
- 站点：空

建议你确认：

- 如果这个邮箱只用于后台/前台展示，建议改为空，和旧设计一致。
- 如果未来要靠邮箱生成头像或标识，可以保留，但要在文档里明确它是系统占位邮箱。

我的倾向：先改为空。少一个假邮箱，少一个后续解释成本。

### 3.2 回复内容校验

当前接口请求体用了 `@Valid`，前端也会阻止空内容提交。

建议复核：

- 空白内容是否稳定返回参数错误。
- 超长 Markdown 是否沿用评论内容领域校验。

如果测试已覆盖，就不改。

### 3.3 前台展示

后台回复是正式评论，理论上会进入现有前台评论查询。

建议只做一次浏览器级人工验收：

- 后台回复文章评论。
- 打开前台文章页。
- 确认能看到“站长”回复。
- 确认回复挂在正确的评论目标下。

不建议现在改成复杂树形 UI。先确认数据链路通。

## 4. 最小执行计划

### 任务 1：代码审阅

检查这些文件即可：

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/application/AdminCommentCommandService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/AdminCommentController.java`
- `frontend/apps/admin/src/api/comment.ts`
- `frontend/apps/admin/src/features/comments/useCommentManagement.ts`
- `frontend/apps/admin/src/features/comments/index.vue`

输出：

- 不改代码，或者只列出必须小修点。

### 任务 2：定向测试

运行：

```powershell
cd E:\My-Blog\MyBlog-springboot-v2
& 'D:\apache-maven-3.9.16\bin\mvn.cmd' '-Dtest=AdminCommentModerationServiceTest,AdminCommentControllerTest,CommentOpenApiTest' test

cd E:\My-Blog\frontend\apps\admin
pnpm exec vitest run src/api/comment.test.ts src/features/comments/useCommentManagement.test.ts src/features/comments/index.test.ts
pnpm run typecheck
```

输出：

- 如果全部通过，只记录结果。
- 如果失败，先定位根因，不顺手改无关代码。

### 任务 3：人工验收

本地启动：

- 后端：local profile + `myblog_v2_dev`
- 后台：`frontend/apps/admin`
- 前台：`frontend/apps/blog`

验收路径：

1. 后台登录 ADMIN。
2. 找一条 `PASS` 评论。
3. 点击回复，输入短 Markdown。
4. 提交后确认列表刷新。
5. 前台打开对应文章或留言页确认回复可见。

输出：

- 通过：记录到 review 文档。
- 不通过：只修阻断项。

## 5. 如果要小修，提交拆分

最多三类提交，不混在一起：

1. `修正后台评论回复站长身份字段`
2. `补充后台评论回复验证`
3. `记录后台评论回复人工验收结果`

如果审阅没有发现问题，只提交第 3 个文档提交即可。

## 6. 审阅结论待填

- [ ] 确认站长邮箱策略：空 / 保留 `admin@myblog.local`
- [ ] 定向测试通过
- [ ] 前台可见性验收通过
- [ ] 是否需要小修：是 / 否
