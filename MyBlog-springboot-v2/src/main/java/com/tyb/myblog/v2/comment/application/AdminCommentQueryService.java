package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.comment.domain.AdminCommentDetail;
import com.tyb.myblog.v2.comment.domain.AdminCommentItem;
import com.tyb.myblog.v2.comment.domain.AdminCommentQuery;
import com.tyb.myblog.v2.comment.domain.AdminCommentReader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
/**
 * 后台评论查询应用服务。
 *
 * <p>负责后台评论列表和详情查询，支持审核状态、删除状态、类型、主题和关键词筛选。</p>
 */
public class AdminCommentQueryService {

    private final AdminCommentReader reader;

    public AdminCommentQueryService(AdminCommentReader reader) {
        this.reader = reader;
    }

    /**
     * 查询后台评论分页列表。
     */
    public PageResponse<AdminCommentItem> list(AdminCommentQuery query) {
        return reader.list(query);
    }

    /**
     * 查询后台评论详情。
     */
    public AdminCommentDetail detail(int id) {
        return reader.findDetail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));
    }
}
