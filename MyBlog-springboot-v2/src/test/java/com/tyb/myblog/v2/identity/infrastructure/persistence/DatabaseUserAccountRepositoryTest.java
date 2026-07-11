package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.UserAccount;
import com.tyb.myblog.v2.identity.domain.account.UserAccountRepository;
import com.tyb.myblog.v2.identity.domain.bootstrap.AdminBootstrapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录账号数据库仓储集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class DatabaseUserAccountRepositoryTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AdminBootstrapRepository adminBootstrapRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void shouldReadActiveAccountByUsername() {
        LocalDateTime lockedUntil = LocalDateTime.of(2026, 6, 12, 21, 30);
        insertAccount(1L, "admin", 1, 4, 2, lockedUntil, 0);

        Optional<UserAccount> result = userAccountRepository.findActiveByUsername("admin");

        assertTrue(result.isPresent());
        UserAccount account = result.orElseThrow();
        assertEquals(1L, account.id());
        assertEquals("admin", account.username());
        assertEquals("$2a$10$test-password-hash", account.passwordHash());
        assertEquals(AccountType.ADMIN, account.type());
        assertEquals(4, account.tokenVersion());
        assertEquals(2, account.loginFailCount());
        assertEquals(lockedUntil, account.lockedUntil());
    }

    @Test
    void shouldIgnoreDeletedAccount() {
        insertAccount(2L, "deleted-admin", 1, 0, 0, null, 1);

        Optional<UserAccount> result = userAccountRepository.findActiveByUsername("deleted-admin");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldCreateAdminWithGeneratedIdAndDetectIt() {
        assertFalse(adminBootstrapRepository.existsActiveAdmin());

        UserAccount created = adminBootstrapRepository.createAdmin(
                "admin", "$2a$10$test-password-hash");

        assertTrue(created.id() > 0);
        assertEquals("admin", created.username());
        assertEquals(AccountType.ADMIN, created.type());
        assertTrue(adminBootstrapRepository.existsActiveAdmin());
        assertEquals("admin", jdbcTemplate.queryForObject(
                "SELECT username FROM t_user_auth WHERE id = ?",
                String.class,
                created.id()));
    }

    private void insertAccount(
            long id,
            String username,
            int type,
            int tokenVersion,
            int loginFailCount,
            LocalDateTime lockedUntil,
            int deleted
    ) {
        jdbcTemplate.update("""
                        INSERT INTO t_user_auth (
                            id,
                            username,
                            password_hash,
                            type,
                            token_version,
                            login_fail_count,
                            locked_until,
                            deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                username,
                "$2a$10$test-password-hash",
                type,
                tokenVersion,
                loginFailCount,
                lockedUntil,
                deleted
        );
    }
}
