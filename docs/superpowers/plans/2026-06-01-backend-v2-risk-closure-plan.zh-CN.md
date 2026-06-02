# 后端 V2 风险收口实施计划

> **给执行该计划的代理：** 必须逐个任务执行。用户要求“一个任务一个任务来”，每个任务完成、验证、提交后必须停下汇报，不要一次性把所有任务做完。任务完成后同步勾选本文件中的 checkbox。

**日期：** 2026-06-01  
**输入依据：** `E:/Data/backend-v2-refactor-risk-review.zh-CN.md`  
**适用范围：** `MyBlog-springboot-v2`、`docs/superpowers/**`  
**当前状态：** 已核对 Review 中的主要风险，结论为“需要先收口工程规则和安全风险，再继续推进原 MyBatis-Plus 迁移计划”。  

---

## 1. 计划目标

本计划不是新增业务功能，而是把当前后端 V2 重构已经暴露出的风险先收口，避免项目继续向后推进时规则发散。

我要解决的问题是：

- 我已经把包名、模块结构、注释规范、MyBatis-Plus 方向定下来，但部分文档和代码还没有完全同步。
- 我已经引入 MyBatis-Plus，但当前只是试迁移阶段，生产代码仍有大量 `JdbcTemplate`。
- 我发现复杂 SQL 有继续写进 Mapper 注解的趋势，这会把原来 Java 字符串 SQL 的可维护性问题换一种形式保留下来。
- 我发现安全白名单只按 path 匹配，无法表达 `GET /api/comments` 公开但 `POST /api/comments` 需要登录。
- 我发现部分基础配置仍带有过渡期痕迹，例如 Maven `groupId` 还是旧值、JWT secret 有默认 fallback、Javadoc 位置不统一。

本计划完成后，再回到 `2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md` 继续按模块迁移持久层。

---

## 2. 已确认风险

| 优先级 | 风险                      | 已核对事实                                                                                | 收口方向                        |
| --- | ----------------------- | ------------------------------------------------------------------------------------ | --------------------------- |
| P0  | 复杂 SQL 规则过松             | `ContentCatalogMapper` 已出现 `left join`、`count`、`group by`、`order by` 的 `@Select` SQL | 明确复杂 SQL 必须进入 XML           |
| P0  | 安全白名单缺少 HTTP Method     | `myblog.security.public-endpoints` 当前是 path 列表，`/api/comments` 同时承载 GET 和 POST       | 改为 `method + path` 配置，并补测试  |
| P0  | 原迁移计划继续推进前规则不够稳         | MyBatis-Plus 迁移计划中存在“Mapper 注解或 XML”的宽松表述                                            | 先修正文档，再继续迁移                 |
| P1  | 文档权威关系不清                | 早期文档里仍可看到旧包名、旧结构或旧术语                                                                 | 增加 docs 入口说明，标明当前权威文档       |
| P1  | Maven 坐标仍是旧命名           | `pom.xml` 中 `groupId` 仍为 `com.aurora`                                                | 同步为 `com.tyb`               |
| P1  | 注释规范未完全落地               | 多处类注释位于 Spring 注解之后                                                                  | 统一 Javadoc 放在注解之前           |
| P1  | JWT secret 有默认 fallback | `application.yml` 存在 `change-me...` 默认值                                              | 生产缺失 secret 时启动失败，本地和测试显式配置 |
| P2  | Bearer token 解析分散       | `AuthController.logout` 直接 `replaceFirst("Bearer ", "")`                             | 抽取统一解析器                     |
| P2  | 401/403 语义边界不够清楚        | `ApiErrorCode.UNAUTHORIZED` 注释含义接近 403                                               | 清理命名或注释，补足测试                |

---

## 3. 执行原则

- 每个任务单独提交，提交信息必须使用中文。
- 每个任务完成后必须勾选本计划对应任务。
- 行为变更任务必须先补测试，再改生产代码。
- 文档、格式、坐标类任务可以不先写测试，但必须运行 `git diff --check` 和 Maven 验证。
- 不继续扩大重构范围，不新增 Redis、MQ、部署流水线或新业务能力。
- 当前阶段不改线上运行服务，只在 `backend-v2-refactor` 分支推进。
- 复杂 SQL 进入 XML 是硬规则，不再使用“注解或 XML 均可”的模糊表达。

---

## 4. 任务列表

## Task 1: 收口持久层 SQL 放置规则

**目的：** 先把 MyBatis-Plus 迁移规则定死，避免后续继续产生复杂注解 SQL。

**Files:**

- Create: `docs/superpowers/specs/2026-06-01-backend-v2-persistence-sql-placement-rules.zh-CN.md`

- Modify: `docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md`

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [x] **Step 1: 新增 SQL 放置规范**

新增规范文档，明确：

- 单表按 ID 查询优先使用 MyBatis-Plus `BaseMapper`。

- 单表简单条件查询可使用 `LambdaQueryWrapper`，但只能在 `infrastructure` 层。

- 很短、固定、无 join、无动态条件的 SQL 可以使用注解。

- 多表 join、动态 where、聚合统计、分页排序、后台管理查询、projection DTO 查询必须使用 XML。

- SQL 超过 10 行或需要解释旧库兼容规则时，必须使用 XML 并写中文注释。

- [x] **Step 2: 修正 MyBatis-Plus 迁移计划**

把原计划中的宽松规则：

```text
多表聚合、动态条件、批量更新优先使用 Mapper 注解或 XML
```

修正为：

```text
多表聚合、动态条件、批量更新、分页排序、后台管理查询必须使用 XML
```

并补充 `ContentCatalogMapper` 已有复杂注解 SQL 需要优先迁入 XML。

- [x] **Step 3: 验证**

Run:

```powershell
rg "Mapper 注解或 XML|注解或 XML" docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md
git diff --check
```

Expected:

- `rg` 不再找到“注解或 XML”的宽松表达。

- `git diff --check` 无输出。

- [x] **Step 4: 提交**

```powershell
git add docs/superpowers/specs/2026-06-01-backend-v2-persistence-sql-placement-rules.zh-CN.md docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "收口后端V2持久层SQL放置规则"
```

---

## Task 2: 增加后端 V2 文档权威入口

**目的：** 解决文档越来越多后“该看哪份、哪份更新、哪份只是历史记录”的问题。

**Files:**

- Create: `docs/superpowers/README.md`

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [x] **Step 1: 新增 docs 入口**

入口文档至少说明：

- 当前权威结构文档是哪一份。

- 当前权威框架决策文档是哪一份。

- 当前权威 MyBatis-Plus 迁移文档是哪一份。

- 当前风险收口计划是哪一份。

- 早期文档若与当前决策冲突，以最新权威文档为准。

- [x] **Step 2: 验证**

Run:

```powershell
git diff --check
rg "com.aurora|modules|api 层|api层" docs/superpowers/README.md
```

Expected:

- `git diff --check` 无输出。

- README 不再把旧包名、旧 `modules` 结构、旧 `api` 层作为当前推荐结构。

- [x] **Step 3: 提交**

```powershell
git add docs/superpowers/README.md docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "新增后端V2文档权威入口"
```

---

## Task 3: 同步 Maven 坐标到新包名

**目的：** Java 包名已经迁到 `com.tyb.myblog.v2`，Maven 坐标继续保留 `com.aurora` 会让工程身份不一致。

**Files:**

- Modify: `MyBlog-springboot-v2/pom.xml`

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [x] **Step 1: 修改 groupId**

把 `MyBlog-springboot-v2/pom.xml` 中项目自身 `groupId` 改为：

```xml
<groupId>com.tyb</groupId>
```

不改依赖坐标，不改包名，不改业务代码。

- [x] **Step 2: 验证**

Run:

```powershell
cd MyBlog-springboot-v2
mvn test
```

Expected: `BUILD SUCCESS`。

- [x] **Step 3: 提交**

```powershell
git add MyBlog-springboot-v2/pom.xml docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "同步后端V2 Maven 坐标"
```

---

## Task 4: 统一中文 Javadoc 位置

**目的：** 把已经写下的注释规范落到代码风格上，避免“注释写了但位置不标准”。

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/**/*.java`

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [x] **Step 1: 搜索 Javadoc 位于注解之后的类**

Run:

```powershell
rg -n -U "@(RestController|Service|Component|Repository|Configuration|ConfigurationProperties|Mapper|SpringBootApplication)[\\s\\S]{0,240}/\\*\\*" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2
```

Expected: 列出需要修正的位置。

- [x] **Step 2: 调整注释位置**

统一为：

```java
/**
 * 中文类说明。
 */
@Service
public class ExampleService {
}
```

不改方法逻辑、不改字段、不改导入。

- [x] **Step 3: 验证**

Run:

```powershell
cd MyBlog-springboot-v2
mvn test
```

Expected: `BUILD SUCCESS`。

- [x] **Step 4: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2 docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "统一后端V2中文注释位置"
```

---

## Task 5: 安全白名单支持 HTTP Method

**目的：** 修复 `GET /api/comments` 与 `POST /api/comments` 共用 path 导致的放行边界不清问题。

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/SecurityPublicEndpointProperties.java`

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java`

- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`

- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`

- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`

- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [x] **Step 1: 先补安全回归测试**

在 `SecurityConfigTest` 中增加或确认：

- 匿名访问 `GET /api/comments` 允许通过。
- 匿名访问 `POST /api/comments` 必须返回 401。
- 其他未配置公开端点默认受保护。

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=SecurityConfigTest" test
```

Expected: 新增 `POST /api/comments` 断言先失败，证明当前 path-only 白名单存在风险。

- [x] **Step 2: 修改配置模型**

把公开端点从字符串列表改为对象列表：

```yaml
myblog:
  security:
    public-endpoints:
      - method: GET
        path: /api/comments
```

配置类使用 `PublicEndpoint(method, path)` 表达。

- [x] **Step 3: 修改 SecurityConfig**

按 method + path 注册 `requestMatchers`，只放行明确配置的方法和路径。

- [x] **Step 4: 验证**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=SecurityConfigTest,CommentControllerTest" test
mvn test
```

Expected: `BUILD SUCCESS`。

- [x] **Step 5: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common MyBlog-springboot-v2/src/main/resources MyBlog-springboot-v2/src/test/resources MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "支持后端V2安全白名单请求方法"
```

---

## Task 6: 移除 JWT secret 默认 fallback

**目的：** 避免生产环境在未配置真实 secret 时仍然使用默认字符串启动。

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/resources/application.yml`

- Modify: `MyBlog-springboot-v2/src/main/resources/application-local.yml`

- Modify: `MyBlog-springboot-v2/src/test/resources/application-test.yml`

- Create or Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/jwt/JwtSecretStartupValidator.java`

- Create or Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/jwt/JwtSecretStartupValidatorTest.java`

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [x] **Step 1: 先补启动校验测试**

测试应覆盖：

- secret 为空时启动校验失败。

- secret 为 `change-me-change-me-change-me-change-me` 时启动校验失败。

- local/test 明确配置测试 secret 时通过。

- [x] **Step 2: 修改配置**

主配置去掉默认 fallback：

```yaml
secret: ${MYBLOG_JWT_SECRET}
```

本地和测试配置显式提供非生产测试 secret，禁止提交真实线上密钥。

- [x] **Step 3: 验证**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=JwtSecretStartupValidatorTest" test
mvn test
```

Expected: `BUILD SUCCESS`。

- [x] **Step 4: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/resources MyBlog-springboot-v2/src/test/resources MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/jwt MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/jwt docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "移除后端V2 JWT 默认密钥"
```

---

## Task 7: 抽取 Bearer Token 解析器

**目的：** 统一 `Authorization: Bearer xxx` 的解析逻辑，避免 Controller 自己处理字符串。

**Files:**

- Create: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/auth/BearerTokenResolver.java`

- Create: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/auth/BearerTokenResolverTest.java`

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/auth/JwtAuthenticationFilter.java`

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java`

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/AuthController.java`

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [x] **Step 1: 先补解析器测试**

测试应覆盖：

- 标准 `Bearer token`。

- 大小写或多空格是否接受，需要先定规则，建议只接受标准格式。

- 缺失 header、非 Bearer、空 token 返回空。

- [x] **Step 2: 替换 AuthController 中的字符串处理**

`AuthController.logout` 不再直接调用 `replaceFirst("Bearer ", "")`，改为依赖 `BearerTokenResolver`。

- [x] **Step 3: 验证**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=BearerTokenResolverTest,AuthControllerTest" test
mvn test
```

Expected: `BUILD SUCCESS`。

- [x] **Step 4: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/auth MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/auth MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/auth/JwtAuthenticationFilter.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/identity/web/AuthController.java docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "统一后端V2 Bearer Token 解析"
```

---

## Task 8: 清理 401 和 403 错误码语义

**目的：** 让认证失败和授权失败的错误码、注释、HTTP 状态一致。

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/api/ApiErrorCode.java`

- Modify: related exception handler or security tests after usage inspection

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [ ] **Step 1: 先盘点使用点**

Run:

```powershell
rg "UNAUTHORIZED|FORBIDDEN|ACCESS_DENIED|认证|授权|权限" MyBlog-springboot-v2/src/main/java MyBlog-springboot-v2/src/test/java -n
```

Expected: 明确当前 401/403 使用边界。

- [ ] **Step 2: 修改错误码注释或命名**

推荐规则：

- 401：未登录、token 缺失、token 无效、认证失败。
- 403：已登录但角色或权限不足。

若已有代码只使用 401，则先修注释和测试，不强行扩大 403 行为面。

- [ ] **Step 3: 验证**

Run:

```powershell
cd MyBlog-springboot-v2
mvn test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 4: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common MyBlog-springboot-v2/src/test/java docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "明确后端V2认证授权错误码语义"
```

---

## Task 9: 将 ContentCatalog 复杂注解 SQL 迁入 XML

**目的：** 用一个已存在的 Mapper 作为样板，把“复杂 SQL 必须 XML”的规则真正落地。

**Files:**

- Modify: `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/ContentCatalogMapper.java`

- Create: `MyBlog-springboot-v2/src/main/resources/mapper/content/ContentCatalogMapper.xml`

- Modify: `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/DatabaseContentCatalogReaderTest.java`

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [ ] **Step 1: 先补 Mapper 不含复杂注解 SQL 的守护测试**

测试目标：

- `ContentCatalogMapper` 不再存在 `@Select` 复杂 SQL。

- 分类、标签、热门标签读取行为不变。

- [ ] **Step 2: 新增 XML**

把以下方法迁入 XML：

- `listCategorySummaries()`
- `listTagSummaries()`
- `listTopTagSummaries(int limit)`

XML 中需要保留旧库字段含义的中文注释。

- [ ] **Step 3: 验证**

Run:

```powershell
cd MyBlog-springboot-v2
mvn "-Dtest=DatabaseContentCatalogReaderTest" test
mvn test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 4: 提交**

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/infrastructure/persistence/mapper/ContentCatalogMapper.java MyBlog-springboot-v2/src/main/resources/mapper/content/ContentCatalogMapper.xml MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/DatabaseContentCatalogReaderTest.java docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "迁移内容目录复杂SQL到XML"
```

---

## Task 10: 回到 MyBatis-Plus 分模块迁移

**目的：** 风险收口完成后，继续原计划中的持久层迁移，但按新的 SQL XML 规则执行。

**Files:**

- Modify: `docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md`

- Modify: `docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md`

- [ ] **Step 1: 更新原迁移计划状态**

在原 MyBatis-Plus 迁移计划中标注：

- SQL 放置规则已收口。

- `ContentCatalogMapper` 已作为 XML 样板迁移。

- 后续从 identity 低风险读模型继续推进。

- [ ] **Step 2: 验证**

Run:

```powershell
git diff --check
rg "注解或 XML|Mapper 注解或 XML" docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md
```

Expected:

- `git diff --check` 无输出。

- `rg` 不再找到宽松表达。

- [ ] **Step 3: 提交**

```powershell
git add docs/superpowers/plans/2026-05-31-backend-v2-mybatis-plus-module-migration.zh-CN.md docs/superpowers/plans/2026-06-01-backend-v2-risk-closure-plan.zh-CN.md
git commit -m "更新后端V2持久层迁移计划状态"
```

---

## 5. 收口后的继续方向

完成本计划后，再继续推进：

1. `identity` 当前用户资料读取迁移到 MyBatis-Plus。
2. `identity` 登录凭证和角色读取迁移到 MyBatis-Plus。
3. `identity` 菜单树读取迁移到 MyBatis-Plus XML。
4. `identity` 登录审计写入迁移到 MyBatis-Plus。
5. `content` 文章读取迁移到 XML + projection。
6. `comment` 前台评论读取迁移到 XML。
7. `comment` 后台评论读取迁移到 XML 动态 SQL。
8. `comment` 审核、删除、恢复、提交写入继续拆分业务规则和持久化实现。

这个顺序的依据是：先低风险读模型，再核心内容读模型，最后处理评论写入和审核状态流转。

---

## 6. 完成判定

本计划全部完成时，应满足：

- `mvn test` 通过。
- 安全白名单支持 method + path。
- 主配置不再带默认 JWT secret fallback。
- 复杂 SQL 放置规则已写入规范和迁移计划。
- `ContentCatalogMapper` 的复杂 SQL 已迁入 XML。
- Maven 坐标与 `com.tyb.myblog.v2` 包名方向一致。
- Javadoc 位置统一为注释在注解之前。
- 本计划所有任务 checkbox 已勾选。
