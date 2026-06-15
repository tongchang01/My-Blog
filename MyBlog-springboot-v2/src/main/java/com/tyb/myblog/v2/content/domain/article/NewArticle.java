package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 尚未分配主键的新文章。
 */
public record NewArticle(
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        String body,
        Long categoryId,
        long authorId,
        String slug,
        ArticleStatus status,
        String accessPassword,
        LocalDateTime publishAt,
        Long coverAttachmentId,
        int commentCount,
        List<Long> tagIds,
        long createdBy,
        LocalDateTime createdAt) {

    public static NewArticle create(
            String titleZh,
            String titleJa,
            String titleEn,
            String summaryZh,
            String summaryJa,
            String summaryEn,
            String body,
            Long categoryId,
            long authorId,
            String slug,
            ArticleStatus status,
            String accessPassword,
            LocalDateTime publishAt,
            Long coverAttachmentId,
            List<Long> tagIds,
            long createdBy,
            LocalDateTime now) {
        if (createdBy <= 0) {
            throw new IllegalArgumentException("创建者 ID 必须为正数");
        }
        ArticleValidation.ArticleValues values =
                ArticleValidation.validateForWrite(
                        new ArticleValidation.ArticleValues(
                                titleZh,
                                titleJa,
                                titleEn,
                                summaryZh,
                                summaryJa,
                                summaryEn,
                                body,
                                categoryId,
                                authorId,
                                slug,
                                status,
                                accessPassword,
                                publishAt,
                                coverAttachmentId,
                                0,
                                tagIds),
                        now);
        return new NewArticle(
                values.titleZh(),
                values.titleJa(),
                values.titleEn(),
                values.summaryZh(),
                values.summaryJa(),
                values.summaryEn(),
                values.body(),
                values.categoryId(),
                values.authorId(),
                values.slug(),
                values.status(),
                values.accessPassword(),
                values.publishAt(),
                values.coverAttachmentId(),
                values.commentCount(),
                values.tagIds(),
                createdBy,
                now);
    }
}
