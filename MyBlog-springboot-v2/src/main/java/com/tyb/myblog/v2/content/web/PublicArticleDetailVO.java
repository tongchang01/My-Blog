package com.tyb.myblog.v2.content.web;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArticleDetailVO(
        String id,
        String title,
        String summary,
        String body,
        String categoryId,
        String categoryName,
        String slug,
        LocalDateTime publishAt,
        String coverUrl,
        int commentCount,
        List<PublicArticleTagVO> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean locked) {
}
