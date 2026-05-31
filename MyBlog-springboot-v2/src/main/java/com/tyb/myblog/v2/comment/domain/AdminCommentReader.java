package com.tyb.myblog.v2.comment.domain;

import com.tyb.myblog.v2.common.web.PageResponse;

import java.util.Optional;

public interface AdminCommentReader {

    PageResponse<AdminCommentItem> list(AdminCommentQuery query);

    Optional<AdminCommentDetail> findDetail(int id);
}
