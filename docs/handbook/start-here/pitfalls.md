# pitfalls.md — 红线与历史踩坑

> 本文档回答："不要做什么？什么坑别再踩？"
> 性质：持续追加，**只增不删**（被废止的条目标注，不删除）。

## 使用方式

- AI 在动手前应整体扫一遍本文件
- 每次踩到新坑、或发现新红线，立即追加条目
- 条目结构：`现象` → `后果` → `禁止做法` → `正确做法` → `相关引用`

---

## 红线（永久禁止）

### 🔴 R-001 不得硬编码任何密钥

- **禁止**：在 `.java` / `.yml` / `.properties` 中写死 JWT secret、DB 密码、第三方 token
- **正确**：通过环境变量注入，且无默认值（缺失则启动失败）
- **守护**：`JwtSecretStartupValidator` 启动时校验
- **相关**：`../rules/security-baseline.md`、`../adr/0007-jwt-via-spring-security-jose.md`

### 🔴 R-002 不得修改 MyBlog-springboot/（V1）

- **原因**：V1 是历史参考（活的考古素材），已冻结；V2 全量重设计期间，V1 仍是「这个功能原来怎么做」的唯一权威来源
- **正确**：所有改动在 `MyBlog-springboot-v2/`；要对照 V1 行为只读不改

### 🔴 R-003 不得引入未授权的中间件与依赖

- **禁止**：未经 ADR 授权直接引入 Redis、RabbitMQ、Elasticsearch、Kafka 等
- **正确**：先写 ADR 说明必要性，评审通过后再引入

### 🔴 R-004 V2 业务模块之间不得跨过应用层互调基础设施

- **禁止**：comment 模块直接 new identity 模块的 Mapper / Repository
- **正确**：通过 identity 模块对外暴露的 application 接口协作
- **守护**：ArchUnit 规则 #5

### 🔴 R-005 不得用 hutool-all

- **禁止**：引入 `hutool-all` 全量包
- **正确**：按需引入 `hutool-core`、`hutool-crypto` 等具体子模块
- **相关**：`../adr/0008-hutool-scoped-usage.md`

### 🔴 R-006 不得在 Controller 直接 try-catch 业务异常后返错

- **禁止**：`try { ... } catch (Exception e) { return ApiResponse.fail(...); }`
- **正确**：抛 `ApiException(ApiErrorCode.XXX)`，由 `GlobalExceptionHandler` 统一处理
- **相关**：`../rules/error-handling.md` §7

### 🔴 R-007 不得在 HTTP 200 中返回业务失败

- **禁止**：200 + `{ success:false, msg:"..." }`
- **正确**：返回对应 HTTP 状态（400/401/403/404/409/500）+ 标准 ApiResponse
- **相关**：`../rules/api-response.md` §1、`../rules/error-handling.md` §4

### 🔴 R-008 不得在异常消息中暴露内部细节

- **禁止**：把 SQL、堆栈、表名、调用栈直接返给客户端
- **正确**：业务异常用 ApiErrorCode 默认消息；系统异常统一 INTERNAL_ERROR
- **相关**：`../rules/error-handling.md` §7

### 🔴 R-009 不得跳过 ArchUnit 测试

- **禁止**：`mvn test -Dtest='!ArchitectureRulesTest'` 或注释掉规则
- **正确**：违反规则就修代码或写 ADR 改规则
- **相关**：`../adr/0012-archunit-guards.md`

### 🔴 R-010 不得修改已 apply 的 Flyway 脚本

- **禁止**：改已经在任何环境跑过的 `V{n}__xxx.sql` 内容（checksum 会失败）
- **正确**：写新的 `V{n+1}__fix_xxx.sql` 做修补
- **相关**：`../workflows/add-new-table.md`

### 🔴 R-011 不得在业务代码直接调用 `LocalDateTime.now()` / `Instant.now()`

- **禁止**：散落使用 `LocalDateTime.now()`、`new Date()`、`System.currentTimeMillis()`
- **正确**：注入 `Clock`（`Clock.system(ZoneId.of("Asia/Tokyo"))`），通过 `LocalDateTime.now(clock)` 取时间
- **原因**：① 全站时区统一 Asia/Tokyo；② 测试可替换 `Clock.fixed(...)` 控制时间
- **相关**：`../../archive/project-handbook/product/decisions-draft.md` R7 D11

### 🔴 R-012 不得给业务表加 DB FOREIGN KEY 约束

- **禁止**：DDL 出现 `FOREIGN KEY (xxx_id) REFERENCES ...`
- **正确**：只建普通索引 `KEY idx_xxx (xxx_id)`，引用完整性由 application 层维护
- **原因**：阿里规范推荐；改表 / 模块迁移 / 软删语义灵活
- **相关**：`../../archive/project-handbook/product/decisions-draft.md` R7 D2

### 🔴 R-013 不得直接渲染评论 `content_md`

- **禁止**：前台直接把 `content_md` 当 Markdown 渲染、或直接 `v-html` 渲染未清洗的内容
- **正确**：前台**只渲染** `content_html`（后端 Markdown 子集解析 + Sanitizer 清洗后产物）
- **原因**：评论是匿名写入口，原文不可信，渲染前必须经服务端清洗
- **相关**：`../../archive/project-handbook/product/decisions-draft.md` R4 #12-P0

---

## 历史踩坑（按时间追加）

> 早期重构产生的踩坑会在工程实际运行中陆续补全。当前已识别如下：

### ⚠️ P-001 JWT 撤销存储是内存实现

- **时间**：2026-04
- **现象**：登出后 `TokenRevocationStore` 仅写本进程内存
- **后果**：服务重启 → 已撤销 token 重新生效；多实例部署 → 撤销不跨实例
- **教训**：早期为快速跑通而选内存实现，必须在多实例部署前迁 Redis
- **状态更新（R6 C1）**：✅ V2 已重设计为 **`token_version` + DB `t_refresh_token` 表**（不引 Redis）。access token 携带 `ver` claim，校验时比对 `t_user_auth.token_version`；改密 / 登出 / 强制下线 时递增 token_version。撤销跨重启 & 跨实例均生效，原"内存实现"问题在 V2 不再存在。
- **相关**：`../../archive/project-handbook/product/decisions-draft.md` R6 C1、`../architecture/auth-flow.md`

### ⚠️ P-002 ContentCatalogMapper 含 @Select 长查询

- **时间**：2026-05
- **现象**：`ContentCatalogMapper` 使用 `@Select` 注解承载多表 join 的长 SQL
- **根因**：迁移过程中图方便先用注解
- **教训**：复杂 SQL 必须走 XML，便于维护与调优
- **跟进**：迁移到 `src/main/resources/mapper/content/ContentCatalogMapper.xml`
- **相关**：`../rules/sql-placement.md`、`../adr/0010-sql-placement-strategy.md`

### ⚠️ P-003 V1 硬编码 JWT issuer "huaweimian"

- **时间**：V1 历史
- **现象**：V1 `TokenServiceImpl` 中 JWT issuer 直接写死字符串
- **根因**：原作者未抽配置
- **教训**：所有签发标识必须可配
- **修复**：V2 通过 `myblog.security.jwt.*` 配置统一管理
- **相关**：`v1-vs-v2.md`

### ⚠️ P-004 V1 JWT 不含 exp claim

- **时间**：V1 历史
- **现象**：V1 JWT 不带过期时间，依赖 Redis key 过期判失效
- **后果**：Redis 不可用 → token 等同永久有效
- **教训**：JWT 必须自包含过期信息，外部存储仅做撤销不做主校验
- **修复**：V2 token 含标准 `exp` claim
- **相关**：`../architecture/auth-flow.md` §3

### ⚠️ P-005 V1 fastjson 1.2.76 已有多个 CVE

- **时间**：V1 历史
- **现象**：V1 仍在使用 fastjson 1.2.76
- **后果**：存在反序列化漏洞风险
- **教训**：依赖必须定期审计
- **修复**：V2 不引入 fastjson，使用 Jackson（Spring Boot 默认）
- **相关**：`v1-vs-v2.md`

### ⚠️ P-006 V1 安全白名单只配 path 未配 method

- **时间**：V1 历史
- **现象**：白名单只声明 path 导致 `POST` 也被匿名放行
- **后果**：本应需登录的写接口可匿名调用
- **教训**：白名单必须 method+path 双维度
- **修复**：V2 `myblog.security.public-endpoints` 强制 method+path
- **相关**：`../rules/security-baseline.md` §6

### ⚠️ P-007 V1 Controller 大量 try-catch 后返 `success:false`

- **时间**：V1 历史
- **现象**：V1 多处 Controller 自接异常，返 200 + 自定义错误体
- **后果**：HTTP 状态与业务结果脱节，客户端难以统一处理
- **教训**：错误应通过 HTTP 状态码 + 统一异常处理表达
- **修复**：V2 强制 `GlobalExceptionHandler` 统一兜底（见 R-006、R-007）
- **相关**：`../rules/error-handling.md`

### ⚠️ P-008 V1 JdbcTemplate 与 MyBatis 混用无规则

- **时间**：V1 历史
- **现象**：同一业务在不同方法用不同 ORM，风格混乱
- **教训**：必须明确主 ORM，遗留代码渐进替换
- **修复**：V2 以 MyBatis-Plus 为主，JdbcTemplate 仅过渡期，按 SOP 迁移
- **相关**：`../workflows/migrate-jdbc-to-mybatis-plus.md`

### ⚠️ P-009 V1 i18n CJK 字体混排 + 机翻质量差

- **时间**：V1 历史
- **现象**：V1 原本只有中英两语，后期加入日语后，UI 可正常切换，但中文文章正文字体变得"很奇怪"——字形看着不像中文；同时 UI 文案是机翻，日本访客读起来僵硬。
- **根因**：
  1. **字体**：全站 `font-family` 单一栈，HTML 顶层 `<html lang="ja">` 后浏览器对整页（包括中文文章）优先匹配日文字体，CJK 同字不同形的字（"骨/直/角"等）渲染成日式字形。
  2. **翻译**：UI 文案直接走 Google Translate 一把梭，无术语表无人工校对，按钮/菜单/错误提示翻得不地道，作品集场景下减分严重。
- **教训**：
  - 字体要跟"**内容的语言**"走，不是跟 UI 语言走。
  - 多语言项目从一开始就要规划字体分层 + 翻译流水线，不能事后补。
  - 机翻只能做底稿，关键文案必须人工校对，且需要术语表保证一致性。
- **修复（V2）**：
  - **字体**：HTML 分层 `lang`（`<html lang="ja">` 但 `<article lang="zh-CN">`）+ CSS `:lang()` 选择器分别配字体栈 + Noto Sans SC/JP/Latin 全家桶保底。
  - **翻译**：DeepL 打底 → 抄 Qiita / Zenn 现成术语 → 关键文案人工校对 → 维护 `../frontend/blog/i18n-glossary.md` 术语表。
- **相关**：`../../archive/project-handbook/product/decisions-draft.md` Round 2 #18 + #19

### ⚠️ P-010 GitHub runner 环境与本机不一致导致 CI 误判

- **时间**：2026-07-01
- **现象**：首次把 CI 合入 `main` 后，`Admin frontend tests` 通过，`Backend tests` 连续失败；GitHub annotations 只显示 `Process completed with exit code 1`。
- **根因**：
  1. 本机没有 Docker，`@Testcontainers(disabledWithoutDocker = true)` 的 `MySql*Test` 会跳过；GitHub runner 有 Docker，会真实执行 MySQL 专项测试，超出了当前“最小 CI”边界。
  2. 本机 JVM 默认时区是 `Asia/Tokyo`，GitHub Ubuntu runner 默认不是；`MyBlogConfigStartupValidator` 要求默认时区为 `Asia/Tokyo`，导致 Spring `ApplicationContext` 批量启动失败。
- **后果**：本机 `mvn test` 绿，不代表 GitHub runner 一定绿；runner 的 Docker、时区、系统环境差异会把隐藏假设放大成 CI 失败。
- **禁止做法**：
  - 不要只看 GitHub annotations 后盲猜失败原因。
  - 不要因为 CI 失败就顺手扩大 CI 范围或塞入 CD。
  - 不要用本机测试通过替代 runner 环境验证。
- **正确做法**：
  - 用 GitHub job logs 查完整失败日志。
  - 当前最小 CI 显式排除 `MySql*Test`，真实 MySQL 方言验证保留在阶段结束或发布前检查。
  - backend job 显式设置 `MAVEN_OPTS=-Duser.timezone=Asia/Tokyo` 和 `TZ=Asia/Tokyo`。
- **相关**：`../ops/ci-cd.md`、`../ops/ci-troubleshooting.md`

---

## 未解决但已识别（需后续跟进）

> 这些是已知问题但暂未处理，新增工作前请评估是否会被影响：

### 🟡 U-001 system 模块尚未建立

- **状态**：✅ 已在 R5 B1 模块边界中确认（system 模块承载 t_site_config / t_attachment / 字典 / 全局配置）
- **后续**：实际建模块时按 `../workflows/add-new-module.md`

### 🟡 U-002 富文本 XSS 清洗未做

- **状态**：✅ 评论已由 R4 #12-P0 决定（Markdown 子集解析 → 禁用原始 HTML → Sanitizer 白名单清洗 → 同时存 `content_md` / `content_html`，前台只渲染 `content_html`）
- **影响**：文章正文（管理员写）仍走 Markdown 渲染，作者可信度高但仍建议清洗
- **后续**：迁移文章模块前为正文渲染管线接入 Sanitizer
- **相关**：`../../archive/project-handbook/product/decisions-draft.md` R4 #12-P0、R-013

### 🟡 U-003 上传文件安全（MIME、大小）未做

- **状态**：⚠️ 表结构已在 R5 A3 定（`t_attachment` 含 `content_type` / `file_size` / `hash_sha256`），但 MIME 白名单 / 大小上限 / 病毒扫描尚未落实
- **后续**：实现 `/api/admin/attachments POST` 时落实白名单校验与大小限制
- **相关**：`../../archive/project-handbook/product/decisions-draft.md` R5 A3

### 🟡 U-004 登录限流 / 验证码未做

- **状态**：✅ 已由 R7 D6 限流策略定（登录 同 IP+同 username 5 次/10 分钟冷却，Caffeine 计数器）
- **后续**：实现 `LoginRateLimiter`
- **相关**：`../../archive/project-handbook/product/decisions-draft.md` R7 D6

### 🟡 U-005 IP 归属地未解析

- **状态**：✅ 已在 R2 #16 明确**不做**（许可证 + 数据更新成本不值；作品集场景不需要）
- **决定**：`ip_source` 字段不再保留

### 🟡 U-006 CommentCommandService / AdminCommentCommandService 集成测试缺失

- **状态**：✅ 已关闭（2026-06-17）
- **处理**：已补 `CommentIntegrationTest`，覆盖公开提交、后台 approve / hide / delete / restore、DEMO 只读和文章 `comment_count` 联动
- **原影响**：评论核心命令路径覆盖不够
- **相关**：`../rules/testing-policy.md` §10

### 🟡 U-007 评论软删除 → 恢复完整链路测试缺失

- **状态**：✅ 已关闭（2026-06-17）
- **处理**：`CommentIntegrationTest` 已端到端验证 PASS 文章评论软删除计数 -1、恢复计数 +1
- **原影响**：恢复路径未端到端验证

### 🟡 U-008 Bearer Token 解析逻辑散落

- **影响**：多处重复解析 `Authorization` header
- **后续**：抽出公共工具

---

## 编号约定

- `R-NNN`：永久红线
- `P-NNN`：历史踩坑
- `U-NNN`：已识别未解决项
- 编号递增，不复用
