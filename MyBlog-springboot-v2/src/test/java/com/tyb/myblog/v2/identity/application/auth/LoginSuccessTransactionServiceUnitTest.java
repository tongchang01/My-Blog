package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.auth.token.TokenPair;
import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.auth.LoginStateRecorder;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 后台登录成功短事务单元测试。
 */
class LoginSuccessTransactionServiceUnitTest {

    private static final LocalDateTime LOGGED_IN_AT =
            LocalDateTime.of(2026, 6, 12, 12, 0);
    private static final String CLIENT_IP = "203.0.113.10";

    private final LoginStateRecorder loginStateRecorder =
            mock(LoginStateRecorder.class);
    private final RefreshTokenService refreshTokenService =
            mock(RefreshTokenService.class);
    private final AccessTokenIssuer accessTokenIssuer =
            mock(AccessTokenIssuer.class);
    private final SecurityJwtProperties properties = new SecurityJwtProperties(
            "myblog-v2-test",
            "test-secret-test-secret-test-secret-123456",
            Duration.ofMinutes(15),
            Duration.ofDays(7));
    private final LoginSuccessTransactionService service =
            new LoginSuccessTransactionService(
                    loginStateRecorder,
                    refreshTokenService,
                    accessTokenIssuer,
                    properties);

    @Test
    void completesAdminLoginWithConfiguredTokenTtl() {
        UserAccount account = account(AccountType.ADMIN);
        stubTokens(account);

        LoginTokenResult result =
                service.complete(account, CLIENT_IP, LOGGED_IN_AT);

        verify(loginStateRecorder).recordSuccessfulLogin(
                1001L, LOGGED_IN_AT, CLIENT_IP);
        verify(refreshTokenService).issue(1001L);
        verify(accessTokenIssuer).issueAccessToken(
                "1001", "admin", List.of("ADMIN"), 3);
        assertThat(result).isEqualTo(new LoginTokenResult(
                "access-value", "refresh-value", 900, 604800));
    }

    @Test
    void issuesDemoRoleForDemoAccount() {
        UserAccount account = account(AccountType.DEMO);
        stubTokens(account);

        service.complete(account, CLIENT_IP, LOGGED_IN_AT);

        verify(accessTokenIssuer).issueAccessToken(
                "1001", "admin", List.of("DEMO"), 3);
    }

    private void stubTokens(UserAccount account) {
        when(refreshTokenService.issue(account.id()))
                .thenReturn(new IssuedRefreshToken(
                        account.id(),
                        "refresh-value",
                        LOGGED_IN_AT.plusDays(7)));
        when(accessTokenIssuer.issueAccessToken(
                "1001",
                "admin",
                List.of(account.type().name()),
                3))
                .thenReturn(new TokenPair(
                        "access-value",
                        Instant.parse("2026-06-12T03:15:00Z")));
    }

    private UserAccount account(AccountType type) {
        return new UserAccount(
                1001L,
                "admin",
                "hash",
                type,
                3,
                0,
                null);
    }
}
