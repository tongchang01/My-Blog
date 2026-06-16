package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开文章分页条目不包含正文，PASSWORD 文章仅显示锁定元数据。
 */
public record PublicArticlePageItem(
        long id,
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
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
        LocalDateTime createdAt) {
}
