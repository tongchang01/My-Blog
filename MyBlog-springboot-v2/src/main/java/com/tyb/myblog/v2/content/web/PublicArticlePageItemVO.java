package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.application.article.PublicArticleTagResult;
import java.time.LocalDateTime;
import java.util.List;

public record PublicArticlePageItemVO(
        long id,
        String title,
        String summary,
        Long categoryId,
        String categoryName,
        String slug,
        LocalDateTime publishAt,
        String coverUrl,
        int commentCount,
        List<PublicArticleTagResult> tags,
        LocalDateTime createdAt,
        boolean locked) {
}
