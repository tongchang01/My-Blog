package com.tyb.myblog.v2.comment.web;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 后台评论恢复请求。
 *
 * @param ids 评论 ID 列表，单次最多 100 条
 */
public record AdminCommentRestoreRequest(
        @NotEmpty
        @Size(max = 100)
        List<Integer> ids
) {
}
