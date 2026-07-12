# 发布检查清单

> 状态：当前有效
> 适用范围：V2 生产发布与回滚
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/`
> 权威程度：发布门槛

## 构建与配置

- [ ] 后端 `mvn clean test` 与真实 MySQL 专项通过。
- [ ] blog/admin 的 test、typecheck、lint、production build 通过。
- [ ] 发布提交 SHA、构建产物和数据库迁移版本已记录。
- [ ] prod profile 显式启用，JVM 时区为 `Asia/Tokyo`。
- [ ] `environment.md` 中生产必填变量由受控环境注入，未进入产物或日志。
- [ ] OpenAPI、Swagger UI 与 Knife4j 在 prod 关闭。
- [ ] GitHub deploy job 使用与镜像一致的完整 SHA，production Environment 仅允许 main。
- [ ] CD 独立安全组只在部署期间临时允许 Runner SSH /32，完成后已撤销。

## 数据与存储

- [ ] 数据库在迁移前完成备份，并在临时库演练过恢复。
- [ ] Flyway checksum 与目标库历史一致，迁移账号权限满足且不过宽。
- [ ] S3 上传、读取、软删除和恢复通过，公开 URL 不泄露凭据。
- [ ] 对象存储版本、生命周期或备份策略已经记录。

## 网络与安全

- [ ] 同源代理保留 `/api` 前缀；history 路由配置前端 fallback。
- [ ] 跨域部署只允许实际 origin，OPTIONS 预检成功，未知 origin 被拒绝。
- [ ] trusted proxies 只包含实际代理 IP/CIDR，代理覆盖外部伪造转发头。
- [ ] 直连后端时不信任 `X-Forwarded-For` 和 `X-Real-IP`。
- [ ] ADMIN/DEMO 权限、token refresh/退出、PASSWORD 内容阻断通过冒烟。
- [ ] 首个管理员初始化成功后，已通过管理端修改初始密码；旧会话失效，初始化专用环境变量已从服务器删除。

## 产品冒烟

- [ ] 首页、文章详情、分类、标签、归档、搜索、关于、友链和评论可用。
- [ ] 作者资料、站点配置、建站日期和页脚统计显示正确。
- [ ] 后台文章、首页槽位、分类标签、评论、友链、附件、配置和个人资料可用。
- [ ] 公开访问打点写入并能在聚合后进入 dashboard。
- [ ] health endpoint、服务日志、磁盘和数据库连接正常。

## 回滚

- [ ] 上一版后端与前端产物仍可用，切换与重启命令已验证。
- [ ] 数据库迁移失败时停止发布，不自动修改 Flyway 历史或回写旧 schema。
- [ ] 需要数据恢复时使用已演练备份；应用版本回滚不能假定 schema 自动回滚。
- [ ] 发布结果、异常、回滚点和后续处理已记录。
