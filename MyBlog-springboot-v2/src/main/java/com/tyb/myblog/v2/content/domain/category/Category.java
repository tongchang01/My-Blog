package com.tyb.myblog.v2.content.domain.category;

import com.tyb.myblog.v2.content.domain.ContentName;
import com.tyb.myblog.v2.content.domain.ContentSlug;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 有效分类聚合。
 */
public record Category(
        long id,
        ContentName name,
        ContentSlug slug,
        int sortOrder,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {

    static final int MAX_SORT_ORDER = 1_000_000;

    public static Category reconstitute(
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
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "分类 ID 必须为正数");
        }
        validateSortOrder(sortOrder);
        return new Category(
                id,
                ContentName.of(nameZh, nameJa, nameEn),
                ContentSlug.of(slug),
                sortOrder,
                Objects.requireNonNull(
                        createdAt, "分类创建时间不能为空"),
                createdBy,
                Objects.requireNonNull(
                        updatedAt, "分类更新时间不能为空"),
                updatedBy);
    }

    public Category replace(
            String nameZh,
            String nameJa,
            String nameEn,
            String slug,
            int sortOrder) {
        return reconstitute(
                id,
                nameZh,
                nameJa,
                nameEn,
                slug,
                sortOrder,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy);
    }

    static void validateSortOrder(int sortOrder) {
        if (sortOrder < 0 || sortOrder > MAX_SORT_ORDER) {
            throw new IllegalArgumentException(
                    "分类排序值必须在 0 到 1000000 之间");
        }
    }
}
