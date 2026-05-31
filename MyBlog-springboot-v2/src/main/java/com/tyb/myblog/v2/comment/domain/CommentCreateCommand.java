package com.tyb.myblog.v2.comment.domain;

public record CommentCreateCommand(
        int userId,
        CommentType type,
        Integer topicId,
        Integer parentId,
        Integer replyUserId,
        String content,
        String clientIp,
        String userAgent
) {
}
