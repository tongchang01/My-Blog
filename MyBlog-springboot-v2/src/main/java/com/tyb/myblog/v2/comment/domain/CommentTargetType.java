package com.tyb.myblog.v2.comment.domain;

import java.util.Arrays;

public enum CommentTargetType {
    ARTICLE(1),
    GUESTBOOK(2);

    private final int databaseValue;

    CommentTargetType(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public static CommentTargetType fromDatabase(int value) {
        return Arrays.stream(values())
                .filter(type -> type.databaseValue == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "未知评论目标类型：" + value));
    }
}
