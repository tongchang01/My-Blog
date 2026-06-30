# 前台分类标签文章数量实施思路

> 状态：实施思路
> 适用范围：前台 blog、后端 content、公开 API、后台 taxonomy 管理
> 最后校准：2026-06-30
> 权威程度：过程计划，裁决后需提炼到 handbook

## 本文档回答什么问题

本文档记录已经裁决的公开分类和标签文章数量处理方向，作为后续拆分正式实现任务前的实施依据。

本文档不直接替代 `docs/handbook/start-here/open-issues.md`、`docs/handbook/api/category-tag.md` 或业务规则文档。实现完成后，需要把最终结论提炼回 handbook。

## 前台原始语义

Aurora/Hexo 原始前台通过静态 JSON 消费分类和标签：

```text
/categories.json
/tags.json
```

每一项都包含数量字段：

```json
{
  "name": "Vue",
  "slug": "vue",
  "path": "tags/vue/",
  "count": 5
}
```

该 `count` 的语义是当前生成站点中，挂在这个分类或标签下的文章数量。它不是分类/标签自身权重，也不是后台管理计数。

迁移到 V2 后端后，该语义调整为：

> 当前公开列表里可被读者看到的文章数量。

## 已定方向

### 1. 保留公开文章数量字段

公开分类和标签响应保留文章数量能力，建议字段名为 `articleCount`。

旧前台字段名是 `count`，V2 API 建议使用更明确的 `articleCount`；前台 mapper 可以把 `articleCount` 映射为组件需要的 `count`，或直接调整前台 view model 字段。

### 2. 不新增数据库计数字段

不在 `t_category` / `t_tag` 增加 `count`、`article_count` 等存储字段。

原因：

- 数量会随文章状态变化。
- 数量会随定时发布变化。
- 数量会随文章删除、恢复变化。
- 数量会随文章分类、标签调整变化。
- 落库字段需要额外维护一致性，容易和真实公开列表脱节。

`articleCount` 是公开查询聚合结果，应由 SQL 或查询模型实时计算。

### 3. 统计口径与公开文章列表一致

`articleCount` 只统计读者在公开文章列表中能看到的文章元数据。

纳入统计：

- 文章未软删除。
- 文章状态为 `PUBLISHED`。
- 文章状态为 `PASSWORD`。
- `publish_at <= now`。

排除统计：

- `DRAFT`。
- `PRIVATE`。
- `SCHEDULED`。
- `publish_at > now`。
- 已软删除文章。

`PASSWORD` 文章纳入统计，因为它在公开列表可见，只是正文需要解锁。

### 4. 公开端只返回有公开文章的项

公开读者端分类/标签列表只返回 `articleCount > 0` 的项。

理由：

- 更接近 Hexo 生成站点的 taxonomy 体验。
- 读者端分类侧栏和标签云不需要展示空分类、空标签。
- 后台管理空分类和空标签是 admin 的职责，不应污染公开导航。

### 5. 后台 admin 不受公开过滤影响

后台分类/标签管理仍返回完整 active 列表，不因为 `articleCount = 0` 被隐藏。

后台是否显示文章数量可以后续单独设计；如果要显示，也应明确它是管理视角数量还是公开视角数量。本轮只裁决公开读者端 `articleCount`。

## 三端变更摘要

### 后端

- `PublicCategoryVO` / `PublicTagVO` 增加 `articleCount`。
- 公开分类/标签 mapper 查询改为聚合公开文章数量。
- 统计口径复用公开文章列表口径：文章未软删除、`PUBLISHED/PASSWORD`、`publish_at <= now`。
- 公开分类/标签列表只返回 `articleCount > 0` 的项。
- 不修改 `t_category` / `t_tag` schema，不新增计数字段。
- 后台 admin 分类/标签接口保持完整 active 列表，不受公开端 `articleCount > 0` 过滤影响。

### 后台管理端

- 当前后台分类/标签管理可以不改。
- 后台列表仍展示完整 active 分类/标签，包括没有公开文章的项。
- 若后续后台也要展示数量，需要另行裁决数量口径，不默认复用公开读者端过滤。

### 前台展示端

- 分类侧栏、标签侧栏、标签页继续显示数量。
- 前台 taxonomy contract 增加 `articleCount`。
- mapper 将后端 `articleCount` 映射到现有组件需要的 `count`，或统一调整 view model 字段。
- 前台不再依赖旧 `/categories.json`、`/tags.json` 中的 `count`，改为消费 V2 公开分类/标签 API。
- 因后端公开列表已过滤 `articleCount > 0`，前台无需额外隐藏空分类/空标签。

## 影响面

### 后端 content

- 分类/标签公开查询 projection 需要补 `articleCount`。
- 分类/标签公开 VO 需要补 `articleCount`。
- `CategoryMapper.xml` / `TagMapper.xml` 公开列表需要聚合文章数量。
- 后端测试至少覆盖：
  - `PUBLISHED` 文章计入数量。
  - `PASSWORD` 文章计入数量。
  - `DRAFT`、`PRIVATE`、`SCHEDULED` 不计入数量。
  - `publish_at > now` 不计入数量。
  - 已软删除文章不计入数量。
  - `articleCount = 0` 的分类/标签不出现在公开列表。

### 前台 blog

- 分类/标签 API contract 增加 `articleCount`。
- 分类和标签 view model 需要保留数量展示能力。
- `CategoryBox.vue`、`TagBox.vue`、`tags.vue` 可继续展示数量。
- 分类页和标签页筛选仍以 slug 为公开 URL 语义，数量字段只负责展示。

### 后台 admin

- 本轮不要求修改后台分类/标签管理页面。
- 后台接口继续用于管理完整 active 分类/标签，不按公开文章数量过滤。
- 如果后续后台需要展示数量，应在后台 API 独立定义字段和口径。

### API 文档

- `docs/handbook/api/category-tag.md` 需要补公开分类/标签响应中的 `articleCount`。
- 文档需要明确 `articleCount` 的公开统计口径。
- 文档需要说明公开读者端只返回 `articleCount > 0` 的分类和标签。

## 待实现时继续确认

1. 公开分类/标签列表是否支持按 `articleCount DESC` 排序。本轮不默认改变排序，优先保持当前分类/标签本体排序规则。
2. `articleCount` 使用 `int` 还是 `long`。建议使用 `int`，除非现有分页总数统一使用 `long`。
3. 聚合 SQL 是否抽公共片段复用公开文章列表条件，避免后续公开口径漂移。

## 建议拆分

### P1：API 契约和业务规则

- 在 handbook 中明确公开分类/标签 `articleCount` 字段。
- 明确统计口径和 `articleCount > 0` 过滤。
- 明确不新增数据库计数字段。

### P2：后端公开查询聚合

- 修改公开分类/标签 projection、VO 和 mapper。
- 补公开分类/标签聚合测试。
- 确认后台 admin 分类/标签接口不受影响。

### P3：前台分类标签接入

- 更新前台 taxonomy contract。
- 将后端 `articleCount` 映射到前台数量展示字段。
- 分类侧栏、标签侧栏、标签页继续展示数量。

### P4：文档回填

- 更新 `docs/handbook/api/category-tag.md`。
- 更新必要的前台接入说明。
