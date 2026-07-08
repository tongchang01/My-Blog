# 前台友链 V2 数据源迁移计划

> 适用范围：`frontend/apps/blog`
> 分支：`feature/blog-friend-links-v2`
> 日期：2026-07-08

## 目标

把前台友链页和页脚友链从旧 `/pages/links/index.json` / `avatarWall` 数据源迁到 V2 后端已有的 `GET /api/public/friend-links`。

## 范围

本轮只做第一版发布需要的最小闭环：

1. 新增前台公开友链 API 包装，读取 `id/name/url/avatarUrl/description`。
2. 把后端友链 DTO 映射成现有 `Link` 组件可消费的 `nick/avatar/link/description/label`。
3. `pages/links.vue` 改为展示公开友链简单卡片。
4. `FooterLink.vue` 当前没有被挂载，本轮直接删除，避免保留旧 `avatarWall` 数据源。
5. `links.vue` 不再使用旧 `LinkBox` 头像墙入口；`LinkBox` / `LinkAvatar` / `LinkCategoryList` 若确认无其它引用，一并删除。

## 不做

- 不新增后端接口。
- 不恢复旧头像墙、分组、申请说明、友链评论和页面统计。
- 不做在线友链申请；第一版仍按文档走邮件或 GitHub issue。
- 不清理通用 page 评论和旧第三方评论工具；等友链页脱离后单独做物理清理。

## 验收

1. `links.vue` 不再调用 `articleStore.fetchArticle('links')`。
2. `rg "fetchArticle\\('links'\\)|avatarWall|/pages/links/index.json" frontend/apps/blog/src` 无主动旧调用。
3. 友链 DTO 映射有单元测试。
4. `pnpm --dir frontend/apps/blog test` 通过。
5. `pnpm --dir frontend/apps/blog typecheck` 通过。
6. `pnpm --dir frontend/apps/blog build` 通过。
