# ADR-0016: 文章 URL 策略——id 主导 + slug 可读性增强

- 状态：accepted
- 日期：2026-06
- 决策者：项目负责人

## 背景

文章 URL 形态是"一旦上线就难改"的决定（改了破坏所有外链 + SEO）。常见方案：

1. **纯 slug**：`/posts/spring-security-jwt-pitfalls`
2. **纯 id**：`/posts/123`
3. **id + slug 组合**：`/posts/123-spring-security-jwt-pitfalls`

纯 slug 方案虽对 SEO 友好，但带来一连串复杂度：
- slug 必须全局唯一
- 软删除的 slug 是否复用 / 何时复用
- 改 slug 后旧 URL 怎么 301（需 `t_article_slug_history`）
- 跨表 slug 冲突（分类 slug vs 文章 slug 命名空间）
- 多语言 slug 是否每语言一份

## 决定

V2 采用 **id 主导 + slug 可读性增强**：

```
/{lang}/posts/{id}
/{lang}/posts/{id}-{slug}
```

示例：
- `/zh/posts/123`
- `/zh/posts/123-spring-security-jwt-pitfalls`
- `/ja/posts/123-spring-security-jwt-pitfalls`

### 字段定位

| 字段 | 角色 |
|---|---|
| `id` | 文章**唯一定位键**，后端查询只依赖 id |
| `slug` | URL 可读性字段，**不承担全局唯一身份**，允许为空，允许修改 |

### 表设计

- `t_article.slug VARCHAR(160) NULL`
- **不强制唯一**（不加 UNIQUE 约束）
- **不维护** `t_article_slug_history`（不做 slug 历史 301）
- **不引入** `t_slug_registry`（不做跨表 slug 命名空间）

### slug 输入规则

- 只允许 `a-z 0-9 -`
- 长度上限 160
- 可为空
- 可任意修改

### 访问 / canonical 规则

1. `/posts/{id}-{slug}` 先解析 id 查文章；若 URL 中 slug **与当前 slug 一致** → 200；**不一致** → 301 到 canonical
2. `/posts/{id}`（无 slug 段）：文章**有 slug** → 301 到 `/{lang}/posts/{id}-{currentSlug}`；文章**无 slug** → 200
3. canonical URL：
   - 有 slug → `/{lang}/posts/{id}-{currentSlug}`
   - 无 slug → `/{lang}/posts/{id}`
4. 同一篇文章公开只有一个 200 地址，避免重复内容
5. 改 slug 不破坏旧 URL 访问（id 仍能命中）；浏览器/爬虫到 301 后会更新

### 例外：分类与标签的 slug

- `t_category.slug` 与 `t_tag.slug` **UNIQUE**
- 理由：分类/标签是后台白名单管理的小集合，唯一性保证后台数据质量；软删后不复用
- 与文章 slug 策略不同：文章是用户内容主体，slug 允许"宽松"；分类/标签是元数据，需严格

## 理由

- 个人作品集场景，**不**靠 SEO 流量，不需要纯 slug 带来的可分享性最大化
- 用 id 做唯一定位键 = 后端逻辑简洁；slug 仅做 canonical 展示
- 避免 slug 历史表 / 软删占用 / 跨表命名空间这一连串复杂度
- 301 机制兜底改 slug 后的旧 URL，老地址不会 404

## 后果

正面：
- 后端 URL 路由极简（id 解析即可）
- 改 slug 零成本
- 删除文章后即使 id 复用也不会有数据混淆（id 是 BIGINT AUTO_INCREMENT/ASSIGN_ID，不会复用）

负面：
- URL 不如纯 slug "干净"（多了一段 id）；对作品集场景非问题
- 分享后被截短 URL（如 `/posts/123` 没 slug 段）会触发 301；可接受

## 相关

- 关联决定：`product/decisions-draft.md` R3 #6
- 关联 pitfalls：无（新设计）
