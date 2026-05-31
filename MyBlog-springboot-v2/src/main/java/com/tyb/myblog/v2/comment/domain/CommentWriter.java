package com.tyb.myblog.v2.comment.domain;

public interface CommentWriter {

    int save(CommentCreateCommand command);
}
