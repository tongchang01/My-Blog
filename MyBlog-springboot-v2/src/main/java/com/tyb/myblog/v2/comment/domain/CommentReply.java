package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;

/**
 * 评论回复。
 *
 * @param id        回复 ID
 * @param parentId  父评论 ID
 * @param author    回复作者
 * @param replyUser 被回复用户
 * @param content   回复内容
 * @param createdAt 创建时间
 */
public record CommentReply(
        int id,
        int parentId,
        CommentAuthor author,
        CommentAuthor replyUser,
        String content,
        LocalDateTime createdAt
) {
}
