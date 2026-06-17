package com.tyb.myblog.v2.comment.application;

import com.tyb.myblog.v2.comment.domain.CommentTarget;

public interface DuplicateCommentGuard {

    void checkAndRecord(
            String clientIp,
            CommentTarget target,
            String contentMd);
}
