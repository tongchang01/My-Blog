# 后台文章生命周期管理设计

## 目标

在现有文章列表和编辑闭环上补齐文章软删除、回收站查询和恢复能力。ADMIN 可以删除与恢复文章，DEMO 只读；后端继续作为权限、引用有效性和软删除审计的最终边界。

## 前置契约修复

回收站接口当前返回的 `DeletedArticlePageItemVO.id`、`categoryId`、`deletedBy` 仍是 Java `long/Long`，与已建立的浏览器 Snowflake ID 字符串契约不一致。

在实现前端前，先在 `backend-v2-integration-ready` 分支完成独立小修复：

- 回收站文章 `id` 输出 JSON string，OpenAPI 为 `string/int64`。
- 可空 `categoryId`、`deletedBy` 输出 nullable JSON string，OpenAPI 为 nullable `string/int64`。
- Java application/domain/repository 内部继续使用 `long/Long`。
- 增加 MockMvc 与 OpenAPI 回归测试，不修改数据库结构和业务行为。

后端修复只提交到后端分支；前端代码只提交到 `frontend-v2-clean`。

## 页面与路由

沿用内容管理菜单组：

- `/articles/list`：活动文章列表，ADMIN 增加删除操作，DEMO 保持只读。
- `/articles/recycle-bin`：独立回收站页面，ADMIN 与 DEMO 均可访问。
- 文章新增、编辑路由继续隐藏；回收站作为可见菜单项。

回收站不与活动列表共用 Tab，避免两套分页、错误状态和操作权限互相耦合。

## 活动文章删除

文章列表的操作列对 ADMIN 展示“编辑”和“删除”。删除流程：

1. 打开二次确认，明确文章标题和“删除后可从回收站恢复”。
2. 确认后调用 `DELETE /api/admin/articles/{id}`。
3. 成功后保留当前筛选条件并刷新当前页。
4. 如果删除后当前页为空且页码大于 1，自动回到上一页重新加载。
5. 失败时不改列表，显示本地化错误提示。

DEMO 不渲染删除按钮。后端权限仍是最终边界。

## 回收站页面

回收站使用独立结果卡片，不增加后端不支持的关键词筛选。表格展示：

- 当前后台语言下的文章标题，并显示其他已填写语言徽标。
- 删除前文章状态。
- 分类名称；分类字典查询失败或分类已删除时显示 `—`。
- 删除时间。
- 删除人 ID。
- ADMIN 操作列：恢复。

接口只支持 `page`、`size`，页面提供分页、刷新、loading、empty、error 和 retry。分类字典与回收站列表并行加载，分类失败不阻断回收站主体。

## 恢复流程

恢复 `PUBLISHED` 或其他公开状态文章可能立即重新出现在前台，因此所有状态恢复都需要二次确认。

1. 确认框展示文章标题和删除前状态。
2. 调用 `POST /api/admin/articles/{id}/restore`。
3. 成功后刷新当前回收站页；若当前页为空且页码大于 1，退回上一页。
4. `90004` 显示“分类、标签或封面引用已失效，无法恢复”。
5. `90003` 显示文章已不存在。
6. 其他错误保留当前页并显示通用操作失败。

DEMO 只读，不渲染恢复按钮。

## 数据模型与状态边界

前端新增 `DeletedArticleListItem`：

```ts
interface DeletedArticleListItem {
  id: string;
  titleZh: string | null;
  titleJa: string | null;
  titleEn: string | null;
  status: ArticleStatus;
  categoryId: string | null;
  deletedAt: string;
  deletedBy: string | null;
}
```

活动文章列表状态只增加单篇删除能力；回收站使用独立 `useArticleRecycleBin` 控制器，管理分页、列表、分类字典、加载错误和恢复错误。API 层增加 `deleteArticle`、`listDeletedArticles`、`restoreArticle`。

## 错误与交互

- 危险操作统一使用 `ElMessageBox.confirm`。
- 删除、恢复请求期间禁用重复提交。
- `10003`：无写权限。
- `90003`：目标不存在。
- `90004`：恢复引用失效或并发状态冲突。
- 网络和未知错误：保留页面状态并允许重试。
- 三语文案覆盖菜单、列名、确认提示、空状态和错误提示。

## 测试与验收

### 后端

- MockMvc 使用超过 JavaScript 安全整数的 fixture，断言回收站 ID 字段为字符串。
- OpenAPI 断言 `id/categoryId/deletedBy` 为 `string/int64`，可空字段保留 nullable。
- 运行文章 Web 局部测试，再运行 `mvn clean test`。

### 前端

- API：删除、回收站分页、恢复的路径、方法和参数。
- 活动列表状态：删除成功刷新、尾页回退、错误保留。
- 回收站状态：并行加载、分页、刷新、字典降级、恢复成功和引用冲突。
- 页面：ADMIN 删除/恢复、DEMO 只读、二次确认、loading/empty/error/retry。
- 路由：回收站可见并允许 ADMIN/DEMO 读取。
- 阶段门禁：71 项既有测试基础上运行全量测试、typecheck 和生产构建。
- 真实 MySQL 浏览器联调：创建一篇临时草稿，删除、进入回收站、恢复、再次删除，最后彻底清理临时文章并确认原有 5 篇活动文章未改变。

## 暂不实现

- 永久删除、批量删除、批量恢复和回收站关键词筛选。
- 分类、标签或附件失效后的自动修复。
- 回收站文章详情预览。
- Markdown 编辑器增强、评论审核及其他后台模块。

