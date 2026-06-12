package com.tyb.myblog.v2.identity.domain.account;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录账号领域行为测试。
 */
class UserAccountTest {

    @Test
    void shouldDelegateAdminLoginPermissionToAccountType() {
        UserAccount admin = account(AccountType.ADMIN, null);
        UserAccount guest = account(AccountType.GUEST, null);

        assertTrue(admin.canLoginToAdmin());
        assertFalse(guest.canLoginToAdmin());
    }

    @Test
    void shouldBeLockedOnlyBeforeLockedUntil() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 20, 0);
        UserAccount locked = account(AccountType.ADMIN, now.plusMinutes(1));
        UserAccount unlockedAtBoundary = account(AccountType.ADMIN, now);
        UserAccount neverLocked = account(AccountType.ADMIN, null);

        assertTrue(locked.isLockedAt(now));
        assertFalse(unlockedAtBoundary.isLockedAt(now));
        assertFalse(neverLocked.isLockedAt(now));
    }

    private UserAccount account(AccountType type, LocalDateTime lockedUntil) {
        return new UserAccount(
                1L,
                "admin",
                "$2a$10$hash",
                type,
                3,
                0,
                lockedUntil
        );
    }
}
