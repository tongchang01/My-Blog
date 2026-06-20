package com.tyb.myblog.v2.comment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 评论关键词审核配置。
 *
 * @param blockedKeywords 触发人工审核的关键词
 */
@ConfigurationProperties("myblog.comment.audit")
public record CommentAuditProperties(List<String> blockedKeywords) {

    public CommentAuditProperties {
        blockedKeywords = blockedKeywords == null
                ? List.of()
                : blockedKeywords.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(keyword -> !keyword.isEmpty())
                        .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                        .distinct()
                        .toList();
    }
}
