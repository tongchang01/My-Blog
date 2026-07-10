# ADR-0006：使用 Spring Boot 3.5 与 Java 17

> 状态：当前有效
> 适用范围：V2 后端构建基线
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/pom.xml`
> 权威程度：ADR

## 背景

V1 的 Spring Boot 2.3 与 Java 8 已不适合作为新主线。V2 需要受支持的 Jakarta、Spring Security 和测试生态。

## 决策

当前基线为 Spring Boot 3.5.14、Java 17 和 Maven 3.9.x。Maven Enforcer 限定 Java `[17,18)`、Maven `[3.9.0,4.0.0)` 并检查依赖收敛。

## 结果

升级 Java 或 Spring Boot 必须单独评估依赖兼容、运行环境、迁移脚本和完整测试，不能只修改版本号。
