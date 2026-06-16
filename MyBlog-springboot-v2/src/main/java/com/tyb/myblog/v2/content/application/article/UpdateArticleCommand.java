package com.tyb.myblog.v2.content.application.article;

import com.tyb.myblog.v2.content.domain.article.ArticleStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 后台完整编辑文章的字段命令。
 *
 * <p>password 为 null 且目标状态仍为 PASSWORD 时保留原哈希；
 * 非 null 时替换为新哈希。</p>
 */
public record UpdateArticleCommand(
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
