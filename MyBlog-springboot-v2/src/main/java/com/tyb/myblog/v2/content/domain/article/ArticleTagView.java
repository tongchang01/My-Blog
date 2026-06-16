package com.tyb.myblog.v2.content.domain.article;

/**
 * 文章公开查询使用的标签只读视图。
 */
public record ArticleTagView(
        long id,
        String nameZh,
        String nameJa,
        String nameEn,
        String slug) {
}
