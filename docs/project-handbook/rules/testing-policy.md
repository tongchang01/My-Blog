# 测试策略

> 本文档回答："什么必须写测试？用什么框架？怎么跑？"
> 适用范围：V2 所有代码。
> 最近全量结果和当次测试数量以 `../status.md` 或当前阶段 review 为准；本规则不固化
> 会持续变化的测试总数。

## 1. 测试技术栈

| 框架 | 用途 |
|------|------|
| JUnit 5 | 单元测试基础 |
| Spring Boot Test | 集成测试 |
| Spring Security Test | 安全相关集成测试 |
| H2 | 测试数据库（内存） |
| Flyway | H2 自动迁移到目标结构 |
| ArchUnit | 架构守护规则（必跑） |

## 2. 必须写测试的场景

1. **权限边界**：哪些角色能/不能访问；资源所有者校验
2. **状态流转**：评论审核通过 → 不可再删；已删除 → 可恢复
3. **安全规则**：JWT 签发/解析/撤销/过期；登录审计字段更新
4. **容易误删/误展示数据的场景**：软删除后前台是否还可见
5. **复杂 SQL**：自定义 XML 查询必须有 `DatabaseXxxReaderTest` 验证

## 3. 可以不写的场景

- 纯 getter/setter
- Spring 配置的样板 bean
- 简单的字符串拼接

## 4. ArchUnit 强制规则（任意违反即构建失败）

`src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`：

1. `..domain..` 不依赖 `..web..` / `..infrastructure..`
2. `..web..` 不访问 `..infrastructure.persistence.mapper..`
3. `..application..` 不直接访问 MyBatis-Plus Mapper
4. `..common..` 不依赖业务模块
5. 业务模块不互相访问对方 `infrastructure.persistence`

**新增模块时必须同步更新 ArchUnit 规则**，否则新模块无守护。

## 5. 命名约定

| 文件 | 用途 |
|------|------|
| `XxxTest` | 单元/集成测试 |
| `DatabaseXxxReaderTest` | 持久层读测试（连真实 H2） |
| `DatabaseXxxWriterTest` | 持久层写测试 |
| `XxxControllerTest` | Web 层测试（MockMvc / WebMvcTest） |
| `FlywayMigrationTest` | 验证迁移脚本能在 H2 上执行 |
| `ArchitectureRulesTest` | 架构守护（唯一一份） |

## 6. 测试方法命名

风格：`should{ExpectedBehavior}_when{Condition}` 或更口语化的 `{behavior}When{condition}`

```java
@Test
void excludesDeletedCommentsFromPublicList() { ... }

@Test
void rejectsLoginWhenPasswordIsWrong() { ... }
```

## 7. 测试数据库策略

- 本地开发（profile=local）：连真实 MySQL，**Flyway 关闭**（避免改坏开发库）
- 测试（profile=test）：H2 内存库，Flyway 启动时自动迁移
- 集成测试在 `@SpringBootTest` 时强制使用 H2

## 8. 必跑命令

```bash
mvn test                        # 全量测试（含 ArchUnit）
mvn test -Dtest=ArchitectureRulesTest    # 单跑架构守护
```

任何 PR / 重大改动前必须运行约定命令，并以 `0 failures / 0 errors` 为通过门槛。
Docker 不可用导致的 Testcontainers MySQL 条件测试跳过必须明确记录；发布前仍需完成
真实 MySQL 方言验证。测试数量只作为当次执行证据，不作为长期规范。

## 9. 测试覆盖率

- **不**强制最低覆盖率百分比
- 但关键模块（auth、comment 审核、文章访问控制）的核心路径**必须**覆盖
- 后续可引入 JaCoCo，但仅作参考

## 10. 当前覆盖状态

当前模块覆盖、已知缺测和最近一次全量测试证据见 `../status.md` 与当前阶段 review。
