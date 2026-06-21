# 后台文章新增与编辑实施计划

**目标：** 在现有 Pure Admin 后台中实现 ADMIN 可用的文章新增与完整编辑闭环。

**架构：** 扩展文章 API 与模型，通过纯函数负责表单默认值、详情映射、校验和请求体规范化；编辑页只编排加载、交互和保存。列表提供入口，静态路由负责 ADMIN 权限隔离。

**技术栈：** Vue 3、TypeScript、Element Plus、Vue Router、Axios、Vitest、Vue Test Utils。

---

### 任务 1：文章写 API 与模型

**文件：**
- 修改：`src/features/articles/model.ts`
- 修改：`src/api/article.ts`
- 修改：`src/api/article.test.ts`

- [x] 先添加详情、创建、更新请求测试并确认失败。
- [x] 定义 `ArticleDetail` 与 `ArticleWritePayload`，实现 `getArticle`、`createArticle`、`updateArticle`。
- [x] 运行 `pnpm vitest run src/api/article.test.ts` 并提交。

### 任务 2：编辑表单领域逻辑

**文件：**
- 新建：`src/features/articles/editor/form.ts`
- 新建：`src/features/articles/editor/form.test.ts`

- [x] 先覆盖默认值、详情映射、可选字段转 null、非草稿分类、定时发布时间和密码规则并确认失败。
- [x] 实现 `createEmptyArticleForm`、`articleDetailToForm`、`validateArticleForm`、`articleFormToPayload`。
- [x] 运行领域测试并提交。

### 任务 3：编辑页面、路由和列表入口

**文件：**
- 新建：`src/features/articles/editor/index.vue`
- 新建：`src/features/articles/editor/index.test.ts`
- 修改：`src/features/articles/index.vue`
- 修改：`src/features/articles/index.test.ts`
- 修改：`src/router/modules/articles.ts`
- 修改：`src/router/static-router.test.ts`

- [x] 先添加路由、列表入口与编辑页交互测试并确认失败。
- [x] 实现 ADMIN 新建/编辑路由、列表入口、表单加载和保存。
- [x] 运行相关组件与路由测试并提交。

### 任务 4：三语文案与阶段验证

**文件：**
- 修改：`locales/zh-CN.yaml`
- 修改：`locales/ja.yaml`
- 修改：`locales/en.yaml`
- 修改：`docs/README.md`

- [x] 增加文章编辑页三语文案并更新实现状态。
- [x] 运行 `pnpm test`、`pnpm typecheck`、`pnpm build`。
- [x] 启动后端和 Admin，使用本地 MySQL 创建、编辑并发布一篇测试文章，确认列表与持久化结果后清理联调数据。
- [x] 检查 `git diff --stat`、`git status --short` 后提交文案与状态文档。
