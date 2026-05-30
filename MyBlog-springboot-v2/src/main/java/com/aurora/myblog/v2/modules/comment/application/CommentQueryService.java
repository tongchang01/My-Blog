package com.aurora.myblog.v2.modules.comment.application;

import com.aurora.myblog.v2.common.web.PageResponse;
import com.aurora.myblog.v2.modules.comment.domain.CommentPageQuery;
import com.aurora.myblog.v2.modules.comment.domain.CommentReader;
import com.aurora.myblog.v2.modules.comment.domain.CommentReply;
import com.aurora.myblog.v2.modules.comment.domain.CommentThread;
import com.aurora.myblog.v2.modules.comment.domain.CommentType;
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
