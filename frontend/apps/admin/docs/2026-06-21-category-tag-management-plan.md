# 后台分类与标签管理实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 Pure Admin 后台中实现 ADMIN 可写、DEMO 只读的分类与标签独立管理页面。

**Architecture:** 新建 `taxonomy` 领域集中保存分类/标签 DTO、表单纯函数与 API；分类和标签分别使用独立状态控制器与页面。后端返回完整活动列表，关键词筛选在前端计算；写操作成功后统一重新加载，分类排序只提交脏项。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vue Router、Axios、Vitest、Vue Test Utils、happy-dom。

---

### Task 1：分类标签模型、API 与表单领域逻辑

**Files:**
- Create: `src/features/taxonomy/model.ts`
- Create: `src/features/taxonomy/form.ts`
- Create: `src/features/taxonomy/form.test.ts`
- Create: `src/api/taxonomy.ts`
- Create: `src/api/taxonomy.test.ts`
- Modify: `src/features/articles/model.ts`
- Modify: `src/utils/http/error.ts`
- Modify: `src/utils/http/error.test.ts`

- [x] **Step 1: 写 API 与表单失败测试**

API 测试必须断言分类、标签的列表、详情、创建、更新、删除路径，以及 `PUT /api/admin/categories/sort-orders` 的 `{ items }` 请求体。表单测试覆盖中文名称和 slug 必填、可选语言空白转 `null`、分类排序范围 `0..1000000`、详情映射保留字符串 ID。错误测试断言 `90004` 映射为 `conflict`。

- [x] **Step 2: 运行测试并确认红灯**

```powershell
corepack pnpm exec vitest run src/api/taxonomy.test.ts src/features/taxonomy/form.test.ts src/utils/http/error.test.ts
```

Expected: FAIL，原因是 taxonomy 模块和 `conflict` 错误类型尚未定义。

- [x] **Step 3: 实现模型与纯函数**

`model.ts` 定义：

```ts
export interface TaxonomyAuditFields {
  id: string;
  createdAt: string;
  createdBy: string | null;
  updatedAt: string;
  updatedBy: string | null;
}

export interface CategoryItem extends TaxonomyAuditFields {
  nameZh: string;
  nameJa: string | null;
  nameEn: string | null;
  slug: string;
  sortOrder: number;
}

export interface TagItem extends Omit<CategoryItem, "sortOrder"> {}
export interface CategoryWritePayload {
  nameZh: string;
  nameJa: string | null;
  nameEn: string | null;
  slug: string;
  sortOrder: number;
}
export type TagWritePayload = Omit<CategoryWritePayload, "sortOrder">;
export interface CategorySortItem { id: string; sortOrder: number }
```

`form.ts` 暴露 `createCategoryForm`、`createTagForm`、`categoryToForm`、`tagToForm`、`validateCategoryForm`、`validateTagForm`、`categoryFormToPayload`、`tagFormToPayload`。错误码限定为 `required | sortOrderRange`，可选语言统一 trim 后转 `null`。

从文章模型移除重复的 `CategoryItem`、`TagItem` 定义，改为从 taxonomy 模型导入并重新导出，保持现有文章列表与编辑器导入路径兼容。

- [x] **Step 4: 实现 API 与冲突错误分类**

`taxonomy.ts` 暴露分类和标签的 list/detail/create/update/delete，以及 `updateCategorySortOrders(items)`。请求体完整提交后端要求字段，排序请求包装为 `{ items }`。在 `ApiErrorKind` 增加 `conflict`，把 `90004` 映射为 `conflict`，不改变 token 刷新逻辑。

- [x] **Step 5: 运行局部测试和类型检查**

```powershell
corepack pnpm exec vitest run src/api/taxonomy.test.ts src/features/taxonomy/form.test.ts src/utils/http/error.test.ts
corepack pnpm typecheck
```

Expected: 全部 PASS。

- [x] **Step 6: 检查并提交**

```powershell
git diff --stat
git status --short
git add frontend/apps/admin/src/api/taxonomy.ts frontend/apps/admin/src/api/taxonomy.test.ts frontend/apps/admin/src/features/taxonomy/model.ts frontend/apps/admin/src/features/taxonomy/form.ts frontend/apps/admin/src/features/taxonomy/form.test.ts frontend/apps/admin/src/features/articles/model.ts frontend/apps/admin/src/utils/http/error.ts frontend/apps/admin/src/utils/http/error.test.ts
git commit -m "实现分类标签写接口基础"
```

### Task 2：分类管理状态与页面

**Files:**
- Create: `src/features/taxonomy/categories/useCategoryManagement.ts`
- Create: `src/features/taxonomy/categories/useCategoryManagement.test.ts`
- Create: `src/features/taxonomy/categories/index.vue`
- Create: `src/features/taxonomy/categories/index.test.ts`

- [x] **Step 1: 写状态控制器失败测试**

覆盖初始化加载、本地关键词匹配三语名称与 slug、打开新增/编辑弹窗、保存后刷新、只收集修改过的排序值、删除冲突映射。关键断言：

```ts
expect(state.filteredItems.value.map(item => item.id)).toEqual(["100"]);
expect(state.dirtySortItems.value).toEqual([{ id: "100", sortOrder: 30 }]);
await state.saveSortOrders();
expect(api.updateCategorySortOrders).toHaveBeenCalledWith([
  { id: "100", sortOrder: 30 }
]);
```

- [x] **Step 2: 运行测试确认缺少实现**

```powershell
corepack pnpm exec vitest run src/features/taxonomy/categories/useCategoryManagement.test.ts
```

Expected: FAIL，模块不存在。

- [x] **Step 3: 实现分类状态控制器**

状态包含 `items`、`keyword`、`loading`、`requestError`、`operationError`、`dialogOpen`、`editingId`、`form`、`formErrors`、`saving`、`sortDrafts`。方法为 `initialize`、`retry`、`openCreate`、`openEdit`、`closeDialog`、`save`、`setSortOrder`、`saveSortOrders`、`remove`。

`filteredItems` 按原始列表顺序返回；关键词使用 `trim().toLocaleLowerCase()` 比较。写操作成功后重新请求列表并清空对应错误。

- [x] **Step 4: 写页面失败测试**

测试 ADMIN 可见新增、编辑、删除和保存排序，DEMO 不渲染写控件；测试弹窗字段、删除确认和 loading/error/empty 状态。

- [x] **Step 5: 实现分类页面**

页面使用独立筛选卡片和结果卡片。表格列为名称、slug、排序、更新时间和操作；ADMIN 的排序列使用 `el-input-number`，弹窗使用 `el-dialog`，删除使用 `ElMessageBox.confirm`。冲突提示按保存或删除操作显示本地化文案。

- [x] **Step 6: 运行局部验证并提交**

```powershell
corepack pnpm exec vitest run src/features/taxonomy/categories
corepack pnpm typecheck
git diff --stat
git status --short
git add frontend/apps/admin/src/features/taxonomy/categories
git commit -m "实现后台分类管理页面"
```

### Task 3：标签管理状态与页面

**Files:**
- Create: `src/features/taxonomy/tags/useTagManagement.ts`
- Create: `src/features/taxonomy/tags/useTagManagement.test.ts`
- Create: `src/features/taxonomy/tags/index.vue`
- Create: `src/features/taxonomy/tags/index.test.ts`

- [x] **Step 1: 写标签状态和页面失败测试**

覆盖本地筛选、新增、编辑、删除、写后刷新、ADMIN 写控件、DEMO 只读、删除确认和冲突提示，并明确断言页面不存在排序控件。

- [x] **Step 2: 运行测试确认红灯**

```powershell
corepack pnpm exec vitest run src/features/taxonomy/tags
```

Expected: FAIL，模块不存在。

- [x] **Step 3: 实现标签状态控制器与页面**

标签状态与分类保持同样的加载、筛选、弹窗和删除语义，但不包含 `sortDrafts`、`dirtySortItems` 或排序 API。表格列为名称、slug、更新时间和操作。

- [x] **Step 4: 运行局部验证并提交**

```powershell
corepack pnpm exec vitest run src/features/taxonomy/tags
corepack pnpm typecheck
git diff --stat
git status --short
git add frontend/apps/admin/src/features/taxonomy/tags
git commit -m "实现后台标签管理页面"
```

### Task 4：内容管理路由、三语文案与文档

**Files:**
- Modify: `src/router/modules/articles.ts`
- Modify: `src/router/static-router.test.ts`
- Modify: `locales/zh-CN.yaml`
- Modify: `locales/ja.yaml`
- Modify: `locales/en.yaml`
- Modify: `docs/README.md`
- Modify: `docs/2026-06-21-category-tag-management-plan.md`

- [x] **Step 1: 写路由失败测试**

```ts
expect(categoryList?.path).toBe("/categories/list");
expect(categoryList?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
expect(tagList?.path).toBe("/tags/list");
expect(tagList?.meta?.roles).toEqual(["ADMIN", "DEMO"]);
```

- [x] **Step 2: 实现菜单与路由**

在现有父路由下保留文章列表、新增、编辑路由，增加 `CategoryList` 与 `TagList` 两个可见子项。父菜单标题改为三语“内容管理”，内部 route name 保持 `Articles`，避免无关重命名。

- [x] **Step 3: 增加完整三语文案**

文案覆盖筛选、列名、新增/编辑弹窗、排序、删除确认、loading/empty/error/retry、表单校验、slug 冲突、引用冲突和成功反馈。三个语言文件键结构保持一致。

- [x] **Step 4: 更新文档并运行局部验证**

`docs/README.md` 记录分类标签页面能力、权限边界和暂未实现项；计划中已完成步骤改为 `[x]`。

```powershell
corepack pnpm exec vitest run src/router/static-router.test.ts src/features/taxonomy
corepack pnpm typecheck
```

- [x] **Step 5: 检查并提交**

```powershell
git diff --check
git diff --stat
git status --short
git add frontend/apps/admin/src/router/modules/articles.ts frontend/apps/admin/src/router/static-router.test.ts frontend/apps/admin/locales frontend/apps/admin/docs
git commit -m "接入分类标签菜单与三语文案"
```

### Task 5：完整验证与真实 MySQL 验收

**Files:**
- Modify: `docs/README.md`
- Modify: `docs/2026-06-21-category-tag-management-plan.md`

- [x] **Step 1: 运行完整前端门禁**

```powershell
corepack pnpm test
corepack pnpm typecheck
corepack pnpm build
```

Expected: 0 failures，typecheck exit 0，build exit 0；允许既有 Browserslist 数据过期警告。

- [x] **Step 2: 启动本地后端与 Admin**

后端连接 `myblog_v2_dev`，Admin 使用 `http://127.0.0.1:8848/`，确认 8080 与 8848 监听成功。

- [x] **Step 3: 浏览器验证分类闭环**

使用 ADMIN 新增临时分类，编辑三语名称和排序值，保存排序并确认列表回显；删除前完成二次确认验证。再尝试删除一个被活动文章引用的既有分类，确认页面显示引用冲突且数据未删除。

- [x] **Step 4: 浏览器验证标签闭环**

使用 ADMIN 新增临时标签，编辑名称和 slug，确认列表回显后删除；再尝试删除一个被活动文章引用的既有标签，确认引用保护提示。

- [x] **Step 5: 清理联调数据与停止服务**

通过管理 API 或针对已知临时 ID 的数据库事务彻底删除临时分类和标签；确认原有分类、标签和五篇活动文章未改变。停止 8080、8848 服务并确认端口不再监听。

- [x] **Step 6: 写验收结果并提交**

```powershell
git diff --check
git diff --stat
git status --short
git add frontend/apps/admin/docs
git commit -m "记录分类标签管理验收结果"
```

- [x] **Step 7: 最终检查**

```powershell
git status -sb
git log --oneline -8
```

Expected: 工作区干净，所有实现按单一目的拆分为中文提交。
