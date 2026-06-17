package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.CommentCreateCommand;

public record PublicCommentCreateRequest(
        String nickname,
        String email,
        String site,
        String contentMd,
        Long replyToCommentId) {

    public CommentCreateCommand toCommand(
            long targetId,
            String clientIp,
            String userAgent) {
        return new CommentCreateCommand(
                targetId,
                nickname,
                email,
                site,
                contentMd,
                replyToCommentId,
                clientIp,
                userAgent);
    }
}
