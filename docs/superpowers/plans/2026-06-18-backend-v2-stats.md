# Backend V2 访问统计纵向切片 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 分五个小批次完成 V2 公开访问打点、每日 PV/UV 聚合、90 天明细清理和后台统计总览，并以此结束 M3 后端模块重建。

**Architecture:** 继续使用 `web -> application -> domain <- infrastructure`。公开请求只追加 `t_page_view`，调度任务按 JST 日期幂等重算 `t_page_view_daily`；stats 仅通过 content application 校验文章状态和批量补齐标题，所有统计 SQL 位于 Mapper XML。

**Tech Stack:** Java 17、Spring Boot 3.5.14、MyBatis-Plus 3.5.12、Mapper XML、MapStruct 1.6.3、Lombok、Caffeine、JCA HmacSHA256、H2、JUnit 5、AssertJ、Mockito、MockMvc、springdoc/Knife4j。

---

## 0. 执行约束

- 设计依据：`docs/superpowers/specs/2026-06-18-backend-v2-stats-design.md`。
- 工作目录：`E:\My-Blog\.worktrees\backend-v2-refactor`。
- Maven 命令从仓库根目录执行，显式使用 `-f MyBlog-springboot-v2/pom.xml`。
- 不修改冻结的 `MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql`。
- 不使用 `@Select`、`@Insert`、`@Update`、`@Delete` 注解 SQL。
- 不引入 Redis、消息队列、时序数据库、第三方统计 SDK 或分布式锁。
- DTO、Command、Result、查询条件优先使用 `record`；Entity 使用 Lombok `@Getter/@Setter`。
- Spring 依赖注入类使用 Lombok `@RequiredArgsConstructor`。
- 所有业务时间来自注入的 `Clock`，禁止直接调用系统时间。
- 复杂聚合、批量查询、清理 SQL 必须进入 Mapper XML，并保留中文口径注释。
- 每个 Task 执行 RED -> GREEN -> 定向回归 -> 静态检查 -> 独立中文提交。
- 不使用子代理；按当前会话顺序执行五个批次。
- Docker 不作为通过前提，只允许既有 Testcontainers MySQL 条件测试在 Docker 不可用时 skipped。

## 1. 提交拆分

1. `建立访问统计领域模型与明细持久化`
2. `实现公开页面访问打点与隐私标识`
3. `实现访问日聚合与明细清理`
4. `实现后台访问统计总览`
5. `完成访问统计契约与M3收尾`

## 2. 文件结构

以下 Java 路径均相对 `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`，测试路径均相对 `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/`；文中另行标出的 `src/main/resources` 均位于 `MyBlog-springboot-v2/` 下。

### 2.1 Task 1

```text
stats/domain/
├── PageViewEvent.java
├── PageViewRepository.java
└── StatsLanguage.java
stats/infrastructure/persistence/
├── entity/PageViewEntity.java
├── mapper/PageViewMapper.java
├── mapping/PageViewPersistenceMapping.java
└── repository/MyBatisPageViewRepository.java
src/main/resources/mapper/stats/PageViewMapper.xml

测试：
stats/domain/PageViewEventTest.java
stats/infrastructure/persistence/DatabasePageViewRepositoryTest.java
```

### 2.2 Task 2

```text
content/application/article/
├── ArticleStatisticsGateway.java
├── ArticleStatisticsPolicySnapshot.java
└── PublicArticleStatisticsPolicyService.java
content/infrastructure/persistence/
├── projection/ArticleStatisticsPolicyRow.java
└── repository/MyBatisArticleStatisticsGateway.java

stats/application/
├── PageViewRateLimitService.java
├── PageViewRecordCommand.java
└── PageViewRecordService.java
stats/domain/VisitorHashGenerator.java
stats/infrastructure/
├── config/StatsProperties.java
├── privacy/HmacVisitorHashGenerator.java
├── privacy/StatsHashSecretStartupValidator.java
└── protection/CaffeinePageViewRateLimitService.java
stats/web/
├── PageViewRecordOpenApiRequest.java
├── PageViewRecordRequest.java
└── PublicPageViewController.java

修改 ArticleMapper.java/XML、application.yml、application-local.yml、
application-prod.yml、src/test/resources/application-test.yml。
```

### 2.3 Task 3

```text
stats/domain/
├── DailyPageView.java
└── PageViewAggregationRepository.java
stats/application/
├── PageViewDailyRebuildService.java
└── PageViewMaintenanceService.java
stats/infrastructure/persistence/
├── entity/PageViewDailyEntity.java
├── mapper/PageViewAggregationMapper.java
├── projection/PageViewAggregateRow.java
└── repository/MyBatisPageViewAggregationRepository.java
stats/infrastructure/scheduling/
├── StatsSchedulingConfiguration.java
└── PageViewMaintenanceScheduler.java
src/main/resources/mapper/stats/PageViewAggregationMapper.xml
```

### 2.4 Task 4

```text
content/application/article/
├── ArticleStatisticsSummary.java
└── ArticleStatisticsSummaryService.java
content/infrastructure/persistence/projection/ArticleStatisticsSummaryRow.java

stats/domain/
├── DailyTrafficPoint.java
├── LanguageTraffic.java
├── StatsDashboardRepository.java
└── TopArticleTraffic.java
stats/application/
├── StatsAuthorization.java
├── StatsDashboardQuery.java
├── StatsDashboardResult.java
└── StatsDashboardService.java
stats/infrastructure/persistence/
├── projection/DailyTrafficRow.java
├── projection/LanguageTrafficRow.java
├── projection/TopArticleTrafficRow.java
└── repository/MyBatisStatsDashboardRepository.java
stats/web/
├── AdminStatsController.java
├── StatsDashboardOpenApiResponse.java
└── StatsDashboardVO.java

修改 ArticleStatisticsGateway、ArticleMapper.java/XML、
PageViewAggregationMapper.java/XML、SecurityConfig.java。
```

### 2.5 Task 5

```text
stats/integration/StatsIntegrationTest.java
stats/web/StatsOpenApiTest.java
docs/project-handbook/api-contract/stats.md

修改 api-contract/README.md、roadmap.md、status.md、testing-policy.md、
设计文档和本实施计划。
```

## 3. Task 1：访问事件与明细持久化

- [ ] **Step 1：写领域失败测试**

Create `PageViewEventTest`：

```java
@Test
void acceptsArticleAndGeneralPageEvents() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 18, 12, 0);
    PageViewEvent article = PageViewEvent.create(
            100L, StatsLanguage.ZH, "a".repeat(64),
            "https://example.com", now);
    PageViewEvent general = PageViewEvent.create(
            null, StatsLanguage.EN, "b".repeat(64), null, now);

    assertThat(article.articleId()).isEqualTo(100L);
    assertThat(general.articleId()).isNull();
}

@Test
void rejectsInvalidStoredValues() {
    assertThatThrownBy(() -> PageViewEvent.create(
            0L, StatsLanguage.ZH, "a".repeat(64), null,
            LocalDateTime.of(2026, 6, 18, 12, 0)))
            .isInstanceOf(IllegalArgumentException.class);
}
```

Run：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PageViewEventTest" test
```

Expected：编译失败，缺少 stats domain 类型。

- [ ] **Step 2：实现领域模型和端口**

`StatsLanguage` 固定 `ZH("zh") / JA("ja") / EN("en")`，`fromCode` 对其他值抛 `IllegalArgumentException`，由 application 映射为 `VALIDATION_ERROR`。`PageViewEvent.create` 校验：articleId 为空或正数、visitorHash 匹配 `[0-9a-f]{64}`、referrer 不超过 512 字符、createdAt 非空。

```java
public interface PageViewRepository {
    long append(PageViewEvent event);
}
```

- [ ] **Step 3：写明细 Repository 失败测试**

Create `DatabasePageViewRepositoryTest`，使用 `@SpringBootTest`、test profile 和 JdbcTemplate：

```java
@BeforeEach
void resetState() {
    jdbcTemplate.update("DELETE FROM t_page_view_daily");
    jdbcTemplate.update("DELETE FROM t_page_view");
}

@Test
void appendsArticleAndGeneralViewsWithAutoIncrementIds() {
    long first = repository.append(event(100L, "a".repeat(64)));
    long second = repository.append(event(null, "b".repeat(64)));

    assertThat(first).isPositive();
    assertThat(second).isGreaterThan(first);
    assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM t_page_view", Integer.class))
            .isEqualTo(2);
}
```

Run：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=DatabasePageViewRepositoryTest" test
```

Expected：编译失败，缺少持久化实现。

- [ ] **Step 4：实现 Entity、MapStruct、Mapper XML 和 Repository**

`PageViewEntity` 不继承 BaseEntity/AuditOnlyBase：

```java
@Getter
@Setter
@TableName("t_page_view")
public class PageViewEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private String lang;
    private String visitorHash;
    private String referrer;
    private LocalDateTime createdAt;
}
```

`PageViewPersistenceMapping` 使用 `@Mapper(componentModel = "spring")`，忽略 id，并把 `language.code()` 映射到 lang。`PageViewMapper` 继承 `BaseMapper<PageViewEntity>`，不声明注解 SQL。`MyBatisPageViewRepository.append` 要求 insert 影响 1 行且回填 id 为正数，否则抛 `IllegalStateException("访问明细写入失败")`。

- [ ] **Step 5：验证并提交 Task 1**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PageViewEventTest,DatabasePageViewRepositoryTest,ArchitectureRulesTest" test
rg -n "@(Select|Insert|Update|Delete)\(" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats
rg -n "extends (BaseEntity|AuditOnlyBase)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats
git diff --check
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats `
  MyBlog-springboot-v2/src/main/resources/mapper/stats `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/stats
git diff --cached --check
git commit -m "建立访问统计领域模型与明细持久化"
```

Expected：测试通过；两个 rg 无结果；提交只包含 Task 1。

## 4. Task 2：公开页面访问打点与隐私标识

- [ ] **Step 1：写 content 公开统计策略失败测试**

Create `PublicArticleStatisticsPolicyServiceTest`：

```java
@ParameterizedTest
@EnumSource(value = ArticleStatus.class,
        names = {"PUBLISHED", "PASSWORD"})
void allowsPubliclyReachableArticles(ArticleStatus status) {
    when(gateway.findPolicy(100L)).thenReturn(Optional.of(
            new ArticleStatisticsPolicySnapshot(100L, status)));
    assertThat(service.requirePublicTrackable(100L)).isEqualTo(100L);
}

@ParameterizedTest
@EnumSource(value = ArticleStatus.class,
        names = {"DRAFT", "PRIVATE", "SCHEDULED"})
void hidesNonPublicArticles(ArticleStatus status) {
    when(gateway.findPolicy(100L)).thenReturn(Optional.of(
            new ArticleStatisticsPolicySnapshot(100L, status)));
    assertThatThrownBy(() -> service.requirePublicTrackable(100L))
            .isInstanceOf(ApiException.class)
            .extracting(error -> ((ApiException) error).code())
            .isEqualTo(ApiErrorCode.NOT_FOUND);
}
```

同时覆盖 gateway empty 与非正 articleId，均返回 NOT_FOUND。

- [ ] **Step 2：实现 content application 端口**

```java
public interface ArticleStatisticsGateway {
    Optional<ArticleStatisticsPolicySnapshot> findPolicy(long articleId);
}
```

`PublicArticleStatisticsPolicyService` 只允许 PUBLISHED/PASSWORD。ArticleMapper XML 增加：

```xml
<!-- stats 只读取公开状态，避免单次打点加载正文与标签。 -->
<select id="selectStatisticsPolicy"
        resultType="com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleStatisticsPolicyRow">
    SELECT id, status
    FROM t_article
    WHERE id = #{id}
      AND deleted = 0
</select>
```

由 `MyBatisArticleStatisticsGateway` 映射到 application snapshot；stats 不得直接依赖 ArticleMapper 或 content domain。

- [ ] **Step 3：写 HMAC、密钥和限流失败测试**

`HmacVisitorHashGeneratorTest`：

```java
@Test
void rotatesHashByDateWithoutExposingInputs() {
    VisitorHashGenerator generator = generator(
            "stats-secret-stats-secret-1234567890");
    String first = generator.hash(
            LocalDate.of(2026, 6, 18), "203.0.113.1", "JUnit");
    String same = generator.hash(
            LocalDate.of(2026, 6, 18), "203.0.113.1", "JUnit");
    String next = generator.hash(
            LocalDate.of(2026, 6, 19), "203.0.113.1", "JUnit");

    assertThat(first).matches("[0-9a-f]{64}").isEqualTo(same);
    assertThat(next).isNotEqualTo(first);
    assertThat(first).doesNotContain("203.0.113.1", "JUnit");
}
```

`StatsHashSecretStartupValidatorTest` 覆盖空密钥、少于 32 UTF-8 字节失败和 test 密钥通过。`CaffeinePageViewRateLimitServiceTest` 覆盖同 IP 前 120 次通过、第 121 次抛 RATE_LIMITED、不同 IP 隔离、空 IP 归为 unknown。

- [ ] **Step 4：实现配置、HMAC 与 Caffeine 适配**

```yaml
myblog:
  stats:
    hash-secret: ${MYBLOG_STATS_HASH_SECRET}
    page-view-max-requests-per-minute: 120
    rate-limit-maximum-size: 20000
    scheduling:
      enabled: true
      aggregate-cron: "0 */5 * * * *"
      maintenance-cron: "0 30 3 * * *"
```

test profile 使用至少 32 字节固定密钥并关闭 scheduling；local/prod 显式引用环境变量。`RuntimeProfileConfigurationTest` 断言 local/prod 没有默认 stats 密钥。

HMAC 核心实现：

```java
String payload = date + "\n" + normalize(clientIp)
        + "\n" + normalize(userAgent);
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
return HexFormat.of().formatHex(
        mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
```

每次调用创建新 Mac，避免并发共享非线程安全实例。Caffeine 使用 1 分钟 expireAfterWrite 与 `asMap().compute` 原子计数。

- [ ] **Step 5：写应用服务与 Controller 失败测试**

`PageViewRecordServiceTest` 覆盖：非文章不调用 content policy；文章先校验；Clock 决定 JST 日期和 createdAt；Referer trim 后截断 512；缺失 UA 按空串；限流失败不持久化。

`PublicPageViewControllerTest` 核心请求：

```java
mockMvc.perform(post("/api/public/stats/page-views")
        .header("User-Agent", "JUnit")
        .header("Referer", "https://example.com/posts")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
                {"articleId":"100","lang":"zh"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("00000"));
```

另测非法 lang 返回 400/90001，请求类型不声明 IP/UA/referrer 字段，非文章请求可省略 articleId；OpenAPI 泄漏检查放在 Task 5。

- [ ] **Step 6：实现公开打点接口**

```java
public record PageViewRecordCommand(
        Long articleId,
        String lang,
        String clientIp,
        String userAgent,
        String referrer) {
}
```

`PageViewRecordService.record` 顺序：限流 -> 解析语言 -> 可选文章策略 -> JST 日期/时间 -> HMAC -> Referer 规范化 -> append。Controller 只接收 articleId/lang，通过 ClientIpResolver 与请求头补齐内部 Command。

公开路径为 `POST /api/public/stats/page-views`。把精确路径加入各 profile 的 public endpoints；OpenAPI 使用独立 `PageViewRecordOpenApiRequest`，不暴露 Command。

- [ ] **Step 7：验证并提交 Task 2**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=PublicArticleStatisticsPolicyServiceTest,HmacVisitorHashGeneratorTest,StatsHashSecretStartupValidatorTest,CaffeinePageViewRateLimitServiceTest,PageViewRecordServiceTest,PublicPageViewControllerTest,RuntimeProfileConfigurationTest,SecurityConfigTest" test
rg -n "@(Select|Insert|Update|Delete)\(" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content
rg -n "(clientIp|userAgent)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats/infrastructure/persistence
git diff --check
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats `
  MyBlog-springboot-v2/src/main/resources/mapper/content `
  MyBlog-springboot-v2/src/main/resources/application*.yml `
  MyBlog-springboot-v2/src/test/resources/application-test.yml `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/stats `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common
git diff --cached --check
git commit -m "实现公开页面访问打点与隐私标识"
```

Expected：测试通过；无注解 SQL；stats persistence 不保存 IP/UA。

## 5. Task 3：访问日聚合与明细清理

- [ ] **Step 1：写聚合 Repository 失败测试**

Create `DatabasePageViewAggregationRepositoryTest`，同一天准备：article=100/lang=zh/hash=A 两次，hash=B 一次；article=NULL/lang=en/hash=C 一次。断言：

```java
assertThat(repository.summarize(start, end, date))
        .containsExactlyInAnyOrder(
                new DailyPageView(
                        100L, StatsLanguage.ZH, date, 3, 2),
                new DailyPageView(
                        0L, StatsLanguage.EN, date, 1, 1));
```

同时覆盖 replaceDay 重复执行不翻倍、删除目标日期不影响其他日期、`deleteRawBefore` 只删严格早于 cutoff 的明细、并发 append 不覆盖。

Run：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=DatabasePageViewAggregationRepositoryTest" test
```

Expected：编译失败。

- [ ] **Step 2：实现聚合模型、端口和 XML**

`DailyPageView` 校验 articleId >= 0、日期/语言非空、pv/uv 非负且 uv <= pv。

```java
public interface PageViewAggregationRepository {
    List<DailyPageView> summarize(
            LocalDateTime start,
            LocalDateTime end,
            LocalDate statDate);
    void deleteDay(LocalDate statDate);
    void insertAll(List<DailyPageView> rows);
    int deleteRawBefore(LocalDateTime cutoff);
}
```

`PageViewAggregationMapper.xml` 核心 SQL：

```xml
<!-- NULL article_id 统一落到 0；UV 只在同文章、语言、JST 日期内去重。 -->
<select id="summarize"
        resultType="com.tyb.myblog.v2.stats.infrastructure.persistence.projection.PageViewAggregateRow">
    SELECT COALESCE(article_id, 0) AS article_id,
           lang,
           COUNT(*) AS pv,
           COUNT(DISTINCT visitor_hash) AS uv
    FROM t_page_view
    WHERE created_at &gt;= #{start}
      AND created_at &lt; #{end}
    GROUP BY COALESCE(article_id, 0), lang
</select>

<delete id="deleteDay">
    DELETE FROM t_page_view_daily WHERE stat_date = #{statDate}
</delete>

<insert id="insertAll">
    INSERT INTO t_page_view_daily
        (article_id, lang, stat_date, pv, uv, created_at)
    VALUES
    <foreach collection="rows" item="row" separator=",">
        (#{row.articleId}, #{row.lang}, #{row.statDate},
         #{row.pv}, #{row.uv}, CURRENT_TIMESTAMP)
    </foreach>
</insert>

<delete id="deleteRawBefore">
    DELETE FROM t_page_view WHERE created_at &lt; #{cutoff}
</delete>
```

空聚合结果只执行 deleteDay，不执行空批量 insert。`PageViewDailyEntity` 不继承审计基类。

- [ ] **Step 3：写事务与维护服务失败测试**

`PageViewDailyRebuildServiceTest` 断言使用 Clock 的 ZoneId 计算 `[dayStart,nextDayStart)`，调用顺序为 summarize -> deleteDay -> insertAll。集成测试让 insertAll 抛错并验证旧聚合因事务回滚仍存在。

`PageViewMaintenanceServiceTest` 使用固定 Clock：

```java
service.rebuildCurrentWindow();
verify(rebuildService).rebuild(LocalDate.of(2026, 6, 17));
verify(rebuildService).rebuild(LocalDate.of(2026, 6, 18));

service.reconcileAndCleanup();
verify(rebuildService, times(90)).rebuild(any(LocalDate.class));
verify(repository).deleteRawBefore(
        LocalDateTime.of(2026, 3, 21, 0, 0));
```

最近 90 天定义为 today.minusDays(89) 到 today，包含首尾；参数化测试覆盖跨月、跨年。

- [ ] **Step 4：实现事务服务与维护编排**

`PageViewDailyRebuildService.rebuild(LocalDate)` 标注 `@Transactional`。维护服务核心逻辑：

```java
public void rebuildCurrentWindow() {
    LocalDate today = LocalDate.now(clock);
    rebuildService.rebuild(today.minusDays(1));
    rebuildService.rebuild(today);
}

public void reconcileAndCleanup() {
    LocalDate today = LocalDate.now(clock);
    IntStream.range(0, 90)
            .mapToObj(today::minusDays)
            .sorted()
            .forEach(rebuildService::rebuild);
    repository.deleteRawBefore(
            today.minusDays(89).atStartOfDay());
}
```

这里的 `LocalDate.now(clock)` 是允许的注入 Clock 用法；禁止无参数 now。

- [ ] **Step 5：写调度测试并实现本地互斥**

`PageViewMaintenanceSchedulerTest` 覆盖：ApplicationReadyEvent 调用 reconcile、5 分钟任务调用 current window、凌晨任务调用 reconcile、任务重入时第二次跳过。

```java
@Scheduled(
        cron = "${myblog.stats.scheduling.aggregate-cron:0 */5 * * * *}",
        zone = "Asia/Tokyo")
public void aggregateRecentDays() {
    runExclusively(
            "访问统计近期聚合",
            maintenanceService::rebuildCurrentWindow);
}

@Scheduled(
        cron = "${myblog.stats.scheduling.maintenance-cron:0 30 3 * * *}",
        zone = "Asia/Tokyo")
public void reconcileAndCleanup() {
    runExclusively(
            "访问统计校准清理",
            maintenanceService::reconcileAndCleanup);
}

@EventListener(ApplicationReadyEvent.class)
public void catchUpAfterStartup() {
    runExclusively(
            "访问统计启动补算",
            maintenanceService::reconcileAndCleanup);
}

private void runExclusively(String taskName, Runnable action) {
    if (!lock.tryLock()) {
        log.info("{}因已有任务运行而跳过", taskName);
        return;
    }
    try {
        action.run();
    } catch (RuntimeException exception) {
        log.error("{}失败，等待下一次调度重试", taskName, exception);
    } finally {
        lock.unlock();
    }
}
```

三个入口共用一个 `ReentrantLock.tryLock()`。异常日志只写任务名和堆栈，不写访客输入。`PageViewMaintenanceScheduler` 本身必须标注 `@ConditionalOnProperty(prefix="myblog.stats.scheduling", name="enabled", havingValue="true", matchIfMissing=true)`，防止其他模块启用全局 scheduling 后误注册 stats 任务；test profile 关闭。

- [ ] **Step 6：验证并提交 Task 3**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=DatabasePageViewAggregationRepositoryTest,PageViewDailyRebuildServiceTest,PageViewMaintenanceServiceTest,PageViewMaintenanceSchedulerTest,ArchitectureRulesTest" test
rg -n "@(Select|Insert|Update|Delete)\(" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats
rg -n "LocalDate(Time)?\.now\(\)|Instant\.now\(\)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats
git diff --check
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats `
  MyBlog-springboot-v2/src/main/resources/mapper/stats `
  MyBlog-springboot-v2/src/main/resources/application.yml `
  MyBlog-springboot-v2/src/test/resources/application-test.yml `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/stats
git diff --cached --check
git commit -m "实现访问日聚合与明细清理"
```

Expected：测试通过；无注解 SQL；无无参系统时间调用。

## 6. Task 4：后台访问统计总览

- [ ] **Step 1：写后台查询应用失败测试**

`StatsDashboardServiceTest` 固定默认 30 个 JST 自然日：

```java
@Test
void defaultsToThirtyDaysAndBuildsAccurateSummary() {
    when(repository.findTrend(
            LocalDate.of(2026, 5, 20),
            LocalDate.of(2026, 6, 18)))
            .thenReturn(List.of(new DailyTrafficPoint(
                    LocalDate.of(2026, 6, 18), 48, 31)));
    when(repository.findTopArticles(any(), any(), eq(10)))
            .thenReturn(List.of(new TopArticleTraffic(
                    100L, 320, 180)));
    when(summaryService.findTitles(Set.of(100L)))
            .thenReturn(Map.of(100L, "文章标题"));

    StatsDashboardResult result = service.dashboard(
            principal("DEMO"),
            new StatsDashboardQuery(null, null));

    assertThat(result.periodPv()).isEqualTo(48);
    assertThat(result.todayPv()).isEqualTo(48);
    assertThat(result.todayUv()).isEqualTo(31);
    assertThat(result.averageDailyUv())
            .isEqualByComparingTo("1.0");
    assertThat(result.trend()).hasSize(30);
}
```

31 / 30 按 scale 1、HALF_UP 为 1.0，不能只对有数据日期求平均。另测 from/to 同时出现、顺序、366 天上限、趋势补零、today 不在自定义区间时仍单查今天、标题缺失保留 TOP 行、ADMIN/DEMO 权限、区间 PV=0 时 ratio=0。

- [ ] **Step 2：实现查询模型、端口与应用服务**

```java
public interface StatsDashboardRepository {
    List<DailyTrafficPoint> findTrend(
            LocalDate from, LocalDate to);
    List<TopArticleTraffic> findTopArticles(
            LocalDate from, LocalDate to, int limit);
    List<LanguageTraffic> findLanguages(
            LocalDate from, LocalDate to);
}
```

`StatsAuthorization.requireReadable` 只允许 ADMIN/DEMO。默认 from=today.minusDays(29)、to=today；区间包含首尾最多 366 天。periodPv 从补零趋势求和；today 单独查询；averageDailyUv 按完整区间天数计算；语言 ratio scale 4、HALF_UP。

- [ ] **Step 3：写 Mapper 失败测试并实现查询 XML**

`DatabaseStatsDashboardRepositoryTest` 插入跨日期、文章、语言日聚合，验证闭区间和稳定排序。XML：

```xml
<!-- 缺失日期由 application 补零。 -->
<select id="selectTrend"
        resultType="com.tyb.myblog.v2.stats.infrastructure.persistence.projection.DailyTrafficRow">
    SELECT stat_date, SUM(pv) AS pv, SUM(uv) AS uv
    FROM t_page_view_daily
    WHERE stat_date BETWEEN #{from} AND #{to}
    GROUP BY stat_date
    ORDER BY stat_date ASC
</select>

<!-- TOP 排除 article_id=0，同 PV 时按 article_id 升序。 -->
<select id="selectTopArticles"
        resultType="com.tyb.myblog.v2.stats.infrastructure.persistence.projection.TopArticleTrafficRow">
    SELECT article_id, SUM(pv) AS pv,
           SUM(uv) AS daily_uv_sum
    FROM t_page_view_daily
    WHERE stat_date BETWEEN #{from} AND #{to}
      AND article_id &gt; 0
    GROUP BY article_id
    ORDER BY pv DESC, article_id ASC
    LIMIT #{limit}
</select>

<select id="selectLanguages"
        resultType="com.tyb.myblog.v2.stats.infrastructure.persistence.projection.LanguageTrafficRow">
    SELECT lang, SUM(pv) AS pv
    FROM t_page_view_daily
    WHERE stat_date BETWEEN #{from} AND #{to}
    GROUP BY lang
    ORDER BY lang ASC
</select>
```

累计 daily UV 的 Java/VO 字段必须命名 `dailyUvSum`，不能写成跨日 uv。

- [ ] **Step 4：实现文章标题批量补齐**

扩展 gateway：

```java
List<ArticleStatisticsSummary> findSummaries(
        Set<Long> articleIds);
```

空集合直接返回 Map.of；非空只调用 gateway 一次。ArticleMapper XML：

```xml
<!-- 历史统计允许补齐已删除或私密文章标题，不按 deleted/status 过滤。 -->
<select id="selectStatisticsSummaries"
        resultType="com.tyb.myblog.v2.content.infrastructure.persistence.projection.ArticleStatisticsSummaryRow">
    SELECT id, title_zh
    FROM t_article
    WHERE id IN
    <foreach collection="articleIds" item="id"
             open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
```

不存在的 ID 不伪造标题，stats 返回 null；禁止 N+1。

- [ ] **Step 5：写并实现 Controller 与权限**

`AdminStatsControllerTest` 覆盖默认/显式日期和字段名。`SecurityConfigTest` 覆盖 DEMO GET 成功、其他方法 403、匿名/GUEST 拒绝。

```text
GET /api/admin/stats/dashboard?from=2026-05-20&to=2026-06-18
```

Controller 使用 `@CurrentUser AuthenticatedPrincipal`，返回字段：periodPv、todayPv、todayUv、averageDailyUv、trend、topArticles、languageDistribution。TOP 项字段为 articleId/title/pv/dailyUvSum。SecurityConfig 在通用 admin 规则前加入精确 GET 的 ADMIN/DEMO 权限。

- [ ] **Step 6：验证并提交 Task 4**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=StatsDashboardServiceTest,DatabaseStatsDashboardRepositoryTest,ArticleStatisticsSummaryServiceTest,AdminStatsControllerTest,SecurityConfigTest,ArchitectureRulesTest" test
rg -n "@(Select|Insert|Update|Delete)\(" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content
rg -n "content\.(domain|web|infrastructure)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats
git diff --check
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats `
  MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java `
  MyBlog-springboot-v2/src/main/resources/mapper/content `
  MyBlog-springboot-v2/src/main/resources/mapper/stats `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/stats `
  MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security
git diff --cached --check
git commit -m "实现后台访问统计总览"
```

Expected：测试通过；stats 只依赖 content application；所有统计 SQL 在 XML。

## 7. Task 5：契约、集成和 M3 收尾

- [ ] **Step 1：写真 HTTP 集成失败测试**

Create `StatsIntegrationTest`，使用真实 Spring context、H2、JWT 和 MockMvc：

1. 插入 PUBLISHED、PASSWORD、PRIVATE、DRAFT、SCHEDULED 和已删除文章。
2. 匿名打点 PUBLISHED、PASSWORD 和非文章页面成功。
3. 其他状态、已删除和不存在文章返回 404，且不写明细。
4. 同 IP/UA/日期对同文章重复打点形成 PV=2、UV=1。
5. 手动调用 `PageViewDailyRebuildService.rebuild(date)` 后验证 daily 表。
6. DEMO 查询 dashboard 成功，匿名查询 401。
7. dashboard 趋势、TOP 10、语言分布和字段名符合契约。
8. 数据库不包含原始 IP 和 User-Agent 文本。

核心断言：

```java
assertThat(jdbcTemplate.queryForObject(
        "SELECT pv FROM t_page_view_daily "
                + "WHERE article_id=100 AND lang='zh' "
                + "AND stat_date=?",
        Integer.class, date)).isEqualTo(2);
assertThat(jdbcTemplate.queryForObject(
        "SELECT uv FROM t_page_view_daily "
                + "WHERE article_id=100 AND lang='zh' "
                + "AND stat_date=?",
        Integer.class, date)).isEqualTo(1);
assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM t_page_view "
                + "WHERE visitor_hash LIKE '%203.0.113.1%'",
        Integer.class)).isZero();
```

Run：

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=StatsIntegrationTest" test
```

Expected：若前四批存在遗漏则失败；逐项修正，不扩展设计范围。

- [ ] **Step 2：写 OpenAPI 守护测试**

Create `StatsOpenApiTest`：

```java
assertMethods(root,
        "/paths/~1api~1public~1stats~1page-views", "post");
assertMethods(root,
        "/paths/~1api~1admin~1stats~1dashboard", "get");

assertThat(root.toString()).doesNotContain(
        "PageViewEntity",
        "PageViewMapper",
        "PageViewRepository",
        "PageViewRecordCommand",
        "visitorHash",
        "clientIp",
        "userAgent",
        "hashSecret",
        "uvTotal");
```

断言请求 schema 只有 articleId/lang，dashboard TOP 项使用 dailyUvSum。

- [ ] **Step 3：补接口契约文档**

Create `docs/project-handbook/api-contract/stats.md`，写清：

- 打点接口、请求字段、服务端读取的请求头和统一响应。
- PUBLISHED/PASSWORD 可打点，其余状态返回 404。
- PV、日 UV、非文章 articleId=0、每日 hash 口径。
- dashboard 默认 30 天、最大 366 天、权限矩阵和完整字段。
- dailyUvSum/averageDailyUv 均不是跨日独立访客数。
- 5 分钟延迟、启动补算、90 天明细清理和限流错误码。

更新 README：

```markdown
| `stats.md` | 公开访问打点、日 PV/UV 聚合和后台数据总览 | 已落地 |
```

- [ ] **Step 4：同步状态和测试文档**

- roadmap.md：勾选 stats，M3 全部完成，下一项为 M4 前台/后台骨架。
- status.md：记录每日 hash、准实时延迟、清理规则和 M3 完成状态。
- testing-policy.md：更新实际测试数量和 stats 覆盖。
- 设计文档：回填五批提交和最终状态。
- 本计划：只勾选实际完成步骤，不预先勾选。

- [ ] **Step 5：运行最终验证**

```powershell
mvn -f MyBlog-springboot-v2/pom.xml "-Dtest=StatsIntegrationTest,StatsOpenApiTest,SecurityConfigTest,ArchitectureRulesTest" test
mvn -f MyBlog-springboot-v2/pom.xml clean test
rg -n "@(Select|Insert|Update|Delete)\(" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats
rg -n "LocalDate(Time)?\.now\(\)|Instant\.now\(\)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats
rg -n "content\.(domain|web|infrastructure)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats
rg -n "clientIp|userAgent" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats/infrastructure/persistence
git diff --check
git diff --exit-code HEAD -- MyBlog-springboot-v2/src/main/resources/db/migration/V1__init.sql
```

Expected：全量测试通过，仅 Docker/Testcontainers 条件测试可 skipped；静态检查无违规；V1 DDL 无差异。

- [ ] **Step 6：提交 Task 5**

```powershell
git add MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/stats `
  docs/project-handbook/api-contract `
  docs/project-handbook/roadmap.md `
  docs/project-handbook/status.md `
  docs/project-handbook/rules/testing-policy.md `
  docs/superpowers/specs/2026-06-18-backend-v2-stats-design.md `
  docs/superpowers/plans/2026-06-18-backend-v2-stats.md
git diff --cached --check
git commit -m "完成访问统计契约与M3收尾"
```

## 8. 最终验收清单

- [ ] stats 四层结构完整，只依赖 content application。
- [ ] 明细只保存 articleId、lang、每日 visitorHash、referrer、createdAt。
- [ ] 原始 IP、User-Agent、hash secret 不入库、不进 API、不写日志。
- [ ] PUBLISHED/PASSWORD 可打点，其他状态和已删除文章返回 404。
- [ ] 每次有效访问增加 PV；同访客、文章、语言、JST 日期只增加一次 UV。
- [ ] 非文章页面明细使用 NULL、聚合使用 articleId=0。
- [ ] 单日聚合可重复执行且不翻倍。
- [ ] 启动补算、5 分钟校准和 90 天清理使用同实例互斥。
- [ ] dashboard 支持默认 30 天、最大 366 天、连续趋势、TOP 10 和语言分布。
- [ ] API 不提供虚假跨日 UV，累计字段命名 dailyUvSum。
- [ ] ADMIN/DEMO 可读 dashboard，GUEST/匿名不可读。
- [ ] 所有复杂 SQL 位于 XML，生产代码保留必要中文注释。
- [ ] `V1__init.sql` 未修改。
- [ ] 全量测试和静态检查通过，M3 文档状态完成收尾。
