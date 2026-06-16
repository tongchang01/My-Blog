package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;

public record Comment(
        long id,
        CommentTarget target,
        Long parentId,
        Long replyToCommentId,
        Long replyToUserId,
        String replyToNickname,
        CommentAuthor author,
        CommentContent content,
        CommentAuditStatus auditStatus,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy,
        boolean deleted,
        LocalDateTime deletedAt,
        Long deletedBy) {

    public static Comment reconstitute(
            long id,
            CommentTarget target,
            Long parentId,
            Long replyToCommentId,
            Long replyToUserId,
            String replyToNickname,
            CommentAuthor author,
            CommentContent content,
            CommentAuditStatus auditStatus,
            LocalDateTime createdAt,
            Long createdBy,
            LocalDateTime updatedAt,
            Long updatedBy,
            boolean deleted,
            LocalDateTime deletedAt,
            Long deletedBy) {
        if (id <= 0) {
            throw new IllegalArgumentException("评论 ID 必须为正数");
        }
        NewComment.validateForStored(
                target,
                parentId,
                replyToCommentId,
                replyToUserId,
                auditStatus,
                createdAt,
                createdBy);
        if (author == null || content == null) {
            throw new IllegalArgumentException("评论作者和内容不能为空");
        }
        validateAudit(updatedAt, deleted, deletedAt, deletedBy);
        return new Comment(
                id,
                target,
                parentId,
                replyToCommentId,
                replyToUserId,
                replyToNickname,
                author,
                content,
                auditStatus,
                createdAt,
                createdBy,
                updatedAt,
                updatedBy,
                deleted,
                deletedAt,
                deletedBy);
    }

    private static void validateAudit(
            LocalDateTime updatedAt,
            boolean deleted,
            LocalDateTime deletedAt,
            Long deletedBy) {
        if (updatedAt == null) {
            throw new IllegalArgumentException("评论更新时间不能为空");
        }
        if (deleted) {
            if (deletedAt == null || deletedBy == null || deletedBy <= 0) {
                throw new IllegalArgumentException("已删除评论必须包含删除审计");
            }
        } else if (deletedAt != null || deletedBy != null) {
            throw new IllegalArgumentException("未删除评论不得包含删除审计");
        }
    }
}
