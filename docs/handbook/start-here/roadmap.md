# 路线图

> 状态：当前有效
> 适用范围：MyBlog V2 后续实施顺序
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/`
> 权威程度：路线图

## 1. 补齐本地初始化的 reset 合约覆盖

- 以 PowerShell 7+ 作为唯一脚本运行时；Windows 与 Ubuntu CI 合约验证已通过。
- 扩充合约测试以覆盖显式 `-Reset`，并在 CI 中固定该场景。
- reset 合约通过后关闭 ISSUE-001。

## 2. 完成可回滚生产发布

- 确认服务器、域名、TLS、反向代理、MySQL 和 S3。
- 校准生产环境变量，完成真实 MySQL 测试和备份恢复。
- 建立手动构建、上传、迁移、切换、health、冒烟和回滚步骤。
- 手动链路稳定后再评估自动 CD。

## 3. 产品增量

优先级由实际使用需求决定：

1. PASSWORD 文章独立解锁授权。
2. 留言板博客端页面。
3. Spotify Embed 与分享/搜索分发能力。

## 4. 触发式架构扩展

- 多人或更高安全要求触发 HttpOnly Cookie 与 CSRF 设计。
- 多实例部署触发共享限流与运行协调。
- 明确搜索收录目标后再实现 sitemap、RSS/Atom、Open Graph 和结构化数据。
- 有稳定手动发布、密钥和回滚机制后再实现 CD。

具体完成条件见 `open-issues.md`。
