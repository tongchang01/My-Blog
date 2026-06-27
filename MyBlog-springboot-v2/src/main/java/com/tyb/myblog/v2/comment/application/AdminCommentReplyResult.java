package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;

public record AdminCommentReplyResult(
        long id,
        CommentAuditStatus auditStatus) {
}
