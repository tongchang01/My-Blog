# 架构决策记录

> 状态：当前有效
> 适用范围：V2 关键架构决策
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/`
> 权威程度：导航与维护规则

ADR 记录仍影响当前实现的关键取舍。实现细节以代码、配置、迁移和测试为准；操作方法进入 handbook，已经完全失效的 ADR 删除并由 Git 历史保留。

## 当前决策

| 编号 | 决策 |
| --- | --- |
| 0001 | [采用模块化单体](0001-modular-monolith.md) |
| 0002 | [基础包使用 com.tyb.myblog.v2](0002-package-base-com-tyb-myblog-v2.md) |
| 0003 | [业务模块采用四层架构](0003-four-layer-architecture.md) |
| 0004 | [五个业务模块与 common 基础设施](0004-six-business-modules.md) |
| 0005 | [MyBatis-Plus 作为主要持久化框架](0005-mybatis-plus-as-primary-orm.md) |
| 0006 | [使用 Spring Boot 3.5 与 Java 17](0006-upgrade-to-spring-boot-3.md) |
| 0007 | [使用 Spring Security JOSE 与数据库会话撤销](0007-jwt-via-spring-security-jose.md) |
| 0009 | [springdoc 生成 OpenAPI，Knife4j 提供本地 UI](0009-springdoc-replaces-knife4j.md) |
| 0010 | [按复杂度分配 MyBatis SQL](0010-sql-placement-strategy.md) |
| 0011 | [代码注释统一使用中文](0011-chinese-only-comments.md) |
| 0012 | [使用 ArchUnit 守护架构边界](0012-archunit-guards.md) |
| 0013 | [V2 不兼容 V1 数据结构和接口](0013-no-v1-compatibility.md) |
| 0014 | [V2 数据库结构设计原则](0014-schema-redesign-principles.md) |
| 0015 | [统一审计列与软删除](0015-audit-columns-and-soft-delete.md) |
| 0016 | [文章 URL 由 ID 定位并附带可读 slug](0016-url-strategy-id-led.md) |
| 0017 | [不使用数据库外键约束](0017-no-db-foreign-key.md) |
| 0018 | [运行时统一使用 Asia/Tokyo](0018-timezone-asia-tokyo.md) |

0008 对应的 Hutool 引入决策已随依赖移除而删除，编号不再复用。

## 新建格式

文件名使用 `NNNN-short-topic.md`。正文至少包含：

```markdown
# ADR-NNNN：决策标题

> 状态：提议中 / 当前有效 / 已取代
> 适用范围：受影响的模块
> 最后校准：YYYY-MM-DD
> 对应代码：`path/`
> 权威程度：ADR

## 背景

## 决策

## 结果
```

创建和维护流程见 `../workflows/write-adr.md`。
