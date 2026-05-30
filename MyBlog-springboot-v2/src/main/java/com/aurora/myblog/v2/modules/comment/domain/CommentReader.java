package com.aurora.myblog.v2.modules.comment.domain;

import com.aurora.myblog.v2.common.web.PageResponse;

import java.util.List;

public interface CommentReader {

    PageResponse<CommentThread> listComments(CommentType type, Integer topicId, CommentPageQuery query);

    List<CommentReply> listRepliesByCommentId(int commentId);

    List<CommentThread> listTopComments(int limit);
}
