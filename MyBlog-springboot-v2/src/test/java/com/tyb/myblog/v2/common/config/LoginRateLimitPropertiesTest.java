package com.tyb.myblog.v2.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 登录限流配置测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class LoginRateLimitPropertiesTest {

    @Autowired
    private LoginRateLimitProperties properties;

    @Test
    void shouldBindLoginRateLimitSettings() {
        assertThat(properties.loginMaxFailures()).isEqualTo(5);
        assertThat(properties.loginCooldown()).isEqualTo(Duration.ofMinutes(10));
        assertThat(properties.loginMaximumSize()).isEqualTo(10_000);
    }

    @Test
    void shouldRejectInvalidSettings() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginRateLimitProperties(
                        0,
                        Duration.ofMinutes(10),
                        10_000));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginRateLimitProperties(
                        5,
                        Duration.ZERO,
                        10_000));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LoginRateLimitProperties(
                        5,
                        Duration.ofMinutes(10),
                        0));
    }
}
