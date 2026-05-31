package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.CategorySummary;

/**
 * 文章分类响应。
 *
 * @param id   分类 ID
 * @param name 分类名称
 */
public record ArticleCategoryResponse(int id, String name) {

    static ArticleCategoryResponse from(CategorySummary category) {
        return new ArticleCategoryResponse(category.id(), category.name());
    }
}
