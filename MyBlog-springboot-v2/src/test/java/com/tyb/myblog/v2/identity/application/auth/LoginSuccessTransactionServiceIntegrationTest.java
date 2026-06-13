package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.auth.token.TokenPair;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
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
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * 登录成功短事务的数据库集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@Import(LoginSuccessTransactionServiceIntegrationTest.TestIssuerConfiguration.class)
class LoginSuccessTransactionServiceIntegrationTest {

    private static final long USER_ID = 1001L;
    private static final String CLIENT_IP = "203.0.113.10";
    private static final LocalDateTime LOGGED_IN_AT =
            LocalDateTime.of(2026, 6, 12, 12, 0);

    @Autowired
    private LoginSuccessTransactionService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @BeforeEach
    void prepareAccount() {
        reset(accessTokenIssuer);
        jdbcTemplate.update("delete from t_refresh_token");
        jdbcTemplate.update("delete from t_user_auth");
        jdbcTemplate.update("""
                insert into t_user_auth (
                    id, username, password_hash, type, token_version,
                    login_fail_count, locked_until, deleted
                ) values (?, 'admin', '$2a$10$test-password-hash', 1, 3, 4, ?, 0)
                """, USER_ID, LOGGED_IN_AT.minusMinutes(1));
    }

    @Test
    void commitsAuditAndRefreshTokenWhenAccessTokenSigningSucceeds() {
        when(accessTokenIssuer.issueAccessToken(
                anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(new TokenPair(
                        "access-value",
                        Instant.parse("2026-06-12T03:15:00Z")));

        LoginTokenResult result = service.complete(account(), CLIENT_IP, LOGGED_IN_AT);

        LoginState state = readLoginState();
        assertThat(state.lastLoginAt()).isEqualTo(LOGGED_IN_AT);
        assertThat(state.lastLoginIp()).isEqualTo(CLIENT_IP);
        assertThat(state.loginFailCount()).isZero();
        assertThat(state.lockedUntil()).isNull();
        assertThat(refreshTokenCount()).isEqualTo(1);
        assertThat(storedRefreshTokenHash()).isNotEqualTo(result.refreshToken());
    }

    @Test
    void rollsBackAuditAndRefreshTokenWhenAccessTokenSigningFails() {
        when(accessTokenIssuer.issueAccessToken(
                anyString(), anyString(), anyList(), anyInt()))
                .thenThrow(new IllegalStateException("token signing failed"));

        assertThatThrownBy(() -> service.complete(account(), CLIENT_IP, LOGGED_IN_AT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("token signing failed");

        LoginState state = readLoginState();
        assertThat(state.lastLoginAt()).isNull();
        assertThat(state.lastLoginIp()).isNull();
        assertThat(state.loginFailCount()).isEqualTo(4);
        assertThat(state.lockedUntil()).isEqualTo(LOGGED_IN_AT.minusMinutes(1));
        assertThat(refreshTokenCount()).isZero();
    }

    private UserAccount account() {
        return new UserAccount(
                USER_ID,
                "admin",
                "$2a$10$test-password-hash",
                AccountType.ADMIN,
                3,
                4,
                LOGGED_IN_AT.minusMinutes(1));
    }

    private LoginState readLoginState() {
        return jdbcTemplate.queryForObject(
                """
                        select last_login_at, last_login_ip, login_fail_count, locked_until
                        from t_user_auth
                        where id = ?
                        """,
                (resultSet, rowNumber) -> new LoginState(
                        resultSet.getObject("last_login_at", LocalDateTime.class),
                        resultSet.getString("last_login_ip"),
                        resultSet.getInt("login_fail_count"),
                        resultSet.getObject("locked_until", LocalDateTime.class)),
                USER_ID);
    }

    private int refreshTokenCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from t_refresh_token where user_id = ?",
                Integer.class,
                USER_ID);
    }

    private String storedRefreshTokenHash() {
        return jdbcTemplate.queryForObject(
                "select token_hash from t_refresh_token where user_id = ?",
                String.class,
                USER_ID);
    }

    private record LoginState(
            LocalDateTime lastLoginAt,
            String lastLoginIp,
            int loginFailCount,
            LocalDateTime lockedUntil
    ) {
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
