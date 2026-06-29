# 后端 V2 第一批审查问题修复设计

## 1. 目标

在不扩大首版产品范围的前提下，优先修复后端发布前审查中已经具备明确答案、且会影响本地前端联调或测试可信度的问题。

本批完成后：

- local Profile 与基础配置组合后保留全部公开业务端点；
- local/test 只声明各自新增的公开端点，不再复制基础白名单；
- 配置测试能够阻止 Profile 覆盖基础白名单的回归；
- 审计字段集成测试不再向共享 MySQL 测试库遗留临时表；
- 与本批事实直接相关的阶段文档、规则链接和生产注释恢复准确；
- H2 与本地 MySQL 广泛回归继续保持零失败、零错误。

## 2. 范围

### 2.1 本批处理

1. Important-3：local Profile 遗漏匿名端点。
2. Minor-1：SQL 规则引用错误的 ADR 文件名。
3. Minor-3：测试基线和已知缺测清单过时。
4. Minor-5：生产注释仍描述旧迁移阶段或旧库约束。
5. Minor-6：`AuditFieldHandlerIntegrationTest` 遗留临时表。
6. Important-1 中与当前阶段直接相关的入口文档同步。

### 2.2 本批不处理

- PASSWORD 文章解锁链路继续保持首版延期，不新增 unlock API。
- ADMIN/DEMO 敏感字段可见性不在没有产品裁决时改变。
- Web 对 Domain 类型的依赖及 ArchUnit 规则单独设计、单独迁移。
- OpenAPI/Javadoc 的大范围补齐单独规划，避免机械添加低价值注释。
- S3 `/media/**` 装配条件、真实 S3/Resend、Docker/Testcontainers 环境留到发布验证批次。

## 3. 公开端点配置设计

### 3.1 选择

采用“基础端点 + Profile 增量端点”组合模型。

`application.yml` 的 `myblog.security.public-endpoints` 是所有环境共享的业务白名单。`application-local.yml` 和 `application-test.yml` 改用 `myblog.security.additional-public-endpoints`，只声明探针、OpenAPI、Swagger/Knife4j 和本地媒体等环境专用路径。

不采用以下方案：

- 不继续在每个 Profile 复制完整列表；Spring Boot 对列表属性的覆盖语义会再次造成漂移。
- 不把白名单硬编码进 `SecurityConfig`；method + path 仍应由类型化配置承载并接受配置测试。

### 3.2 配置对象

`SecurityPublicEndpointProperties` 保留现有 `publicEndpoints()` 访问器，并新增 `additionalPublicEndpoints()`。构造时把 `null` 规范化为空列表，提供 `allPublicEndpoints()` 返回不可变合并结果。

`SecurityConfig` 只遍历 `allPublicEndpoints()` 注册 matcher。这样基础业务端点与 Profile 增量端点使用同一套 method + path 授权逻辑，不引入第二套安全来源。

### 3.3 回归保护

测试分三层：

1. 配置对象单元测试：基础列表与增量列表按顺序合并，任一列表缺失时不抛异常。
2. YAML 配置测试：基础文件包含站点配置、文章评论和留言板；local/test 文件不再重复这些端点，只包含环境增量。
3. Spring Security 集成测试：同时注入基础和增量端点，验证两类路径都匿名可访问，并验证同路径的非白名单 HTTP 方法仍被拒绝。

## 4. 测试数据库隔离设计

`AuditFieldHandlerIntegrationTest` 保留测试前的建表步骤，并增加测试后的 `DROP TABLE IF EXISTS t_audit_update_test`。清理使用 Spring Test SQL 的 `AFTER_TEST_METHOD` 阶段执行，即使断言失败也由测试框架执行清理。

不把临时表加入 Flyway，也不让生产 Schema 感知测试夹具。MySQL 回归结束后通过 `information_schema.tables` 断言该表不存在。

## 5. 文档与注释同步设计

本批只修改已经有确定事实的内容：

- `CLAUDE.md`：当前阶段改为 M4 前端骨架，后端进入审查问题修复与联调支持。
- `arch/module-map.md`：删除“仅 common/identity 已建立”的过时状态，更新为六模块已建立。
- `rules/testing-policy.md`：测试总数以本批完成后的 fresh H2 结果为准，修正已更名/已不存在的缺测条目。
- `rules/sql-placement.md`：修正 ADR-0005 与 ADR-0010 的真实文件名；旧库兼容规则的系统性重写留到后续质量批次。
- `MyBatisPlusConfig`：删除“尚未迁移 JdbcTemplate”的阶段性描述，改为当前 Mapper 扫描边界。
- `UserAgentResolver`：把 255 字符解释为当前审计字段上限和防止异常请求放大，不再引用旧库。

文档只陈述当前可验证事实，不顺带修改 PASSWORD、DEMO 或 Web/Domain 尚未裁决的内容。

## 6. 实施与提交边界

实施使用 TDD，并拆成三个独立提交：

1. `修复Profile公开端点配置合并`：配置对象、YAML、SecurityConfig 和回归测试。
2. `清理审计集成测试临时表`：测试夹具清理和 MySQL 元数据验证。
3. `同步后端V2当前阶段文档`：入口文档、测试基线、ADR 链接和两处生产注释。

每个提交先运行定向测试。三个提交完成后运行：

```powershell
mvn clean test
```

随后使用授权的 `myblog_v2_dev` 运行既有 MySQL 广泛回归命令，并额外确认 `t_audit_update_test` 不存在。凭据只通过进程环境变量注入，不写入设计、计划、代码或提交记录。

## 7. 验收标准

- local Profile 下以下请求不再因白名单漂移返回 401：
  - `GET /api/public/site-config`
  - `POST /api/public/articles/{id}/comments`
  - `GET /api/public/guestbook/comments`
  - `POST /api/public/guestbook/comments`
- local 专用安全探针、文档和 `/media/**` 仍保持匿名可访问。
- 白名单继续按 HTTP method + path 双维度匹配，没有扩大后台接口匿名范围。
- H2 全量测试零失败、零错误。
- 本地 MySQL 广泛回归零失败、零错误。
- MySQL 测试库不存在 `t_audit_update_test`。
- 本批只包含上述三类提交，不夹带 PASSWORD、DEMO、架构或全量注释改造。
