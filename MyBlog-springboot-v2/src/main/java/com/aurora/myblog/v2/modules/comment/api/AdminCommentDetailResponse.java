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
                detail.createdAt(),
                detail.updatedAt());
    }
}
