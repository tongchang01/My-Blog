# Backend V2 分类与标签纵向切片设计

> **状态：已实施（2026-06-15）**

实施提交：

- `41d375d` 建立分类标签领域与持久化查询
- `24b2d50` 实现公开与后台分类标签查询
- `2ed74ea` 实现分类标签新增与编辑
- `5541c21` 实现分类排序与引用保护删除
- 收尾提交：完成分类标签纵向切片（本文档所在提交）

## 1. 目标

建立 `content` 模块的第一个完整纵向切片，围绕 `t_category` 和 `t_tag` 提供：

- 访客读取三语分类、标签列表。
- ADMIN、DEMO 读取后台分类、标签列表和详情。
- ADMIN 新增、完整编辑、排序和软删除分类。
- ADMIN 新增、完整编辑和软删除标签。
- slug 规范化、全局唯一和引用保护。
- 首次引入 MapStruct，消除机械类型转换代码。

本轮不实现文章 CRUD、文章状态机、文章筛选、文章数统计、恢复、回收站、批量删除或标签排序。

## 2. 方案选择

采用“分类和标签基础切片”方案，不提前实现公开文章数。

原因：

- 分类和标签是文章写入、筛选和展示的前置依赖。
- 文章数必须遵守文章五态、定时发布、软删除和 PASSWORD 可见性规则；文章切片尚未落地时提前统计会复制一套不完整规则。
- 删除引用保护只判断是否存在 active 引用，不依赖文章公开可见性，因此可在本轮独立完成。

公开文章数、分类筛选和标签筛选在文章查询切片中统一实现。

## 3. 已确认规则

### 3.1 分类

- 分类是平铺结构，不支持父子层级。
- 名称包含 `nameZh`、`nameJa`、`nameEn`。
- `nameZh` 必填；日文、英文可空。
- 分类按 `sortOrder ASC, id ASC` 稳定排序。
- slug 全局唯一。
- 被 active 文章引用时不得删除。

### 3.2 标签

- 标签由 ADMIN 维护，访客不能创建。
- 名称包含 `nameZh`、`nameJa`、`nameEn`。
- `nameZh` 必填；日文、英文可空。
- 后台和公开列表按 `nameZh ASC, id ASC` 稳定排序。
- slug 全局唯一。
- 被 active 文章引用时不得删除。

### 3.3 多语言 fallback

公开接口接收 `lang=zh|ja|en`：

- `zh` 返回 `nameZh`。
- `ja` 优先返回 `nameJa`，空值时返回 `nameZh`。
- `en` 优先返回 `nameEn`，空值时返回 `nameZh`。
- 缺少或不支持的 `lang` 返回 `400 + 90001`，不静默猜测语言。

后台接口始终返回全部三语字段，不执行 fallback。

`content` 定义本模块的 `ContentLanguage` 值对象，不依赖 `system.domain.siteconfig.SiteLanguage`。两者虽然使用相同语言代码，但属于不同业务模块；若后续出现第三个业务模块需要同一解析规则，再通过独立设计评估是否上移到 common。

## 4. slug 规则

slug 在领域层统一规范化：

1. trim。
2. 使用 `Locale.ROOT` 转小写。
3. 长度为 1 到 64。
4. 只允许 `a-z`、`0-9` 和单个连字符。
5. 不允许连字符位于首尾。
6. 不允许连续连字符。

分类和标签分别在自身表内全局唯一。冻结 DDL 的唯一索引包含软删除数据，因此删除后 slug 不复用；该行为可避免历史 URL 指向新的业务对象。

应用层在写入前执行可读的唯一性检查，数据库唯一索引负责并发兜底。并发冲突统一映射为 `409 + 90004`，不暴露索引名或 SQL 异常。

## 5. API 设计

### 5.1 公开接口

```http
GET /api/public/categories?lang=zh
GET /api/public/tags?lang=zh
```

匿名可访问，不分页。

公开响应只包含：

```json
{
  "id": 101,
  "name": "Java",
  "slug": "java"
}
```

不返回三语原始字段、排序值、文章数或审计字段。

### 5.2 后台分类接口

| Method | Path | ADMIN | DEMO |
|---|---|---:|---:|
| GET | `/api/admin/categories` | 允许 | 允许 |
| GET | `/api/admin/categories/{id}` | 允许 | 允许 |
| POST | `/api/admin/categories` | 允许 | 禁止 |
| PUT | `/api/admin/categories/{id}` | 允许 | 禁止 |
| PUT | `/api/admin/categories/sort-orders` | 允许 | 禁止 |
| DELETE | `/api/admin/categories/{id}` | 允许 | 禁止 |

### 5.3 后台标签接口

| Method | Path | ADMIN | DEMO |
|---|---|---:|---:|
| GET | `/api/admin/tags` | 允许 | 允许 |
| GET | `/api/admin/tags/{id}` | 允许 | 允许 |
| POST | `/api/admin/tags` | 允许 | 禁止 |
| PUT | `/api/admin/tags/{id}` | 允许 | 禁止 |
| DELETE | `/api/admin/tags/{id}` | 允许 | 禁止 |

数据量很小，后台列表不分页。分类和标签查询均过滤 `deleted=0`。

## 6. 写入契约

### 6.1 分类

POST 和 PUT 使用完整覆盖：

```json
{
  "nameZh": "后端",
  "nameJa": "バックエンド",
  "nameEn": "Backend",
  "slug": "backend",
  "sortOrder": 10
}
```

- 五个字段必须全部出现。
- `nameZh` trim 后必填，最长 64。
- `nameJa`、`nameEn` 可显式为 `null`；空白统一保存为 `null`，最长 64。
- `sortOrder` 范围为 0 到 1,000,000。

### 6.2 标签

POST 和 PUT 使用完整覆盖：

```json
{
  "nameZh": "Java",
  "nameJa": "Java",
  "nameEn": "Java",
  "slug": "java"
}
```

- 四个字段必须全部出现。
- 名称规则与分类一致。
- 标签不增加 `sortOrder`，不提供排序接口。

未知字段、字段缺失和非法值返回 `400 + 90001`。

## 7. 删除与引用保护

删除使用显式软删除，同时写入：

- `deleted=1`
- `deletedAt`
- `deletedBy`
- `updatedAt`
- `updatedBy`

分类删除流程：

1. 锁定 active 分类行。
2. 查询是否存在 `deleted=0` 的文章引用该 `category_id`。
3. 存在引用时返回 `409 + 90004`。
4. 无引用时执行条件软删除。

标签删除流程：

1. 锁定 active 标签行。
2. 通过 `t_article_tag` 与 `t_article` 查询是否存在 active 文章引用。
3. 存在引用时返回 `409 + 90004`。
4. 无引用时执行条件软删除。

不自动改文章分类，不自动解绑标签，不级联删除，不调用通用 `deleteById`。

## 8. 并发与事务

- 编辑、排序和删除前使用 `SELECT ... FOR UPDATE` 锁定 active 行。
- 分类批量排序接收 1 到 100 项，ID 不得重复；按 ID 升序锁定，任一目标缺失则整体回滚。
- 创建依靠数据库唯一索引收敛并发 slug 冲突。
- 编辑先锁定目标，再检查其它记录是否占用 slug；数据库唯一索引继续作为最终兜底。
- 删除与文章写入的完整并发约束将在文章切片中补齐：未来创建或修改文章分类、标签关联时，必须先锁定对应 active 分类和标签，再写入文章与关联表。本轮没有文章写入口，因此当前切片不存在新的竞态入口。
- 所有时间来自注入的 `Clock`，不在领域和应用层直接调用系统时间。

## 9. 分层与文件职责

```text
content/
├── web/
│   ├── PublicCategoryController / PublicTagController
│   ├── AdminCategoryController / AdminTagController
│   └── Request / VO / OpenAPI 文档模型
├── application/
│   ├── category/
│   └── tag/
├── domain/
│   ├── category/
│   └── tag/
└── infrastructure/
    └── persistence/
        ├── entity/
        ├── mapper/
        ├── mapping/
        └── repository/
```

- web 只依赖 application。
- application 负责权限、事务、审计、唯一性冲突和引用保护编排。
- domain 负责字段规范化与业务不变量，不依赖 Spring、MyBatis 或 HTTP 类型。
- infrastructure 负责 Entity、Mapper XML、Repository 和数据库异常翻译。
- 所有生产 SQL 位于 Mapper XML，不使用注解 SQL。

## 10. MapStruct 引入

使用官方当前稳定版 MapStruct `1.6.3`，配置 Maven 编译期注解处理器，并加入 `lombok-mapstruct-binding 0.2.0` 保证 Lombok 与 MapStruct 的处理顺序。

MapStruct 仅用于无业务判断的机械映射：

- `CategoryEntity` 与 `Category`。
- `TagEntity` 与 `Tag`。
- application result 与 web VO 中字段同名的部分。

映射接口放在转换目标所属层：Entity 与领域对象的映射放在 infrastructure，application result 到 VO 的映射放在 web。不得建立 infrastructure 到 application/web 的反向依赖。

以下逻辑禁止隐藏在生成映射中：

- 多语言 fallback。
- slug 规范化。
- 枚举或数据库状态校验。
- 唯一性和引用判断。
- 审计字段生成。

映射接口使用 `componentModel = "spring"`，并启用未映射目标字段编译报错，避免新增字段被静默遗漏。

## 11. 持久化

### 11.1 CategoryRepository

提供：

- 查询全部 active 分类。
- 按 ID 查询和锁定 active 分类。
- 按 slug 查询，包括已删除记录。
- 插入和完整更新。
- 批量锁定和更新排序。
- 查询 active 文章引用是否存在。
- 显式软删除。

### 11.2 TagRepository

提供：

- 查询全部 active 标签。
- 按 ID 查询和锁定 active 标签。
- 按 slug 查询，包括已删除记录。
- 插入和完整更新。
- 查询 active 文章引用是否存在。
- 显式软删除。

### 11.3 SQL 规则

- 生产 SQL 全部放在 `src/main/resources/mapper/content/`。
- 复杂引用查询使用 XML 中文注释说明业务场景和 active 条件。
- 不修改冻结的 `V1__init.sql`。
- 不新增数据库外键。

## 12. Security

白名单新增：

```yaml
- method: GET
  path: /api/public/categories
- method: GET
  path: /api/public/tags
```

在通用 `/api/admin/**` ADMIN 规则前，为分类和标签 GET 路径开放 ADMIN、DEMO。应用服务仍执行角色校验，防止绕过 Web Security 后丢失业务边界。

## 13. 错误处理

| 场景 | HTTP | code |
|---|---:|---|
| lang、字段、slug、排序请求非法 | 400 | `90001` |
| token 缺失或失效 | 401 | `10002` |
| DEMO 执行写操作 | 403 | `10003` |
| 分类或标签不存在、已删除 | 404 | `90003` |
| slug 已占用、目标仍被文章引用 | 409 | `90004` |
| 更新行数异常、未知持久化状态 | 500 | `99999` |

错误消息使用后端稳定中文兜底，不回显 SQL、索引名和内部异常。

## 14. 测试策略

### 14.1 领域测试

- 三语名称 trim、空值和长度。
- slug 小写规范化、字符集、首尾和连续连字符。
- 分类排序边界。
- 可选语言名称 fallback。

### 14.2 Repository 测试

- active 查询和稳定排序。
- 已删除数据不可见。
- slug 查询包含已删除记录。
- ASSIGN_ID 插入和审计字段。
- 完整更新、分类批量排序和显式软删除。
- 分类、标签 active 文章引用判断。
- 所有 SQL 位于 XML。

### 14.3 Application 测试

- ADMIN、DEMO 查询权限。
- ADMIN 写权限和 principal ID 校验。
- 创建、完整编辑和 slug 冲突。
- 分类批量排序缺失目标与事务回滚。
- 被引用目标删除冲突。
- 未引用目标软删除成功。

### 14.4 Web、Security 与 OpenAPI

- 匿名公开三语读取与 fallback。
- ADMIN、DEMO 后台读取。
- DEMO 写操作 403，匿名后台请求 401。
- 未知字段、字段缺失和非法 slug 400。
- 路由方法和 schema 不暴露 Entity、Mapper、删除审计字段。

### 14.5 全量验证

- `mvn clean test`
- Maven Enforcer dependency convergence
- ArchUnit
- `git diff --check`
- content 模块无注解 SQL和通用物理删除
- application/domain 不依赖 web、Entity 或 Mapper
- Docker 不可用时只允许既有 Testcontainers MySQL 条件测试 skipped

## 15. 提交拆分

实施阶段保持五个中文提交：

1. `建立分类标签领域与持久化查询`
   - MapStruct、领域模型、Repository、Entity、Mapper XML 和基础查询。
2. `实现公开与后台分类标签查询`
   - 三语公开读取、后台 GET、Security 和 OpenAPI 查询契约。
3. `实现分类标签新增与编辑`
   - POST、PUT、完整字段契约、slug 唯一和审计。
4. `实现分类排序与引用保护删除`
   - 分类批量排序、分类/标签引用检查、显式软删除和并发测试。
5. `完成分类标签纵向切片`
   - 完整集成测试、接口契约、状态、路线图和全量验证。

每个提交只包含对应范围，不混入文章实现或其它模块重构。

## 16. 验收标准

- 匿名用户可按明确语言读取分类和标签，日英缺失时 fallback 到中文。
- ADMIN、DEMO 可读取后台完整数据，DEMO 不能写。
- ADMIN 可新增、完整编辑、排序分类并软删除未引用分类。
- ADMIN 可新增、完整编辑并软删除未引用标签。
- slug 按固定规则规范化并在各自表中永久唯一。
- 被 active 文章引用的分类或标签删除时返回 409，不自动解绑。
- 不返回文章数，不提前实现文章可见性规则。
- MapStruct 只承担机械映射，业务规则保持显式。
- 不修改冻结 DDL，不使用数据库外键、注解 SQL或通用物理删除。
- 新代码包含必要中文业务注释。
- 五批分别形成中文本地提交。
- 全量 Maven、Enforcer、ArchUnit 和静态检查通过。
