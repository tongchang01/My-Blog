package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record DeletedArticlePageItemVO(
        @Schema(format = "int64") String id,
        String titleZh,
        String titleJa,
        String titleEn,
        ArticleStatus status,
        @Schema(format = "int64") String categoryId,
        LocalDateTime deletedAt,
        @Schema(format = "int64") String deletedBy) {
}
