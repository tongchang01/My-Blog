package com.aurora.myblog.v2.common.security;

import com.aurora.myblog.v2.common.config.SecurityJwtProperties;
import com.aurora.myblog.v2.common.security.auth.JwtTokenService;
import com.aurora.myblog.v2.common.security.support.InMemoryTokenRevocationStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private final JwtTokenService tokenService = new JwtTokenService(
            new SecurityJwtProperties("myblog-v2-test", "test-secret-test-secret-test-secret-123456", Duration.ofMinutes(15)),
            new InMemoryTokenRevocationStore());

    @Test
    void issuesAndParsesAccessToken() {
        var token = tokenService.issueAccessToken("user-1", "admin@example.com", List.of("ADMIN"));
        var claims = tokenService.parse(token.accessToken()).orElseThrow();

        assertThat(claims.userId()).isEqualTo("user-1");
        assertThat(claims.username()).isEqualTo("admin@example.com");
        assertThat(claims.roles()).containsExactly("ADMIN");
    }

    @Test
    void revokedTokenCannotBeParsed() {
        var token = tokenService.issueAccessToken("user-1", "admin@example.com", List.of("ADMIN"));
        tokenService.revoke(token.accessToken());

        assertThat(tokenService.parse(token.accessToken())).isEmpty();
    }
}
