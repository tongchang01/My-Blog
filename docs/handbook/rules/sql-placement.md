# SQL 摆放规则

> 状态：当前有效
> 适用范围：MyBlog V2 后端所有持久层代码
> 最后校准：2026-06-29
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/infrastructure/persistence/`、`MyBlog-springboot-v2/src/main/resources/mapper/`
> 权威程度：规则

## 本文档回答什么问题

本文档规定一条 SQL 应该写在哪里：使用 MyBatis-Plus `BaseMapper`、Wrapper、Mapper 注解，还是 XML。目标是让 SQL 位置稳定、可测试、可审查，不让 SQL 上浮到 web/application/domain 层。

## 1. 总原则

- `web` 层禁止拼 SQL。
- `application` 层禁止拼 SQL。
- `domain` 层禁止保存 SQL 或依赖 Mapper。
- 所有 SQL 实现集中在 `infrastructure/persistence/` 和 `src/main/resources/mapper/`。
- 简单单表 CRUD 可用 MyBatis-Plus `BaseMapper`。
- 复杂查询、动态条件、聚合、排序、分页、projection 返回统一使用 XML。

## 2. 当前路径约定

Mapper 接口：

```text
MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/{module}/infrastructure/persistence/mapper/XxxMapper.java
```

Mapper XML：

```text
MyBlog-springboot-v2/src/main/resources/mapper/{module}/XxxMapper.xml
```

规则：

- XML 文件名必须等于 Mapper 接口名。
- XML `namespace` 必须等于 Mapper 接口全限定名。
- SQL `id` 必须等于 Mapper 方法名。
- XML 按模块放入 `mapper/{module}/`。

当前已有 XML 模块：common、identity、content、comment、system、stats。

## 3. SQL 写法选择

| 场景 | 推荐写法 | 说明 |
|------|----------|------|
| 单表按主键 CRUD | `BaseMapper` | 仅限 infrastructure 内使用 |
| 单表简单条件查询 | MyBatis-Plus Wrapper | 不得上浮到 application/domain |
| 多表 join | XML | 必须 |
| 动态 where | XML | 使用 `<where>` / `<if>` |
| 批量 IN | XML | 使用 `<foreach>`，禁止手拼占位符 |
| 聚合 count/group by/having | XML | 必须 |
| 后台列表筛选、分页、排序 | XML | 必须 |
| 返回 projection，而不是 Entity | XML | 必须 |
| SQL 超过 10 行 | XML | 必须 |
| 需要解释复杂业务口径 | XML | 注释和 SQL 放在一起 |
| 极短固定 SQL 注解 | 原则上不新增 | 当前生产代码不依赖注解 SQL，新增前需说明理由 |

## 4. 禁止事项

禁止：

- 在 Controller 中拼 SQL。
- 在 ApplicationService 中拼 SQL。
- 在 domain 对象或领域服务中拼 SQL。
- 使用 Java 字符串拼接动态条件。
- 手拼 `IN (...)` 占位符。
- 在 Mapper 注解中写多表 join 或复杂动态条件。
- 新增生产代码使用 `JdbcTemplate` 绕过既有持久化策略。
- 业务模块直接访问其它模块 Mapper。
- 把 Entity 泄漏到 web 响应。

## 5. XML 注释规则

复杂 SQL 必须用中文注释说明：

- 服务哪个业务场景。
- 为什么筛选这些状态。
- 聚合口径是什么。
- 为什么排除软删除或保留某些行。
- 排序规则为什么这样设计。
- 并发锁定或 `for update` 的目的。

示例：

```xml
<!-- 锁定当前可用 refresh token，保证同一旧 token 并发刷新时最多一次成功。 -->
<select id="selectActiveForUpdate" resultType="..." >
  ... for update
</select>
```

## 6. Projection 规则

如果查询结果不是完整数据库 Entity，应定义 projection 类型，并通过 XML 显式映射。

规则：

- projection 放在 infrastructure/persistence/projection 或同等持久层包下。
- projection 不得上浮为 web VO。
- application 层应拿到用例结果或领域模型，不直接依赖 MyBatis 细节。

## 7. 分页与排序规则

后台管理列表、公开文章列表等分页查询应满足：

- page 从 1 开始。
- size 必须校验边界。
- 排序字段由服务端白名单控制，不能直接信任前端字段名拼 SQL。
- 动态筛选进入 XML。
- 空结果返回空 records。

## 8. Flyway SQL 规则

Flyway 脚本位于：

```text
MyBlog-springboot-v2/src/main/resources/db/migration/
```

规则：

- 已执行的迁移脚本禁止修改。
- 后续 schema 变更使用新的 `V2__xxx.sql`、`V3__xxx.sql` 等。
- 禁止引入数据库外键 `FOREIGN KEY`。
- DDL 必须符合 V2 schema 和审计列/软删除约定。
- MySQL 方言变更发布前必须做真实方言验证。

## 9. 测试要求

以下场景必须有测试：

- XML 动态条件。
- 分页和排序。
- 聚合统计。
- 软删除过滤。
- 权限可见性或公开/后台可见性差异。
- 行锁、并发、唯一冲突兜底。
- Flyway 迁移。

测试命令见 `testing-policy.md`。

## 10. 当前现状

截至 2026-06-29，当前生产代码未发现 `@Select` / `@Update` / `@Insert` / `@Delete` 注解 SQL 用法。后续新增 SQL 时应优先遵守本文规则，不恢复旧的注解复杂 SQL 风格。

## 相关文档

- 包结构规则：`package-layout.md`
- 测试策略：`testing-policy.md`
- 持久化策略：`../architecture/persistence-strategy.md`（待迁移）
- Schema 设计：`../architecture/schema-design.md`（待迁移）
