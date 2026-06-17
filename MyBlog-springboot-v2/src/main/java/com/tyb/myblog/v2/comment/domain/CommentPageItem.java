package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;
import java.util.List;

public record CommentPageItem(
        long id,
        Long parentId,
        Long replyToCommentId,
        String replyToNickname,
        String authorNickname,
        String authorSite,
        String contentHtml,
        LocalDateTime createdAt,
        List<CommentPageItem> replies) {

    public CommentPageItem {
        replies = replies == null ? List.of() : List.copyOf(replies);
    }
}
