package com.tyb.myblog.v2.comment.domain;

import java.util.Arrays;

public enum CommentAuditStatus {
    PASS(1),
    PENDING(2),
    HIDDEN(3);

    private final int databaseValue;

    CommentAuditStatus(int databaseValue) {
        this.databaseValue = databaseValue;
    }

    public int databaseValue() {
        return databaseValue;
    }

    public boolean publiclyVisible() {
        return this == PASS;
    }

    public static CommentAuditStatus fromDatabase(int value) {
        return Arrays.stream(values())
                .filter(status -> status.databaseValue == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "未知评论审核状态：" + value));
    }
}
