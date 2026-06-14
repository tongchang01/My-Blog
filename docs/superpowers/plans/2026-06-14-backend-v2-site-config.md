# Backend V2 站点配置实施计划

> **执行要求：** 使用 `superpowers:executing-plans` 在当前会话内逐批执行。步骤使用复选框跟踪；每个 Task 单独形成中文提交，不使用子代理。

**目标：** 建立 `system` 模块首个纵向切片，开放公开单语言站点配置查询、后台完整读取和 ADMIN 全量更新。

**架构：** 新建 `system` 四层模块。Domain 负责固定单行模型、语言回退和字段校验；Infrastructure 使用 MyBatis Mapper + XML；Application 编排公开查询、后台查询和事务更新；Web 使用独立公开/后台 DTO，并通过 presence 请求模型实现真正的 PUT 全量覆盖。

**技术栈：** Java 17、Spring Boot 3.5、Spring Security、MyBatis-Plus、Mapper XML、H2、Flyway、JUnit 5、MockMvc、springdoc、Lombok。

---

## 0. 文件结构

本计划新增或修改以下文件。

### Domain

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/siteconfig/SiteLanguage.java`
  - 支持 `zh`、`ja`、`en`，负责解析公开接口语言参数。
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/siteconfig/SiteConfig.java`
  - 固定 `id=1` 的领域模型，承载 13 个业务字段、更新时间和更新人。
  - 负责 trim、空白清空、长度、URL、Spotify ID 校验。
  - 负责按语言逐字段 fallback。
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/siteconfig/SiteConfigRepository.java`
  - 定义普通查询、行锁查询和完整更新端口。

### Application

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/PublicSiteConfigResult.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/PublicSiteConfigQueryService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/AdminSiteConfigResult.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/AdminSiteConfigQueryService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/UpdateSiteConfigCommand.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/siteconfig/SiteConfigUpdateService.java`

### Infrastructure

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence/entity/SiteConfigEntity.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence/mapper/SiteConfigMapper.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence/repository/MyBatisSiteConfigRepository.java`
- `MyBlog-springboot-v2/src/main/resources/mapper/system/SiteConfigMapper.xml`

### Web

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/PublicSiteConfigController.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/AdminSiteConfigController.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/PublicSiteConfigVO.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/AdminSiteConfigVO.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/SubmittedField.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/UpdateSiteConfigRequest.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/UpdateSiteConfigOpenApiRequest.java`

### Tests

- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/domain/siteconfig/SiteConfigTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/infrastructure/persistence/DatabaseSiteConfigRepositoryTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/application/siteconfig/PublicSiteConfigQueryServiceTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/application/siteconfig/AdminSiteConfigQueryServiceTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/application/siteconfig/SiteConfigUpdateServiceTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/application/siteconfig/SiteConfigUpdateConcurrencyTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/web/PublicSiteConfigControllerTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/web/AdminSiteConfigControllerTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/web/SiteConfigOpenApiTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/integration/SiteConfigIntegrationTest.java`
- 修改 `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/security/SecurityConfigTest.java`

### Configuration and docs

- 修改 `MyBlog-springboot-v2/src/main/resources/application.yml`
- 修改 `MyBlog-springboot-v2/src/test/resources/application-test.yml`
- 修改 `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/security/SecurityConfig.java`
- 新建 `docs/project-handbook/api-contract/site-config.md`
- 修改 `docs/project-handbook/api-contract/README.md`
- 修改 `docs/project-handbook/status.md`
- 修改 `docs/project-handbook/roadmap.md`
- 修改 `docs/project-handbook/m3-preflight-review.md`
- 修改本计划和对应设计文档的实施证据

不修改 Flyway。`t_site_config` 和固定初始化行已经存在。

---

## Task 1：建立领域模型与持久化读取

**提交信息：** `建立站点配置领域与持久化读取`

### Step 1：写领域模型失败测试

- [x] 新建 `SiteConfigTest.java`，覆盖：

```java
@Test
void normalizesEditableFields() {
    SiteConfig config = SiteConfig.create(
            1L,
            " MyBlog ",
            " ",
            " English ",
            " 中文副标题 ",
            null,
            " ",
            "  # 关于我\n",
            "   ",
            null,
            " https://example.com/logo.png ",
            null,
            " ICP 123 ",
            " playlist_123-abc ",
            UPDATED_AT,
            1001L);

    assertThat(config.siteTitleZh()).isEqualTo("MyBlog");
    assertThat(config.siteTitleJa()).isNull();
    assertThat(config.siteTitleEn()).isEqualTo("English");
    assertThat(config.siteSubtitleZh()).isEqualTo("中文副标题");
    assertThat(config.aboutMdZh()).isEqualTo("  # 关于我\n");
    assertThat(config.aboutMdJa()).isNull();
    assertThat(config.logoUrl()).isEqualTo("https://example.com/logo.png");
    assertThat(config.icpNo()).isEqualTo("ICP 123");
    assertThat(config.spotifyPlaylistId()).isEqualTo("playlist_123-abc");
}
```

再覆盖：

- `id != 1` 拒绝。
- 中文标题为空或超过 128 拒绝。
- 日英标题超过 128 拒绝。
- 副标题超过 255 拒绝。
- Markdown 超过 50,000 拒绝，非空 Markdown 保留首尾。
- logo/favicon 拒绝相对 URL、无 host URL、`javascript:`、`data:`。
- ICP 超过 64 拒绝。
- Spotify ID 超长或包含空格、斜杠拒绝。
- `zh` 直接返回中文。
- `ja` / `en` 对 title、subtitle、aboutMd 分别 fallback 中文。

### Step 2：写数据库读取失败测试

- [x] 新建 `DatabaseSiteConfigRepositoryTest.java`。

测试前显式恢复固定配置行，避免测试之间互相污染：

```java
@BeforeEach
void resetSiteConfig() {
    jdbcTemplate.update("DELETE FROM t_site_config");
    jdbcTemplate.update("""
            INSERT INTO t_site_config (
                id, site_title_zh, site_title_ja, site_title_en,
                site_subtitle_zh, site_subtitle_ja, site_subtitle_en,
                about_md_zh, about_md_ja, about_md_en,
                logo_url, favicon_url, icp_no, spotify_playlist_id,
                updated_at, updated_by, deleted
            ) VALUES (
                1, 'MyBlog', 'マイブログ', 'My Blog',
                '中文副标题', NULL, 'English subtitle',
                '# 关于我', NULL, '# About',
                'https://example.com/logo.png',
                'https://example.com/favicon.ico',
                'ICP-123', 'playlist_123',
                '2026-01-01 00:00:00', 1001, 0
            )
            """);
}
```

覆盖：

- `findActive()` 返回全部字段和审计值。
- `findActiveForUpdate()` 在 `@Transactional` 中返回固定行。
- `deleted=1` 时两个查询均返回空。

### Step 3：运行测试确认 RED

- [x] 执行：

```powershell
mvn "-Dtest=SiteConfigTest,DatabaseSiteConfigRepositoryTest" test
```

预期：编译失败，提示 `SiteConfig`、`SiteLanguage`、`SiteConfigRepository` 尚不存在。

### Step 4：实现领域模型

- [x] 新建 `SiteLanguage.java`：

```java
public enum SiteLanguage {
    ZH("zh"),
    JA("ja"),
    EN("en");

    private final String code;

    SiteLanguage(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static SiteLanguage parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("语言不能为空");
        }
        return Arrays.stream(values())
                .filter(language -> language.code.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "语言仅支持 zh、ja、en"));
    }
}
```

- [x] 新建 `SiteConfig.java`，签名固定为：

```java
public record SiteConfig(
        long id,
        String siteTitleZh,
        String siteTitleJa,
        String siteTitleEn,
        String siteSubtitleZh,
        String siteSubtitleJa,
        String siteSubtitleEn,
        String aboutMdZh,
        String aboutMdJa,
        String aboutMdEn,
        String logoUrl,
        String faviconUrl,
        String icpNo,
        String spotifyPlaylistId,
        LocalDateTime updatedAt,
        Long updatedBy
) {
    public static final long FIXED_ID = 1L;
    private static final Pattern SPOTIFY_ID =
            Pattern.compile("^[A-Za-z0-9_-]+$");

    public static SiteConfig create(
            long id,
            String siteTitleZh,
            String siteTitleJa,
            String siteTitleEn,
            String siteSubtitleZh,
            String siteSubtitleJa,
            String siteSubtitleEn,
            String aboutMdZh,
            String aboutMdJa,
            String aboutMdEn,
            String logoUrl,
            String faviconUrl,
            String icpNo,
            String spotifyPlaylistId,
            LocalDateTime updatedAt,
            Long updatedBy) {
        if (id != FIXED_ID) {
            throw new IllegalArgumentException("站点配置 ID 必须固定为1");
        }
        return new SiteConfig(
                id,
                required(siteTitleZh, "中文站点标题", 128),
                optional(siteTitleJa, "日文站点标题", 128),
                optional(siteTitleEn, "英文站点标题", 128),
                optional(siteSubtitleZh, "中文站点副标题", 255),
                optional(siteSubtitleJa, "日文站点副标题", 255),
                optional(siteSubtitleEn, "英文站点副标题", 255),
                markdown(aboutMdZh, "中文关于我"),
                markdown(aboutMdJa, "日文关于我"),
                markdown(aboutMdEn, "英文关于我"),
                url(logoUrl, "站点 Logo URL"),
                url(faviconUrl, "站点 favicon URL"),
                optional(icpNo, "ICP备案号", 64),
                spotifyId(spotifyPlaylistId),
                updatedAt,
                updatedBy);
    }
}
```

领域模型必须提供：

```java
public String siteTitle(SiteLanguage language)
public String siteSubtitle(SiteLanguage language)
public String aboutMd(SiteLanguage language)
```

日英值为 `null` 时逐字段 fallback 中文。不要把三个字段合并成一次整体 fallback。

辅助方法要求：

- `optional`：null/blank → null，否则 trim 后校验长度。
- `required`：复用 `optional`，null 时抛中文异常。
- `markdown`：null/blank → null；否则不 trim，长度上限 50,000。
- `url`：调用 `optional(value, field, 255)`，再用 `URI.create` 校验 scheme 和 host。
- `spotifyId`：调用 `optional(value, "Spotify 播放列表 ID", 64)`，再匹配 `^[A-Za-z0-9_-]+$`。

### Step 5：实现仓储读取

- [x] 新建 `SiteConfigRepository.java`：

```java
public interface SiteConfigRepository {
    Optional<SiteConfig> findActive();
    Optional<SiteConfig> findActiveForUpdate();
    boolean update(
            SiteConfig config,
            LocalDateTime updatedAt,
            Long updatedBy);
}
```

本 Task 先实现两个查询；`update` 可返回 `UnsupportedOperationException` 吗？不允许。Mapper 和仓储应同时声明完整更新方法，但本 Task 不调用它；XML 更新在 Task 4 落地。为保持生产代码始终可运行，本 Task 的仓储 `update` 先调用 Mapper 的完整方法，Task 1 就把 XML UPDATE 写好并由 Task 4 补写入测试。

- [x] 新建 `SiteConfigEntity.java`：

```java
@Getter
@Setter
@TableName("t_site_config")
public class SiteConfigEntity extends AuditOnlyBase {
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;
    // 13 个业务字段，均写中文字段注释
}
```

- [x] 新建 `SiteConfigMapper.java`：

```java
@Mapper
public interface SiteConfigMapper extends BaseMapper<SiteConfigEntity> {
    SiteConfigEntity selectActive();
    SiteConfigEntity selectActiveForUpdate();
    int updateActive(
            @Param("config") SiteConfigEntity config,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("updatedBy") Long updatedBy);
}
```

- [x] 新建 `SiteConfigMapper.xml`。

查询固定条件：

```xml
FROM t_site_config
WHERE id = 1
  AND deleted = 0
```

行锁查询追加：

```xml
FOR UPDATE
```

完整更新必须显式写 13 个业务字段和审计字段：

```xml
UPDATE t_site_config
SET site_title_zh = #{config.siteTitleZh},
    site_title_ja = #{config.siteTitleJa},
    site_title_en = #{config.siteTitleEn},
    site_subtitle_zh = #{config.siteSubtitleZh},
    site_subtitle_ja = #{config.siteSubtitleJa},
    site_subtitle_en = #{config.siteSubtitleEn},
    about_md_zh = #{config.aboutMdZh},
    about_md_ja = #{config.aboutMdJa},
    about_md_en = #{config.aboutMdEn},
    logo_url = #{config.logoUrl},
    favicon_url = #{config.faviconUrl},
    icp_no = #{config.icpNo},
    spotify_playlist_id = #{config.spotifyPlaylistId},
    updated_at = #{updatedAt},
    updated_by = #{updatedBy}
WHERE id = 1
  AND deleted = 0
```

XML 前添加中文注释，说明固定单行、软删除过滤和行锁用途。

- [x] 新建 `MyBatisSiteConfigRepository.java`：

```java
@Repository
@RequiredArgsConstructor
public class MyBatisSiteConfigRepository implements SiteConfigRepository {
    private final SiteConfigMapper mapper;

    @Override
    public Optional<SiteConfig> findActive() {
        return Optional.ofNullable(mapper.selectActive()).map(this::toDomain);
    }

    @Override
    public Optional<SiteConfig> findActiveForUpdate() {
        return Optional.ofNullable(mapper.selectActiveForUpdate())
                .map(this::toDomain);
    }

    @Override
    public boolean update(
            SiteConfig config,
            LocalDateTime updatedAt,
            Long updatedBy) {
        return mapper.updateActive(
                toEntity(config), updatedAt, updatedBy) == 1;
    }
}
```

仓储不读取 SecurityContext，不自行获取时间；更新时间和更新人由应用事务显式传入。

### Step 6：运行定向测试确认 GREEN

- [x] 执行：

```powershell
mvn "-Dtest=SiteConfigTest,DatabaseSiteConfigRepositoryTest,ArchitectureRulesTest" test
```

预期：全部通过。`ArchitectureRulesTest` 已包含 `system`，新增类后既有四层和跨模块规则开始真实生效。

### Step 7：静态检查并提交

- [x] 执行：

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
git diff --check
```

预期：无注解 SQL，`git diff --check` 无输出。

- [x] 提交：

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system MyBlog-springboot-v2/src/main/resources/mapper/system MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system docs/superpowers/plans/2026-06-14-backend-v2-site-config.md
git commit -m "建立站点配置领域与持久化读取"
```

---

## Task 2：开放公开站点配置查询

**提交信息：** `开放公开站点配置查询`

### Step 1：写应用服务失败测试

- [x] 新建 `PublicSiteConfigQueryServiceTest.java`。

覆盖：

```java
@Test
void returnsRequestedLanguageWithFieldLevelChineseFallback() {
    when(repository.findActive()).thenReturn(Optional.of(siteConfig()));

    PublicSiteConfigResult result = service.query("ja");

    assertThat(result.siteTitle()).isEqualTo("日本語タイトル");
    assertThat(result.siteSubtitle()).isEqualTo("中文副标题");
    assertThat(result.aboutMd()).isEqualTo("# 中文关于我");
    assertThat(result.logoUrl()).isEqualTo("https://example.com/logo.png");
}
```

再覆盖：

- `zh` 读取中文。
- `en` 逐字段回退。
- lang null、空串、大写 `ZH`、未知值返回 `VALIDATION_ERROR`。
- 固定配置行缺失返回 `INTERNAL_ERROR`，日志只包含 `siteConfigId=1`。

### Step 2：写 Controller 与匿名访问失败测试

- [x] 新建 `PublicSiteConfigControllerTest.java`，使用 `@SpringBootTest + @AutoConfigureMockMvc` 或项目现有测试方式。

覆盖：

- `GET /api/public/site-config?lang=zh` 无 token 返回 200。
- lang 缺失返回 `400 + 90001`。
- lang 非法返回 `400 + 90001`。
- POST 同一路径不是公开接口。
- 响应只包含 7 个公开字段，不含 `siteTitleZh`、`updatedAt`、`deleted`。

### Step 3：运行测试确认 RED

- [x] 执行：

```powershell
mvn "-Dtest=PublicSiteConfigQueryServiceTest,PublicSiteConfigControllerTest" test
```

预期：缺少应用服务、Controller、VO，或匿名请求被 Security 返回 401。

### Step 4：实现公开查询应用层

- [x] 新建 `PublicSiteConfigResult.java`：

```java
public record PublicSiteConfigResult(
        String siteTitle,
        String siteSubtitle,
        String aboutMd,
        String logoUrl,
        String faviconUrl,
        String icpNo,
        String spotifyPlaylistId
) {
}
```

- [x] 新建 `PublicSiteConfigQueryService.java`：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicSiteConfigQueryService {
    private final SiteConfigRepository repository;

    public PublicSiteConfigResult query(String languageCode) {
        SiteLanguage language;
        try {
            language = SiteLanguage.parse(languageCode);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }

        SiteConfig config = repository.findActive()
                .orElseThrow(this::missingConfig);
        return new PublicSiteConfigResult(
                config.siteTitle(language),
                config.siteSubtitle(language),
                config.aboutMd(language),
                config.logoUrl(),
                config.faviconUrl(),
                config.icpNo(),
                config.spotifyPlaylistId());
    }

    private ApiException missingConfig() {
        log.error("站点配置固定行不存在，siteConfigId={}",
                SiteConfig.FIXED_ID);
        return new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
```

### Step 5：实现公开 Web 接口

- [x] 新建 `PublicSiteConfigVO.java`，字段与 `PublicSiteConfigResult` 完全一致，并提供 `from(result)`。

- [x] 新建 `PublicSiteConfigController.java`：

```java
@Tag(name = "公开站点配置", description = "前台公开站点信息")
@RestController
@RequestMapping("/api/public/site-config")
@RequiredArgsConstructor
public class PublicSiteConfigController {
    private final PublicSiteConfigQueryService queryService;

    @Operation(summary = "查询当前语言站点配置")
    @GetMapping
    public ApiResponse<PublicSiteConfigVO> get(
            @RequestParam String lang) {
        return ApiResponse.ok(
                PublicSiteConfigVO.from(queryService.query(lang)));
    }
}
```

### Step 6：开放精确匿名白名单

- [x] 在 `application.yml` 和 `application-test.yml` 的 `public-endpoints` 增加：

```yaml
- method: GET
  path: /api/public/site-config
```

不要开放 `/api/public/**` 通配符。

### Step 7：运行测试确认 GREEN

- [x] 执行：

```powershell
mvn "-Dtest=PublicSiteConfigQueryServiceTest,PublicSiteConfigControllerTest,SecurityConfigTest" test
```

预期：全部通过。

### Step 8：提交 Task 2

- [x] 执行：

```powershell
git diff --check
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-14-backend-v2-site-config.md
git commit -m "开放公开站点配置查询"
```

---

## Task 3：开放后台完整读取

**提交信息：** `开放后台站点配置读取`

### Step 1：写后台查询应用服务失败测试

- [x] 新建 `AdminSiteConfigQueryServiceTest.java`，覆盖：

- ADMIN 返回完整配置。
- DEMO 返回完整配置。
- null principal 返回 `INVALID_TOKEN`。
- GUEST 或未知角色返回 `FORBIDDEN`。
- 固定配置行缺失返回 `INTERNAL_ERROR`。

应用服务签名：

```java
public AdminSiteConfigResult query(AuthenticatedPrincipal principal)
```

### Step 2：写 Web 与 Security 失败测试

- [x] 新建 `AdminSiteConfigControllerTest.java`，mock 应用服务，覆盖：

- GET 返回完整 13 字段和 `updatedAt`、`updatedBy`。
- Controller 把 `AuthenticatedPrincipal` 原样传给应用服务。

- [x] 扩展 `SecurityConfigTest.java`：

```java
@Test
void permitsAdminAndDemoToReadSiteConfig() throws Exception {
    // 分别使用 ADMIN / DEMO access token 请求 GET /api/admin/site-config
    // 断言 200
}

@Test
void requiresAuthenticationForAdminSiteConfigRead() throws Exception {
    mockMvc.perform(get("/api/admin/site-config"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("10002"));
}
```

### Step 3：运行测试确认 RED

- [x] 执行：

```powershell
mvn "-Dtest=AdminSiteConfigQueryServiceTest,AdminSiteConfigControllerTest,SecurityConfigTest" test
```

预期：DEMO 被现有 `/api/admin/**` ADMIN 规则拒绝，且后台服务/Controller 尚不存在。

### Step 4：实现后台结果和查询服务

- [x] 新建 `AdminSiteConfigResult.java`，包含：

```java
public record AdminSiteConfigResult(
        String siteTitleZh,
        String siteTitleJa,
        String siteTitleEn,
        String siteSubtitleZh,
        String siteSubtitleJa,
        String siteSubtitleEn,
        String aboutMdZh,
        String aboutMdJa,
        String aboutMdEn,
        String logoUrl,
        String faviconUrl,
        String icpNo,
        String spotifyPlaylistId,
        LocalDateTime updatedAt,
        Long updatedBy
) {
    public static AdminSiteConfigResult from(SiteConfig config) {
        return new AdminSiteConfigResult(
                config.siteTitleZh(),
                config.siteTitleJa(),
                config.siteTitleEn(),
                config.siteSubtitleZh(),
                config.siteSubtitleJa(),
                config.siteSubtitleEn(),
                config.aboutMdZh(),
                config.aboutMdJa(),
                config.aboutMdEn(),
                config.logoUrl(),
                config.faviconUrl(),
                config.icpNo(),
                config.spotifyPlaylistId(),
                config.updatedAt(),
                config.updatedBy());
    }
}
```

- [x] 新建 `AdminSiteConfigQueryService.java`：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSiteConfigQueryService {
    private final SiteConfigRepository repository;

    public AdminSiteConfigResult query(
            AuthenticatedPrincipal principal) {
        requireReadableRole(principal);
        SiteConfig config = repository.findActive()
                .orElseThrow(this::missingConfig);
        return AdminSiteConfigResult.from(config);
    }
}
```

`requireReadableRole` 只允许 `ADMIN`、`DEMO`。null → `INVALID_TOKEN`，其它角色 → `FORBIDDEN`。

### Step 5：实现后台 Controller

- [x] 新建 `AdminSiteConfigVO.java`，字段与 `AdminSiteConfigResult` 一致，提供 `from(result)`。

- [x] 新建 `AdminSiteConfigController.java`：

```java
@Tag(name = "后台站点配置", description = "站点配置读取与维护")
@RestController
@RequestMapping("/api/admin/site-config")
@RequiredArgsConstructor
public class AdminSiteConfigController {
    private final AdminSiteConfigQueryService queryService;

    @Operation(summary = "查询完整站点配置")
    @GetMapping
    public ApiResponse<AdminSiteConfigVO> get(
            @CurrentUser AuthenticatedPrincipal principal) {
        return ApiResponse.ok(
                AdminSiteConfigVO.from(queryService.query(principal)));
    }
}
```

### Step 6：增加 DEMO 精确只读规则

- [x] 在 `SecurityConfig.java` 的 `/api/admin/**` 通用规则之前增加：

```java
.requestMatchers(
        HttpMethod.GET,
        "/api/admin/site-config")
.hasAnyRole("ADMIN", "DEMO")
```

保留后续：

```java
.requestMatchers("/api/admin/**").hasRole("ADMIN")
```

因此 PUT/POST/PATCH/DELETE 仍只允许 ADMIN。

### Step 7：运行测试确认 GREEN

- [x] 执行：

```powershell
mvn "-Dtest=AdminSiteConfigQueryServiceTest,AdminSiteConfigControllerTest,SecurityConfigTest,ArchitectureRulesTest" test
```

预期：全部通过。

### Step 8：提交 Task 3

- [x] 执行：

```powershell
git diff --check
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-14-backend-v2-site-config.md
git commit -m "开放后台站点配置读取"
```

---

## Task 4：实现 ADMIN 全量更新、事务与并发

**提交信息：** `实现站点配置全量更新`

### Step 1：写更新应用服务失败测试

- [x] 新建 `SiteConfigUpdateServiceTest.java`，覆盖：

- ADMIN 成功更新并返回重新读取的数据库结果。
- DEMO 返回 `FORBIDDEN`。
- null principal 和非法主体 ID 返回 `INVALID_TOKEN`。
- null command 返回 `VALIDATION_ERROR`。
- 领域字段非法映射 `VALIDATION_ERROR`。
- 固定配置行缺失返回 `INTERNAL_ERROR`。
- update 返回 false 时返回 `INTERNAL_ERROR`。
- update 成功但重新读取缺失时返回 `INTERNAL_ERROR`。
- 仓储接收的 `updatedAt` 来自注入 `Clock`，`updatedBy` 来自当前主体 ID。

使用固定 Clock：

```java
Clock clock = Clock.fixed(
        Instant.parse("2026-06-14T03:00:00Z"),
        ZoneId.of("Asia/Tokyo"));
```

### Step 2：写请求 presence 和 Controller 失败测试

- [x] 在 `AdminSiteConfigControllerTest.java` 增加：

- 13 个字段全部出现时调用更新服务。
- 任意一个字段缺失返回 `400 + 90001`。
- 可选字段显式 null 被传入命令。
- 空白可选字段被传入命令并由领域规范化。
- 未知字段返回 `400 + 90001`。

请求示例固定为：

```json
{
  "siteTitleZh": "MyBlog",
  "siteTitleJa": null,
  "siteTitleEn": "My Blog",
  "siteSubtitleZh": "中文副标题",
  "siteSubtitleJa": null,
  "siteSubtitleEn": null,
  "aboutMdZh": "# 关于我",
  "aboutMdJa": null,
  "aboutMdEn": null,
  "logoUrl": "https://example.com/logo.png",
  "faviconUrl": null,
  "icpNo": null,
  "spotifyPlaylistId": "playlist_123"
}
```

### Step 3：写 Repository 更新测试

- [x] 扩展 `DatabaseSiteConfigRepositoryTest.java`：

- 完整更新 13 个字段。
- null 字段真实写为 SQL NULL。
- `updated_at`、`updated_by` 使用传入值。
- `deleted=1` 时 update 返回 false。

### Step 4：写并发失败测试

- [x] 新建 `SiteConfigUpdateConcurrencyTest.java`。

参考已有 `CurrentUserProfileUpdateConcurrencyTest`，使用：

- 两个线程。
- 两个独立事务。
- `@Primary CoordinatedSiteConfigRepository` 包装真实仓储。
- 第一个请求取得 `SELECT ... FOR UPDATE` 行锁后等待。
- 第二个请求开始更新，断言第一事务释放前不能完成。
- 第一事务释放后两个请求依次成功。
- 最终 13 个字段完整来自第二个请求，不允许 title 来自第二个、aboutMd 来自第一个的混合状态。

### Step 5：运行测试确认 RED

- [x] 执行：

```powershell
mvn "-Dtest=SiteConfigUpdateServiceTest,AdminSiteConfigControllerTest,DatabaseSiteConfigRepositoryTest,SiteConfigUpdateConcurrencyTest" test
```

预期：缺少命令、更新服务、presence 请求模型和 PUT Controller。

### Step 6：实现更新命令

- [x] 新建 `UpdateSiteConfigCommand.java`：

```java
public record UpdateSiteConfigCommand(
        String siteTitleZh,
        String siteTitleJa,
        String siteTitleEn,
        String siteSubtitleZh,
        String siteSubtitleJa,
        String siteSubtitleEn,
        String aboutMdZh,
        String aboutMdJa,
        String aboutMdEn,
        String logoUrl,
        String faviconUrl,
        String icpNo,
        String spotifyPlaylistId
) {
    public SiteConfig toDomain(SiteConfig current) {
        return SiteConfig.create(
                SiteConfig.FIXED_ID,
                siteTitleZh,
                siteTitleJa,
                siteTitleEn,
                siteSubtitleZh,
                siteSubtitleJa,
                siteSubtitleEn,
                aboutMdZh,
                aboutMdJa,
                aboutMdEn,
                logoUrl,
                faviconUrl,
                icpNo,
                spotifyPlaylistId,
                current.updatedAt(),
                current.updatedBy());
    }
}
```

### Step 7：实现事务更新服务

- [x] 新建 `SiteConfigUpdateService.java`：

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteConfigUpdateService {
    private final SiteConfigRepository repository;
    private final Clock clock;

    @Transactional
    public AdminSiteConfigResult update(
            AuthenticatedPrincipal principal,
            UpdateSiteConfigCommand command) {
        requireAdmin(principal);
        long userId = parsePositiveUserId(principal.id());
        if (command == null) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    "站点配置请求不能为空");
        }

        SiteConfig current = repository.findActiveForUpdate()
                .orElseThrow(this::missingConfig);
        SiteConfig updated;
        try {
            updated = command.toDomain(current);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    ApiErrorCode.VALIDATION_ERROR,
                    exception.getMessage());
        }

        LocalDateTime updatedAt = LocalDateTime.now(clock);
        if (!repository.update(updated, updatedAt, userId)) {
            log.error("站点配置更新行数异常，siteConfigId={}",
                    SiteConfig.FIXED_ID);
            throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
        }
        return AdminSiteConfigResult.from(
                repository.findActive().orElseThrow(this::missingConfig));
    }
}
```

日志不能打印 command 或关于我正文。

### Step 8：实现 PUT presence 请求

- [x] 新建 `SubmittedField.java`：

```java
record SubmittedField<T>(boolean present, T value) {
    static <T> SubmittedField<T> absent() {
        return new SubmittedField<>(false, null);
    }

    static <T> SubmittedField<T> of(T value) {
        return new SubmittedField<>(true, value);
    }
}
```

该类型保持 package-private，只服务 system Web 请求解析。

- [x] 新建 `UpdateSiteConfigRequest.java`：

- 13 个字段都初始化为 `SubmittedField.absent()`。
- 每个字段有一个 `@JsonSetter`，写为 `SubmittedField.of(value)`。
- `@JsonAnySetter` 拒绝未知字段。
- `toCommand()` 先检查全部字段 `present()`。

缺失字段异常：

```java
throw new ApiException(
        ApiErrorCode.VALIDATION_ERROR,
        "PUT 请求必须包含全部站点配置字段");
```

检查通过后构造只含普通字符串的 `UpdateSiteConfigCommand`。

- [x] 新建 `UpdateSiteConfigOpenApiRequest.java`。

`siteTitleZh`：

```java
@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
String siteTitleZh
```

其余字段：

```java
@Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        types = {"string", "null"})
String siteTitleJa
```

13 个字段均 required；可选业务值字段在 schema 中允许 null。

### Step 9：扩展后台 Controller

- [x] 在 `AdminSiteConfigController` 注入 `SiteConfigUpdateService`，增加：

```java
@Operation(
        summary = "全量更新站点配置",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                content = @Content(schema = @Schema(
                        implementation =
                                UpdateSiteConfigOpenApiRequest.class))))
@PutMapping
public ApiResponse<AdminSiteConfigVO> update(
        @CurrentUser AuthenticatedPrincipal principal,
        @RequestBody UpdateSiteConfigRequest request) {
    return ApiResponse.ok(AdminSiteConfigVO.from(
            updateService.update(principal, request.toCommand())));
}
```

### Step 10：补 Security 更新边界测试

- [x] 扩展 `SecurityConfigTest`：

- ADMIN PUT 返回业务层结果。
- DEMO PUT 返回 `403 + 10003`。
- 匿名 PUT 返回 `401 + 10002`。
- DEMO POST/PATCH/DELETE 不因 GET 只读规则被放行。

### Step 11：运行定向回归确认 GREEN

- [x] 执行：

```powershell
mvn "-Dtest=SiteConfigTest,DatabaseSiteConfigRepositoryTest,SiteConfigUpdateServiceTest,SiteConfigUpdateConcurrencyTest,AdminSiteConfigControllerTest,SecurityConfigTest,ArchitectureRulesTest" test
```

预期：全部通过。

### Step 12：静态检查并提交

- [x] 执行：

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
rg -n "aboutMd|siteTitle|siteSubtitle" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system | rg "log\\.|Logger"
git diff --check
```

预期：无注解 SQL，无配置正文日志，diff 检查通过。

- [x] 提交：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-14-backend-v2-site-config.md
git commit -m "实现站点配置全量更新"
```

---

## Task 5：集成契约、OpenAPI 与文档收尾

**提交信息：** `完成站点配置纵向切片`

### Step 1：写完整 HTTP 集成测试

- [ ] 新建 `SiteConfigIntegrationTest.java`，使用真实 Spring context、H2、JWT 登录和 MockMvc。

覆盖一个完整流程：

1. 匿名 `GET /api/public/site-config?lang=ja`，验证逐字段 fallback。
2. DEMO 登录后 `GET /api/admin/site-config` 成功。
3. DEMO PUT 返回 `403 + 10003`，数据库不变。
4. ADMIN 登录后 PUT 全量更新成功。
5. 响应返回完整规范化值和正确 `updatedBy`。
6. 再次公开读取 zh/ja/en，验证更新立即可见。
7. PUT 缺少任意字段返回 `400 + 90001`，数据库不变。
8. 非法 URL、超长 Markdown、非法 Spotify ID 返回 `400 + 90001`。
9. 固定配置行被删除后，公开查询、后台查询和更新返回 `500 + 99999`。

### Step 2：写 OpenAPI 契约测试

- [ ] 新建 `SiteConfigOpenApiTest.java`。

覆盖：

- `/api/public/site-config` 只有 GET。
- `/api/admin/site-config` 有 GET 和 PUT。
- 公开响应 schema 只含 7 个字段。
- 后台响应 schema 含 13 个业务字段、`updatedAt`、`updatedBy`。
- PUT request schema 13 个字段全部在 required。
- `siteTitleZh` 是 string；其余业务可空字段是 `["string","null"]`。
- schema 不出现 `SubmittedField`、`deleted`、`createdBy`。

### Step 3：运行集成测试确认 RED 或暴露边界遗漏

- [ ] 执行：

```powershell
mvn "-Dtest=SiteConfigIntegrationTest,SiteConfigOpenApiTest" test
```

若失败，保留失败证据，修正对应实现后重新运行，不通过放宽断言掩盖问题。

### Step 4：补齐接口契约文档

- [ ] 新建 `docs/project-handbook/api-contract/site-config.md`，记录：

- 三个 HTTP 接口。
- 公开语言参数和逐字段 fallback。
- ADMIN / DEMO 权限矩阵。
- PUT 13 字段必须出现。
- 字段长度、URL、Markdown 和 Spotify ID 规则。
- 成功与错误响应。
- 固定配置行缺失属于内部错误。
- 关于我返回 Markdown 原文，前端使用安全渲染管线。

- [ ] 修改 `api-contract/README.md`，加入 `site-config.md` 索引。

### Step 5：更新项目状态

- [ ] 修改 `status.md`：

- 记录 system 模块已建立。
- 记录公开读取、后台读取、ADMIN 全量更新和并发保证。
- 写入最终 Maven 测试数字。
- 下一步改为 system 的 `t_attachment` 纵向切片。

- [ ] 修改 `roadmap.md`：

- system 保持未完成。
- 在 system 项描述中标记 `t_site_config` 已完成，剩余 `t_attachment`、`t_friend_link`。

- [ ] 修改 `m3-preflight-review.md`：

- 增加 2026-06-14 站点配置纵向切片记录。
- 说明无新 Flyway、H2 并发通过、Docker 条件测试状态。

### Step 6：更新设计和计划实施证据

- [ ] 执行：

```powershell
git log -4 --format="%h %s"
```

把 Task 1 至 Task 4 的真实 SHA 和中文提交信息写入：

- `docs/superpowers/specs/2026-06-14-backend-v2-site-config-design.md`
- 本计划

设计状态改为“已实施（2026-06-14）”。Task 5 自身不伪造 SHA。

### Step 7：运行全量验证

- [ ] 执行：

```powershell
git diff --check
mvn clean test
```

预期：

- `BUILD SUCCESS`
- 0 failures
- 0 errors
- Docker 不可用时仅 Testcontainers MySQL 条件测试 skipped

把 Maven 最终 tests、failures、errors、skipped 数量回填到设计文档、`status.md` 和接口架构说明。

### Step 8：最终静态审查

- [ ] 执行：

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
rg -n "aboutMd|siteTitle|siteSubtitle" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system | rg "log\\.|Logger"
rg -n "SubmittedField" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain
git status --short
```

预期：

- 无注解 SQL。
- 无站点配置正文日志。
- `SubmittedField` 不越过 Web 层。
- 提交前只有本 Task 的测试、文档和计划勾选变更。

### Step 9：提交 Task 5

- [ ] 将所有 Task 步骤标记完成，然后提交：

```powershell
git add MyBlog-springboot-v2/src/test docs/project-handbook docs/superpowers/specs/2026-06-14-backend-v2-site-config-design.md docs/superpowers/plans/2026-06-14-backend-v2-site-config.md
git commit -m "完成站点配置纵向切片"
```

### Step 10：最终确认

- [ ] 执行：

```powershell
git status --short
git log -5 --oneline
```

预期：

- 工作区干净。
- 最近五个提交依次对应领域持久化、公开查询、后台读取、全量更新、集成文档收尾。

---

## 完成标准

- [ ] `system` 四层模块真实建立并通过 ArchUnit。
- [ ] `GET /api/public/site-config?lang=zh|ja|en` 匿名可用。
- [ ] 日英展示字段为空时逐字段回退中文。
- [ ] `GET /api/admin/site-config` 允许 ADMIN、DEMO。
- [ ] `PUT /api/admin/site-config` 仅允许 ADMIN。
- [ ] PUT 的 13 个业务字段必须全部出现。
- [ ] 可选字段支持 null / 空白清空。
- [ ] Markdown 原文保留，纯空白清空，单语最大 50,000。
- [ ] URL 仅接受绝对 HTTP / HTTPS。
- [ ] Spotify ID 只接受字母、数字、下划线、连字符。
- [ ] 更新使用固定行锁和完整单条 XML UPDATE。
- [ ] 并发 PUT 不产生字段混合。
- [ ] 固定配置行缺失不自动补建，返回 `500 + 99999`。
- [ ] 无新增 Flyway、注解 SQL、配置缓存和 Markdown 渲染依赖。
- [ ] OpenAPI 不暴露内部 presence 类型和审计删除字段。
- [ ] 五个 Task 分别形成中文本地提交。
- [ ] 全量 Maven 测试和 `git diff --check` 通过。
