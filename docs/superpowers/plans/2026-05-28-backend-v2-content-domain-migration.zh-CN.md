# 后端 V2 Content 只读业务域迁移实施计划

> **给执行该计划的代理：** 必须使用 `superpowers:executing-plans` 按任务逐项执行。每完成一个任务，运行该任务指定的验证命令，提交一次中文 commit，然后停下来等我确认。

## 总目标

我已经完成后端 V2 的身份、当前用户资料和后台菜单基盘。下一步开始进入第一个真实业务域：`content`。

这次不是简单把旧代码复制到 V2，而是把旧后端里文章、分类、标签相关的只读能力重新整理成清晰的业务边界：

- 前台读者接口和后台管理接口拆开。
- 先迁移低风险只读查询，不碰后台写操作。
- 先兼容旧 MySQL 表结构，不立即改真实线上表。
- 不把旧代码里的 Controller、Service、Mapper、DTO 命名和返回结构原样搬过来。
- 每一步都要有 H2 集成测试和 MockMvc API 测试，防止 V2 变成“能跑但不可维护”的新旧混合体。

本计划完成后，后端 V2 应具备第一批内容只读接口：

- 分类列表。
- 标签列表。
- 标签 Top 列表。
- 文章分页列表。
- 按分类查询文章分页列表。
- 按标签查询文章分页列表。
- 公开文章详情。

## 当前问题判断

我在旧后端里发现内容业务存在几个明显问题：

1. 文章、分类、标签接口混在旧式 Controller 里，前台读者接口和后台管理接口没有在代码结构上强隔离。这样会导致后续前台、后台同时重构时，接口契约容易互相污染。
2. 旧 `ArticleServiceImpl` 承担了分页、Redis 浏览量、密码文章访问、上下篇、后台编辑、导入导出等多类职责。这样会导致任何一个小改动都可能影响不相关能力。
3. 旧 MyBatis XML 查询能工作，但聚合字段、标签列表、作者信息、分页总数分散在多段 SQL 中，领域含义不清晰。V2 不能继续把 SQL 结果直接当业务模型使用。
4. 旧公开列表查询包含 `status in (1,2)`，也就是普通公开文章和密码文章都会出现在列表里，但详情访问又依赖 Redis 访问令牌。这个规则要拆成独立阶段处理，否则第一批只读迁移会被密码文章、Redis 和浏览量统计拖复杂。
5. 旧代码注释偏少，边界说明不足。V2 每个领域端口、查询实现和接口 DTO 都要通过命名和测试表达规则，复杂处再补少量注释。

所以本阶段只做 `status = 1` 且 `is_delete = 0` 的公开内容读取。`status = 2` 密码文章、搜索、归档、置顶推荐组合接口、浏览量 Redis 统计、后台文章 CRUD、图片上传、导入导出全部留到后续计划。

## 非目标

本计划不做这些事：

- 不修改真实 MySQL 表结构。
- 不新增线上 Flyway 生产迁移。
- 不迁移后台文章、分类、标签的新增、编辑、删除接口。
- 不迁移文章图片上传、Markdown 导入导出。
- 不迁移文章搜索。
- 不迁移归档接口。
- 不迁移密码文章访问流程。
- 不引入 Redis 浏览量统计。
- 不把 `t_resource` / `t_role_resource` 动态资源权限接进本阶段。
- 不接入前台 Vue3 或后台 Vue3 页面。

## 旧后端盘点

### 旧公开接口

旧 `ArticleController` 中与前台读者相关的接口：

```text
GET  /articles/topAndFeatured
GET  /articles/all
GET  /articles/categoryId
GET  /articles/{articleId}
POST /articles/access
GET  /articles/tagId
GET  /archives/all
GET  /articles/search
```

本阶段只迁移其中的文章列表、按分类文章列表、按标签文章列表、文章详情。`topAndFeatured`、`access`、`archives`、`search` 后续单独做。

旧 `CategoryController` 中与前台读者相关的接口：

```text
GET /categories/all
```

旧 `TagController` 中与前台读者相关的接口：

```text
GET /tags/all
GET /tags/topTen
```

### 旧后台接口

旧后台内容管理接口包括：

```text
GET    /admin/articles
POST   /admin/articles
PUT    /admin/articles/topAndFeatured
PUT    /admin/articles
DELETE /admin/articles/delete
POST   /admin/articles/images
GET    /admin/articles/{articleId}
POST   /admin/articles/import
POST   /admin/articles/export
GET    /admin/categories/search
POST   /admin/categories
PUT    /admin/categories
DELETE /admin/categories/delete
GET    /admin/tags/search
POST   /admin/tags
PUT    /admin/tags
DELETE /admin/tags/delete
```

这些接口全部不进本阶段。原因是后台写操作会牵涉草稿、发布、上下架、标签关系维护、权限、图片资源和表结构治理，应该在读者端契约稳定后再迁。

### 旧表

本阶段只读这些旧表：

```text
t_article
t_category
t_tag
t_article_tag
t_user_info
```

`t_user_info` 只用于文章作者昵称和头像展示，不在内容域里维护用户资料。

### 旧状态规则

旧文章状态从代码行为看至少包含：

- `is_delete = 0`：未删除。
- `is_delete = 1`：已删除。
- `status = 1`：公开发布。
- `status = 2`：密码文章。
- 其他状态：草稿或非公开状态。

V2 第一批只读内容统一使用：

```sql
a.is_delete = 0
and a.status = 1
```

这样做会暂时少返回密码文章，但能避免把 Redis 访问令牌、密码校验和详情访问控制混入第一批内容读取。密码文章会在独立任务恢复。

## 新接口契约

V2 使用 `/api` 前缀，保持和现有 `/api/auth/login`、`/api/auth/me`、`/api/admin/user/menus` 一致。

### 分类列表

```text
GET /api/categories
```

返回：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "Java",
      "articleCount": 2
    }
  ]
}
```

### 标签列表

```text
GET /api/tags
```

返回：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "Spring",
      "articleCount": 2
    }
  ]
}
```

### 标签 Top 列表

```text
GET /api/tags/top?limit=10
```

规则：

- `limit` 默认 10。
- `limit` 小于 1 时按 1 处理。
- `limit` 大于 50 时按 50 处理。
- 按公开文章数量降序，再按标签 ID 升序。

### 文章分页列表

```text
GET /api/articles?page=1&size=10
```

返回：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "title": "后端 V2 第一篇",
        "summary": "摘要一",
        "cover": "/cover/1.png",
        "category": {
          "id": 1,
          "name": "Java"
        },
        "author": {
          "id": 1,
          "nickname": "管理员",
          "avatar": ""
        },
        "tags": [
          {
            "id": 1,
            "name": "Spring"
          }
        ],
        "top": true,
        "featured": true,
        "createdAt": "2026-05-28T10:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10
  }
}
```

### 按分类查询文章

```text
GET /api/categories/{categoryId}/articles?page=1&size=10
```

### 按标签查询文章

```text
GET /api/tags/{tagId}/articles?page=1&size=10
```

### 公开文章详情

```text
GET /api/articles/{articleId}
```

返回：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {
    "id": 1,
    "title": "后端 V2 第一篇",
    "summary": "摘要一",
    "content": "正文一",
    "cover": "/cover/1.png",
    "category": {
      "id": 1,
      "name": "Java"
    },
    "author": {
      "id": 1,
      "nickname": "管理员",
      "avatar": ""
    },
    "tags": [
      {
        "id": 1,
        "name": "Spring"
      }
    ],
    "top": true,
    "featured": true,
    "createdAt": "2026-05-28T10:00:00",
    "updatedAt": "2026-05-28T10:00:00"
  }
}
```

不存在、已删除、草稿、密码文章统一返回：

```json
{
  "success": false,
  "code": "NOT_FOUND",
  "message": "文章不存在",
  "data": null
}
```

## 目标包结构

新增内容域包：

```text
com.aurora.myblog.v2.modules.content
  ├─ api
  ├─ application
  ├─ domain
  └─ infrastructure
```

内容域依赖方向：

```text
api -> application -> domain
infrastructure -> domain
application -> domain
```

约束：

- `domain` 不依赖 Spring MVC、Spring Security、JdbcTemplate。
- `application` 编排领域端口，不写 SQL。
- `infrastructure` 兼容旧表 SQL。
- `api` 只处理 HTTP 入参、出参 DTO 和状态码。
- 内容域不依赖 `modules.identity.infrastructure`。
- 内容域不依赖 `common.security`。

## 任务 1：补齐内容域 H2 测试表

### 目标

先让测试数据库具备内容域旧表，后续所有内容查询都基于 H2 走真实 SQL。

### 修改文件

修改：

```text
MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql
```

### 具体实现

在现有身份、角色、菜单测试表之后追加：

```sql
create table t_category (
    id int auto_increment primary key,
    category_name varchar(50) not null,
    create_time timestamp not null,
    update_time timestamp
);

create table t_tag (
    id int auto_increment primary key,
    tag_name varchar(50) not null,
    create_time timestamp not null,
    update_time timestamp
);

create table t_article (
    id int auto_increment primary key,
    user_id int not null,
    category_id int not null,
    article_cover varchar(1024),
    article_title varchar(100) not null,
    article_abstract varchar(255),
    article_content clob,
    is_top tinyint not null default 0,
    is_featured tinyint not null default 0,
    is_delete tinyint not null default 0,
    status tinyint not null,
    type tinyint,
    password varchar(255),
    original_url varchar(1024),
    create_time timestamp not null,
    update_time timestamp
);

create table t_article_tag (
    id int auto_increment primary key,
    article_id int not null,
    tag_id int not null
);
```

追加测试数据：

```sql
insert into t_category (id, category_name, create_time)
values
    (1, 'Java', current_timestamp),
    (2, '生活', current_timestamp);

insert into t_tag (id, tag_name, create_time)
values
    (1, 'Spring', current_timestamp),
    (2, 'Vue', current_timestamp),
    (3, '重构', current_timestamp);

insert into t_article (
    id, user_id, category_id, article_cover, article_title, article_abstract,
    article_content, is_top, is_featured, is_delete, status, type, create_time, update_time
)
values
    (1, 1, 1, '/cover/java-1.png', '后端V2第一篇', '摘要一', '正文一', 1, 1, 0, 1, 1, timestamp '2026-05-28 10:00:00', timestamp '2026-05-28 10:00:00'),
    (2, 1, 2, '/cover/life-1.png', '生活记录第一篇', '摘要二', '正文二', 0, 0, 0, 1, 1, timestamp '2026-05-28 11:00:00', timestamp '2026-05-28 11:00:00'),
    (3, 1, 1, '/cover/protected.png', '密码文章', '不应出现在第一阶段', '密码正文', 0, 0, 0, 2, 1, timestamp '2026-05-28 12:00:00', timestamp '2026-05-28 12:00:00'),
    (4, 1, 1, '/cover/draft.png', '草稿文章', '不应出现在第一阶段', '草稿正文', 0, 0, 0, 3, 1, timestamp '2026-05-28 13:00:00', timestamp '2026-05-28 13:00:00'),
    (5, 1, 2, '/cover/deleted.png', '已删除文章', '不应出现在第一阶段', '删除正文', 0, 0, 1, 1, 1, timestamp '2026-05-28 14:00:00', timestamp '2026-05-28 14:00:00');

insert into t_article_tag (id, article_id, tag_id)
values
    (1, 1, 1),
    (2, 1, 3),
    (3, 2, 2),
    (4, 2, 3),
    (5, 3, 1),
    (6, 4, 1),
    (7, 5, 2);
```

### 验证命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=FlywayMigrationTest'
```

预期：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

### 提交

```powershell
git add MyBlog-springboot-v2/src/test/resources/db/migration/V2__create_legacy_identity_tables_for_tests.sql
git commit -m "补齐后端V2内容测试表"
```

## 任务 2：新增通用分页响应

### 目标

让内容列表接口有统一分页结构，避免每个接口自己定义 `records/total/page/size`。

### 新增文件

```text
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/PageResponse.java
MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/web/PageResponseTest.java
```

### `PageResponse.java`

```java
package com.aurora.myblog.v2.common.web;

import java.util.List;

public record PageResponse<T>(List<T> records, long total, int page, int size) {

    public PageResponse {
        records = records == null ? List.of() : List.copyOf(records);
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
    }
}
```

### `PageResponseTest.java`

```java
package com.aurora.myblog.v2.common.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageResponseTest {

    @Test
    void replacesNullRecordsWithEmptyList() {
        PageResponse<String> response = new PageResponse<>(null, 0, 1, 10);

        assertThat(response.records()).isEmpty();
    }

    @Test
    void copiesRecordsDefensively() {
        PageResponse<String> response = new PageResponse<>(List.of("a"), 1, 1, 10);

        assertThat(response.records()).containsExactly("a");
        assertThatThrownBy(() -> response.records().add("b"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidMetadata() {
        assertThatThrownBy(() -> new PageResponse<>(List.of(), -1, 1, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageResponse<>(List.of(), 0, 0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PageResponse<>(List.of(), 0, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

### 验证命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=PageResponseTest'
```

预期：

```text
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

### 提交

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/web/PageResponse.java MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/common/web/PageResponseTest.java
git commit -m "新增后端V2分页响应"
```

## 任务 3：迁移分类和标签只读接口

### 目标

先迁移内容目录能力：分类、标签、Top 标签。这个任务不涉及文章详情，也不涉及后台写操作。

### 新增文件

```text
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/CategorySummary.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/TagSummary.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ContentCatalogReader.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/application/ContentQueryService.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/DatabaseContentCatalogReader.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/CategoryResponse.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/TagResponse.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ContentCatalogController.java
MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseContentCatalogReaderTest.java
MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentCatalogControllerTest.java
```

### 领域对象

```java
package com.aurora.myblog.v2.modules.content.domain;

public record CategorySummary(int id, String name, long articleCount) {
}
```

```java
package com.aurora.myblog.v2.modules.content.domain;

public record TagSummary(int id, String name, long articleCount) {
}
```

```java
package com.aurora.myblog.v2.modules.content.domain;

import java.util.List;

public interface ContentCatalogReader {
    List<CategorySummary> listCategories();

    List<TagSummary> listTags();

    List<TagSummary> listTopTags(int limit);
}
```

### 应用服务

```java
package com.aurora.myblog.v2.modules.content.application;

import com.aurora.myblog.v2.modules.content.domain.CategorySummary;
import com.aurora.myblog.v2.modules.content.domain.ContentCatalogReader;
import com.aurora.myblog.v2.modules.content.domain.TagSummary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentQueryService {

    private static final int DEFAULT_TOP_TAG_LIMIT = 10;
    private static final int MAX_TOP_TAG_LIMIT = 50;

    private final ContentCatalogReader catalogReader;

    public ContentQueryService(ContentCatalogReader catalogReader) {
        this.catalogReader = catalogReader;
    }

    public List<CategorySummary> listCategories() {
        return catalogReader.listCategories();
    }

    public List<TagSummary> listTags() {
        return catalogReader.listTags();
    }

    public List<TagSummary> listTopTags(Integer limit) {
        int normalizedLimit = normalizeTopTagLimit(limit);
        return catalogReader.listTopTags(normalizedLimit);
    }

    private int normalizeTopTagLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TOP_TAG_LIMIT;
        }
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, MAX_TOP_TAG_LIMIT);
    }
}
```

### SQL 实现要点

`DatabaseContentCatalogReader` 使用 `JdbcTemplate`。分类 SQL：

```sql
select c.id,
       c.category_name,
       count(a.id) as article_count
from t_category c
left join t_article a
       on a.category_id = c.id
      and a.is_delete = 0
      and a.status = 1
group by c.id, c.category_name
order by c.id asc
```

标签 SQL：

```sql
select t.id,
       t.tag_name,
       count(a.id) as article_count
from t_tag t
left join t_article_tag at on at.tag_id = t.id
left join t_article a
       on a.id = at.article_id
      and a.is_delete = 0
      and a.status = 1
group by t.id, t.tag_name
order by t.id asc
```

Top 标签 SQL：

```sql
select t.id,
       t.tag_name,
       count(a.id) as article_count
from t_tag t
left join t_article_tag at on at.tag_id = t.id
left join t_article a
       on a.id = at.article_id
      and a.is_delete = 0
      and a.status = 1
group by t.id, t.tag_name
order by article_count desc, t.id asc
limit ?
```

### API DTO

```java
package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.CategorySummary;

public record CategoryResponse(int id, String name, long articleCount) {

    static CategoryResponse from(CategorySummary category) {
        return new CategoryResponse(category.id(), category.name(), category.articleCount());
    }
}
```

```java
package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.TagSummary;

public record TagResponse(int id, String name, long articleCount) {

    static TagResponse from(TagSummary tag) {
        return new TagResponse(tag.id(), tag.name(), tag.articleCount());
    }
}
```

### Controller

```java
package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.common.web.ApiResponse;
import com.aurora.myblog.v2.modules.content.application.ContentQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ContentCatalogController {

    private final ContentQueryService contentQueryService;

    public ContentCatalogController(ContentQueryService contentQueryService) {
        this.contentQueryService = contentQueryService;
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryResponse>> listCategories() {
        List<CategoryResponse> categories = contentQueryService.listCategories().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.ok(categories);
    }

    @GetMapping("/api/tags")
    public ApiResponse<List<TagResponse>> listTags() {
        List<TagResponse> tags = contentQueryService.listTags().stream()
                .map(TagResponse::from)
                .toList();
        return ApiResponse.ok(tags);
    }

    @GetMapping("/api/tags/top")
    public ApiResponse<List<TagResponse>> listTopTags(@RequestParam(required = false) Integer limit) {
        List<TagResponse> tags = contentQueryService.listTopTags(limit).stream()
                .map(TagResponse::from)
                .toList();
        return ApiResponse.ok(tags);
    }
}
```

### 测试重点

`DatabaseContentCatalogReaderTest`：

- 分类列表返回 2 个分类。
- `Java` 只统计公开文章，不统计密码文章、草稿、删除文章。
- 标签列表只统计公开文章。
- Top 标签按公开文章数量降序，数量相同时按 ID 升序。

`ContentCatalogControllerTest`：

- `GET /api/categories` 返回 `success=true`。
- `GET /api/tags` 返回标签列表。
- `GET /api/tags/top?limit=1` 只返回 1 条。
- `GET /api/tags/top?limit=999` 不报错，按上限归一化。

### 验证命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseContentCatalogReaderTest,ContentCatalogControllerTest'
```

预期：

```text
Failures: 0, Errors: 0
```

### 提交

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content
git commit -m "迁移后端V2分类标签只读接口"
```

## 任务 4：迁移文章分页列表接口

### 目标

迁移公开文章卡片列表，支持全部文章、按分类、按标签三种分页查询。

### 新增文件

```text
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleSummary.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleTagSummary.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/AuthorSummary.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticlePageQuery.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleReader.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/DatabaseArticleReader.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArticleCategoryResponse.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArticleAuthorResponse.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArticleTagResponse.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArticleSummaryResponse.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ContentArticleController.java
MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java
MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentArticleControllerTest.java
```

### 领域模型

```java
package com.aurora.myblog.v2.modules.content.domain;

public record ArticleTagSummary(int id, String name) {
}
```

```java
package com.aurora.myblog.v2.modules.content.domain;

public record AuthorSummary(int id, String nickname, String avatar) {
}
```

```java
package com.aurora.myblog.v2.modules.content.domain;

public record ArticlePageQuery(int page, int size) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    public static ArticlePageQuery of(Integer page, Integer size) {
        int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new ArticlePageQuery(normalizedPage, normalizedSize);
    }

    public int offset() {
        return (page - 1) * size;
    }
}
```

```java
package com.aurora.myblog.v2.modules.content.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleSummary(
        int id,
        String title,
        String summary,
        String cover,
        CategorySummary category,
        AuthorSummary author,
        List<ArticleTagSummary> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt
) {

    public ArticleSummary {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
```

```java
package com.aurora.myblog.v2.modules.content.domain;

import com.aurora.myblog.v2.common.web.PageResponse;

public interface ArticleReader {
    PageResponse<ArticleSummary> listPublishedArticles(ArticlePageQuery query);

    PageResponse<ArticleSummary> listPublishedArticlesByCategory(int categoryId, ArticlePageQuery query);

    PageResponse<ArticleSummary> listPublishedArticlesByTag(int tagId, ArticlePageQuery query);
}
```

### 应用服务扩展

在 `ContentQueryService` 中新增：

```java
private final ArticleReader articleReader;

public PageResponse<ArticleSummary> listArticles(Integer page, Integer size) {
    return articleReader.listPublishedArticles(ArticlePageQuery.of(page, size));
}

public PageResponse<ArticleSummary> listArticlesByCategory(int categoryId, Integer page, Integer size) {
    return articleReader.listPublishedArticlesByCategory(categoryId, ArticlePageQuery.of(page, size));
}

public PageResponse<ArticleSummary> listArticlesByTag(int tagId, Integer page, Integer size) {
    return articleReader.listPublishedArticlesByTag(tagId, ArticlePageQuery.of(page, size));
}
```

构造函数改为同时注入 `ContentCatalogReader` 和 `ArticleReader`。

### SQL 实现规则

`DatabaseArticleReader` 拆成两类查询：

1. 先查总数。
2. 再查当前页文章 ID。
3. 再按 ID 查文章主体、分类、作者、标签并在 Java 内按文章 ID 聚合标签。

不要用一条 `left join tag` SQL 直接分页，因为一篇文章多个标签会放大行数，导致分页错乱。

总数 SQL 示例：

```sql
select count(*)
from t_article a
where a.is_delete = 0
  and a.status = 1
```

分页 ID SQL 示例：

```sql
select a.id
from t_article a
where a.is_delete = 0
  and a.status = 1
order by a.id desc
limit ? offset ?
```

按分类分页 ID：

```sql
select a.id
from t_article a
where a.is_delete = 0
  and a.status = 1
  and a.category_id = ?
order by a.id desc
limit ? offset ?
```

按标签分页 ID：

```sql
select distinct a.id
from t_article a
join t_article_tag at on at.article_id = a.id
where a.is_delete = 0
  and a.status = 1
  and at.tag_id = ?
order by a.id desc
limit ? offset ?
```

详情聚合 SQL 按 ID 列表查询：

```sql
select a.id,
       a.article_title,
       a.article_abstract,
       a.article_cover,
       a.is_top,
       a.is_featured,
       a.create_time,
       c.id as category_id,
       c.category_name,
       u.id as author_id,
       u.nickname,
       u.avatar,
       t.id as tag_id,
       t.tag_name
from t_article a
join t_category c on c.id = a.category_id
join t_user_info u on u.id = a.user_id
left join t_article_tag at on at.article_id = a.id
left join t_tag t on t.id = at.tag_id
where a.id in (...)
order by a.id desc, t.id asc
```

`in (...)` 占位符必须按 ID 数量动态生成，但只能用 `?` 参数，不允许拼接外部输入。

### API 响应 DTO

`ArticleSummaryResponse.from(ArticleSummary article)` 将领域模型转换成接口模型。

时间字段直接使用 `LocalDateTime`，由 Spring Boot Jackson 默认序列化为 ISO 格式。

### Controller

新增：

```java
@GetMapping("/api/articles")
public ApiResponse<PageResponse<ArticleSummaryResponse>> listArticles(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {
    PageResponse<ArticleSummary> result = contentQueryService.listArticles(page, size);
    return ApiResponse.ok(mapArticlePage(result));
}

@GetMapping("/api/categories/{categoryId}/articles")
public ApiResponse<PageResponse<ArticleSummaryResponse>> listArticlesByCategory(
        @PathVariable int categoryId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {
    PageResponse<ArticleSummary> result = contentQueryService.listArticlesByCategory(categoryId, page, size);
    return ApiResponse.ok(mapArticlePage(result));
}

@GetMapping("/api/tags/{tagId}/articles")
public ApiResponse<PageResponse<ArticleSummaryResponse>> listArticlesByTag(
        @PathVariable int tagId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size) {
    PageResponse<ArticleSummary> result = contentQueryService.listArticlesByTag(tagId, page, size);
    return ApiResponse.ok(mapArticlePage(result));
}
```

### 测试重点

`DatabaseArticleReaderTest`：

- 文章列表只返回 2 篇公开未删除文章。
- 密码文章、草稿、删除文章不进入结果。
- 分页 `page=1,size=1` 返回最新公开文章，`total=2`。
- 按分类只返回该分类公开文章。
- 按标签只返回该标签公开文章。
- 同一篇文章多个标签时不会重复文章。

`ContentArticleControllerTest`：

- `GET /api/articles` 返回统一分页结构。
- `GET /api/articles?page=0&size=999` 被归一化为 `page=1,size=50`。
- `GET /api/categories/1/articles` 正常返回。
- `GET /api/tags/3/articles` 正常返回。

### 验证命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

预期：

```text
Failures: 0, Errors: 0
```

### 提交

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content
git commit -m "迁移后端V2文章列表接口"
```

## 任务 5：迁移公开文章详情接口

### 目标

迁移公开文章详情读取。只允许读取 `status = 1` 且 `is_delete = 0` 的文章。

### 修改文件

新增：

```text
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleDetail.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ArticleDetailResponse.java
```

修改：

```text
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiErrorCode.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/domain/ArticleReader.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/application/ContentQueryService.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/infrastructure/DatabaseArticleReader.java
MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content/api/ContentArticleController.java
MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/DatabaseArticleReaderTest.java
MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content/ContentArticleControllerTest.java
```

### 错误码

给 `ApiErrorCode` 增加：

```java
NOT_FOUND(HttpStatus.NOT_FOUND),
```

放在 `FORBIDDEN` 后、`CONFLICT` 前。

### 领域模型

```java
package com.aurora.myblog.v2.modules.content.domain;

import java.time.LocalDateTime;
import java.util.List;

public record ArticleDetail(
        int id,
        String title,
        String summary,
        String content,
        String cover,
        CategorySummary category,
        AuthorSummary author,
        List<ArticleTagSummary> tags,
        boolean top,
        boolean featured,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public ArticleDetail {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
```

### 端口扩展

```java
Optional<ArticleDetail> findPublishedArticleById(int articleId);
```

### 应用服务扩展

```java
public ArticleDetail getArticleDetail(int articleId) {
    return articleReader.findPublishedArticleById(articleId)
            .orElseThrow(() -> new ApiException(ApiErrorCode.NOT_FOUND, "文章不存在"));
}
```

### SQL 实现规则

详情 SQL 必须带公开过滤：

```sql
where a.id = ?
  and a.is_delete = 0
  and a.status = 1
```

查询字段包含 `article_content` 和 `update_time`。

### Controller

```java
@GetMapping("/api/articles/{articleId}")
public ApiResponse<ArticleDetailResponse> getArticleDetail(@PathVariable int articleId) {
    ArticleDetail article = contentQueryService.getArticleDetail(articleId);
    return ApiResponse.ok(ArticleDetailResponse.from(article));
}
```

### 测试重点

`DatabaseArticleReaderTest`：

- 公开文章详情可以查到正文和标签。
- 密码文章返回 `Optional.empty()`。
- 草稿文章返回 `Optional.empty()`。
- 删除文章返回 `Optional.empty()`。
- 不存在文章返回 `Optional.empty()`。

`ContentArticleControllerTest`：

- `GET /api/articles/1` 返回 `success=true` 和正文。
- `GET /api/articles/3` 返回 HTTP 404、`code=NOT_FOUND`。
- `GET /api/articles/999` 返回 HTTP 404、`code=NOT_FOUND`。

### 验证命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test '-Dtest=DatabaseArticleReaderTest,ContentArticleControllerTest'
```

预期：

```text
Failures: 0, Errors: 0
```

### 提交

```powershell
git add MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/common/error/ApiErrorCode.java MyBlog-springboot-v2/src/main/java/com/aurora/myblog/v2/modules/content MyBlog-springboot-v2/src/test/java/com/aurora/myblog/v2/modules/content
git commit -m "迁移后端V2文章详情接口"
```

## 任务 6：全量验证和本地 MySQL 只读冒烟

### 目标

确认内容域只读迁移没有破坏现有身份、安全、菜单能力，并用本地真实 MySQL 做只读冒烟。

### 验证命令

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml test
mvn -f MyBlog-springboot-v2/pom.xml clean package
```

预期：

```text
BUILD SUCCESS
```

### 本地 MySQL 只读检查

只读检查表数量，不提交任何密码或本地配置：

```powershell
mysql -h localhost -P 3306 -u root -p -e "select count(*) as article_count from my_blog.t_article; select count(*) as category_count from my_blog.t_category; select count(*) as tag_count from my_blog.t_tag; select count(*) as article_tag_count from my_blog.t_article_tag;"
```

如果数据库名不是 `my_blog`，先用：

```powershell
mysql -h localhost -P 3306 -u root -p -e "show databases;"
```

再替换库名。

### 本地服务冒烟

启动 V2：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
mvn -f MyBlog-springboot-v2/pom.xml spring-boot:run
```

调用：

```powershell
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/categories'
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/tags'
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/tags/top?limit=10'
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/articles?page=1&size=5'
```

如果文章列表返回至少一篇公开文章，再调用第一篇文章详情：

```powershell
Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/api/articles/<articleId>'
```

### 计划状态更新

任务 6 完成后，在本文件末尾追加“执行结果”小节，记录：

- 每个任务对应 commit。
- 全量测试结果。
- 打包结果。
- 本地 MySQL 表数量只读结果。
- 本地 V2 API 冒烟结果。
- 未迁移能力清单。

### 提交

```powershell
git add docs/superpowers/plans/2026-05-28-backend-v2-content-domain-migration.zh-CN.md
git commit -m "同步后端V2内容迁移计划状态"
```

## 完成标准

本计划全部完成时，必须满足：

- 内容域包结构存在，且符合 V2 分层边界。
- `/api/categories` 可返回分类和公开文章数量。
- `/api/tags` 可返回标签和公开文章数量。
- `/api/tags/top` 可按公开文章数量返回 Top 标签。
- `/api/articles` 可分页返回公开文章卡片。
- `/api/categories/{categoryId}/articles` 可分页返回分类下公开文章。
- `/api/tags/{tagId}/articles` 可分页返回标签下公开文章。
- `/api/articles/{articleId}` 可返回公开文章详情。
- 密码文章、草稿、删除文章不会在第一阶段公开内容接口中返回。
- H2 集成测试覆盖主要 SQL 规则。
- MockMvc 测试覆盖主要 API 契约。
- `mvn test` 通过。
- `mvn clean package` 通过。
- 本地 MySQL 只读冒烟通过。

## 后续阶段建议

本计划完成后，下一阶段不要马上做后台 CRUD。推荐顺序：

1. 文章归档和置顶推荐读取。
2. 密码文章访问流程。
3. 文章搜索。
4. 后台文章管理列表。
5. 后台文章保存、更新、删除、上下架。
6. 分类和标签后台管理。
7. 图片上传和资源治理。
8. 内容相关真实表结构治理。

原因是读者端能力稳定后，前台 Vue3 可以先接入 V2；后台写操作再跟着真实编辑流程逐步迁移，风险更低。
