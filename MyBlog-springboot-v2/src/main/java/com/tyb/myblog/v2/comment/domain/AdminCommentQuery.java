package com.tyb.myblog.v2.comment.domain;

public record AdminCommentQuery(
        CommentType type,
        Integer topicId,
        Boolean reviewed,
        String keyword,
        Boolean deleted,
        int page,
        int size
) {
    public AdminCommentQuery(CommentType type, Integer topicId, Boolean reviewed, String keyword, int page, int size) {
        this(type, topicId, reviewed, keyword, false, page, size);
    }

    public AdminCommentQuery {
        page = Math.max(page, 1);
        size = Math.max(1, Math.min(size, 100));
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        deleted = deleted == null ? false : deleted;
    }

    public int offset() {
        return (page - 1) * size;
    }
}
