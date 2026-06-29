# 后台 Admin

> 状态：待校准
> 适用范围：V2 后台管理端
> 最后校准：2026-06-29
> 对应代码：`frontend/apps/admin/`
> 权威程度：后台规格入口

## 本文档回答什么问题

本文档记录后台管理端的技术基线、范围和当前文档收口方式。业务页完成度见 `integration-status.md`，接口契约见 `../../api/`。

## 技术基线

- Vue 3、TypeScript、Vite。
- Element Plus、Pinia、Vue Router、vue-i18n。
- Axios、Vitest、Vue Test Utils、happy-dom。
- 中文、日文、英文三语界面。

## 当前范围

后台 admin 覆盖登录、会话刷新、静态路由、ADMIN/DEMO 权限体验、仪表盘、文章、分类标签、评论、友链、附件、站点配置和作者资料。

## 文档收口

`frontend/apps/admin/docs/` 中的日期计划、设计和验收材料属于过程材料。后续只把当前事实提炼到本目录；原文归档或保留跳转，不继续作为长期权威源。

