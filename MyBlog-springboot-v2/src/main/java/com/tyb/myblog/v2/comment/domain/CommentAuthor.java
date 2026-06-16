package com.tyb.myblog.v2.comment.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public record CommentAuthor(
        Long userId,
        String nickname,
        String email,
        String site,
        String ip,
        String userAgent) {

    private static final Pattern EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public CommentAuthor {
        if (userId != null && userId <= 0) {
            throw new IllegalArgumentException("评论作者用户 ID 必须为正数");
        }
        nickname = required(nickname, "评论昵称", 64);
        email = required(email, "评论邮箱", 128)
                .toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("评论邮箱格式非法");
        }
        site = optional(site, "个人站点", 255);
        if (site != null
                && !site.startsWith("http://")
                && !site.startsWith("https://")) {
            throw new IllegalArgumentException("个人站点只允许 http/https");
        }
        ip = optional(ip, "评论 IP", 45);
        userAgent = optional(userAgent, "评论 UA", 512);
    }

    public static CommentAuthor guest(
            String nickname,
            String email,
            String site,
            String ip,
            String userAgent) {
        return new CommentAuthor(
                null,
                nickname,
                email,
                site,
                ip,
                userAgent);
    }

    private static String required(
            String value,
            String field,
            int maxLength) {
        String normalized = optional(value, field, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return normalized;
    }

    private static String optional(
            String value,
            String field,
            int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + "长度不能超过 " + maxLength);
        }
        return normalized;
    }
}
