package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.identity.domain.auth.LoginFailureRecorder;
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
 * 登录密码失败状态数据库记录器集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class DatabaseLoginFailureRecorderTest {

    @Autowired
    private LoginFailureRecorder recorder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void shouldIncrementFourTimesThenLockAndResetCounter() {
        long userId = 101L;
        LocalDateTime failedAt = LocalDateTime.of(2026, 6, 12, 10, 0);
        LocalDateTime lockedUntil = failedAt.plusMinutes(10);
        insertAccount(userId, 0, null, 0);

        for (int expectedCount = 1; expectedCount <= 4; expectedCount++) {
            LocalDateTime currentFailure = failedAt.plusSeconds(expectedCount);
            recorder.recordPasswordFailure(userId, currentFailure, 5, lockedUntil);

            assertEquals(expectedCount, readFailCount(userId));
            assertNull(readLockedUntil(userId));
        }

        recorder.recordPasswordFailure(userId, failedAt.plusSeconds(5), 5, lockedUntil);

        assertEquals(0, readFailCount(userId));
        assertEquals(lockedUntil, readLockedUntil(userId));
    }

    @Test
    void shouldRejectFailureWhileAccountIsLocked() {
        long userId = 102L;
        LocalDateTime failedAt = LocalDateTime.of(2026, 6, 12, 10, 0);
        insertAccount(userId, 0, failedAt.plusMinutes(5), 0);

        assertThrows(
                LoginStateUpdateException.class,
                () -> recorder.recordPasswordFailure(userId, failedAt, 5, failedAt.plusMinutes(10)));
    }

    @Test
    void shouldRestartCounterAfterLockExpires() {
        long userId = 103L;
        LocalDateTime failedAt = LocalDateTime.of(2026, 6, 12, 10, 0);
        insertAccount(userId, 0, failedAt.minusSeconds(1), 0);

        recorder.recordPasswordFailure(userId, failedAt, 5, failedAt.plusMinutes(10));

        assertEquals(1, readFailCount(userId));
        assertNull(readLockedUntil(userId));
    }

    @Test
    void shouldRejectDeletedOrMissingAccount() {
        LocalDateTime failedAt = LocalDateTime.of(2026, 6, 12, 10, 0);
        insertAccount(104L, 0, null, 1);

        assertThrows(
                LoginStateUpdateException.class,
                () -> recorder.recordPasswordFailure(104L, failedAt, 5, failedAt.plusMinutes(10)));
        assertThrows(
                LoginStateUpdateException.class,
                () -> recorder.recordPasswordFailure(999L, failedAt, 5, failedAt.plusMinutes(10)));
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

    private int readFailCount(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT login_fail_count FROM t_user_auth WHERE id = ?",
                Integer.class,
                userId
        );
    }

    private LocalDateTime readLockedUntil(long userId) {
        return jdbcTemplate.queryForObject(
                "SELECT locked_until FROM t_user_auth WHERE id = ?",
                LocalDateTime.class,
                userId
        );
    }
}
