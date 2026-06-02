# 项目概览

> 本文档回答："这是个什么项目？技术栈是什么？目录怎么布局？怎么跑起来？"
> 适用范围：所有进入仓库的人或 AI。

## 项目简介

MyBlog 是一个个人博客系统，包含三块代码：

| 目录 | 角色 | 状态 |
|------|------|------|
| `MyBlog-springboot/` | 后端 V1（原始版本） | 🔒 只读，历史参考 |
| `MyBlog-springboot-v2/` | 后端 V2（重构中） | 🚧 主要工作区 |
| `MyBlog-vue/` | 前台和后台 V1 前端 | 🔒 只读，后续按 V2 规格重构 |

V2 总目标包含前台、后台、Java 后端和数据库。当前执行重点是后端基盘与业务规格，前端详细规格在 `frontend-user/`、`frontend-admin/` 中逐步补齐。

## 技术栈（V2）

> 详细决策原因见 `decisions/`，本节只列"是什么"

- **语言/运行时**：Java 17
- **框架**：Spring Boot 3.5.x
- **持久层**：MyBatis-Plus（目标）+ JdbcTemplate（遗留，逐步迁移）
- **数据库**：MySQL
- **认证**：JWT
- **工具库**：Hutool（按需引入，不允许 `hutool-all`）
- **测试**：JUnit + ArchUnit（架构守护）
- **构建**：Maven

## V2 包结构（顶层）

基础包：`com.tyb.myblog.v2`

六大模块：

| 模块 | 职责 |
|------|------|
| `common` | 跨模块通用工具、基础类 |
| `infrastructure` | 跨模块基础设施（DB、缓存、消息等） |
| `identity` | 用户、角色、权限、登录、审计 |
| `content` | 文章、分类、标签 |
| `comment` | 评论、审核 |
| `system` | 系统管理、菜单、配置 |

每个**业务模块**内部分层：

```
<module>/
├── web/             控制器、请求/响应模型
├── application/     应用服务（编排）、命令/查询
├── domain/          领域模型、领域服务（不依赖框架）
└── infrastructure/  持久化实现、外部集成
```

详见 `arch/module-map.md`。

## 目录布局（V2 工程内）

```
MyBlog-springboot-v2/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/tyb/myblog/v2/
    │   └── resources/
    │       ├── application.yml
    │       └── mapper/         MyBatis XML
    └── test/
```

## 构建与运行

```bash
cd MyBlog-springboot-v2

mvn clean compile          # 编译
mvn test                   # 跑测试（含 ArchUnit 守护）
mvn spring-boot:run -Dspring-boot.run.profiles=local   # 本地启动
mvn clean package          # 打包
```

## 环境变量

非 local/test 环境启动前必须设置（缺失会启动失败）：

- `MYBLOG_JWT_SECRET` — JWT 签名密钥

完整环境变量清单见 `rules/security-baseline.md`。

## 文档导航

- AI 工作入口：`CLAUDE.md`
- 文档索引：`INDEX.md`
- 历史执行计划：`../superpowers/plans/`
