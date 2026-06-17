package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;

import java.time.LocalDateTime;
import java.util.List;

public record AdminCommentPageResult(
        List<Item> records,
        long total,
        int page,
        int size) {

    public AdminCommentPageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }

    public record Item(
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
}
