# Backend V2 附件纵向切片实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Do not use subagents.

**Goal:** 为 `system` 模块建立图片附件上传、去重恢复、分页查询和详情查询，并同时提供 LOCAL 与 AWS S3 存储实现。

**Architecture:** 附件元数据归 `system` 四层模块，存储端口与 LOCAL/S3 adapter 归 `common`。上传服务先把输入流写入受控临时文件并完成 SHA-256 与图片结构识别，再执行查重、存储写入和短事务登记；数据库失败时由外层服务补偿删除本次对象。

**Tech Stack:** Java 17、Spring Boot 3.5、Spring Security、MyBatis-Plus、Mapper XML、AWS SDK for Java v2 S3、TwelveMonkeys ImageIO WebP、H2、JUnit 5、Mockito、MockMvc、springdoc、Lombok。

---

## 0. 执行约束

- 在现有 worktree `E:\My-Blog\.worktrees\backend-v2-refactor` 中执行。
- 每个 Task 独立形成一个中文本地提交，不把五批压成一个大提交。
- 新业务类和关键补偿分支必须有必要的中文注释。
- SQL 只能写在 Mapper XML，禁止 `@Select/@Insert/@Update/@Delete`。
- 不实现附件删除、回收站、清理任务、文章封面引用检查或正文弱审计。
- 不连接真实 AWS；S3 测试只使用 mock `S3Client`。
- Task 1 至 Task 4 每批只运行定向测试；Task 5 执行全量 `mvn clean test`。
- 每次提交前执行 `git diff --check`，并确认没有混入其它文件。

## 1. 文件结构

### Common storage port

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/StorageType.java`
  - 定义 `LOCAL/S3/OSS`，允许读取历史 OSS 元数据。
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/StoreObjectCommand.java`
  - 存储写入命令，只包含受控临时文件、对象键、实际 MIME 和长度。
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/StoredObject.java`
  - 存储写入结果。
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/StorageService.java`
  - `store/exists/delete` 稳定端口。
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/StorageServiceRegistry.java`
  - 按当前配置选上传目标，按记录类型路由既有对象。
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/StorageOperationException.java`
  - 屏蔽文件系统和 AWS SDK 的具体异常。

### Common image and configuration

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/config/StorageProperties.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/config/StorageConfiguration.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/image/ImageFormat.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/image/InspectedImage.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/image/ImageInspector.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/image/UploadSpooler.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/image/SpooledUpload.java`

### LOCAL adapter

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/infrastructure/storage/local/LocalStorageService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/web/LocalStorageWebConfiguration.java`

### S3 adapter

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/infrastructure/storage/s3/S3StorageService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/infrastructure/storage/s3/S3StorageConfiguration.java`

### Attachment domain and persistence

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/attachment/Attachment.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/attachment/NewAttachment.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/attachment/AttachmentLookup.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/attachment/AttachmentPage.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain/attachment/AttachmentRepository.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence/entity/AttachmentEntity.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence/mapper/AttachmentMapper.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/infrastructure/persistence/repository/MyBatisAttachmentRepository.java`
- `MyBlog-springboot-v2/src/main/resources/mapper/system/AttachmentMapper.xml`

### Attachment application and web

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentResult.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadCommand.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentRegistrationService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentRestoreService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentQueryService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/AdminAttachmentController.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/web/AttachmentVO.java`

### Tests and docs

- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/common/storage/**`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/domain/attachment/**`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/application/attachment/**`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/infrastructure/persistence/DatabaseAttachmentRepositoryTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/web/AdminAttachmentControllerTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/web/AttachmentOpenApiTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system/integration/AttachmentIntegrationTest.java`
- 修改 `SecurityConfigTest.java`、`ArchitectureRulesTest.java` 和配置测试。
- 新建 `docs/project-handbook/api-contract/attachment.md` 并更新索引、状态、路线图和预检记录。

不修改 Flyway。`t_attachment`、唯一键 `uk_attachment_hash` 和存储索引已经存在。

---

## Task 1：建立附件领域与持久化查询

**提交信息：** `建立附件领域与持久化查询`

### Step 1：写领域失败测试

- [x] 新建 `AttachmentTest.java`，覆盖：

```java
@Test
void createsValidatedAttachment() {
    Attachment attachment = Attachment.reconstitute(
            1001L,
            StorageType.S3,
            "myblog-assets",
            "attachments/2026/06/a.webp",
            "https://static.example.com/attachments/2026/06/a.webp",
            "image/webp",
            1024L,
            1600,
            900,
            "cover.webp",
            "a".repeat(64),
            LocalDateTime.of(2026, 6, 14, 12, 0),
            2001L);

    assertThat(attachment.id()).isEqualTo(1001L);
    assertThat(attachment.storageType()).isEqualTo(StorageType.S3);
}
```

再覆盖：

- ID 必须为正数。
- `StorageType` 接受 `LOCAL/S3/OSS`。
- object key、public URL、content type、hash 必填。
- public URL 只接受绝对 HTTP/HTTPS URL。
- hash 必须为 64 位小写十六进制。
- file size 为 `1..10 MiB`。
- width/height 为正数、单边不超过 20,000、总像素不超过 40,000,000。
- 总像素使用 `Math.multiplyExact((long) width, height)` 计算，禁止 `int` 乘法溢出
  后绕过限制。
- 原始文件名去路径、控制字符和首尾空白，最长 255；空白转 null。
- `NewAttachment.create(...)` 执行相同业务校验但不要求 ID 和创建审计。

### Step 2：写仓储失败测试

- [x] 新建 `DatabaseAttachmentRepositoryTest.java`，每个测试前执行：

```java
@BeforeEach
void clearAttachments() {
    jdbcTemplate.update("DELETE FROM t_attachment");
}
```

插入 active 与 deleted 测试记录后覆盖：

- `findActiveById` 只返回 active。
- `findByHashIncludingDeleted` 可返回 deleted，并通过 `AttachmentLookup.deleted()` 标识。
- `findActivePage(1, 2)` 按 `created_at DESC, id DESC`。
- `countActive` 不统计 deleted。
- `insert(NewAttachment)` 使用 `ASSIGN_ID` 生成正 ID，审计字段来自 MyBatis handler。
- `restoreDeleted(id, updatedAt, updatedBy)` 清空 `deleted_at/deleted_by` 并更新修改审计。
- 第二次恢复影响 0 行。

### Step 3：运行测试确认 RED

- [x] 执行：

```powershell
mvn "-Dtest=AttachmentTest,DatabaseAttachmentRepositoryTest" test
```

预期：编译失败，提示附件领域、仓储和 Mapper 尚不存在。

### Step 4：实现领域类型

- [x] 新建 `StorageType.java`：

```java
public enum StorageType {
    LOCAL,
    S3,
    OSS;

    public static StorageType parse(String value) {
        try {
            return StorageType.valueOf(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("不支持的附件存储类型", exception);
        }
    }
}
```

- [x] 新建 `Attachment.java`，核心签名固定为：

```java
public record Attachment(
        long id,
        StorageType storageType,
        String bucket,
        String objectKey,
        String publicUrl,
        String contentType,
        long fileSize,
        int width,
        int height,
        String originalFilename,
        String hashSha256,
        LocalDateTime createdAt,
        Long createdBy
) {
    public static Attachment reconstitute(/* 同顺序参数 */) {
        // 调用私有 normalizeAndValidate 后返回实例。
    }
}
```

- [x] 新建 `NewAttachment.java`，字段与 `Attachment` 的存储业务字段一致但不含 ID 和审计：

```java
public record NewAttachment(
        StorageType storageType,
        String bucket,
        String objectKey,
        String publicUrl,
        String contentType,
        long fileSize,
        int width,
        int height,
        String originalFilename,
        String hashSha256,
        long createdBy
) {
    public static NewAttachment create(/* 同顺序参数 */) {
        // 复用 AttachmentValidation 包内辅助类，禁止复制两套边界。
    }
}
```

- [x] 使用 package-private `AttachmentValidation` 集中实现字段规范化，保留中文异常消息。

- [x] 新建：

```java
public record AttachmentLookup(Attachment attachment, boolean deleted) {
}

public record AttachmentPage(
        List<Attachment> records,
        long total,
        int page,
        int size
) {
    public AttachmentPage {
        records = List.copyOf(records);
    }
}
```

### Step 5：实现 Repository、Entity 与 XML

- [x] 新建 `AttachmentRepository.java`：

```java
public interface AttachmentRepository {
    Optional<Attachment> findActiveById(long id);
    Optional<AttachmentLookup> findByHashIncludingDeleted(String hashSha256);
    AttachmentPage findActivePage(int page, int size);
    Attachment insert(NewAttachment attachment);
    boolean restoreDeleted(long id, LocalDateTime updatedAt, long updatedBy);
}
```

- [x] 新建 `AttachmentEntity extends BaseEntity`，完整映射 `t_attachment` 业务列并为字段写中文注释。

- [x] 新建 `AttachmentMapper extends BaseMapper<AttachmentEntity>`：

```java
AttachmentEntity selectActiveById(@Param("id") long id);
AttachmentEntity selectByHashIncludingDeleted(@Param("hash") String hash);
List<AttachmentEntity> selectActivePage(
        @Param("offset") long offset,
        @Param("size") int size);
long countActive();
int restoreDeleted(
        @Param("id") long id,
        @Param("updatedAt") LocalDateTime updatedAt,
        @Param("updatedBy") long updatedBy);
```

- [x] `AttachmentMapper.xml` 必须显式包含完整列清单。关键 SQL 固定为：

```xml
<select id="selectByHashIncludingDeleted"
        resultType="com.tyb.myblog.v2.system.infrastructure.persistence.entity.AttachmentEntity">
    SELECT <include refid="attachmentColumns"/>
    FROM t_attachment
    WHERE hash_sha256 = #{hash}
    LIMIT 1
</select>

<select id="selectActivePage" resultType="...AttachmentEntity">
    SELECT <include refid="attachmentColumns"/>
    FROM t_attachment
    WHERE deleted = 0
    ORDER BY created_at DESC, id DESC
    LIMIT #{size} OFFSET #{offset}
</select>

<update id="restoreDeleted">
    UPDATE t_attachment
    SET deleted = 0,
        deleted_at = NULL,
        deleted_by = NULL,
        updated_at = #{updatedAt},
        updated_by = #{updatedBy}
    WHERE id = #{id}
      AND deleted = 1
</update>
```

XML 前写中文注释说明 hash 查询故意绕过逻辑删除过滤。

- [x] `MyBatisAttachmentRepository`：

  - page/size 转 offset 时使用 `Math.multiplyExact((long) page - 1, size)`。
  - `insert` 用 Mapper `insert(entity)`，确认影响 1 行且生成正 ID，再转回领域。
  - hash 查询从 Entity 的 `deleted` 构造 `AttachmentLookup`。
  - 所有 Entity/Domain 转换集中在仓储 adapter。

### Step 6：运行定向测试确认 GREEN

- [x] 执行：

```powershell
mvn "-Dtest=AttachmentTest,DatabaseAttachmentRepositoryTest,ArchitectureRulesTest" test
```

预期：全部通过。

### Step 7：静态检查并提交

- [x] 执行：

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
git diff --check
```

预期：无注解 SQL，diff 检查无输出。

- [x] 提交：

```powershell
git add MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/storage/StorageType.java MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system MyBlog-springboot-v2/src/main/resources/mapper/system/AttachmentMapper.xml MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/system docs/superpowers/plans/2026-06-14-backend-v2-attachment.md
git commit -m "建立附件领域与持久化查询"
```

---

## Task 2：建立存储抽象与本地实现

**提交信息：** `建立附件存储抽象与本地实现`

### Step 1：增加图片依赖并写配置失败测试

- [x] 在 `pom.xml` properties 增加：

```xml
<twelvemonkeys.version>3.13.1</twelvemonkeys.version>
```

并增加：

```xml
<dependency>
    <groupId>com.twelvemonkeys.imageio</groupId>
    <artifactId>imageio-webp</artifactId>
    <version>${twelvemonkeys.version}</version>
</dependency>
```

- [x] 新建 `StoragePropertiesTest.java`，覆盖：

- LOCAL 当前后端要求 root、bucket alias、public base URL。
- S3 当前后端要求 region、bucket、public base URL。
- 非当前后端配置可完全缺省。
- 非当前后端配置完整时可以注册对应 adapter。
- max file size 大于 0 且不超过 `10 MiB`。
- public base URL 仅 HTTP/HTTPS，并去除末尾 `/`。
- `type=OSS` 启动失败，消息说明本轮没有 OSS 上传 adapter。

### Step 2：写 spool 与图片识别失败测试

- [x] 新建 `UploadSpoolerTest.java`，使用 `@TempDir` 覆盖：

```java
@Test
void spoolsAndHashesWithinLimit() throws Exception {
    byte[] content = "image-bytes".getBytes(UTF_8);
    try (SpooledUpload upload = spooler.spool(
            new ByteArrayInputStream(content), content.length)) {
        assertThat(Files.readAllBytes(upload.path())).isEqualTo(content);
        assertThat(upload.size()).isEqualTo(content.length);
        assertThat(upload.sha256()).hasSize(64);
    }
    assertThat(Files.list(tempDir)).isEmpty();
}
```

再覆盖空输入、读取到 `limit + 1` 立即删除临时文件并抛校验异常、输入流异常仍清理。

- [x] 新建 `ImageInspectorTest.java`：

- 用测试资源或运行时生成 JPEG、PNG、GIF。
- 增加最小合法 WebP fixture 到
  `src/test/resources/images/valid.webp`。
- 四种格式返回实际 MIME、扩展名、宽高。
- 客户端扩展名和 MIME 不参与识别。
- 文本伪装图片、截断图片、0 尺寸、超单边、超总像素拒绝。
- GIF 超 500 帧拒绝；逐帧读取失败拒绝。

### Step 3：写 LOCAL 与 Registry 失败测试

- [x] 新建 `LocalStorageServiceTest.java`，使用 `@TempDir` 覆盖：

- 写入后字节一致，返回 `LOCAL/bucket/objectKey/publicUrl`。
- 中间目录自动创建。
- 最终路径不存在时先写同目录临时文件，再原子移动。
- `exists` 和 `delete` 正常。
- `../`、绝对路径、规范化后越出 root 的 key 拒绝。
- 写入失败不保留最终文件或临时文件。

- [x] 新建 `StorageServiceRegistryTest.java`：

- 当前 type 选择上传目标。
- 按指定 `StorageType` 路由。
- `OSS` 或未注册的非当前后端抛 `StorageOperationException`。
- 重复注册同一 type 在构造时失败。

### Step 4：运行测试确认 RED

- [x] 执行：

```powershell
mvn "-Dtest=StoragePropertiesTest,UploadSpoolerTest,ImageInspectorTest,LocalStorageServiceTest,StorageServiceRegistryTest" test
```

预期：缺少 storage port、配置、图片和 LOCAL 实现。

### Step 5：实现稳定存储端口

- [x] 新建：

```java
public record StoreObjectCommand(
        Path source,
        String objectKey,
        String contentType,
        long contentLength
) {
    public StoreObjectCommand {
        Objects.requireNonNull(source, "存储源文件不能为空");
        // 校验 objectKey、contentType 与正长度。
    }
}

public record StoredObject(
        StorageType storageType,
        String bucket,
        String objectKey,
        String publicUrl
) {
}

public interface StorageService {
    StorageType type();
    StoredObject store(StoreObjectCommand command);
    boolean exists(String bucket, String objectKey);
    void delete(String bucket, String objectKey);
}
```

- [x] `StorageOperationException extends RuntimeException` 只保存稳定中文消息和 cause。

- [x] `StorageServiceRegistry` 构造时将 `List<StorageService>` 收口为
  `EnumMap<StorageType, StorageService>`：

```java
public StorageService current() {
    return required(properties.type());
}

public StorageService required(StorageType type) {
    StorageService service = services.get(type);
    if (service == null) {
        throw new StorageOperationException("附件存储后端不可用：" + type);
    }
    return service;
}
```

### Step 6：实现配置、spool 和图片识别

- [x] `StorageProperties` 使用 `@ConfigurationProperties("myblog.storage")`，结构固定为：

```java
public class StorageProperties {
    private StorageType type = StorageType.LOCAL;
    private DataSize maxFileSize = DataSize.ofMegabytes(10);
    private Local local = new Local();
    private S3 s3 = new S3();
    // getter/setter 使用 Lombok @Getter/@Setter
}
```

`Local`：`Path root`、`String bucketAlias`、`URI publicBaseUrl`。

`S3`：`String region`、`String bucket`、`URI publicBaseUrl`。

- [x] `StorageConfiguration` 提供 `StorageProperties`、`UploadSpooler`、
  `ImageInspector` 和 `StorageServiceRegistry` bean，并执行条件校验。当前后端必须有
  adapter；非当前后端只在配置完整时注册。

- [x] `SpooledUpload implements AutoCloseable`：

```java
public record SpooledUpload(Path path, long size, String sha256)
        implements AutoCloseable {
    @Override
    public void close() throws IOException {
        Files.deleteIfExists(path);
    }
}
```

- [x] `UploadSpooler.spool(InputStream input, long maxBytes)`：

  - `Files.createTempFile("myblog-upload-", ".tmp")`。
  - 单次读取 8 KiB。
  - 同步更新 `MessageDigest SHA-256` 和字节数。
  - 超限或异常时 `deleteIfExists`。
  - 空文件抛 `IllegalArgumentException("上传文件不能为空")`。

- [x] `ImageFormat` 固定：

```java
JPEG("image/jpeg", "jpg"),
PNG("image/png", "png"),
WEBP("image/webp", "webp"),
GIF("image/gif", "gif");
```

- [x] `ImageInspector.inspect(Path)`：

  - 使用 `ImageIO.createImageInputStream` 和首个 `ImageReader`。
  - 将 reader format name 映射到四种白名单格式。
  - 先读 width/height 并执行限制，再 `reader.read(0)` 完整解码首帧。
  - GIF 调 `getNumImages(true)`，帧数必须 `1..500`，逐帧读取并校验尺寸。
  - 所有 reader 和 stream 在 finally/dispose 中释放。
  - 不返回 `BufferedImage`，只返回 `InspectedImage(format, width, height)`。

### Step 7：实现 LOCAL adapter 和只读映射

- [x] `LocalStorageService`：

  - 构造时把 root 转绝对规范路径并创建目录。
  - `resolveObjectKey` 将 `/` 分段解析，拒绝空段、`.`、`..` 和绝对路径。
  - 解析后要求 `target.startsWith(root)`。
  - 在目标目录 `createTempFile`，`Files.copy(source, temp, REPLACE_EXISTING)`，
    再 `Files.move(temp, target, ATOMIC_MOVE)`；文件系统不支持原子移动时记录 warn 并
    使用 `REPLACE_EXISTING` 移动。
  - `publicUrl = baseUrl + "/" + objectKey`。
  - bucket 参数必须等于配置 alias。

- [x] `LocalStorageWebConfiguration implements WebMvcConfigurer`：

```java
registry.addResourceHandler("/media/**")
        .addResourceLocations(properties.local().root().toUri().toString())
        .setCacheControl(CacheControl.noCache());
```

只提供静态 GET/HEAD，不实现目录接口。

### Step 8：配置各 profile

- [x] `application.yml` 增加：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 11MB
myblog:
  storage:
    type: LOCAL
    max-file-size: 10MB
```

- [x] `application-local.yml` 增加：

```yaml
myblog:
  storage:
    type: LOCAL
    local:
      root: ${MYBLOG_STORAGE_LOCAL_ROOT:${user.home}/.myblog-v2/uploads}
      bucket-alias: local
      public-base-url: ${MYBLOG_STORAGE_LOCAL_PUBLIC_BASE_URL:http://localhost:8080/media}
  security:
    public-endpoints:
      - method: GET
        path: /media/**
```

保留已有 public endpoints，不能覆盖丢失。

- [x] `application-test.yml` 配置稳定测试临时根和 `/media/**` GET 白名单；集成测试
  在 `@BeforeEach` 清空附件目录。

### Step 9：运行定向测试确认 GREEN

- [x] 执行：

```powershell
mvn "-Dtest=StoragePropertiesTest,UploadSpoolerTest,ImageInspectorTest,LocalStorageServiceTest,StorageServiceRegistryTest,ApplicationConfigurationTest,ArchitectureRulesTest" test
```

预期：全部通过，Enforcer dependency convergence 通过。

### Step 10：静态检查并提交

- [x] 执行：

```powershell
rg -n "access.?key|secret.?key|public-read" MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
git diff --check
```

预期：没有硬编码 AWS 凭证或 `public-read`。

- [x] 提交：

```powershell
git add MyBlog-springboot-v2/pom.xml MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test MyBlog-springboot-v2/src/test/resources docs/superpowers/plans/2026-06-14-backend-v2-attachment.md
git commit -m "建立附件存储抽象与本地实现"
```

---

## Task 3：接入 AWS S3 附件存储

**提交信息：** `接入AWS S3附件存储`

### Step 1：增加 AWS SDK 依赖

- [x] 在 `pom.xml` properties 增加：

```xml
<aws.sdk.version>2.46.9</aws.sdk.version>
```

在 `dependencyManagement` 引入 AWS BOM：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>${aws.sdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

只增加：

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
```

### Step 2：写 S3 adapter 失败测试

- [x] 新建 `S3StorageServiceTest.java`，mock `S3Client`，覆盖：

```java
verify(s3Client).putObject(
        argThat(request ->
                request.bucket().equals("myblog-assets")
                && request.key().equals("attachments/2026/06/a.webp")
                && request.contentType().equals("image/webp")
                && request.contentLength() == 1024L
                && request.acl() == null),
        any(RequestBody.class));
```

再覆盖：

- 返回 configured public base URL 拼接后的地址。
- `exists` 对成功 `headObject` 返回 true。
- `NoSuchKeyException` 或 HTTP 404 返回 false。
- 其它 `S3Exception` 包装为 `StorageOperationException`。
- `deleteObject` 使用正确 bucket/key。
- 日志上下文只包含 type、bucket、key、request id；测试通过 ListAppender 断言不含
  文件内容和凭证。

### Step 3：写 S3 bean 配置失败测试

- [x] 通过 `StoragePropertiesTest` 和运行配置测试覆盖 S3 配置边界：

- `type=S3` 且配置完整时创建 `S3Client`、`S3StorageService`。
- client 使用 `Region.of(region)` 和 `DefaultCredentialsProvider.create()`。
- 没有 access key/secret key 配置字段。
- 当前 LOCAL 且 S3 配置缺省时不创建 S3 bean。
- 当前 LOCAL 且 S3 配置完整时同时注册 S3，以支持迁移期既有记录。

### Step 4：运行测试确认 RED

- [x] 执行：

```powershell
mvn "-Dtest=S3StorageServiceTest,StoragePropertiesTest" test
```

预期：S3 adapter 与配置尚不存在。

### Step 5：实现 S3 bean 配置

- [x] `S3StorageConfiguration` 使用条件：

- 当前 type 为 S3，或 S3 三项配置完整时启用。
- `S3Client.builder().region(...).credentialsProvider(DefaultCredentialsProvider.create()).build()`。
- bean 使用 `destroyMethod = "close"`。
- 禁止 endpoint override、静态凭证和对象 ACL。

### Step 6：实现 `S3StorageService`

- [x] `store` 固定使用：

```java
PutObjectRequest request = PutObjectRequest.builder()
        .bucket(properties.s3().bucket())
        .key(command.objectKey())
        .contentType(command.contentType())
        .contentLength(command.contentLength())
        .build();
s3Client.putObject(request, RequestBody.fromFile(command.source()));
```

- [x] `exists` 使用 `headObject`；只把 404/NoSuchKey 映射为 false。

- [x] `delete` 使用 `deleteObject`。

- [x] catch `S3Exception` 时日志仅写：

```java
log.error(
        "S3附件操作失败，operation={}, bucket={}, objectKey={}, requestId={}",
        operation,
        bucket,
        objectKey,
        exception.requestId());
```

随后抛 `StorageOperationException("附件存储操作失败", exception)`。

### Step 7：配置生产环境

- [x] `application-prod.yml` 增加：

```yaml
myblog:
  storage:
    type: ${MYBLOG_STORAGE_TYPE:S3}
    max-file-size: 10MB
    s3:
      region: ${MYBLOG_STORAGE_S3_REGION}
      bucket: ${MYBLOG_STORAGE_S3_BUCKET}
      public-base-url: ${MYBLOG_STORAGE_S3_PUBLIC_BASE_URL}
```

不添加 access key 或 secret key；部署通过 AWS Default Credentials Provider Chain。

### Step 8：运行定向测试确认 GREEN

- [x] 执行：

```powershell
mvn "-Dtest=S3StorageServiceTest,StoragePropertiesTest,StorageServiceRegistryTest,RuntimeProfileConfigurationTest,ArchitectureRulesTest" test
```

预期：全部通过，未访问网络或真实 AWS。

### Step 9：依赖和安全检查并提交

- [x] 执行：

```powershell
mvn dependency:tree "-Dincludes=software.amazon.awssdk"
rg -n "StaticCredentialsProvider|AwsBasicCredentials|public-read|access.?key|secret.?key" MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test
git diff --check
```

预期：业务显式依赖只有 S3 模块；没有静态凭证和 ACL。

- [x] 提交：

```powershell
git add MyBlog-springboot-v2/pom.xml MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test MyBlog-springboot-v2/src/main/resources/application-prod.yml docs/superpowers/plans/2026-06-14-backend-v2-attachment.md
git commit -m "接入AWS S3附件存储"
```

---

## Task 4：实现附件上传与去重恢复

**提交信息：** `实现附件上传与去重恢复`

### Step 1：写上传应用服务失败测试

- [x] 新建 `AttachmentUploadServiceTest.java`，使用 mock repository、registry、
  spooler、inspector、registration service、restore service 和固定 `Clock`。

覆盖：

- null principal → `INVALID_TOKEN`。
- DEMO/GUEST → `FORBIDDEN`。
- 非数字或非正 ID → `INVALID_TOKEN`。
- null command/input → `VALIDATION_ERROR`。
- active hash 命中且对象存在：直接返回，不 store、不 insert。
- active hash 命中但对象缺失：`INTERNAL_ERROR`。
- deleted hash 命中且对象存在：调用恢复服务并返回同 ID，不 store。
- deleted hash 命中但对象缺失：`INTERNAL_ERROR`。
- 未命中：对象键符合 `attachments/yyyy/MM/{uuid}.{ext}`，写当前后端后短事务插入。
- 插入普通异常：删除本请求对象并返回内部错误。
- `DuplicateKeyException`：删除本请求对象，重新查获胜记录并返回。
- 补偿删除失败：保留原数据库异常为 cause，日志记录 type/bucket/key。
- `SpooledUpload.close()` 在所有成功和失败路径执行。

### Step 2：写短事务服务失败测试

- [x] 新建 `AttachmentRegistrationServiceTest.java`：

- `register(NewAttachment)` 只调用 repository insert。
- `AttachmentRestoreService.restore(id, actorId)` 使用固定 Clock。
- 条件恢复成功后重新按 hash 或 ID 读取 active。
- 更新 0 行时重新读取，支持并发恢复获胜者。
- 恢复后仍无 active 记录返回内部错误。

- [x] 事务边界、唯一键竞争和物理补偿由应用服务测试与
  `AttachmentIntegrationTest.java` 联合验证：

- `AttachmentRegistrationService.register` 的数据库异常回滚。
- `AttachmentUploadService` 在事务外执行物理补偿。
- 唯一键竞争后只保留获胜行。

### Step 3：写 Controller 与 Security 失败测试

- [x] 新建 `AdminAttachmentControllerTest.java`：

- multipart 字段名固定为 `file`。
- Controller 只把 `originalFilename` 和 `InputStream` 交给应用命令。
- 成功返回 `ApiResponse<AttachmentVO>`。
- 缺少 file、空 file、超限和非法图片返回 `400 + 90001`。
- VO 不包含 `deleted/updatedAt/updatedBy/deletedAt/deletedBy`。

- [x] 扩展 `SecurityConfigTest.java`：

- ADMIN POST `/api/admin/attachments` 可进入业务层。
- DEMO POST 返回 `403 + 10003`。
- 匿名 POST 返回 `401 + 10002`。

### Step 4：运行测试确认 RED

- [x] 执行：

```powershell
mvn "-Dtest=AttachmentUploadServiceTest,AttachmentRegistrationServiceTest,AttachmentUploadTransactionIntegrationTest,AdminAttachmentControllerTest,SecurityConfigTest" test
```

预期：上传服务、事务服务和 Controller 尚不存在。

### Step 5：实现应用命令与结果

- [x] 新建：

```java
public record AttachmentUploadCommand(
        String originalFilename,
        InputStream inputStream
) {
}

public record AttachmentResult(
        long id,
        StorageType storageType,
        String bucket,
        String objectKey,
        String publicUrl,
        String contentType,
        long fileSize,
        int width,
        int height,
        String originalFilename,
        String hashSha256,
        LocalDateTime createdAt,
        Long createdBy
) {
    public static AttachmentResult from(Attachment attachment) {
        // 一一映射公开后台字段。
    }
}
```

### Step 6：实现短事务服务

- [x] `AttachmentRegistrationService`：

```java
@Service
@RequiredArgsConstructor
public class AttachmentRegistrationService {
    private final AttachmentRepository repository;

    @Transactional
    public Attachment register(NewAttachment attachment) {
        return repository.insert(attachment);
    }
}
```

- [x] `AttachmentRestoreService`：

```java
@Transactional
public Attachment restore(
        AttachmentLookup lookup,
        long actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    repository.restoreDeleted(lookup.attachment().id(), now, actorId);
    return repository.findByHashIncludingDeleted(
                    lookup.attachment().hashSha256())
            .filter(result -> !result.deleted())
            .map(AttachmentLookup::attachment)
            .orElseThrow(this::restoreFailed);
}
```

恢复服务不碰物理存储；物理存在性由外层上传服务确认。

### Step 7：实现上传编排

- [x] `AttachmentUploadService` 保持无 `@Transactional`，流程固定为：

```java
public AttachmentResult upload(
        AuthenticatedPrincipal principal,
        AttachmentUploadCommand command) {
    long actorId = requireAdmin(principal);
    try (InputStream input = command.inputStream();
         SpooledUpload upload = spooler.spool(
                 input, properties.maxFileBytes())) {
        InspectedImage image = inspector.inspect(upload.path());
        Optional<AttachmentLookup> duplicate =
                repository.findByHashIncludingDeleted(upload.sha256());
        if (duplicate.isPresent()) {
            return AttachmentResult.from(
                    reuseOrRestore(duplicate.get(), actorId));
        }
        return AttachmentResult.from(
                storeAndRegister(upload, image, command.originalFilename(), actorId));
    } catch (IllegalArgumentException exception) {
        throw new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                exception.getMessage());
    } catch (IOException exception) {
        log.error("附件临时文件清理失败", exception);
        throw new ApiException(ApiErrorCode.INTERNAL_ERROR);
    }
}
```

- [x] 应用服务取得 command 后负责关闭上传输入流；Controller 不在调用返回前提前关闭，
  所有异常路径都由同一个 try-with-resources 收口。

- [x] `reuseOrRestore` 必须：

  - 使用记录自己的 `storageType` 通过 registry 路由。
  - `exists(bucket,key)` 为 false 时记录必要上下文并返回内部错误。
  - active 直接返回。
  - deleted 调 `restoreService.restore`。

- [x] `storeAndRegister`：

  - `Clock` 获取 Asia/Tokyo 当前年月。
  - UUID 使用注入的 `Supplier<UUID>` 或独立 `ObjectKeyGenerator`，测试不得依赖随机值。
  - 调当前 StorageService store。
  - 用实际存储结果、图片识别结果、spool size/hash 创建 `NewAttachment`。
  - 调 registration service。
  - 普通 RuntimeException 先补偿 delete，再映射内部错误。
  - `DuplicateKeyException` 补偿后重新按 hash 查获胜记录，并复用相同
    active/deleted 分支。

补偿方法不能吞掉原异常：

```java
private void compensate(
        StorageService storage,
        StoredObject object,
        RuntimeException original) {
    try {
        storage.delete(object.bucket(), object.objectKey());
    } catch (RuntimeException compensationFailure) {
        original.addSuppressed(compensationFailure);
        log.error(
                "附件对象补偿删除失败，storageType={}, bucket={}, objectKey={}",
                object.storageType(),
                object.bucket(),
                object.objectKey(),
                compensationFailure);
    }
}
```

### Step 8：实现 multipart Controller

- [x] `AttachmentVO` 字段与 `AttachmentResult` 一致并提供 `from`。

- [x] `AdminAttachmentController`：

```java
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@Operation(
        summary = "上传图片附件",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                required = true,
                content = @Content(
                        mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                        schema = @Schema(
                                implementation = AttachmentUploadOpenApiRequest.class))))
public ApiResponse<AttachmentVO> upload(
        @CurrentUser AuthenticatedPrincipal principal,
        @RequestPart("file") MultipartFile file) throws IOException {
    return ApiResponse.ok(AttachmentVO.from(
            uploadService.upload(
                    principal,
                    new AttachmentUploadCommand(
                            file.getOriginalFilename(),
                            file.getInputStream()))));
}
```

- [x] 新建仅用于文档的 `AttachmentUploadOpenApiRequest`：

```java
public record AttachmentUploadOpenApiRequest(
        @Schema(type = "string", format = "binary")
        MultipartFile file
) {
}
```

若 springdoc 能直接从 `@RequestPart MultipartFile` 生成正确 binary schema，则删除该
文档类型并在 OpenAPI 测试中固定实际输出，避免无必要类型。

### Step 9：补全异常映射

- [x] 在 `GlobalExceptionHandler` 增加：

- `MaxUploadSizeExceededException` → `400 + 90001 + "上传文件不能超过10 MiB"`。
- `MissingServletRequestPartException` → `400 + 90001 + "缺少上传文件"`。
- 不回显 multipart 解析器内部路径或异常消息。

### Step 10：运行定向测试确认 GREEN

- [x] 执行：

```powershell
mvn "-Dtest=AttachmentUploadServiceTest,AttachmentRegistrationServiceTest,AttachmentUploadTransactionIntegrationTest,AdminAttachmentControllerTest,SecurityConfigTest,GlobalExceptionHandlerTest,ArchitectureRulesTest" test
```

预期：全部通过。

### Step 11：静态检查并提交

- [x] 执行：

```powershell
rg -n "MultipartFile|jakarta.servlet" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain
rg -n "@Transactional" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application/attachment/AttachmentUploadService.java
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
git diff --check
```

预期：application/domain 无 Multipart/Servlet；外层上传服务无事务；无注解 SQL。

- [x] 提交：`0ee0eee`

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/superpowers/plans/2026-06-14-backend-v2-attachment.md
git commit -m "实现附件上传与去重恢复"
```

---

## Task 5：开放附件查询并完成契约收尾

**提交信息：** `完成附件纵向切片`

### Step 1：写查询应用服务失败测试

- [x] 新建 `AttachmentQueryServiceTest.java`，覆盖：

- ADMIN/DEMO 可分页查询。
- null principal → `INVALID_TOKEN`。
- 未知角色 → `FORBIDDEN`。
- page < 1、size < 1、size > 100 → `VALIDATION_ERROR`。
- 详情 ID 非正数 → `VALIDATION_ERROR`。
- 不存在或 deleted → `NOT_FOUND`。
- application 输出使用 `AttachmentPageResult`，避免应用层依赖 `common.web`；
  Controller 再转换为 `PageResponse<AttachmentVO>`，JSON 字段为
  `records/total/page/size`。

### Step 2：写 Web、Security 和 OpenAPI 失败测试

- [x] 扩展 `AdminAttachmentControllerTest.java`：

- GET `/api/admin/attachments?page=1&size=20` 返回分页。
- GET `/api/admin/attachments/{id}` 返回详情。
- page/size 缺省分别为 1/20。
- 响应不暴露删除审计和 SDK/Path 类型。

- [x] 扩展 `SecurityConfigTest.java`：

- ADMIN/DEMO 可 GET 列表和详情。
- 匿名 GET 返回 `401 + 10002`。
- DEMO 仍不能 POST。

- [x] 新建 `AttachmentOpenApiTest.java`：

- `/api/admin/attachments` 只有 GET/POST。
- `/{id}` 只有 GET。
- POST request media type 为 multipart/form-data，file 为 binary string。
- 分页 data schema 含 `records/total/page/size`。
- Attachment schema 字段与设计一致，不含 Entity、deleted、Path、S3 类型。

### Step 3：写完整集成测试

- [x] 新建 `AttachmentIntegrationTest.java`，使用真实 Spring context、H2、JWT、
  MockMvc 和 LOCAL 临时目录，覆盖完整流程：

1. ADMIN 上传 PNG，返回 200，数据库新增一行，文件存在。
2. 同一文件再次上传，返回相同 ID，数据库和目录仍各一份。
3. 把记录手工标记 deleted，再上传同文件，恢复原 ID 且不重复写文件。
4. 删除物理文件后再上传相同 hash，返回 `500 + 99999`。
5. DEMO 可查看列表/详情但 POST 403。
6. 匿名 GET/POST 401。
7. 非图片、损坏图片、超限请求 400。
8. 列表排序和分页字段为 `records/total/page/size`。

并增加并发测试：两个线程上传相同新文件，最终数据库一行、目录一个最终对象、两个
请求返回同一 ID。若 H2 无法稳定复现唯一键时序，保留应用层 barrier 测试，并为
MySQL Testcontainers 增加条件测试；Docker 不可用时只跳过 MySQL 测试。

### Step 4：运行测试确认 RED

- [x] 执行：

```powershell
mvn "-Dtest=AttachmentQueryServiceTest,AdminAttachmentControllerTest,AttachmentOpenApiTest,AttachmentIntegrationTest,SecurityConfigTest" test
```

预期：查询服务、GET Controller 和精确 DEMO 规则尚不存在。

### Step 5：实现查询服务

- [x] `AttachmentQueryService`：

```java
public AttachmentPageResult page(
        AuthenticatedPrincipal principal,
        int page,
        int size) {
    requireReadableRole(principal);
    validatePage(page, size);
    AttachmentPage result = repository.findActivePage(page, size);
    return new AttachmentPageResult(
            result.records().stream()
                    .map(AttachmentResult::from)
                    .toList(),
            result.total(),
            result.page(),
            result.size());
}

public AttachmentResult detail(
        AuthenticatedPrincipal principal,
        long id) {
    requireReadableRole(principal);
    if (id <= 0) {
        throw new ApiException(
                ApiErrorCode.VALIDATION_ERROR,
                "附件 ID 必须为正数");
    }
    return repository.findActiveById(id)
            .map(AttachmentResult::from)
            .orElseThrow(() -> new ApiException(
                    ApiErrorCode.NOT_FOUND,
                    "附件不存在"));
}
```

只允许 ADMIN/DEMO，角色校验与站点配置服务保持一致。

### Step 6：扩展 Controller 和 Security

- [x] `AdminAttachmentController` 增加：

```java
@GetMapping
public ApiResponse<PageResponse<AttachmentVO>> page(
        @CurrentUser AuthenticatedPrincipal principal,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size) {
    AttachmentPageResult result =
            queryService.page(principal, page, size);
    return ApiResponse.ok(new PageResponse<>(
            result.records().stream().map(AttachmentVO::from).toList(),
            result.total(),
            result.page(),
            result.size()));
}

@GetMapping("/{id}")
public ApiResponse<AttachmentVO> detail(
        @CurrentUser AuthenticatedPrincipal principal,
        @PathVariable long id) {
    return ApiResponse.ok(
            AttachmentVO.from(queryService.detail(principal, id)));
}
```

- [x] 在 `SecurityConfig` 的 `/api/admin/**` ADMIN 通用规则之前增加：

```java
.requestMatchers(HttpMethod.GET, "/api/admin/attachments")
.hasAnyRole("ADMIN", "DEMO")
.requestMatchers(HttpMethod.GET, "/api/admin/attachments/*")
.hasAnyRole("ADMIN", "DEMO")
```

POST 继续落入通用 ADMIN 规则。

### Step 7：完成接口文档

- [x] 新建 `docs/project-handbook/api-contract/attachment.md`，记录：

- 三个 HTTP 接口和 ADMIN/DEMO 权限矩阵。
- 四种格式、10 MiB、尺寸/像素/GIF 帧限制。
- `records/total/page/size` 分页契约。
- active 复用、deleted 恢复、物理缺失错误。
- LOCAL/S3 配置和 Default Credentials Provider Chain。
- S3 公开读取由 Bucket Policy/CloudFront 管理，不使用对象 ACL。
- 本轮明确不提供删除。

- [x] 更新 `api-contract/README.md` 加入附件文档索引。

### Step 8：更新状态与实施证据

- [x] 更新：

- `docs/project-handbook/status.md`
- `docs/project-handbook/roadmap.md`
- `docs/project-handbook/m3-preflight-review.md`
- `docs/superpowers/specs/2026-06-14-backend-v2-attachment-design.md`
- 本计划

记录 Task 1 至 Task 4 的真实 SHA；设计状态改为“已实施”。下一步改为
`t_friend_link` 纵向切片。Task 5 自身不伪造 SHA。

### Step 9：运行全量验证

- [x] 执行：

```powershell
git diff --check
mvn clean test
```

预期：

- `BUILD SUCCESS`
- 0 failures
- 0 errors
- Docker 不可用时仅 MySQL Testcontainers 条件测试 skipped

把最终 tests/failures/errors/skipped 数量写回状态、设计和计划。

实际结果：378 tests，0 failures，0 errors，4 skipped。

### Step 10：最终静态审查

- [x] 执行：

```powershell
rg -n "@(Select|Update|Insert|Delete)" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
rg -n "MultipartFile|jakarta.servlet" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/application MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system/domain
rg -n "StaticCredentialsProvider|AwsBasicCredentials|public-read|access.?key|secret.?key" MyBlog-springboot-v2/src/main
rg -n "t_article|cover_attachment_id" MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/system
git status --short
```

预期：

- 无注解 SQL。
- application/domain 不绑定 multipart 或 servlet。
- 无静态 AWS 凭证和对象 ACL。
- system 附件实现不跨模块查询文章表。
- 提交前只有 Task 5 的查询、集成测试和文档变更。

### Step 11：提交 Task 5

- [ ] 将已完成步骤勾选后提交：

```powershell
git add MyBlog-springboot-v2/src/main MyBlog-springboot-v2/src/test docs/project-handbook docs/superpowers/specs/2026-06-14-backend-v2-attachment-design.md docs/superpowers/plans/2026-06-14-backend-v2-attachment.md
git commit -m "完成附件纵向切片"
```

### Step 12：最终确认

- [ ] 执行：

```powershell
git status --short
git log -5 --oneline
```

预期工作区干净，最近五个提交依次是：

1. `建立附件领域与持久化查询`
2. `建立附件存储抽象与本地实现`
3. `接入AWS S3附件存储`
4. `实现附件上传与去重恢复`
5. `完成附件纵向切片`

---

## 完成标准

- [x] `t_attachment` 领域、XML Mapper 和 Repository 完成。
- [x] LOCAL 与 S3 通过同一端口工作，运行时不双写。
- [x] 历史 OSS 元数据可读取，但没有 adapter 时存储操作明确失败。
- [x] 只接受 JPEG、PNG、WebP、GIF，最大 10 MiB。
- [x] 图片结构、尺寸、总像素和 GIF 帧数完成服务端校验。
- [x] ADMIN 可上传；ADMIN/DEMO 可查看列表和详情。
- [x] active hash 直接复用；deleted hash 恢复原记录。
- [x] 并发重复上传最终一行、一个有效对象、同一附件 ID。
- [x] 数据库失败补偿本次物理对象，补偿失败保留 suppressed 异常和必要日志。
- [x] 临时文件在所有路径清理。
- [x] LOCAL 无目录穿越；S3 不使用 ACL 或静态凭证。
- [x] 分页响应沿用 `records/total/page/size`。
- [x] OpenAPI 不暴露 Entity、Path、Multipart 内部类型或 AWS SDK 类型。
- [x] 本轮不查询 `t_article`，不实现删除。
- [x] 五个 Task 分别形成中文本地提交。
- [x] 全量 Maven 测试、Enforcer 和 `git diff --check` 通过。
