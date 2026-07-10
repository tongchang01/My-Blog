# ADR-0001：采用模块化单体

> 状态：当前有效
> 适用范围：V2 后端架构
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`、`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
> 权威程度：ADR

## 背景

项目需要清晰业务边界，但部署规模不需要服务注册、网关、分布式事务和链路追踪等微服务基础设施。传统技术分层单体无法约束跨业务依赖。

## 决策

V2 使用单个 Spring Boot 部署单元，并按业务域组织模块。模块边界和依赖方向由 ArchUnit 自动验证。

## 结果

- 部署和本地运行保持简单。
- 业务代码按 identity、content、comment、system、stats 划分。
- 跨模块调用必须通过 application 能力。
- 新增模块需要同步更新架构文档、ADR 和 ArchUnit。

模块边界见 `../architecture/module-map.md`。
