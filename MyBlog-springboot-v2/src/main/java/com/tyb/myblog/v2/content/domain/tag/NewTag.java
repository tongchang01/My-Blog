package com.tyb.myblog.v2.content.domain.tag;

import com.tyb.myblog.v2.content.domain.ContentName;
import com.tyb.myblog.v2.content.domain.ContentSlug;

/**
 * 待持久化的新标签。
 */
public record NewTag(
        ContentName name,
        ContentSlug slug,
        long createdBy) {

    public static NewTag create(
            String nameZh,
            String nameJa,
            String nameEn,
            String slug,
            long createdBy) {
        if (createdBy <= 0) {
            throw new IllegalArgumentException(
                    "标签创建者 ID 必须为正数");
        }
        return new NewTag(
                ContentName.of(nameZh, nameJa, nameEn),
                ContentSlug.of(slug),
                createdBy);
    }
}
