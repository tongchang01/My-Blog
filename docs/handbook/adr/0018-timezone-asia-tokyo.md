# ADR-0018：运行时统一使用 Asia/Tokyo

> 状态：当前有效
> 适用范围：V2 后端、数据库连接和博客时间展示
> 最后校准：2026-07-10
> 对应代码：`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/common/config/`、`MyBlog-springboot-v2/src/main/resources/application*.yml`、`frontend/apps/blog/src/shared/time/`
> 权威程度：ADR

## 背景

数据库使用无时区的 `DATETIME`，业务又包含定时发布、统计日期和多语言展示。各层使用不同默认时区会产生错日和错时。

## 决策

- 后端 JVM 必须通过 `-Duser.timezone=Asia/Tokyo` 启动，启动校验器拒绝其他默认时区。
- 业务时间从 `TimeConfig` 提供的 `Clock` 获取，domain 禁止直接调用 `LocalDateTime.now()`。
- Jackson 以 `Asia/Tokyo` 和 `yyyy-MM-dd'T'HH:mm:ss` 序列化本地日期时间。
- 本地 MySQL 默认连接 URL 设置 `connectionTimeZone=Asia/Tokyo`、`forceConnectionTimeToSession=true` 和 `time_zone=+09:00`；生产连接 URL由部署环境提供并承担同等配置责任。
- schema 使用 `DATETIME`，不依赖数据库隐式时区转换。
- 博客端把后端本地日期时间按 `Asia/Tokyo` 解析并格式化。

## 结果

定时发布、日统计和页面时间在同一时区语义下运行。部署检查必须同时覆盖 JVM 参数和数据库连接参数。
