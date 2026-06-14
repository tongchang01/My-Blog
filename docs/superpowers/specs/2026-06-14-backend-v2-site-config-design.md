# Backend V2 站点配置设计

> **状态：已实施（2026-06-14）**

## 1. 目标

建立 `system` 模块的第一个完整纵向切片，围绕 `t_site_config` 提供：

- 前台匿名读取当前语言的公开站点配置。
- ADMIN、DEMO 读取完整三语配置。
- ADMIN 全量更新站点配置。

本轮不实现附件上传、友链管理、前端页面、Markdown 转 HTML、配置缓存、配置历史或动态 KV 配置。

## 2. 已确认决策

### 2.1 数据模型

继续使用已冻结的 `t_site_config` 宽表，不新增 Flyway：

- 全站固定只有一行，`id=1`。
- Flyway 已初始化 `id=1, site_title_zh='MyBlog'`。
- 展示型字段保存中文、日语、英语三份内容。
- 非展示型字段保持单值。
- 普通后台操作不提供删除接口。

业务字段共 13 个：

1. `siteTitleZh`
2. `siteTitleJa`
3. `siteTitleEn`
4. `siteSubtitleZh`
5. `siteSubtitleJa`
6. `siteSubtitleEn`
7. `aboutMdZh`
8. `aboutMdJa`
9. `aboutMdEn`
10. `logoUrl`
11. `faviconUrl`
12. `icpNo`
13. `spotifyPlaylistId`

### 2.2 接口范围

公开接口：

```http
GET /api/public/site-config?lang=zh|ja|en
```

后台读取：

```http
GET /api/admin/site-config
Authorization: Bearer <access-token>
```

后台更新：

```http
PUT /api/admin/site-config
Authorization: Bearer <access-token>
Content-Type: application/json
```

权限语义：

- 公开读取允许匿名访问。
- ADMIN、DEMO 均可读取后台完整配置。
- 仅 ADMIN 可更新。
- DEMO 更新返回 `403 + 10003`。

### 2.3 PUT 全量覆盖

`PUT` 是真正的全量替换，不使用 PATCH 语义：

- 13 个业务字段必须全部在 JSON 中出现。
- `siteTitleZh` 必须有有效值。
- 其余字段允许显式 `null` 或空白字符串，统一清空为数据库 `NULL`。
- 字段未出现返回 `400 + 90001`。
- 未知字段仍由全局 Jackson 严格配置拒绝。

Java 普通 record 无法区分“字段缺失”和“显式 null”。Web 请求模型使用 `@JsonSetter` 记录每个字段的 presence；转换为应用命令前统一检查 13 个字段是否全部出现。该 presence 状态只停留在 Web 边界，不进入领域模型，也不暴露到 OpenAPI schema。

## 3. 公开读取与语言回退

公开响应只返回当前语言的展示字段和公共单值字段：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "siteTitle": "MyBlog",
    "siteSubtitle": null,
    "aboutMd": null,
    "logoUrl": null,
    "faviconUrl": null,
    "icpNo": null,
    "spotifyPlaylistId": null
  }
}
```

`lang` 必填，只允许 `zh`、`ja`、`en`：

- `zh` 直接读取中文字段。
- `ja`、`en` 按字段独立回退；目标语言字段为空时回退对应中文字段。
- 中文可选字段本身为空时返回 `null`。
- 标题中文字段按数据库约束和应用校验始终非空。
- 非法或缺失语言返回 `400 + 90001`。

公开接口不返回 `id`、审计字段、删除字段或全部三语副本。

## 4. 后台读取与更新响应

后台读取和更新成功后均返回完整配置：

```json
{
  "code": "00000",
  "msg": "success",
  "data": {
    "siteTitleZh": "MyBlog",
    "siteTitleJa": null,
    "siteTitleEn": null,
    "siteSubtitleZh": null,
    "siteSubtitleJa": null,
    "siteSubtitleEn": null,
    "aboutMdZh": null,
    "aboutMdJa": null,
    "aboutMdEn": null,
    "logoUrl": null,
    "faviconUrl": null,
    "icpNo": null,
    "spotifyPlaylistId": null,
    "updatedAt": "2026-06-14T12:00:00",
    "updatedBy": 1001
  }
}
```

后台响应不返回 `createdAt`、`createdBy`、`deleted`、`deletedAt`、`deletedBy`。保留 `updatedAt` 和 `updatedBy`，供后台识别最近更新时间。

## 5. 字段校验与规范化

| 字段 | 规则 |
|---|---|
| `siteTitleZh` | trim 后必填，最长 128 |
| `siteTitleJa` / `siteTitleEn` | trim，可空，最长 128 |
| `siteSubtitleZh` / `siteSubtitleJa` / `siteSubtitleEn` | trim，可空，最长 255 |
| `aboutMdZh` / `aboutMdJa` / `aboutMdEn` | 可空，最长 50,000；保留正文首尾内容，不 trim；纯空白清空 |
| `logoUrl` / `faviconUrl` | trim，可空，最长 255；仅绝对 HTTP / HTTPS URL |
| `icpNo` | trim，可空，最长 64 |
| `spotifyPlaylistId` | trim，可空，最长 64；仅字母、数字、下划线、连字符 |

长度校验基于规范化后的最终值。URL 必须具有 `http` 或 `https` scheme 和非空 host，不接受相对 URL、`javascript:`、`data:` 等协议。

关于我内容本轮按 Markdown 原文保存和返回。后端不临时引入 Markdown 渲染器；前端后续使用禁用原始 HTML 的安全 Markdown 管线渲染。

## 6. 架构

新建 `com.tyb.myblog.v2.system` 四层模块，首批只创建实际使用的 SiteConfig 文件，不建立附件和友链空壳。

```text
system.web
    -> system.application
        -> system.domain
system.infrastructure
    -> system.domain
```

主要职责：

- Domain：`SiteConfig` 聚合、语言枚举、字段规范化规则和 `SiteConfigRepository`。
- Application：公开语言查询、后台完整查询、ADMIN 全量更新。
- Infrastructure：MyBatis Mapper、XML SQL、持久化对象和 Repository 实现。
- Web：公开 Controller、后台 Controller、请求 presence 处理、响应 DTO 和 OpenAPI 文档模型。

SQL 只写在 Mapper XML 中，不使用 `@Select`、`@Update` 等注解 SQL。

## 7. 持久化与事务

查询固定使用 `id=1 AND deleted=0`。

更新流程：

1. 从认证主体确认 ADMIN 和正数用户 ID。
2. `SELECT ... FOR UPDATE` 锁定固定配置行。
3. 校验并构造规范化后的完整 `SiteConfig`。
4. 单条 XML `UPDATE` 写入全部 13 个业务字段、`updated_at`、`updated_by`。
5. 在同一事务内重新读取并返回更新后的完整配置。

两个并发 PUT 串行执行。由于请求是完整覆盖，后提交者覆盖前提交者，不产生不同列来自不同请求的混合状态。

固定配置行缺失、已软删除或更新影响行数不是 1，均视为系统数据损坏：

- 日志只记录固定配置 ID 和必要上下文。
- 返回 `500 + 99999`。
- 不在运行时自动插入或恢复配置行，避免掩盖迁移或运维问题。

## 8. 错误处理

| 场景 | HTTP | code |
|---|---:|---|
| lang 缺失或非法 | 400 | `90001` |
| PUT 字段缺失、长度非法、URL 非法、Spotify ID 非法或 JSON 非法 | 400 | `90001` |
| access token 缺失或失效 | 401 | `10002` |
| DEMO 或非 ADMIN 更新 | 403 | `10003` |
| 固定配置行缺失、已删除、更新行数异常或持久化失败 | 500 | `99999` |

日志禁止输出完整更新请求、三语关于我正文或 Authorization/token。

## 9. 测试策略

### 9.1 Domain 与 Application

- `zh` 直接读取中文字段。
- `ja` / `en` 逐字段回退中文。
- 文本 trim、空白清空、Markdown 保留首尾。
- URL 和 Spotify ID 校验。
- DEMO / 非 ADMIN 更新拒绝。
- 非法认证主体 ID 拒绝。
- 配置行缺失和更新行数异常映射内部错误。

### 9.2 Repository

- 固定 ID 正常查询。
- `SELECT ... FOR UPDATE` 查询。
- 13 个字段完整更新。
- `updated_at`、`updated_by` 使用应用层 `Clock` 和当前用户。
- 已软删除配置不被读取或更新。

### 9.3 Web、Security 与 OpenAPI

- 公开接口匿名访问。
- lang 缺失和非法值返回稳定错误。
- ADMIN、DEMO 可读取后台完整配置。
- ADMIN 可 PUT，DEMO 和匿名请求分别返回 403、401。
- PUT 缺失任意字段会失败，显式 null 可以清空可选字段。
- OpenAPI 公开响应不暴露后台字段；后台 PUT 的 13 个字段均标记 required/nullable 语义。

### 9.4 集成与并发

- ADMIN 更新后，后台读取和三种语言公开读取立即反映最新值。
- 日语、英语逐字段回退中文。
- 更新失败时数据库保持原值。
- 两个并发 PUT 通过行锁串行，最终数据完整来自其中一个请求，不出现字段混合。
- ArchUnit 守护 system 四层依赖。
- 静态检查 system 模块没有注解 SQL。

## 10. 提交拆分

实施计划按小批次拆分，避免一次提交大量文件：

1. system 模块骨架、领域模型和持久化读取。
2. 公开站点配置查询与语言回退。
3. 后台完整读取。
4. ADMIN 全量更新、事务与并发。
5. Web/Security/OpenAPI 集成验收和文档收尾。

每批执行 RED、GREEN、定向回归和中文提交。

实施提交：

1. `a8f1194 建立站点配置领域与持久化读取`
2. `a44a375 开放公开站点配置查询`
3. `369dae1 开放后台站点配置读取`
4. `3d69de3 实现站点配置全量更新`
5. 集成契约与文档收尾由本轮最终提交承载

## 11. 验收标准

- 三个接口按约定开放且权限正确。
- 公开响应只返回当前语言内容和公共单值字段。
- 日英字段缺失时逐字段回退中文。
- PUT 13 个业务字段必须全部出现。
- 可选字段可通过 null 或空白清空。
- 站点标题、URL、Markdown 长度和 Spotify ID 校验稳定。
- 固定配置行缺失不会被静默重建。
- 并发更新不会产生字段混合。
- 没有新增 Flyway、注解 SQL、配置缓存或 Markdown 渲染依赖。
- 新代码有必要的中文业务注释。
- 全量 Maven 测试和 `git diff --check` 通过。

## 12. 实施验证

- `mvn clean test`：329 tests，0 failures，0 errors，4 skipped。
- 4 个 skipped 均为 Docker 不可用时的 Testcontainers MySQL 条件测试。
- H2 行锁并发、真实 JWT HTTP 流程、Security 权限和 OpenAPI 契约均通过。
- system 模块无注解 SQL、无配置正文日志，presence 类型未越过 Web 层。
