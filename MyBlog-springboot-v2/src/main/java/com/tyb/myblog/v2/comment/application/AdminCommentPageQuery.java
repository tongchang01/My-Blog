package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;

public record AdminCommentPageQuery(
        CommentTargetType targetType,
        Long targetId,
        CommentAuditStatus auditStatus,
        String keyword,
        boolean includeDeleted,
        int page,
        int size) {

    public AdminCommentPageQuery {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
    }
}
