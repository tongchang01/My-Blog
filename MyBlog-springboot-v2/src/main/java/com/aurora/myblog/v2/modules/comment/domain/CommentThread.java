package com.aurora.myblog.v2.modules.comment.domain;

import java.time.LocalDateTime;
import java.util.List;

public record CommentThread(
        int id,
        CommentType type,
        Integer topicId,
        CommentAuthor author,
        String content,
        LocalDateTime createdAt,
        List<CommentReply> replies
) {
    public CommentThread {
        replies = replies == null ? List.of() : List.copyOf(replies);
    }
}
