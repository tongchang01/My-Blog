# 代码注释规范

> 本文档回答："注释该写什么、写在哪、用什么语言？"
> 适用范围：V2 所有 Java 代码、配置文件、测试。
> 相关 ADR：`../decisions/0011-chinese-only-comments.md`

## 1. 语言

- **统一中文**。Javadoc、字段注释、行内注释、测试场景注释、OpenAPI 字段描述都用中文
- **允许保留英文**：技术名词（Spring、MyBatis-Plus、JWT 等）、数据库字段名、包/类/方法名、配置 key、HTTP header、错误码等固定标识
- **禁止**中英混杂导致语义不清

## 2. 核心原则

> 注释要解释**业务语义**，而不是翻译代码。

```java
// ❌ 翻译代码
// 如果用户为空
if (user == null) { }

// ✅ 解释为什么
// 旧库允许用户软删除后仍保留登录记录，此处空值表示账号已注销
if (user == null) { }
```

## 3. 必须写注释的地方

| 类型 | 必写内容 |
|------|---------|
| Entity 类 | 对应表、业务用途、是否为过渡实现或 V1 导入映射 |
| Entity 字段 | 对应字段、中文含义、取值规则、是否可空 |
| DTO/Command/Query/Response 类 + 字段 | 业务用途；Response 面向前端解释 |
| Controller 类 | 接口归属、职责边界 |
| Controller 公开方法 | 业务边界 |
| ApplicationService 类 | 业务用例 |
| ApplicationService 方法 | 流程、状态、权限 |
| Domain Service / 领域规则方法 | 业务规则 |
| Repository/Reader/Writer/Mapper 接口 | 查询/写入职责 |
| Repository/Reader/Writer/Mapper 复杂方法 | 筛选条件说明 |
| Enum 类 | 业务场景 |
| Enum 值 | 中文含义（如确有历史含义需对照，可附在 ADR 而不在代码注释里铺陈）|
| 配置类 + 字段 | 用途、默认值风险、生产环境要求 |

## 4. 必须写**行内注释**的业务场景

- 审核、恢复、软删除等状态流转
- 权限边界判断
- 登录审计、访问审计、客户端 IP 解析
- 时间、过期、锁定、限流相关逻辑
- 任何"看代码不知道为什么这么写"的特殊处理

> 注：早期 V2 曾要求标注"旧库兼容字段"，现已转向全量重设计（ADR-0013），无需再为旧库语义写兼容注释。

## 5. 禁止无意义注释

```java
// ❌ 只重复代码
/** 获取用户。*/
public User getUser() { }

/** 设置名称。*/
public void setName(String name) { }
```

## 6. 注解类注释位置

`@TableName`、`@TableId`、`@TableField` 等注解**只**说明映射关系，**不能**替代中文业务注释。

```java
/**
 * 审核状态，对应 {@code t_comment.is_review}。
 *
 * <p>旧库使用数字状态：{@code 0} 表示未审核，{@code 1} 表示已审核。
 * 业务层应优先转换为明确的枚举语义，避免在业务代码中散落魔法数字。</p>
 */
@TableField("is_review")
private Integer reviewStatus;
```

## 7. OpenAPI 与 Javadoc 的关系

- Javadoc 是代码可维护性的基础
- OpenAPI（`@Schema(description=...)`）用于接口文档展示
- **两者并存**：DTO/Command/Response 字段必须保留 Javadoc，再补 `@Schema`
- `@Schema` 描述与 Javadoc 内容保持一致

## 8. 测试注释规则

普通单元测试不强制写注释，**以下场景必须**说明业务场景：
- V1 数据导入映射或过渡实现逻辑
- 权限边界
- 状态流转
- 安全规则
- 容易误删/误展示数据的场景

```java
/**
 * 验证已删除评论不会出现在前台列表中。
 * <p>该场景保护后台软删除语义，避免用户在文章详情页继续看到已处理评论。</p>
 */
@Test
void excludesDeletedCommentsFromPublicList() { ... }
```

## 9. 新增代码硬规则（强制）

新增或修改的代码：

- 新增 Entity → 类注释 + 字段注释**全要**
- 新增 DTO/Command/Query/Response → 类注释 + 字段注释**全要**
- 新增 Controller 公开接口 → 业务边界注释
- 新增 ApplicationService 公开方法 → 业务流程注释
- 新增 Enum → 每个枚举值的中文含义
- 新增 Mapper/Reader/Writer/Repository 复杂查询 → 筛选条件说明
- 新增配置项 → 用途 + 生产风险
- 涉及权限/审核/软删除/审计/限流/时间过期 → 行内注释

## 10. 不写注释的唯一前提

仅当**命名和结构足以让维护者准确理解业务含义**时，方可省略注释。这是高门槛，默认还是写。
