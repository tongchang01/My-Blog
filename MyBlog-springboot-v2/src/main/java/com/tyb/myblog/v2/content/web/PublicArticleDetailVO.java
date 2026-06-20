package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.article.PublicArticleTagResult;
import java.time.LocalDateTime;
import java.util.List;

public record PublicArticleDetailVO(
        long id,
        String title,
        String summary,
        String body,
        Long categoryId,
        String categoryName,
        String slug,
        LocalDateTime publishAt,
        String coverUrl,
        int commentCount,
        List<PublicArticleTagResult> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean locked) {
}
