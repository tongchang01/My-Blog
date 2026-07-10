# 代码注释规则

> 状态：当前有效
> 适用范围：V2 生产代码、测试、配置和 OpenAPI
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/`、`frontend/apps/blog/src/`、`frontend/apps/admin/src/`
> 权威程度：规则

- 业务注释、Javadoc、测试场景说明和 OpenAPI 描述使用简洁中文。
- 技术名词、类名、配置键、HTTP header、JWT claim、数据库字段和协议固定值保留英文。
- 注释解释业务边界、设计原因、信任边界和并发顺序，不逐句翻译代码。
- 公开类、关键 application 用例、端口接口和不易从命名判断语义的方法应有 Javadoc。
- 权限裁剪、状态流转、软删除恢复、行锁、限流、token 撤销、代理 IP、上传校验和内容清洗需要说明原因。
- 复杂 XML SQL 的注释说明筛选状态、聚合口径、排序和锁定目的。
- DTO getter/setter、机械映射、简单构造器和无分支样板不需要注释。
- 不保留过期 TODO，不在注释中放真实账号、密码、token、密钥或生产连接信息。
- OpenAPI 描述不能替代业务规则，也不能与 `../api/` 契约冲突。

评审标准是信息是否能防止误改，而不是注释数量。
