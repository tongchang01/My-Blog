package com.tyb.myblog.v2.comment.application;

import java.time.LocalDateTime;
import java.util.List;

public record CommentPageResult(
        List<Item> records,
        long total,
        int page,
        int size) {

    public CommentPageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }

    public record Item(
            long id,
            Long parentId,
            Long replyToCommentId,
            String replyToNickname,
            String authorNickname,
            String authorSite,
            String contentHtml,
            LocalDateTime createdAt,
            List<Item> replies) {

        public Item {
            replies = replies == null ? List.of() : List.copyOf(replies);
        }
    }
}
