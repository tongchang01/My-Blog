# 部署方向与服务器待确认项

> 状态：当前有效
> 适用范围：MyBlog V2 第一版上线准备
> 最后校准：2026-07-07
> 权威程度：部署前提与问题清单

## 当前判断

当前只确定项目代码侧具备生产 profile、S3 存储配置、健康检查、CI 和发布检查清单；服务器真实拓扑尚未记录。第一版上线前不要直接设计复杂 CD，先把服务器现状、手动部署和回滚路径跑通。

## 服务器待确认项

- 服务器厂商、区域、系统版本、CPU、内存、磁盘容量和剩余空间。
- 是否已有 Docker；如果已有，当前是否用于 MySQL、Nginx 或其它服务。
- 是否已有 Nginx / Caddy；80、443、8080、3306 等端口占用。
- 域名、DNS 托管方、是否经过 Cloudflare 或其它 CDN。
- TLS 证书来源：Let's Encrypt、云厂商证书或手工证书。
- MySQL 部署方式：本机、容器、托管服务；版本、账号、字符集和备份方式。
- Java 运行环境；是否允许服务器安装 JDK 运行 jar。
- 前端静态文件托管目录和 Nginx root 策略。
- S3 provider、bucket、region、public base URL、Bucket Policy / CDN、凭证注入方式。
- 是否允许 GitHub Actions 通过 SSH 部署；如果允许，SSH key、known_hosts、sudo 权限和回滚权限怎么管。

## 第一版推荐部署形态

先采用单机单实例：

- Nginx 负责 HTTPS、静态文件和 `/api/**` 反向代理。
- blog/admin 前端构建为静态文件，由 Nginx 托管。
- Spring Boot 后端用 systemd 运行 jar，只监听 `127.0.0.1:8080` 或内网地址。
- MySQL 先使用现有实例；上线前完成一次备份恢复演练。
- 附件使用 S3；生产不依赖本机 `/media/**` 存储。
- 邮件先保持可关闭状态，Resend/SES 等后续再校准。

暂不优先：

- Kubernetes。
- 多实例部署。
- 蓝绿发布。
- 自建对象存储。
- 把 CD、生产密钥和服务器操作塞进现有 CI。

## 手动部署最低步骤

1. 后端在 CI 和本地通过测试。
2. 前台 blog/admin 通过 typecheck、test、build。
3. 服务器准备环境变量文件，至少包含数据库、JWT、统计 hash、S3、CORS、trusted proxies。
4. 上传后端 jar 到 release 目录。
5. 上传 blog/admin dist 到静态 release 目录。
6. 切换 `current` symlink。
7. 重启 systemd 后端服务。
8. `curl /actuator/health`。
9. 浏览器冒烟：首页、文章详情、分类、标签、归档、关于、搜索、友链、后台登录、附件上传。
10. 记录本次版本、提交 SHA、环境变量变更和回滚点。

## CD 大致方向

手动部署跑通后，再做轻量 CD：

- 触发方式：GitHub Actions `workflow_dispatch`，人工选择分支或 SHA。
- 构建：GitHub runner 构建 jar 和前端 dist。
- 传输：SSH/rsync 或 scp 上传到服务器 release 目录。
- 切换：服务器脚本更新 `current` symlink。
- 重启：systemd restart 后端服务，Nginx 静态 root 指向 current。
- 验证：自动请求 health 和几个公开 URL；失败则停止并提示人工回滚。
- 回滚：保留最近若干 release，回切 symlink + systemd restart。

CD 不负责：

- 首次服务器初始化。
- 创建数据库和生产账号。
- 管理 S3 bucket。
- 自动修复迁移失败。
- 绕过发布前备份。

## 第一版上线前必须补齐

- 环境变量权威清单。
- Nginx 反向代理配置草案。
- systemd service 草案。
- 数据库备份和恢复步骤。
- S3 上传、读取、删除冒烟记录。
- 发布与回滚命令。
