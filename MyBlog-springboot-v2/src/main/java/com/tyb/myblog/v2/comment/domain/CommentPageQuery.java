package com.tyb.myblog.v2.comment.domain;

/**
 * 前台评论分页查询条件。
 *
 * @param page 页码，从 1 开始
 * @param size 每页大小
 */
public record CommentPageQuery(int page, int size) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    /**
     * 根据外部入参创建安全分页条件。
     */
    public static CommentPageQuery of(Integer page, Integer size) {
        int safePage = page == null ? DEFAULT_PAGE : Math.max(page, 1);
        int safeSize = size == null ? DEFAULT_SIZE : Math.max(size, 1);
        return new CommentPageQuery(safePage, Math.min(safeSize, MAX_SIZE));
    }

    /**
     * 计算 SQL 查询偏移量。
     */
    public int offset() {
        return (page - 1) * size;
    }
}
