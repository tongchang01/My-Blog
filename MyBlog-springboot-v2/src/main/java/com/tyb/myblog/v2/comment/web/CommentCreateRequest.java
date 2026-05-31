package com.tyb.myblog.v2.comment.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
