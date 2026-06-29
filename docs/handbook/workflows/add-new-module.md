# 新增业务模块（SOP）

> 目标：从零搭出符合 V2 架构规范的新业务模块。
> 适用：新增如 `system`、`media` 等业务模块。

## 1. 前置条件

- 模块边界清晰，与现有模块无重叠
- 已确认模块名（小写，单数）
- 阅读：`../architecture/module-map.md`、`../rules/package-layout.md`

## 2. 步骤

### 步骤 1：建包结构

```
com.tyb.myblog.v2.{module}
├── web
│   ├── {Module}Controller.java
│   ├── request/
│   ├── response/
│   └── mapper/   ← Web 层 DTO ↔ Command/Result 映射
├── application
│   ├── {Module}ApplicationService.java
│   ├── command/
│   ├── query/
│   └── result/
├── domain
│   ├── model/   ← Entity、值对象
│   ├── service/ ← Domain Service
│   └── repository/  ← Repository 接口
└── infrastructure
    └── persistence
        ├── {Module}RepositoryImpl.java
        ├── mapper/   ← MyBatis-Plus Mapper
        └── po/       ← 持久化对象（若与 domain 分离）
```

### 步骤 2：写 Domain（先建模）

- 定义 Entity、值对象
- 定义 Repository 接口
- Domain Service 承载跨实体业务规则

### 步骤 3：写 infrastructure

- Mapper（按需 BaseMapper + XML）
- Repository 实现
- PO ↔ Entity 映射类

### 步骤 4：写 application

- ApplicationService 承载用例
- Command/Query/Result 与 web 层 DTO 分离
- 在用例方法标 `@Transactional`

### 步骤 5：写 web

- Controller、Request/Response
- `@Valid` 校验
- 返回 `ApiResponse<T>`

### 步骤 6：更新 ArchUnit 规则

🔴 必做。在 `ArchitectureRulesTest` 中加入新模块的守护规则：
- 跨模块禁止访问本模块 `infrastructure.persistence`
- 本模块 `domain` 不依赖 web/infrastructure

漏更新 = 新模块无守护。

### 步骤 7：写测试

按 `../rules/testing-policy.md` §2 必测场景列表对照。

### 步骤 8：更新文档

- `../architecture/module-map.md` — 补新模块行
- `../status.md` — 加新模块进度
- 若涉及关键决策（如选用非常规组件），写 ADR

### 步骤 9：跑 `mvn test`

确认 ArchUnit + 单元 + 集成测试全过。

## 3. 常见错误

- ❌ 漏更新 ArchUnit 规则
- ❌ Controller 直接 import 其它模块的 Mapper
- ❌ Entity 暴露在跨模块边界（应改用 application 层 DTO）
- ❌ 在 web/infrastructure 写业务规则
