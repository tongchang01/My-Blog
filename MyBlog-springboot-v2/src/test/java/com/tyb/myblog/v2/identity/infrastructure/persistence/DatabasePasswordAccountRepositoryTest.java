package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.ChangeablePasswordAccount;
import com.tyb.myblog.v2.identity.domain.account.PasswordAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 修改密码账号数据库仓储集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class DatabasePasswordAccountRepositoryTest {

    private static final LocalDateTime UPDATED_AT =
            LocalDateTime.of(2026, 6, 14, 12, 0);

    @Autowired
    private PasswordAccountRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void locksAndReturnsOnlyActiveAccountForPasswordChange() {
        insertAccount(1001L, 1, "old-hash", 3, 0);

        ChangeablePasswordAccount account =
                repository.findActiveByIdForUpdate(1001L).orElseThrow();

        assertThat(account.id()).isEqualTo(1001L);
        assertThat(account.type()).isEqualTo(AccountType.ADMIN);
        assertThat(account.passwordHash()).isEqualTo("old-hash");
    }

    @Test
    void excludesDeletedAccount() {
        insertAccount(1002L, 1, "old-hash", 0, 1);

        assertThat(repository.findActiveByIdForUpdate(1002L)).isEmpty();
    }

    @Test
    void updatesPasswordAndIncrementsTokenVersionWithAudit() {
        insertAccount(1003L, 1, "old-hash", 7, 0);

        boolean updated = repository.updatePasswordAndIncrementTokenVersion(
                1003L, "new-hash", UPDATED_AT, 1003L);

        assertThat(updated).isTrue();
        Map<String, Object> account = jdbcTemplate.queryForMap(
                """
                        SELECT password_hash, token_version, updated_at, updated_by
                        FROM t_user_auth
                        WHERE id = ?
                        """,
                1003L);
        assertThat(account)
                .containsEntry("PASSWORD_HASH", "new-hash")
                .containsEntry("TOKEN_VERSION", 8)
                .containsEntry("UPDATED_AT", Timestamp.valueOf(UPDATED_AT))
                .containsEntry("UPDATED_BY", 1003L);
    }

    private void insertAccount(
            long id,
            int type,
            String passwordHash,
            int tokenVersion,
            int deleted
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type,
                            token_version, deleted
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        """,
                id,
                "user-" + id,
                passwordHash,
                type,
                tokenVersion,
                deleted);
    }
}
