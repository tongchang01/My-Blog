package com.tyb.myblog.v2.stats.application;

/**
 * 公开页面访问打点命令。
 */
public record PageViewRecordCommand(
        Long articleId,
        String lang,
        String clientIp,
        String userAgent,
        String referrer) {
}
