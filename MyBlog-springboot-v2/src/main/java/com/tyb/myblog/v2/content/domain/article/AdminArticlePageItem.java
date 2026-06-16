package com.tyb.myblog.v2.content.domain.article;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台文章分页条目。
 */
public record AdminArticlePageItem(
        long id,
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        Long categoryId,
        String categoryNameZh,
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

    public AdminArticlePageItem {
        tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
    }
}
