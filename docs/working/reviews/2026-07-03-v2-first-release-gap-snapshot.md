# V2 距首版上线差距快照

> 状态：初稿
> 适用范围：MyBlog V2 上线准备
> 最后校准：2026-07-03
> 权威程度：整理过程材料（时间点快照，不长期跟踪）
> 关联文档：[current-status.md](../../handbook/start-here/current-status.md)、[open-issues.md](../../handbook/start-here/open-issues.md)、[roadmap.md](../../handbook/start-here/roadmap.md)、[release-checklist.md](../../handbook/ops/release-checklist.md)、[ci-cd.md](../../handbook/ops/ci-cd.md)

## 本文档回答什么问题

回答一个问题：截至 2026-07-03，MyBlog V2 距离"第一版部署到生产 + CD 走通"还差多少。

本文档只是一个时间点快照，用于在启动 CD 测试之前对齐范围。不作为长期跟踪。**权威源仍是 `open-issues.md` 和 `roadmap.md`**；如果本文档与它们不一致，以它们为准。

## 大盘判断

- 后端：只欠一处安全裁剪，其他都可延后。轻。
- 前台：欠 3~4 个页面。中。
- 运维 / CD：整个 [release-checklist.md](../../handbook/ops/release-checklist.md) 除通用测试外基本从零，是本次最重的一块，也是接下来要重点测试的部分。

## 后端

- **必须先做**：[O-002](../../handbook/start-here/open-issues.md#o-002-demo-敏感字段裁剪边界) DEMO 敏感字段裁剪（P1）。属于安全边界，不能拖到上线后。
- **可上线后补**：
  - [O-001](../../handbook/start-here/open-issues.md#o-001-password-文章完整解锁流程) PASSWORD 完整解锁——首版保持锁定态占位即可。
  - [O-019](../../handbook/start-here/open-issues.md#o-019-评论和留言迁移到-v2-自研-api) 评论迁移。
  - [O-020](../../handbook/start-here/open-issues.md#o-020-访问统计前台打点和展示口径) 前台统计打点接入。
- **无阻塞的功能层**：M1/M2/M3 已完成，六大模块第一版已交付。

## 前台 blog

已接入 V2 的部分：首页、公开文章列表、文章详情、公开分类、公开标签。

**还差 3~4 个页面**（属 [O-003](../../handbook/start-here/open-issues.md#o-003-前台读者主链路补齐) 及分解）：

- 归档 —— [O-016](../../handbook/start-here/open-issues.md#o-016-公开归档时间线接口缺失)。后端归档时间线接口也未做，需要一起补。
- 关于 —— [O-018](../../handbook/start-here/open-issues.md#o-018-关于页仍依赖旧-page-json)。后端 `aboutMd` 已就绪，前台改数据源即可。
- 搜索 —— [O-017](../../handbook/start-here/open-issues.md#o-017-搜索实现方式与前后端能力不一致)。走 `?keyword=`，不做静态索引。
- 友链前台 —— `open-issues.md` 里已下调优先级，是否延后需要单独裁决。

**可上线后补**：评论 / 留言 / 统计前台接入（[O-004](../../handbook/start-here/open-issues.md#o-004-前台评论留言和统计接入)），是否与"首版"标准挂钩要拍板。

## 后台 admin

功能层主要业务页已完成。剩下的：

- [O-002](../../handbook/start-here/open-issues.md#o-002-demo-敏感字段裁剪边界) DEMO 敏感字段裁剪（与后端同一条）。
- O-012 已关闭，不再阻塞。

## 运维与发布（最重的一块）

对应 [O-007](../../handbook/start-here/open-issues.md#o-007-上线前-seo发布和运维准备) 与 [release-checklist.md](../../handbook/ops/release-checklist.md) 的 9 节清单。除"通用检查"里已跑通的自动化测试之外，其余基本都还没做：

- **CD 从零**。[ci-cd.md](../../handbook/ops/ci-cd.md) 明确写"当前只做 CI，不做 CD"，CD 等部署拓扑定了再设计。要"测试 CD"就要先把这套设计出来（部署目标、密钥管理、回滚方式、数据库迁移策略、发布审批）。
- SEO：`<title>` / description / canonical / OG、`robots.txt`、`sitemap.xml`、RSS / Atom。
- 生产环境变量权威清单、生产 CORS、trusted proxies、JWT secret。
- 反向代理 / 客户端 IP 真实拓扑验证。
- 数据库备份策略 + 至少一次恢复演练。
- 附件存储在生产存储类型下的验证。
- 上线冒烟。

## 已明确不阻塞的项

- [O-008](../../handbook/start-here/open-issues.md#o-008-后台-token-存储方式升级) 后台 token localStorage —— 已接受风险。
- [O-009](../../handbook/start-here/open-issues.md#o-009-多实例部署下限流方案) 多实例限流 —— 单实例前提已确认。
- 三份代码审查 review —— 已按 [2026-07-03-backend-v2-review-dimensions.md](2026-07-03-backend-v2-review-dimensions.md) 统一延后到上线 + CD 走通 + 稳定观察窗口后再启动整改。

## 推荐推进顺序

按依赖和风险从低到高：

1. 后端做完 O-002 DEMO 裁剪。
2. 前台补 3~4 页（归档、关于、搜索；友链看是否延后）；同步补后端 O-016 归档接口。
3. 设计 CD（部署目标、密钥、回滚、迁移策略、审批）。
4. 补 SEO / RSS / Sitemap / robots。
5. 环境变量清单 + 备份演练 + 反向代理 / IP 验证。
6. 首版部署。
7. CD 走一轮完整的"构建 → 部署 → 回滚"演练。
8. 稳定观察窗口。
9. 回来启动三份 code review 的整改。

## 待裁决

- 友链前台是否延后到首版之后？（`open-issues.md` 已标"下调优先级"，未终裁。）
- O-004 前台评论 / 留言 / 统计是否算入首版？如果算入，路径 3~9 之间要插入 R3 相关工作。
- O-001 PASSWORD 完整解锁是否首版就要（锁定态占位不算"完整"）。
