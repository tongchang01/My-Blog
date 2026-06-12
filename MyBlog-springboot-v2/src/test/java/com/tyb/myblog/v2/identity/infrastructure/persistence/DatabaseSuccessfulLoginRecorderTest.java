package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.identity.domain.auth.LoginStateRecorder;
import com.tyb.myblog.v2.identity.domain.auth.LoginStateUpdateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 登录成功状态数据库记录器集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class DatabaseSuccessfulLoginRecorderTest {

    @Autowired
    private LoginStateRecorder recorder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void shouldRecordSuccessfulLoginAndClearFailureState() {
        long userId = 301L;
        LocalDateTime loggedInAt = LocalDateTime.of(2026, 6, 12, 12, 0);
        insertAccount(userId, 3, loggedInAt.minusMinutes(1), 0);

        recorder.recordSuccessfulLogin(userId, loggedInAt, "2001:db8::1");

        LoginState state = readState(userId);
        assertEquals(loggedInAt, state.lastLoginAt());
        assertEquals("2001:db8::1", state.lastLoginIp());
        assertEquals(0, state.loginFailCount());
        assertNull(state.lockedUntil());
        assertEquals(loggedInAt, state.updatedAt());
        assertEquals(userId, state.updatedBy());
    }

    @Test
    void shouldAcceptMissingClientIp() {
        long userId = 302L;
        LocalDateTime loggedInAt = LocalDateTime.of(2026, 6, 12, 12, 0);
        insertAccount(userId, 0, null, 0);

        recorder.recordSuccessfulLogin(userId, loggedInAt, null);

        LoginState state = readState(userId);
        assertEquals(loggedInAt, state.lastLoginAt());
        assertNull(state.lastLoginIp());
    }

    @Test
    void shouldRejectDeletedLockedOrMissingAccount() {
        LocalDateTime loggedInAt = LocalDateTime.of(2026, 6, 12, 12, 0);
        insertAccount(303L, 0, null, 1);
        insertAccount(304L, 0, loggedInAt.plusMinutes(1), 0);

        assertThrows(
                LoginStateUpdateException.class,
                () -> recorder.recordSuccessfulLogin(303L, loggedInAt, "127.0.0.1"));
        assertThrows(
                LoginStateUpdateException.class,
                () -> recorder.recordSuccessfulLogin(304L, loggedInAt, "127.0.0.1"));
        assertThrows(
                LoginStateUpdateException.class,
                () -> recorder.recordSuccessfulLogin(999L, loggedInAt, "127.0.0.1"));
    }

    private void insertAccount(
            long userId,
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
                        ) VALUES (?, ?, 'hash', 1, 0, ?, ?, ?)
                        """,
                userId,
                "admin-" + userId,
                loginFailCount,
                lockedUntil,
                deleted
        );
    }

    private LoginState readState(long userId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT last_login_at,
                               last_login_ip,
                               login_fail_count,
                               locked_until,
                               updated_at,
                               updated_by
                        FROM t_user_auth
                        WHERE id = ?
                        """,
                (resultSet, rowNumber) -> new LoginState(
                        resultSet.getObject("last_login_at", LocalDateTime.class),
                        resultSet.getString("last_login_ip"),
                        resultSet.getInt("login_fail_count"),
                        resultSet.getObject("locked_until", LocalDateTime.class),
                        resultSet.getObject("updated_at", LocalDateTime.class),
                        resultSet.getObject("updated_by", Long.class)
                ),
                userId
        );
    }

    private record LoginState(
            LocalDateTime lastLoginAt,
            String lastLoginIp,
            int loginFailCount,
            LocalDateTime lockedUntil,
            LocalDateTime updatedAt,
            Long updatedBy
    ) {
    }
}
