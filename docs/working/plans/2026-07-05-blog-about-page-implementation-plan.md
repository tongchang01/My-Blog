# 前台关于页 V2 站点配置接入实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 前台 About 页停止读取旧 `/pages/about/index.json`，改为展示 V2 公开站点配置返回的 `aboutMd`。

**架构：** 不新增后端接口、不新增后台入口、不新增前端数据层。About 页直接读取现有 `useSiteSettingsStore().settings.aboutMd`，复用 `renderMarkdown` 生成安全 HTML，再继续交给现有 `PageContent` 展示。

**技术栈：** Vue 3、Pinia、Vue Router、markdown-it、Vitest、vue-tsc。

---

## 范围

本批只处理 O-018。搜索 O-017、友链、评论、留言、访问统计不进入本批。

## 现状

- `frontend/apps/blog/src/pages/about.vue` 当前调用 `useArticleStore().fetchArticle('about')`。
- `fetchArticle('about')` 最终请求旧 page JSON。
- `frontend/apps/blog/src/features/site-settings/store.ts` 已加载公开站点配置。
- `frontend/apps/blog/src/features/site-settings/model.ts` 已有 `aboutMd: string | null`。
- `frontend/apps/blog/src/shared/markdown/render.ts` 已配置 `html: false`，可复用。
- `frontend/apps/blog/src/components/PageContent.vue` 已包含 `Profile`、`Toc`、`PostStats` 和正文骨架。

## 不做

- 不新增 `GET /api/public/about`。
- 不改后端 schema。
- 不改后台站点配置表单。
- 不新建 About 专用大组件。
- 不恢复旧 page JSON 兜底。
- 不在 About 页接入评论。

## Task 1: About 页切换到 site settings

**Files:**
- Modify: `frontend/apps/blog/src/pages/about.vue`

- [ ] **Step 1: 修改数据源**

将 `about.vue` 中的 `useArticleStore` 和旧 `fetchArticle('about')` 删除，改用站点配置。

核心实现保持这个形状：

```ts
import { computed, onMounted, onUnmounted } from 'vue'
import { useSiteSettingsStore } from '@/features/site-settings/store'
import { Page } from '@/models/Article.class'
import { renderMarkdown } from '@/shared/markdown/render'

const siteSettingsStore = useSiteSettingsStore()

const pageData = computed(() => {
  const page = new Page()
  page.title = pageTitle.value
  page.content = siteSettingsStore.settings.aboutMd
    ? renderMarkdown(siteSettingsStore.settings.aboutMd)
    : ''
  page.comments = false
  return page
})
```

保留：

```ts
commonStore.setHeaderImage(defaultCover)
updateTitle()
```

删除：

```ts
const articleStore = useArticleStore()
pageData.value = await articleStore.fetchArticle('about')
```

- [ ] **Step 2: 确认空内容行为**

`aboutMd` 为空时，`page.content` 保持空字符串，让现有 `PageContent` 骨架显示。不要伪造旧内容。

- [ ] **Step 3: 本地验证**

运行：

```bash
pnpm --dir frontend/apps/blog typecheck
pnpm --dir frontend/apps/blog test
```

预期：

```text
typecheck 通过
vitest 全部通过
```

- [ ] **Step 4: 提交**

提交前检查：

```bash
git diff --stat
git status --short
```

提交：

```bash
git add frontend/apps/blog/src/pages/about.vue
git commit -m "前台：关于页接入站点配置"
```

## Task 2: 回填文档状态

**Files:**
- Modify: `docs/handbook/frontend/blog/integration-status.md`
- Modify: `docs/handbook/start-here/open-issues.md`

- [ ] **Step 1: 更新前台 Blog 联调状态**

将关于页从待补齐列表移到已完成列表，说明：

```text
关于页已接入公开站点配置 `aboutMd`，不再读取旧 `/pages/about/index.json`。
```

- [ ] **Step 2: 关闭 O-018**

将 O-018 状态改为已关闭，关闭原因写明：

```text
前台 About 页已读取 `siteSettings.aboutMd`，使用现有 `renderMarkdown` 渲染 Markdown，保留 raw HTML 禁用策略；为空时走现有骨架空态；后端和后台未新增 schema 或入口。
```

- [ ] **Step 3: 文档验证**

运行：

```bash
rg -n "O-018|aboutMd|/pages/about/index.json|关于页" docs/handbook/start-here/open-issues.md docs/handbook/frontend/blog/integration-status.md
```

预期：

```text
O-018 标记为已关闭
前台状态文档不再把关于页列为待补齐
```

- [ ] **Step 4: 提交**

提交前检查：

```bash
git diff --stat
git status --short
```

提交：

```bash
git add docs/handbook/frontend/blog/integration-status.md docs/handbook/start-here/open-issues.md
git commit -m "文档：关闭前台关于页接入事项"
```

## 阶段验收

- [ ] `frontend/apps/blog/src/pages/about.vue` 不再导入 `useArticleStore`。
- [ ] 代码库中 About 页不再请求旧 `/pages/about/index.json`。
- [ ] About Markdown 仍通过 `renderMarkdown` 渲染。
- [ ] raw HTML 禁用策略由现有 `renderMarkdown` 测试覆盖。
- [ ] `pnpm --dir frontend/apps/blog typecheck` 通过。
- [ ] `pnpm --dir frontend/apps/blog test` 通过。
- [ ] 文档中 O-018 已关闭。

## 提交拆分

1. `前台：关于页接入站点配置`
2. `文档：关闭前台关于页接入事项`
