package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.auth.LoginCredentialResult;
import com.tyb.myblog.v2.identity.domain.auth.LoginCredentialVerifier;
import com.tyb.myblog.v2.identity.domain.auth.LoginRateLimiter;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 后台登录应用编排测试。
 */
class AuthApplicationServiceTest {

    private static final String CLIENT_IP = "203.0.113.10";
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 12, 12, 0);

    private final LoginCredentialVerifier credentialVerifier =
            mock(LoginCredentialVerifier.class);
    private final LoginRateLimiter rateLimiter = mock(LoginRateLimiter.class);
    private final LoginSuccessTransactionService successService =
            mock(LoginSuccessTransactionService.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-12T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));
    private final AuthApplicationService service = new AuthApplicationService(
            credentialVerifier,
            rateLimiter,
            successService,
            clock);

    @Test
    void rejectsBlockedKeyBeforeCredentialVerification() {
        when(rateLimiter.isBlocked(CLIENT_IP, "admin")).thenReturn(true);

        assertThatThrownBy(() -> service.login(
                new LoginCommand(" Admin ", "secret", CLIENT_IP)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.RATE_LIMITED));

        verifyNoInteractions(credentialVerifier, successService);
    }

    @Test
    void recordsCaffeineFailureForBadCredentials() {
        when(credentialVerifier.verify("admin", " secret ", NOW))
                .thenReturn(LoginCredentialResult.BadCredentials.INSTANCE);

        assertThatThrownBy(() -> service.login(
                new LoginCommand(" Admin ", " secret ", CLIENT_IP)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.BAD_CREDENTIALS));

        verify(rateLimiter).recordFailure(CLIENT_IP, "admin");
        verifyNoInteractions(successService);
    }

    @Test
    void mapsLockedAccountWithoutGrowingCaffeineCount() {
        when(credentialVerifier.verify("admin", "secret", NOW))
                .thenReturn(LoginCredentialResult.Locked.INSTANCE);

        assertThatThrownBy(() -> service.login(
                new LoginCommand("admin", "secret", CLIENT_IP)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.BAD_CREDENTIALS));

        verify(rateLimiter, never()).recordFailure(anyString(), anyString());
        verifyNoInteractions(successService);
    }

    @Test
    void resetsRateLimitBeforeCompletingSuccessfulLogin() {
        UserAccount account = account();
        LoginTokenResult expected =
                new LoginTokenResult("access", "refresh", 900, 604800);
        when(credentialVerifier.verify("admin", " secret ", NOW))
                .thenReturn(new LoginCredentialResult.Authenticated(account));
        when(successService.complete(account, CLIENT_IP, NOW))
                .thenReturn(expected);

        LoginTokenResult actual = service.login(
                new LoginCommand(" Admin ", " secret ", CLIENT_IP));

        assertThat(actual).isEqualTo(expected);
        InOrder order = inOrder(rateLimiter, credentialVerifier, successService);
        order.verify(rateLimiter).isBlocked(CLIENT_IP, "admin");
        order.verify(credentialVerifier).verify("admin", " secret ", NOW);
        order.verify(rateLimiter).reset(CLIENT_IP, "admin");
        order.verify(successService).complete(account, CLIENT_IP, NOW);
    }

    @Test
    void propagatesSuccessfulTransactionFailure() {
        UserAccount account = account();
        IllegalStateException failure =
                new IllegalStateException("token signing failed");
        when(credentialVerifier.verify("admin", "secret", NOW))
                .thenReturn(new LoginCredentialResult.Authenticated(account));
        when(successService.complete(account, CLIENT_IP, NOW))
                .thenThrow(failure);

        assertThatThrownBy(() -> service.login(
                new LoginCommand("admin", "secret", CLIENT_IP)))
                .isSameAs(failure);
    }

    private UserAccount account() {
        return new UserAccount(
                1001L,
                "admin",
                "hash",
                AccountType.ADMIN,
                3,
                0,
                null);
    }
}
