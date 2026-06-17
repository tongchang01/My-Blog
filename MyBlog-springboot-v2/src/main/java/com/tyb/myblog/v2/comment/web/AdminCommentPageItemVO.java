package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.application.AdminCommentPageResult;
import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;
import com.tyb.myblog.v2.comment.domain.CommentTargetType;

import java.time.LocalDateTime;

public record AdminCommentPageItemVO(
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
        LocalDateTime createdAt) {

    public static AdminCommentPageItemVO from(AdminCommentPageResult.Item item) {
        return new AdminCommentPageItemVO(
                item.id(),
                item.targetType(),
                item.targetId(),
                item.parentId(),
                item.replyToCommentId(),
                item.replyToNickname(),
                item.authorNickname(),
                item.authorEmail(),
                item.authorSite(),
                item.authorIp(),
                item.authorUserAgent(),
                item.contentMd(),
                item.contentHtml(),
                item.auditStatus(),
                item.createdAt());
    }
}
