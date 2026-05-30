package com.aurora.myblog.v2.modules.comment.domain;

import java.time.LocalDateTime;

public record CommentReply(
        int id,
        int parentId,
        CommentAuthor author,
        CommentAuthor replyUser,
        String content,
        LocalDateTime createdAt
) {
}
