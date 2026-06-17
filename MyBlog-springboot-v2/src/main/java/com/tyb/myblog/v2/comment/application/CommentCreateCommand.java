package com.tyb.myblog.v2.comment.application;

public record CommentCreateCommand(
        long targetId,
        String nickname,
        String email,
        String site,
        String contentMd,
        Long replyToCommentId,
        String clientIp,
        String userAgent) {
}
