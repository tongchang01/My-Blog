# 架构现状

> 状态：当前有效
> 适用范围：MyBlog V2 后端架构
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 权威程度：架构入口

## 本文档回答什么问题

本目录记录 MyBlog V2 当前架构长什么样。这里描述的是当前事实，不记录历史方案，也不承载未来计划。

## 文档清单

| 文档 | 回答的问题 | 当前状态 |
|------|------------|----------|
| `module-map.md` | V2 有哪些模块、模块间能怎么依赖 | 已迁移校准 |
| `auth-flow.md` | 登录、JWT、refresh、logout、改密怎么走 | 待迁移 |
| `request-flow.md` | 一个请求从 Controller 到数据库如何流转 | 待迁移 |
| `persistence-strategy.md` | MyBatis-Plus、XML、Repository、Flyway 如何分工 | 待迁移 |
| `schema-design.md` | 当前 V2 表结构和 DDL 设计 | 待迁移 |

## 写作规则

- 只写当前已经落地或已经冻结的结构。
- 未来计划和争议事项写入 `../start-here/open-issues.md`。
- 设计原因写入 `../adr/`。
- 编码约束写入 `../rules/`。
- 架构变化完成后，必须同步更新本目录。

## 当前架构摘要

V2 后端采用模块化单体：

```text
com.tyb.myblog.v2
├── common
├── identity
├── content
├── comment
├── system
└── stats
```

业务模块内部遵循 `web -> application -> domain <- infrastructure` 的四层结构。跨业务模块协作只允许通过对方 application 层公开契约，禁止访问对方 domain、web 或 infrastructure 内部实现。
