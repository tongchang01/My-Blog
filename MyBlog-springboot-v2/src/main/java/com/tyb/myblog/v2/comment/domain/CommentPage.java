package com.tyb.myblog.v2.comment.domain;

import java.util.List;

public record CommentPage(
        List<CommentPageItem> records,
        long total,
        int page,
        int size) {

    public CommentPage {
        records = records == null ? List.of() : List.copyOf(records);
    }
}
