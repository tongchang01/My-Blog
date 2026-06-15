package com.tyb.myblog.v2.content.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 分类和标签共用的 URL 别名。
 */
public record ContentSlug(String value) {

    private static final int MAX_LENGTH = 64;
    private static final Pattern PATTERN =
            Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    public static ContentSlug of(String value) {
        if (value == null) {
            throw new IllegalArgumentException("slug 不能为空");
        }
        String normalized =
                value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()
                || normalized.length() > MAX_LENGTH
                || !PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("slug 格式非法");
        }
        return new ContentSlug(normalized);
    }
}
