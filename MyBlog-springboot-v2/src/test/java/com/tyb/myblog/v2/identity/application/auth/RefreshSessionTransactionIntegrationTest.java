package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.auth.token.TokenPair;
import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * refresh token 轮换事务的数据库集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@Import(RefreshSessionTransactionIntegrationTest.TestIssuerConfiguration.class)
class RefreshSessionTransactionIntegrationTest {

    private static final long USER_ID = 1001L;

    @Autowired
    private RefreshSessionTransactionService service;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @BeforeEach
    void prepareAccount() {
        reset(accessTokenIssuer);
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        jdbcTemplate.update("""
                INSERT INTO t_user_auth (
                    id, username, password_hash, type, token_version, deleted
                ) VALUES (?, 'admin', '$2a$10$test-password-hash', 1, 3, 0)
                """, USER_ID);
    }

    @Test
    void shouldCommitOldRevocationAndNewRefreshToken() {
        stubSuccessfulAccessToken();
        IssuedRefreshToken oldToken = refreshTokenService.issue(USER_ID);

        LoginTokenResult result =
                service.refresh(oldToken.token()).orElseThrow();

        assertThat(result.refreshToken()).isNotEqualTo(oldToken.token());
        assertThat(activeTokenCount()).isEqualTo(1);
        assertThat(revokedTokenCount()).isEqualTo(1);
    }

    @Test
    void shouldRollbackRotationWhenAccessTokenSigningFails() {
        IssuedRefreshToken oldToken = refreshTokenService.issue(USER_ID);
        when(accessTokenIssuer.issueAccessToken(
                anyString(), anyString(), anyList(), anyInt()))
                .thenThrow(new IllegalStateException("token signing failed"));

        assertThatThrownBy(() -> service.refresh(oldToken.token()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("token signing failed");

        assertThat(activeTokenCount()).isEqualTo(1);
        assertThat(totalTokenCount()).isEqualTo(1);

        stubSuccessfulAccessToken();
        assertThat(service.refresh(oldToken.token())).isPresent();
    }

    @Test
    void shouldRevokeOldTokenWhenAccountCannotRefresh() {
        IssuedRefreshToken oldToken = refreshTokenService.issue(USER_ID);
        jdbcTemplate.update(
                "UPDATE t_user_auth SET locked_until = DATEADD('MINUTE', 5, CURRENT_TIMESTAMP) WHERE id = ?",
                USER_ID);

        assertThat(service.refresh(oldToken.token())).isEmpty();
        assertThat(activeTokenCount()).isZero();
        assertThat(totalTokenCount()).isEqualTo(1);
    }

    private void stubSuccessfulAccessToken() {
        reset(accessTokenIssuer);
        when(accessTokenIssuer.issueAccessToken(
                anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(new TokenPair(
                        "access-value",
                        Instant.parse("2026-06-13T01:15:00Z")));
    }

    private int activeTokenCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_refresh_token WHERE user_id = ? AND revoked = 0",
                Integer.class,
                USER_ID);
    }

    private int revokedTokenCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_refresh_token WHERE user_id = ? AND revoked = 1",
                Integer.class,
                USER_ID);
    }

    private int totalTokenCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_refresh_token WHERE user_id = ?",
                Integer.class,
                USER_ID);
    }

    @TestConfiguration
    static class TestIssuerConfiguration {

        @Bean
        @Primary
        AccessTokenIssuer testAccessTokenIssuer() {
            return mock(AccessTokenIssuer.class);
        }
    }
}
