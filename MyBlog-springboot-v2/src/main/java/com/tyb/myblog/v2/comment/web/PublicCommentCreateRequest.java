package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.CommentCreateCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PublicCommentCreateRequest(
        @NotBlank @Size(max = 64) String nickname,
        @NotBlank @Email @Size(max = 128) String email,
        @Size(max = 255) String site,
        @NotBlank @Size(max = 5000) String contentMd,
        @Positive Long replyToCommentId) {

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
