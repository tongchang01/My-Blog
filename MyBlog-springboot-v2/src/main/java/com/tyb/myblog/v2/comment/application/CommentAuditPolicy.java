package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentAuditStatus;

/**
 * 评论内容审核策略。
 */
public interface CommentAuditPolicy {

    CommentAuditStatus audit(String contentMarkdown);
}
