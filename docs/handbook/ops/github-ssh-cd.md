# GitHub SSH 自动部署

> 状态：已启用；2026-07-12 首次真实演练通过
> 适用范围：当前唯一生产 EC2、GitHub Actions、GHCR、Docker Compose

## 机制与边界

main 的镜像发布成功后，deploy job 使用 GitHub OIDC 获得 AWS 短期凭据，将当前 Runner IPv4 的临时 /32 加入独立 CD SSH 安全组，再以 production Environment 的部署专用私钥调用 deploy 用户。工作流结束时撤销该规则。

这不是 SSM、ECS 或 ECR；运行拓扑仍是标准 Docker Compose，镜像仍在 GHCR。不会开放 3306/8080、不会保存 runtime.env 或长期 AWS 密钥、不会自动回滚数据库。

## GitHub OIDC provider 与 MyBlogGitHubCdRole

IAM → Identity providers → Add provider：

| 字段 | 值 |
| --- | --- |
| Provider type | OpenID Connect |
| Provider URL | https://token.actions.githubusercontent.com |
| Audience | sts.amazonaws.com |

如 provider 已存在则复用。创建 MyBlogGitHubCdRole 时，trust policy 必须只允许 production Environment：

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::<AWS_ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        "token.actions.githubusercontent.com:sub": "repo:tongchang01/My-Blog:environment:production"
      }
    }
  }]
}
```

替换 AWS_ACCOUNT_ID。权限仅授予 DescribeSecurityGroups、DescribeSecurityGroupRules、AuthorizeSecurityGroupIngress、RevokeSecurityGroupIngress；写操作 Resource 限定为 CD 安全组 ARN，Condition 限定当前 VPC。不要给该 Role EC2 全权限或长期 Access Key。

VPC 条件值必须是 **VPC ARN**，不能只填写 `vpc-...`。最小权限策略的写权限部分如下；将三个占位符替换为实际值：

```json
{
  "Effect": "Allow",
  "Action": [
    "ec2:AuthorizeSecurityGroupIngress",
    "ec2:RevokeSecurityGroupIngress"
  ],
  "Resource": "arn:aws:ec2:<AWS_REGION>:<AWS_ACCOUNT_ID>:security-group/<CD_SECURITY_GROUP_ID>",
  "Condition": {
    "StringEquals": {
      "ec2:Vpc": "arn:aws:ec2:<AWS_REGION>:<AWS_ACCOUNT_ID>:vpc/<VPC_ID>"
    }
  }
}
```

## 独立 myblog-github-cd-ssh 安全组

创建名为 myblog-github-cd-ssh 的安全组，使用当前实例 VPC，不添加常驻入站规则，保留默认出站规则。将它附加到生产实例，但不修改 default 安全组的 80/443 与维护者 SSH 规则。

记录该组 ID。每次 deploy 只在这个组中临时加入 TCP 22、Runner IPv4 的 /32；结束后撤销。

## GitHub production Environment

Settings → Environments → New environment，创建 production：

1. Deployment branches 仅允许 main；
2. 不设 required reviewers，实现 main 合并后自动部署；
3. Environment secrets：

| Secret | 值 |
| --- | --- |
| PROD_SSH_PRIVATE_KEY | 部署专用 ed25519 私钥全文 |
| PROD_SSH_KNOWN_HOSTS | 已人工核对的生产 SSH host key 行 |

4. Environment variables：

| Variable | 值 |
| --- | --- |
| AWS_REGION | ap-northeast-1 |
| AWS_ROLE_ARN | MyBlogGitHubCdRole 完整 ARN |
| CD_SECURITY_GROUP_ID | myblog-github-cd-ssh ID |
| PROD_HOST | 生产 Elastic IP 或主机名 |
| PROD_PORT | 22 |
| PROD_USER | deploy |

GitHub Secrets 不应被 echo 或转换后打印。

## 服务器安装

以维护者 SSH 登录，生成新部署专用密钥，不能复用个人私钥：

```bash
umask 077
ssh-keygen -t ed25519 -f /home/ec2-user/myblog-github-cd -C myblog-github-cd
cat /home/ec2-user/myblog-github-cd.pub
sudo /opt/myblog-v2/deploy/cd/install-github-cd.sh --public-key-file /home/ec2-user/myblog-github-cd.pub
sudo /usr/local/sbin/myblog-release not-a-sha; test "$?" -ne 0
sudo -u deploy SSH_ORIGINAL_COMMAND='deploy not-a-sha' /usr/local/sbin/myblog-cd-entrypoint; test "$?" -ne 0
shred -u /home/ec2-user/myblog-github-cd /home/ec2-user/myblog-github-cd.pub
```

私钥只保存到 PROD_SSH_PRIVATE_KEY。安装后 /opt/myblog-v2 归 deploy 所有；deploy 不加入 docker group，也没有通用 sudo。

从可信现有 SSH 连接核对 host key 指纹后，生成 known_hosts：

```bash
ssh-keyscan -H <PROD_HOST> > /tmp/myblog-known-hosts
cat /tmp/myblog-known-hosts
```

将核对后的内容保存为 PROD_SSH_KNOWN_HOSTS，再删除临时文件。

## workflow_dispatch 首次演练

在 GitHub Actions 的 Publish container images 中选择 main 并 Run workflow。验收：

1. publish 与 deploy SHA 一致；
2. CD 安全组仅在运行期间出现 TCP 22 的临时 /32；
3. /opt/myblog-v2 的 HEAD 与 runtime.env IMAGE_TAG 一致；
4. mysql、api、web 均 healthy，API health 为 UP；
5. run 结束后 CD 安全组没有入站规则。

```bash
sudo -u deploy git -C /opt/myblog-v2 rev-parse HEAD
sudo docker compose --env-file /etc/myblog-v2/runtime.env ps
sudo docker exec myblog-v2-api-1 curl --fail --silent http://127.0.0.1:8080/actuator/health
```

首次真实演练于 2026-07-12 通过：工作流 `29195240729` 的 publish 与 deploy 均使用 `85df0d4b0dad967aed1d915bdd619594bda43e85`；OIDC、临时 SSH /32 放行、受限 SSH、部署和撤销步骤均成功，三个容器均为 healthy。

## 失败与手工撤销

OIDC 失败时核对 trust 的仓库/environment 条件、Role ARN、区域和安全组 ID。SSH 失败时确认临时 /32、deploy 公钥与 known_hosts。发布失败时查看 GitHub run 与 Docker Compose 日志；Flyway 或数据错误时停止，不自动回滚数据库。

若撤销失败，从 job 日志取得 Runner CIDR，在 AWS 控制台删除 myblog-github-cd-ssh 中该 TCP 22 规则；或在受控管理员终端执行：

```bash
aws ec2 revoke-security-group-ingress \
  --region ap-northeast-1 \
  --group-id <CD_SECURITY_GROUP_ID> \
  --ip-permissions 'IpProtocol=tcp,FromPort=22,ToPort=22,IpRanges=[{CidrIp=<RUNNER_CIDR>}]'
```

确认独立安全组没有入站规则。不要删除 default 安全组中的维护者 SSH 规则。
