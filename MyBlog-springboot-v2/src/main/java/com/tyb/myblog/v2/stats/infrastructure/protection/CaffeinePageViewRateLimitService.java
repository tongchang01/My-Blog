package com.tyb.myblog.v2.stats.infrastructure.protection;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.stats.application.PageViewRateLimitService;
import com.tyb.myblog.v2.stats.infrastructure.config.StatsProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 单实例公开访问打点限流器。
 */
@Service
public class CaffeinePageViewRateLimitService
        implements PageViewRateLimitService {

    private final int maximumRequests;
    private final Cache<String, Integer> requests;

    public CaffeinePageViewRateLimitService(
            StatsProperties properties) {
        this.maximumRequests =
                properties.pageViewMaxRequestsPerMinute();
        this.requests = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(properties.rateLimitMaximumSize())
                .build();
    }

    @Override
    public void checkAndRecord(String clientIp) {
        String key = normalize(clientIp);
        Integer count = requests.asMap().compute(
                key,
                (ignored, current) -> {
                    int next = current == null ? 1 : current + 1;
                    return Math.min(next, maximumRequests + 1);
                });
        if (count != null && count > maximumRequests) {
            throw new ApiException(
                    ApiErrorCode.RATE_LIMITED,
                    "页面访问打点过于频繁，请稍后再试");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim();
    }
}
