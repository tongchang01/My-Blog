package com.tyb.myblog.v2.stats.application;

/**
 * 公开访问打点限流端口。
 */
public interface PageViewRateLimitService {

    void checkAndRecord(String clientIp);
}
