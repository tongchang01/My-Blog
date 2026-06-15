package com.tyb.myblog.v2.content.domain.article;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 可选文章 URL 别名，不承担唯一身份。
 */
public record ArticleSlug(String value) {

    private static final int MAX_LENGTH = 160;
    private static final Pattern PATTERN =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    public static Optional<ArticleSlug> optional(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_LENGTH
                || !PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("文章 slug 格式非法");
        }
        return Optional.of(new ArticleSlug(normalized));
    }
}
