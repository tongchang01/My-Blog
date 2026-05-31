package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;

public record AdminCommentItem(
        int id,
        CommentType type,
        Integer topicId,
        String topicTitle,
        Integer parentId,
        Integer replyUserId,
        int userId,
        String nickname,
        String avatar,
        String content,
        boolean reviewed,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
