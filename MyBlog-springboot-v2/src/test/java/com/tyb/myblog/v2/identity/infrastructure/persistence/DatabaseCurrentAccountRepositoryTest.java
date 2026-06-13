package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.identity.domain.account.AccountType;
import com.tyb.myblog.v2.identity.domain.account.CurrentAccount;
import com.tyb.myblog.v2.identity.domain.account.CurrentAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 当前账号查询仓储集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class DatabaseCurrentAccountRepositoryTest {

    @Autowired
    private CurrentAccountRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void shouldReadActiveAccountFieldsById() {
        insertAccount(1001L, "admin", 1, 0);

        Optional<CurrentAccount> result = repository.findActiveById(1001L);

        assertThat(result).contains(
                new CurrentAccount(1001L, "admin", AccountType.ADMIN));
    }

    @Test
    void shouldIgnoreDeletedAccount() {
        insertAccount(1002L, "deleted-admin", 2, 1);

        assertThat(repository.findActiveById(1002L)).isEmpty();
    }

    private void insertAccount(
            long id,
            String username,
            int type,
            int deleted
    ) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type, deleted
                        ) VALUES (?, ?, ?, ?, ?)
                        """,
                id,
                username,
                "$2a$10$test-password-hash",
                type,
                deleted);
    }
}
