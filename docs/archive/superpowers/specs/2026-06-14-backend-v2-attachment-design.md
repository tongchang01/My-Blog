# Backend V2 附件纵向切片设计

> **状态：已实施（2026-06-14）**

实施提交：

1. `effb3be` 建立附件领域与持久化查询
2. `231c265` 建立附件存储抽象与本地实现
3. `4311f07` 接入AWS S3附件存储
4. `0ee0eee` 实现附件上传与去重恢复
5. Task 5 由本轮最终提交承载，提交前不预写 SHA

最终验证：`mvn clean test` 通过，378 tests，0 failures，0 errors，4 skipped；
跳过项均为 Docker 不可用时的既有 Testcontainers MySQL 条件测试。

## 1. 目标

建立 `system` 模块的第二个完整纵向切片，围绕 `t_attachment` 提供：

- ADMIN 上传图片附件。
- ADMIN、DEMO 分页查看附件列表。
- ADMIN、DEMO 查看附件详情。
- 相同内容去重复用，命中软删除记录时恢复原记录。
- LOCAL 与 AWS S3 两种存储实现，通过配置选择新文件的上传目标。

本轮不实现附件删除、回收站、物理清理任务、引用审计任务、前端上传组件、
文章封面选择器、正文编辑器接入、S3 预签名上传或 CDN 缓存刷新。

附件删除依赖 content 模块提供文章封面结构化引用查询能力。content 尚未建立，
因此本轮不允许 system 直接查询 `t_article`，避免破坏模块边界。

## 2. 已确认决策

### 2.1 文件范围

V2 起点只接受图片：

| 实际格式 | MIME | 扩展名 |
|---|---|---|
| JPEG | `image/jpeg` | `.jpg` |
| PNG | `image/png` | `.png` |
| WebP | `image/webp` | `.webp` |
| GIF | `image/gif` | `.gif` |

- 单文件最大 `10 MiB`，即 `10 * 1024 * 1024` 字节。
- 空文件拒绝。
- 不接受 PDF、SVG、压缩包、视频或任意二进制文件。
- 不信任 multipart 声明的 Content-Type 和原始文件扩展名。
- 后端根据文件签名和可解码的图片结构识别实际格式。
- 图片必须能读取正数宽高；格式签名正确但图片结构损坏仍拒绝。
- 单边尺寸最大 20,000 像素，总像素最大 40,000,000，防止小文件解压为超大
  位图造成内存耗尽。
- 原始文件名只作为展示元数据保存，不参与路径生成。

SVG 本轮明确不支持，避免脚本、外链和 XML 实体带来的额外安全面。

### 2.2 接口范围

上传：

```http
POST /api/admin/attachments
Authorization: Bearer <access-token>
Content-Type: multipart/form-data

file=<binary>
```

列表：

```http
GET /api/admin/attachments?page=1&size=20
Authorization: Bearer <access-token>
```

详情：

```http
GET /api/admin/attachments/{id}
Authorization: Bearer <access-token>
```

权限语义：

- ADMIN 可以上传、查看列表和详情。
- DEMO 可以查看列表和详情，不能上传。
- 匿名请求返回 `401 + 10002`。
- DEMO 上传返回 `403 + 10003`。
- 上传不限流。

列表只返回 `deleted=0` 的附件，按 `created_at DESC, id DESC` 排序。分页从
1 开始，默认 `size=20`，最大 `size=100`。

### 2.3 响应字段

上传、详情和列表项共用公开给后台的附件结果：

```json
{
  "id": 123,
  "storageType": "S3",
  "bucket": "myblog-assets",
  "objectKey": "attachments/2026/06/uuid.webp",
  "publicUrl": "https://static.example.com/attachments/2026/06/uuid.webp",
  "contentType": "image/webp",
  "fileSize": 102400,
  "width": 1600,
  "height": 900,
  "originalFilename": "cover.webp",
  "hashSha256": "64位小写十六进制",
  "createdAt": "2026-06-14T12:00:00",
  "createdBy": 1001
}
```

后台需要使用 `id` 作为未来文章封面逻辑引用，并使用 `publicUrl` 插入 Markdown。
不返回 `updatedAt`、`updatedBy`、`deleted`、`deletedAt`、`deletedBy`。

SHA-256 不属于敏感数据，保留在后台响应中用于识别重复文件和排查存储问题。

## 3. 存储抽象

### 3.1 边界

存储能力归 `common-infra`，附件业务归 `system`：

```text
system.application
    -> common.storage.StorageService

common.storage
    <- LocalStorageService
    <- S3StorageService
```

`system` 不依赖 AWS SDK、`java.nio.file` 的具体写入实现或 Spring 静态资源处理器。

存储端口固定为：

```java
public interface StorageService {
    StorageType type();
    StoredObject store(StoreObjectCommand command);
    boolean exists(String bucket, String objectKey);
    void delete(String bucket, String objectKey);
}
```

`StorageType` 定义在 `common.storage`，固定为 `LOCAL`、`S3`、`OSS`。附件领域
引用该稳定枚举，`common` 不得反向依赖 `system`。本轮只实现 `LOCAL`、`S3`
adapter；历史 `OSS` 记录可以读取和展示，但需要执行 `exists/delete` 时明确失败，
不能错误路由到其它后端。

再由 `StorageServiceRegistry`：

- 根据 `myblog.storage.type` 选择新上传的目标实现。
- 根据数据库中的 `storageType` 路由已有对象的后续操作。
- 未注册的存储类型在启动或调用时安全失败。
- 当前上传目标的配置必须完整；非当前后端只有在配置完整时才注册，因此迁移期可同时
  保留 LOCAL 与 S3 的读取、补偿能力，但不要求所有环境配置两个后端。

本轮虽然不开放删除接口，`delete` 仍用于数据库写入失败后的物理文件补偿。

### 3.2 对象键

LOCAL 与 S3 使用同一种对象键规则：

```text
attachments/yyyy/MM/{UUID}.{actualExtension}
```

- UUID 由服务端生成。
- 扩展名来自实际识别格式。
- 路径使用 `/`，禁止原始文件名进入对象键。
- LOCAL 实现解析后的绝对路径必须仍位于配置根目录内，阻止目录穿越。

随机对象键使并发重复上传产生的临时对象彼此独立。数据库唯一键竞争失败的一方
可以安全删除自己的对象，不会误删获胜请求使用的对象。

## 4. LOCAL 存储

LOCAL 是开发、测试和单机部署均可使用的正式实现，不是仅供测试的假实现。

配置：

```yaml
myblog:
  storage:
    type: LOCAL
    max-file-size: 10MB
    local:
      root: ${MYBLOG_STORAGE_LOCAL_ROOT}
      bucket-alias: local
      public-base-url: ${MYBLOG_STORAGE_LOCAL_PUBLIC_BASE_URL}
```

行为：

- 启动时校验 root、bucket alias 和 public base URL。
- root 不存在时创建；不可创建或不可写时启动失败。
- 文件先写入目标目录下的临时文件，完成后原子移动到最终路径。
- `publicUrl = normalizedPublicBaseUrl + "/" + objectKey`。
- LOCAL profile 注册只读静态资源映射，例如 `/media/**` 指向配置 root。
- 对外地址推荐配置为 `http://localhost:8080/media`；生产单机部署可改为 Nginx
  暴露的固定地址。
- 只允许 GET/HEAD 读取，不提供目录列表和写接口。

测试使用 JUnit 临时目录，不写仓库或用户固定目录。

## 5. AWS S3 存储

生产可配置为 AWS S3：

```yaml
myblog:
  storage:
    type: S3
    max-file-size: 10MB
    s3:
      region: ${MYBLOG_STORAGE_S3_REGION}
      bucket: ${MYBLOG_STORAGE_S3_BUCKET}
      public-base-url: ${MYBLOG_STORAGE_S3_PUBLIC_BASE_URL}
```

实现约束：

- 使用 AWS SDK for Java 2.x 的 S3 Client。
- 凭证使用 AWS Default Credentials Provider Chain，不在项目配置中保存 access key
  或 secret key。
- 上传时设置实际 `Content-Type` 和精确 `Content-Length`。
- 不设置对象级 `public-read` ACL。
- 读取权限由 S3 Bucket Policy 或 CloudFront 统一管理。
- `public-base-url` 用于生成对外地址，可配置为 S3 公开域名或 CloudFront 域名。
- SDK 异常只记录 storage type、bucket、object key 和 AWS request id，不记录文件
  内容、认证信息或完整凭证。

本轮测试不连接真实 AWS。S3 adapter 使用 mock S3 Client 验证请求参数、异常映射和
对象地址生成；真实 S3 冒烟验证留给部署环境。

## 6. 配置与依赖

新增 `StorageProperties` 并在启动时执行条件校验：

- `type=LOCAL` 时 LOCAL 配置必填，S3 配置可缺省。
- `type=S3` 时 region、bucket、public-base-url 必填。
- `max-file-size` 必须大于 0 且不得超过本轮固定安全上限 10 MiB。
- 所有 public base URL 必须是绝对 HTTP/HTTPS URL，统一去除末尾 `/`。

Spring multipart 限制同步设置：

```yaml
spring.servlet.multipart.max-file-size: 10MB
spring.servlet.multipart.max-request-size: 11MB
```

请求上限为 multipart 边界和字段元数据预留空间。应用层仍按 `10 MiB` 检查实际
文件字节数，不能只依赖容器限制。

新增依赖固定为：

- 使用 AWS SDK v2 BOM `2.46.9`，只引入 `software.amazon.awssdk:s3`。
- 使用 TwelveMonkeys `com.twelvemonkeys.imageio:imageio-webp:3.13.1`。
- 通过 Maven Enforcer dependency convergence。
- 只引入 S3 所需模块，不引入 AWS SDK 全量包。

图片识别使用独立 `ImageInspector`：

- JPEG、PNG、GIF 使用 JDK 标准 ImageIO reader。
- WebP 由 TwelveMonkeys ImageIO 插件提供 reader。
- 先读取 reader 格式名和尺寸，执行尺寸与总像素限制，再完整解码首帧验证结构。
- GIF 读取帧数并逐帧验证尺寸，最多接受 500 帧。
- 禁止仅凭文件后缀或客户端 MIME 判断。

依赖依据：

- TwelveMonkeys 官方格式表确认 WebP 读取支持：
  <https://github.com/haraldk/TwelveMonkeys>
- AWS S3 Java 2.x 官方指南：
  <https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_s3_code_examples.html>
- AWS 默认凭证链：
  <https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html>

## 7. 领域与持久化

### 7.1 领域模型

`Attachment` 包含：

- `id`
- `StorageType storageType`
- `bucket`
- `objectKey`
- `publicUrl`
- `contentType`
- `fileSize`
- `width`
- `height`
- `originalFilename`
- `hashSha256`
- `createdAt`
- `createdBy`

领域不持有 MultipartFile、S3 Client、Path 或 MyBatis Entity。

校验规则：

- ID 为正数。
- storage type 只接受 `LOCAL`、`S3`、`OSS`；本轮可读取历史 OSS 记录，但没有
  OSS adapter 时不能执行存储操作。
- object key、public URL、content type、hash 必填。
- hash 固定为 64 位小写十六进制。
- file size 为 `1..10 MiB`。
- width、height 必须同时存在且均为正数。
- width、height 和总像素必须满足图片安全上限。
- original filename 去除路径部分和控制字符，trim 后最长 255；空值保存 null。

### 7.2 Repository

`AttachmentRepository` 提供：

- 按 ID 查询 active 记录。
- 分页查询 active 记录与总数。
- 按 hash 查询记录，必须包含 deleted 记录。
- 插入新记录。
- 恢复 deleted 记录并写入 `updated_at/updated_by`，同时清空
  `deleted_at/deleted_by`。

所有 SQL 写在 Mapper XML，禁止注解 SQL。

恢复使用条件更新：

```sql
WHERE id = #{id}
  AND deleted = 1
```

并发恢复只有一个请求影响 1 行；其余请求重新读取 active 记录后返回。

## 8. 上传流程与事务

上传流程：

1. Web 层确认 multipart 字段存在。
2. 应用层确认 principal 为 ADMIN 且 ID 为正数。
3. 将 multipart 输入流写入受控临时文件，同时累计字节数并计算 SHA-256；读取到
   `10 MiB + 1` 时立即终止并报文件过大。
4. `ImageInspector` 识别实际格式、MIME、扩展名和宽高。
5. 将 SHA-256 转为小写十六进制。
6. 查询包含 deleted 的同 hash 记录。
7. 命中 active：确认该记录对应的物理对象仍存在；存在则直接返回，不写物理对象，
   不更新审计字段；缺失则按存储损坏处理。
8. 命中 deleted：确认物理对象仍存在，然后恢复原记录并返回原 ID/public URL。
9. 未命中：生成随机 object key，写当前配置的存储后端。
10. 在短事务中插入附件记录。
11. 插入成功后返回数据库记录。
12. 无论成功或失败，最终都清理受控临时文件。

软删除策略保证物理对象保留，因此恢复时不重新上传。如果数据库记录存在但物理
对象缺失，视为存储损坏，记录必要上下文并返回 `500 + 99999`，不静默改写对象。

### 8.1 数据库失败补偿

物理存储不是数据库事务资源。新对象写入成功后，如果数据库插入失败：

- 普通数据库异常：尽力删除本请求写入的对象，然后返回内部错误。
- hash 唯一键竞争：删除本请求的随机对象，重新读取获胜记录。
- 获胜记录 active：返回获胜记录。
- 获胜记录 deleted：按恢复流程处理。
- 补偿删除失败：记录 storage type、bucket、object key，保留原异常并返回内部错误；
  后续物理清理任务负责孤儿对象。

数据库事务边界放在独立注册服务中，确保唯一键异常回滚后，外层仍可执行存储补偿
和重新查询。

### 8.2 并发去重

`hash_sha256` 唯一键是最终并发防线。并发上传同一新文件时：

- 两个请求可能各自写入不同随机 object key。
- 只有一个数据库 INSERT 成功。
- 失败请求删除自己的物理对象并返回获胜记录。
- 最终数据库只有一行，存储只保留获胜对象。

并发上传命中 deleted 记录时，条件恢复保证只恢复一次，所有请求最终返回同一 ID。

## 9. 列表与详情

列表请求：

```http
GET /api/admin/attachments?page=1&size=20
```

响应沿用项目现有 `PageResponse` 约定：

```json
{
  "total": 1,
  "records": [],
  "page": 1,
  "size": 20
}
```

- page 小于 1、size 小于 1 或大于 100 返回 `400 + 90001`。
- 只读取 active 记录。
- DEMO 与 ADMIN 看到相同附件元数据。

详情不存在或已删除返回 `404 + 90003`。

本轮不提供按文件名搜索、MIME 筛选、存储类型筛选和 deleted 回收站列表；数据量小，
先保持接口最小。

## 10. 错误处理

| 场景 | HTTP | code |
|---|---:|---|
| 空文件、超限、非白名单格式、损坏图片、非法分页 | 400 | `90001` |
| access token 缺失或失效 | 401 | `10002` |
| DEMO 上传 | 403 | `10003` |
| 附件详情不存在或已删除 | 404 | `90003` |
| 存储写入失败、物理对象缺失、数据库异常、补偿失败 | 500 | `99999` |

对客户端只返回稳定中文错误消息。日志禁止输出文件二进制、Authorization、AWS
凭证和 multipart 完整请求。

## 11. 安全

- MIME 以服务端识别结果为准。
- 不接受 SVG。
- 对图片执行完整结构解码，不能只检查前几个 magic bytes。
- object key 不使用用户输入。
- LOCAL 路径解析后检查仍位于 root 下。
- S3 不使用对象 ACL。
- 原始文件名只做展示，并清除路径和控制字符。
- multipart 容器限制和应用字节限制同时存在。
- 上传接口仅 ADMIN，不因 DEMO 的 GET 规则放开其它 HTTP 方法。
- 文件公开 URL 只用于读取，上传仍必须经过认证 API。

本轮不做病毒扫描。只允许四种可解码图片格式后，风险面已显著收敛；如未来开放
PDF、Office 或压缩包，必须另做设计并引入病毒扫描。

## 12. 架构

新增文件遵循已有四层结构：

```text
system.web
    -> system.application
        -> system.domain
        -> common.storage port
system.infrastructure
    -> system.domain

common.storage
    <- local adapter
    <- s3 adapter
```

跨模块规则：

- content 后续只依赖 system application 暴露的附件存在性查询能力。
- system 删除附件时只通过 content application 端口查询封面引用。
- system 不访问 content Mapper、Entity 或数据库表。
- common 不依赖 system 领域对象。

## 13. 测试策略

### 13.1 图片与领域

- 四种允许格式识别、MIME、扩展名和宽高。
- 文件签名伪造、截断图片、空文件和超限拒绝。
- SHA-256 稳定且为小写十六进制。
- 原始文件名清洗和领域边界校验。

### 13.2 存储

- LOCAL 使用临时目录验证原子写入、exists、delete、路径越界防护和 public URL。
- S3 使用 mock Client 验证 bucket、key、Content-Type、Content-Length、无 ACL 和异常
  映射。
- Registry 按当前配置和记录 storage type 路由。
- 不连接真实 AWS。

### 13.3 Repository 与应用

- active ID 查询、分页顺序和总数。
- hash 查询包含 deleted。
- 新记录插入、deleted 恢复和软删审计清空。
- active 去重复用不写存储。
- deleted 去重恢复不重复上传。
- 物理对象缺失返回内部错误。
- 数据库失败触发物理对象补偿。
- 并发新文件上传只保留一行和一个对象。
- 并发 deleted 恢复返回同一记录。

### 13.4 Web、Security 与 OpenAPI

- ADMIN 上传成功。
- DEMO、匿名上传分别返回 403、401。
- ADMIN、DEMO 可查看列表和详情。
- multipart 缺失、超限、格式非法返回稳定错误。
- OpenAPI 使用 `multipart/form-data` 的 binary file schema。
- OpenAPI 不暴露 Entity、Path、S3 SDK 类型和删除审计字段。
- ArchUnit 与静态 SQL 检查通过。

### 13.5 全量验证

- `mvn clean test`
- Maven Enforcer dependency convergence
- `git diff --check`
- system 模块无注解 SQL
- Docker 不可用时只允许现有 Testcontainers MySQL 条件测试 skipped

## 14. 提交拆分

实施继续保持小批次中文提交：

1. `建立附件领域与持久化查询`
   - Attachment 领域、StorageType、Repository、Entity、Mapper XML、列表和 hash 查询。
2. `建立附件存储抽象与本地实现`
   - StorageProperties、StorageService、Registry、图片识别、LOCAL adapter 和临时目录测试。
3. `接入AWS S3附件存储`
   - AWS SDK v2、S3 adapter、配置校验和 mock Client 测试。
4. `实现附件上传与去重恢复`
   - ADMIN 上传、短事务注册、唯一键竞争、补偿、恢复和 Security 边界。
5. `开放附件查询并完成契约收尾`
   - ADMIN/DEMO 列表与详情、集成测试、OpenAPI、接口文档、状态与路线图。

删除能力在 content 的封面引用查询端口完成后另建独立设计和提交，不混入本轮。

## 15. 验收标准

- LOCAL 与 AWS S3 均通过同一存储端口工作，运行时配置切换且不双写。
- 新上传使用当前配置的存储类型，已有记录保留自己的 storage type。
- 只接受 JPEG、PNG、WebP、GIF，最大 10 MiB，并以实际内容识别格式。
- ADMIN 可上传；ADMIN、DEMO 可查看；DEMO 不能上传。
- 相同 SHA-256 的 active 记录直接复用。
- 相同 SHA-256 的 deleted 记录恢复原记录且不重复上传。
- 并发重复上传最终只有一条数据库记录和一个有效物理对象。
- 数据库失败执行物理对象补偿。
- LOCAL 无目录穿越，S3 不使用 public-read ACL。
- SQL 只在 Mapper XML。
- 本轮不跨模块查询 `t_article`，不提前实现删除。
- 新代码具备必要的中文业务注释。
- 全量 Maven 测试和 `git diff --check` 通过。
