# 包结构与依赖规则

> 状态：当前有效
> 适用范围：`MyBlog-springboot-v2/` Java 代码
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`、`MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`
> 权威程度：规则

基础包固定为 `com.tyb.myblog.v2`。顶层包含 common，以及 identity、content、comment、system、stats 五个业务模块；禁止恢复 `com.tyb.myblog.v2.infrastructure` 顶层技术包。

## 模块四层

| 层 | 职责 | 禁止 |
| --- | --- | --- |
| web | Controller、Request、VO、校验、OpenAPI | Entity、Mapper、SQL、核心业务规则 |
| application | Command/Query/Result、用例编排、事务、跨模块契约 | Servlet、Web DTO、Mapper、infrastructure 类型 |
| domain | 领域对象、枚举、规则、Repository 端口 | Spring Web/Security、MyBatis、Servlet、系统时间直读 |
| infrastructure | Entity、Mapper、XML、Repository 与外部适配 | Controller、业务规则、依赖 web |

common 提供响应、异常、安全接入、token 端口、时间、持久化配置、邮件和存储等通用能力。common 不依赖业务模块，业务模块不依赖 `common.security` 的具体实现。

## 跨模块

跨模块只依赖对方 application 暴露的能力。例如 comment 校验文章、content 校验附件、stats 查询文章标题。禁止访问对方 domain、web、Mapper、Entity 或 Repository 实现。

Web 只允许使用 `ArchitectureRulesTest` 白名单中的稳定领域枚举；其他领域类型需转换为 application contract 或 Web VO。

## 自动守护

`ArchitectureRulesTest` 验证：

- domain 与框架、上层和 infrastructure 隔离；
- web/application 与 infrastructure 隔离；
- common 的依赖方向；
- 跨模块只能访问 application；
- 五个业务模块无循环依赖；
- domain 不直接读取系统时间；
- 旧顶层 infrastructure 包不存在。

新增模块或调整允许依赖时，必须同步修改 `../architecture/module-map.md`、相关 ADR 和 ArchUnit 测试。详细设计见 `../architecture/request-flow.md`。
