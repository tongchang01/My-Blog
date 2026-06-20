# SQL 摆放规则

> 本文档回答："这条 SQL 该写在哪？用 BaseMapper、`@Select` 注解，还是 XML？"
> 适用范围：V2 所有持久层代码。
> 相关 ADR：`../decisions/0005-mybatis-plus-as-primary-orm.md`、`../decisions/0010-sql-placement-strategy.md`

## 1. 总原则

- `web` 与 `application` 层**禁止**拼 SQL
- `domain` 层只定义业务端口，**不**保存 SQL
- 所有 SQL 实现集中在 `infrastructure/persistence/`
- 简单单表用 MyBatis-Plus `BaseMapper`，复杂查询必进 XML

## 2. 三种写法的选择

| 场景 | 推荐写法 | 备注 |
|------|---------|------|
| 单表按主键 CRUD | `BaseMapper` | 直接用框架能力 |
| 单表简单条件查询 | `LambdaQueryWrapper` | 仅在 `infrastructure` 内使用，不上浮 |
| 短小固定、无 join、无动态条件 | `@Select` 注解 | 仅过渡使用 |
| 多表 join | **XML** | 必须 |
| 动态 where（`<if>` / `<where>`） | **XML** | 必须 |
| 批量 `IN` 查询 | **XML** 用 `<foreach>` | 禁止手拼占位符 |
| 聚合（count / group by / having） | **XML** | 必须 |
| 分页、排序、多条件组合 | **XML** | 后台管理列表尤其如此 |
| 返回 projection DTO（不是 Entity） | **XML** | 优先 XML |
| SQL 超过 10 行 | **XML** | 硬规则 |
| 需要解释复杂业务口径 | **XML** | 注释与 SQL 放在一起 |

## 3. 审查口径

只要 SQL 满足以下任一条，**直接进 XML**：

1. 有 join
2. 有动态条件
3. 有聚合统计
4. 有分页排序
5. 返回结果不是 Entity
6. SQL 超过 10 行
7. 需要解释复杂业务筛选、聚合或排序口径

## 4. 禁止事项

- ❌ `application` / `web` 层拼 SQL
- ❌ `@Select` 写多表 join
- ❌ Java 字符串拼接动态条件
- ❌ Java 中手拼 `in (?,?,?)` 占位符
- ❌ 后台管理列表写成超长 `@Select`
- ❌ 新增生产代码引入 `JdbcTemplate`

## 5. XML 命名与路径约定

```
Mapper 接口：
  com.tyb.myblog.v2.content.infrastructure.persistence.mapper.ContentCatalogMapper

对应 XML：
  src/main/resources/mapper/content/ContentCatalogMapper.xml
```

- XML 文件名 == Mapper 接口名
- XML `namespace` == Mapper 接口全限定名
- SQL `id` == Mapper 方法名
- 按模块归类：`resources/mapper/{module}/`

## 6. XML 注释要求

复杂 SQL 必须写中文注释说明：
- 服务于哪个业务场景
- 关键表和状态条件的业务含义
- 聚合口径或排除条件为何存在
- 排序规则为何这样写

## 7. 正反例

### ✅ 正例：简单单表查询用注解

```java
@Select("select * from t_article where id = #{id}")
ArticleEntity findById(Long id);
```

### ✅ 正例：复杂多表查询用 XML

```xml
<!-- 查询公开分类摘要；只统计已发布且未删除文章，空分类也保留。 -->
<select id="listCategorySummaries" resultType="...CategorySummaryDTO">
  SELECT c.id, c.name, COUNT(a.id) AS article_count
  FROM t_category c
  LEFT JOIN t_article a ON c.id = a.category_id AND a.deleted = 0 AND a.status = 2
  WHERE c.deleted = 0
  GROUP BY c.id
  ORDER BY c.create_time DESC
</select>
```

### ❌ 反例：多表 join 写在注解里

```java
// 禁止
@Select("select a.*, c.name from t_article a left join t_category c ...")
List<X> listArticles(...);
```

### ❌ 反例：Java 字符串拼条件

```java
// 禁止
String sql = "select * from t_comment where 1=1";
if (status != null) sql += " and status = " + status;
```

## 8. 当前已知违规（待修复）

- `content.infrastructure.persistence.mapper.ContentCatalogMapper` 中 `listCategorySummaries()`、`listTagSummaries()`、`listTopTagSummaries()` 仍是 `@Select`，按本规则**必须**迁入 XML。见 `pitfalls.md` 与风险收敛计划 Task 9。

## 9. 例外

无。本规则对 V2 全部新代码强制执行。
