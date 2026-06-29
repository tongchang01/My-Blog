# 后端 V2 真实身份与权限模型盘点

## 盘点结论

我已经把本地导入后的 `aurora` 数据库、旧后端身份权限代码、以及 V2 当前安全认证基础工程放在一起看了一遍。结论是：下一阶段不应该急着写业务接口，而应该先做“真实身份领域迁移”。

当前 V2 已经具备 Bearer JWT 登录、解析、撤销、`/me`、401/403 统一响应和基础角色鉴权，但用户来源仍然是配置文件。旧系统真实用户体系由 `t_user_auth`、`t_user_info`、`t_user_role`、`t_role`、`t_menu`、`t_role_menu`、`t_resource`、`t_role_resource` 共同组成，里面同时承担了账号登录、用户资料、后台菜单、接口资源权限、在线用户和登录状态维护。

如果我现在直接迁移文章、评论、后台管理接口，会导致这些接口要么只能依赖临时配置账号，要么把旧系统的权限耦合直接复制进 V2。前者会让后续联调反复返工，后者会把这次重构重新带回旧代码的问题里。

## 本地数据库证据

连接目标：`localhost:3306`，数据库：`aurora`。本次只做了只读查询，没有修改数据。

核心表规模：

| 表 | 记录数 | 用途 |
| --- | ---: | --- |
| `t_user_auth` | 1 | 登录账号、密码哈希、登录类型、登录 IP、最后登录时间 |
| `t_user_info` | 1 | 用户资料、头像、邮箱、订阅状态、禁用状态 |
| `t_user_role` | 1 | 用户与角色关系 |
| `t_role` | 3 | 角色定义 |
| `t_menu` | 35 | 后台管理端菜单 |
| `t_role_menu` | 70 | 角色与菜单关系 |
| `t_resource` | 145 | 接口资源定义 |
| `t_role_resource` | 148 | 角色与接口资源关系 |

角色分布：

| 角色 ID | 角色名 | 禁用状态 | 用户数 | 资源数 | 菜单数 |
| ---: | --- | ---: | ---: | ---: | ---: |
| 1 | `admin` | 0 | 1 | 113 | 35 |
| 2 | `user` | 0 | 0 | 2 | 0 |
| 14 | `test` | 0 | 0 | 33 | 35 |

资源与菜单分布：

| 项目 | 值 | 数量 |
| --- | ---: | ---: |
| 接口资源 `is_anonymous = 0` | 非匿名 | 113 |
| 接口资源 `is_anonymous = 1` | 匿名 | 32 |
| 菜单 `is_hidden = 0` | 显示 | 30 |
| 菜单 `is_hidden = 1` | 隐藏 | 5 |

完整性检查结果：

| 检查项 | 异常数量 |
| --- | ---: |
| `t_user_auth.user_info_id` 找不到用户资料 | 0 |
| `t_user_role.user_id` 找不到用户资料 | 0 |
| `t_user_role.role_id` 找不到角色 | 0 |
| `t_role_resource.role_id` 找不到角色 | 0 |
| `t_role_resource.resource_id` 找不到资源 | 0 |
| `t_role_menu.role_id` 找不到角色 | 0 |
| `t_role_menu.menu_id` 找不到菜单 | 0 |

数据库约束现状：

- 每张核心表都有主键。
- `t_user_auth.username` 有唯一索引。
- 关联关系没有数据库外键约束，完整性主要靠应用层维护。

这个点很重要：V2 迁移时我不能只照着旧表复制字段，还需要补上必要的唯一约束、查询索引、关系完整性策略。否则数据量一上来，权限和用户关系会更难排查。

## 旧后端身份链路

旧后端登录入口是 Spring Security 表单登录：

- `WebSecurityConfig` 配置 `/users/login` 作为登录处理地址。
- `UserDetailServiceImpl` 按 `username` 查询 `t_user_auth`。
- 查询到账号后，再读取 `t_user_info` 和 `t_role`，拼成 `UserDetailsDTO`。
- `AuthenticationSuccessHandlerImpl` 登录成功后签发 token，并更新登录 IP、来源、最后登录时间。
- `TokenServiceImpl` 用 JWT 只保存用户账号 ID，完整用户信息放在 Redis 的 `login_user` 哈希里。
- `JwtAuthenticationTokenFilter` 每次请求从 token 里拿账号 ID，再去 Redis 取 `UserDetailsDTO` 并续期。

这个设计能跑，但 V2 不建议原样照搬。原因是 JWT 和 Redis 登录态绑得太紧，token 本身不是完整认证凭证；服务重启、Redis 丢失、序列化结构变化，都会影响已登录用户。V2 当前 Bearer JWT 已经把 `subject`、`username`、`roles`、`jti`、过期时间放进 token，并用撤销存储处理登出，方向更清晰。

## 旧后端权限链路

旧系统接口权限是动态资源权限：

- `t_resource` 保存接口 URL、请求方法、是否匿名、父级资源。
- `t_role_resource` 保存角色能访问哪些资源。
- `RoleMapper.listResourceRoles` 加载所有非匿名接口资源与角色关系。
- `FilterInvocationSecurityMetadataSourceImpl` 用 `AntPathMatcher` 匹配请求 URL 和 HTTP Method。
- 如果资源没有绑定角色，返回特殊权限 `disable`。
- `AccessDecisionManagerImpl` 对比当前用户角色和资源所需角色，不匹配则 403。
- 新增或修改角色、资源时，通过 `clearDataSource()` 清空静态缓存，下次请求重新从数据库加载。

这个模型的优点是后台可以配置接口权限，不需要改代码。但旧实现有几个明显问题：

- 权限缓存是 `static List`，没有并发保护，也没有版本号，后续多实例部署会失效。
- URL 权限来自 Swagger 导入，接口重构后如果没有同步导入，权限表会和真实接口漂移。
- `anyRequest().permitAll()` 叠加自定义 `FilterSecurityInterceptor`，理解成本高，安全边界不直观。
- 角色名直接作为 Spring Security authority 使用，没有统一前缀规范。
- 关联表没有外键，删除角色、菜单、资源时主要靠业务代码兜底。

所以 V2 不应该直接复制这套类，而应该把“权限模型”抽出来，分成更清晰的几层：认证身份、角色、后台菜单、接口访问策略、权限缓存。

## V2 当前差距

V2 当前已经完成的是安全骨架，不是真实身份体系：

- 账号来自 `myblog.identity.users` 配置。
- 角色只有 `ADMIN` 和 `USER`。
- `/api/admin/**` 只是固定要求 `ADMIN`。
- 没有连接 `t_user_auth` 和 `t_user_info`。
- 没有后台菜单 API。
- 没有动态资源权限。
- 没有 Redis 在线用户和踢下线能力。
- 没有生产数据库连接配置。

这正好说明下一阶段要先做真实身份领域，而不是开始迁移文章等业务模块。

## 我的建议

我建议下一阶段采用“先真实账号登录，后动态权限”的顺序。

第一步先把 V2 登录从配置文件切到数据库：

- 使用现有 `spring-boot-starter-jdbc` 和 `JdbcTemplate`，先不急着引入 MyBatis Plus 或 JPA。
- 建立 `DatabaseUserCredentialReader`，替换 `ConfiguredUserCredentialReader`。
- 读取 `t_user_auth`、`t_user_info`、`t_user_role`、`t_role`，返回 V2 需要的 `AuthenticatedUser`。
- 保留当前 JWT 签发、解析、撤销机制。
- 明确禁用用户不能登录。
- 登录成功后更新最后登录时间、IP 信息这类审计字段可以作为第二个小任务，不和读取登录混在一起。

第二步再做权限模型：

- 先保留 V2 固定规则：`/api/admin/**` 需要管理员角色。
- 把旧角色名映射到 V2 authority，例如 `admin -> ADMIN`、`user -> USER`、`test -> TEST`。
- 后台菜单和动态资源权限不要塞进登录任务里，单独出计划。

第三步再决定是否恢复动态接口资源权限：

- 如果后台管理端确实需要“页面上配置接口权限”，再实现 V2 版动态权限。
- 如果个人博客体量长期较小，可以只保留角色级权限，把接口资源表作为后台展示和审计数据，不作为运行时鉴权核心。

我倾向先不恢复旧的动态接口权限到运行时。原因是你的项目体量小，动态资源权限带来的复杂度明显高于收益。真正必须保留的是：管理员能进后台，普通用户只能做评论、个人信息、订阅等有限动作，后台菜单能按角色显示。

## 下一份实施计划建议

下一份计划建议命名为：

`docs/superpowers/plans/2026-05-24-backend-v2-database-identity-login.zh-CN.md`

建议只覆盖一个阶段目标：让 V2 使用本地 MySQL 的真实账号表登录。

计划边界：

- 接入 MySQL 配置。
- 为测试环境建立 H2 兼容的身份表迁移和测试数据。
- 新增数据库身份读取适配器。
- 用真实数据库字段替换配置账号读取。
- 补充登录、禁用用户、角色加载、缺失关系的回归测试。
- 暂不做后台菜单接口。
- 暂不做动态资源权限。
- 暂不做 Redis 在线用户。
- 暂不改前台和后台管理端。

这个边界比较稳：它能把 V2 从“安全骨架”推进到“真实账号可用”，但不会一次性把权限、菜单、Redis、前端联调全搅在一起。

## 需要我后续重点保护的约束

- 不改当前线上服务。
- 继续在 `backend-v2-refactor` 分支做。
- 每个任务完成后停下。
- Git 提交信息使用中文。
- 文档和沟通使用中文。
- 优先保留业务语义，不盲目照搬旧实现。
