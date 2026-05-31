package com.aurora.myblog.v2.modules.comment.api;

import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDetail;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;

import java.time.LocalDateTime;

public record AdminCommentDetailResponse(
        int id,
        CommentType type,
        Integer topicId,
        String topicTitle,
        Integer parentId,
        Integer replyUserId,
        String replyNickname,
        int userId,
        String nickname,
        String avatar,
        String content,
        boolean reviewed,
        boolean deleted,
        String createIp,
        String userAgent,
        Integer reviewedBy,
        LocalDateTime reviewTime,
        Integer deletedBy,
        LocalDateTime deleteTime,
        Integer restoredBy,
        LocalDateTime restoreTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    static AdminCommentDetailResponse from(AdminCommentDetail detail) {
        return new AdminCommentDetailResponse(
                detail.id(),
                detail.type(),
                detail.topicId(),
                detail.topicTitle(),
                detail.parentId(),
                detail.replyUserId(),
                detail.replyNickname(),
                detail.userId(),
                detail.nickname(),
                detail.avatar(),
                detail.content(),
                detail.reviewed(),
                detail.deleted(),
                detail.createIp(),
                detail.userAgent(),
                detail.reviewedBy(),
                detail.reviewTime(),
                detail.deletedBy(),
                detail.deleteTime(),
                detail.restoredBy(),
                detail.restoreTime(),
                detail.createdAt(),
                detail.updatedAt());
    }
}
