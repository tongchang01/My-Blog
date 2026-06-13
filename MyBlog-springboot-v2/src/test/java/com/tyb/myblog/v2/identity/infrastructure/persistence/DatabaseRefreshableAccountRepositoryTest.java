package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccount;
import com.tyb.myblog.v2.identity.domain.account.RefreshableAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 可刷新后台账号查询的数据库集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class DatabaseRefreshableAccountRepositoryTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 13, 10, 0);

    @Autowired
    private RefreshableAccountRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void shouldReturnAdminAccountSnapshot() {
        insertAccount(1L, "admin", 1, 4, null, 0);

        RefreshableAccount account =
                repository.findRefreshableById(1L, NOW).orElseThrow();

        assertThat(account.id()).isEqualTo(1L);
        assertThat(account.username()).isEqualTo("admin");
        assertThat(account.type()).isEqualTo(AccountType.ADMIN);
        assertThat(account.tokenVersion()).isEqualTo(4);
    }

    @Test
    void shouldReturnDemoAccountSnapshot() {
        insertAccount(2L, "demo", 2, 5, null, 0);

        Optional<RefreshableAccount> result =
                repository.findRefreshableById(2L, NOW);

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().type()).isEqualTo(AccountType.DEMO);
    }

    @Test
    void shouldRejectGuestDeletedAndLockedAccounts() {
        insertAccount(3L, "guest", 3, 0, null, 0);
        insertAccount(4L, "deleted", 1, 0, null, 1);
        insertAccount(5L, "locked", 1, 0, NOW.plusSeconds(1), 0);

        assertThat(repository.findRefreshableById(3L, NOW)).isEmpty();
        assertThat(repository.findRefreshableById(4L, NOW)).isEmpty();
        assertThat(repository.findRefreshableById(5L, NOW)).isEmpty();
    }

    @Test
    void shouldAllowAccountWhenLockHasEnded() {
        insertAccount(6L, "unlocked", 1, 0, NOW, 0);

        assertThat(repository.findRefreshableById(6L, NOW)).isPresent();
    }

    private void insertAccount(
            long id,
            String username,
            int type,
            int tokenVersion,
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
                            locked_until,
                            deleted
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                username,
                "$2a$10$test-password-hash",
                type,
                tokenVersion,
                lockedUntil,
                deleted);
    }
}
