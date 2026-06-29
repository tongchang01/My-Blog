# 后端 V2 登录审计字段更新设计

## 背景

后端 V2 已经完成真实 MySQL 账号登录闭环：可以从旧库 `t_user_auth`、`t_user_info`、`t_user_role`、`t_role` 读取账号、校验 BCrypt 密码、签发 JWT，并用 JWT 读取当前用户。

当前缺口是登录成功后没有回写旧表审计字段。旧表 `t_user_auth` 已经包含 `last_login_time`、`ip_address`、`ip_source`，但 V2 目前只读取账号，不更新登录审计信息。

## 目标

本阶段只补齐登录成功后的最小审计闭环：

- 登录成功后更新 `t_user_auth.last_login_time`。
- 登录成功后更新 `t_user_auth.ip_address`。
- 暂不更新 `ip_source`，因为 IP 归属地解析会引入额外依赖、外部数据源或旧工具迁移，超出当前基盘任务。
- 登录失败、账号不存在、密码错误、禁用用户都不更新审计字段。

## 非目标

- 不改登录接口响应结构。
- 不改 JWT 签发、解析、撤销逻辑。
- 不新增 IP 归属地解析能力。
- 不引入 Redis 在线用户、踢下线、登录设备管理。
- 不调整旧表结构。
- 不修改前台或后台管理端。

## 推荐方案

采用端口加适配器方式实现：

- 在 `modules.identity.domain` 新增 `LoginAuditRecorder` 端口。
- 在 `modules.identity.infrastructure` 新增 `DatabaseLoginAuditRecorder`，使用 `JdbcTemplate` 更新旧表。
- 在 `AuthService.login(...)` 中，密码校验成功、JWT 签发前后均可调用审计；建议放在构造 `AuthenticatedUser` 之后、签发 token 之前。这样语义是“认证已通过，正在完成登录副作用”。
- 在 `AuthController.login(...)` 中从 `HttpServletRequest` 提取客户端 IP，并传入应用层登录命令。
- `LoginCommand` 从只包含用户名和密码，扩展为包含 `clientIp`。为了单元测试和旧调用清晰，可以保留一个双参数构造器，默认 `clientIp = null`。

## IP 提取规则

本阶段只做保守实现：

- 优先读取请求头 `X-Forwarded-For` 的第一个非空 IP。
- 如果没有 `X-Forwarded-For`，读取 `X-Real-IP`。
- 如果还没有，使用 `request.getRemoteAddr()`。
- 对空白值返回 `null`。
- 不做公网、内网、代理可信链校验，因为部署拓扑还没有进入本期范围。

这套规则满足本地开发和常见反向代理场景，同时不会把部署专项提前拉进来。

## 数据更新规则

数据库实现只更新旧表 `t_user_auth`：

```sql
update t_user_auth
set last_login_time = current_timestamp,
    ip_address = ?
where id = ?
```

说明：

- `id` 使用当前登录流程中的 `UserCredential.id()`，它现在对应 `t_user_auth.id`。
- `ip_address` 允许为 `null`，避免在无法识别客户端 IP 时写入伪造值。
- `ip_source` 保持原值，不主动清空。

## 错误处理

登录审计属于登录成功后的附带副作用，但它也是旧系统账号审计的一部分。本阶段建议：

- 如果审计更新 SQL 失败，登录接口返回系统错误，不签发 token。
- 原因是：数据库已可读但不可写时，系统状态不一致，继续签发 token 会掩盖审计链路问题。
- 后续如果要提高可用性，可以单独设计异步审计或失败降级策略，但不放入本阶段。

## 测试策略

本阶段需要补以下测试：

- `DatabaseLoginAuditRecorderTest`：调用记录器后，断言 `t_user_auth.last_login_time` 不为空，`ip_address` 被更新。
- `AuthControllerTest`：登录成功时传入 `X-Forwarded-For`，再查数据库确认 IP 被写入。
- `AuthControllerTest`：密码错误时确认管理员账号的 `last_login_time` 仍为空。
- `AuthServiceTest`：使用 fake `LoginAuditRecorder` 确认只有登录成功才调用审计。

## 计划边界

这个任务完成后，身份基盘将达到：

- 真实旧库账号可登录。
- 登录后能签发 JWT。
- JWT 能读取当前用户。
- 登录成功能回写登录时间和 IP 地址。

下一阶段再考虑后台菜单、角色权限展示、动态资源权限或 Redis 在线用户，不和本任务混在一起。
