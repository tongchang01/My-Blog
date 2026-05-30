package com.aurora.myblog.v2.modules.comment.domain;

public interface AdminCommentModerator {

    int review(AdminCommentModerationCommand command);

    int delete(AdminCommentDeletionCommand command);

    int restore(AdminCommentRestoreCommand command);
}
