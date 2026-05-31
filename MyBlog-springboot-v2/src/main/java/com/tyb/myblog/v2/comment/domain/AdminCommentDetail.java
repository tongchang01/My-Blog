package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;

public record AdminCommentDetail(
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
}
