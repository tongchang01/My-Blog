# 前台文章评论迁移 V2 实施计划

> 状态：已完成
> 适用范围：O-019 评论和留言迁移到 V2 自研 API 的第一批，前台 blog 文章评论
> 最后校准：2026-07-06

## 目标

把前台文章详情页评论从 Gitalk / Valine / Twikoo / Waline 插件迁移到 V2 自研公开评论 API，并尽量复用当前评论区视觉：

- 保留 `Comment.vue` 作为文章评论组件入口。
- 复用当前评论区外层卡片、标题、深色底、圆角、头像形状和渐变按钮风格。
- 接入 `GET /api/public/articles/{articleId}/comments`。
- 接入 `POST /api/public/articles/{articleId}/comments`。
- 支持根评论、回复、分页、提交成功提示、待审核提示和错误态。
- 不再在文章评论主链路初始化第三方评论插件。

## 不做范围

- 本批不做留言板评论。留言板使用同一套组件和 store 的目标抽象，等文章评论验收后再接。
- 本批不做友链评论。
- 本批不做最近评论侧栏。既有裁决是移除，不规划 V2 最近评论接口。
- 本批不恢复第三方评论插件。
- 本批不做 PASSWORD 文章评论。PASSWORD 评论等 O-001 Article Access Token 完成后再接。
- 本批不新增前端依赖。

## 现状结论

- 后端公开评论 API 已存在，公开评论 ID、父评论 ID、回复目标 ID 是 JSON string 或 `null`。
- `docs/handbook/api/comment.md` 的提交请求表格里 `replyToCommentId` 仍写 `number/null`，这和同文档的 ID string 约定冲突。本批前端按 `string/null` 处理，并同步修正文档。
- `frontend/apps/blog/src/components/Comment.vue` 当前外层 UI 可复用，但内部全是第三方插件初始化和第三方 DOM 容器。
- 文章详情页 `frontend/apps/blog/src/pages/post/[slug].vue` 当前没有挂载 `Comment.vue`。
- `frontend/apps/blog/src/components/PageContent.vue` 和旧 page/link 页面仍通过 `useCommentPlugin()` 判断评论插件是否启用；本批只处理文章详情页，不顺手迁移旧 page/link。

## 文件边界

### 新增

- `frontend/apps/blog/src/features/comments/contract.ts`
  - 定义 `PublicCommentDto`、`CreateCommentPayload`、`CreateCommentResultDto`。
- `frontend/apps/blog/src/features/comments/model.ts`
  - 定义前台评论 view model、表单状态、列表状态。
- `frontend/apps/blog/src/features/comments/api.ts`
  - 封装文章评论列表和提交 API。
- `frontend/apps/blog/src/features/comments/mapper.ts`
  - 把公开评论 DTO 映射为组件可直接渲染的数据。
- `frontend/apps/blog/src/features/comments/store.ts`
  - 管理当前文章评论列表、分页、加载、提交、回复目标和错误。
- `frontend/apps/blog/src/features/comments/*.test.ts`
  - 覆盖 API、mapper、store。

### 修改

- `frontend/apps/blog/src/components/Comment.vue`
  - 删除第三方插件初始化逻辑。
  - 保留外层标题和卡片 class。
  - 改为渲染 V2 评论列表、回复列表和提交表单。
- `frontend/apps/blog/src/pages/post/[slug].vue`
  - 在文章正文后挂载 `Comment.vue`。
  - 只在 `detailStatus === 'ready'` 且文章未锁定时展示。
- `frontend/apps/blog/src/components/PageContent.vue`
  - 第一批不主动接入，但需要避免新增依赖旧 `useCommentPlugin` 的代码。
- `frontend/apps/blog/src/hooks/useCommentPlugin.ts`
  - 本批不删除。它仍被 `RecentComment.vue`、旧 page/link 页面引用；删除会扩大范围。
- `docs/handbook/api/comment.md`
  - 修正 `replyToCommentId` 类型为 `string/null`。
- `docs/handbook/start-here/open-issues.md`
  - 本批完成后把 O-019 更新为“文章评论已接入，留言板待接入”。
- `docs/handbook/frontend/blog/integration-status.md`
  - 本批完成后记录文章评论接入状态。

## UI 复用规则

- 保留 `MainTitle` 标题，继续使用 `titles.comment`。
- 保留外层容器：
  - `bg-ob-deep-800 p-4 mt-8 lg:px-14 lg:py-10 rounded-2xl shadow-xl mb-8 lg:mb-0`
  - `comment-${profile_shape}-avatar`
- 输入区复用 Waline 风格：
  - 编辑框使用 `bg-ob-deep-800`、聚焦提升透明度。
  - 昵称、邮箱、站点输入使用深色小输入框。
  - 提交按钮使用 `var(--main-gradient)`。
- 评论卡片复用 Waline 风格：
  - 根评论和回复卡片使用 `bg-ob-deep-900 p-4 rounded-lg`。
  - 作者名使用 `var(--text-sub-accent)` 或 `var(--text-accent)`。
  - 头像按 `profile_shape` 切换圆形、圆角、菱形。
- 评论正文只渲染后端返回的 `contentHtml`，不在前端自行渲染 Markdown。

## 数据流

1. 文章详情页加载成功后，把 `article.id` 传给 `Comment.vue`。
2. `Comment.vue` 调用 comments store 的 `load(articleId, 1)`。
3. store 调 `GET /public/articles/{articleId}/comments?page=1&size=20`。
4. mapper 保留 string ID，递归映射 `replies`。
5. 用户提交根评论时，payload 为：

```json
{
  "nickname": "TYB",
  "email": "tyb@example.com",
  "site": null,
  "contentMd": "hello",
  "replyToCommentId": null
}
```

6. 用户回复评论时，`replyToCommentId` 使用被回复评论的 string ID。
7. 提交成功后：
   - `auditStatus === "PASS"`：刷新当前第一页或当前页，显示新评论。
   - `auditStatus === "PENDING"`：显示“评论已提交，等待审核”，不强行插入列表。

## 实施拆分

### 1. comments API 和 mapper

- 新增 `contract.ts`、`api.ts`、`mapper.ts`、`model.ts`。
- API 使用现有 `requestApi`，路径不要带 `/api` 前缀。
- `loadArticleComments(articleId, { page, size, signal })` 返回 `PageResponse<PublicCommentDto>`。
- `createArticleComment(articleId, payload)` 返回 `CreateCommentResultDto`。
- mapper 只做字段整理和默认值处理，不格式化时间。

验证：

- `pnpm --dir frontend/apps/blog test -- src/features/comments/api.test.ts src/features/comments/mapper.test.ts`

提交：

- `添加前台评论API和映射`

### 2. comments store

- 新增 Pinia store，管理：
  - `comments`
  - `page`
  - `size`
  - `total`
  - `pages`
  - `status: idle/loading/ready/empty/error/submitting`
  - `error`
  - `replyTarget`
  - `notice`
- 支持：
  - 加载第一页
  - 切换分页
  - 设置/清除回复目标
  - 提交评论
  - 提交后刷新列表
- 不做本地持久化昵称/邮箱；这是体验增强，首批先不加。

验证：

- `pnpm --dir frontend/apps/blog test -- src/features/comments/store.test.ts`

提交：

- `添加前台评论状态管理`

### 3. Comment.vue 替换第三方插件主链路

- 保留组件名和外层视觉。
- props 改为：
  - `articleId: string`
  - `enabled: boolean`
- 删除 `gitalk-container`、`vcomments`、`tcomment`、`waline` 容器。
- 删除第三方插件 imports 和 `usePostStore` 缓存逻辑。
- 渲染：
  - loading 骨架
  - empty 状态
  - error + retry
  - 评论列表
  - 回复列表
  - 分页按钮
  - 评论表单
  - 待审核/成功提示
- 表单最小字段：
  - 昵称
  - 邮箱
  - 站点
  - 内容

验证：

- 用源码级或 store 级测试保证 `Comment.vue` 不再包含第三方插件初始化字符串。
- `pnpm --dir frontend/apps/blog test -- src/components/Comment.test.ts`

提交：

- `替换前台评论组件为V2实现`

### 4. 文章详情页挂载评论

- 修改 `frontend/apps/blog/src/pages/post/[slug].vue`：
  - 在正文后加 `<div id="comments"><Comment :article-id="article.id" :enabled="!article.locked" /></div>`。
  - 导入 `Comment.vue`。
  - 不再依赖 `useCommentPlugin()` 控制文章评论展示。
- 文章 `locked` 状态不挂载评论；PASSWORD 评论等 O-001。
- 顶部 `commentCount` 继续使用文章详情接口字段，不在评论提交后强行本地改数。

验证：

- `pnpm --dir frontend/apps/blog typecheck`
- `pnpm --dir frontend/apps/blog test`

提交：

- `接入文章详情评论区`

### 5. 文档收口

- `docs/handbook/api/comment.md` 修正 `replyToCommentId` 类型为 `string/null`。
- `docs/handbook/start-here/open-issues.md` 更新 O-019：
  - 文章评论已接入。
  - 留言板仍未完成。
  - 第三方评论插件主链路已下线。
- `docs/handbook/frontend/blog/integration-status.md` 更新前台评论状态。
- 本计划状态改为“已完成”并补验证结果。

验证：

- `git diff --check`

提交：

- `更新文章评论接入文档状态`

## 验收命令

- `pnpm --dir frontend/apps/blog test`
- `pnpm --dir frontend/apps/blog typecheck`
- `pnpm --dir frontend/apps/blog build`

## 实施结果

- 已新增 comments API、mapper、Pinia store 和对应 Vitest 覆盖。
- 已将 `Comment.vue` 从第三方插件初始化入口替换为 V2 自研文章评论组件，同时保留旧 page/link 调用的 props 类型兼容；没有 `articleId` 时不加载 V2 评论。
- 已在文章详情页正文后挂载 V2 评论区。
- 已修正评论 API 文档中公开评论 ID 示例和 `replyToCommentId` 类型，统一为 string/null。
- 已更新 O-019 和前台 Blog 联调状态：文章评论已接入，留言板仍待接入。

阶段验证：

- `pnpm --dir frontend/apps/blog test`：通过。
- `pnpm --dir frontend/apps/blog typecheck`：通过。
- `git diff --check`：通过。

如本批只改前台和文档，不需要跑后端 Maven；若同步修改后端契约测试或 API 文档校准到后端代码，则补跑相关 Maven 定向测试。

## 风险和取舍

- 第三方评论插件代码本批不物理删除，因为旧 page/link/RecentComment 仍引用 `useCommentPlugin`。删除会扩大范围。
- 留言板不和文章评论一起做，避免同时改页面路由、旧 links/page 逻辑和评论组件。
- 评论提交后不乐观插入列表，直接刷新或显示待审核提示。这样少写同步逻辑，也符合审核场景。
- 评论表单信息不做 localStorage 记忆。用户体验可以后续补，不影响闭环。
