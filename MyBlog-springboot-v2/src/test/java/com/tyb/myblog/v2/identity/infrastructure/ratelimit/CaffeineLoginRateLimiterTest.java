package com.tyb.myblog.v2.identity.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import com.tyb.myblog.v2.common.config.LoginRateLimitProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Caffeine 登录失败限流器测试。
 */
class CaffeineLoginRateLimiterTest {

    private static final String IP = "127.0.0.1";
    private static final String USERNAME = "admin";

    @Test
    void shouldBlockOnlyAfterFifthFailureIsRecorded() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);

        assertThat(limiter.isBlocked(IP, USERNAME)).isFalse();
        for (int attempt = 1; attempt <= 4; attempt++) {
            limiter.recordFailure(IP, USERNAME);
            assertThat(limiter.isBlocked(IP, USERNAME)).isFalse();
        }

        limiter.recordFailure(IP, USERNAME);

        assertThat(limiter.isBlocked(IP, USERNAME)).isTrue();
    }

    @Test
    void shouldResetAfterSuccessfulCredentials() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);
        recordFailures(limiter, 5);

        limiter.reset(IP, USERNAME);

        assertThat(limiter.isBlocked(IP, USERNAME)).isFalse();
    }

    @Test
    void shouldNormalizeIpAndUsernameIntoSameKey() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);

        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.recordFailure(" 2001:DB8::1 ", " Admin ");
        }

        assertThat(limiter.isBlocked("2001:db8::1", "admin")).isTrue();
    }

    @Test
    void shouldUseUnknownIpBucketAndKeepDifferentKeysIndependent() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);
        for (int attempt = 0; attempt < 5; attempt++) {
            limiter.recordFailure(null, "admin");
        }

        assertThat(limiter.isBlocked(" ", "ADMIN")).isTrue();
        assertThat(limiter.isBlocked("127.0.0.1", "admin")).isFalse();
        assertThat(limiter.isBlocked(null, "other")).isFalse();
    }

    @Test
    void shouldExpireTenMinutesAfterFifthFailureWithoutExtendingOnRead() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);
        recordFailures(limiter, 4);
        ticker.advance(Duration.ofMinutes(3));
        limiter.recordFailure(IP, USERNAME);

        ticker.advance(Duration.ofMinutes(9));
        assertThat(limiter.isBlocked(IP, USERNAME)).isTrue();

        ticker.advance(Duration.ofMinutes(1));
        assertThat(limiter.isBlocked(IP, USERNAME)).isFalse();
    }

    @Test
    void shouldKeepConcurrentFailureCountWithoutGrowingBeyondThreshold() throws Exception {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 10_000);
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<? extends Future<?>> futures = IntStream.range(0, threadCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        limiter.recordFailure(IP, USERNAME);
                        return null;
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(limiter.isBlocked(IP, USERNAME)).isTrue();
        assertThat(limiter.failureCountForTesting(IP, USERNAME)).isEqualTo(5);
    }

    @Test
    void shouldBoundCacheSize() {
        MutableTicker ticker = new MutableTicker();
        CaffeineLoginRateLimiter limiter = limiter(ticker, 2);

        limiter.recordFailure("127.0.0.1", "user-1");
        limiter.recordFailure("127.0.0.1", "user-2");
        limiter.recordFailure("127.0.0.1", "user-3");

        assertThat(limiter.estimatedSizeAfterCleanup()).isLessThanOrEqualTo(2);
    }

    private static CaffeineLoginRateLimiter limiter(Ticker ticker, long maximumSize) {
        return new CaffeineLoginRateLimiter(
                new LoginRateLimitProperties(
                        5,
                        Duration.ofMinutes(10),
                        maximumSize),
                ticker);
    }

    private static void recordFailures(CaffeineLoginRateLimiter limiter, int count) {
        for (int attempt = 0; attempt < count; attempt++) {
            limiter.recordFailure(IP, USERNAME);
        }
    }

    private static final class MutableTicker implements Ticker {

        private final AtomicLong nanos = new AtomicLong();

        @Override
        public long read() {
            return nanos.get();
        }

        void advance(Duration duration) {
            nanos.addAndGet(duration.toNanos());
        }
    }
}
