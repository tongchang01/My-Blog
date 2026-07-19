# 前端实现说明

> 状态：当前有效
> 适用范围：V2 博客端与管理端
> 最后校准：2026-07-19
> 对应代码：`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：前端导航

| 应用 | 当前说明 | 本地端口 | 路由模式 |
| --- | --- | --- | --- |
| blog | [读者端](blog/README.md) | 5173 | history，所有公开路径统一带 `zh / ja / en` |
| admin | [管理端](admin/README.md) | 8848 | hash |

接口字段以 `../api/` 为准，跨端功能状态以 `../product/feature-inventory.md` 为准，未完成事项只登记到 `../start-here/open-issues.md`。
