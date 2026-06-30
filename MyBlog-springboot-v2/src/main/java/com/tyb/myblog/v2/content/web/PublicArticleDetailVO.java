package com.tyb.myblog.v2.content.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record PublicArticleDetailVO(
        @Schema(format = "int64") String id,
        String title,
        String summary,
        String body,
        @Schema(format = "int64") String categoryId,
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
