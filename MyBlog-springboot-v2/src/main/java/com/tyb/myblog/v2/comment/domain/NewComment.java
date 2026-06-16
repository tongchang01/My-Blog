package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;

public record NewComment(
        CommentTarget target,
        Long parentId,
        Long replyToCommentId,
        Long replyToUserId,
        String replyToNickname,
        CommentAuthor author,
        CommentContent content,
        CommentAuditStatus auditStatus,
        LocalDateTime createdAt,
        Long createdBy) {

    public static NewComment create(
            CommentTarget target,
            Long parentId,
            Long replyToCommentId,
            Long replyToUserId,
            String replyToNickname,
            Long authorUserId,
            String authorNickname,
            String authorEmail,
            String authorSite,
            String authorIp,
            String authorUserAgent,
            String contentMarkdown,
            String contentHtml,
            CommentAuditStatus auditStatus,
            LocalDateTime createdAt,
            Long createdBy) {
        return new NewComment(
                target,
                parentId,
                replyToCommentId,
                replyToUserId,
                normalize(replyToNickname, 64),
                new CommentAuthor(
                        authorUserId,
                        authorNickname,
                        authorEmail,
                        authorSite,
                        authorIp,
                        authorUserAgent),
                CommentContent.of(contentMarkdown, contentHtml),
                auditStatus,
                createdAt,
                createdBy);
    }

    public NewComment {
        validateTargetAndReply(target, parentId, replyToCommentId);
        if (replyToUserId != null && replyToUserId <= 0) {
            throw new IllegalArgumentException("被回复用户 ID 必须为正数");
        }
        if (auditStatus == null) {
            throw new IllegalArgumentException("评论审核状态不能为空");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("评论创建时间不能为空");
        }
        if (createdBy != null && createdBy <= 0) {
            throw new IllegalArgumentException("评论创建者 ID 必须为正数");
        }
    }

    private static void validateTargetAndReply(
            CommentTarget target,
            Long parentId,
            Long replyToCommentId) {
        if (target == null) {
            throw new IllegalArgumentException("评论目标不能为空");
        }
        if ((parentId == null) != (replyToCommentId == null)) {
            throw new IllegalArgumentException(
                    "回复评论必须同时包含 parentId 和 replyToCommentId");
        }
        if (parentId != null && parentId <= 0) {
            throw new IllegalArgumentException("父评论 ID 必须为正数");
        }
        if (replyToCommentId != null && replyToCommentId <= 0) {
            throw new IllegalArgumentException("被回复评论 ID 必须为正数");
        }
    }

    static void validateForStored(
            CommentTarget target,
            Long parentId,
            Long replyToCommentId,
            Long replyToUserId,
            CommentAuditStatus auditStatus,
            LocalDateTime createdAt,
            Long createdBy) {
        validateTargetAndReply(target, parentId, replyToCommentId);
        if (replyToUserId != null && replyToUserId <= 0) {
            throw new IllegalArgumentException("被回复用户 ID 必须为正数");
        }
        if (auditStatus == null) {
            throw new IllegalArgumentException("评论审核状态不能为空");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("评论创建时间不能为空");
        }
        if (createdBy != null && createdBy <= 0) {
            throw new IllegalArgumentException("评论创建者 ID 必须为正数");
        }
    }

    private static String normalize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("被回复昵称长度不能超过 " + maxLength);
        }
        return normalized;
    }
}
