package com.tyb.myblog.v2.comment.domain;

import java.util.List;

/**
 * 后台评论审核命令。
 *
 * @param ids            评论 ID 列表
 * @param reviewed       目标审核状态
 * @param operatorUserId 当前后台操作人用户 ID
 */
public record AdminCommentModerationCommand(List<Integer> ids, boolean reviewed, int operatorUserId) {
}
