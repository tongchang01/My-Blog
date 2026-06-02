# ADR-0009: 用 springdoc-openapi 替换 knife4j

- 状态：accepted
- 日期：2026-04
- 决策者：项目负责人

## 背景

V1 使用 knife4j 2.x 提供 Swagger UI。knife4j 旧版与 Spring Boot 3 兼容性差，且依赖较重。

## 决定

V2 使用 `springdoc-openapi`（OpenAPI 3 规范）。

## 理由

- 官方推荐方案，活跃维护
- 原生支持 Spring Boot 3、Jakarta EE 9
- 直接遵循 OpenAPI 3 规范，工具生态广
- 配合 `@Operation`、`@Schema` 等注解，文档与代码同步

## 后果

正面：标准化、轻量、与 Spring Boot 3 兼容
负面：UI 风格与 knife4j 不同，需要适应；中文检索/导出等增强功能需自行评估

## 相关

- 相关 rules：`rules/api-response.md` §9 Swagger/OpenAPI
