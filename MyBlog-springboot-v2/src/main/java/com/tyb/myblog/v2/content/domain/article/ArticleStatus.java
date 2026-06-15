package com.tyb.myblog.v2.content.domain.article;

import java.util.Arrays;

/**
 * 文章业务状态及其数据库编码。
 */
public enum ArticleStatus {

    DRAFT(1),
    PUBLISHED(2),
    PRIVATE(3),
    PASSWORD(4),
    SCHEDULED(5);

    private final int databaseValue;

    ArticleStatus(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public static ArticleStatus fromDatabase(int value) {
        return Arrays.stream(values())
                .filter(status -> status.databaseValue == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "未知文章状态: " + value));
    }
}
