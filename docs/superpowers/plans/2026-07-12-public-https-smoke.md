# 公网 HTTPS 冒烟 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 GitHub Actions 部署同一 SHA 后验证三个公开 HTTPS 入口可达，并保持失败时临时 SSH 规则仍会撤销。

**Architecture:** 复用 Caddy 已有的匿名 `/healthz` 路由，不增加后端接口或依赖。deploy job 在 SSH 发布成功后由 GitHub Hosted Runner 顺序 curl 三个公开 URL；任何请求失败即失败，而既有 `if: always()` 撤销步骤保持原样。

**Tech Stack:** GitHub Actions、Bash、curl、现有 workflow contract test、Markdown。

## Global Constraints

- 只验证 `tong-yibin.com`、`www.tong-yibin.com`、`admin.tong-yibin.com` 的 `/healthz`。
- 每个请求使用 20 秒最大时长，不添加重试、浏览器、登录或写接口。
- 冒烟失败必须仍执行既有临时 SSH `/32` 撤销步骤。
- `aws-actions/configure-aws-credentials@v5` 不在本次升级；只在文档记录 Node 24 升级为低优先级待办。

---

### Task 1: 部署后公网冒烟与工作流契约

**Files:**
- Modify: `.github/workflows/images.yml`
- Modify: `deploy/cd/test/workflow-contract-test.sh`

**Interfaces:**
- Consumes: deploy job 的 `Deploy same SHA` 与 `Revoke temporary SSH ingress` 步骤。
- Produces: 一个名为 `Smoke test public HTTPS` 的 deploy job 步骤；三个 URL 均必须通过 curl。

- [ ] **Step 1: 写入失败的工作流契约断言**

在 `deploy/cd/test/workflow-contract-test.sh` 的既有 `require_line` 断言后加入：

```bash
require_line 'Smoke test public HTTPS'
require_line 'https://tong-yibin.com/healthz'
require_line 'https://www.tong-yibin.com/healthz'
require_line 'https://admin.tong-yibin.com/healthz'
require_line '--max-time 20'
```

- [ ] **Step 2: 运行测试并确认失败**

在具备 Bash 的临时目录运行：

```bash
bash deploy/cd/test/workflow-contract-test.sh .github/workflows/images.yml
```

预期：失败并提示缺少 `Smoke test public HTTPS` 工作流契约。

- [ ] **Step 3: 在 deploy job 增加最小 curl 步骤**

把下列步骤放在 `Deploy same SHA` 后、`Revoke temporary SSH ingress` 前：

```yaml
      - name: Smoke test public HTTPS
        run: |
          for url in \
            https://tong-yibin.com/healthz \
            https://www.tong-yibin.com/healthz \
            https://admin.tong-yibin.com/healthz; do
            curl --fail --silent --show-error --max-time 20 --output /dev/null "$url"
          done
```

- [ ] **Step 4: 运行测试并确认通过**

再次运行：

```bash
bash deploy/cd/test/workflow-contract-test.sh .github/workflows/images.yml
```

预期：输出 `workflow contract: PASS`。

- [ ] **Step 5: 提交工作流与契约**

```bash
git add .github/workflows/images.yml deploy/cd/test/workflow-contract-test.sh
git commit -m "增加公网 HTTPS 冒烟检查"
```

### Task 2: 更新部署边界与低优先级待办

**Files:**
- Modify: `docs/handbook/ops/github-ssh-cd.md`
- Modify: `docs/handbook/ops/ci-cd.md`

**Interfaces:**
- Consumes: Task 1 的三个 URL、20 秒超时与失败行为。
- Produces: 运维者可理解的公网覆盖范围，以及 Node 24 action 升级的低优先级记录。

- [ ] **Step 1: 更新 CD 运维手册**

在首次演练验收项中增加：部署后 Runner 对三个公开 `/healthz` 地址执行 20 秒 curl；该检查失败会标红部署，但既有 `if: always()` 仍撤销临时 SSH 规则。

- [ ] **Step 2: 更新 CI/CD 边界说明**

补充 deploy job 已覆盖三域名公网 HTTPS 可达性；同时登记 `aws-actions/configure-aws-credentials@v5` 的 Node 20 弃用提示为低优先级升级到 v6 的待办，不设置 Node 20 回退环境变量。

- [ ] **Step 3: 复跑工作流契约**

```bash
bash deploy/cd/test/workflow-contract-test.sh .github/workflows/images.yml
```

预期：输出 `workflow contract: PASS`。

- [ ] **Step 4: 提交文档**

```bash
git add docs/handbook/ops/github-ssh-cd.md docs/handbook/ops/ci-cd.md
git commit -m "补充公网冒烟与 Node 升级待办"
```
