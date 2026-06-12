package com.tyb.myblog.v2.identity.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.tyb.myblog.v2.common.config.LoginRateLimitProperties;
import com.tyb.myblog.v2.identity.domain.auth.LoginRateLimiter;

/**
 * 基于单实例 Caffeine 缓存的登录连续失败限流器。
 */
public class CaffeineLoginRateLimiter implements LoginRateLimiter {

    private final Cache<LoginRateLimitKey, Integer> failures;
    private final int maxFailures;

    public CaffeineLoginRateLimiter(
            LoginRateLimitProperties properties,
            Ticker ticker
    ) {
        this.maxFailures = properties.loginMaxFailures();
        this.failures = Caffeine.newBuilder()
                .expireAfterWrite(properties.loginCooldown())
                .maximumSize(properties.loginMaximumSize())
                .ticker(ticker)
                .build();
    }

    @Override
    public boolean isBlocked(String clientIp, String normalizedUsername) {
        Integer failureCount = failures.getIfPresent(
                new LoginRateLimitKey(clientIp, normalizedUsername));
        return failureCount != null && failureCount >= maxFailures;
    }

    @Override
    public void recordFailure(String clientIp, String normalizedUsername) {
        LoginRateLimitKey key = new LoginRateLimitKey(clientIp, normalizedUsername);
        failures.asMap().compute(key, (ignored, current) -> {
            int next = current == null ? 1 : current + 1;
            // 达到阈值后保持固定值，避免攻击流量导致计数无界增长。
            return Math.min(next, maxFailures);
        });
    }

    @Override
    public void reset(String clientIp, String normalizedUsername) {
        failures.invalidate(new LoginRateLimitKey(clientIp, normalizedUsername));
    }

    /**
     * 读取当前失败次数，仅供同包并发测试验证原子累计和阈值封顶。
     */
    int failureCountForTesting(String clientIp, String normalizedUsername) {
        Integer count = failures.getIfPresent(
                new LoginRateLimitKey(clientIp, normalizedUsername));
        return count == null ? 0 : count;
    }

    /**
     * 返回维护后的缓存估算大小，仅供同包测试验证容量边界。
     */
    long estimatedSizeAfterCleanup() {
        failures.cleanUp();
        return failures.estimatedSize();
    }
}
