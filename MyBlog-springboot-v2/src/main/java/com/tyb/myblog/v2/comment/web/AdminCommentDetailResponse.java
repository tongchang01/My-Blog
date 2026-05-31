package com.tyb.myblog.v2.comment.web;

import com.tyb.myblog.v2.comment.domain.AdminCommentDetail;
import com.tyb.myblog.v2.comment.domain.CommentType;

import java.time.LocalDateTime;

/**
 * 后台评论详情响应。
 *
 * @param id            评论 ID
 * @param type          评论类型
 * @param topicId       主题 ID
 * @param topicTitle    主题标题
 * @param parentId      父评论 ID
 * @param replyUserId   被回复用户 ID
 * @param replyNickname 被回复用户昵称
 * @param userId        评论人用户 ID
 * @param nickname      评论人昵称
 * @param avatar        评论人头像
 * @param content       评论内容
 * @param reviewed      是否已审核
 * @param deleted       是否已软删除
 * @param createIp      提交 IP
 * @param userAgent     提交 User-Agent
 * @param reviewedBy    审核人 ID
 * @param reviewTime    审核时间
 * @param deletedBy     删除人 ID
 * @param deleteTime    删除时间
 * @param restoredBy    恢复人 ID
 * @param restoreTime   恢复时间
 * @param createdAt     创建时间
 * @param updatedAt     更新时间
 */
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

    /**
     * 从后台评论详情领域对象转换为响应。
     */
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
