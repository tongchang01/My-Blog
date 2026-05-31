package com.tyb.myblog.v2.comment.domain;

public interface AdminCommentModerator {

    int review(AdminCommentModerationCommand command);

    int delete(AdminCommentDeletionCommand command);

    int restore(AdminCommentRestoreCommand command);
}
