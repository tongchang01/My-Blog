package com.aurora.myblog.v2.modules.comment.domain;

public record CommentPageQuery(int page, int size) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    public static CommentPageQuery of(Integer page, Integer size) {
        int safePage = page == null ? DEFAULT_PAGE : Math.max(page, 1);
        int safeSize = size == null ? DEFAULT_SIZE : Math.max(size, 1);
        return new CommentPageQuery(safePage, Math.min(safeSize, MAX_SIZE));
    }

    public int offset() {
        return (page - 1) * size;
    }
}
