package com.tyb.myblog.v2.content.application.tag;

import com.tyb.myblog.v2.content.domain.tag.Tag;

import java.time.LocalDateTime;

/**
 * 后台标签查询结果。
 */
public record TagResult(
        long id,
        String nameZh,
        String nameJa,
        String nameEn,
        String slug,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {

    public static TagResult from(Tag tag) {
        return new TagResult(
                tag.id(),
                tag.name().zh(),
                tag.name().ja(),
                tag.name().en(),
                tag.slug().value(),
                tag.createdAt(),
                tag.createdBy(),
                tag.updatedAt(),
                tag.updatedBy());
    }
}
