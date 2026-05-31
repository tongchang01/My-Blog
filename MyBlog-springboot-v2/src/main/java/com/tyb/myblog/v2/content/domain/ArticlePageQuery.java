package com.tyb.myblog.v2.content.domain;

/**
 * 文章分页查询条件。
 *
 * @param page 页码，从 1 开始
 * @param size 每页大小
 */
public record ArticlePageQuery(int page, int size) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    /**
     * 根据外部入参创建安全分页条件。
     */
    public static ArticlePageQuery of(Integer page, Integer size) {
        int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new ArticlePageQuery(normalizedPage, normalizedSize);
    }

    /**
     * 计算 SQL 查询偏移量。
     */
    public int offset() {
        return (page - 1) * size;
    }
}
