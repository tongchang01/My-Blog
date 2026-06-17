package com.tyb.myblog.v2.comment.domain;

public interface AdminCommentQueryRepository {

    AdminCommentPage page(AdminCommentQueryCriteria criteria);
}
