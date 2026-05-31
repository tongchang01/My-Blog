package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;

/**
 * 后台评论详情。
 *
 * @param id            评论 ID
 * @param type          评论类型
 * @param topicId       主题 ID，文章评论时为文章 ID
 * @param topicTitle    主题标题
 * @param parentId      父评论 ID，空表示一级评论
 * @param replyUserId   被回复用户 ID
 * @param replyNickname 被回复用户昵称
 * @param userId        评论人用户 ID
 * @param nickname      评论人昵称
 * @param avatar        评论人头像
 * @param content       评论内容
 * @param reviewed      是否已审核，对应旧库 {@code is_review}
 * @param deleted       是否已软删除，对应旧库 {@code is_delete}
 * @param createIp      提交 IP
 * @param userAgent     提交浏览器 User-Agent
 * @param reviewedBy    审核人 ID
 * @param reviewTime    审核时间
 * @param deletedBy     删除人 ID
 * @param deleteTime    删除时间
 * @param restoredBy    恢复人 ID
 * @param restoreTime   恢复时间
 * @param createdAt     创建时间
 * @param updatedAt     更新时间
 */
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
