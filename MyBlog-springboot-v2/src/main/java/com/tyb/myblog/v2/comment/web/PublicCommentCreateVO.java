package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.CommentCreateResult;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record PublicCommentCreateVO(
        @Schema(format = "int64") String id,
        CommentAuditStatus auditStatus) {

    public static PublicCommentCreateVO from(CommentCreateResult result) {
        return new PublicCommentCreateVO(
                Long.toString(result.id()), result.auditStatus());
    }
}
