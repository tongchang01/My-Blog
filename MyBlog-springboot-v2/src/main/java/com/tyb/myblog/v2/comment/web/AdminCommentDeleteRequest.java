package com.tyb.myblog.v2.comment.web;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminCommentDeleteRequest(
        @NotEmpty(message = "评论 ID 不能为空")
        List<Integer> ids
) {
}
