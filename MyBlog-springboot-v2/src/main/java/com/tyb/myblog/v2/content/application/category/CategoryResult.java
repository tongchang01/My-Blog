package com.tyb.myblog.v2.content.application.category;

import com.tyb.myblog.v2.content.domain.category.Category;

import java.time.LocalDateTime;

/**
 * 后台分类查询结果。
 */
public record CategoryResult(
        long id,
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        int sortOrder,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {

    public static CategoryResult from(Category category) {
        return new CategoryResult(
                category.id(),
                category.name().zh(),
                category.name().ja(),
                category.name().en(),
                category.slug().value(),
                category.sortOrder(),
                category.createdAt(),
                category.createdBy(),
                category.updatedAt(),
                category.updatedBy());
    }
}
