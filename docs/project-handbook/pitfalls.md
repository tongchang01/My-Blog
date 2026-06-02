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
- **相关**：`rules/security-baseline.md`、`decisions/0007-jwt-via-spring-security-jose.md`

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
- **相关**：`decisions/0008-hutool-scoped-usage.md`

### 🔴 R-006 不得在 Controller 直接 try-catch 业务异常后返错

- **禁止**：`try { ... } catch (Exception e) { return ApiResponse.fail(...); }`
- **正确**：抛 `ApiException(ApiErrorCode.XXX)`，由 `GlobalExceptionHandler` 统一处理
- **相关**：`rules/error-handling.md` §7

### 🔴 R-007 不得在 HTTP 200 中返回业务失败

- **禁止**：200 + `{ success:false, msg:"..." }`
- **正确**：返回对应 HTTP 状态（400/401/403/404/409/500）+ 标准 ApiResponse
- **相关**：`rules/api-response.md` §1、`rules/error-handling.md` §4

### 🔴 R-008 不得在异常消息中暴露内部细节

- **禁止**：把 SQL、堆栈、表名、调用栈直接返给客户端
- **正确**：业务异常用 ApiErrorCode 默认消息；系统异常统一 INTERNAL_ERROR
- **相关**：`rules/error-handling.md` §7

### 🔴 R-009 不得跳过 ArchUnit 测试

- **禁止**：`mvn test -Dtest='!ArchitectureRulesTest'` 或注释掉规则
- **正确**：违反规则就修代码或写 ADR 改规则
- **相关**：`decisions/0012-archunit-guards.md`

### 🔴 R-010 不得修改已 apply 的 Flyway 脚本

- **禁止**：改已经在任何环境跑过的 `V{n}__xxx.sql` 内容（checksum 会失败）
- **正确**：写新的 `V{n+1}__fix_xxx.sql` 做修补
- **相关**：`workflows/add-new-table.md`

---

## 历史踩坑（按时间追加）

> 早期重构产生的踩坑会在工程实际运行中陆续补全。当前已识别如下：

### ⚠️ P-001 JWT 撤销存储是内存实现

- **时间**：2026-04
- **现象**：登出后 `TokenRevocationStore` 仅写本进程内存
- **后果**：服务重启 → 已撤销 token 重新生效；多实例部署 → 撤销不跨实例
- **教训**：早期为快速跑通而选内存实现，必须在多实例部署前迁 Redis
- **跟进**：待新增 ADR 决定 Redis 引入时机
- **相关**：`arch/auth-flow.md` §5、`rules/security-baseline.md` §4

### ⚠️ P-002 ContentCatalogMapper 含 @Select 长查询

- **时间**：2026-05
- **现象**：`ContentCatalogMapper` 使用 `@Select` 注解承载多表 join 的长 SQL
- **根因**：迁移过程中图方便先用注解
- **教训**：复杂 SQL 必须走 XML，便于维护与调优
- **跟进**：迁移到 `src/main/resources/mapper/content/ContentCatalogMapper.xml`
- **相关**：`rules/sql-placement.md`、`decisions/0010-sql-placement-strategy.md`

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
- **相关**：`arch/auth-flow.md` §3

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
- **相关**：`rules/security-baseline.md` §6

### ⚠️ P-007 V1 Controller 大量 try-catch 后返 `success:false`

- **时间**：V1 历史
- **现象**：V1 多处 Controller 自接异常，返 200 + 自定义错误体
- **后果**：HTTP 状态与业务结果脱节，客户端难以统一处理
- **教训**：错误应通过 HTTP 状态码 + 统一异常处理表达
- **修复**：V2 强制 `GlobalExceptionHandler` 统一兜底（见 R-006、R-007）
- **相关**：`rules/error-handling.md`

### ⚠️ P-008 V1 JdbcTemplate 与 MyBatis 混用无规则

- **时间**：V1 历史
- **现象**：同一业务在不同方法用不同 ORM，风格混乱
- **教训**：必须明确主 ORM，遗留代码渐进替换
- **修复**：V2 以 MyBatis-Plus 为主，JdbcTemplate 仅过渡期，按 SOP 迁移
- **相关**：`workflows/migrate-jdbc-to-mybatis-plus.md`

---

## 未解决但已识别（需后续跟进）

> 这些是已知问题但暂未处理，新增工作前请评估是否会被影响：

### 🟡 U-001 system 模块尚未建立

- **影响**：系统配置、字典、菜单等仍散落于 V1，未迁
- **后续**：按 `workflows/add-new-module.md` 建立

### 🟡 U-002 富文本 XSS 清洗未做

- **影响**：评论、文章正文若包含富文本可能被注入
- **后续**：迁移文章模块前必做，引入 Jsoup 或 OWASP HTML Sanitizer
- **相关**：`rules/security-baseline.md` §10

### 🟡 U-003 上传文件安全（MIME、大小）未做

- **影响**：文件上传可能被滥用
- **后续**：迁移上传模块前必做
- **相关**：`rules/security-baseline.md` §10

### 🟡 U-004 登录限流 / 验证码未做

- **影响**：暴力破解风险
- **后续**：需 ADR 决定方案（基于 IP / 基于用户名 / 验证码引入哪个库）

### 🟡 U-005 IP 归属地未解析

- **影响**：审计字段 `ip_source` 空
- **后续**：引入第三方库前需评估隐私与许可证

### 🟡 U-006 CommentCommandService / AdminCommentCommandService 集成测试缺失

- **影响**：评论核心命令路径覆盖不够
- **后续**：补 `@SpringBootTest` 集成测试
- **相关**：`rules/testing-policy.md` §10

### 🟡 U-007 评论软删除 → 恢复完整链路测试缺失

- **影响**：恢复路径未端到端验证
- **后续**：补集成测试

### 🟡 U-008 Bearer Token 解析逻辑散落

- **影响**：多处重复解析 `Authorization` header
- **后续**：抽出公共工具

---

## 编号约定

- `R-NNN`：永久红线
- `P-NNN`：历史踩坑
- `U-NNN`：已识别未解决项
- 编号递增，不复用
