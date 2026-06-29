# ADR-0018: 时区统一 Asia/Tokyo（五层模型）

- 状态：accepted
- 日期：2026-06
- 决策者：项目负责人
- 依赖：ADR-0015（时间列用 DATETIME）

## 背景

时间处理是分布式系统最容易出错的地方之一。常见反模式：

- JVM 用一个时区，MySQL session 用另一个时区，数据写进 DB 后被悄悄转换
- 业务代码散落 `LocalDateTime.now()` / `new Date()`，无法在测试中替换时间
- 前端拿到时间字符串，按浏览器本地时区解析，与业务期望不一致
- `TIMESTAMP` 列在 session 时区切换时被自动转换，导致历史数据"漂移"

V2 目标受众在日本（SES 作品集），用户不分布全球。早期定一个清晰的时区基线，比将来出问题再修便宜得多。

## 决定

**V2 所有时间相关处理统一 Asia/Tokyo（UTC+9）**，五层一致：

| 层 | 配置 / 做法 |
|---|---|
| JVM | 启动参数 `-Duser.timezone=Asia/Tokyo` |
| MySQL session | DataSource URL 设置 `connectionTimeZone=Asia/Tokyo`，并通过 `sessionVariables=time_zone='+09:00'` 强制 session 时区 |
| 应用层 | 注入 `Clock.system(ZoneId.of("Asia/Tokyo"))`；🔴 禁止散落 `LocalDateTime.now()` 直接调用 |
| API 返回 | `LocalDateTime` 序列化为 ISO-8601 **不带时区后缀**（如 `"2026-06-03T14:30:00"`），按 JST 语义 |
| 前端展示 | 统一按 JST 渲染；日/英 UI 显示日期格式分别按 ja-JP / en-US 区域设置（但时区固定 JST） |

## 落地细节

### JVM

```yaml
# docker-compose / k8s 启动参数
JAVA_OPTS: "-Duser.timezone=Asia/Tokyo"
```

### MySQL 连接 URL

```
jdbc:mysql://host:3306/myblog?connectionTimeZone=Asia/Tokyo&forceConnectionTimeToSession=true&sessionVariables=time_zone='%2B09:00'&characterEncoding=utf8
```

### Spring `Clock` Bean

```java
@Configuration
public class TimeConfig {
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Tokyo"));
    }
}
```

业务代码取时间：

```java
// ✅ 正例
LocalDateTime now = LocalDateTime.now(clock);

// ❌ 反例（R-011 红线）
LocalDateTime now = LocalDateTime.now();
new Date();
System.currentTimeMillis();
```

### SCHEDULED 任务 SQL

不用 DB `NOW()`（受 session 时区影响），由应用层传入 `LocalDateTime now = LocalDateTime.now(clock)` 作为 `#{now}` 参数。

```sql
UPDATE t_article
SET status = 2, updated_at = #{now}, updated_by = NULL
WHERE status = 5
  AND publish_at <= #{now}
  AND deleted = 0;
```

### Jackson 序列化

```yaml
spring:
  jackson:
    time-zone: Asia/Tokyo
    serialization:
      write-dates-as-timestamps: false
    date-format: yyyy-MM-dd'T'HH:mm:ss
```

### DATETIME 而非 TIMESTAMP

时间列类型由 ADR-0015 统一为 `DATETIME`：
- `DATETIME` 不参与 session 时区转换，存什么读什么
- 与"应用层强制 JST"模型契合：应用永远写入 JST `LocalDateTime`，DB 永远保留这个值

## 理由

- **单时区受众**：作品集面向日本主管，无跨时区访问需求
- **五层一致**：从 JVM 到前端任一层"漏配"都会引入隐性 bug；五层强制对齐消除歧义
- **`Clock` 可注入**：测试用 `Clock.fixed(...)` 控制时间，业务代码免被"当前时间"污染
- **不存 UTC + 渲染时转**：跨时区项目的标准做法；单时区项目这么做反而增加心智负担

## 替代未选

| 方案 | 不选理由 |
|---|---|
| 存 UTC + 前端按浏览器时区渲染 | 单时区场景过度设计；调试要心算 UTC↔JST |
| `OffsetDateTime` / `ZonedDateTime` | 数据库存储复杂度上升；个人博客无收益 |
| `TIMESTAMP` + MySQL 自动时区转换 | session 时区切换会"漂移"历史数据；运维风险高 |

## 后果

正面：
- 时间处理可预期，从 DB 到前端"所见即所得"
- 测试可控（`Clock.fixed`）
- 调试时不需心算时区转换

负面：
- **未来若扩展非日本访客**，需在前端加时区转换层；后置 V3 评估
- DATETIME 不可移植到"全球用户"场景；如未来真要全球化，需大改

## 守护

- **`pitfalls.md` R-011 红线**：业务代码直接 `LocalDateTime.now()` 即违规
- 启动日志打印当前 `ZoneId.systemDefault()`，便于部署时眼检
- `MyBlogConfigStartupValidator` 校验 JVM 时区为 Asia/Tokyo（缺则启动失败）

## 相关

- 关联决定：`../../archive/project-handbook/product/decisions-draft.md` R7 D11
- 关联 ADR：ADR-0015（DATETIME 时间列）
- 关联 pitfalls：R-011（不得直接 `LocalDateTime.now()`）
