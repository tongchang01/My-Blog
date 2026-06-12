package com.tyb.myblog.v2.identity.domain.auth;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 后台登录凭据领域校验测试。
 */
class LoginCredentialVerifierTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 12, 20, 0);

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
        LoginCredentialVerifier missingVerifier =
                new LoginCredentialVerifier(missingRepository, missingPasswordVerifier);

        assertThat(missingVerifier.verify("missing", "raw", NOW))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);
        verifyNoInteractions(missingPasswordVerifier);
        assertThat(verify(account(AccountType.GUEST, null), true))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);
        assertThat(verify(account(AccountType.ADMIN, null), false))
                .isSameAs(LoginCredentialResult.BadCredentials.INSTANCE);
    }

    @Test
    void returnsLockedWithoutCheckingPassword() {
        PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
        UserAccount account = account(AccountType.ADMIN, NOW.plusMinutes(1));
        LoginCredentialVerifier verifier =
                new LoginCredentialVerifier(username -> Optional.of(account), passwordVerifier);

        assertThat(verifier.verify("admin", "raw", NOW))
                .isSameAs(LoginCredentialResult.Locked.INSTANCE);
        verifyNoInteractions(passwordVerifier);
    }

    private LoginCredentialResult verify(UserAccount account, boolean passwordMatches) {
        PasswordHashVerifier passwordVerifier = mock(PasswordHashVerifier.class);
        when(passwordVerifier.matches("raw", "hash")).thenReturn(passwordMatches);
        LoginCredentialVerifier verifier =
                new LoginCredentialVerifier(username -> Optional.of(account), passwordVerifier);
        return verifier.verify(account.username(), "raw", NOW);
    }

    private UserAccount account(AccountType type, LocalDateTime lockedUntil) {
        return new UserAccount(1001L, "admin", "hash", type, 3, 0, lockedUntil);
    }
}
