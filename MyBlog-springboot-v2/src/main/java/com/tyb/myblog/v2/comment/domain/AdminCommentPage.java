package com.tyb.myblog.v2.comment.domain;

import java.util.List;

public record AdminCommentPage(
        List<AdminCommentPageItem> records,
        long total,
        int page,
        int size) {

    public AdminCommentPage {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
