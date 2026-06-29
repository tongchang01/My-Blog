# 构建与测试（SOP）

> 目标：本地与 CI 的标准构建/测试命令。

## 1. 环境准备

- JDK 17（Enforcer 拒绝其它大版本）
- Maven 3.9.x（Enforcer 暂不接受 Maven 4）
- 本地 MySQL 8.x（启动 `local` profile 时需要）
- Docker（可选；用于 Testcontainers MySQL 方言验证，未启动时对应测试自动跳过）
- 环境变量：
  - `MYBLOG_JWT_SECRET`（≥32 字节）— 启动 `local` / `prod` 必需；`test` profile 使用测试专用值
  - `MYBLOG_DATASOURCE_URL` — `prod` 必需；`local` 默认连接本机 `myblog_v2_dev`
  - `MYBLOG_DATASOURCE_USERNAME` / `MYBLOG_DATASOURCE_PASSWORD` — `local` / `prod` 必需
  - `MYBLOG_CORS_ALLOWED_ORIGINS`（生产环境按需设置，多个来源用逗号分隔）
  - `MYBLOG_WEB_TRUSTED_PROXIES`（使用反向代理时设置；Spring Boot 直接绑定到 `myblog.web.trusted-proxies`，多个值用逗号分隔）

## 2. 常用命令

```bash
# 进入 V2 工程根
cd MyBlog-springboot-v2

# 干净编译
mvn clean compile

# 只检查 Java / Maven 基线与依赖收敛
mvn validate

# 跑全部测试（含 ArchUnit + Flyway 迁移验证）
mvn test

# 只跑架构守护
mvn test -Dtest=ArchitectureRulesTest

# 只跑迁移验证
mvn test -Dtest=FlywayMigrationTest

# 只跑真实 MySQL 迁移验证（需要 Docker）
mvn test -Dtest=MySqlFlywayMigrationTest

# 只跑某个模块测试
mvn test -Dtest='com.tyb.myblog.v2.comment.**'

# 跳过测试打包（不推荐，仅本地速跑）
mvn package -DskipTests

# 正常打包（含测试）
mvn package

# 本地起服务（local profile）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 生产配置基线（必须显式激活 prod）
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## 3. PR / 提交前 Checklist

- [ ] 当前提交只完成一个明确目的，没有混入下一阶段或无关模块
- [ ] 已检查 `git status --short` 和 `git diff --stat`
- [ ] 变更文件过多时已继续拆分；不可再拆的批量机械变更已提前说明
- [ ] Git 提交信息使用中文，并准确描述本次提交
- [ ] `mvn clean test` 全过
- [ ] ArchitectureRulesTest 通过
- [ ] 新增/修改的逻辑有对应测试
- [ ] 注释符合 `../rules/comment-style.md`
- [ ] 新增 API 已加 Swagger 注解
- [ ] 涉及配置变更已在 `../architecture/` 或 `../rules/` 同步
- [ ] 新增第三方依赖有当前任务的实际使用点，没有仅为未来可能需求提前占位

部署或发布前还必须执行 [发布检查清单](release-checklist.md)；PR 测试通过不能替代
CORS、反向代理路径和客户端 IP 解析验证。

## 4. 测试 Profile 行为

| Profile | 数据库 | Flyway | API 文档 |
|---------|--------|--------|----------|
| `local` | V2 开发 MySQL | **关闭** | 开启 |
| `test`  | 默认 H2 内存；专项测试使用 Testcontainers MySQL 8.4 | **启动时执行** | 开启 |
| `prod`  | 生产 MySQL | **启动时执行** | 关闭 |

常规集成测试默认走 `test` profile 和 H2。`MySqlFlywayMigrationTest` 使用 Testcontainers MySQL 8.4 补充真实方言验证；Docker 不可用时该测试自动跳过，不阻塞常规构建。

应用不设置默认 profile。`local` / `prod` 都必须显式激活；数据库账号、密码和 JWT 密钥没有代码默认值，缺失时启动失败。

## 5. 常见构建报错

| 报错 | 可能原因 |
|------|----------|
| `JWT 密钥不能为空` | 未配置 `MYBLOG_JWT_SECRET` |
| `Failed to configure a DataSource` / `MYBLOG_DATASOURCE_URL` | 未配置数据库环境变量或未显式激活 profile |
| Maven Enforcer 失败 | Java / Maven 版本不符合基线，或依赖树出现版本分叉 |
| ArchitectureRulesTest 失败 | 新写代码违反层依赖规则 |
| Flyway checksum mismatch | 已执行的迁移脚本被改动（不允许）|
| H2 Syntax error | 写了 MySQL 专属语法，H2 不识别 |
| Testcontainers 提示找不到 Docker | Docker 未启动；MySQL 专项测试会跳过，需真实方言验证时启动 Docker 后重跑 |

## 6. 不要做

- ❌ `mvn test -DskipTests` 用于 PR 前自检
- ❌ `mvn package -DskipTests` 后误以为构建成功
- ❌ 在本地 local profile 改 Flyway 脚本然后直接连开发库
