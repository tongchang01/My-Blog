package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台文章分页条目响应。
 */
public record AdminArticlePageItemVO(
        @Schema(format = "int64") String id,
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        @Schema(format = "int64") String categoryId,
        String categoryNameZh,
        String slug,
        ArticleStatus status,
        LocalDateTime publishAt,
        @Schema(format = "int64") String coverAttachmentId,
        String coverUrl,
        int commentCount,
        @ArraySchema(schema = @Schema(
                implementation = String.class,
                format = "int64"))
        List<String> tagIds,
        LocalDateTime createdAt,
        @Schema(format = "int64") String createdBy,
        LocalDateTime updatedAt,
        @Schema(format = "int64") String updatedBy) {
}
