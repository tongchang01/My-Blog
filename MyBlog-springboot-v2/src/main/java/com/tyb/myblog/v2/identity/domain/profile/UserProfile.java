package com.tyb.myblog.v2.identity.domain.profile;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * 用户公开资料领域模型，负责统一规范化和校验可编辑资料字段。
 */
public record UserProfile(
        long userId,
        String nickname,
        String avatarUrl,
        String bioZh,
        String bioJa,
        String bioEn,
        String location,
        String website,
        String emailPublic,
        String githubUrl,
        String twitterUrl,
        String linkedinUrl,
        String zhihuUrl,
        String qiitaUrl,
        String juejinUrl
) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * 创建经过规范化和完整校验的用户资料。
     *
     * @return 可安全持久化的用户资料
     */
    public static UserProfile create(
            long userId,
            String nickname,
            String avatarUrl,
            String bioZh,
            String bioJa,
            String bioEn,
            String location,
            String website,
            String emailPublic,
            String githubUrl,
            String twitterUrl,
            String linkedinUrl,
            String zhihuUrl,
            String qiitaUrl,
            String juejinUrl) {
        if (userId <= 0) {
            throw new IllegalArgumentException("用户 ID 必须为正数");
        }

        return new UserProfile(
                userId,
                required(nickname, "昵称", 64),
                url(avatarUrl, "头像 URL"),
                optional(bioZh, "中文简介", 5000),
                optional(bioJa, "日文简介", 5000),
                optional(bioEn, "英文简介", 5000),
                optional(location, "所在地", 64),
                url(website, "个人主页"),
                email(emailPublic),
                url(githubUrl, "GitHub 链接"),
                url(twitterUrl, "Twitter 链接"),
                url(linkedinUrl, "LinkedIn 链接"),
                url(zhihuUrl, "知乎链接"),
                url(qiitaUrl, "Qiita 链接"),
                url(juejinUrl, "掘金链接"));
    }

    private static String required(String value, String field, int maxLength) {
        String normalized = optional(value, field, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return normalized;
    }

    private static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private static String email(String value) {
        String normalized = optional(value, "公开邮箱", 128);
        if (normalized != null && !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("公开邮箱格式错误");
        }
        return normalized;
    }

    private static String url(String value, String field) {
        String normalized = optional(value, field, 255);
        if (normalized == null) {
            return null;
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + "格式错误", exception);
        }
        if (uri.getHost() == null
                || (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException(field + "仅支持 HTTP 或 HTTPS");
        }
        return normalized;
    }
}
