package com.aurora.myblog.v2.modules.comment.domain;

public record AdminCommentQuery(
        CommentType type,
        Integer topicId,
        Boolean reviewed,
        String keyword,
        int page,
        int size
) {
    public AdminCommentQuery {
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    public int offset() {
        return (page - 1) * size;
    }
}
