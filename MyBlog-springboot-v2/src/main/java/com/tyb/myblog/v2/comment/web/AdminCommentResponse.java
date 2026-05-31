package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.domain.AdminCommentItem;

import java.time.LocalDateTime;

/**
 * 后台评论列表响应。
 *
 * @param id          评论 ID
 * @param type        评论类型编码
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
    /**
     * 从后台评论列表项转换为响应。
     */
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
