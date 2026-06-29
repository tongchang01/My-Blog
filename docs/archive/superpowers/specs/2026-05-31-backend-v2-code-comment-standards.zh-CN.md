# 后端 V2 注释与 Javadoc 编码规范

**日期：** 2026-05-31

**适用范围：** `MyBlog-springboot-v2`

**状态：** 生效。后续新增 Java 代码必须遵守；已有 V2 代码需要按计划逐步补齐。

---

## 1. 背景

我这次重构后端 V2，不只是为了让功能能跑，还要解决旧项目中代码不优雅、注释偏少、业务意图不清晰的问题。

当前 V2 代码已经有基础结构和部分业务模块，但注释标准还没有建立，很多类、方法、字段缺少必要说明。随着后续业务继续迁移，如果不现在规定注释规范，后面代码量越大，补齐成本越高，也会让后续前后台联调、接口文档生成、表结构重构都变得困难。

因此，本规范从现在开始生效，并作为后端 V2 的编码质量标准之一。

---

## 2. 总原则

### 2.0 注释统一使用中文

后端 V2 的 Javadoc、字段注释、行内注释、测试场景注释、OpenAPI 字段描述必须统一使用中文。

允许保留英文的情况仅限于：

- Java、Spring、MyBatis-Plus、JWT、OpenAPI 等标准技术名词。
- 数据库字段名、表名、包名、类名、方法名、配置 key。
- 第三方 API、协议、HTTP header、错误码等固定英文标识。

不允许出现中英文混杂导致语义不清的注释。注释面向的是未来维护者和前后台联调人员，必须优先保证中文业务含义清楚。

### 2.1 注释要解释业务语义

注释不是翻译代码，而是解释代码背后的业务含义、数据来源、状态边界和维护注意事项。

不合格示例：

```java
/**
 * 获取评论。
 */
public List<CommentResponse> listComments() {
    ...
}
```

合格示例：

```java
/**
 * 查询前台文章详情页可展示的评论列表。
 *
 * <p>只返回已审核且未删除的一级评论，并按创建时间倒序排列。
 * 后台审核中、已删除、被隐藏的评论不应出现在前台响应中。</p>
 */
public List<CommentResponse> listVisibleComments(long articleId) {
    ...
}
```

### 2.2 必须注释的地方不能省

以下内容必须写 Javadoc 或字段注释：

- Entity 类。
- Entity 字段。
- DTO、Command、Query、Response 类。
- DTO、Command、Query、Response 字段。
- Controller 类和公开接口方法。
- Application Service 类和公开业务方法。
- Domain Service、领域对象、领域规则方法。
- Repository、Reader、Writer、Mapper 接口和复杂查询方法。
- Enum 类和每个枚举值。
- 配置属性类和每个配置项。
- 复杂业务判断、状态流转、权限判断、兼容旧库逻辑。

### 2.3 不写无意义注释

以下注释禁止作为主要注释使用：

```java
/**
 * 设置名称。
 */
public void setName(String name) {
    this.name = name;
}
```

```java
// 如果用户为空
if (user == null) {
    ...
}
```

这类注释只是重复代码，没有维护价值。注释必须说明业务原因，而不是简单复述语法。

---

## 3. Entity 注释规范

### 3.1 Entity 类必须声明对应表

每个实体类必须写明对应数据库表、业务用途、是否兼容旧库。

示例：

```java
/**
 * 评论实体，对应旧库表 {@code t_comment}。
 *
 * <p>该实体承载前台评论提交、后台审核、软删除和恢复所需的数据。
 * V2 迁移期间仍兼容旧库字段命名，字段含义以数据库中文注释和本文档为准。</p>
 */
public class CommentEntity {
}
```

### 3.2 Entity 字段必须声明数据库字段和中文含义

每个 Entity 字段必须说明：

- 对应表字段。
- 中文业务含义。
- 关键取值规则或状态含义。
- 是否允许为空。
- 兼容旧库时的特殊说明。

示例：

```java
/**
 * 评论主键，对应 {@code t_comment.id}。
 */
private Long id;

/**
 * 评论内容，对应 {@code t_comment.comment_content}。
 *
 * <p>前台用户提交的正文内容。入库前必须完成基础文本校验，
 * 后台展示时需要按接口层规则做必要转义。</p>
 */
private String commentContent;

/**
 * 审核状态，对应 {@code t_comment.is_review}。
 *
 * <p>旧库使用数字状态：{@code 0} 表示未审核，{@code 1} 表示已审核。
 * 业务层应优先转换为明确的枚举语义，避免在业务代码中散落魔法数字。</p>
 */
private Integer reviewStatus;
```

### 3.3 MyBatis-Plus 注解不能替代注释

`@TableName`、`@TableId`、`@TableField` 只能说明映射关系，不能替代中文业务注释。

示例：

```java
/**
 * 用户头像地址，对应 {@code t_user_info.avatar}。
 *
 * <p>当前旧库保存的是可直接访问的 URL。后续如果迁移到 AWS S3，
 * 该字段可能改为对象 key 或通过文件服务转换为临时访问地址。</p>
 */
@TableField("avatar")
private String avatar;
```

---

## 4. DTO、Command、Query、Response 注释规范

### 4.1 接口入参和出参字段必须注释

这些对象会被前端、后台、接口文档和测试共同依赖，字段注释必须写清楚。

示例：

```java
/**
 * 评论提交请求。
 */
public record CreateCommentCommand(
        /**
         * 文章 ID，对应要评论的文章主键。
         */
        Long articleId,

        /**
         * 评论正文，不能为空，长度限制由接口校验规则控制。
         */
        String content,

        /**
         * 父评论 ID。
         *
         * <p>为空表示提交一级评论；不为空表示回复指定评论。</p>
         */
        Long parentId
) {
}
```

### 4.2 Response 字段要面向前端解释

Response 注释要说明前端如何理解该字段，而不只是说明数据库来源。

示例：

```java
/**
 * 评论是否已通过审核。
 *
 * <p>前台只展示 {@code true} 的评论；后台可以通过该字段区分待审核和已审核评论。</p>
 */
private boolean reviewed;
```

---

## 5. Controller 注释规范

### 5.1 Controller 类必须说明接口归属

示例：

```java
/**
 * 前台评论接口。
 *
 * <p>负责文章详情页评论展示、评论提交和回复查询。
 * 后台审核、删除、恢复等管理能力不在本控制器中暴露。</p>
 */
@RestController
class CommentController {
}
```

### 5.2 公开接口方法必须说明业务边界

示例：

```java
/**
 * 提交文章评论。
 *
 * <p>该接口只负责接收前台用户评论并记录提交端信息。
 * 评论是否需要审核、是否立即展示，由应用服务根据站点规则决定。</p>
 */
@PostMapping("/api/comments")
ResponseEntity<CommentResponse> create(@RequestBody CreateCommentCommand command) {
    ...
}
```

---

## 6. Application Service 注释规范

### 6.1 类注释说明业务用例

示例：

```java
/**
 * 评论应用服务。
 *
 * <p>编排评论提交、查询、审核状态判断和客户端信息记录。
 * 该类不直接关心 SQL 细节，持久化能力由 comment infrastructure 提供。</p>
 */
public class CommentApplicationService {
}
```

### 6.2 方法注释说明流程、状态和权限

示例：

```java
/**
 * 审核通过指定评论。
 *
 * <p>仅后台管理员可以调用。审核通过后，评论会在前台文章详情页可见。
 * 如果评论已被软删除，不允许直接审核通过，必须先恢复评论。</p>
 *
 * @param commentId 评论 ID
 * @param operatorId 当前后台操作人用户 ID
 */
public void approveComment(long commentId, long operatorId) {
    ...
}
```

---

## 7. Repository、Reader、Writer、Mapper 注释规范

### 7.1 接口必须说明查询或写入职责

示例：

```java
/**
 * 评论读取端口。
 *
 * <p>封装前台评论展示所需的读取能力。实现层负责处理旧库字段映射、
 * 审核状态、软删除状态和分页查询。</p>
 */
public interface CommentReader {
}
```

### 7.2 复杂查询必须解释筛选条件

示例：

```java
/**
 * 查询文章下可公开展示的一级评论。
 *
 * <p>筛选条件必须包含：指定文章、父评论为空、已审核、未删除。
 * 排序规则为创建时间倒序，避免新评论被隐藏在列表末尾。</p>
 */
List<CommentEntity> findVisibleRootComments(long articleId, PageRequest pageRequest);
```

---

## 8. Enum 注释规范

枚举类必须说明业务场景。每个枚举值必须说明中文含义和旧库对应值。

示例：

```java
/**
 * 评论审核状态。
 */
public enum CommentReviewStatus {
    /**
     * 未审核，对应旧库 {@code t_comment.is_review = 0}。
     */
    PENDING,

    /**
     * 已审核，对应旧库 {@code t_comment.is_review = 1}。
     */
    APPROVED
}
```

---

## 9. 配置类注释规范

配置类和配置字段必须说明用途、默认值风险和生产环境要求。

示例：

```java
/**
 * JWT 配置项。
 *
 * <p>生产环境必须通过环境变量覆盖默认密钥，不能使用代码中的开发默认值。</p>
 */
@ConfigurationProperties("myblog.security.jwt")
public record SecurityJwtProperties(
        /**
         * JWT 签发者标识，用于区分当前系统签发的 token。
         */
        String issuer,

        /**
         * JWT 签名密钥。
         *
         * <p>生产环境必须至少 32 字节，并通过安全配置注入。</p>
         */
        String secret
) {
}
```

---

## 10. 业务逻辑行内注释规范

### 10.1 需要行内注释的场景

以下代码块必须写行内注释：

- 兼容旧库字段或旧状态值。
- 审核、恢复、软删除等状态流转。
- 权限边界判断。
- 登录审计、访问审计、客户端 IP 解析。
- 涉及时间、过期、锁定、限流的逻辑。
- 为了前端兼容而保留的特殊返回值。

### 10.2 行内注释示例

```java
// 旧库使用 is_delete = 0 表示未删除，V2 在业务层统一转换为 deleted=false。
boolean deleted = row.getInt("is_delete") == 1;
```

```java
// 已软删除的评论不能直接审核通过，否则前台会展示被管理员删除过的内容。
if (comment.isDeleted()) {
    throw new BusinessException("已删除评论不能直接审核");
}
```

---

## 11. 测试注释规范

普通单元测试不强制写注释，但以下测试必须说明业务场景：

- 覆盖旧库兼容逻辑。
- 覆盖权限边界。
- 覆盖状态流转。
- 覆盖安全规则。
- 覆盖容易误删或误展示数据的场景。

示例：

```java
/**
 * 验证已删除评论不会出现在前台列表中。
 *
 * <p>该场景保护后台软删除语义，避免用户在文章详情页继续看到已处理评论。</p>
 */
@Test
void excludesDeletedCommentsFromPublicList() {
    ...
}
```

---

## 12. 和 OpenAPI 文档的关系

Javadoc 是代码可维护性的基础。OpenAPI 注解用于接口文档展示，两者不能互相替代。

后续引入 `springdoc-openapi` 后：

- DTO、Command、Response 字段仍然必须保留 Javadoc。
- 需要展示到接口文档的字段，再补充 `@Schema(description = "...")`。
- `@Schema` 的中文描述应和 Javadoc 保持一致，不允许出现两套含义。

---

## 13. 已有代码补齐规则

已有 V2 代码不要求一次性全部补完，但必须按模块逐步补齐。

推荐顺序：

1. common：配置、安全、错误处理、Web 支撑能力。
2. identity：登录、用户、角色、菜单、审计。
3. content：文章、分类、标签、访问控制。
4. comment：评论提交、展示、审核、删除、恢复、审计。
5. infrastructure：数据库、认证适配、外部服务适配。

每个模块补注释时，应遵守以下要求：

- 不改变业务行为。
- 不混入无关重构。
- 不为了补注释而大面积改代码格式。
- 如果发现注释无法写清楚，说明代码命名或职责可能有问题，应单独记录重构点。

---

## 14. 后续开发硬规则

从本规范提交后开始，后端 V2 新增或修改代码必须遵守：

- 新增或修改的 Javadoc、字段注释、行内注释、测试场景注释必须使用中文。
- 新增 Entity 必须有类注释和字段注释。
- 新增 DTO、Command、Query、Response 必须有类注释和字段注释。
- 新增 Controller 公开接口必须有业务边界注释。
- 新增 Application Service 公开方法必须有业务流程注释。
- 新增 Enum 必须说明每个枚举值的中文含义。
- 新增 Mapper、Reader、Writer、Repository 复杂查询必须说明筛选条件。
- 新增配置项必须说明用途和生产风险。
- 涉及旧库兼容、权限、审核、软删除、审计、限流、时间过期的逻辑必须写行内注释。

如果某段代码不需要注释，也应该满足一个前提：仅通过命名和结构，维护者就能准确理解业务含义。

---

## 15. 近期执行建议

1. 先将本规范提交。
2. 在《后端 V2 项目结构决策记录》中引用本规范。
3. 在 MyBatis-Plus 基础设施计划里明确 Entity、Mapper、XML、DTO 的注释要求。
4. 安排一轮“现有 V2 代码注释补齐计划”。
5. 后续每个业务迁移任务都把注释作为完成条件之一。
