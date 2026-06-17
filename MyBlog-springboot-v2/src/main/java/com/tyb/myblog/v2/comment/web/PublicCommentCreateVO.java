package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.CommentCreateResult;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;

public record PublicCommentCreateVO(
        long id,
        CommentAuditStatus auditStatus) {

    public static PublicCommentCreateVO from(CommentCreateResult result) {
        return new PublicCommentCreateVO(result.id(), result.auditStatus());
    }
}
