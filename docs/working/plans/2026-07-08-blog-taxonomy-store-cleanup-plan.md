# 前台分类标签旧兼容 store 清理计划

> 适用范围：`frontend/apps/blog`
> 分支：`feature/blog-taxonomy-store-cleanup`
> 日期：2026-07-08

## 目标

分类、标签相关 UI 直接使用 `features/taxonomy/store`，删除旧 Aurora/Hexo 兼容 store 和旧 JSON API 门面。

## 范围

1. `CategoryBox.vue` 直接使用 `useTaxonomyStore().loadCategories()`。
2. `TagBox.vue` 和 `tags.vue` 直接使用 `useTaxonomyStore().loadTags()`。
3. 删除 `stores/category.ts`、`stores/tag.ts`、`stores/post.ts`。
4. 删除已无消费者的 `api/index.ts`。
5. 删除 `utils/index.ts` 中只供旧 API 使用的 `shuffleArray`、`throttle`、`paginator`，保留 `getDaysTillNow`。
6. 删除已无消费者的旧 `Statistic.class.ts`。
7. 更新当前状态文档，避免继续把这些旧 store 当作待清理项。

## 不做

- 不改公开分类/标签 API。
- 不改分类页、标签页路由和展示样式。
- 不处理留言板、PASSWORD 解锁或部署。

## 验收

1. `rg "@/stores/(category|tag|post)|use(Category|Tag|Post)Store|@/api|fetchAllTags|fetchAllCategories|fetchArchivesList|fetchPostsListBy|shuffleArray|paginator\\(" frontend/apps/blog/src` 无命中。
2. `pnpm --dir frontend/apps/blog test` 通过。
3. `pnpm --dir frontend/apps/blog typecheck` 通过。
4. `pnpm --dir frontend/apps/blog build` 通过。
