package com.tyb.myblog.v2.content.web;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArticlePageItemVO(
        String id,
        String title,
        String summary,
        String categoryId,
        String categoryName,
        String slug,
        LocalDateTime publishAt,
        String coverUrl,
        int commentCount,
        List<PublicArticleTagVO> tags,
        LocalDateTime createdAt,
        boolean locked) {
}
