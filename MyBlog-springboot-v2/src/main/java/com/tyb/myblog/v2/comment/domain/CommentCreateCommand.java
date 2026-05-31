package com.tyb.myblog.v2.comment.domain;

/**
 * 评论创建命令。
 *
 * @param userId      评论人用户 ID
 * @param type        评论类型
 * @param topicId     主题 ID，文章评论时为文章 ID
 * @param parentId    父评论 ID，空表示一级评论
 * @param replyUserId 被回复用户 ID
 * @param content     评论内容
 * @param clientIp    客户端 IP，用于审计
 * @param userAgent   客户端 User-Agent，用于审计
 */
public record CommentCreateCommand(
        int userId,
        CommentType type,
        Integer topicId,
        Integer parentId,
        Integer replyUserId,
        String content,
        String clientIp,
        String userAgent
) {
}
