# 后端 V2 持久层 SQL 放置规范

**日期：** 2026-06-01  
**适用范围：** `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/infrastructure/persistence`  
**状态：** 当前有效规范  

---

## 1. 目标

我引入 MyBatis-Plus 的目的不是简单把 `JdbcTemplate` 字符串 SQL 搬到 Mapper 注解里，而是把持久层访问方式收敛到可维护、可测试、可解释的统一规则下。

这份规范用于回答一个问题：后端 V2 的 SQL 应该放在哪里。

---

## 2. 总体原则

- `web` 和 `application` 层禁止拼 SQL，也禁止直接依赖 Mapper。
- `domain` 层只定义业务端口和领域模型，不保存 SQL。
- `infrastructure` 层负责实现持久化细节。
- Entity、Mapper、XML 都属于 `infrastructure.persistence` 范围。
- 简单单表操作优先使用 MyBatis-Plus 能力。
- 复杂 SQL 必须进入 XML，不再写在 Java 注解或 Java 字符串中。
- 旧库兼容条件、字段语义、状态位含义必须用中文注释写清楚。

---

## 3. 放置规则

| 场景 | 推荐放置方式 | 说明 |
| --- | --- | --- |
| 单表按主键查询、插入、更新、删除 | MyBatis-Plus `BaseMapper` | 直接使用框架能力，不额外写 SQL |
| 单表简单条件查询 | `LambdaQueryWrapper` | 只能放在 `infrastructure` 层，不允许上浮到业务层 |
| 短小、固定、无 join、无动态条件的查询 | Mapper 注解 SQL | 仅限非常简单的过渡场景 |
| 多表 join | XML | 必须进入 XML |
| 动态 `where` | XML | 使用 `<if>`、`<where>` 等 XML 能力 |
| 批量 `in` 查询 | XML | 使用 `<foreach>`，禁止在 Java 中手动拼占位符 |
| 聚合统计 | XML | 例如 `count`、`group by`、`having` |
| 分页、排序、多条件组合查询 | XML | 尤其是后台管理列表 |
| 返回 projection DTO 或 row record | XML | 查询结果不是 Entity 时优先 XML |
| SQL 超过 10 行 | XML | 代码审查时按硬规则处理 |
| 需要解释旧库兼容逻辑的 SQL | XML | XML 中写中文注释 |

---

## 4. 禁止事项

- 禁止在 `application` 层拼 SQL。
- 禁止在 `web` 层拼 SQL。
- 禁止把多表 join 写进 `@Select`。
- 禁止把动态查询条件写进 Java 字符串拼接。
- 禁止在 Java 中手动拼接 `in (?, ?, ?)` 占位符。
- 禁止为了省事把后台管理列表查询写成超长 Mapper 注解。
- 禁止新增生产代码对 `JdbcTemplate` 的直接依赖，除非先单独写明例外原因并获得确认。

---

## 5. XML 命名和路径

Mapper 接口路径示例：

```text
MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/ContentCatalogMapper.java
```

对应 XML 路径示例：

```text
MyBlog-springboot-v2/src/main/resources/mapper/content/ContentCatalogMapper.xml
```

命名规则：

- XML 文件名与 Mapper 接口名一致。
- XML namespace 必须使用 Mapper 接口全限定名。
- SQL id 必须与 Mapper 方法名一致。
- 同一业务模块的 XML 放在 `resources/mapper/{module}/` 下。

---

## 6. 中文注释要求

复杂 SQL 的 XML 中需要写中文注释，说明：

- 这段 SQL 服务哪个业务场景。
- 涉及哪些旧表。
- 关键状态字段含义，例如 `is_delete`、`status`、`is_review`。
- 旧库兼容条件为什么存在。
- 排序规则为什么这样写。

示例：

```xml
<!--
  查询前台可见的分类摘要。
  t_article.status = 1 表示已发布文章，is_delete = 0 表示旧库未删除数据。
  使用 left join 是为了保留暂无文章的分类。
-->
<select id="listCategorySummaries" resultType="...">
</select>
```

---

## 7. 当前需要立即修正的样板

`ContentCatalogMapper` 当前已经使用 MyBatis-Plus，但复杂查询仍放在 `@Select` 注解中：

- `listCategorySummaries()`
- `listTagSummaries()`
- `listTopTagSummaries(int limit)`

这些 SQL 包含 join、聚合、分组、排序和旧库状态过滤，必须在后续风险收口任务中迁入 XML。

---

## 8. 审查口径

后续每次新增或迁移持久层代码时，按以下问题审查：

1. 这是不是单表简单查询？
2. 有没有 join？
3. 有没有动态条件？
4. 有没有聚合统计？
5. 有没有分页排序？
6. 返回结果是不是 Entity？
7. SQL 是否超过 10 行？
8. 是否需要解释旧库字段或状态含义？

只要第 2 到第 8 个问题中任意一个答案为“是”，默认进入 XML。

