# 前台契约地基实现计划

> 执行说明：本计划在 `feature/blog-contract-foundation` 分支内按小任务逐项完成。每个小任务独立验证、独立提交；代码实现前不新增临时接口，不改变内部领域 ID 类型，只调整 HTTP 边界和已确认的 slug 生命周期规则。

## 目标

落实 Batch 0 的契约地基，消除前台对接前最容易扩散的四类不稳定点：

1. 公开内容接口的文章、分类、标签 ID 在 JSON 边界统一输出字符串。
2. 公开评论接口的评论 ID、父评论 ID、回复目标 ID 在 JSON 边界统一输出字符串。
3. 统计后台 Top Article 的 `articleId` 在 JSON 边界输出字符串。
4. 分类/标签 URL 采用 slug 主导，但 slug 创建后锁定；文章 URL 继续采用文章 ID 主导。

## 范围边界

- 保留数据库和应用层的 `BIGINT` / `long` / `Long`，避免引入表结构迁移。
- 保留文章 URL 的 `id + 可选 slug` 策略，不实现文章 slug 历史或文章 slug 重定向。
- 分类/标签本轮只做 slug 锁定，不做 slug 历史、别名、自动重定向。
- 本轮不重建前台路由，只保证后端契约和 admin 编辑体验先稳定。
- `articleId=0` 继续表示首页/非文章页汇总，不改为 `null`。

## 小任务与提交拆分

### 1. 公开内容 ID 字符串化

涉及后端文件：

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArticlePageItemVO.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicArticleDetailVO.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicCategoryVO.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/PublicTagVO.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/ArticleWebMapping.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/CategoryWebMapping.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/web/TagWebMapping.java`

实现要点：

- 新增或复用 web 层标签 VO，把公开文章响应中的 tag ID 转成字符串。
- `categoryId` 允许为空，但非空时输出字符串。
- 不修改 `PublicArticleTagResult` 等 application result，避免把 HTTP 契约污染到应用层。

验证：

- `mvn -f MyBlog-springboot-v2/pom.xml -Dtest=PublicArticleControllerTest,PublicCategoryTagControllerTest,ArticleOpenApiTest,CategoryTagOpenApiTest test`

提交信息：

- `后端：公开内容接口输出字符串ID`

### 2. 公开评论 ID 字符串化

涉及后端文件：

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/PublicCommentVO.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/comment/web/PublicCommentCreateVO.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/web/PublicCommentControllerTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/comment/web/CommentOpenApiTest.java`

实现要点：

- `id` 必须输出字符串。
- `parentId`、`replyToCommentId` 为空时保持 `null`，非空时输出字符串。
- 评论创建响应的 `id` 也输出字符串。

验证：

- `mvn -f MyBlog-springboot-v2/pom.xml -Dtest=PublicCommentControllerTest,CommentOpenApiTest test`

提交信息：

- `后端：公开评论接口输出字符串ID`

### 3. 统计 Top Article ID 字符串化

涉及后端文件：

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/stats/web/StatsDashboardVO.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/stats/web/AdminStatsControllerTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/stats/web/StatsOpenApiTest.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/stats/integration/StatsIntegrationTest.java`

实现要点：

- 只调整 `topArticles[].articleId` 的 HTTP 输出类型。
- 保持统计聚合、查询和 `articleId=0` 语义不变。

验证：

- `mvn -f MyBlog-springboot-v2/pom.xml -Dtest=AdminStatsControllerTest,StatsOpenApiTest,StatsIntegrationTest test`

提交信息：

- `后端：统计热门文章输出字符串ID`

### 4. 分类/标签 slug 创建后锁定

涉及后端文件：

- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/category/CategoryUpdateService.java`
- `MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/application/tag/TagUpdateService.java`
- `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/application/CategoryTagWriteServiceTest.java`
- 必要时补充 `MyBlog-springboot-v2/src/test/java/com/tyb/myblog/v2/content/integration/CategoryTagIntegrationTest.java`

实现要点：

- 更新分类/标签时，如果请求 slug 与当前 slug 不一致，返回明确业务错误。
- 名称、排序、启用状态等非 slug 字段继续可编辑。
- 创建时仍保留 slug 唯一性校验。

验证：

- `mvn -f MyBlog-springboot-v2/pom.xml -Dtest=CategoryTagWriteServiceTest,CategoryTagIntegrationTest test`

提交信息：

- `后端：锁定分类标签slug`

### 5. Admin taxonomy 表单提示 slug 规则

涉及前端文件：

- `frontend/apps/admin/src/features/taxonomy/categories/index.vue`
- `frontend/apps/admin/src/features/taxonomy/tags/index.vue`
- `frontend/apps/admin/locales/zh-CN.yaml`
- `frontend/apps/admin/locales/en.yaml`
- `frontend/apps/admin/locales/ja.yaml`
- 对应 taxonomy 测试文件按现有测试结构补充断言

实现要点：

- 新建分类/标签时提示 slug 会进入公开 URL，创建后锁定。
- 编辑分类/标签时 slug 输入框只读或禁用，避免用户以为可以修改。
- 保存更新时继续提交原 slug，保持现有后端请求结构。

验证：

- `pnpm --dir frontend/apps/admin test -- taxonomy`
- 如项目脚本不支持过滤，改用现有 vitest 过滤命令。

提交信息：

- `前端：提示分类标签slug锁定规则`

### 6. 阶段级验证与文档回写

阶段完成后执行：

- `mvn -f MyBlog-springboot-v2/pom.xml clean test`
- `pnpm --dir frontend/apps/admin typecheck`
- `pnpm --dir frontend/apps/admin test`

文档回写：

- 更新 `docs/handbook/start-here/open-issues.md`，把 O-010、O-011、O-012、O-014 标记为已落实或改为后续前台接入项。
- 必要时更新 `docs/working/plans/2026-06-30-frontend-integration-batch-plan.md` 的 Batch 0 状态。

提交信息：

- `文档：回写前台契约地基状态`

## 风险点

- MapStruct 自动映射在公开 VO 类型改变后可能失效，优先用显式 default method 保持可读性。
- OpenAPI schema 断言必须跟随 HTTP 边界类型变化，否则前端生成或人工对接仍会误判 ID 类型。
- 前端 admin taxonomy 组件使用 Element Plus stub 测试时，可能需要按现有测试风格补一个可观察的输入禁用断言。
- `StatsIntegrationTest` 涉及日期和聚合数据，若出现与本次无关的时间基线失败，先记录失败证据，再拆分最小修复。
