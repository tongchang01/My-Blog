package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.AdminCommentDeletionCommand;
import com.tyb.myblog.v2.comment.domain.AdminCommentModerationCommand;
import com.tyb.myblog.v2.comment.domain.AdminCommentModerator;
import com.tyb.myblog.v2.comment.domain.AdminCommentRestoreCommand;
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
