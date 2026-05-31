package com.tyb.myblog.v2.content.domain;

public record ArticlePageQuery(int page, int size) {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    public static ArticlePageQuery of(Integer page, Integer size) {
        int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int normalizedSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new ArticlePageQuery(normalizedPage, normalizedSize);
    }

    public int offset() {
        return (page - 1) * size;
    }
}
