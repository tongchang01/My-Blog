# 产品与业务规格

> 状态：当前有效
> 适用范围：MyBlog V2 产品能力与业务不变量
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/`、`frontend/apps/`
> 权威程度：业务规格导航

本目录描述当前产品已经支持什么，以及业务状态和约束。接口字段见 `../api/`，实现边界见 `../architecture/`，开发约束见 `../rules/`。

| 文件 | 内容 |
| --- | --- |
| [功能清单](feature-inventory.md) | 当前后端、博客端和管理端覆盖范围 |
| [业务规则](business-rules.md) | 权限、文章、评论、配置和统计不变量 |
| [领域模型](data-model.md) | 聚合、值对象和模块归属 |
| [关系图](er-diagram.md) | 模块与数据表的逻辑关系 |

未完成事项只登记到 `../start-here/open-issues.md`，不在产品规格中复制待办列表。已经失效的产品讨论由 Git 历史保留。
