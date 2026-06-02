# 路线图

> 本文档回答："接下来要干什么？短期 / 中期 / 长期分别做什么？"
> 性质：滚动更新，每完成一个里程碑就调整。
> 当前日期：2026-06

## 1. 短期（1~2 个月内）

聚焦：**V2 既有模块的稳定与补齐**。

### S1：修历史遗留
- [ ] `ContentCatalogMapper` 中 @Select 长查询迁移到 XML（参考 `workflows/migrate-jdbc-to-mybatis-plus.md`）
- [ ] 创建 `src/main/resources/mapper/content/` 目录

### S2：补关键测试
- [ ] `CommentCommandService` 集成测试
- [ ] `AdminCommentCommandService` 集成测试
- [ ] 评论软删除 → 恢复完整链路 E2E 测试
- [ ] content 模块 ApplicationService 集成测试

### S3：建 system 模块
- [ ] 按 `workflows/add-new-module.md` 建包结构
- [ ] 迁移系统配置 / 字典等表
- [ ] 更新 ArchUnit 规则纳入 system

### S4：Bearer Token 解析公共化
- [ ] 抽出公共工具，消除多处重复

## 2. 中期（3~6 个月）

聚焦：**安全加固与可运维性**。

### M1：富文本与上传安全
- [ ] 富文本 XSS 清洗（Jsoup 或 OWASP HTML Sanitizer）
- [ ] 上传文件 MIME 校验、大小限制
- [ ] **必须**在迁移文章模块前完成

### M2：登录限流
- [ ] ADR 决定方案（IP / 用户名 / 验证码组合）
- [ ] 实现限流组件
- [ ] 失败次数到达阈值后冷却

### M3：Redis 引入（如确实需要）
- [ ] 写 ADR 论证必要性
- [ ] 引入 Redis
- [ ] `TokenRevocationStore` 迁到 Redis
- [ ] 考虑在线用户管理 / 踢下线 / 设备管理

### M4：监控与日志
- [ ] Actuator 端点暴露（注意安全）
- [ ] 关键业务日志结构化
- [ ] 考虑接入轻量监控（如 Prometheus + Grafana，视部署规模）

### M5：CI/CD
- [ ] GitHub Actions 跑 `mvn test`
- [ ] PR 必须通过 ArchUnit + 单元测试
- [ ] 可选：构建产物自动部署到测试环境

## 3. 长期（6 个月以上）

聚焦：**V1 下线 + 性能/扩展性**。

### L1：V1 完整迁移
- [ ] 全部业务迁到 V2
- [ ] V1 与 V2 共用数据库期间的过渡稳定
- [ ] V1 下线计划（流量切换、回滚预案）
- [ ] V1 代码归档（保留分支，主分支移除）

### L2：技术债清零
- [ ] 全部 JdbcTemplate 替换为 MyBatis-Plus
- [ ] 包名 v2 后缀去除（需 ADR 取代 ADR-0002）

### L3：性能优化
- [ ] 评估热点查询，加索引或加缓存
- [ ] 评估前后端分离 + CDN 静态资源
- [ ] 考虑读写分离（视访问量）

### L4：可选增强
- [ ] 第三方登录（OAuth2）
- [ ] IP 归属地（视隐私权衡）
- [ ] 文章/评论全文搜索（先试 MySQL 全文索引，不够再考虑 ES）

## 4. 永远不做（或需充分论证才做）

- 拆微服务（个人博客规模不匹配）
- 引入复杂消息中间件
- 多语言 / i18n（无需求）
- 大型前后端框架重写（前端 Vue 暂稳定）

## 5. 里程碑节奏建议

按"修历史 → 补能力 → 拓边界"循环：

```
S1 + S2 完成
    ↓
S3 + S4 启动
    ↓
M1 完成（解锁文章模块迁移）
    ↓
M3 完成（解锁多实例部署）
    ↓
L1 启动
```

## 6. 与文档的同步

每完成一个里程碑：
- 更新 `status.md`
- 涉及决策的写 ADR
- 涉及规则变更的改 `rules/`
- 涉及架构变更的改 `arch/`

## 7. 相关文档

- 当前进度：`status.md`
- 已知问题：`pitfalls.md`
- V1 对比：`v1-vs-v2.md`
- 操作 SOP：`workflows/`
