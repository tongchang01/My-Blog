package com.tyb.myblog.v2.common.security.jwt;

import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 密钥启动校验测试。
 */
class JwtSecretStartupValidatorTest {

    @Test
    void rejectsBlankSecret() {
        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(properties("   "));

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT 密钥");
    }

    @Test
    void rejectsDefaultDevelopmentSecret() {
        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(
                properties("change-me-change-me-change-me-change-me"));

        assertThatThrownBy(validator::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("默认");
    }

    @Test
    void acceptsExplicitTestSecret() {
        JwtSecretStartupValidator validator = new JwtSecretStartupValidator(
                properties("test-secret-test-secret-test-secret-123456"));

        assertThatCode(validator::afterPropertiesSet)
                .doesNotThrowAnyException();
    }

    private SecurityJwtProperties properties(String secret) {
        return new SecurityJwtProperties("myblog-v2-test", secret, Duration.ofMinutes(15));
    }
}
