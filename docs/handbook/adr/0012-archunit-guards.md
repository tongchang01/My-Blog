# ADR-0012：使用 ArchUnit 守护架构边界

> 状态：当前有效
> 适用范围：V2 后端架构测试
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
> 权威程度：ADR

## 背景

模块化单体的依赖边界容易被普通 import 逐步破坏，仅依赖人工审查无法持续保证一致性。

## 决策

使用 ArchUnit 在测试阶段验证以下边界：

- domain 不依赖 application、web、infrastructure 和框架 API，也不直接读取系统时间；
- web 与 application 不直接依赖 infrastructure，application 不绑定 Servlet；
- common 不反向依赖业务模块，业务模块不依赖 common.security 实现；
- 跨业务模块只允许依赖对方 application 能力；
- 五个业务模块不存在循环依赖；
- 禁止恢复旧的顶层 infrastructure 包。

测试还包含故意违规的 fixture，用于证明关键规则确实能够失败。

## 结果

架构边界成为构建的一部分。新增模块或调整依赖方向时，必须同步更新架构说明、ADR 和测试规则。
