package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开文章详情只读模型，PASSWORD 只用于判定锁定状态，不暴露正文。
 */
public record PublicArticleDetail(
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
        String categoryNameJa,
        String categoryNameEn,
        String slug,
        ArticleStatus status,
        LocalDateTime publishAt,
        Long coverAttachmentId,
        String coverUrl,
        int commentCount,
        List<ArticleTagView> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
