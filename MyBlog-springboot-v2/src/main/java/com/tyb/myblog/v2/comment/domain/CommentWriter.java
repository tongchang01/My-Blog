package com.tyb.myblog.v2.comment.domain;

/**
 * 评论写入端口。
 */
public interface CommentWriter {

    /**
     * 保存评论并返回新评论 ID。
     */
    int save(CommentCreateCommand command);
}
