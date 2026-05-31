package com.tyb.myblog.v2.comment.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 前台评论提交请求。
 *
 * @param type        评论类型
 * @param topicId     主题 ID，文章评论时为文章 ID
 * @param parentId    父评论 ID，空表示一级评论
 * @param replyUserId 被回复用户 ID
 * @param content     评论内容
 */
public record CommentCreateRequest(
        @NotNull(message = "评论类型不能为空")
        Integer type,
        Integer topicId,
        Integer parentId,
        Integer replyUserId,
        @NotBlank(message = "评论内容不能为空")
        String content
) {
}
