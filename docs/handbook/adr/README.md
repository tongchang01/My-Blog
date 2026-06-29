# ADR — 架构决策记录

> 本目录回答："为什么是这样定的？"
> 性质：永久保留。一个决策一份文件，被取代时不删除，而是标记 superseded。

## 什么是 ADR

ADR（Architecture Decision Record）是一种轻量级文档，记录一项重要决策的：
- **背景**：当时面临什么问题、有哪些选项
- **决定**：选了哪个
- **理由**：为什么这么选
- **后果**：带来的好处与代价
- **状态**：accepted / superseded / deprecated

## 文件命名

`NNNN-短主题.md`，NNNN 是 4 位递增编号，不空号、不复用。

例：
- `0001-use-mybatis-plus.md`
- `0002-package-base-com-tyb-myblog-v2.md`
- `0003-no-default-jwt-secret.md`

## 模板

```markdown
# ADR-NNNN: 短主题

- 状态：accepted | superseded by ADR-XXXX | deprecated
- 日期：YYYY-MM-DD
- 决策者：xxx

## 背景

当时遇到的问题，涉及的约束。

## 备选方案

- 方案 A：……
- 方案 B：……
- 方案 C：……

## 决定

选 X。

## 理由

为什么选 X，关键权衡。

## 后果

正面：……
负面：……
后续需关注：……

## 相关

- 受影响的 rules：……
- 取代的 ADR：……
```

## 与其它目录的关系

- `rules/` 写"怎么做"，本目录写"为什么这么做"
- `arch/` 描述的结构由本目录的 ADR 决定
- 决策被取代时，原 ADR 状态改为 `superseded by ADR-XXXX`，文件保留

## 已有 ADR

| ADR | 主题 | 状态 |
|---|---|---|
| 0001 | 模块化单体 | accepted |
| 0002 | 包名 `com.tyb.myblog.v2` | accepted |
| 0003 | 四层架构 | accepted |
| 0004 | 6 个业务模块 | accepted |
| 0005 | MyBatis-Plus 作为主 ORM | accepted |
| 0006 | 升级到 Spring Boot 3 | accepted |
| 0007 | JWT 改用 spring-security-oauth2-jose | accepted（2026-06 补充 R6 C1 双 token 机制） |
| 0008 | Hutool 按需引入 | accepted |
| 0009 | OpenAPI 基于 springdoc + UI 使用 Knife4j 4.x | accepted（2026-06 修订，原"用 springdoc 替换 knife4j"修订为两者共存） |
| 0010 | SQL 放置策略 | accepted |
| 0011 | 中文注释 | accepted |
| 0012 | ArchUnit 守护规则 | accepted |
| 0013 | 不兼容 V1 数据结构 | accepted |
| 0014 | schema 重设计原则 | accepted（**部分被 0015 / 0018 超越**：主键 `BIGINT AUTO_INCREMENT`→`BIGINT NOT NULL` + MyBatis-Plus `ASSIGN_ID`（日志型表例外），时间类型 TIMESTAMP→DATETIME，审计列 2→8 列，软删 `deleted_at TIMESTAMP NULL` 单列→`deleted/deleted_at/deleted_by` 三件套；表命名 / 索引 / 字符集 / COMMENT / 关联表原则仍有效） |
| 0015 | 审计列规范与软删三件套 | accepted（supersede 0014 §3-§5） |
| 0016 | URL 策略——id 主导 + slug | accepted |
| 0017 | 不使用 DB FOREIGN KEY | accepted |
| 0018 | 时区统一 Asia/Tokyo（五层） | accepted |
