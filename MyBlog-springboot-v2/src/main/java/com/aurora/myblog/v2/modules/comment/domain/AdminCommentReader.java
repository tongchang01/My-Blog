package com.aurora.myblog.v2.modules.comment.domain;

import com.aurora.myblog.v2.common.web.PageResponse;

import java.util.Optional;

public interface AdminCommentReader {

    PageResponse<AdminCommentItem> list(AdminCommentQuery query);

    Optional<AdminCommentDetail> findDetail(int id);
}
