# rules/ — 强约束规则

> 本目录回答："写代码时必须遵守什么？"
> 性质：长期有效的硬规则；AI 在动手前必须查阅相关条目。

## 写作约定

- 每个文件**只回答一类问题**，文件名即问题主题
- 单文件 5–10KB 以内，超过就拆
- 结构建议：`适用范围` → `规则正文（含正反例）` → `例外` → `相关 ADR 链接`
- 规则发生重大变化时，必须同步更新引用此规则的 ADR 与 pitfalls

## 已有规则（待迁入 / 新建）

| 文件 | 主题 | 状态 |
|------|------|------|
| `package-layout.md` | 包结构、命名、分层依赖方向 | 待迁入（源：旧 specs/2026-05-31-backend-v2-package-structure-decisions） |
| `sql-placement.md` | SQL 该写注解还是 XML | 待迁入（源：旧 specs/2026-06-01-backend-v2-persistence-sql-placement-rules） |
| `comment-style.md` | 中文注释位置、Javadoc 规范 | 待迁入（源：旧 specs/2026-05-31-backend-v2-code-comment-standards） |
| `error-handling.md` | 异常分类、全局异常处理、错误码 | 待新建 |
| `security-baseline.md` | JWT、密码、白名单、CORS、密钥管理 | 待新建 |
| `api-response.md` | 统一响应格式、分页、状态码语义 | 待新建 |
| `testing-policy.md` | 单测/集成测试覆盖要求 | 待新建 |

## 与其它目录的关系

- 规则的**理由**写在 `../decisions/` 对应 ADR 里，本目录只写"怎么做"
- 违反规则导致的事故，记录到 `../pitfalls.md`
