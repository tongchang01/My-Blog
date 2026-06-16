package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章聚合根。
 */
public record Article(
        long id,
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
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy,
        boolean deleted,
        LocalDateTime deletedAt,
        Long deletedBy) {

    public static Article reconstitute(
            long id,
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
            LocalDateTime createdAt,
            Long createdBy,
            LocalDateTime updatedAt,
            Long updatedBy,
            boolean deleted,
            LocalDateTime deletedAt,
            Long deletedBy) {
        if (id <= 0) {
            throw new IllegalArgumentException("文章 ID 必须为正数");
        }
        ArticleValidation.ArticleValues values =
                ArticleValidation.validateStored(
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
                                commentCount,
                                tagIds));
        validateAudit(
                createdAt,
                createdBy,
                updatedAt,
                deleted,
                deletedAt,
                deletedBy);
        return new Article(
                id,
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
                createdAt,
                createdBy,
                updatedAt,
                updatedBy,
                deleted,
                deletedAt,
                deletedBy);
    }

    public Article replace(
            String titleZh,
            String titleJa,
            String titleEn,
            String summaryZh,
            String summaryJa,
            String summaryEn,
            String body,
            Long categoryId,
            String slug,
            ArticleStatus status,
            String accessPassword,
            LocalDateTime publishAt,
            Long coverAttachmentId,
            List<Long> tagIds,
            LocalDateTime updatedAt,
            Long updatedBy) {
        return reconstitute(
                id,
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
                commentCount,
                tagIds,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy,
                deleted,
                deletedAt,
                deletedBy);
    }

    public String existingPassword() {
        return accessPassword;
    }

    private static void validateAudit(
            LocalDateTime createdAt,
            Long createdBy,
            LocalDateTime updatedAt,
            boolean deleted,
            LocalDateTime deletedAt,
            Long deletedBy) {
        if (createdAt == null || updatedAt == null) {
            throw new IllegalArgumentException(
                    "文章创建和更新时间不能为空");
        }
        if (createdBy != null && createdBy <= 0) {
            throw new IllegalArgumentException("创建者 ID 必须为正数");
        }
        if (deleted) {
            if (deletedAt == null || deletedBy == null || deletedBy <= 0) {
                throw new IllegalArgumentException(
                        "已删除文章必须包含删除审计");
            }
        } else if (deletedAt != null || deletedBy != null) {
            throw new IllegalArgumentException(
                    "未删除文章不得包含删除审计");
        }
    }
}
