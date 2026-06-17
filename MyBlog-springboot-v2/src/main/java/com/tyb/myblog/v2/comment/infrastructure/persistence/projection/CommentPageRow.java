package com.tyb.myblog.v2.comment.infrastructure.persistence.projection;

import java.time.LocalDateTime;

public record CommentPageRow(
        long id,
        Long parentId,
        Long replyToCommentId,
        String replyToNickname,
        String authorNickname,
        String authorSite,
        String contentHtml,
        LocalDateTime createdAt) {
}
