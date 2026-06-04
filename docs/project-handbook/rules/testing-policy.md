# 测试策略

> 本文档回答："什么必须写测试？用什么框架？怎么跑？"
> 适用范围：V2 所有代码。
> 当前测试文件：33 份（含 `ArchitectureRulesTest`）

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

1. **旧库兼容逻辑**：`is_delete=0/1` ↔ `deleted=false/true` 之类的转换
2. **权限边界**：哪些角色能/不能访问；资源所有者校验
3. **状态流转**：评论审核通过 → 不可再删；已删除 → 可恢复
4. **安全规则**：JWT 签发/解析/撤销/过期；登录审计字段更新
5. **容易误删/误展示数据的场景**：软删除后前台是否还可见
6. **复杂 SQL**：自定义 XML 查询必须有 `DatabaseXxxReaderTest` 验证

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

任何 PR / 重大改动前必须本地跑通 `mvn test`。

## 9. 测试覆盖率

- **不**强制最低覆盖率百分比
- 但关键模块（auth、comment 审核、文章访问控制）的核心路径**必须**覆盖
- 后续可引入 JaCoCo，但仅作参考

## 10. 当前测试覆盖现状（2026-06）

| 模块 | 测试文件数 | 覆盖评估 |
|------|-----------|---------|
| comment | 6 | 较好（含 admin/audit） |
| content | 5 | 中等（缺 ApplicationService 集成） |
| identity | 8 | 较好（含登录审计） |
| common | 9 | 较好（安全、Web 工具均覆盖） |
| 架构守护 | 1 (ArchUnit) | ✅ 已启用 |
| Flyway | 1 | ✅ 已启用 |

**已知缺测**：
- `CommentCommandService` 集成测试
- `AdminCommentCommandService` 集成测试
- 评论软删除 → 恢复完整链路测试

详见 `../pitfalls.md` 与 `../arch/README.md` 中的待办。
