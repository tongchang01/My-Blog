# ADR-0012: 用 ArchUnit 守护架构规则

- 状态：accepted
- 日期：2026-04
- 决策者：项目负责人

## 背景

模块化单体的最大风险是规则被悄悄破坏：今天加一行 import，明天就出现循环依赖、Controller 直连 Mapper、跨模块直接访问对方 infrastructure。仅靠 code review 不可靠。

## 决定

引入 [ArchUnit](https://www.archunit.org/) 作为测试期架构守护：
- 规则集中在 `src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
- 跑 `mvn test` 时自动验证
- 任何违反即构建失败

## 当前规则集

1. `..domain..` 不依赖 `..web..` / `..infrastructure..`
2. `..web..` 不访问 `..infrastructure.persistence.mapper..`
3. `..application..` 不直接访问 MyBatis-Plus Mapper
4. `..common..` 不依赖业务模块
5. 业务模块不互相访问对方 `infrastructure.persistence`

## 理由

- 在 CI 阶段挡住违规，比 review 可靠
- 规则即文档，新人看 `ArchitectureRulesTest` 即可了解架构边界
- 重构时若需要打破规则，必须先改规则并写 ADR

## 后果

正面：架构规则可执行、自动验证
负面：
- 新增模块时需同步更新规则，遗漏会导致新模块无守护
- 跑测试时间略增（可忽略）

后续需关注：
- 新增模块同步更新 ArchUnit 规则
- 规则集成长，必要时拆分多个 test 文件

## 相关

- 相关 rules：`rules/package-layout.md`、`rules/testing-policy.md`
