# CI 排查手册

> 状态：当前有效
> 适用范围：MyBlog V2 CI 排查
> 最后校准：2026-07-01
> 权威程度：运维手册

## 本文档回答什么问题

本文档说明 CI 失败时怎么查。项目级踩坑经验统一沉淀到 [`../start-here/pitfalls.md`](../start-here/pitfalls.md)，规则边界仍以 [CI/CD 规则](ci-cd.md) 为准。

## 2026-07-01：首次 main CI 失败

### 现象

`main` 首次推送后触发 `CI` workflow：

- `Admin frontend tests` 通过。
- `Backend tests` 失败。

GitHub Actions 页面只给出 `Process completed with exit code 1` 时，不要只看 annotations；需要读取 job logs。

### 坑 1：最小 CI 误跑真实 MySQL 专项

本地 `mvn test` 在没有 Docker 时会跳过 `@Testcontainers(disabledWithoutDocker = true)` 测试；GitHub runner 有 Docker，因此会真实执行 `MySql*Test`。

当前 CI 规则定义的是最小 CI，不包含真实 MySQL/Docker 专项，所以后端 CI 排除：

```bash
mvn -f MyBlog-springboot-v2/pom.xml test "-Dtest=!MySqlFlywayMigrationTest,!MySqlChangePasswordConcurrencyTest,!MySqlLoginFailureConcurrencyTest"
```

真实 MySQL 方言验证仍属于阶段结束或发布前检查，不从当前最小 CI 删除其必要性。

### 坑 2：GitHub runner 默认时区不是 Asia/Tokyo

后端启动校验要求 JVM 默认时区为 `Asia/Tokyo`。本机默认时区满足要求，GitHub Ubuntu runner 默认不是该时区，导致 Spring `ApplicationContext` 批量启动失败。

backend job 必须显式设置：

```yaml
env:
  MAVEN_OPTS: -Duser.timezone=Asia/Tokyo
  TZ: Asia/Tokyo
```

判断依据：

- 日志中出现大量 `ApplicationContext failure threshold`。
- `MyBlogConfigStartupValidator` 要求 `ZoneId.systemDefault()` 为 `Asia/Tokyo`。
- 加上 `MAVEN_OPTS` 后同款后端命令本地通过，GitHub CI 也通过。

## 推荐排查方式

优先使用 GitHub 插件读取 workflow job logs。它能直接拿到失败 job 的完整日志，比手动轮询网页可靠。

可用命令行环境时，安装 GitHub CLI 后使用：

```bash
gh run list --repo tongchang01/My-Blog --limit 5
gh run watch --repo tongchang01/My-Blog
gh run view --repo tongchang01/My-Blog --log-failed
```

没有 `gh` 时，可用 GitHub REST API 查询 run 和 job 状态；但只靠 REST annotations 通常不够，annotations 经常只有 `Process completed with exit code 1`。

## 不要做

- 不要因为 CI 失败就扩大 workflow 范围。
- 不要把 CD、部署密钥或 Docker 发布流程塞进当前最小 CI。
- 不要只根据本机 `mvn test` 通过判断 GitHub runner 一定通过；runner 的 Docker、时区和系统环境可能不同。
