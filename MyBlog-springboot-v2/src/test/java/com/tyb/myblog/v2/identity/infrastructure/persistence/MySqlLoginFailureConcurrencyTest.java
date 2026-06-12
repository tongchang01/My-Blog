package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.identity.domain.auth.LoginStateRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MySQL 下登录失败原子累计的并发集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MySqlLoginFailureConcurrencyTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("myblog_v2_login_test")
            .withUsername("myblog")
            .withPassword("myblog-test-password");

    @Autowired
    private LoginStateRecorder recorder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void shouldLockAccountWithoutLosingConcurrentFailures() throws Exception {
        long userId = 201L;
        LocalDateTime failedAt = LocalDateTime.of(2026, 6, 12, 11, 0);
        LocalDateTime lockedUntil = failedAt.plusMinutes(10);
        insertAccount(userId);

        CountDownLatch ready = new CountDownLatch(5);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            List<? extends Future<?>> futures = IntStream.range(0, 5)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        recorder.recordPasswordFailure(
                                userId,
                                failedAt.plusNanos(index),
                                5,
                                lockedUntil
                        );
                        return null;
                    }))
                    .toList();

            ready.await();
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(0, readFailCount(userId));
        assertEquals(lockedUntil, readLockedUntil(userId));
    }

    private void insertAccount(long userId) {
        jdbcTemplate.update("""
                        INSERT INTO t_user_auth (
                            id,
                            username,
                            password_hash,
                            type,
                            token_version,
                            login_fail_count,
                            deleted
                        ) VALUES (?, ?, 'hash', 1, 0, 0, 0)
                        """,
                userId,
                "admin-" + userId
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
