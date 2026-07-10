# ADR-0004：五个业务模块与 common 基础设施

> 状态：当前有效
> 适用范围：V2 后端模块边界
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/`
> 权威程度：ADR

## 背景

模块过粗会重新形成全局 Service，模块过细会增加跨模块编排。访问统计与内容生命周期不同，公共安全和存储能力又不属于业务域。

## 决策

业务域固定为：

- `identity`：账号、认证和资料。
- `content`：文章、分类和标签。
- `comment`：评论、留言和通知。
- `system`：站点配置、附件和友链。
- `stats`：访问明细、聚合和看板。

`common` 承载跨模块基础设施，文档可称 `common-infra`，但不视为业务域。

## 结果

V1 的动态菜单、数据库字典、操作日志表、Quartz 管理、相册、说说和音乐库不进入这些模块。完整职责和允许依赖见 `../architecture/module-map.md`。
