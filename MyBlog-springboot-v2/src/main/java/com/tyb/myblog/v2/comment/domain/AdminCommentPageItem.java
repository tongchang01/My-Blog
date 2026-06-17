package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;

public record AdminCommentPageItem(
        long id,
        CommentTargetType targetType,
        long targetId,
        Long parentId,
        Long replyToCommentId,
        String replyToNickname,
        String authorNickname,
        String authorEmail,
        String authorSite,
        String authorIp,
        String authorUserAgent,
        String contentMd,
        String contentHtml,
        CommentAuditStatus auditStatus,
        LocalDateTime createdAt,
        boolean deleted) {
}
