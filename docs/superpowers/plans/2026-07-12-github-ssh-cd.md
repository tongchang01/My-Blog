# GitHub SSH CD Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 main 同 SHA 镜像发布成功后，通过 GitHub Actions 自动部署到唯一生产 EC2。

**Architecture:** 扩展现有 images.yml：publish 成功后，deploy job 经 OIDC 临时把 Runner 的 IPv4 /32 加到独立 CD SSH 安全组，使用 production Environment 的部署专用私钥调用服务器 forced command。root-owned 发布脚本验证 main SHA、更新 IMAGE_TAG、拉取同 SHA 镜像、等待 Compose 健康检查。

**Tech Stack:** GitHub Actions、GitHub Environment、OIDC、AWS CLI、EC2 Security Group、OpenSSH、Bash、Docker Compose、GHCR。

## Global Constraints

- 仅 main 成功发布的完整 40 位 SHA 可部署；不使用 latest、PR SHA 或 tag 自动部署。
- production 仅限 main，concurrency 为 myblog-production，cancel-in-progress 为 false。
- 不存储 runtime.env、数据库密码、AWS 长期密钥或日常 SSH 私钥。
- CD 安全组常态没有入站规则；原 SSH 规则和 3306/8080 边界不变。
- 每次 run 都撤销临时 /32；部署失败不自动回滚数据库 schema。
- Bash 脚本先写会失败的合约测试；测试仅使用临时目录与 stub 命令。
- 提交信息使用中文，每个提交只有一个目的。

---

## Task 1: 发布入口与合约测试

**Files:**
- Create: `deploy/cd/myblog-release`
- Create: `deploy/cd/myblog-cd-entrypoint`
- Create: `deploy/cd/test/release-contract-test.sh`

**Interfaces:**
- `myblog-release <sha>`：默认根目录 /opt/myblog-v2、环境文件 /etc/myblog-v2/runtime.env；测试用 MYBLOG_DEPLOY_ROOT、MYBLOG_RUNTIME_ENV、MYBLOG_COMPOSE_BIN 覆盖。
- `myblog-cd-entrypoint`：只接受 SSH_ORIGINAL_COMMAND 为 `deploy <sha>`，执行 `sudo /usr/local/sbin/myblog-release <sha>`。

- [ ] **Step 1: 写失败的测试**

测试创建 mktemp fake repo、runtime.env 与 PATH stub。定义合法 SHA 为 40 个 a，断言无效 SHA 不调用 git/docker；合法 SHA 的调用顺序必须包含：

```bash
git -C "$ROOT" fetch --prune origin
git -C "$ROOT" merge-base --is-ancestor "$SHA" origin/main
git -C "$ROOT" checkout --detach "$SHA"
compose config --quiet
compose pull
compose up -d --wait --wait-timeout 180
docker exec myblog-v2-api-1 curl --fail --silent http://127.0.0.1:8080/actuator/health
```

还要断言 runtime.env 仅改写 IMAGE_TAG 为 SHA；entrypoint 拒绝空命令、多参数、短 SHA 与非 deploy 前缀。

- [ ] **Step 2: 验证 RED**

Run: `bash deploy/cd/test/release-contract-test.sh`
Expected: FAIL，两个生产脚本尚不存在。

- [ ] **Step 3: 实现最小脚本**

myblog-release 使用 bash strict mode，要求 root、验证 SHA 正则和 runtime.env 0600 权限；以 deploy 用户执行 fetch、merge-base、checkout；以 root 执行 Compose。IMAGE_TAG 替换使用同目录临时文件和 `install -m 600 -o root -g root`，恰好一行 IMAGE_TAG，否则失败。

```bash
runuser -u deploy -- git -C "$ROOT" fetch --prune origin
runuser -u deploy -- git -C "$ROOT" merge-base --is-ancestor "$SHA" origin/main
runuser -u deploy -- git -C "$ROOT" checkout --detach "$SHA"
replace_image_tag "$RUNTIME" "$SHA"
"$COMPOSE" --env-file "$RUNTIME" config --quiet
"$COMPOSE" --env-file "$RUNTIME" pull
"$COMPOSE" --env-file "$RUNTIME" up -d --wait --wait-timeout 180
docker exec myblog-v2-api-1 curl --fail --silent http://127.0.0.1:8080/actuator/health
```

entrypoint 只接受两段命令、第一段 deploy、第二段匹配 `^[0-9a-f]{40}$`，其他情况 exit 64。

- [ ] **Step 4: 验证 GREEN 并提交**

Run:

```bash
bash deploy/cd/test/release-contract-test.sh
bash -n deploy/cd/myblog-release deploy/cd/myblog-cd-entrypoint
```

Expected: PASS。

```bash
git add deploy/cd/myblog-release deploy/cd/myblog-cd-entrypoint deploy/cd/test/release-contract-test.sh
git commit -m "新增受限 CD 发布脚本"
```

## Task 2: 服务器安装脚本

**Files:**
- Create: `deploy/cd/install-github-cd.sh`
- Modify: `deploy/cd/test/release-contract-test.sh`

**Interfaces:**
- `sudo ./install-github-cd.sh --public-key-file /secure/github-cd.pub` 安装 deploy 用户和入口。
- 生成的 authorized_keys 使用 `restrict,command="/usr/local/sbin/myblog-cd-entrypoint"`。
- sudoers 仅允许 `deploy ALL=(root) NOPASSWD: /usr/local/sbin/myblog-release *`。

- [ ] **Step 1: 写失败的安装测试**

增加断言：缺少 public-key-file 失败；非单行 ssh-ed25519/ssh-rsa 公钥失败；authorized_keys 包含 restrict 与 forced command；sudoers 只有 release 脚本；安装文件权限为 755 root:root，authorized_keys 为 600 deploy:deploy。

- [ ] **Step 2: 验证 RED**

Run: `bash deploy/cd/test/release-contract-test.sh`
Expected: FAIL，install-github-cd.sh 尚不存在。

- [ ] **Step 3: 实现安装脚本**

脚本必须创建 deploy（无 docker group、无通用 sudo），安装三个 root-owned 脚本，安装 sudoers 后运行 `visudo -cf`，并把 /opt/myblog-v2 所有权改为 deploy:deploy。公钥私钥不进入仓库。

- [ ] **Step 4: 验证 GREEN 并提交**

Run:

```bash
bash deploy/cd/test/release-contract-test.sh
bash -n deploy/cd/install-github-cd.sh
```

Expected: PASS。

```bash
git add deploy/cd/install-github-cd.sh deploy/cd/test/release-contract-test.sh
git commit -m "新增 CD 部署用户安装脚本"
```

## Task 3: GitHub 部署 job 与静态合约

**Files:**
- Create: `deploy/cd/test/workflow-contract-test.sh`
- Modify: `.github/workflows/images.yml`

**Interfaces:**
- GitHub Environment production secrets：PROD_SSH_PRIVATE_KEY、PROD_SSH_KNOWN_HOSTS。
- GitHub Environment variables：AWS_REGION、AWS_ROLE_ARN、CD_SECURITY_GROUP_ID、PROD_HOST、PROD_PORT、PROD_USER。
- deploy job 使用与 publish 相同的 github.sha。

- [ ] **Step 1: 写失败的 YAML 合约**

用 grep 断言 images.yml 含 deploy、needs publish、main 条件、environment production、concurrency group myblog-production、id-token write、configure-aws-credentials、authorize-security-group-ingress、revoke-security-group-ingress、if always、StrictHostKeyChecking=yes 和 deploy SHA 命令。

- [ ] **Step 2: 验证 RED**

Run: `bash deploy/cd/test/workflow-contract-test.sh`
Expected: FAIL，images.yml 尚无 deploy job。

- [ ] **Step 3: 添加 deploy job**

deploy job 使用：

```yaml
needs: publish
if: github.ref == 'refs/heads/main'
environment: production
concurrency:
  group: myblog-production
  cancel-in-progress: false
permissions:
  contents: read
  id-token: write
env:
  RELEASE_SHA: ${{ github.sha }}
```

步骤：checkout；OIDC；curl -4fsS https://api.ipify.org；AWS CLI 在 CD_SECURITY_GROUP_ID 加 TCP 22 的 current-ip/32；写私钥、known_hosts；以严格 host key 校验执行 `ssh ... deploy "$RELEASE_SHA"`；最后 `if: always()` 撤销同一 CIDR。撤销不依赖 SSH 成功。

- [ ] **Step 4: 验证 GREEN 并提交**

Run:

```bash
bash deploy/cd/test/workflow-contract-test.sh
git diff --check
```

Expected: PASS。

```bash
git add .github/workflows/images.yml deploy/cd/test/workflow-contract-test.sh
git commit -m "新增 GitHub SSH 自动部署工作流"
```

## Task 4: CD 文档与 AWS 配置清单

**Files:**
- Create: `docs/handbook/ops/github-ssh-cd.md`
- Modify: `docs/handbook/ops/README.md`
- Modify: `docs/handbook/ops/ci-cd.md`
- Modify: `docs/handbook/ops/deployment-direction.md`
- Modify: `docs/handbook/ops/production-runbook.md`
- Modify: `docs/handbook/ops/release-checklist.md`
- Modify: `deploy/cd/test/workflow-contract-test.sh`

- [ ] **Step 1: 写失败的文档合约**

扩展 workflow 合约，要求新手册有：GitHub OIDC provider、MyBlogGitHubCdRole、myblog-github-cd-ssh、production Environment、临时 /32、撤销、manual workflow_dispatch 演练、不自动数据库回滚。

- [ ] **Step 2: 验证 RED**

Run: `bash deploy/cd/test/workflow-contract-test.sh`
Expected: FAIL，新手册和 CD 运行边界不存在。

- [ ] **Step 3: 写手册并同步旧入口**

新手册给出 AWS OIDC provider、Role trust、独立 CD 安全组、GitHub Environment、部署密钥、服务器安装、首次演练、失败排查和回滚边界。旧文档将“无自动部署”改为 GitHub Actions 控制面加 AWS 临时安全组 API；生产拓扑仍为标准 Compose。

- [ ] **Step 4: 验证 GREEN 并提交**

Run:

```bash
bash deploy/cd/test/workflow-contract-test.sh
git diff --check
```

Expected: PASS。

```bash
git add docs/handbook/ops deploy/cd/test/workflow-contract-test.sh
git commit -m "补充 GitHub SSH CD 运维手册"
```

## Task 5: AWS、GitHub 与服务器首次演练

**Uses:**
- `deploy/cd/install-github-cd.sh`
- `docs/handbook/ops/github-ssh-cd.md`

- [ ] **Step 1: 配置 AWS**

创建 GitHub OIDC provider、MyBlogGitHubCdRole、独立 myblog-github-cd-ssh 安全组，并附加到当前 EC2。Role trust 仅允许 repo:tongchang01/My-Blog:environment:production；不修改现有 80/443 和维护者 SSH 规则。

- [ ] **Step 2: 配置 GitHub Environment**

创建 production，只允许 main；添加部署专用私钥与 known_hosts 为 secrets，区域、Role ARN、CD 安全组 ID、主机、端口、用户为 variables。

- [ ] **Step 3: 安装并拒绝测试**

服务器生成部署专用 ed25519 key；私钥仅上传 production Environment。运行：

```bash
sudo /usr/local/sbin/myblog-release not-a-sha; test "$?" -ne 0
sudo -u deploy SSH_ORIGINAL_COMMAND='deploy not-a-sha' /usr/local/sbin/myblog-cd-entrypoint; test "$?" -ne 0
```

- [ ] **Step 4: 首次真实演练**

手动触发 images.yml 到当前已验证 main SHA。确认临时安全组仅短暂出现 Runner /32 的 TCP 22；服务器 Git HEAD 与 IMAGE_TAG 一致；三个服务 healthy；API health UP；临时规则已撤销。

- [ ] **Step 5: 记录结果**

把 run URL、SHA、时间、撤销结果与异常写入私有台账。演练通过后，后续 main push 视为自动部署。
