package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.AdminCommentReplyResult;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;

public record AdminCommentReplyVO(
        String id,
        CommentAuditStatus auditStatus) {

    public static AdminCommentReplyVO from(AdminCommentReplyResult result) {
        return new AdminCommentReplyVO(
                String.valueOf(result.id()),
                result.auditStatus());
    }
}
