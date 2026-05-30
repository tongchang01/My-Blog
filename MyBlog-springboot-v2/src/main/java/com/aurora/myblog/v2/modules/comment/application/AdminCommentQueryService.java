package com.aurora.myblog.v2.modules.comment.application;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentDetail;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentItem;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentQuery;
import com.aurora.myblog.v2.modules.comment.domain.AdminCommentReader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCommentQueryService {

    private final AdminCommentReader reader;

    public AdminCommentQueryService(AdminCommentReader reader) {
        this.reader = reader;
    }

    public PageResponse<AdminCommentItem> list(AdminCommentQuery query) {
        return reader.list(query);
    }

    public AdminCommentDetail detail(int id) {
        return reader.findDetail(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "comment not found"));
    }
}
