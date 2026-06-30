# 前台关于页 aboutMd 接入实施思路

> 状态：实施思路
> 适用范围：前台 blog、后端 system、后台 admin、Markdown 渲染
> 最后校准：2026-06-30
> 权威程度：过程计划，裁决后需提炼到 handbook

## 本文档回答什么问题

本文档记录已经裁决的前台 About 页迁移方向，作为后续拆分正式实现任务前的实施依据。

本文档不直接替代 `docs/handbook/start-here/open-issues.md`、`docs/handbook/api/site-config.md` 或前台接入状态文档。实现完成后，需要把最终结论提炼回 handbook。

## 前台原始语义

当前 About 页仍沿用旧 page 数据模型：

```text
/pages/about/index.json
```

页面实现：

- `about.vue` 调用 `articleStore.fetchArticle('about')`。
- `PageContent` 负责渲染页面主体。
- `PageContent` 当前包含标题、正文区域、`PostStats`、右侧 `Profile` 和 `Toc`。

这套结构来自旧 Aurora/Hexo page 语义。迁移到 V2 后，About 内容不再作为一篇旧 page 维护，而是站点配置的一部分。

## 已定方向

### 1. 数据源迁移到 V2 站点配置

About 页不再请求旧 `/pages/about/index.json`。

新的数据源为公开站点配置中的：

```text
siteSettings.aboutMd
```

后端已有能力：

- `PublicSiteConfigVO.aboutMd`
- 后台三语字段 `aboutMdZh`、`aboutMdJa`、`aboutMdEn`
- 后端按语言 fallback 返回当前语言 About Markdown

### 2. 使用现有 Markdown 渲染策略

前台使用现有 `renderMarkdown(aboutMd)` 渲染 About 内容。

必须保持：

- `html: false`
- 不直接把 `aboutMd` 原文作为可信 HTML 插入页面
- 外链安全属性按现有 Markdown renderer 处理

About 页 Markdown 渲染策略应与公开文章正文保持一致。

### 3. 现阶段保留旧视觉组件

现阶段尽量保留旧 About 页面视觉和组件，包括：

- `Profile`
- `Toc`
- `PostStats`

本轮只迁移数据源，不主动调整 About 页视觉结构。`PostStats` 是否长期适合 About 页，后续可单独评估。

### 4. 空内容处理

如果 `aboutMd` 为空：

- 显示空态或骨架。
- 不伪造旧 page。
- 不请求旧 `/pages/about/index.json` 兜底。

### 5. 后端和后台原则上不改

后端和后台已有 About Markdown 能力，本轮原则上不新增：

- schema 字段
- 后台入口
- 公开 API 字段

如果实现时发现现有站点配置加载时序或前台 store 不能稳定提供 `aboutMd`，只在前台接入层修正。

## 三端变更摘要

### 后端

- 原则上不改。
- 继续通过公开站点配置返回 `aboutMd`。
- 继续按语言 fallback。
- 确认 `aboutMd` 是 Markdown 原文，不返回 HTML。
- 若测试不足，可补站点配置公开查询测试。

### 后台管理端

- 原则上不改。
- 继续在站点配置里维护三语 About Markdown。
- 不新增独立 About 页面管理入口。

### 前台展示端

- About 页改为读取 site settings 中的 `aboutMd`。
- 不再调用 `articleStore.fetchArticle('about')`。
- 不再请求旧 `/pages/about/index.json`。
- 使用 `renderMarkdown(aboutMd)` 得到 HTML 后再渲染。
- 尽量保留当前 `PageContent` 视觉和 `Profile`、`Toc`、`PostStats`。
- `aboutMd` 为空时显示空态或骨架。

## 影响面

### 前台 blog

- `about.vue` 数据来源需要从旧 article store 切换到 site settings store。
- 需要为 About Markdown 构造页面展示模型，供现有 `PageContent` 或后续 About 专用组件使用。
- 需要确认 Toc 能从渲染后的 Markdown 标题中继续工作。
- 需要确认 `PostStats` 在 about 数据模型缺少阅读时间、评论数时不会报错。

### 后端 system

- 当前 API 已有 `aboutMd`，原则上不新增字段。
- 如测试不足，补充公开站点配置语言 fallback 和 `aboutMd` 返回测试。

### 后台 admin

- 当前不改。
- 后续如果 About 编辑体验不足，再单独讨论后台站点配置页面优化。

### API 文档

- `docs/handbook/api/site-config.md` 已说明 `aboutMd` 返回 Markdown 原文，不返回 HTML。
- 后续可在前台接入状态文档中标记 About 页已接入 V2 site config。

## 待实现时继续确认

1. About 页空态文案。
2. 是否复用 `PageContent`，还是拆一个轻量 AboutContent 组件。本轮倾向先复用，减少视觉变化。
3. `PostStats` 在 About 页长期是否保留。本轮先保留。

## 建议拆分

### P1：前台 About 数据源迁移

- About 页读取 site settings。
- 使用 `renderMarkdown` 渲染 About Markdown。
- 保留当前视觉结构。

### P2：空态和安全验证

- 处理 `aboutMd` 为空。
- 补前台 Markdown 渲染相关测试或页面测试。
- 确认 raw HTML 不会被渲染。

### P3：文档回填

- 更新前台接入状态。
- 如实现时调整 API 文档，同步 `docs/handbook/api/site-config.md`。
