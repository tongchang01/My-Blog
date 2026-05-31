package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.CategorySummary;

public record CategoryResponse(int id, String name, long articleCount) {

    static CategoryResponse from(CategorySummary category) {
        return new CategoryResponse(category.id(), category.name(), category.articleCount());
    }
}
