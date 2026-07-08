# 路线图

> 状态：当前有效
> 适用范围：MyBlog V2 后续开发
> 最后校准：2026-07-08
> 对应代码：`MyBlog-springboot-v2/`、`frontend/apps/blog/`、`frontend/apps/admin/`
> 权威程度：路线图

## 当前主线

当前主线已经从“前台旧数据源清理”转入“第一版上线准备”：

1. 上线部署硬项准备：服务器信息、S3、环境变量、反向代理、备份和冒烟。
2. 手动部署跑通后再设计 CD。
3. 第一版发布后再做留言板评论、PASSWORD 完整解锁、SEO/RSS/Sitemap/Open Graph 等增强项。

## 已完成

- 后端 V2 六大模块第一版。
- 后台 admin 主要业务闭环。
- 前台 blog 主阅读链路：首页、列表、详情、分类、标签、归档、关于、搜索、友链、作者资料、访问统计、文章评论。
- 前台已确认的旧 Aurora/Hexo 活跃数据源清理：作者 JSON、旧统计 JSON、旧友链 page、通用 page、旧第三方评论、旧分类/标签包装 store、旧 `api/index.ts`。
- CI 已覆盖后端 MySQL 测试和前端基础验证。

## 第一版上线准备

目标：先把个人博客第一版可靠部署上线，不提前扩展复杂 CD 或多实例架构。

- [ ] 服务器现状记录。
- [ ] 生产环境变量权威清单。
- [ ] S3 附件存储生产校准。
- [ ] Nginx / systemd 草案。
- [ ] 数据库备份和恢复演练记录。
- [ ] 手动部署步骤和回滚路径。
- [ ] 公开前台、后台登录、健康检查冒烟。
- [ ] 手动部署跑通后再设计 CD。

关联 open issues：O-007。

## 第一版后置

- O-001 PASSWORD 文章完整解锁流程。
- O-004 / O-019 留言板评论前台接入。
- 完整 SEO / RSS / Sitemap / Open Graph / 结构化数据。
- Spotify Embed。
- Markdown chunk 分包和 Sass 旧 API / `@import` 清理。
- 后台筛选组件体验扩展。
- 多实例限流、HttpOnly Cookie 等部署或安全增强。
