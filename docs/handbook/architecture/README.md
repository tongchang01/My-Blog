# 后端架构

> 状态：当前有效
> 适用范围：MyBlog V2 后端
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/`、`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
> 权威程度：架构入口

| 文档 | 内容 |
| --- | --- |
| [`module-map.md`](module-map.md) | 模块、分层、依赖方向和 ArchUnit 守护 |
| [`request-flow.md`](request-flow.md) | HTTP 请求、事务、错误和跨模块调用流程 |
| [`auth-flow.md`](auth-flow.md) | 登录、JWT、refresh、退出、改密和公开作者资料 |
| [`persistence-strategy.md`](persistence-strategy.md) | Repository、MyBatis-Plus、XML、Flyway 和测试策略 |
| [`schema-design.md`](schema-design.md) | 当前迁移、表职责、审计列和数据不变量 |

代码、迁移和测试是架构事实源。本目录解释当前结构，不保存实施计划或旧版本对照。
