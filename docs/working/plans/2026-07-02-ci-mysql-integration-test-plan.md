# CI 补齐 MySQL 集成测试实施思路

> 状态：方案已定 / 实现待落地。本文记录 CI 补回 `MySql*Test` 的边界与顺序。

## 背景

当前 [.github/workflows/ci.yml](../../../.github/workflows/ci.yml) 后端 job 通过
`-Dtest=!MySqlFlywayMigrationTest,!MySqlChangePasswordConcurrencyTest,!MySqlLoginFailureConcurrencyTest`
排除了三个真实 MySQL 集成测试（提交 `3f53271`）。

排除原因：干净 runner 上首次全量跑 `mvn test` 时相关测试爆红，未及时定位就先缩窄了范围。
副作用：数据库事务隔离、行锁、并发条件更新等最容易在线上出事故的逻辑没有自动化保护。

## 目标

把三个 `MySql*Test` 加回 CI，同时不拖慢主 job 的快速反馈、不让 flaky 训练团队忽略红灯。

## 方案总览

CI 拆成两个 job，不合并成一个、也不细拆到三个：

| Job | 覆盖范围 | 预期时长 | required |
|-----|----------|----------|----------|
| `backend-test` | H2 + 纯单元测试（现状） | ~3 min | 是 |
| `backend-mysql-test` | 三个 `MySql*Test`（新增） | 5-8 min | **观察期先设为 non-required** |

两个 job 并行，`backend-mysql-test` 不 `needs` 前者。

### 为什么不再拆成三个 job

- `MySqlFlywayMigrationTest` 只跑 Flyway 迁移，本身不到 1 分钟；单开一个 job 的固定开销
  （checkout + setup-java + maven cache restore ≈ 30-60s，加 runner 排队）比测试本身还大。
- 三个 job 会分别拉 `mysql:8.4`，重复消耗 Docker Hub 匿名限流额度，触发 429 概率反而上升。
- 拆分时机等到"MySQL 相关测试超过 15 min"或"某个子类频繁 flaky 且需要独立 rerun"时再做。

## `backend-mysql-test` 具体设计

### Job 骨架

- `runs-on: ubuntu-latest`
- `timeout-minutes: 15`（硬上限，防止挂死占额度）
- 观察期不设 `continue-on-error`；失败应显示红灯，但暂不加入分支保护 required checks。
- 环境变量：
  - `TZ: Asia/Tokyo`
  - `MAVEN_OPTS: -Duser.timezone=Asia/Tokyo`

### 步骤顺序

1. `actions/checkout@v4`
2. `actions/setup-java@v4`（Temurin 17 + maven cache）
3. **Pre-pull 镜像**：`docker pull mysql:8.4`
   - 作用：把网络问题和测试失败分离，pre-pull 挂了就是镜像/网络问题，一眼可辨。
4. `mvn -f MyBlog-springboot-v2/pom.xml test -Dtest=MySqlFlywayMigrationTest,MySqlChangePasswordConcurrencyTest,MySqlLoginFailureConcurrencyTest`

### 不做的事

- 不引入 Docker 层缓存 action（复杂度/收益不划算，先看 pre-pull 是否够）。
- 不引入 Docker Hub 登录（先看匿名拉取的稳定性，rate limit 真出问题再加）。
- **不改 test 代码**：暂不抽 `AbstractMySqlIntegrationTest` 共享容器和共享 Spring 上下文；
  三个测试各自独立、状态清晰，动它们容易引入新耦合。8 分钟以内的 job 时长可以接受，
  等观察期确认成为瓶颈再做。

## 文档同步

在 [docs/handbook/ops/ci-cd.md](../../handbook/ops/ci-cd.md) 做以下改动：

1. "当前最小检查" 表格新增 `backend-mysql-test` 行，标注 **non-required / 观察期**。
2. 删掉 "暂不跑 MySql*Test 真实 MySQL 专项" 这句。
3. 新增 **MySQL 测试 rerun 规则** 小节：
   - 只允许在以下情况 rerun：pre-pull 步骤失败、Testcontainers 启动超时
     （日志含 `Timed out waiting for container port`）。
   - **业务断言失败必须查代码，不允许直接 rerun 掩盖。**
4. 记录观察期终止条件：**连续两周 flaky ≤ 1 次** → 改为 required 并在分支保护里勾选。

## 落地顺序

按 [AGENTS.md](../../../AGENTS.md) 的拆分规则，分成两个独立提交：

1. **提交 A：`工程：新增CI MySQL集成测试job`**
   - 只改 `.github/workflows/ci.yml`
   - 验证方式：推分支后触发一次 CI，看 `backend-mysql-test` 是否跑绿；失败时根据步骤 3 的输出区分镜像/超时/断言。
2. **提交 B：`文档：CI补齐MySQL测试规则说明`**
   - 只改 `docs/handbook/ops/ci-cd.md`
   - 在 A 跑通之后再提，避免文档描述与 CI 实际状态错位。

## 转正条件（观察期结束后）

同时满足：

- 连续 14 天内 `backend-mysql-test` flaky ≤ 1 次
- 期间没有因基础设施波动被迫 rerun 超过 2 次

满足后再同步分支保护规则、更新 handbook。

## 风险与放弃条件

- 若两周内 flaky 超过 3 次且都归因于基础设施（非业务），先考虑：
  1. 加 Docker Hub 登录，把匿名 100/6h 提到 200/6h
  2. 抽共享容器 + 共享上下文（前面第 3 步"不做的事"里推迟的那项）
- 若上述都不能压住 flaky，回退到当前排除方案，并在 handbook 记录"CI 真实 MySQL 测试暂缓，
  依赖发布前手工验证"，不允许再次静默重试。
