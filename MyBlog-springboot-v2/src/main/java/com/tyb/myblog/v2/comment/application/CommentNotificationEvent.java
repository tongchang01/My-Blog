package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;

public record CommentNotificationEvent(
        long commentId,
        Long replyToCommentId,
        String authorNickname,
        String contentHtml,
        CommentAuditStatus auditStatus) {
}
