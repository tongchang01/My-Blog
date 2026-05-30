package com.aurora.myblog.v2.modules.comment.domain;

public interface CommentWriter {

    int save(CommentCreateCommand command);
}
