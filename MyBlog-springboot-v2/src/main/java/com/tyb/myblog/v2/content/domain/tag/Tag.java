package com.tyb.myblog.v2.content.domain.tag;

import com.tyb.myblog.v2.content.domain.ContentName;
import com.tyb.myblog.v2.content.domain.ContentSlug;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 有效标签聚合。
 */
public record Tag(
        long id,
        ContentName name,
        ContentSlug slug,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {

    public static Tag reconstitute(
            long id,
            String nameZh,
            String nameJa,
            String nameEn,
            String slug,
            LocalDateTime createdAt,
            Long createdBy,
            LocalDateTime updatedAt,
            Long updatedBy) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "标签 ID 必须为正数");
        }
        return new Tag(
                id,
                ContentName.of(nameZh, nameJa, nameEn),
                ContentSlug.of(slug),
                Objects.requireNonNull(
                        createdAt, "标签创建时间不能为空"),
                createdBy,
                Objects.requireNonNull(
                        updatedAt, "标签更新时间不能为空"),
                updatedBy);
    }

    public Tag replace(
            String nameZh,
            String nameJa,
            String nameEn,
            String slug) {
        return reconstitute(
                id,
                nameZh,
                nameJa,
                nameEn,
                slug,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy);
    }
}
