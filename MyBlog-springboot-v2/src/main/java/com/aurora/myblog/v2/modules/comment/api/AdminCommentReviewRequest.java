package com.aurora.myblog.v2.modules.comment.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdminCommentReviewRequest(
        @NotEmpty(message = "评论 ID 不能为空")
        List<Integer> ids,

        @NotNull(message = "审核状态不能为空")
        Boolean reviewed
) {
}
