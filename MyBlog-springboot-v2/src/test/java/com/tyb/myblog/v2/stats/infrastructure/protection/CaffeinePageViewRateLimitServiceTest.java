package com.tyb.myblog.v2.stats.infrastructure.protection;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.stats.infrastructure.config.StatsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaffeinePageViewRateLimitServiceTest {

    @Test
    void allowsOneHundredTwentyRequestsAndRejectsNext() {
        CaffeinePageViewRateLimitService service = service();

        for (int index = 0; index < 120; index++) {
            assertThatCode(() -> service.checkAndRecord("203.0.113.1"))
                    .doesNotThrowAnyException();
        }

        assertThatThrownBy(() -> service.checkAndRecord("203.0.113.1"))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo(ApiErrorCode.RATE_LIMITED);
        assertThatCode(() -> service.checkAndRecord("203.0.113.2"))
                .doesNotThrowAnyException();
    }

    @Test
    void normalizesMissingClientAddress() {
        CaffeinePageViewRateLimitService service = service();

        for (int index = 0; index < 120; index++) {
            service.checkAndRecord(null);
        }

        assertThatThrownBy(() -> service.checkAndRecord(" "))
                .isInstanceOf(ApiException.class);
    }

    private CaffeinePageViewRateLimitService service() {
        return new CaffeinePageViewRateLimitService(
                new StatsProperties(
                        "test-stats-secret-test-stats-secret-123456",
                        120,
                        20_000,
                        new StatsProperties.Scheduling(
                                false,
                                "0 */5 * * * *",
                                "0 30 3 * * *")));
    }
}
