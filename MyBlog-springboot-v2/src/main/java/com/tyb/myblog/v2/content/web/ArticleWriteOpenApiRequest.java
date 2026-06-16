package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章写入 OpenAPI 展示模型；运行时使用 presence-aware request。
 */
public record ArticleWriteOpenApiRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String titleZh,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String titleJa,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String titleEn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String summaryZh,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String summaryJa,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String summaryEn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String body,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long categoryId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> tagIds,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String slug,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        ArticleStatus status,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String password,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        LocalDateTime publishAt,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"integer", "null"})
        Long coverAttachmentId) {
}
