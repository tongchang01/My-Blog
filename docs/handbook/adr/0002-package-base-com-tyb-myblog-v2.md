# ADR-0002：基础包使用 com.tyb.myblog.v2

> 状态：当前有效
> 适用范围：V2 Java 代码
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 权威程度：ADR

## 背景

V1 使用原项目命名空间，V2 需要与旧实现隔离并提供稳定的架构测试边界。

## 决策

V2 Java 基础包固定为 `com.tyb.myblog.v2`。V2 业务代码不得放入旧包名或并列根包。

## 结果

- V1 与 V2 类型不会被误引用。
- ArchUnit 可以从固定根包识别全部模块。
- 修改基础包属于全局架构变更，需要新的 ADR 和完整验证。
