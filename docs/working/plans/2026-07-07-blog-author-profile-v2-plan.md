# 前台作者资料 V2 数据源计划

> 状态：执行中
> 适用范围：`frontend/apps/blog/`
> 日期：2026-07-07

## 目标

让首页右侧作者卡片和移动菜单不再读取旧 `/authors/blog-author.json` 和 `/statistic.json`。

## 范围

本批只做作者资料展示：

- `Profile.vue`
- `MobileMenu.vue`
- 作者资料 view model / store
- 旧作者和旧统计 API 活跃调用清理
- 前台单测、typecheck、build

不做：

- 友链页和页脚友链。
- 后端公开作者 API。
- 站点配置 schema 扩展。
- 字数统计新接口。

## 方案

复用现有 V2 能力，不新增后端接口：

- 作者名、头像、描述、社交链接来自 `site-settings` 映射出的 `themeConfig.site` / `themeConfig.socials`。
- 文章数用 `GET /api/public/articles?page=1&size=1` 的 `total`。
- 分类数用 `GET /api/public/categories` 返回数组长度。
- 标签数用 `GET /api/public/tags` 返回数组长度。
- 字数统计当前无 V2 口径，第一版展示为 `0`，后续需要时再补真实统计。

## 验收

- `rg "/authors|/statistic.json|fetchStat|useAuthorStore" frontend/apps/blog/src` 不再命中活跃代码。
- `Profile.vue` 和 `MobileMenu.vue` 使用同一个 V2 作者资料 store。
- `pnpm --dir frontend/apps/blog test` 通过。
- `pnpm --dir frontend/apps/blog typecheck` 通过。
- `pnpm --dir frontend/apps/blog build` 通过。
