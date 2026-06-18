package com.tyb.myblog.v2.stats.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 已完成隐私处理、可直接持久化的页面访问事件。
 */
public record PageViewEvent(
        Long articleId,
        StatsLanguage language,
        String visitorHash,
        String referrer,
        LocalDateTime createdAt) {

    private static final Pattern SHA_256_PATTERN =
            Pattern.compile("[0-9a-f]{64}");

    public static PageViewEvent create(
            Long articleId,
            StatsLanguage language,
            String visitorHash,
            String referrer,
            LocalDateTime createdAt) {
        if (articleId != null && articleId <= 0) {
            throw new IllegalArgumentException("文章 ID 必须为正数");
        }
        Objects.requireNonNull(language, "统计语言不能为空");
        if (visitorHash == null
                || !SHA_256_PATTERN.matcher(visitorHash).matches()) {
            throw new IllegalArgumentException(
                    "访客标识必须是 64 位小写十六进制摘要");
        }
        if (referrer != null && referrer.length() > 512) {
            throw new IllegalArgumentException("来源地址不能超过 512 字符");
        }
        Objects.requireNonNull(createdAt, "访问时间不能为空");
        return new PageViewEvent(
                articleId,
                language,
                visitorHash,
                referrer,
                createdAt);
    }
}
