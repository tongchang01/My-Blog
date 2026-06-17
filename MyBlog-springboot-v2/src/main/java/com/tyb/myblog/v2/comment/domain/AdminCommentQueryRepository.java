package com.tyb.myblog.v2.comment.domain;

import com.tyb.myblog.v2.comment.application.AdminCommentPageQuery;

public interface AdminCommentQueryRepository {

    AdminCommentPage page(AdminCommentPageQuery query);
}
