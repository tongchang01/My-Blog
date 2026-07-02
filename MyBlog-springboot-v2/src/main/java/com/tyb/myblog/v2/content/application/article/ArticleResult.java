package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.Article;
import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import com.tyb.myblog.v2.content.domain.article.HomepageSlot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章写操作后的非敏感结果，不暴露密码哈希。
 */
public record ArticleResult(
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
        HomepageSlot homepageSlot,
        LocalDateTime publishAt,
        Long coverAttachmentId,
        int commentCount,
        List<Long> tagIds,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {

    public ArticleResult(
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
            LocalDateTime publishAt,
            Long coverAttachmentId,
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
                authorId,
                slug,
                status,
                HomepageSlot.NONE,
                publishAt,
                coverAttachmentId,
                commentCount,
                tagIds,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy);
    }

    public static ArticleResult from(Article article) {
        return new ArticleResult(
                article.id(),
                article.titleZh(),
                article.titleJa(),
                article.titleEn(),
                article.summaryZh(),
                article.summaryJa(),
                article.summaryEn(),
                article.body(),
                article.categoryId(),
                article.authorId(),
                article.slug(),
                article.status(),
                article.homepageSlot(),
                article.publishAt(),
                article.coverAttachmentId(),
                article.commentCount(),
                article.tagIds(),
                article.createdAt(),
                article.createdBy(),
                article.updatedAt(),
                article.updatedBy());
    }
}
