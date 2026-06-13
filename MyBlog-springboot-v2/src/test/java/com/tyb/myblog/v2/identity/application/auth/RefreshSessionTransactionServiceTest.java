package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.token.AccessTokenIssuer;
import com.tyb.myblog.v2.common.auth.token.TokenPair;
import com.tyb.myblog.v2.common.config.SecurityJwtProperties;
import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccount;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccountRepository;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRecord;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * refresh token 轮换事务单元测试。
 */
class RefreshSessionTransactionServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-13T01:00:00Z"),
            ZoneId.of("Asia/Tokyo"));
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private final RefreshTokenService refreshTokenService =
            mock(RefreshTokenService.class);
    private final RefreshableAccountRepository accountRepository =
            mock(RefreshableAccountRepository.class);
    private final AccessTokenIssuer accessTokenIssuer =
            mock(AccessTokenIssuer.class);
    private final RefreshSessionTransactionService service =
            new RefreshSessionTransactionService(
                    refreshTokenService,
                    accountRepository,
                    accessTokenIssuer,
                    properties(),
                    CLOCK);

    @Test
    void shouldStopWhenOldTokenDoesNotExist() {
        when(refreshTokenService.findActiveForUpdate("raw-token", NOW))
                .thenReturn(Optional.empty());

        assertThat(service.refresh("raw-token")).isEmpty();
        verifyNoInteractions(accountRepository, accessTokenIssuer);
    }

    @Test
    void shouldRevokeOldTokenWhenAccountCannotRefresh() {
        RefreshTokenRecord token = token();
        when(refreshTokenService.findActiveForUpdate("raw-token", NOW))
                .thenReturn(Optional.of(token));
        when(accountRepository.findRefreshableById(1001L, NOW))
                .thenReturn(Optional.empty());

        assertThat(service.refresh("raw-token")).isEmpty();
        verify(refreshTokenService).revoke(10L);
        verify(refreshTokenService, never()).issue(1001L);
        verifyNoInteractions(accessTokenIssuer);
    }

    @Test
    void shouldStopWhenOldTokenCannotBeRevoked() {
        RefreshTokenRecord token = token();
        RefreshableAccount account = account();
        when(refreshTokenService.findActiveForUpdate("raw-token", NOW))
                .thenReturn(Optional.of(token));
        when(accountRepository.findRefreshableById(1001L, NOW))
                .thenReturn(Optional.of(account));
        when(refreshTokenService.revoke(10L)).thenReturn(false);

        assertThat(service.refresh("raw-token")).isEmpty();
        verify(refreshTokenService, never()).issue(1001L);
        verifyNoInteractions(accessTokenIssuer);
    }

    @Test
    void shouldRotateInOrderUsingLatestAccountSnapshot() {
        RefreshTokenRecord token = token();
        RefreshableAccount account = account();
        IssuedRefreshToken newRefresh = new IssuedRefreshToken(
                1001L, "new-refresh", NOW.plusDays(7));
        when(refreshTokenService.findActiveForUpdate("raw-token", NOW))
                .thenReturn(Optional.of(token));
        when(accountRepository.findRefreshableById(1001L, NOW))
                .thenReturn(Optional.of(account));
        when(refreshTokenService.revoke(10L)).thenReturn(true);
        when(refreshTokenService.issue(1001L)).thenReturn(newRefresh);
        when(accessTokenIssuer.issueAccessToken(
                "1001", "renamed-admin", List.of("ADMIN"), 7))
                .thenReturn(new TokenPair(
                        "new-access",
                        Instant.parse("2026-06-13T01:15:00Z")));

        Optional<LoginTokenResult> result = service.refresh("raw-token");

        assertThat(result).contains(new LoginTokenResult(
                "new-access", "new-refresh", 900, 604800));
        InOrder order = inOrder(
                refreshTokenService,
                accountRepository,
                accessTokenIssuer);
        order.verify(refreshTokenService)
                .findActiveForUpdate("raw-token", NOW);
        order.verify(accountRepository)
                .findRefreshableById(1001L, NOW);
        order.verify(refreshTokenService).revoke(10L);
        order.verify(refreshTokenService).issue(1001L);
        order.verify(accessTokenIssuer).issueAccessToken(
                "1001", "renamed-admin", List.of("ADMIN"), 7);
    }

    private RefreshTokenRecord token() {
        return new RefreshTokenRecord(
                10L, 1001L, "hash", NOW.plusDays(1), false);
    }

    private RefreshableAccount account() {
        return new RefreshableAccount(
                1001L, "renamed-admin", AccountType.ADMIN, 7);
    }

    private SecurityJwtProperties properties() {
        return new SecurityJwtProperties(
                "myblog-v2-test",
                "test-secret-test-secret-test-secret-123456",
                Duration.ofMinutes(15),
                Duration.ofDays(7));
    }
}
