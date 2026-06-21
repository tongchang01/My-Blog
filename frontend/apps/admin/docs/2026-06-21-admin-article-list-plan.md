# 后台文章只读列表实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 Pure Admin Thin 后台中交付可直接对接 V2 后端的文章只读列表，支持独立筛选卡片、分页、刷新、三语展示和完整加载状态，同时消除 Snowflake ID 在浏览器中的精度风险。

**Architecture:** 后端仅调整 Web DTO 与 OpenAPI 表达，不改变 application/domain/repository 的 `long` 模型；前端按“API 契约 → 纯查询/展示函数 → 可测试状态控制器 → Vue 页面”分层。文章主请求与分类/标签字典并行，文章请求采用 latest-request-wins，字典失败降级显示 `—`，不阻断列表。

**Tech Stack:** Java 21、Spring Boot 3、MockMvc、springdoc-openapi、Vue 3、TypeScript、Element Plus、Vue Router、Vue I18n、Axios、Vitest、Vue Test Utils、pnpm 9。

---

## 文件职责

- `MyBlog-springboot-v2/.../content/web/*VO.java`：浏览器可见的文章、分类、标签 ID 字符串契约。
- `MyBlog-springboot-v2/.../content/web/*WebMapping.java`：将内部 `long` ID 统一转换为十进制字符串。
- `MyBlog-springboot-v2/.../content/web/*ControllerTest.java`：用大于 `Number.MAX_SAFE_INTEGER` 的 ID 锁定 JSON 精度契约。
- `MyBlog-springboot-v2/.../*OpenApiTest.java`：锁定 OpenAPI 中 ID 的 `string/int64` schema。
- `docs/project-handbook/api-contract/article.md`：记录真实查询参数、状态枚举、时间与 ID 语义。
- `frontend/apps/admin/src/api/article.ts`：文章、分类、标签只读请求。
- `frontend/apps/admin/src/features/articles/model.ts`：后端契约和页面模型。
- `frontend/apps/admin/src/features/articles/query.ts`：筛选条件规范化和请求参数生成。
- `frontend/apps/admin/src/features/articles/presentation.ts`：多语言名称、状态和时间的纯展示转换。
- `frontend/apps/admin/src/features/articles/useArticleList.ts`：加载、失败、分页、刷新和并发请求状态。
- `frontend/apps/admin/src/features/articles/index.vue`：独立筛选卡片和只读表格页面。
- `frontend/apps/admin/src/router/modules/articles.ts`：ADMIN/DEMO 均可访问的静态文章路由。
- `frontend/apps/admin/locales/{zh-CN,ja,en}.yaml`：菜单、筛选、表格和状态三语文案。

### Task 1：修复后台文章 ID 的前端精度契约

**Files:**
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/AdminArticleControllerTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/ArticleOpenApiTest.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/AdminArticlePageItemVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/AdminArticleDetailVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`

- [ ] **Step 1: 写失败测试**

在 MockMvc fixture 中使用 `9007199254740993L`，断言列表和详情 JSON 的 `id`、`categoryId`、`authorId`、`coverAttachmentId`、`tagIds`、`createdBy`、`updatedBy` 均为字符串；在 OpenAPI 测试中断言相应 schema 为 `type=string, format=int64`。

- [ ] **Step 2: 验证 RED**

Run: `cd MyBlog-springboot-v2; mvn test -Dtest=AdminArticleControllerTest,ArticleOpenApiTest`

Expected: FAIL，现有 JSON 返回 number 或 OpenAPI schema 为 integer/int64。

- [ ] **Step 3: 最小实现**

将两个 Web record 的浏览器可见 ID 改为 `String`、可空 ID 改为 `String`、集合改为 `List<String>`，并在 `ArticleWebMapping` 中复用 `id(long)` / `nullableId(Long)`，新增列表映射：

```java
private List<String> ids(List<Long> values) {
    return values.stream().map(this::id).toList();
}
```

- [ ] **Step 4: 验证 GREEN**

Run: `cd MyBlog-springboot-v2; mvn test -Dtest=AdminArticleControllerTest,ArticleOpenApiTest`

Expected: Tests run，Failures 0，Errors 0。

- [ ] **Step 5: 检查范围并提交**

Run: `git diff --stat; git status --short`

Commit: `修复后台文章ID前端精度契约`

### Task 2：修复后台分类标签 ID 的前端精度契约

**Files:**
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/AdminCategoryTagControllerTest.java`
- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/web/CategoryTagOpenApiTest.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/AdminCategoryVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/AdminTagVO.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/CategoryWebMapping.java`
- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/TagWebMapping.java`

- [ ] **Step 1: 写失败测试**

使用 `9007199254740993L` 断言分类/标签的 `id`、`createdBy`、`updatedBy` 为字符串，OpenAPI schema 为 `string/int64`。

- [ ] **Step 2: 验证 RED**

Run: `cd MyBlog-springboot-v2; mvn test -Dtest=AdminCategoryTagControllerTest,CategoryTagOpenApiTest`

Expected: FAIL，现有字段仍序列化为 JSON number。

- [ ] **Step 3: 最小实现**

仅修改 Web VO 和 mapping；内部 category/tag application/domain 类型保持 `long`。

- [ ] **Step 4: 验证 GREEN 并提交**

Run: `cd MyBlog-springboot-v2; mvn test -Dtest=AdminCategoryTagControllerTest,CategoryTagOpenApiTest`

Run: `git diff --stat; git status --short`

Commit: `修复后台分类标签ID前端精度契约`

### Task 3：同步后台文章查询接口文档

**Files:**
- Modify: `docs/project-handbook/api-contract/article.md`

- [ ] **Step 1: 对照 Controller 和 VO 核对文档**

确认查询参数为 `titleKeyword`、`status`、`page`、`size`；状态为 `DRAFT/PUBLISHED/PASSWORD/SCHEDULED`；ID 为十进制字符串；时间为 Asia/Tokyo 的 `LocalDateTime` 文本，前端不得再次做时区换算。

- [ ] **Step 2: 修改示例与字段表**

移除数字状态和 `keyword` 旧契约，补齐分类、标签、评论数、发布时间、更新时间字段。

- [ ] **Step 3: 验证并提交**

Run: `rg -n 'keyword|status.*number|"status": [1-4]' docs/project-handbook/api-contract/article.md`

Expected: 不再命中已废弃契约。

Run: `git diff --stat; git status --short`

Commit: `同步后台文章查询接口文档`

### Task 4：建立前端文章数据模型、查询和展示转换

**Files:**
- Create: `frontend/apps/admin/src/api/article.ts`
- Create: `frontend/apps/admin/src/api/article.test.ts`
- Create: `frontend/apps/admin/src/features/articles/model.ts`
- Create: `frontend/apps/admin/src/features/articles/query.ts`
- Create: `frontend/apps/admin/src/features/articles/query.test.ts`
- Create: `frontend/apps/admin/src/features/articles/presentation.ts`
- Create: `frontend/apps/admin/src/features/articles/presentation.test.ts`

- [ ] **Step 1: 写查询参数失败测试**

覆盖标题 trim、空标题省略、`ALL` 状态省略、页码转换为后端 0-based、size 原样传递。

- [ ] **Step 2: 验证 RED**

Run: `cd frontend/apps/admin; pnpm test -- src/features/articles/query.test.ts`

Expected: FAIL，模块不存在。

- [ ] **Step 3: 实现模型与查询转换并验证 GREEN**

定义 `ArticleStatus`、`ArticleListItem`、`CategoryItem`、`TagItem`、`PageResponse<T>` 和 `buildArticleListParams()`，不引入编辑字段或模拟数据。

- [ ] **Step 4: 写展示转换失败测试**

覆盖名称回退顺序“当前语言 → zh-CN → 任意非空值 → `—`”、未知状态回退、JST 文本原样格式化且不调用 `new Date()`。

- [ ] **Step 5: 实现展示函数并验证 GREEN**

Run: `cd frontend/apps/admin; pnpm test -- src/features/articles/query.test.ts src/features/articles/presentation.test.ts`

Expected: 相关测试全部通过。

- [ ] **Step 6: 写 API 失败测试并实现请求**

使用 Axios mock 断言：

```text
GET /api/v2/admin/articles
GET /api/v2/admin/categories
GET /api/v2/admin/tags
```

文章请求只发送 `buildArticleListParams()` 产生的参数。

- [ ] **Step 7: 验证并提交**

Run: `cd frontend/apps/admin; pnpm test -- src/api/article.test.ts src/features/articles/query.test.ts src/features/articles/presentation.test.ts`

Run: `git diff --stat; git status --short`

Commit: `建立后台文章列表数据模型`

### Task 5：实现文章列表请求状态和并发控制

**Files:**
- Create: `frontend/apps/admin/src/features/articles/useArticleList.ts`
- Create: `frontend/apps/admin/src/features/articles/useArticleList.test.ts`

- [ ] **Step 1: 写失败测试**

通过依赖注入的 `ArticleListApi` 覆盖：首次三请求并行、查询重置到第 1 页、重置恢复默认条件、刷新保留条件、主请求失败进入 error、字典失败不阻断、后发请求结果覆盖先发请求。

- [ ] **Step 2: 验证 RED**

Run: `cd frontend/apps/admin; pnpm test -- src/features/articles/useArticleList.test.ts`

Expected: FAIL，状态模块不存在。

- [ ] **Step 3: 最小实现**

使用 Vue `ref/reactive/computed` 管理状态；每次文章请求递增 `requestVersion`，仅当响应版本等于当前版本时写入数据；分类和标签请求分别 catch 并清空对应字典。

- [ ] **Step 4: 验证 GREEN 并提交**

Run: `cd frontend/apps/admin; pnpm test -- src/features/articles/useArticleList.test.ts`

Run: `git diff --stat; git status --short`

Commit: `实现后台文章列表查询状态`

### Task 6：实现文章列表页面、路由和三语文案

**Files:**
- Create: `frontend/apps/admin/src/features/articles/index.vue`
- Create: `frontend/apps/admin/src/features/articles/index.test.ts`
- Create: `frontend/apps/admin/src/router/modules/articles.ts`
- Modify: `frontend/apps/admin/src/router/static-router.test.ts`
- Modify: `frontend/apps/admin/locales/zh-CN.yaml`
- Modify: `frontend/apps/admin/locales/ja.yaml`
- Modify: `frontend/apps/admin/locales/en.yaml`

- [ ] **Step 1: 写页面与路由失败测试**

断言页面有独立筛选卡片、标题与状态筛选、查询/重置/刷新、分页、加载/空/error/retry 分支；断言不存在操作列和编辑/删除按钮；断言 `/articles` 对 `ADMIN`、`DEMO` 可见。

- [ ] **Step 2: 验证 RED**

Run: `cd frontend/apps/admin; pnpm test -- src/features/articles/index.test.ts src/router/static-router.test.ts`

Expected: FAIL，页面与路由尚不存在。

- [ ] **Step 3: 实现选定的 Query Workspace 页面**

顶部使用可折叠独立筛选卡片；下方表格卡片展示多语言标题、状态、分类、标签、评论数、发布时间、更新时间；窄屏允许横向滚动；不增加后台尚未支持的编辑操作。

- [ ] **Step 4: 补齐三语文案并验证 GREEN**

Run: `cd frontend/apps/admin; pnpm test -- src/features/articles/index.test.ts src/router/static-router.test.ts`

Expected: 相关测试全部通过。

- [ ] **Step 5: 前端局部质量门禁并提交**

Run: `cd frontend/apps/admin; pnpm typecheck`

Run: `cd frontend/apps/admin; pnpm test`

Run: `git diff --stat; git status --short`

Commit: `建立后台文章只读列表页面`

### Task 7：完整验证并记录验收结果

**Files:**
- Modify: `frontend/apps/admin/docs/README.md`
- Modify: `frontend/apps/admin/docs/2026-06-21-admin-article-list-plan.md`
- Modify if current status conventions require: `docs/project-handbook/status.md`
- Modify if current roadmap conventions require: `docs/project-handbook/roadmap.md`

- [ ] **Step 1: 运行前端完整验证**

Run: `cd frontend/apps/admin; pnpm test && pnpm typecheck && pnpm build`

Expected: exit 0，Vitest 无失败，TypeScript 无错误，Vite build 成功。

- [ ] **Step 2: 运行后端完整验证**

Run: `cd MyBlog-springboot-v2; mvn clean test`

Expected: exit 0，Failures 0，Errors 0；含 ArchUnit。

- [ ] **Step 3: 更新验收记录**

在本文勾选已完成步骤，并在后台 README/项目状态中记录：只读范围、ADMIN/DEMO 权限、已验证命令、后续编辑能力仍待后台表单与权限设计。

- [ ] **Step 4: 最终范围检查并提交**

Run: `git diff --stat; git status --short; git log --oneline -8`

Commit: `记录后台文章列表验收结果`

## 自审结果

- 规格覆盖：独立筛选卡片、两项当前筛选、未来扩展位、只读权限、无操作列、三语、分页和全状态均有对应任务。
- 类型一致：后端 Web ID 与前端模型统一使用 `string`；内部 Java ID 保持 `long`。
- 并发与降级：主列表 latest-request-wins，字典失败不阻断，错误态可重试。
- 范围控制：本批次不实现新建、编辑、删除、批量操作，也不顺带清理 V1。
- 提交边界：后端契约、接口文档、前端模型、状态控制、页面、验收分别提交。

## 执行结果（2026-06-21）

- Task 1–7 均已执行；实现提交按后端文章 ID、分类标签 ID、接口文档、前端模型、请求状态、页面、视觉 QA 和验收文档拆分。
- 实施时按实际后端契约修正两处计划表述：接口前缀为 `/api/admin/*`，后台分页从 1 开始，前端不做 0-based 转换。
- 前端：13 个测试文件、43 tests 全部通过；typecheck 和生产构建通过。
- 后端：`mvn clean test` 共 637 tests，0 failures，0 errors，4 skipped。
- 视觉：1253 × 989 同视口对比完成，`../design-qa.md` 的 `final result: passed`。
