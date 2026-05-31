package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;

/**
 * 后台评论列表项。
 *
 * @param id          评论 ID
 * @param type        评论类型
 * @param topicId     主题 ID
 * @param topicTitle  主题标题
 * @param parentId    父评论 ID
 * @param replyUserId 被回复用户 ID
 * @param userId      评论人用户 ID
 * @param nickname    评论人昵称
 * @param avatar      评论人头像
 * @param content     评论内容
 * @param reviewed    是否已审核
 * @param deleted     是否已软删除
 * @param createdAt   创建时间
 * @param updatedAt   更新时间
 */
public record AdminCommentItem(
        int id,
        CommentType type,
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
}
