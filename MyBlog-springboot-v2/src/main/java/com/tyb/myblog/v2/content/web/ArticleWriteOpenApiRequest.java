package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章写入 OpenAPI 展示模型；运行时使用 presence-aware request。
 */
public record ArticleWriteOpenApiRequest(
        String titleZh,
        String titleJa,
        String titleEn,
        String summaryZh,
        String summaryJa,
        String summaryEn,
        String body,
        Long categoryId,
        List<Long> tagIds,
        String slug,
        ArticleStatus status,
        String password,
        LocalDateTime publishAt,
        Long coverAttachmentId) {
}
