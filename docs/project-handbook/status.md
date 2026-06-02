# 当前进度

> 本文档回答："V2 现在做到哪了？什么能用、什么还没做？"
> 更新时机：每完成一个模块/重要功能后更新。
> 当前日期：2026-06

## 1. 模块进度

| 模块 | 状态 | 已完成 | 待完成 |
|------|------|--------|--------|
| `common` | ✅ 稳定 | 响应封装、异常体系、安全组件、Web 工具 | — |
| `infrastructure` | ✅ 仅含全局配置 | MyBatis-Plus / Flyway 配置 | 后续视需要扩展 |
| `identity` | ⚠️ 过渡实现 | 用户、登录、JWT 签发/撤销、登录审计 | 按新产品规格和新 schema 调整；在线用户管理、踢下线后续专项 |
| `content` | ⚠️ 过渡实现 | 文章/分类/标签读取；`ContentCatalogMapper` XML 样板已落地 | 等产品清单、领域模型、新 schema 定稿后重写或调整 |
| `comment` | ⚠️ 过渡实现 | 评论、审核、软删除/恢复 | 等评论业务规则和新 schema 定稿后重写或调整 |
| `system` | ⏳ 未开始 | — | 整体模块尚未创建 |

## 2. 已落地的关键能力

### 2.1 安全
- ✅ JWT 签发/解析/撤销（spring-security-oauth2-jose）
- ✅ JWT secret 环境变量强校验
- ✅ 登录审计字段更新（`last_login_time` / `ip_address`）
- ✅ 客户端 IP 提取（X-Forwarded-For → X-Real-IP → RemoteAddr）
- ✅ 安全白名单 method+path 双维度
- ✅ BCrypt 密码

### 2.2 持久化
- ✅ MyBatis-Plus 主 ORM
- ✅ Flyway 启用，测试 profile 自动迁移到 H2
- ✅ Repository 抽象（domain 接口 + infrastructure 实现）

### 2.3 错误与响应
- ✅ ApiResponse / PageResponse 统一
- ✅ ApiException + ApiErrorCode
- ✅ GlobalExceptionHandler 统一兜底
- ✅ HTTP 状态码语义明确

### 2.4 架构守护
- ✅ ArchUnit 启用，5 条核心规则
- ✅ FlywayMigrationTest 验证迁移脚本 H2 兼容
- ✅ 35+ 份测试文件，具体数量以 `mvn test` 输出为准

### 2.5 文档
- ✅ `docs/project-handbook/` 体系建立（rules / arch / decisions / workflows / product / api-contract / migration）

## 3. 测试覆盖现状

| 模块 | 测试文件数 | 覆盖评估 |
|------|-----------|----------|
| comment | 6 | 较好（含 admin/audit） |
| content | 5 | 中等（缺 ApplicationService 集成） |
| identity | 8 | 较好（含登录审计） |
| common | 9 | 较好（安全、Web 工具均覆盖） |
| 架构守护 | 1（ArchUnit） | ✅ |
| Flyway | 1 | ✅ |

## 4. 已知遗留问题（汇总）

详见 `pitfalls.md`。

短期内已解决：
- `ContentCatalogMapper` @Select → XML（P-002）

短期内仍需关注：
- 评论核心命令集成测试（U-006）

迁移其它模块前必须解决：
- 富文本 XSS 清洗（U-002）
- 上传文件安全（U-003）

部署到多实例前必须解决：
- TokenRevocationStore 迁 Redis（P-001）

## 5. 当前不建议做的事

- ❌ 拆微服务（项目规模不匹配）
- ❌ 引入消息队列（无场景需求）
- ❌ 引入 Elasticsearch（搜索量小，MySQL 全文索引足够）
- ❌ 大规模重写 V1（V1 只读，V2 独立重构）
- ❌ 在产品清单和新 schema 未定稿前继续大规模迁移 content/comment 业务

## 6. 下一步重点

详见 `roadmap.md`。

近期：
1. 标注 `product/feature-inventory.md` 的 V2 去留
2. 基于功能清单生成 `product/use-cases.md`、`product/business-rules.md`、`product/data-model.md`
3. 定稿 `arch/schema-design.md`
4. 再决定后端业务代码和三端接口契约推进顺序

中期：
4. 富文本 XSS / 上传安全（迁文章前）
5. TokenRevocationStore 迁 Redis（前提：引入 Redis 的 ADR）

## 7. 相关文档

- 路线图：`roadmap.md`
- 历史与红线：`pitfalls.md`
- 架构现状：`arch/`
- 操作流程：`workflows/`
