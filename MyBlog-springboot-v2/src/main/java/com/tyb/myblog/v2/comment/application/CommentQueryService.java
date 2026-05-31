package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.comment.domain.CommentPageQuery;
import com.tyb.myblog.v2.comment.domain.CommentReader;
import com.tyb.myblog.v2.comment.domain.CommentReply;
import com.tyb.myblog.v2.comment.domain.CommentThread;
import com.tyb.myblog.v2.comment.domain.CommentType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommentQueryService {

    private final CommentReader commentReader;

    public CommentQueryService(CommentReader commentReader) {
        this.commentReader = commentReader;
    }

    public PageResponse<CommentThread> listComments(Integer type, Integer topicId, Integer page, Integer size) {
        CommentType commentType = CommentType.fromCode(type);
        return commentReader.listComments(commentType, topicId, CommentPageQuery.of(page, size));
    }

    public List<CommentReply> listRepliesByCommentId(int commentId) {
        return commentReader.listRepliesByCommentId(commentId);
    }

    public List<CommentThread> listTopComments() {
        return commentReader.listTopComments(6);
    }
}
