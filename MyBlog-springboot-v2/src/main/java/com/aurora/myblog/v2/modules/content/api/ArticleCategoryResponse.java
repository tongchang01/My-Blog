package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.CategorySummary;

public record ArticleCategoryResponse(int id, String name) {

    static ArticleCategoryResponse from(CategorySummary category) {
        return new ArticleCategoryResponse(category.id(), category.name());
    }
}
