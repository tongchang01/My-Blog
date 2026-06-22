# 后台文章生命周期管理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复回收站 Snowflake ID Web 契约，并在 Admin 实现文章软删除、回收站分页和恢复闭环。

**Architecture:** 后端只调整回收站 Web VO 与映射，内部 long 模型和业务逻辑保持不变。前端在现有活动文章状态中加入单篇删除，并以独立状态控制器和页面实现回收站；所有写操作沿用 ADMIN 可写、DEMO 只读和二次确认规则。

**Tech Stack:** Spring Boot 3.5、MockMvc、springdoc、Vue 3、TypeScript、Element Plus、Axios、Vitest、Vue Test Utils。

---

### Task 1：修复后端回收站 ID 字符串契约

**Worktree:** `E:\My-Blog\.worktrees\backend-v2-refactor`

**Files:**
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/DeletedArticlePageItemVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/AdminArticleControllerTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/ArticleOpenApiTest.java`

- [ ] **Step 1: 写失败测试**

在 Controller 测试使用 `9007199254740993L`，断言：

```java
.andExpect(jsonPath("$.data.records[0].id")
        .value("9007199254740993"))
.andExpect(jsonPath("$.data.records[0].categoryId")
        .value("9007199254740994"))
.andExpect(jsonPath("$.data.records[0].deletedBy")
        .value("9007199254740995"));
```

在 OpenAPI 测试增加：

```java
assertStringId(root, "DeletedArticlePageItemVO", "id");
assertStringId(root, "DeletedArticlePageItemVO", "categoryId");
assertStringId(root, "DeletedArticlePageItemVO", "deletedBy");
```

- [ ] **Step 2: 运行测试确认红灯**

```powershell
mvn "-Dtest=AdminArticleControllerTest,ArticleOpenApiTest" test
```

Expected: 回收站 ID JSON 类型或 OpenAPI schema 仍为 integer，测试失败。

- [ ] **Step 3: 实现最小契约修复**

VO 改为：

```java
public record DeletedArticlePageItemVO(
        @Schema(format = "int64") String id,
        String titleZh,
        String titleJa,
        String titleEn,
        ArticleStatus status,
        @Schema(format = "int64") String categoryId,
        LocalDateTime deletedAt,
        @Schema(format = "int64") String deletedBy) {
}
```

映射使用既有 `id(item.id())` 和 `nullableId(...)`，不修改 application/domain/repository。

- [ ] **Step 4: 运行局部测试和后端全量测试**

```powershell
mvn "-Dtest=AdminArticleControllerTest,ArticleOpenApiTest" test
mvn clean test
```

Expected: 0 failures、0 errors；既有条件跳过项允许保留。

- [ ] **Step 5: 检查并提交后端**

```powershell
git diff --check
git diff --stat
git status --short
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/DeletedArticlePageItemVO.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/AdminArticleControllerTest.java MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/ArticleOpenApiTest.java
git commit -m "修复回收站文章ID前端精度契约"
```

### Task 2：扩展前端生命周期 API 与模型

**Worktree:** `E:\My-Blog\.worktrees\frontend-v2-clean`

**Files:**
- Modify: `frontend/apps/admin/src/features/articles/model.ts`
- Modify: `frontend/apps/admin/src/api/article.ts`
- Modify: `frontend/apps/admin/src/api/article.test.ts`

- [ ] **Step 1: 写失败测试**

断言：

```ts
await deleteArticle("100");
expect(mock.history.delete[0].url).toBe("/api/admin/articles/100");

await listDeletedArticles(2, 20);
expect(mock.history.get[0].params).toEqual({ page: 2, size: 20 });

await restoreArticle("100");
expect(mock.history.post[0].url).toBe("/api/admin/articles/100/restore");
```

- [ ] **Step 2: 运行测试确认缺少函数**

```powershell
corepack pnpm exec vitest run src/api/article.test.ts
```

Expected: FAIL，删除、回收站和恢复函数未定义。

- [ ] **Step 3: 实现模型与 API**

增加 `DeletedArticleListItem`，字段与设计文档一致。实现：

```ts
deleteArticle(id: string): Promise<ApiResponse<null>>
listDeletedArticles(page: number, size: number):
  Promise<ApiResponse<PageResponse<DeletedArticleListItem>>>
restoreArticle(id: string): Promise<ApiResponse<ArticleDetail>>
```

路径分别为 `DELETE /api/admin/articles/{id}`、`GET /api/admin/articles/recycle-bin`、`POST /api/admin/articles/{id}/restore`。

- [ ] **Step 4: 验证并提交**

```powershell
corepack pnpm exec vitest run src/api/article.test.ts
corepack pnpm typecheck
git diff --check
git diff --stat
git status --short
git add frontend/apps/admin/src/features/articles/model.ts frontend/apps/admin/src/api/article.ts frontend/apps/admin/src/api/article.test.ts
git commit -m "扩展文章删除回收站接口"
```

### Task 3：活动文章列表接入删除

**Files:**
- Modify: `frontend/apps/admin/src/features/articles/useArticleList.ts`
- Modify: `frontend/apps/admin/src/features/articles/useArticleList.test.ts`
- Modify: `frontend/apps/admin/src/features/articles/index.vue`
- Modify: `frontend/apps/admin/src/features/articles/index.test.ts`

- [ ] **Step 1: 写状态和页面失败测试**

状态测试覆盖删除成功刷新、尾页为空退回上一页、删除失败保留数据：

```ts
await expect(state.remove("100")).resolves.toBe(true);
expect(source.deleteArticle).toHaveBeenCalledWith("100");
expect(source.listArticles).toHaveBeenCalledTimes(2);
```

页面测试断言 ADMIN 操作列含删除能力、DEMO 不渲染操作列，并通过 `ElMessageBox.confirm` 后调用 `state.remove(id)`。

- [ ] **Step 2: 运行测试确认红灯**

```powershell
corepack pnpm exec vitest run src/features/articles/useArticleList.test.ts src/features/articles/index.test.ts
```

Expected: FAIL，状态无 `remove`，页面无删除确认。

- [ ] **Step 3: 实现删除状态**

`ArticleListApi` 增加 `deleteArticle`。状态增加 `operationError`、`deletingId` 和 `remove(id)`。删除成功后刷新当前页；若刷新后 `items=[]`、`total>0` 且 `page>1`，页码减一并再次加载。

- [ ] **Step 4: 实现页面删除确认**

ADMIN 操作列宽度调整为可容纳编辑、删除。确认文案包含本地化标题，确认后调用 `remove`；请求期间对应删除按钮 loading/disabled。失败显示不关闭的错误 Alert。

- [ ] **Step 5: 验证并提交**

```powershell
corepack pnpm exec vitest run src/features/articles/useArticleList.test.ts src/features/articles/index.test.ts
corepack pnpm typecheck
git diff --check
git diff --stat
git status --short
git add frontend/apps/admin/src/features/articles/useArticleList.ts frontend/apps/admin/src/features/articles/useArticleList.test.ts frontend/apps/admin/src/features/articles/index.vue frontend/apps/admin/src/features/articles/index.test.ts
git commit -m "实现文章列表软删除"
```

### Task 4：实现文章回收站状态与页面

**Files:**
- Create: `frontend/apps/admin/src/features/articles/recycle-bin/useArticleRecycleBin.ts`
- Create: `frontend/apps/admin/src/features/articles/recycle-bin/useArticleRecycleBin.test.ts`
- Create: `frontend/apps/admin/src/features/articles/recycle-bin/index.vue`
- Create: `frontend/apps/admin/src/features/articles/recycle-bin/index.test.ts`

- [ ] **Step 1: 写状态失败测试**

覆盖列表与分类并行加载、分类失败降级、分页、刷新、恢复成功、尾页回退、`90004` 恢复冲突。

状态 API：

```ts
interface ArticleRecycleBinApi {
  listDeletedArticles(page: number, size: number): Promise<...>;
  listCategories(): Promise<...>;
  restoreArticle(id: string): Promise<...>;
}
```

- [ ] **Step 2: 运行状态测试确认红灯**

```powershell
corepack pnpm exec vitest run src/features/articles/recycle-bin/useArticleRecycleBin.test.ts
```

Expected: FAIL，模块不存在。

- [ ] **Step 3: 实现状态控制器**

状态包含 `items/categories/page/size/total/loading/error/operationError/restoringId`。方法为 `initialize/retry/refresh/changePage/restore`。主体请求采用 latest-request-wins；分类失败只清空字典。

- [ ] **Step 4: 写页面失败测试**

断言结果卡、分页、loading/empty/error/retry；ADMIN 显示恢复操作，DEMO 只读；恢复确认后调用状态方法；`90004` 显示引用失效提示。

- [ ] **Step 5: 实现回收站页面**

表格列为标题、删除前状态、分类、删除时间、删除人、操作。复用三语标题回退、状态标签和 JST 时间格式化。恢复确认文案包含标题和状态。

- [ ] **Step 6: 验证并提交**

```powershell
corepack pnpm exec vitest run src/features/articles/recycle-bin
corepack pnpm typecheck
git diff --check
git diff --stat
git status --short
git add frontend/apps/admin/src/features/articles/recycle-bin
git commit -m "实现后台文章回收站"
```

### Task 5：路由、三语文案与文档

**Files:**
- Modify: `frontend/apps/admin/src/router/modules/articles.ts`
- Modify: `frontend/apps/admin/src/router/static-router.test.ts`
- Modify: `frontend/apps/admin/locales/zh-CN.yaml`
- Modify: `frontend/apps/admin/locales/ja.yaml`
- Modify: `frontend/apps/admin/locales/en.yaml`
- Modify: `frontend/apps/admin/docs/README.md`
- Modify: `frontend/apps/admin/docs/2026-06-22-article-lifecycle-management-plan.md`

- [ ] **Step 1: 写路由失败测试**

```ts
expect(recycleBin?.path).toBe("/articles/recycle-bin");
expect(recycleBin?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
expect(recycleBin?.meta?.showLink).toBe(true);
```

- [ ] **Step 2: 接入路由和三语文案**

新增可见 `ArticleRecycleBin` 子路由。文案覆盖删除、恢复、确认框、回收站列名、状态、空态、请求错误、无权限、目标不存在和引用冲突。

- [ ] **Step 3: 更新文档并运行局部验证**

```powershell
corepack pnpm exec vitest run src/router/static-router.test.ts src/features/articles
corepack pnpm typecheck
```

README 记录生命周期能力和暂不实现项；计划已完成步骤改为 `[x]`。

- [ ] **Step 4: 检查并提交**

```powershell
git diff --check
git diff --stat
git status --short
git add frontend/apps/admin/src/router/modules/articles.ts frontend/apps/admin/src/router/static-router.test.ts frontend/apps/admin/locales frontend/apps/admin/docs
git commit -m "接入文章回收站菜单与三语文案"
```

### Task 6：完整验证与真实 MySQL 验收

**Files:**
- Modify: `frontend/apps/admin/docs/README.md`
- Modify: `frontend/apps/admin/docs/2026-06-22-article-lifecycle-management-plan.md`

- [ ] **Step 1: 运行前端完整门禁**

```powershell
corepack pnpm test
corepack pnpm typecheck
corepack pnpm build
```

Expected: 0 failures，typecheck/build exit 0；允许既有 Browserslist 数据过期提示。

- [ ] **Step 2: 启动后端和 Admin**

后端连接 `myblog_v2_dev`，Admin 监听 8848，确认 8080/8848 可访问。

- [ ] **Step 3: 浏览器验证生命周期闭环**

ADMIN 新建临时草稿；从列表二次确认删除；回收站确认记录、状态和删除时间；二次确认恢复；再次删除。验证 DEMO 路由可读且无删除/恢复控件。

- [ ] **Step 4: 清理联调数据**

针对临时文章已知 ID 删除 `t_article_tag` 与 `t_article`，确认临时记录为 0、原有活动文章仍为 5。停止服务并确认 8080/8848 未监听。

- [ ] **Step 5: 写验收结果并提交**

```powershell
git diff --check
git diff --stat
git status --short
git add frontend/apps/admin/docs
git commit -m "记录文章生命周期管理验收结果"
```

- [ ] **Step 6: 最终验证并推送两个分支**

重新运行后端和前端完整门禁，确认两个工作区干净。分别推送 `backend-v2-integration-ready` 与 `frontend-v2-clean`，不创建 PR、不合并、不删除 worktree。

