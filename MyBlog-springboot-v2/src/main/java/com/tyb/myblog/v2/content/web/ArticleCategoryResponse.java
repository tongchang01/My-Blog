package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.CategorySummary;

public record ArticleCategoryResponse(int id, String name) {

    static ArticleCategoryResponse from(CategorySummary category) {
        return new ArticleCategoryResponse(category.id(), category.name());
    }
}
