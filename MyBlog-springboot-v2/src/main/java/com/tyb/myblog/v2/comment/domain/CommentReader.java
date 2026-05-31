package com.tyb.myblog.v2.comment.domain;

import com.tyb.myblog.v2.common.web.PageResponse;

import java.util.List;

/**
 * 前台评论读取端口。
 *
 * <p>实现层必须保证只返回已审核且未删除的评论，避免后台处理中评论泄露到前台。</p>
 */
public interface CommentReader {

    /**
     * 查询前台一级评论列表。
     */
    PageResponse<CommentThread> listComments(CommentType type, Integer topicId, CommentPageQuery query);

    /**
     * 查询指定一级评论下的回复。
     */
    List<CommentReply> listRepliesByCommentId(int commentId);

    /**
     * 查询首页热门评论。
     */
    List<CommentThread> listTopComments(int limit);
}
