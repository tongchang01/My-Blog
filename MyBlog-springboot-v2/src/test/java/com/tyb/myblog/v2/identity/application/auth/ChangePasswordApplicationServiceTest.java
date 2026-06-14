package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.ChangeablePasswordAccount;
import com.tyb.myblog.v2.identity.domain.account.PasswordAccountRepository;
import com.tyb.myblog.v2.identity.domain.auth.PasswordHashService;
import com.tyb.myblog.v2.identity.domain.token.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 修改密码应用服务单元测试。
 */
class ChangePasswordApplicationServiceTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 14, 12, 0);

    private final PasswordAccountRepository repository =
            mock(PasswordAccountRepository.class);
    private final PasswordHashService passwordHashService =
            mock(PasswordHashService.class);
    private final RefreshTokenRepository refreshTokenRepository =
            mock(RefreshTokenRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-06-14T03:00:00Z"),
            ZoneId.of("Asia/Tokyo"));
    private final ChangePasswordApplicationService service =
            new ChangePasswordApplicationService(
                    repository,
                    passwordHashService,
                    refreshTokenRepository,
                    clock);

    @BeforeEach
    void resetMocks() {
        reset(repository, passwordHashService, refreshTokenRepository);
    }

    @Test
    void changesPasswordAndRevokesAllRefreshTokensForAdmin() {
        when(repository.findActiveByIdForUpdate(1001L))
                .thenReturn(Optional.of(account(AccountType.ADMIN)));
        when(passwordHashService.matches("old-password", "old-hash"))
                .thenReturn(true);
        when(passwordHashService.matches("new-password", "old-hash"))
                .thenReturn(false);
        when(passwordHashService.encode("new-password"))
                .thenReturn("new-hash");
        when(repository.updatePasswordAndIncrementTokenVersion(
                1001L, "new-hash", NOW, 1001L))
                .thenReturn(true);

        service.change(
                principal("1001", "ADMIN"),
                new ChangePasswordCommand(
                        "old-password",
                        "new-password"));

        verify(repository).updatePasswordAndIncrementTokenVersion(
                1001L, "new-hash", NOW, 1001L);
        verify(refreshTokenRepository).revokeAllByUserId(1001L);
    }

    @Test
    void rejectsDemoBeforeAccessingPasswordState() {
        assertCode(
                ApiErrorCode.FORBIDDEN,
                () -> service.change(
                        principal("1001", "DEMO"),
                        validCommand()));

        verifyNoInteractions(
                repository,
                passwordHashService,
                refreshTokenRepository);
    }

    @Test
    void rejectsAccountThatIsNoLongerAdmin() {
        when(repository.findActiveByIdForUpdate(1001L))
                .thenReturn(Optional.of(account(AccountType.DEMO)));

        assertCode(
                ApiErrorCode.FORBIDDEN,
                () -> service.change(
                        principal("1001", "ADMIN"),
                        validCommand()));

        verifyNoInteractions(
                passwordHashService,
                refreshTokenRepository);
    }

    @Test
    void rejectsInvalidPrincipalId() {
        assertCode(
                ApiErrorCode.INVALID_TOKEN,
                () -> service.change(
                        principal("invalid", "ADMIN"),
                        validCommand()));

        verifyNoInteractions(
                repository,
                passwordHashService,
                refreshTokenRepository);
    }

    @Test
    void rejectsInvalidNewPasswordLength() {
        assertCode(
                ApiErrorCode.VALIDATION_ERROR,
                () -> service.change(
                        principal("1001", "ADMIN"),
                        new ChangePasswordCommand(
                                "old-password",
                                "short")));

        verifyNoInteractions(
                repository,
                passwordHashService,
                refreshTokenRepository);
    }

    @Test
    void rejectsWrongCurrentPasswordWithoutChangingState() {
        when(repository.findActiveByIdForUpdate(1001L))
                .thenReturn(Optional.of(account(AccountType.ADMIN)));
        when(passwordHashService.matches("wrong-password", "old-hash"))
                .thenReturn(false);

        assertCode(
                ApiErrorCode.BAD_CREDENTIALS,
                () -> service.change(
                        principal("1001", "ADMIN"),
                        new ChangePasswordCommand(
                                "wrong-password",
                                "new-password")));

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void rejectsPasswordThatMatchesCurrentHash() {
        when(repository.findActiveByIdForUpdate(1001L))
                .thenReturn(Optional.of(account(AccountType.ADMIN)));
        when(passwordHashService.matches("old-password", "old-hash"))
                .thenReturn(true);
        when(passwordHashService.matches("same-password", "old-hash"))
                .thenReturn(true);

        assertCode(
                ApiErrorCode.VALIDATION_ERROR,
                () -> service.change(
                        principal("1001", "ADMIN"),
                        new ChangePasswordCommand(
                                "old-password",
                                "same-password")));

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void mapsMissingAccountToInternalError() {
        when(repository.findActiveByIdForUpdate(1001L))
                .thenReturn(Optional.empty());

        assertCode(
                ApiErrorCode.INTERNAL_ERROR,
                () -> service.change(
                        principal("1001", "ADMIN"),
                        validCommand()));

        verifyNoInteractions(
                passwordHashService,
                refreshTokenRepository);
    }

    @Test
    void mapsUnexpectedUpdateCountToInternalError() {
        when(repository.findActiveByIdForUpdate(1001L))
                .thenReturn(Optional.of(account(AccountType.ADMIN)));
        when(passwordHashService.matches("old-password", "old-hash"))
                .thenReturn(true);
        when(passwordHashService.matches("new-password", "old-hash"))
                .thenReturn(false);
        when(passwordHashService.encode("new-password"))
                .thenReturn("new-hash");
        when(repository.updatePasswordAndIncrementTokenVersion(
                1001L, "new-hash", NOW, 1001L))
                .thenReturn(false);

        assertCode(
                ApiErrorCode.INTERNAL_ERROR,
                () -> service.change(
                        principal("1001", "ADMIN"),
                        validCommand()));

        verifyNoInteractions(refreshTokenRepository);
    }

    private ChangeablePasswordAccount account(AccountType type) {
        return new ChangeablePasswordAccount(1001L, type, "old-hash");
    }

    private ChangePasswordCommand validCommand() {
        return new ChangePasswordCommand(
                "old-password",
                "new-password");
    }

    private AuthenticatedPrincipal principal(String id, String role) {
        return new AuthenticatedPrincipal(id, "admin", List.of(role));
    }

    private void assertCode(ApiErrorCode code, Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(code));
    }
}
