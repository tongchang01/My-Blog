package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.AdminCommentDeletionCommand;
import com.tyb.myblog.v2.comment.domain.AdminCommentModerationCommand;
import com.tyb.myblog.v2.comment.domain.AdminCommentModerator;
import com.tyb.myblog.v2.comment.domain.AdminCommentRestoreCommand;
import org.springframework.stereotype.Service;

@Service
/**
 * 后台评论命令应用服务。
 *
 * <p>编排评论审核、软删除和恢复操作，具体旧库字段更新由 {@link AdminCommentModerator} 实现。</p>
 */
public class AdminCommentCommandService {

    private final AdminCommentModerator moderator;

    public AdminCommentCommandService(AdminCommentModerator moderator) {
        this.moderator = moderator;
    }

    /**
     * 审核或取消审核评论。
     */
    public Result review(AdminCommentModerationCommand command) {
        return new Result(moderator.review(command));
    }

    /**
     * 软删除评论。
     */
    public Result delete(AdminCommentDeletionCommand command) {
        return new Result(moderator.delete(command));
    }

    /**
     * 恢复已软删除评论。
     */
    public Result restore(AdminCommentRestoreCommand command) {
        return new Result(moderator.restore(command));
    }

    /**
     * 后台批量操作结果。
     *
     * @param affected 受影响评论数量
     */
    public record Result(int affected) {
    }
}
