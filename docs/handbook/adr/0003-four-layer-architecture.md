# ADR-0003：业务模块采用四层架构

> 状态：当前有效
> 适用范围：五个 V2 业务模块
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`、`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
> 权威程度：ADR

## 背景

传统 Controller、Service、Mapper 三层容易把协议、用例、业务规则和持久化细节混在一起。完整六边形架构对当前规模增加过多样板。

## 决策

每个业务模块使用 `web / application / domain / infrastructure` 四层：

- web：HTTP 接入和协议映射。
- application：用例、事务和跨模块公开能力。
- domain：业务模型、规则和端口。
- infrastructure：持久化和外部服务适配。

## 结果

层间依赖由 `ArchitectureRulesTest` 守护。Application 和 domain 不依赖具体基础设施，Web 不直接访问 Mapper 或 Entity。

详细规则见 `../rules/package-layout.md`。
