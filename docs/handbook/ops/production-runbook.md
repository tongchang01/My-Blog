# 生产部署运行手册

> 状态：当前有效；V2 已上线，日常发布由 GitHub Actions 自动部署
> 适用范围：生产运行核对、故障恢复与受控手工操作
> 最后校准：2026-07-14
> 对应文档：`deployment-direction.md`、`release-checklist.md`、`environment.md`
> 权威程度：生产操作顺序

## 私有信息说明

敏感生产信息已保存在仓库外的私有生产台账中，包括真实 IP、AWS 资源标识、安全组规则、Docker 卷标识、宿主机路径、证书状态、备份位置和环境变量值。

本手册不得写入这些真实值。执行人员必须在维护窗口前核对私有台账，并将本手册中的变量映射到已确认的真实资源；不得根据名称猜测服务器、容器、卷或目录。

私有台账至少应记录以下字段：

| 分类 | 必填字段 |
| --- | --- |
| AWS | 区域、实例 ID、弹性 IP、根卷 ID、快照或 AMI ID、安全组 ID、IAM Role 名称 |
| 数据与备份 | 数据库名、逻辑备份位置、SHA-256、创建时间、恢复演练记录、对象存储生命周期 |
| V2 | 发布提交 SHA、部署目录、生产环境文件位置、Compose 项目名、镜像完整名称 |
| 验收 | 主域名、`www` 域名、管理端域名、执行时间、执行人、异常与处理结果 |

约定的非敏感标准路径为：部署目录 `/opt/myblog-v2`，生产环境文件 `/etc/myblog-v2/runtime.env`。如实际路径不同，必须先在私有台账中记录，后续命令保持一致。

## 当前生产基线

- V2 运行在现有 AWS EC2，Docker Compose 管理 MySQL、API 和 Caddy；S3 由 EC2 IAM Role 访问。
- 正常 `main` 发布由 GitHub Actions 构建 GHCR 镜像，并以同一完整提交 SHA 自动部署；工作流会从公网检查主站、`www` 和管理端的 `/healthz`。
- 首次 GitHub OIDC SSH CD 演练和后续 `main` 自动发布均已成功。运行编号、实际 SHA 与敏感资源标识以私有生产台账和 GitHub Actions 为准。

## 执行原则

- 先获得可恢复证据，再停止、替换或删除任何资源。
- 删除容器不等于删除数据卷；所有卷操作都必须先与私有台账逐项核对。
- 清理命令逐项执行和复核，不使用通配符批量删除。
- 不把密码、密钥或完整环境文件粘贴到命令行、聊天、工单或日志。
- 禁止执行 `docker compose down -v`；它会删除 V2 MySQL 和 Caddy 持久卷。
- 任一对象、输出或状态与本手册及私有台账不一致时立即停止。

## 发布前或故障前核对

常规发布不登录服务器手工操作。只有排障、手工恢复或受控维护时，才登录私有台账记录的实例并执行：

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

将输出与私有台账逐项核对：确认目标实例、当前 Compose 服务、80/443 的占用、磁盘、内存与 swap 都符合预期。3306 和 8080 不应暴露到公网。

维护前必须已有可用的数据库备份和恢复路径；如果本次会改变基础设施或数据，再创建并验证 AMI/EBS 快照。具体证据要求以 [`release-checklist.md`](release-checklist.md) 为准。

## 手工恢复或全新重建

以下命令只用于 GitHub Actions 无法完成的受控恢复，或在空数据库上重建环境。常规 `main` 发布不执行本节；若文件布局变化，先同步修改本手册。

### 1. 放置已审核配置

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

### 2. 校验并拉取镜像

```bash
cd /opt/myblog-v2
sudo docker compose --env-file /etc/myblog-v2/runtime.env config --quiet
sudo docker compose --env-file /etc/myblog-v2/runtime.env config --images
sudo docker compose --env-file /etc/myblog-v2/runtime.env pull
```

核对输出中的两个应用镜像都带目标提交 SHA，且 `config --images` 只出现 `mysql:8.4`、目标 API 镜像和目标 web 镜像。若出现 `latest`、标签不一致、认证失败或变量缺失，停止部署。

### 3. 按依赖顺序启动

```bash
cd /opt/myblog-v2
set -euo pipefail
sudo docker compose --env-file /etc/myblog-v2/runtime.env up -d --wait --wait-timeout 180 mysql
sudo docker compose --env-file /etc/myblog-v2/runtime.env up -d --wait --wait-timeout 180 api
sudo docker compose --env-file /etc/myblog-v2/runtime.env logs --tail=200 api
sudo docker compose --env-file /etc/myblog-v2/runtime.env up -d --wait --wait-timeout 180 web
sudo docker compose --env-file /etc/myblog-v2/runtime.env ps
sudo docker compose --env-file /etc/myblog-v2/runtime.env logs --tail=200 web
```

`--wait` 依赖 Compose 中为服务定义有效健康检查。MySQL 未健康时不得启动 API；Flyway、S3 凭据、证书或代理出现错误时不得继续验收。

空数据库首次初始化管理员时，按 [`environment.md`](environment.md) 创建临时 root-only `bootstrap.env`，单独执行初始化命令并在成功后删除该文件；已有数据库不得重复执行初始化流程。

## 生产验收

### 主机与容器

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

### 外部与产品冒烟

每次手工恢复后至少检查主域名、`www` 和管理端 HTTPS、`/api` 代理、登录与后台权限、Flyway、S3、可信代理、端口边界及资源状态。完整逐项门槛见 [`release-checklist.md`](release-checklist.md)。

全部通过后，把发布 SHA、时间和结果写入私有台账，再结束维护窗口。

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
- 空数据库重建时可以按故障处置方案新建数据卷；已有需要保留的数据时不得直接删卷。
- 已产生需要保留的 V2 数据后，不允许直接删卷，必须另行设计数据库恢复步骤。

### 主机故障

- 停止继续修改当前实例。
- 使用私有台账记录的 AMI/EBS 快照恢复旧环境。
- 必要时从已验证的数据库备份恢复，并按其 SHA-256 校验文件。
- 恢复访问入口前，重新核对安全组、弹性 IP、DNS 和证书。

## 发布后收尾

- [ ] 记录实际发布 SHA、执行日期、异常和最终资源状态。
- [ ] 更新数据库备份、恢复演练与 S3 验证证据。
- [ ] 将确认后的非敏感事实同步回仓库文档；敏感事实继续只更新私有生产台账。

删除动作必须逐项执行；任何未在私有台账中同时记录“对象 ID、用途、替代对象和清理日期”的资源，都不得删除。
