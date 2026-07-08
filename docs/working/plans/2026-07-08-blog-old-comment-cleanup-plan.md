# 前台旧第三方评论与通用 page 清理计划

> 适用范围：`frontend/apps/blog`
> 分支：`feature/blog-old-comment-cleanup`
> 日期：2026-07-08

## 目标

在友链页已经迁到 V2 公开友链接口后，物理删除第一版不再需要的旧第三方评论插件链路和通用静态 page 入口，避免前台继续残留 Gitalk / Valine / Twikoo / Waline / `/pages/{slug}/index.json` 旧依赖。

## 范围

1. 删除通用 `pages/page/[slug].vue`，第一版只保留显式 `about.vue` / `links.vue`。
2. 删除旧 `stores/article.ts` 和 `fetchImplicitPageBySource`，切断 `/pages/{source}/index.json`。
3. `PageContent.vue` 移除旧评论插件 gating，只负责页面内容、统计、侧栏和灯箱。
4. `PostStats.vue` 移除已经无效的插件 props，只保留阅读时长和字数。
5. 删除 `RecentComment.vue` 导出与组件文件，不新增最近评论公开接口。
6. 删除 `useCommentPlugin.ts` 和 `utils/comments/*`。
7. `ThemeConfig.plugins` 只保留当前仍使用的 `copy_protection`，删除旧评论/旧统计字段。
8. `utils/index.ts` 删除只供旧最新评论使用的 `RecentComment`、`formatTime`、`filterHTMLContent`、`cleanPath`；保留仍可能被旧兼容 store 使用的 `paginator`。

## 不做

- 不新增 V2 最近评论接口。
- 不迁移留言板评论。
- 不处理旧 `stores/post.ts` / `stores/tag.ts` 等其它旧 JSON 数据源；它们不是本轮第三方评论和通用 page 清理范围。
- 不改后端。

## 验收

1. `rg "useCommentPlugin|RecentComment|utils/comments|gitalk|valine|twikoo|waline|recent_comments|busuanzi|fetchImplicitPageBySource|pages/\\$\\{source\\}/index\\.json|pages/page" frontend/apps/blog/src` 无命中。
2. `pnpm --dir frontend/apps/blog test` 通过。
3. `pnpm --dir frontend/apps/blog typecheck` 通过。
4. `pnpm --dir frontend/apps/blog build` 通过。
