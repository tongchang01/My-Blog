# arch/ — 架构现状

> 本目录回答："系统现在长什么样？"
> 性质：当前快照。架构变化时直接修改本目录文件，**不留历史版本**（历史靠 git）。

## 写作约定

- 描述"现状"，不是"未来计划"，更不是"曾经的方案"
- 用图表（mermaid / ASCII）说明结构，配最少必要文字
- 引用关键代码路径（`file_path:line_number`），便于 AI 跳转验证
- 任何架构调整完成后，必须更新本目录对应文件

## 计划包含的文件

| 文件 | 内容 |
|------|------|
| `module-map.md` | 六大模块边界、依赖方向、ArchUnit 守护规则一览 |
| `persistence-strategy.md` | ORM 现状（JdbcTemplate 与 MyBatis-Plus 混用情况）+ 迁移目标 |
| `auth-flow.md` | JWT 颁发/校验、登录流程、审计点 |
| `request-flow.md` | 一个请求从 Controller 到 DB 的完整链路示例 |

## 与其它目录的关系

- `arch/` 只描述**现状**；现状为何如此见 `../decisions/`
- 调整架构时遵循的硬规则见 `../rules/`
