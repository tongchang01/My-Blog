package com.aurora.myblog.v2.modules.comment.application;

import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDeletionCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerationCommand;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentModerator;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentRestoreCommand;
import org.springframework.stereotype.Service;

@Service
public class AdminCommentCommandService {

    private final AdminCommentModerator moderator;

    public AdminCommentCommandService(AdminCommentModerator moderator) {
        this.moderator = moderator;
    }

    public Result review(AdminCommentModerationCommand command) {
        return new Result(moderator.review(command));
    }

    public Result delete(AdminCommentDeletionCommand command) {
        return new Result(moderator.delete(command));
    }

    public Result restore(AdminCommentRestoreCommand command) {
        return new Result(moderator.restore(command));
    }

    public record Result(int affected) {
    }
}
