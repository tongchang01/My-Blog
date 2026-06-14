# Backend V2 友链纵向切片设计

> **状态：已确认，待实施（2026-06-14）**

## 1. 目标

建立 `system` 模块的第三个完整纵向切片，围绕 `t_friend_link` 提供：

- 访客查看公开友链。
- ADMIN、DEMO 查看后台友链列表和详情。
- ADMIN 新增、编辑、显示、隐藏、排序和软删除友链。
- 首次为标准业务表落实统一的软删除审计三件套。

本轮不实现在线友链申请、审核状态、回收站、恢复、批量删除、可用性探测、
自动隐藏、站点截图或三语友链说明。

友链申请继续通过邮件或 GitHub issue 完成。未来若增加在线申请，单独建立
`t_friend_link_application`，不污染当前主表。

## 2. 已确认决策

### 2.1 公开接口

```http
GET /api/public/friend-links
```

- 匿名可访问。
- 不接收语言参数。
- 只返回 `deleted=0` 且状态为 `VISIBLE` 的记录。
- 按 `sort_order ASC, id ASC` 排序。
- 不分页。友链规模预计最多几十条，引入公开分页没有实际收益。

公开响应字段：

```json
{
  "id": 101,
  "name": "Example",
  "url": "https://example.com",
  "avatarUrl": "https://example.com/logo.png",
  "description": "一句话介绍"
}
```

不返回状态、排序值和审计字段。

### 2.2 后台接口

| Method | Path | ADMIN | DEMO |
|---|---|---:|---:|
| GET | `/api/admin/friend-links?page=1&size=20` | 允许 | 允许 |
| GET | `/api/admin/friend-links/{id}` | 允许 | 允许 |
| POST | `/api/admin/friend-links` | 允许 | 禁止 |
| PUT | `/api/admin/friend-links/{id}` | 允许 | 禁止 |
| PATCH | `/api/admin/friend-links/{id}/status` | 允许 | 禁止 |
| PUT | `/api/admin/friend-links/sort-orders` | 允许 | 禁止 |
| DELETE | `/api/admin/friend-links/{id}` | 允许 | 禁止 |

- 匿名访问后台接口返回 `401 + 10002`。
- DEMO 写操作返回 `403 + 10003`。
- 后台分页只过滤 `deleted=0`，同时返回 VISIBLE 和 HIDDEN。
- 后台列表按 `sort_order ASC, id ASC` 排序。
- `page` 默认 1，`size` 默认 20，最大 100。
- 后台列表和详情返回完整业务字段及创建、更新审计字段，不返回删除审计字段。

后台结果：

```json
{
  "id": 101,
  "name": "Example",
  "url": "https://example.com",
  "avatarUrl": "https://example.com/logo.png",
  "description": "一句话介绍",
  "sortOrder": 10,
  "status": "VISIBLE",
  "createdAt": "2026-06-14T12:00:00",
  "createdBy": 1001,
  "updatedAt": "2026-06-14T12:30:00",
  "updatedBy": 1001
}
```

分页响应沿用 `records/total/page/size`。

## 3. 领域模型

### 3.1 FriendLink

`FriendLink` 是聚合根，包含：

- `id`
- `name`
- `url`
- `avatarUrl`
- `description`
- `sortOrder`
- `FriendLinkStatus status`
- `createdAt`
- `createdBy`
- `updatedAt`
- `updatedBy`

领域对象不持有 Entity、Mapper、Web DTO 或 Spring Web 类型。

### 3.2 状态

```java
public enum FriendLinkStatus {
    VISIBLE(1),
    HIDDEN(2)
}
```

- 数据库存储稳定数值 1、2。
- API 使用枚举名称 `VISIBLE`、`HIDDEN`。
- 未知数据库值按内部数据损坏处理，不能默认为 VISIBLE。
- 显示和隐藏是业务状态，不使用 `deleted` 代替。

### 3.3 字段校验

| 字段 | 规则 |
|---|---|
| `name` | trim 后必填，最长 64 |
| `url` | trim 后必填，最长 255，只接受绝对 HTTP/HTTPS URL |
| `avatarUrl` | trim，可空，最长 255，只接受绝对 HTTP/HTTPS URL |
| `description` | 单中文字段，trim，可空，最长 255 |
| `sortOrder` | 0 到 1,000,000 |
| `status` | 只接受 VISIBLE、HIDDEN |

- URL 不要求唯一，允许重复。
- 不解析或主动请求友链 URL。
- URL 校验拒绝相对地址、无 host 地址、非 HTTP/HTTPS scheme 和用户信息部分。
- 可选字符串的空白输入统一保存为 `null`。

## 4. 应用服务

应用层按职责拆分：

```text
friendlink/
├── FriendLinkQueryService
├── FriendLinkCreateService
├── FriendLinkUpdateService
├── FriendLinkStatusService
├── FriendLinkSortService
├── FriendLinkDeleteService
├── FriendLinkResult
├── FriendLinkPageResult
└── command records
```

查询服务：

- `publicList()` 不需要 principal。
- `adminPage(principal, page, size)` 允许 ADMIN、DEMO。
- `adminDetail(principal, id)` 允许 ADMIN、DEMO。

写服务：

- 只允许 ADMIN。
- principal ID 必须可解析为正数，用作审计用户。
- 领域校验异常映射为 `400 + 90001`。
- 不存在或已删除目标映射为 `404 + 90003`。

## 5. 新增与编辑

新增请求：

```json
{
  "name": "Example",
  "url": "https://example.com",
  "avatarUrl": null,
  "description": "一句话介绍",
  "sortOrder": 10,
  "status": "VISIBLE"
}
```

- POST 请求的六个业务字段全部必须出现。
- `avatarUrl`、`description` 可显式为 null。
- 新增使用 MyBatis-Plus `ASSIGN_ID`。
- `createdBy`、`updatedBy` 均为当前 ADMIN ID。

编辑请求字段与新增一致，采用完整覆盖：

- PUT 的六个业务字段全部必须出现。
- 编辑前使用 `SELECT ... FOR UPDATE` 锁定 active 行。
- 更新 SQL 使用 `WHERE id = ? AND deleted = 0`。
- 更新结果不是一行时返回内部错误，避免静默成功。

本轮不提供 PATCH 通用部分更新，避免 nullable 字段 presence 语义和接口数量膨胀。

## 6. 状态切换

```http
PATCH /api/admin/friend-links/{id}/status
Content-Type: application/json

{"status":"HIDDEN"}
```

- 请求必须明确提供状态。
- 修改前锁定 active 行。
- 更新 `status`、`updated_at`、`updated_by`。
- 重复设置相同状态允许成功，仍按一次明确后台操作刷新更新审计。

公开列表会立即按新状态过滤。

## 7. 批量排序

```http
PUT /api/admin/friend-links/sort-orders
Content-Type: application/json

{
  "items": [
    {"id":101,"sortOrder":0},
    {"id":102,"sortOrder":10}
  ]
}
```

规则：

- `items` 必填，数量为 1 到 100。
- 每个 ID 必须为正数。
- 请求内 ID 不得重复。
- 每个 `sortOrder` 必须在 0 到 1,000,000。
- 不要求 sortOrder 唯一；相同值由 `id ASC` 稳定排序。
- 请求只更新提交的目标，不要求包含数据库全部友链。
- 所有目标必须存在且未删除。

事务流程：

1. 对 ID 排序，使用同一顺序批量 `SELECT ... FOR UPDATE`，降低并发死锁风险。
2. 校验锁定行数量和 ID 集合与请求完全一致。
3. 逐项执行条件更新，写入统一的 `updated_at/updated_by`。
4. 任一更新不是一行时抛出异常，整个事务回滚。

不使用动态拼接 CASE SQL；最多 100 项的逐项 XML 更新更容易审计和验证。

## 8. 软删除

```http
DELETE /api/admin/friend-links/{id}
```

本轮首次为标准业务表落实 P2-5 软删除规则：

```sql
UPDATE t_friend_link
SET deleted = 1,
    deleted_at = #{deletedAt},
    deleted_by = #{deletedBy},
    updated_at = #{deletedAt},
    updated_by = #{deletedBy}
WHERE id = #{id}
  AND deleted = 0
```

- 删除前通过 `SELECT ... FOR UPDATE` 锁定 active 行。
- 不调用 MyBatis-Plus 通用 `deleteById`。
- 不物理删除。
- 已删除或不存在统一返回 `404 + 90003`。
- 本轮不提供恢复和回收站接口。
- 删除后公开、后台列表和详情均不可见。

友链没有跨模块结构化引用，因此删除不需要查询 content 或 comment。

## 9. 持久化

### 9.1 Repository 端口

`FriendLinkRepository` 提供：

- 查询公开可见列表。
- 查询 active 后台分页和总数。
- 按 ID 查询 active。
- 按 ID 锁定 active。
- 按 ID 集合按稳定顺序锁定 active。
- 插入。
- 完整更新。
- 更新状态。
- 更新排序值。
- 软删除并写入完整审计。

所有 SQL 写在 Mapper XML，禁止注解 SQL。

### 9.2 Entity

`FriendLinkEntity` 继承 `BaseEntity`：

- `@TableName("t_friend_link")`
- 主键使用 `IdType.ASSIGN_ID`
- status 在 Entity 中保存为 Integer，由 Repository 显式转换枚举。

Repository 负责 Entity 和领域对象转换，不把持久化类型泄漏到 application/web。

### 9.3 查询顺序

公开列表：

```sql
WHERE deleted = 0
  AND status = 1
ORDER BY sort_order ASC, id ASC
```

后台分页：

```sql
WHERE deleted = 0
ORDER BY sort_order ASC, id ASC
LIMIT #{offset}, #{size}
```

分页 offset 在 Repository 中使用安全整数计算，页码过大时返回空 records 和真实 total。

## 10. 并发语义

- 编辑、状态切换和删除通过单行 `FOR UPDATE` 串行。
- 批量排序按升序 ID 锁定，避免不同请求以不同顺序获取锁。
- 删除先获得锁时，后续编辑重新读取不到 active 行并返回 404。
- 编辑先获得锁时，删除等待编辑提交后再软删除，最终删除状态优先。
- 排序与删除竞争时，任一目标在锁定后已不可见都会使排序整体失败并回滚。
- 不新增 version 列，不修改冻结的 V1 DDL。

## 11. Web 与 OpenAPI

Web DTO 分为：

- `PublicFriendLinkVO`
- `AdminFriendLinkVO`
- `CreateFriendLinkRequest`
- `UpdateFriendLinkRequest`
- `UpdateFriendLinkStatusRequest`
- `UpdateFriendLinkSortOrdersRequest`

请求类型使用 Jakarta Validation 处理结构性校验，领域层再次执行业务校验。

OpenAPI 必须固定：

- 公开路径只有 GET。
- 后台集合路径只有 GET、POST。
- `/{id}` 只有 GET、PUT、DELETE。
- `/{id}/status` 只有 PATCH。
- `/sort-orders` 只有 PUT。
- 公开 schema 不包含 status、sortOrder 和审计字段。
- 后台 schema 不包含 deleted、deletedAt、deletedBy、Entity 或 Mapper 类型。

路由必须确保 `/sort-orders` 不被 `{id}` 错误解析。

## 12. Security

白名单增加：

```yaml
- method: GET
  path: /api/public/friend-links
```

在通用 `/api/admin/**` ADMIN 规则前增加：

```java
.requestMatchers(
        HttpMethod.GET,
        "/api/admin/friend-links",
        "/api/admin/friend-links/*")
.hasAnyRole("ADMIN", "DEMO")
```

由于 `/api/admin/friend-links/*` 也会匹配 `/sort-orders`，但只放行 GET，而该路径只提供
PUT，因此不会给 DEMO 开放排序。其它写方法继续落入 ADMIN 通用规则。

应用服务仍执行角色校验，避免绕过 Web Security 时丢失业务边界。

## 13. 错误处理

| 场景 | HTTP | code |
|---|---:|---|
| 字段、URL、状态、分页或排序请求非法 | 400 | `90001` |
| token 缺失或失效 | 401 | `10002` |
| DEMO 执行写操作 | 403 | `10003` |
| 友链不存在或已删除 | 404 | `90003` |
| 持久化更新行数异常或未知状态值 | 500 | `99999` |

错误响应使用稳定中文消息，不回显 SQL、锁等待信息或内部异常详情。

## 14. 测试策略

### 14.1 领域

- 状态数值解析。
- 字段 trim、空值、长度、URL scheme 和 user-info 校验。
- sortOrder 上下界。
- Entity 重建时未知状态值失败。

### 14.2 Repository

- 公开查询只返回 VISIBLE active，顺序稳定。
- 后台分页包含 VISIBLE/HIDDEN，不含 deleted。
- active ID 查询和行锁查询。
- ASSIGN_ID 插入及创建审计。
- 完整更新、状态更新和排序更新。
- 软删除同时写入五个字段。
- 所有 SQL 位于 XML。

### 14.3 Application

- ADMIN/DEMO 查询权限。
- ADMIN 写权限及 principal ID 校验。
- 新增和完整编辑。
- 状态切换。
- 批量排序重复 ID、缺失目标和事务回滚。
- 删除后不可查询。
- 编辑/删除与排序/删除并发语义。

### 14.4 Web、Security 与 OpenAPI

- 匿名公开读取。
- ADMIN/DEMO 后台读取。
- DEMO 所有写方法 403。
- 匿名后台请求 401。
- 请求字段缺失、未知字段和非法 URL 400。
- OpenAPI 方法和 schema 精确匹配。

### 14.5 全量验证

- `mvn clean test`
- Maven Enforcer dependency convergence
- `git diff --check`
- system 模块无注解 SQL
- application/domain 不依赖 Web、Entity、Mapper
- Docker 不可用时只允许既有 Testcontainers MySQL 条件测试 skipped

## 15. 提交拆分

实施保持五个中文提交：

1. `建立友链领域与持久化查询`
   - 状态、领域、Repository、Entity、Mapper XML、公开和后台查询。
2. `实现公开与后台友链查询`
   - 查询应用服务、公开/后台 GET、分页、Security 和 OpenAPI 读取契约。
3. `实现友链新增与编辑`
   - POST、PUT、完整字段校验、事务与审计。
4. `实现友链状态排序与软删除`
   - PATCH 状态、批量排序、DELETE、软删除三件套和并发测试。
5. `完成友链纵向切片`
   - 完整集成测试、接口契约、状态、路线图和全量验证。

每个提交只包含对应范围，避免再次形成数百文件的大提交。

## 16. 验收标准

- 匿名只看到 VISIBLE 且未删除友链。
- ADMIN、DEMO 可查看后台列表和详情，DEMO 不能写。
- ADMIN 可新增、完整编辑、显示、隐藏、批量排序和软删除。
- 友链 URL 和头像 URL 只接受绝对 HTTP/HTTPS 地址。
- URL 允许重复，不增加唯一索引。
- 批量排序最多 100 项，重复或缺失 ID 整体回滚。
- 软删除完整写入 deleted、deletedAt、deletedBy、updatedAt、updatedBy。
- 不使用通用 deleteById，不修改 V1 DDL。
- 所有 SQL 在 Mapper XML。
- OpenAPI 不暴露持久化和删除审计类型。
- 新代码包含必要中文业务注释。
- 五批分别形成中文本地提交。
- 全量 Maven、Enforcer、ArchUnit 和静态检查通过。
