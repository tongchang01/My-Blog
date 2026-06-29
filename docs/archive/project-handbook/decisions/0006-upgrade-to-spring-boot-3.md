# ADR-0006: 升级到 Spring Boot 3.5.x / Java 17

- 状态：accepted
- 日期：2026-04
- 决策者：项目负责人

## 背景

V1 使用 Spring Boot 2.3.7 + Java 1.8：
- Spring Boot 2.3.x 已 EOL
- 多个第三方依赖（fastjson 1.2.76 等）有 CVE
- Java 8 限制新语法与新 API

## 决定

V2 直接升级到 Spring Boot 3.5.14 + Java 17。

## 理由

- 长期支持版本，安全补丁有保障
- Jakarta EE 9+ 命名空间，避免后续大改
- Java 17 是 LTS，提供 record/sealed/pattern matching 等特性
- Spring Boot 3.x 默认整合 Spring Security 6、Spring 6，配套更新一步到位

## 后果

正面：
- 享受最新性能与安全更新
- 代码可用 record/sealed 改善 DTO/枚举写法
- 长期维护成本低

负面：
- 部分老旧依赖（如 knife4j 旧版、jjwt 0.9）不兼容，须替换
- Servlet API 命名空间从 javax 改为 jakarta，迁移需注意

后续需关注：
- 跟进 Spring Boot 3.x 补丁
- Java 21 发布稳定后评估再升级

## 相关

- 相关 ADR：ADR-0007（JWT 改用 spring-security-oauth2-jose）、ADR-0009（替换 knife4j）
