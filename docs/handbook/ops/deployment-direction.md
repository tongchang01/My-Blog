# 部署方向与待确认前提

> 状态：需要环境确认
> 适用范围：V2 生产部署设计
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/resources/application-prod.yml`、`frontend/apps/`
> 权威程度：部署约束

仓库已经具备 prod profile、S3 适配、健康检查、CI 和发布清单，但没有记录实际服务器、域名、代理、MySQL、证书和备份拓扑。因此当前只定义可行基线，不把假定拓扑写成已部署事实。

## 推荐基线

- 单实例 Spring Boot jar，由 systemd 或等价进程管理器运行，仅监听回环或内网地址。
- Nginx/Caddy 提供 TLS、两个前端静态文件和 `/api/**` 反向代理。
- MySQL 8 使用独立账号，生产启动由 Flyway 迁移。
- 附件使用 S3 或兼容服务；应用通过默认凭证链获取权限。
- 发布以不可变 release 目录和 current 指针切换，保留上一版产物用于回滚。
- 当前不需要 Kubernetes、多实例、蓝绿部署或自动 CD。

## 实施前必须确认

- 服务器系统、资源、端口、运行用户和 Java 环境。
- 域名、DNS/CDN、TLS 证书和反向代理配置。
- MySQL 位置、版本、字符集、账号、备份、恢复和迁移权限。
- S3 provider、region、bucket、公开 URL、凭据和生命周期策略。
- blog/admin 的域名或路径、history/hash 路由回退策略。
- 环境变量存放、文件权限、服务重启和日志轮转。
- 发布审批、健康检查、冒烟、失败回滚和数据恢复责任。

实际拓扑确认后，应把可执行的代理、服务、备份和回滚命令加入本目录，再考虑 CD。
