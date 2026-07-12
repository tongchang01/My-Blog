# 生产部署运行手册

> 状态：上线镜像产物已完成；服务器预检、配置核对、部署和验收尚未完成，因此当前禁止清理 V1
> 适用范围：当前 EC2 清理 V1 后原地部署 MyBlog V2
> 最后校准：2026-07-11
> 对应文档：`deployment-direction.md`、`release-checklist.md`、`environment.md`
> 权威程度：生产操作顺序

## 私有信息说明

敏感生产信息已保存在仓库外的私有生产台账中，包括真实 IP、AWS 资源标识、安全组规则、Docker 卷标识、宿主机路径、证书状态、备份位置和环境变量值。

本手册不得写入这些真实值。执行人员必须在维护窗口前核对私有台账，并将本手册中的变量映射到已确认的真实资源；不得根据名称猜测服务器、容器、卷或目录。

私有台账至少应记录以下字段：

| 分类 | 必填字段 |
| --- | --- |
| AWS | 区域、实例 ID、弹性 IP、根卷 ID、快照或 AMI ID、安全组 ID、IAM Role 名称 |
| V1 | Java 进程识别信息、4 个容器名称、旧 MySQL 卷 ID、旧应用目录、旧配置与环境文件位置 |
| 备份 | V1 数据库名、逻辑备份位置、SHA-256、创建时间、统一清理日期 |
| V2 | 发布提交 SHA、部署目录、生产环境文件位置、Compose 项目名、镜像完整名称 |
| 验收 | 主域名、`www` 域名、管理端域名、执行时间、执行人、异常与处理结果 |

约定的非敏感标准路径为：部署目录 `/opt/myblog-v2`，生产环境文件 `/etc/myblog-v2/runtime.env`。如实际路径不同，必须先在私有台账中记录，后续命令保持一致。

## 执行原则

- 不重装操作系统，只清理 V1 应用层资源。
- 不迁移 V1 业务数据，V2 使用全新数据库和数据卷。
- 先获得可恢复证据，再停止或删除任何资源。
- 删除容器不等于删除数据卷；旧 MySQL 卷保留 7 天。
- 清理命令逐项执行和复核，不使用通配符批量删除。
- 不把密码、密钥或完整环境文件粘贴到命令行、聊天、工单或日志。
- 禁止执行 `docker compose down -v`；它会删除 V2 MySQL 和 Caddy 持久卷。
- 任一对象、输出或状态与本手册及私有台账不一致时立即停止。

## 阶段一：停机前硬门槛

以下项目必须全部完成；任何一项未完成，都不得停止或清理 V1：

- [x] 首个管理员一次性初始化能力已实现并通过测试。
- [x] `myblog-api` Dockerfile 已通过 GHCR 构建，包含 Java 17、非 root 用户和健康检查。
- [x] `myblog-web` 已通过 GHCR 构建，包含 blog/admin 生产构建和 Caddy 配置。
- [ ] 生产 Compose 已固定私有网络、MySQL/Caddy 持久卷、资源限制和 restart policy。
- [x] 后端、真实 MySQL 专项、blog/admin 测试和生产构建全部通过 CI。
- [x] 两个公共 GHCR 镜像已使用同一提交 SHA `fb37fac5477183e61d3b8521ba76c2d5ffe9066b` 发布。
- [ ] 在未登录 GHCR 的服务器上能够匿名拉取两个镜像。
- [x] `.github/workflows/images.yml` 已在目标提交上成功运行，两个镜像的 SHA 标签完全一致。
- [ ] `compose.yaml` 已通过 `docker compose config --quiet`。
- [ ] 待发布提交 SHA、两个镜像完整名称和上一可用 SHA 已写入私有台账。

阶段一仍有服务器侧未完成项，所以这份手册虽然提供了执行命令，但还不能用于实际清理服务器。

## 阶段二：准备 AWS 与生产配置

### 2.1 AWS 控制台

- [ ] AWS root 已启用 MFA，日常操作使用单独管理员身份。
- [ ] 创建并挂载 EC2 IAM Role，仅允许访问 V2 S3 附件前缀。
- [ ] 实例 metadata options 已设置为要求 IMDSv2，容器访问所需 hop limit 已核对。
- [ ] 安全组最终只保留：80/443 公网、22 受限；不开放 3306/8080。
- [ ] Route 53 主域名、`www` 和管理端记录均指向当前弹性 IP。

修改 IAM Role、metadata options 或安全组后，将实际角色名和规则 ID 更新到私有台账。后端通过 EC2 IAM Role 获取临时 S3 凭据，不创建 Access Key。

### 2.2 生产配置

- [ ] 已生成全新的 MySQL、JWT、统计哈希和管理员凭据。
- [ ] V2 使用只允许访问 `myblog_v2.*` 的专用 MySQL 账号，不使用 root；账号具备 Flyway 和应用运行所需权限。
- [ ] 生产环境文件仅 root 可读，且未进入 shell history、Git 或日志。
- [ ] 首次上线保持邮件功能关闭。
- [ ] 首个管理员初始化使用临时 root-only 文件，成功后已删除；常驻 API 环境不保留 bootstrap 密码变量。
- [ ] Java 最大堆为 512 MiB，时区为 `Asia/Tokyo`。
- [ ] 容器环境中禁用 IMDSv1 回退。
- [ ] 已安排可接受停机的维护窗口。

环境变量名称和约束以 [`environment.md`](environment.md) 为准。部署产物实现时，`compose.yaml` 必须把其中的生产变量显式传给 `api`，并补充镜像标签、MySQL 初始化和一次性管理员初始化所需变量；变量值只保存在 root-only 环境文件中。

在服务器上创建标准目录，随后使用安全传输方式写入环境文件：

```bash
sudo install -d -m 0755 /opt/myblog-v2
sudo install -d -m 0700 /etc/myblog-v2
sudo test -e /etc/myblog-v2/runtime.env || sudo install -m 0600 /dev/null /etc/myblog-v2/runtime.env
sudo chown root:root /etc/myblog-v2/runtime.env
sudo chmod 0600 /etc/myblog-v2/runtime.env
sudo stat -c '%a %U:%G %n' /etc/myblog-v2/runtime.env
```

预期权限为 `600 root:root`。不要用 `echo` 把密钥追加到文件，也不要提交该文件。

根目录 `compose.yaml` 使用 `GHCR_OWNER` 和 `IMAGE_TAG` 选择镜像。`IMAGE_TAG` 必须填写私有台账记录的完整提交 SHA；不要填写 `latest` 或短 SHA。镜像发布由 `.github/workflows/images.yml` 完成，PR 不推送镜像。

首次创建管理员时，另外创建临时文件 `/etc/myblog-v2/bootstrap.env`，只写入本次命令需要的四个 `MYBLOG_BOOTSTRAP_ADMIN_*` 变量。不要在聊天、工单、终端输出或 Git 中记录文件内容：

```bash
sudo install -m 0600 /dev/null /etc/myblog-v2/bootstrap.env
sudo chown root:root /etc/myblog-v2/bootstrap.env
sudoedit /etc/myblog-v2/bootstrap.env
sudo stat -c '%a %U:%G %n' /etc/myblog-v2/bootstrap.env
```

文件中应由维护人员填入真实的 `enabled=true`、管理员用户名、一次性密码（8-128 字符）和 `exit-after-run=true`。该文件不是常驻配置，初始化成功后必须删除并确认不存在：

```bash
sudo rm -f /etc/myblog-v2/bootstrap.env
test ! -e /etc/myblog-v2/bootstrap.env
```

## 阶段三：只读预检与回滚点

### 3.1 只读预检

登录私有台账记录的实例，执行：

```bash
set -euo pipefail
date -Is
uname -a
docker version
docker compose version
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}'
pgrep -af java || true
sudo ss -lntp
df -h
free -h
swapon --show
```

将输出与私有台账逐项核对。确认当前确实是目标实例、V1 进程和容器名称一致、80/443/3306/8080 的占用符合预期，并确认磁盘足以同时保存备份和拉取新镜像。

### 3.2 建立主机恢复点

在 EC2 控制台为当前实例创建 AMI，或为根 EBS 卷创建快照。等待状态变为可用后，将资源 ID、创建时间和统一清理日期写入私有台账。

- [ ] 快照或 AMI 状态为可用。
- [ ] 已确认自己具备从该恢复点启动实例并重新绑定网络入口的权限。
- [ ] 恢复点覆盖旧应用、证书和 Docker 元数据。

### 3.3 导出 V1 MySQL 逻辑备份

先从私有台账填写非密钥变量。`V1_MYSQL_CONTAINER`、`V1_DATABASE` 和 `BACKUP_DIR` 不能为空：

```bash
set -euo pipefail
V1_MYSQL_CONTAINER='' # 从私有台账填写
V1_DATABASE=''        # 从私有台账填写
BACKUP_DIR=''         # 从私有台账填写
: "${V1_MYSQL_CONTAINER:?}"
: "${V1_DATABASE:?}"
: "${BACKUP_DIR:?}"

umask 077
install -d -m 0700 "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR/myblog-v1-$(date +%Y%m%d-%H%M%S).sql"
docker inspect "$V1_MYSQL_CONTAINER" >/dev/null
docker exec -e V1_DATABASE="$V1_DATABASE" "$V1_MYSQL_CONTAINER" sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --events --triggers --databases "$V1_DATABASE"' \
  >"$BACKUP_FILE"
test -s "$BACKUP_FILE"
sha256sum "$BACKUP_FILE"
ls -lh "$BACKUP_FILE"
```

把备份完整路径、大小、SHA-256、创建时间和 7 天清理日期写入私有台账。命令依赖旧 MySQL 容器内已有的 `MYSQL_ROOT_PASSWORD`，不会把密码带到宿主机命令行；若容器未设置该变量，立即停止并改用已确认的受控凭据方案。

只有快照或 AMI、逻辑备份和旧 MySQL 卷 ID 三项均已确认，才能进入维护窗口。

## 阶段四：清理 V1 应用层

### 4.1 最后确认

- [ ] 阶段一全部通过。
- [ ] AWS 恢复点可用，V1 备份非空且 SHA-256 已记录。
- [ ] 旧 MySQL 卷 ID 已记录，并与即将创建的 V2 卷明确区分。
- [ ] 私有台账中的实例、进程、容器和目录刚刚重新核对过。
- [ ] 维护窗口已经开始，访问者已知晓停机。

### 4.2 停止 V1 Java

先识别进程，再把刚刚确认的 PID 填入变量。不得使用历史台账中的旧 PID：

```bash
pgrep -af java
V1_JAVA_PID='' # 填写刚刚确认的 PID
: "${V1_JAVA_PID:?}"
ps -p "$V1_JAVA_PID" -o pid,user,lstart,args
kill -TERM "$V1_JAVA_PID"
for i in $(seq 1 30); do
  kill -0 "$V1_JAVA_PID" 2>/dev/null || break
  sleep 1
done
kill -0 "$V1_JAVA_PID" 2>/dev/null && echo '进程未停止，请人工处理' && exit 1
pgrep -af java || true
```

### 4.3 逐个删除旧容器，保留旧卷

从私有台账填写名称，先 inspect，再逐个 stop/rm：

```bash
V1_NGINX_CONTAINER=''  # 从私有台账填写
V1_MYSQL_CONTAINER=''  # 从私有台账填写
V1_REDIS_CONTAINER=''  # 从私有台账填写
V1_RABBITMQ_CONTAINER='' # 从私有台账填写
: "${V1_NGINX_CONTAINER:?}"
: "${V1_MYSQL_CONTAINER:?}"
: "${V1_REDIS_CONTAINER:?}"
: "${V1_RABBITMQ_CONTAINER:?}"

docker inspect "$V1_NGINX_CONTAINER" >/dev/null
docker stop "$V1_NGINX_CONTAINER" && docker rm "$V1_NGINX_CONTAINER"
docker inspect "$V1_MYSQL_CONTAINER" >/dev/null
docker stop "$V1_MYSQL_CONTAINER" && docker rm "$V1_MYSQL_CONTAINER"
docker inspect "$V1_REDIS_CONTAINER" >/dev/null
docker stop "$V1_REDIS_CONTAINER" && docker rm "$V1_REDIS_CONTAINER"
docker inspect "$V1_RABBITMQ_CONTAINER" >/dev/null
docker stop "$V1_RABBITMQ_CONTAINER" && docker rm "$V1_RABBITMQ_CONTAINER"

docker ps -a
docker volume ls
sudo ss -lntp
```

不要执行 `docker volume prune`，不要删除私有台账记录的旧 MySQL 卷。旧目录和 Certbot 状态已有主机快照保护；为降低误删风险，本次上线不要求立即删除，待 V2 验收后再按私有台账逐项清理。

### 4.4 增加 1 GiB swap

先检查已有 swap。只有 `swapon --show` 无输出且 `/swapfile` 不存在时，才执行创建步骤：

```bash
swapon --show
test ! -e /swapfile
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
grep -qF '/swapfile none swap sw 0 0' /etc/fstab || \
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
swapon --show
free -h
```

如已有 swap 或 `/swapfile` 已存在，先调查现状，不覆盖、不重复追加 `/etc/fstab`。

## 阶段五：部署 V2

以下命令假定部署产物最终包含根目录 `compose.yaml`，并使用标准路径。若实现后的文件布局不同，必须先同步修改本手册。

### 5.1 放置已审核配置

从公开仓库取得待发布提交，并只检出私有台账记录的 SHA：

```bash
set -euo pipefail
RELEASE_SHA='' # 从私有台账填写完整提交 SHA
: "${RELEASE_SHA:?}"

sudo chown "$(id -u):$(id -g)" /opt/myblog-v2
if [ ! -d /opt/myblog-v2/.git ]; then
  git clone https://github.com/tongchang01/My-Blog.git /opt/myblog-v2
fi
cd /opt/myblog-v2
git fetch --prune origin
git checkout --detach "$RELEASE_SHA"
test "$(git rev-parse HEAD)" = "$RELEASE_SHA"
test -f compose.yaml
```

### 5.2 校验并拉取镜像

```bash
cd /opt/myblog-v2
sudo docker compose --env-file /etc/myblog-v2/runtime.env config --quiet
sudo docker compose --env-file /etc/myblog-v2/runtime.env config --images
sudo docker compose --env-file /etc/myblog-v2/runtime.env pull
```

核对输出中的两个应用镜像都带目标提交 SHA，且 `config --images` 只出现 `mysql:8.4`、目标 API 镜像和目标 web 镜像。若出现 `latest`、标签不一致、认证失败或变量缺失，停止部署。

### 5.3 按依赖顺序启动

```bash
cd /opt/myblog-v2
set -euo pipefail
sudo docker compose --env-file /etc/myblog-v2/runtime.env up -d --wait --wait-timeout 180 mysql
sudo docker compose --env-file /etc/myblog-v2/runtime.env --env-file /etc/myblog-v2/bootstrap.env run --rm --no-deps api
sudo rm -f /etc/myblog-v2/bootstrap.env
test ! -e /etc/myblog-v2/bootstrap.env
sudo docker compose --env-file /etc/myblog-v2/runtime.env up -d --wait --wait-timeout 180 api
sudo docker compose --env-file /etc/myblog-v2/runtime.env logs --tail=200 api
sudo docker compose --env-file /etc/myblog-v2/runtime.env up -d --wait --wait-timeout 180 web
sudo docker compose --env-file /etc/myblog-v2/runtime.env ps
sudo docker compose --env-file /etc/myblog-v2/runtime.env logs --tail=200 web
```

`--wait` 依赖 Compose 中为服务定义有效健康检查。MySQL 未健康时不得启动 API；Flyway、S3 凭据、证书或代理出现错误时不得继续验收。

一次性命令必须以退出码 0 结束，并且日志只应出现初始化成功或已有管理员跳过的结果（日志不得出现密码或密码摘要）。不要追加 `--spring.main.web-application-type=none`：安全配置需要完整的 Web 应用上下文；`bootstrap.env` 中的 `MYBLOG_BOOTSTRAP_ADMIN_EXIT_AFTER_RUN=true` 会在初始化完成后受控退出。退出码非 0 时不要删除 `bootstrap.env`，先保留现场并处理数据库、配置或镜像问题。初始化成功后再继续启动常驻 `api`，并确认临时文件已删除。

## 阶段六：生产验收

### 6.1 主机与容器

```bash
cd /opt/myblog-v2
sudo docker compose --env-file /etc/myblog-v2/runtime.env ps
sudo docker compose --env-file /etc/myblog-v2/runtime.env logs --tail=200 mysql api web
sudo ss -lntp
free -h
swapon --show
df -h
```

预期只有 `web` 向宿主机发布 80/443；3306 和 8080 不应监听公网地址。检查日志时不得复制或传播敏感值。

### 6.2 外部与产品冒烟

- [ ] 主域名、`www` 和管理端 HTTPS 正常，证书链有效。
- [ ] 公共 API 可访问，`/api` 前缀被完整保留。
- [ ] 首个管理员创建成功；初始化开关和明文密码变量已从运行环境移除，再次执行初始化会被拒绝。
- [ ] 登录、刷新、退出和后台权限正常。
- [ ] Flyway 版本与 checksum 正常。
- [ ] S3 上传、公开读取、软删除和恢复正常。
- [ ] 客户端 IP 只信任 Compose 代理网段。
- [ ] 从外部网络确认 443 可达，而 3306 和 8080 不可达。
- [ ] 内存、swap、磁盘、日志和数据库连接稳定。

完整产品冒烟与回滚门槛继续执行 [`release-checklist.md`](release-checklist.md)，本手册不复制其内容。全部通过后，把发布 SHA、时间和结果写入私有台账，再结束维护窗口。

## 自动 CD

常规 main 发布由 GitHub Actions 自动完成；AWS 仅临时放行当前 Runner 的 SSH /32。首次启用、部署用户安装、GitHub Environment 和 OIDC 配置见 [`github-ssh-cd.md`](github-ssh-cd.md)。CD 失败时不自动回滚数据库，按本手册的回滚边界处理。

## 回滚

### V2 镜像或配置问题

从私有台账取上一条已验证提交 SHA，恢复对应代码和镜像变量，然后执行：

```bash
cd /opt/myblog-v2
PREVIOUS_RELEASE_SHA='' # 从私有台账填写完整提交 SHA
: "${PREVIOUS_RELEASE_SHA:?}"
git checkout --detach "$PREVIOUS_RELEASE_SHA"
test "$(git rev-parse HEAD)" = "$PREVIOUS_RELEASE_SHA"
sudo docker compose --env-file /etc/myblog-v2/runtime.env config --quiet
sudo docker compose --env-file /etc/myblog-v2/runtime.env pull
sudo docker compose --env-file /etc/myblog-v2/runtime.env up -d --wait --wait-timeout 180
```

重新执行最低生产验收。不要运行 `docker compose down -v`。

### 数据库迁移问题

- 停止发布，不修改 Flyway history，不假定镜像回滚会自动回滚 schema。
- 首次上线时 V2 使用全新数据卷；如果确认无需保留其中任何 V2 数据，记录并核对新卷 ID 后，才可按故障处置方案重建空库。
- 已产生需要保留的 V2 数据后，不允许直接删卷，必须另行设计数据库恢复步骤。

### 主机或 V1 清理事故

- 停止继续修改当前实例。
- 使用私有台账记录的 AMI/EBS 快照恢复旧环境。
- 必要时从 V1 SQL 备份恢复数据库，并按其 SHA-256 校验文件。
- 恢复访问入口前，重新核对安全组、弹性 IP、DNS 和证书。

## 七天收尾

- [ ] 连续观察 7 天，未发生需恢复 V1 的问题。
- [ ] 删除旧 MySQL 数据卷前，再次核对其 ID 与 V2 卷 ID 不同。
- [ ] 删除临时 AMI/EBS 快照和 V1 MySQL 保险备份。
- [ ] 清理旧 JAR、前端、Nginx、Certbot 状态和 V1 加密环境文件副本。
- [ ] 记录实际发布 SHA、执行日期、异常和最终资源状态。
- [ ] 将确认后的非敏感事实同步回仓库文档；敏感事实继续只更新私有生产台账。

删除动作必须逐项执行；任何未在私有台账中同时记录“对象 ID、用途、替代对象和清理日期”的资源，都不得删除。
