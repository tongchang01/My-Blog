package com.tyb.myblog.v2.comment.infrastructure.persistence.projection;

import java.time.LocalDateTime;

public record AdminCommentPageRow(
        long id,
        Integer targetType,
        Long targetId,
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
        Integer auditStatus,
        LocalDateTime createdAt,
        Integer deleted) {
}
