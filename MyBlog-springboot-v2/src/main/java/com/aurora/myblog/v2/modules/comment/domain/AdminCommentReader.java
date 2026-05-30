package com.aurora.myblog.v2.modules.comment.domain;

import com.aurora.myblog.v2.common.web.PageResponse;

public interface AdminCommentReader {

    PageResponse<AdminCommentItem> list(AdminCommentQuery query);
}
