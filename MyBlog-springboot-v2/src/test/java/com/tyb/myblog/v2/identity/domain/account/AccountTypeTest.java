package com.tyb.myblog.v2.identity.domain.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账号类型领域规则测试。
 */
class AccountTypeTest {

    @Test
    void shouldMapDatabaseValueAndAdminLoginPermission() {
        assertEquals(1, AccountType.ADMIN.databaseValue());
        assertEquals(2, AccountType.DEMO.databaseValue());
        assertEquals(3, AccountType.GUEST.databaseValue());

        assertTrue(AccountType.ADMIN.canLoginToAdmin());
        assertTrue(AccountType.DEMO.canLoginToAdmin());
        assertFalse(AccountType.GUEST.canLoginToAdmin());

        assertEquals(AccountType.ADMIN, AccountType.fromDatabaseValue(1));
        assertEquals(AccountType.DEMO, AccountType.fromDatabaseValue(2));
        assertEquals(AccountType.GUEST, AccountType.fromDatabaseValue(3));
    }

    @Test
    void shouldRejectUnknownDatabaseValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> AccountType.fromDatabaseValue(99)
        );

        assertTrue(exception.getMessage().contains("99"));
    }
}
