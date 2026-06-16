package com.tyb.myblog.v2.comment.domain;

public record CommentContent(
        String markdown,
        String safeHtml) {

    public CommentContent {
        markdown = required(markdown, "评论 Markdown", 5000);
        safeHtml = required(safeHtml, "评论安全 HTML", 20000);
    }

    public static CommentContent of(
            String markdown,
            String safeHtml) {
        return new CommentContent(markdown, safeHtml);
    }

    private static String required(
            String value,
            String field,
            int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + "长度不能超过 " + maxLength);
        }
        return normalized;
    }
}
