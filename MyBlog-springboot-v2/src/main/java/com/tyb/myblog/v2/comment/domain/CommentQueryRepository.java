package com.tyb.myblog.v2.comment.domain;

public interface CommentQueryRepository {

    CommentPage page(CommentTarget target, int page, int size);
}
