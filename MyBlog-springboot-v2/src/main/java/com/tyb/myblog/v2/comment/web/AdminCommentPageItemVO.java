package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.AdminCommentPageResult;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;

import java.time.LocalDateTime;

public record AdminCommentPageItemVO(
        String id,
        CommentTargetType targetType,
        String targetId,
        String parentId,
        String replyToCommentId,
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

    public static AdminCommentPageItemVO from(AdminCommentPageResult.Item item) {
        return new AdminCommentPageItemVO(
                Long.toString(item.id()),
                item.targetType(),
                Long.toString(item.targetId()),
                item.parentId() == null ? null : Long.toString(item.parentId()),
                item.replyToCommentId() == null ? null : Long.toString(item.replyToCommentId()),
                item.replyToNickname(),
                item.authorNickname(),
                item.authorEmail(),
                item.authorSite(),
                item.authorIp(),
                item.authorUserAgent(),
                item.contentMd(),
                item.contentHtml(),
                item.auditStatus(),
                item.createdAt(),
                item.deleted());
    }
}
