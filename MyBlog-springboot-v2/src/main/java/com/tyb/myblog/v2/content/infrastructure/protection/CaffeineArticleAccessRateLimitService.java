package com.tyb.myblog.v2.content.infrastructure.protection;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.article.ArticleAccessRateLimitService;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CaffeineArticleAccessRateLimitService implements ArticleAccessRateLimitService {

    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private final Cache<String, Integer> requests = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(20_000)
            .build();

    @Override
    public void checkAndRecord(String clientIp, long articleId) {
        String key = (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim())
                + ':' + articleId;
        Integer count = requests.asMap().compute(key, (ignored, current) ->
                Math.min(current == null ? 1 : current + 1, MAX_REQUESTS_PER_WINDOW + 1));
        if (count != null && count > MAX_REQUESTS_PER_WINDOW) {
            throw new ApiException(ApiErrorCode.RATE_LIMITED, "文章密码尝试过于频繁，请稍后再试");
        }
    }
}
