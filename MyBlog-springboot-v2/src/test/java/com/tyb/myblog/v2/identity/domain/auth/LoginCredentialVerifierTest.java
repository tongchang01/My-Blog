package com.tyb.myblog.v2.identity.domain.auth;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 后台登录凭据领域校验测试。
 */
class LoginCredentialVerifierTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 12, 20, 0);
    private static final LoginLockPolicy LOCK_POLICY =
            new LoginLockPolicy(5, Duration.ofMinutes(10));

    @Test
    void authenticatesAdminAndDemoWithMatchingPassword() {
        UserAccount admin = account(AccountType.ADMIN, null);
        UserAccount demo = account(AccountType.DEMO, null);

        assertThat(verify(admin, true))
                .isEqualTo(new LoginCredentialResult.Authenticated(admin));
        assertThat(verify(demo, true))
                .isEqualTo(new LoginCredentialResult.Authenticated(demo));
    }

    @Test
    void hidesMissingGuestAndWrongPasswordBehindBadCredentials() {
        UserAccountRepository missingRepository = username -> Optional.empty();
        PasswordHashVerifier missingPasswordVerifier = mock(PasswordHashVerifier.class);
        LoginStateRecorder missingRecorder = mock(LoginStateRecorder.class);
        LoginCredentialVerifier missingVerifier =
                new LoginCredentialVerifier(
                        missingRepository,
                        missingPasswordVerifier,
                        missingRecorder,
                        LOCK_POLICY);

        assertThat(missingVerifier.verify("missing", "raw", NOW))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);
        verifyNoInteractions(missingPasswordVerifier);
        verifyNoInteractions(missingRecorder);
        assertThat(verify(account(AccountType.GUEST, null), true))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);
        assertThat(verify(account(AccountType.ADMIN, null), false))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);
    }

    @Test
    void returnsLockedWithoutCheckingPassword() {
        PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
        LoginStateRecorder recorder = mock(LoginStateRecorder.class);
        UserAccount account = account(AccountType.ADMIN, NOW.plusMinutes(1));
        LoginCredentialVerifier verifier = new LoginCredentialVerifier(
                username -> Optional.of(account),
                passwordVerifier,
                recorder,
                LOCK_POLICY);

        assertThat(verifier.verify("admin", "raw", NOW))
                .isSameAs(LoginCredentialResult.Locked.INSTANCE);
        verifyNoInteractions(passwordVerifier);
        verifyNoInteractions(recorder);
    }

    @Test
    void recordsPasswordFailureOnlyForLoginCapableAccount() {
        LoginStateRecorder recorder = mock(LoginStateRecorder.class);
        PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
        UserAccount admin = account(AccountType.ADMIN, null);
        when(passwordVerifier.matches("raw", "hash")).thenReturn(false);
        LoginCredentialVerifier verifier = new LoginCredentialVerifier(
                username -> Optional.of(admin),
                passwordVerifier,
                recorder,
                LOCK_POLICY);

        assertThat(verifier.verify("admin", "raw", NOW))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);

        Mockito.verify(recorder).recordPasswordFailure(
                1001L,
                NOW,
                5,
                NOW.plusMinutes(10));
    }

    @Test
    void doesNotRecordFailureForGuestOrSuccessfulAccount() {
        LoginStateRecorder recorder = mock(LoginStateRecorder.class);
        PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);

        LoginCredentialVerifier guestVerifier = new LoginCredentialVerifier(
                username -> Optional.of(account(AccountType.GUEST, null)),
                passwordVerifier,
                recorder,
                LOCK_POLICY);
        assertThat(guestVerifier.verify("guest", "raw", NOW))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);

        when(passwordVerifier.matches("raw", "hash")).thenReturn(true);
        LoginCredentialVerifier successfulVerifier = new LoginCredentialVerifier(
                username -> Optional.of(account(AccountType.ADMIN, null)),
                passwordVerifier,
                recorder,
                LOCK_POLICY);
        assertThat(successfulVerifier.verify("admin", "raw", NOW))
                .isInstanceOf(LoginCredentialResult.Authenticated.class);

        verifyNoInteractions(recorder);
    }

    @Test
    void propagatesFailureRecordingException() {
        LoginStateRecorder recorder = mock(LoginStateRecorder.class);
        PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
        UserAccount admin = account(AccountType.ADMIN, null);
        when(passwordVerifier.matches("raw", "hash")).thenReturn(false);
        doThrow(LoginStateUpdateException.passwordFailure(1001L))
                .when(recorder)
                .recordPasswordFailure(anyLong(), any(), anyInt(), any());
        LoginCredentialVerifier verifier = new LoginCredentialVerifier(
                username -> Optional.of(admin),
                passwordVerifier,
                recorder,
                LOCK_POLICY);

        assertThatThrownBy(() -> verifier.verify("admin", "raw", NOW))
                .isInstanceOf(LoginStateUpdateException.class);
    }

    private LoginCredentialResult verify(UserAccount account, boolean passwordMatches) {
        PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
        LoginStateRecorder recorder = mock(LoginStateRecorder.class);
        when(passwordVerifier.matches("raw", "hash")).thenReturn(passwordMatches);
        LoginCredentialVerifier verifier = new LoginCredentialVerifier(
                username -> Optional.of(account),
                passwordVerifier,
                recorder,
                LOCK_POLICY);
        return verifier.verify(account.username(), "raw", NOW);
    }

    private UserAccount account(AccountType type, LocalDateTime lockedUntil) {
        return new UserAccount(1001L, "admin", "hash", type, 3, 0, lockedUntil);
    }
}
