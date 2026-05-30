package com.aurora.myblog.v2.modules.comment.api;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminCommentDeleteRequest(
        @NotEmpty(message = "评论 ID 不能为空")
        List<Integer> ids
) {
}
