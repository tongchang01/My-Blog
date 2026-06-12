# M3 开始前全量审查与修正清单

> 本文档回答：“M2 基线是否足以进入 M3？进入业务模块重建前还需要修正什么？”
> 适用范围：`MyBlog-springboot-v2/` 的架构、依赖、配置、安全、持久化、DDL 与测试。
> 审查日期：2026-06-08。

## 1. 审查结论

当前模块方向、技术栈和 14 张表 V1 DDL 可以继续使用，但暂不直接进入 M3。进入前应关闭 6 个 P1 问题；P2 必须在对应业务能力首次落地前完成。

当前验证基线：

- `mvn clean test`：68 tests，0 failures，0 errors
- Maven Enforcer：Java、Maven与依赖收敛通过
- Flyway H2 烟测：14 张表全部创建
- DDL 静态检查：0 外键、0 `TIMESTAMP`、0 `ON UPDATE`
- `AUTO_INCREMENT` 仅用于 `t_page_view`、`t_mail_log`

状态标记：`[ ]` 未处理、`[~]` 处理中、`[x]` 已验收、`[!]` 确认延期。

## 2. P1：进入 M3 前必须修正

### P1-1 审计更新时间不会可靠刷新

- [x] 修正实现与测试

`strictUpdateFill` 默认不覆盖实体已有的非空值；数据库查出的实体通常带有旧 `updatedAt/updatedBy`，因此更新后可能保留旧审计值。

修正要求：

- 更新时明确覆盖 `updatedAt`
- 存在登录用户时明确覆盖 `updatedBy`
- 固定系统任务对 `updatedBy` 的处理语义
- Wrapper-only update 不得假定自动填充一定执行

验收：

- 已有旧审计值的实体更新后写入新值
- 新值来自注入的 `Clock` 和当前认证用户
- Mapper 集成测试覆盖真实 update，不只直接调用 handler

建议提交：`修正持久化审计字段更新策略`

参考：[MyBatis-Plus 自动填充说明](https://baomidou.com/en/guides/auto-fill-field/)

### P1-2 Flyway 缺少 MySQL 支持模块

- [x] 已增加依赖和 Testcontainers 测试；V1 已在本地 MySQL 8.0.35 完成迁移验证，Docker 执行暂缓

项目只有 `flyway-core`，依赖树中没有 `flyway-mysql`。H2 测试不能发现真实 MySQL 初始化问题。

修正要求：

- 增加由 Spring Boot BOM 管理版本的 `flyway-mysql`
- 首个 MySQL 集成测试使用 Testcontainers MySQL
- 从空 schema 执行完整 V1 迁移

验收：

- 依赖树包含同版本 `flyway-core`、`flyway-mysql`
- Testcontainers MySQL 完成 V1 迁移并确认 14 张表
- 不修改已冻结的 V1；发现 schema 问题时新增 V2 迁移

建议提交：`补齐Flyway MySQL迁移能力`

参考：[Flyway MySQL 官方说明](https://documentation.red-gate.com/flyway/reference/database-driver-reference/mysql)

### P1-3 运行环境默认行为不安全

- [x] 取消默认 local profile并建立生产配置基线

`spring.profiles.default=local` 会自动启用本地环境；该环境默认连接旧 `aurora` 库、允许 root 空密码、使用固定 JWT 密钥、关闭 Flyway并公开 API 文档。仓库没有 V2 生产配置。

修正要求：

- 基础配置不默认激活 `local`
- 本地运行必须显式指定 profile
- 建立最小生产 profile；数据库和 JWT 密钥缺失时启动失败
- 生产默认关闭 Knife4j、Swagger UI 和非必要 Actuator 端点
- 统一环境变量名称，不混用 `MYBLOG_DB_*` 与 `MYBLOG_DATASOURCE_*`

验收：

- 未指定 profile且缺少必要配置时安全失败
- local 可显式启动
- prod 不含固定密钥、空密码默认值和公开文档
- 自动化测试验证 profile 和关键属性

建议提交：`收紧后端运行环境配置基线`

### P1-4 JWT 与 identity 模块边界冲突

- [x] 冻结组件所有权并调整 ArchUnit

ArchUnit 禁止业务模块依赖 `common.security`，但 JWT 签发服务位于该包；过滤器又需要 identity 的 `token_version`。当前结构无法避免违规或反向依赖。

目标边界：

- `common`：框架接入、认证上下文、无业务状态的 JWT 编解码原语
- `identity`：登录、refresh token、用户状态、`token_version` 和签发用例
- common 过滤器通过稳定端口校验 token 版本，不依赖 identity infrastructure
- identity 不依赖 common 中的过滤器或 Spring Security 具体实现

同时统一分层规则，推荐：

```text
web -> application -> domain port
infrastructure -> domain port implementation
```

验收：

- identity 可以合法调用 token 签发抽象
- common 不依赖 identity Entity、Mapper、Repository 实现
- application 不直接依赖 Mapper
- 至少一个故意违规的测试夹具证明 ArchUnit 会失败

建议提交：`明确认证能力与identity模块边界`

### P1-5 JWT 实现不符合冻结方案

- [x] 收口 JWT 声明、校验和撤销路线

当前实现没有 `typ`、`ver`，未显式校验 issuer，仍依赖内存撤销，密钥也未校验至少 32 字节。

修正要求：

- 删除内存撤销实现的最终业务依赖
- access token 包含并校验 `typ`、`ver`、`sub`、`iss`、`iat`、`exp`
- refresh token 使用高熵随机值，数据库只保存 SHA-256
- JWT 密钥按字节长度校验至少 32 字节
- access、refresh、article access token 不得混用
- 统一无效 token 的错误码，解决 `10002/10003` 冲突

验收：

- 错误 issuer、typ、过期 token、错误 ver 全部失败
- 登出、改密、强制下线后旧 access token 失效
- 服务重启后撤销仍有效
- refresh token 明文只在签发时返回一次

必须拆为小提交：

1. [x] `统一JWT声明与校验规则`
2. [x] `移除内存Token撤销实现`
3. [x] `实现refresh token持久化流程`
4. [x] `实现token_version持久化校验`
5. [x] `实现用户Token整体撤销用例`
6. [x] `统一认证失败错误码`

### P1-6 客户端 IP 信任模型不安全

- [x] 确定代理模型并修正 IP 解析

客户端可伪造当前无条件信任的转发头，绕过登录/评论限流并污染审计。

修正要求：

- 明确应用是否只允许经过可信反向代理访问
- 代理必须覆盖而不是追加客户端提供的转发头
- 后端仅对可信代理来源解析转发头，否则使用连接远端地址
- 登录、评论和审计共用同一个可信 IP 入口

验收：

- 直连时伪造转发头不能改变客户端 IP
- 可信代理场景能取得真实客户端 IP

建议提交：`收紧客户端IP与代理头信任策略`

## 3. P2：对应业务首次落地前修正

### P2-1 CORS 缺少文章访问 Header

- [ ] 允许请求头 `X-Article-Token`
- [ ] 按需暴露 `Authorization`、`X-Article-Token`
- [ ] 增加浏览器预检测试

PASSWORD 文章功能落地前必须完成。

### P2-2 MySQL session 时区未强制设置

- [x] 使用 Connector/J 当前属性显式设置连接时区
- [x] 强制 session `time_zone`
- [x] 在真实 MySQL 测试中验证 `@@session.time_zone`

`serverTimezone` 是 `connectionTimeZone` 的别名，不等于强制修改 session。

参考：[Connector/J 时间处理说明](https://dev.mysql.com/doc/connector-j/en/connector-j-time-instants.html)

### P2-3 ArchUnit 守护不足

- [x] 禁止 web 依赖 Entity 和 infrastructure
- [x] 禁止 application 依赖 Mapper、Entity、Web DTO
- [x] 禁止 domain 依赖 Spring Web、MyBatis、Servlet API
- [x] 禁止 infrastructure 依赖 web
- [x] 增加模块循环依赖检查
- [x] 处理空模块下 `allowEmptyShould(true)` 的假通过：空模块阶段允许规则跳过，但每条新增规则必须通过故意违规夹具证明能够拦截

### P2-4 测试探针进入生产扫描

- [x] 将 `SecurityProbeController` 限定到 `local/test`

### P2-5 逻辑删除审计没有统一路径

- [ ] 统一写入 `deleted`、`deletedAt`、`deletedBy`
- [ ] 禁止业务代码调用无法补齐审计列的删除方法
- [ ] 为标准业务表建立 Repository 删除测试

`@TableLogic` 默认只保证逻辑删除标记，不自动满足项目定义的软删三件套。

### P2-6 依赖版本与构建可重复性

- [ ] MyBatis-Plus 3.5.12 评估升级到当前 3.5.x
- [ ] springdoc 2.8.8 升级到当前 2.8.x
- [ ] 验证 Knife4j 4.5.0 与所选 springdoc
- [ ] 增加 Maven Wrapper
- [ ] 上线前增加依赖漏洞扫描

版本升级单独提交并完整回归。Spring Boot 3.5.14 当前保留。

## 4. 文档一致性修正

- [x] `rules/package-layout.md` 与 `arch/module-map.md` 统一 application 依赖方向
- [x] `rules/security-baseline.md` 删除内存撤销残留
- [x] `arch/persistence-strategy.md` 删除保留 `TokenRevocationStore` 的旧结论
- [x] `migration/v2-code-reconciliation.md` 更新 JWT 文件处置方案
- [x] JWT 配置名统一为 `access-token-ttl` / `refresh-token-ttl`
- [ ] 登录字段名统一为 `last_login_at`、`last_login_ip`
- [x] 错误码统一 `10002/10003` 语义
- [ ] 根包 `package-info.java` 删除旧顶层 `infrastructure` 描述
- [ ] `workflows/build-and-test.md` 与实际环境变量一致

文档必须随对应行为修改一起提交。

## 5. 修正执行顺序

1. **持久化正确性**
   - P1-1 审计字段更新
   - P1-2 Flyway MySQL 模块
   - P2-2 MySQL session 时区
2. **运行环境安全**
   - P1-3 profile 与生产配置
   - P1-6 代理头信任
   - P2-4 测试探针隔离
3. **M3 架构边界**
   - P1-4 JWT/identity 所有权
   - P2-3 ArchUnit 守护
   - 文档依赖方向统一
4. **identity 安全基座**
   - P1-5 JWT 与撤销路线
   - 错误码、配置名统一
5. **依赖维护**
   - P2-6 依赖升级和 Maven Wrapper

每一步完成后停止，等待确认再执行下一步。

## 6. 提交拆分要求

- 每个提交只完成一个明确目的
- 不把全部 P1/P2 合成一个提交
- 基础设施提交不提前编写 identity 业务接口
- 依赖升级与业务代码分开
- 文档随对应代码提交
- 文件过多时继续按配置、测试、架构规则拆分
- Git 提交信息使用中文

每个提交前至少执行：

```bash
git status --short
git diff --stat
mvn clean test
```

涉及 MySQL 的提交还必须执行对应 Testcontainers 测试。

## 7. M3 准入条件

- [x] P1-1 至 P1-6 全部关闭
- [x] `mvn clean test` 全部通过
- [!] V1 已在本地 MySQL 8.0.35 从空 schema 迁移成功；Testcontainers 执行按当前决定暂缓
- [x] MySQL session 时区验证通过
- [x] JWT 与 identity 依赖方向有代码和 ArchUnit 双重守护
- [x] prod 缺少密钥或数据库配置时失败启动
- [ ] 文档中的错误码、配置名、字段名与代码一致
- [ ] 工作区不存在本轮无关修改

全部满足后，将结论改为“允许进入 M3”。M3 仍按小步提交推进，先做 identity 最小纵向切片，不一次性完成整个模块。

## 8. 审查记录

| 日期 | 结论 | 说明 |
|---|---|---|
| 2026-06-08 | 暂不进入 M3 | 发现 6 个 P1 与 6 个 P2，先按本文档逐项关闭 |
| 2026-06-10 | P1-3 已关闭 | 取消默认 local，新增 prod 基线，统一数据库环境变量并验证安全失败 |
| 2026-06-10 | P1-6 已关闭 | 默认不信任代理头，仅允许显式可信代理 IP / CIDR 提供客户端地址 |
| 2026-06-10 | P2-4 已关闭 | Security 探针仅在 local / test profile 注册 |
| 2026-06-10 | P1-4 已关闭 | token 签发 / 验证端口归 common.auth，identity 与过滤器不依赖对方具体实现 |
| 2026-06-12 | P1-5 已关闭 | JWT 声明、持久化撤销、刷新令牌用户状态与 `10002/10003` 认证错误码路线已统一 |
| 2026-06-12 | 安全基线已同步 | 删除内存撤销实现残留，并明确 access token 需结合持久化 `token_version` 完成校验 |
| 2026-06-12 | JWT 迁移对账已同步 | common 保留 JWT 原语和端口，identity 负责持久化校验、refresh token 与整体撤销 |
| 2026-06-12 | JWT 配置名已统一 | 代码、YAML 与文档统一使用 `access-token-ttl` / `refresh-token-ttl` |
