# ADR-0009: OpenAPI 基于 springdoc + UI 使用 Knife4j 4.x

- 状态：accepted（**2026-06 修订**：原"用 springdoc 替换 knife4j"修订为"springdoc 提供 OpenAPI 规范 + Knife4j 4.x 提供增强 UI"）
- 日期：2026-04（2026-06 修订）
- 决策者：项目负责人

## 背景

V1 使用 knife4j 2.x 提供 Swagger UI。knife4j 旧版与 Spring Boot 3 兼容性差，且依赖较重。

2026-04 初版决定"用 springdoc-openapi 完全替换 knife4j"。R7 D11 评估时发现：

- Knife4j 4.x 已重构为**基于 springdoc-openapi 的 UI 增强层**，不再像 2.x 那样耦合旧 Swagger
- Knife4j 4.x 中文 / 接口分组 / 接口排序 / 调试增强等功能对个人博客开发体验有实际收益
- 两者**不互斥**，可以同时引入

因此修订决定。

## 决定（2026-06 修订）

V2 同时使用：

- **`springdoc-openapi`**：提供 OpenAPI 3 规范生成（核心扫描 + JSON / YAML 输出）
- **`knife4j-openapi3-jakarta-spring-boot-starter` 4.x**：基于 springdoc 的增强 UI

依赖示例：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.x</version>
</dependency>
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>4.x</version>
</dependency>
```

注解写法仍按 OpenAPI 3：`@Operation` / `@Schema` / `@Tag`（**不用** Knife4j 2.x 的 `@Api` / `@ApiModel`）。

UI 访问路径：
- springdoc 默认：`/swagger-ui/index.html`
- Knife4j：`/doc.html`（生产环境推荐用这个）

## 理由

- **OpenAPI 标准底座**：springdoc 提供规范扫描，工具生态广
- **UI 体验加分**：Knife4j 4.x 接口分组 / 中文搜索 / 个性化 / 调试面板，对个人开发者更友好
- **Spring Boot 3 兼容**：4.x 已完全兼容 Jakarta EE 9
- **解耦风险**：不喜欢 Knife4j UI 时只需移除 starter，springdoc 仍能提供原生 Swagger UI；反之亦然

## 后果

正面：
- 标准化（OpenAPI 3）+ 体验增强（Knife4j 4.x）
- 文档分组按 R5 六模块切分（identity / content / comment / system / stats / common-infra）
- 中文搜索 / 接口排序对开发体验明显改善

负面：
- 引入 2 个依赖（其中 Knife4j 体积约 5MB）
- 注解必须按 OpenAPI 3 写，与 Knife4j 2.x 的旧注解不兼容

## 守护

- pom.xml 不得出现 `knife4j-spring-boot-starter`（2.x 旧包名）或 `springfox-*`
- 代码中不得 import `io.swagger.annotations.*`（Swagger 2.x），统一用 `io.swagger.v3.oas.annotations.*`

## 相关

- 相关 rules：`rules/api-response.md` § Swagger/OpenAPI
- 关联决定：`../../archive/project-handbook/product/decisions-draft.md` R7 D9
