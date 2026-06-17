package com.tyb.myblog.v2.comment.application;

public interface CommentRateLimitService {

    void checkAndRecord(String clientIp);
}
