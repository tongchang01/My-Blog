# 构建与测试（SOP）

> 目标：本地与 CI 的标准构建/测试命令。

## 1. 环境准备

- JDK 17+（必须）
- Maven 3.8+
- 本地 MySQL（开发用，可选；测试不需要）
- 环境变量：
  - `MYBLOG_JWT_SECRET`（≥32 字节）— 启动必需
  - `MYBLOG_DB_URL` / `MYBLOG_DB_USERNAME` / `MYBLOG_DB_PASSWORD`（开发/生产）

## 2. 常用命令

```bash
# 进入 V2 工程根
cd MyBlog-springboot-v2

# 干净编译
mvn clean compile

# 跑全部测试（含 ArchUnit + Flyway 迁移验证）
mvn test

# 只跑架构守护
mvn test -Dtest=ArchitectureRulesTest

# 只跑迁移验证
mvn test -Dtest=FlywayMigrationTest

# 只跑某个模块测试
mvn test -Dtest='com.tyb.myblog.v2.comment.**'

# 跳过测试打包（不推荐，仅本地速跑）
mvn package -DskipTests

# 正常打包（含测试）
mvn package

# 本地起服务（local profile）
mvn spring-boot:run -Dspring-boot.run.profiles=local
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
- [ ] 涉及配置变更已在 `../arch/` 或 `../rules/` 同步
- [ ] 新增第三方依赖有当前任务的实际使用点，没有仅为未来可能需求提前占位

## 4. 测试 Profile 行为

| Profile | 数据库 | Flyway |
|---------|--------|--------|
| `local` | 真实 MySQL | **关闭** |
| `test`  | H2 内存 | **启动时执行** |

集成测试默认走 `test` profile，强制 H2。**不**用真实 MySQL 跑自动化测试。

## 5. 常见构建报错

| 报错 | 可能原因 |
|------|----------|
| `MYBLOG_JWT_SECRET must be set` | 未配置环境变量 |
| ArchitectureRulesTest 失败 | 新写代码违反层依赖规则 |
| Flyway checksum mismatch | 已执行的迁移脚本被改动（不允许）|
| H2 Syntax error | 写了 MySQL 专属语法，H2 不识别 |

## 6. 不要做

- ❌ `mvn test -DskipTests` 用于 PR 前自检
- ❌ `mvn package -DskipTests` 后误以为构建成功
- ❌ 在本地 local profile 改 Flyway 脚本然后直接连开发库
