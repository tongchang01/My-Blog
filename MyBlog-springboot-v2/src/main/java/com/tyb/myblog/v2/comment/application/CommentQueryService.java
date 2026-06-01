package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.common.web.PageResponse;
import com.tyb.myblog.v2.comment.domain.CommentPageQuery;
import com.tyb.myblog.v2.comment.domain.CommentReader;
import com.tyb.myblog.v2.comment.domain.CommentReply;
import com.tyb.myblog.v2.comment.domain.CommentThread;
import com.tyb.myblog.v2.comment.domain.CommentType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 前台评论查询应用服务。
 *
 * <p>负责前台评论列表、回复列表和首页热门评论查询。
 * 前台只应展示已审核且未删除的评论。</p>
 */
@Service
public class CommentQueryService {

    private final CommentReader commentReader;

    public CommentQueryService(CommentReader commentReader) {
        this.commentReader = commentReader;
    }

    /**
     * 查询指定类型和主题下的前台评论列表。
     */
    public PageResponse<CommentThread> listComments(Integer type, Integer topicId, Integer page, Integer size) {
        CommentType commentType = CommentType.fromCode(type);
        return commentReader.listComments(commentType, topicId, CommentPageQuery.of(page, size));
    }

    /**
     * 查询指定一级评论下的回复列表。
     */
    public List<CommentReply> listRepliesByCommentId(int commentId) {
        return commentReader.listRepliesByCommentId(commentId);
    }

    /**
     * 查询首页展示的热门评论。
     */
    public List<CommentThread> listTopComments() {
        return commentReader.listTopComments(6);
    }
}
