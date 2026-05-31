package com.tyb.myblog.v2.comment.domain;

import com.tyb.myblog.v2.common.web.PageResponse;

import java.util.Optional;

/**
 * 后台评论读取端口。
 */
public interface AdminCommentReader {

    /**
     * 查询后台评论分页列表。
     */
    PageResponse<AdminCommentItem> list(AdminCommentQuery query);

    /**
     * 查询后台评论详情。
     */
    Optional<AdminCommentDetail> findDetail(int id);
}
