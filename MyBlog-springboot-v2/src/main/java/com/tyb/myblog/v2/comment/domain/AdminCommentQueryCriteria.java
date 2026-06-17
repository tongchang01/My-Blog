package com.tyb.myblog.v2.comment.domain;

public record AdminCommentQueryCriteria(
        CommentTargetType targetType,
        Long targetId,
        CommentAuditStatus auditStatus,
        String keyword,
        boolean includeDeleted,
        int page,
        int size) {
}
