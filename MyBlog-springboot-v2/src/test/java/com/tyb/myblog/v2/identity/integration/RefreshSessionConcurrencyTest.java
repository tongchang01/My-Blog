package com.tyb.myblog.v2.identity.integration;

import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.application.auth.LoginTokenResult;
import com.tyb.myblog.v2.identity.application.auth.RefreshSessionApplicationService;
import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 同一 refresh token 并发重放的数据库验收测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class RefreshSessionConcurrencyTest {

    private static final long USER_ID = 4001L;

    @Autowired
    private RefreshSessionApplicationService refreshSessionApplicationService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void prepareAccount() {
        executor = Executors.newFixedThreadPool(2);
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        jdbcTemplate.update("""
                INSERT INTO t_user_auth (
                    id, username, password_hash, type, token_version, deleted
                ) VALUES (?, 'concurrent-admin', 'hash', 1, 0, 0)
                """, USER_ID);
    }

    @AfterEach
    void shutdownExecutor() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void shouldAllowOnlyOneConcurrentRotation() throws Exception {
        IssuedRefreshToken oldToken = refreshTokenService.issue(USER_ID);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Attempt> task = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                return Attempt.success(
                        refreshSessionApplicationService.refresh(
                                oldToken.token()));
            } catch (ApiException exception) {
                return Attempt.failure(exception.code());
            }
        };

        Future<Attempt> first = executor.submit(task);
        Future<Attempt> second = executor.submit(task);
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<Attempt> attempts = List.of(
                first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS));

        assertThat(attempts).filteredOn(Attempt::succeeded).hasSize(1);
        assertThat(attempts)
                .filteredOn(attempt -> !attempt.succeeded())
                .extracting(Attempt::errorCode)
                .containsExactly(ApiErrorCode.INVALID_TOKEN);
        assertThat(totalTokenCount()).isEqualTo(2);
        assertThat(activeTokenCount()).isEqualTo(1);
        assertThat(revokedTokenCount()).isEqualTo(1);

        String newRefreshToken = attempts.stream()
                .filter(Attempt::succeeded)
                .map(Attempt::result)
                .map(LoginTokenResult::refreshToken)
                .findFirst()
                .orElseThrow();
        assertThat(refreshSessionApplicationService.refresh(newRefreshToken))
                .isNotNull();
    }

    private int totalTokenCount() {
        return countTokens("");
    }

    private int activeTokenCount() {
        return countTokens(" AND revoked = 0");
    }

    private int revokedTokenCount() {
        return countTokens(" AND revoked = 1");
    }

    private int countTokens(String condition) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_refresh_token WHERE user_id = ?"
                        + condition,
                Integer.class,
                USER_ID);
    }

    private record Attempt(
            LoginTokenResult result,
            ApiErrorCode errorCode
    ) {

        static Attempt success(LoginTokenResult result) {
            return new Attempt(result, null);
        }

        static Attempt failure(ApiErrorCode errorCode) {
            return new Attempt(null, errorCode);
        }

        boolean succeeded() {
            return result != null;
        }
    }
}
