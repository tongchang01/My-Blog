# ADR-0009：springdoc 生成 OpenAPI，Knife4j 提供本地 UI

> 状态：当前有效
> 适用范围：V2 后端接口文档
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/pom.xml`、`MyBlog-springboot-v2/src/main/resources/application-local.yml`、`MyBlog-springboot-v2/src/main/resources/application-prod.yml`
> 权威程度：ADR

## 背景

项目需要 OpenAPI 契约测试和本地调试界面。Knife4j 4.x 基于 springdoc，可作为 UI 增强层，不需要旧 Swagger 2 注解。

## 决策

- springdoc 2.8.8 负责 OpenAPI 3 规范生成。
- Knife4j 4.5.0 提供 `/doc.html` 本地调试界面。
- 代码只使用 `io.swagger.v3.oas.annotations`。
- local/test 启用规范和 UI，prod 全部关闭且不加入公开白名单。

## 结果

Controller 和 Web 模型由 OpenAPI 测试约束。生产部署不暴露 `/doc.html`、`/swagger-ui/**` 或 `/v3/api-docs/**`。
