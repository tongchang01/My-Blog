package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.domain.AdminCommentItem;

import java.time.LocalDateTime;

public record AdminCommentResponse(
        int id,
        int type,
        Integer topicId,
        String topicTitle,
        Integer parentId,
        Integer replyUserId,
        int userId,
        String nickname,
        String avatar,
        String content,
        boolean reviewed,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminCommentResponse from(AdminCommentItem item) {
        return new AdminCommentResponse(
                item.id(),
                item.type().code(),
                item.topicId(),
                item.topicTitle(),
                item.parentId(),
                item.replyUserId(),
                item.userId(),
                item.nickname(),
                item.avatar(),
                item.content(),
                item.reviewed(),
                item.deleted(),
                item.createdAt(),
                item.updatedAt());
    }
}
