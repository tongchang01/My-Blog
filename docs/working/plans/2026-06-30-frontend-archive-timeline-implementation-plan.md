# 前台归档时间线实施思路

> 状态：实施思路
> 适用范围：前台 blog、后端 content、公开 API
> 最后校准：2026-06-30
> 权威程度：过程计划，裁决后需提炼到 handbook

## 本文档回答什么问题

本文档记录已经裁决的公开归档页处理方向，作为后续拆分正式实现任务前的实施依据。

本文档不直接替代 `docs/handbook/start-here/open-issues.md`、`docs/handbook/api/article.md` 或业务规则文档。实现完成后，需要把最终结论提炼回 handbook。

## 前台原始语义

Aurora/Hexo 原始归档页请求：

```text
/archives/{page}.json
```

该接口返回一页文章数据，前台 `Archives` 模型再按文章日期分组成 `month/year/posts`，页面渲染成时间线：

- 月份标题。
- 该月下的文章日期。
- 文章标题。
- 文章摘要文本。

当前归档页实际使用的文章字段只有：

- `title`
- `slug`
- `date`
- `text`

归档页没有展示分类、标签、封面、评论数或阅读时间。

## 已定方向

### 1. 保留归档时间线体验

归档页继续作为文章时间线页面，不降级为普通文章列表，也不改成只有月份索引的页面。

### 2. 新增公开归档时间线接口

建议接口：

```text
GET /api/public/archives?page=1&size=12&lang=zh
```

接口语义：

- 返回公开归档时间线数据。
- 分页单位按文章数计算。
- `total` 表示符合公开口径的文章总数，不是月份总数。
- 后端返回按年月分组后的 records，前台只负责渲染时间线。

### 3. 返回结构

建议响应结构：

```json
{
  "records": [
    {
      "yearMonth": "2026-05",
      "year": 2026,
      "month": 5,
      "articles": [
        {
          "title": "文章标题",
          "slug": "article-slug",
          "publishedAt": "2026-05-28T10:00:00",
          "summary": "文章摘要"
        }
      ]
    }
  ],
  "total": 42,
  "page": 1,
  "size": 12
}
```

说明：

- `records` 按 `publishedAt DESC, id DESC` 的文章顺序分组。
- 单页中如果跨多个月，返回多个年月分组。
- 归档文章项只返回 `title`、`slug`、`publishedAt`、`summary`。
- 不返回正文。
- 不返回分类、标签、封面、评论数、阅读时间等归档页当前未展示字段。

### 4. 公开统计口径

归档时间线使用与公开文章列表一致的公开口径：

- 文章未软删除。
- 状态为 `PUBLISHED` 或 `PASSWORD`。
- `publish_at <= now`。

排除：

- `DRAFT`。
- `PRIVATE`。
- `SCHEDULED`。
- `publish_at > now`。
- 已软删除文章。

`PASSWORD` 文章进入归档，因为公开列表可见其元数据，正文仍受密码保护。

### 5. 与首页槽位无关

置顶和推荐不影响归档。

归档是时间线能力，应展示所有公开可见文章的历史顺序。即使文章被设置为 `PINNED` 或 `FEATURED`，也仍按 `publish_at DESC, id DESC` 进入归档。

### 6. 本轮不做月份索引接口

本轮不新增单独的月份聚合接口，例如 `yearMonth/articleCount` 列表。

如果后续要做归档侧栏、月份导航或 SEO 月份页，可以在当前归档时间线接口之外另行设计。

## 三端变更摘要

### 后端

- 新增公开归档时间线接口，建议 `GET /api/public/archives?page=1&size=12&lang=zh`。
- 查询公开可见文章，按 `publish_at DESC, id DESC` 排序，并按文章数分页。
- 将当前页文章按年月分组后返回。
- 返回文章项字段限定为 `title`、`slug`、`publishedAt`、`summary`。
- 公开口径复用公开文章列表：未软删除、`PUBLISHED/PASSWORD`、`publish_at <= now`。
- 不新增月份聚合接口，不改变 `/api/public/articles?archiveMonth=yyyy-MM` 的现有筛选能力。

### 后台管理端

- 当前后台管理端不需要修改。
- 归档是公开读者端展示能力，不影响后台文章列表、分类标签管理或文章编辑。

### 前台展示端

- 归档页改为消费 V2 公开归档时间线接口。
- 保留现有时间线视觉。
- 不再请求旧 `/archives/{page}.json`。
- 不再由前台自行从全量文章分组；按后端返回的年月分组渲染。
- 归档文章跳转使用公开文章 slug 路由，与 O-014 的公开 URL 策略保持一致。

## 影响面

### 后端 content

- 增加归档时间线查询模型和响应 VO。
- 增加 mapper 查询或 repository 方法。
- 复用公开文章列表的状态、发布时间和软删除过滤条件，避免公开口径漂移。
- 后端测试至少覆盖：
  - 按文章数分页。
  - 单页跨多个月时返回多个分组。
  - `PUBLISHED` 和 `PASSWORD` 文章进入归档。
  - `DRAFT`、`PRIVATE`、`SCHEDULED` 不进入归档。
  - `publish_at > now` 不进入归档。
  - 软删除文章不进入归档。
  - 置顶/推荐不改变归档排序。

### 前台 blog

- 增加归档接口 contract 和 mapper。
- 调整 `archives.vue` 数据来源。
- 保留当前时间线 UI 和分页器。
- 归档文章链接迁移到 slug 主导公开路由。

### API 文档

- 补公开归档时间线接口文档。
- 明确分页单位是文章数。
- 明确 `total` 是文章总数。
- 明确归档文章项字段和公开过滤口径。

## 待实现时继续确认

1. `month` 字段返回数字 `1-12`，还是返回前台 i18n key。本轮建议返回数字，由前台本地化月份。
2. `summary` 是否按语言 fallback，与公开文章列表保持一致。
3. 归档接口是否放入 `article.md`，还是新建 `archive.md`。本轮建议先放入公开文章相关 API 文档。

## 建议拆分

### P1：API 契约和业务规则

- 在 handbook 中明确归档时间线接口、分页口径和返回字段。
- 明确归档公开过滤口径。
- 明确本轮不做月份聚合接口。

### P2：后端归档查询

- 增加归档查询 VO / DTO。
- 增加 mapper 查询和应用服务。
- 补归档查询测试。

### P3：前台归档页迁移

- 增加归档 API contract。
- 调整归档页 store / mapper。
- 归档页改接 V2 接口并保持时间线 UI。

### P4：文档回填

- 更新 `docs/handbook/api/article.md` 或新增公开归档 API 文档。
- 同步前台接入状态文档。
