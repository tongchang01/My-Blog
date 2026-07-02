package com.tyb.myblog.v2.content.application.category;

/**
 * 公开分类查询结果。
 */
public record PublicCategoryResult(
        long id,
        String name,
        String slug,
        long articleCount) {
}
