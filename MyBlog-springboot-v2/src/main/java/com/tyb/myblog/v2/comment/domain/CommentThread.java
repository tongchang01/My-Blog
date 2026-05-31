package com.tyb.myblog.v2.comment.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 前台评论线程。
 *
 * @param id        一级评论 ID
 * @param type      评论类型
 * @param topicId   主题 ID
 * @param author    评论作者
 * @param content   评论内容
 * @param createdAt 创建时间
 * @param replies   回复列表
 */
public record CommentThread(
        int id,
        CommentType type,
        Integer topicId,
        CommentAuthor author,
        String content,
        LocalDateTime createdAt,
        List<CommentReply> replies
) {
    /**
     * 复制回复列表，避免响应构造后被外部修改。
     */
    public CommentThread {
        replies = replies == null ? List.of() : List.copyOf(replies);
    }
}
