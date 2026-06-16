package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArticlePageResult(
        List<Item> records,
        long total,
        int page,
        int size) {

    public record Item(
            long id,
            String title,
            String summary,
            Long categoryId,
            String categoryName,
            String slug,
            ArticleStatus status,
            LocalDateTime publishAt,
            Long coverAttachmentId,
            String coverUrl,
            int commentCount,
            List<PublicArticleTagResult> tags,
            LocalDateTime createdAt,
            boolean locked) {
    }
}
