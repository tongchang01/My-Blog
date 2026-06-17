package com.tyb.myblog.v2.comment.infrastructure.protection;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tyb.myblog.v2.comment.application.CommentRateLimitService;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CaffeineCommentRateLimitService implements CommentRateLimitService {

    private static final int MAX_REQUESTS_PER_WINDOW = 5;

    private final Cache<String, Integer> requests = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(20_000)
            .build();

    @Override
    public void checkAndRecord(String clientIp) {
        String key = normalizeIp(clientIp);
        Integer count = requests.asMap().compute(key, (ignored, current) -> {
            int next = current == null ? 1 : current + 1;
            return Math.min(next, MAX_REQUESTS_PER_WINDOW + 1);
        });
        if (count != null && count > MAX_REQUESTS_PER_WINDOW) {
            throw new ApiException(ApiErrorCode.RATE_LIMITED, "评论提交过于频繁，请稍后再试");
        }
    }

    private static String normalizeIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        return clientIp.trim();
    }
}
