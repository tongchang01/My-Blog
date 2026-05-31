package com.tyb.myblog.v2.comment.domain;

import java.util.List;

/**
 * 后台评论恢复命令。
 *
 * @param ids            要恢复的评论 ID 列表
 * @param operatorUserId 当前后台操作人用户 ID
 */
public record AdminCommentRestoreCommand(List<Integer> ids, int operatorUserId) {
}
