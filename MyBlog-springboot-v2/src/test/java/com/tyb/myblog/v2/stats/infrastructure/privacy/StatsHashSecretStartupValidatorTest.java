package com.tyb.myblog.v2.stats.infrastructure.privacy;

import com.tyb.myblog.v2.stats.infrastructure.config.StatsProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatsHashSecretStartupValidatorTest {

    @Test
    void rejectsBlankAndShortSecrets() {
        assertThatThrownBy(() -> validator(" ").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("统计访客标识密钥");
        assertThatThrownBy(() -> validator("short").afterPropertiesSet())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 字节");
    }

    @Test
    void acceptsExplicitLongSecret() {
        assertThatCode(() -> validator(
                "test-stats-secret-test-stats-secret-123456")
                .afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    private StatsHashSecretStartupValidator validator(String secret) {
        return new StatsHashSecretStartupValidator(
                new StatsProperties(
                        secret,
                        120,
                        20_000,
                        new StatsProperties.Scheduling(
                                false,
                                "0 */5 * * * *",
                                "0 30 3 * * *")));
    }
}
