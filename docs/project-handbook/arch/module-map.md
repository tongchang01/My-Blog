# 模块地图

> 本文档回答："V2 现在有哪些模块？模块之间能怎么调？谁守护规则？"
> 适用范围：V2 当前实现。
> 相关 ADR：ADR-0001、ADR-0003、ADR-0004、ADR-0012

## 1. 模块总览

```
com.tyb.myblog.v2
├── common              基础设施（响应、异常、安全工具、Web 工具）
├── infrastructure      全局基础设施（MyBatis-Plus / Flyway 配置）
├── identity            用户、角色、登录、JWT
├── content             文章、分类、标签
├── comment             评论、审核、举报
└── system              ⏳ 系统配置、字典、菜单（尚未创建）
```

| 模块 | 文件数（含测试） | 状态 |
|------|------------------|------|
| common | 25 | ✅ 稳定 |
| infrastructure | 2 | ✅ 仅含配置 |
| identity | 25 | ✅ 已实现 |
| content | 34 | ✅ 已实现（有遗留 @Select 待迁移） |
| comment | 34 | ✅ 已实现 |
| system | — | ⏳ 尚未创建 |

## 2. 业务模块四层结构

每个业务模块（identity/content/comment/system）内部固定四层：

```
{module}
├── web                Controller、入参 Request、出参 Response、Mapper 转换
├── application        ApplicationService（用例编排）、Command/Query/Result
├── domain             Entity、值对象、Domain Service、Repository 接口
└── infrastructure
    └── persistence    Repository 实现、MyBatis-Plus Mapper、PO（如有分离）
```

## 3. 层间依赖方向

```
  web ──► application ──► domain
                 │              ▲
                 └──────────────┤
                                │
       infrastructure ─────────┘ (实现 domain.repository)
```

- `domain` 是核心，不依赖任何其它层
- `application` 仅依赖 `domain`
- `web` 依赖 `application`，不直接访问 `infrastructure`
- `infrastructure` 实现 `domain` 中的仓储接口

## 4. 跨模块依赖

| 调用方 | 被调方 | 允许方式 |
|--------|--------|----------|
| `comment` | `identity` | 通过 `identity.application` 公开接口（如获取用户名） |
| `comment` | `content` | 通过 `content.application` 公开接口（如校验文章存在） |
| 任意业务模块 | `common` / `infrastructure` | 允许直接依赖 |
| 业务模块 A | 业务模块 B 的 `infrastructure.persistence` | 🔴 禁止 |
| 业务模块 A | 业务模块 B 的 `domain` 内部实体 | 🔴 禁止 |

## 5. ArchUnit 守护规则

位置：`src/test/java/com/tyb/myblog/v2/ArchitectureRulesTest.java`

| # | 规则 | 含义 |
|---|------|------|
| 1 | `..domain..` 不依赖 `..web..` / `..infrastructure..` | 领域层保持纯净 |
| 2 | `..web..` 不访问 `..infrastructure.persistence.mapper..` | Controller 不能直连 Mapper |
| 3 | `..application..` 不直接访问 MyBatis-Plus Mapper | 应用层通过仓储抽象 |
| 4 | `..common..` 不依赖业务模块 | 公共层不能反向依赖 |
| 5 | 业务模块不互相访问对方 `infrastructure.persistence` | 跨模块只能走 application 接口 |

任何违反 → `mvn test` 失败。

## 6. 已知遗留

- `content` 模块的 `ContentCatalogMapper` 存在 @Select 长查询，需迁 XML（见 `pitfalls.md`）
- `system` 模块尚未建立，旧库相关表的迁移工作待启动

## 7. 相关文档

- ADR：`../decisions/0001-modular-monolith.md`、`../decisions/0003-four-layer-architecture.md`、`../decisions/0004-six-business-modules.md`、`../decisions/0012-archunit-guards.md`
- 规则：`../rules/package-layout.md`
