# 公网 HTTPS 冒烟设计

> 状态：已确认，待实现
> 范围：生产镜像发布工作流的 deploy job

## 目标

在受限 SSH 部署完成后，从 GitHub Hosted Runner 对三个用户入口执行最小公网可达性验证，避免“服务器容器 healthy、但 DNS、TLS、443 或 Caddy 路由不可用”仍显示绿色。

## 非目标

- 不使用浏览器、Playwright 或第三方监控。
- 不登录管理端、不调用写接口、不验证业务数据或 S3。
- 不重试，不掩盖持续的公网故障。
- 本次不升级 `aws-actions/configure-aws-credentials@v5`；仅在运维文档登记该 Node 24 升级为低优先级待办。

## 工作流行为

在 `Deploy same SHA` 成功后、`Revoke temporary SSH ingress` 前增加一个步骤：依次请求以下 URL 的 `/healthz`，每次限制 20 秒，并要求 curl 以成功 HTTP 状态结束：

1. `https://tong-yibin.com/healthz`
2. `https://www.tong-yibin.com/healthz`
3. `https://admin.tong-yibin.com/healthz`

任意请求失败即让 deploy job 失败。撤销步骤继续使用既有 `if: always()` 条件，因此即使公网冒烟失败，也会撤销本次 Runner 的临时 SSH `/32`。

## 验证与文档

- 先扩展现有 `workflow-contract-test.sh`，断言三个 URL、20 秒超时与撤销步骤的 `if: always()` 都存在；变更前测试必须失败。
- 更新 CD 运维手册与 CI/CD 说明，明确公网冒烟覆盖的边界，并登记 Node 24 action 升级待办。
- 手动 `workflow_dispatch` 验收一次，确认三个 URL 均可达；失败时应保留可读 curl 错误且临时安全组规则仍被撤销。
