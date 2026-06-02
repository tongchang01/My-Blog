# ADR-0003: 每个业务模块采用四层架构

- 状态：accepted
- 日期：2026-04
- 决策者：项目负责人

## 背景

V1 是技术分层（controller/service/mapper），导致：
- 业务逻辑散落在 Service 中，无清晰领域模型
- Controller 直接操作 Mapper，绕过业务层
- 跨业务逻辑混杂，难以测试单独场景

V2 需选择适合的分层模型。

## 备选方案

- 方案 A：传统三层（controller/service/dao）
- 方案 B：DDD 完整六边形架构
- 方案 C：DDD 简化四层（web/application/domain/infrastructure）

## 决定

选 C：每个业务模块内分四层：

- `web` — Controller、入参、出参
- `application` — 用例编排、事务边界
- `domain` — 实体、值对象、领域服务、仓储接口
- `infrastructure` — 仓储实现、Mapper、外部适配

## 理由

- 比三层多一层 application，把"用例编排"与"领域模型"分开
- 比六边形简单，符合个人项目复杂度
- 领域层不依赖框架，可独立测试
- 仓储接口在 domain，实现在 infrastructure，符合依赖倒置

## 后果

正面：
- 业务边界清晰
- 可独立测试领域逻辑
- 易于后续拆分微服务

负面：
- 文件数比三层多
- 新人需要学习四层职责

## 相关

- 相关 rules：`rules/package-layout.md`
- ArchUnit 规则守护层间依赖
