package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台文章分页应用结果。
 */
public record AdminArticlePageResult(
        List<Item> records,
        long total,
        int page,
        int size) {

    public AdminArticlePageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }

    public record Item(
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

        public Item {
            tagIds = tagIds == null ? List.of() : List.copyOf(tagIds);
        }
    }
}
