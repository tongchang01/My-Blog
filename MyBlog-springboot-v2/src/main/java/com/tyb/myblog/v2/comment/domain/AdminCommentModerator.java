package com.tyb.myblog.v2.comment.domain;

/**
 * 后台评论审核与状态变更端口。
 */
public interface AdminCommentModerator {

    /**
     * 审核或取消审核评论。
     */
    int review(AdminCommentModerationCommand command);

    /**
     * 软删除评论。
     */
    int delete(AdminCommentDeletionCommand command);

    /**
     * 恢复已软删除评论。
     */
    int restore(AdminCommentRestoreCommand command);
}
