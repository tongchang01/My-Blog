package com.tyb.myblog.v2.comment.domain;

import java.util.List;

/**
 * 后台评论软删除命令。
 *
 * @param ids            要删除的评论 ID 列表
 * @param operatorUserId 当前后台操作人用户 ID
 */
public record AdminCommentDeletionCommand(List<Integer> ids, int operatorUserId) {
}
