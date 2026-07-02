package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.AdminArticleDetail;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.HomepageSlot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台文章详情应用结果，不包含访问密码哈希。
 */
public record AdminArticleDetailResult(
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

    public AdminArticleDetailResult(
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

    public AdminArticleDetailResult {
        homepageSlot = HomepageSlot.normalize(homepageSlot);
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }

    public static AdminArticleDetailResult from(
            AdminArticleDetail detail,
            String coverUrl,
            boolean includeBody) {
        return new AdminArticleDetailResult(
                detail.id(),
                detail.titleZh(),
                detail.titleJa(),
                detail.titleEn(),
                detail.summaryZh(),
                detail.summaryJa(),
                detail.summaryEn(),
                includeBody ? detail.body() : null,
                detail.categoryId(),
                detail.categoryNameZh(),
                detail.authorId(),
                detail.slug(),
                detail.status(),
                detail.homepageSlot(),
                detail.publishAt(),
                detail.coverAttachmentId(),
                coverUrl,
                detail.commentCount(),
                detail.tagIds(),
                detail.createdAt(),
                detail.createdBy(),
                detail.updatedAt(),
                detail.updatedBy());
    }
}
