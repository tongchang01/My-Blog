# 业务规格

> 状态：当前有效
> 适用范围：MyBlog V2 产品与业务规则
> 最后校准：2026-06-29
> 权威程度：业务规格参考

## 与代码层文档的区别

| 类型 | 关注 | 例 |
|------|------|----|
| `rules/` / `architecture/` / `adr/` | **怎么实现** | "Controller 返回 ApiResponse" |
| `product/` | **要实现什么** | "用户可以给文章点赞" |

`product/` 不写技术，只写业务。技术实现见 architecture、rules 和 adr。

## 当前文件

| 文件 | 内容 |
|------|------|
| `feature-inventory.md` | V1 全部功能清单和 V2 去留决策 |
| `use-cases.md` | GUEST、DEMO、ADMIN、系统任务能做什么 |
| `business-rules.md` | 评论审核、文章可见性、权限模型等关键业务规则 |
| `data-model.md` | 业务实体与关系 |
| `er-diagram.md` | ER / 领域关系图 |

## 维护规则

- 业务规格只记录当前仍有效的产品结论；历史讨论原文保留在 `../../archive/project-handbook/product/`。
- 涉及实现方式时，只链接 architecture、rules、api 或 adr，不在本目录重复展开。
- 未完成或争议事项登记到 `../start-here/open-issues.md`，不在业务规格里并行维护待办。
