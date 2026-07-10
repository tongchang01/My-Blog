# ADR-0016：文章 URL 由 ID 定位并附带可读 slug

> 状态：当前有效
> 适用范围：公开文章详情接口与博客路由
> 最后校准：2026-07-10
> 对应代码：`frontend/apps/blog/src/router/index.ts`、`frontend/apps/blog/src/pages/post/[slug].vue`、`MyBlog-springboot-v2/src/main/java/com/tyb/myblog/v2/content/`
> 权威程度：ADR

## 背景

文章标题和 slug 可能变化，仅使用 slug 定位会引入重定向历史和唯一性维护成本。

## 决策

公开文章详情路由采用 `/{lang}/posts/{id}/{slug?}`：

- 数字 ID 是查询依据；
- slug 只增强可读性，可以省略；
- 详情加载成功后，博客端使用接口返回的当前 slug 替换 URL；
- 文章 slug 不承担主键或历史别名职责；
- 分类和标签仍使用唯一 slug 作为公开列表路由参数，创建后不可修改。

## 结果

文章改名不会改变定位依据，分享链接可以保留可读路径。数据库不需要维护文章 slug 历史表。
