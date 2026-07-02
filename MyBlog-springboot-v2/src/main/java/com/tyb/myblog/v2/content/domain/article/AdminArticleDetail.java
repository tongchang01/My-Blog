package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台文章详情，包含正文和非敏感审计信息。
 */
public record AdminArticleDetail(
        long id,
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        String body,
        Long categoryId,
        String categoryNameZh,
        long authorId,
        String slug,
        ArticleStatus status,
        HomepageSlot homepageSlot,
        LocalDateTime publishAt,
        Long coverAttachmentId,
        String coverUrl,
        int commentCount,
        List<Long> tagIds,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {

    public AdminArticleDetail(
            long id,
            String titleZh,
            String titleJa,
            String titleEn,
            String summaryZh,
            String summaryJa,
            String summaryEn,
            String body,
            Long categoryId,
            String categoryNameZh,
            long authorId,
            String slug,
            ArticleStatus status,
            LocalDateTime publishAt,
            Long coverAttachmentId,
            String coverUrl,
            int commentCount,
            List<Long> tagIds,
            LocalDateTime createdAt,
            Long createdBy,
            LocalDateTime updatedAt,
            Long updatedBy) {
        this(
                id,
                titleZh,
                titleJa,
                titleEn,
                summaryZh,
                summaryJa,
                summaryEn,
                body,
                categoryId,
                categoryNameZh,
                authorId,
                slug,
                status,
                HomepageSlot.NONE,
                publishAt,
                coverAttachmentId,
                coverUrl,
                commentCount,
                tagIds,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy);
    }

    public AdminArticleDetail {
        homepageSlot = HomepageSlot.normalize(homepageSlot);
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }
}
