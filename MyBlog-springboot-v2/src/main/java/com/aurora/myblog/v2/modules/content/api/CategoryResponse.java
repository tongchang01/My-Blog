package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.CategorySummary;

public record CategoryResponse(int id, String name, long articleCount) {

    static CategoryResponse from(CategorySummary category) {
        return new CategoryResponse(category.id(), category.name(), category.articleCount());
    }
}
