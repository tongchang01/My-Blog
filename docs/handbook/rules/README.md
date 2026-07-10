# 开发规则

> 状态：当前有效
> 适用范围：MyBlog V2
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：规则入口

| 文档 | 约束范围 |
| --- | --- |
| [`documentation.md`](documentation.md) | 文档目录、权威源、生命周期、格式和更新触发器 |
| [`package-layout.md`](package-layout.md) | Java 模块、分层、依赖方向和 ArchUnit 边界 |
| [`api-response.md`](api-response.md) | HTTP 响应、分页、ID、错误码和状态码 |
| [`comment-style.md`](comment-style.md) | 代码注释与 Javadoc |
| [`error-handling.md`](error-handling.md) | 异常分类、抛出位置和全局转换 |
| [`security-baseline.md`](security-baseline.md) | 认证、授权、密码、CORS、限流和敏感信息 |
| [`sql-placement.md`](sql-placement.md) | MyBatis-Plus、Wrapper、XML 和迁移脚本的职责 |
| [`testing-policy.md`](testing-policy.md) | 单元、Web、集成、架构和数据库测试 |

规则描述“必须怎样做”，架构事实放入 `../architecture/`，决策理由放入 `../adr/`，未决事项放入 `../start-here/open-issues.md`。
