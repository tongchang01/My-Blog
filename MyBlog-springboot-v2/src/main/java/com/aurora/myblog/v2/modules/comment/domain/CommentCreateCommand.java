package com.aurora.myblog.v2.modules.comment.domain;

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
