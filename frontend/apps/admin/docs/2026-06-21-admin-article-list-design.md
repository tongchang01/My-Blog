# 后台文章列表第一批设计

## 目标

在 `frontend/apps/admin/` 中建立第一个后台业务纵向切片：可实际查询后端数据的只读文章列表。页面供 ADMIN 与 DEMO 使用，完成标题关键词、文章状态、分页、刷新、三语名称显示以及加载、空数据、失败重试闭环。

本批不实现文章创建、编辑、删除、回收站、批量选择或 Vditor。

## 视觉方向

采用 Product Design 方案 **02 · Query Workspace（可扩展查询台）**。

设计延续 V1 后台文章页的高密度管理表格和独立筛选区域，但使用 Pure Admin Thin / Element Plus 的现有导航、颜色、字体、间距和组件体系。页面不引入新的品牌色、字体、图标系统或独立布局框架。

### 页面结构

1. 页面标题与现有面包屑。
2. 独立筛选卡片：
   - 标题关键词输入框。
   - 文章状态下拉框。
   - 查询按钮。
   - 重置按钮。
   - 折叠与展开交互。
3. 独立结果区域：
   - 当前结果总数。
   - 刷新按钮。
   - 文章表格。
   - 右下角分页。

筛选区域采用可扩展网格。第一批只渲染两个筛选项，不显示分类、标签、日期等禁用占位控件。

### 表格

表格列固定为：

| 列 | 展示规则 |
|---|---|
| 标题 | 中文标题为主；slug 为次级信息；日文、英文存在时显示轻量语言标记 |
| 状态 | DRAFT、PUBLISHED、PASSWORD、SCHEDULED 使用低饱和状态标签 |
| 分类 | 按当前后台界面语言显示；缺失时显示 `—` |
| 标签 | 按当前后台界面语言显示紧凑标签；无标签时显示 `—` |
| 评论数 | 显示后端真实值，不伪造统计 |
| 发布时间 | 按 JST 语义显示；为空时显示 `—` |
| 更新时间 | 按 JST 语义显示 |

本批不显示操作列、选择框、新增按钮、编辑按钮、删除按钮或禁用的未来功能按钮。

分页默认每页 10 条，可切换 10/20 条，包含总数、上一页、页码、下一页和跳页。

## 接口契约前置修复

当前后台文章、分类和标签响应仍直接序列化 Java `long`，雪花 ID 可能超过 JavaScript 安全整数范围。进入前端页面实现前，先完成以下 Web 契约修复：

- 后台文章列表与详情中的 `id`、`categoryId`、`coverAttachmentId`、`tagIds`、`createdBy`、`updatedBy` 改为 JSON string 或 nullable string。
- 后台分类与标签响应中的 `id`、`createdBy`、`updatedBy` 改为 JSON string 或 nullable string。
- Java application、domain、repository 与数据库内部继续使用 `long`，只在 Web DTO 映射处转换。
- OpenAPI schema 明确这些字段为 `type: string`、`format: int64`。

同步修正文档与真实控制器不一致的查询契约：

- 标题参数使用 `titleKeyword`，不是 `keyword`。
- 状态参数使用枚举名 `DRAFT`、`PUBLISHED`、`PASSWORD`、`SCHEDULED`。
- 时间参数保持 `yyyy-MM-dd'T'HH:mm:ss` 的 JST 本地时间语义。

## 前端结构

```text
src/api/article.ts                         文章、分类、标签只读请求
src/features/articles/model.ts             前端文章、分类、标签与分页类型
src/features/articles/query.ts             查询参数规范化与序列化
src/features/articles/presentation.ts      三语名称、状态和 JST 时间展示映射
src/features/articles/useArticleList.ts    页面加载、筛选、分页、刷新和重试状态
src/features/articles/index.vue            文章列表页面
src/router/modules/articles.ts             静态菜单与 /articles 路由
```

数据逻辑不放进大型 Vue 组件。API、纯展示映射和页面状态分别测试，页面组件只负责组合 Element Plus 控件与触发动作。

## 数据流

页面初始化时并行请求：

- `GET /api/admin/articles?page=1&size=10`
- `GET /api/admin/categories`
- `GET /api/admin/tags`

文章接口使用真实参数：

```text
page
size
status
titleKeyword
```

点击查询或在关键词输入框按 Enter 时，将页码重置为 1 后加载。点击重置时清空关键词和状态、回到第 1 页并立即加载。切换分页或每页数量时立即加载；修改每页数量时回到第 1 页。刷新按钮保留当前筛选和分页条件。

分类和标签列表用于把文章中的 ID 映射为当前界面语言名称，并为后续增加分类、标签筛选复用。界面语言变化后只重新计算展示名称，不重复请求文章数据。

后端 `LocalDateTime` 字符串按 Asia/Tokyo 本地时间直接格式化，不使用 `new Date()` 做浏览器时区转换。

## 页面状态

### 加载

- 首次加载使用表格骨架或表格 loading 遮罩。
- 后续查询、分页和刷新保留现有表格，显示 loading，避免页面跳动。
- 只接受最近一次请求结果，旧请求不得覆盖新筛选结果。

### 空数据

请求成功但 `records` 为空时显示文章空状态。筛选条件非空时文案说明“没有符合条件的文章”；无筛选条件时说明“暂无文章”。

### 错误

- 文章列表失败时在结果区域显示内联错误和重试按钮。
- 重试保留当前查询条件。
- 分类或标签辅助数据失败不阻断文章列表；对应名称暂时显示 `—`。
- 所有业务判断只使用 `code`，不依赖后端 `msg` 文案。

## 权限

- ADMIN 与 DEMO 均可访问 `/articles` 和只读接口。
- 本批无写操作，因此不需要按钮级权限分支。
- 前端静态守卫负责页面入口；后端角色校验仍是最终安全边界。

## 国际化

新增中文、日文、英文资源，覆盖：

- 菜单与页面标题。
- 筛选字段、状态、查询、重置、刷新。
- 表头、总数、分页。
- 加载、空数据、失败和重试。

文章标题、分类和标签根据当前后台 locale 选择 `nameZh/nameJa/nameEn` 或 `titleZh/titleJa/titleEn`；目标语言为空时回退中文，再回退任一非空值。

## 测试与验收

### 后端

- Controller JSON 测试使用超过 `Number.MAX_SAFE_INTEGER` 的 ID，断言返回字符串。
- OpenAPI 测试断言后台文章、分类和标签 ID schema 为 `string/int64`。
- 运行文章、分类、标签相关局部测试后，再运行 `mvn clean test`。

### 前端

- 查询参数规范化：空关键词不发送、状态枚举正确、页码和每页数量正确。
- 展示映射：三语回退、标签映射、状态文案、JST 时间不发生时区偏移。
- 页面状态：首次加载、查询、Enter、重置、分页、每页数量、刷新、最近请求优先和重试。
- 页面组件：独立筛选卡片、结果总数、表格列、空状态、错误状态，并断言不存在操作列和写操作按钮。
- 静态路由与菜单包含 `/articles`，ADMIN/DEMO 均可访问。
- 最后运行前端 lint、typecheck、test 和 build。

## 非目标

- 文章详情页。
- Vditor 与文章新建、完整编辑。
- 删除、恢复、回收站和批量操作。
- 分类、标签和日期筛选控件。
- 封面上传、导入、导出、置顶和推荐。
- 真实统计仪表盘。
