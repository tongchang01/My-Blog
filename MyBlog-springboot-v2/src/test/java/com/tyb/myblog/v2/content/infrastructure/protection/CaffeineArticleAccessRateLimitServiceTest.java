package com.tyb.myblog.v2.content.infrastructure.protection;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaffeineArticleAccessRateLimitServiceTest {

    @Test
    void allowsFiveAttemptsPerIpAndArticleThenRejectsTheSixth() {
        CaffeineArticleAccessRateLimitService service =
                new CaffeineArticleAccessRateLimitService();

        for (int attempt = 0; attempt < 5; attempt++) {
            service.checkAndRecord("127.0.0.1", 100L);
        }

        assertThatThrownBy(() -> service.checkAndRecord("127.0.0.1", 100L))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.RATE_LIMITED));

        service.checkAndRecord("127.0.0.1", 101L);
        service.checkAndRecord("127.0.0.2", 100L);
    }
}
