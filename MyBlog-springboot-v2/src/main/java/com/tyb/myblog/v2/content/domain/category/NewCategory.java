package com.tyb.myblog.v2.content.domain.category;

import com.tyb.myblog.v2.content.domain.ContentName;
import com.tyb.myblog.v2.content.domain.ContentSlug;

/**
 * 待持久化的新分类。
 */
public record NewCategory(
        ContentName name,
        ContentSlug slug,
        int sortOrder,
        long createdBy) {

    public static NewCategory create(
            String nameZh,
            String nameJa,
            String nameEn,
            String slug,
            int sortOrder,
            long createdBy) {
        Category.validateSortOrder(sortOrder);
        validateActor(createdBy);
        return new NewCategory(
                ContentName.of(nameZh, nameJa, nameEn),
                ContentSlug.of(slug),
                sortOrder,
                createdBy);
    }

    private static void validateActor(long createdBy) {
        if (createdBy <= 0) {
            throw new IllegalArgumentException(
                    "分类创建者 ID 必须为正数");
        }
    }
}
