package com.tyb.myblog.v2.comment.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 后台评论审核请求。
 *
 * @param ids      评论 ID 列表
 * @param reviewed 目标审核状态
 */
public record AdminCommentReviewRequest(
        @NotEmpty(message = "评论 ID 不能为空")
        List<Integer> ids,

        @NotNull(message = "审核状态不能为空")
        Boolean reviewed
) {
}
