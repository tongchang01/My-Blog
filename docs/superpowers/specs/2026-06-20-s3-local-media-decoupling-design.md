# S3 与本地媒体映射解耦设计

## 1. 目标

附件上传后端切换为 S3 时，默认不装配本地 `/media/**` 静态资源映射；LOCAL
模式保持现有行为。迁移期间如仍需访问历史 LOCAL 附件，可以通过独立显式开关
临时保留映射。

本次只调整 Web 资源映射装配，不修改上传流程、`StorageServiceRegistry`、附件表、
已有记录的 `storageType` 路由或公开 URL。

## 2. 方案选择

### 方案 A：独立开关并按当前存储类型提供默认值（采用）

新增 `myblog.storage.local.web-enabled`：

- 未显式配置时，`myblog.storage.type=LOCAL` 默认启用；
- 未显式配置时，`myblog.storage.type=S3` 默认禁用；
- S3 迁移期可以显式设置为 `true`，继续服务历史 LOCAL URL；
- LOCAL 也可以显式设置为 `false`，适用于由外部静态服务器提供本地文件的部署。

该方案同时满足安全默认值和历史数据迁移需求，不把 `/media/**` 永久绑定到 S3。

### 方案 B：只按 `storage.type=LOCAL` 装配

实现最简单，但切换 S3 后无法继续访问历史 LOCAL 附件，不适合渐进迁移。

### 方案 C：始终装配

保持现状，会继续暴露无效本地入口，不能解决审查发现的装配耦合。

## 3. 装配设计

增加本地媒体映射条件，由 Spring `Environment` 读取配置：

1. 若 `myblog.storage.local.web-enabled` 已显式设置，使用该布尔值；
2. 否则读取 `myblog.storage.type`，仅 `LOCAL` 返回 true；
3. 值为空时沿用当前默认存储类型 `LOCAL`；
4. 非法布尔值交由配置绑定或条件测试暴露，不静默当作 true。

`LocalStorageWebConfiguration` 只在该条件成立时成为 Bean，并注册 `/media/**`
到 `myblog.storage.local.root`。S3 客户端、Bucket Policy 和 CloudFront 配置均不受
影响。

## 4. 配置与文档

在通用配置中声明新开关及其按存储类型推导的默认语义。`local` profile 保持 LOCAL
行为，不要求用户增加环境变量。S3 部署默认没有本地映射；只有迁移历史 LOCAL
附件时才显式设置：

```text
MYBLOG_STORAGE_LOCAL_WEB_ENABLED=true
```

附件契约和发布检查需要说明：S3 模式不得依赖 `/media/**`；开启兼容开关时必须
确保本地目录真实存在，并制定历史附件迁移和关闭开关的时间点。

## 5. 测试

使用轻量 Spring application context 测试真实条件装配：

- LOCAL 且未设置开关：存在 `LocalStorageWebConfiguration`；
- S3 且未设置开关：不存在该配置 Bean；
- S3 且显式开启：存在该配置 Bean；
- LOCAL 且显式关闭：不存在该配置 Bean。

保留现有 LOCAL 资源处理行为测试。局部验证通过后运行配置、存储、安全和架构相关
测试；阶段结束运行 `mvn clean test`。

## 6. 提交边界

本任务单独提交，只包含条件装配、配置属性、对应测试与附件/发布文档同步。不顺带
修改 S3 上传、历史附件迁移程序、SecurityFilterChain 或生产代理配置。
