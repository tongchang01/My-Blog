# 项目概览

> 本文档回答："这是个什么项目？技术栈是什么？目录怎么布局？怎么跑起来？"
> 适用范围：所有进入仓库的人或 AI。

## 项目简介

MyBlog 是一个个人博客系统，包含三块代码：

| 目录 | 角色 | 状态 |
|------|------|------|
| `MyBlog-springboot/` | 后端 V1（原始版本） | 🔒 只读，历史参考 |
| `MyBlog-springboot-v2/` | 后端 V2（重构中） | 🚧 主要工作区 |
| `MyBlog-vue/` | 前端 | ⏸ 当前不在重构范围 |

## 技术栈（V2）

> 详细决策原因见 `decisions/`，本节只列"是什么"

- **语言/运行时**：Java 17
- **框架**：Spring Boot 3.5.14
- **持久层**：MyBatis-Plus 3.5.12 + Flyway
- **数据库**：MySQL 8（utf8mb4 + Asia/Tokyo 时区）
- **认证**：JWT（spring-security-oauth2-jose）+ 双 token（access 15min + refresh 7d）
- **API 文档**：Knife4j 4.x（基于 springdoc-openapi）
- **国际化**：vue-i18n（前端）+ DB 三语副本（业务内容）
- **邮件**：Resend HTTP API
- **限流**：Caffeine 进程内（V2 单实例）
- **工具库**：Hutool（按需引入，不允许 `hutool-all`）
- **测试**：JUnit + ArchUnit（架构守护）
- **构建**：Maven
- **时区**：Asia/Tokyo 五层一致（JVM / MySQL / Clock / API / 前端，ADR-0018）

## V2 包结构（顶层，R5 修订）

基础包：`com.tyb.myblog.v2`

六大模块（按业务边界）：

| 模块 | 职责 | 核心表 | 错误码段 |
|------|------|------|------|
| `identity` | 用户、角色、登录、JWT 签发与刷新 | t_user_auth / t_user_info / t_refresh_token | 10xxx |
| `content` | 文章、分类、标签 | t_article / t_article_tag / t_category / t_tag | 20xxx |
| `comment` | 评论、留言板（复用）、审核 | t_comment | 30xxx |
| `system` | 站点配置、附件、友链申请（**不含 V1 的字典 / 菜单 / 操作日志**） | t_site_config / t_attachment / t_friend_link | 40xxx |
| `stats` | 访客统计（自研日聚合） | t_page_view / t_page_view_daily | 50xxx |
| `common-infra` | 跨模块基础设施：响应封装、异常体系、Security 链路、Knife4j、Clock、i18n、ArchUnit、MyBatis-Plus / Flyway 配置 | — | 90xxx（兜底 99999） |

`common-infra` 实际 Java 包路径为 `com.tyb.myblog.v2.common`（含子目录 `infrastructure`），文档中统一以 `common-infra` 指代这一整层。

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

> 待验证：用户首次执行后确认命令是否准确

```bash
cd MyBlog-springboot-v2

mvn clean compile          # 编译
mvn test                   # 跑测试（含 ArchUnit 守护）
mvn spring-boot:run -Dspring-boot.run.profiles=local   # 本地启动
mvn clean package          # 打包
```

## 环境变量

启动 `local` / `prod` 前必须设置：

- `MYBLOG_JWT_SECRET` — JWT 签名密钥（至少 32 字节）
- `MYBLOG_DATASOURCE_USERNAME` / `MYBLOG_DATASOURCE_PASSWORD` — 数据库账号与密码
- `MYBLOG_DATASOURCE_URL` — `prod` 必填，`local` 有本机开发库默认 URL

完整环境变量和测试 profile 说明见 `workflows/build-and-test.md`。

## 文档导航

- AI 工作入口：`CLAUDE.md`
- 文档索引：`INDEX.md`
- 历史文档：`../superpowers/`（已冻结）
