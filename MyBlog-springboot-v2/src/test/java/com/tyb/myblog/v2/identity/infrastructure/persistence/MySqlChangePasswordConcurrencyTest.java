package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.application.auth.ChangePasswordApplicationService;
import com.tyb.myblog.v2.identity.application.auth.ChangePasswordCommand;
import com.tyb.myblog.v2.identity.application.auth.LoginTokenResult;
import com.tyb.myblog.v2.identity.application.auth.RefreshSessionTransactionService;
import com.tyb.myblog.v2.identity.application.token.IssuedRefreshToken;
import com.tyb.myblog.v2.identity.application.token.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL 下修改密码行锁与 refresh 并发集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MySqlChangePasswordConcurrencyTest {

    private static final long USER_ID = 8301L;
    private static final String OLD_PASSWORD = "old-password";

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("myblog_v2_password_test")
                    .withUsername("myblog")
                    .withPassword("myblog-test-password");

    @Autowired
    private ChangePasswordApplicationService changePasswordService;

    @Autowired
    private RefreshSessionTransactionService refreshSessionService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @BeforeEach
    void prepareAccount() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type,
                            token_version, deleted
                        ) VALUES (?, 'mysql-password', ?, 1, 3, 0)
                        """,
                USER_ID,
                passwordEncoder.encode(OLD_PASSWORD));
    }

    @Test
    void allowsOnlyOneConcurrentChangeUsingTheSameOldPassword()
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ApiErrorCode> first = executor.submit(() ->
                    changeWhenStarted(
                            "first-password",
                            ready,
                            start));
            Future<ApiErrorCode> second = executor.submit(() ->
                    changeWhenStarted(
                            "second-password",
                            ready,
                            start));

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<ApiErrorCode> results = Arrays.asList(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS));

            assertThat(results).containsExactlyInAnyOrder(
                    null,
                    ApiErrorCode.BAD_CREDENTIALS);
            assertThat(currentTokenVersion()).isEqualTo(4);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void completesConcurrentChangeAndRefreshWithoutUnboundedWait()
            throws Exception {
        IssuedRefreshToken refreshToken =
                refreshTokenService.issue(USER_ID);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ApiErrorCode> change = executor.submit(() -> {
                start.await();
                return change("changed-password");
            });
            Future<Optional<LoginTokenResult>> refresh =
                    executor.submit(() -> {
                        start.await();
                        return refreshSessionService.refresh(
                                refreshToken.token());
                    });

            start.countDown();
            assertThat(change.get(10, TimeUnit.SECONDS)).isNull();
            assertThat(refresh.get(10, TimeUnit.SECONDS)).isNotNull();
            assertThat(currentTokenVersion()).isEqualTo(4);
        } finally {
            executor.shutdownNow();
        }
    }

    private ApiErrorCode changeWhenStarted(
            String newPassword,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return change(newPassword);
    }

    private ApiErrorCode change(String newPassword) {
        try {
            changePasswordService.change(
                    principal(),
                    new ChangePasswordCommand(
                            OLD_PASSWORD,
                            newPassword));
            return null;
        } catch (ApiException exception) {
            return exception.code();
        }
    }

    private AuthenticatedPrincipal principal() {
        return new AuthenticatedPrincipal(
                Long.toString(USER_ID),
                "mysql-password",
                List.of("ADMIN"));
    }

    private int currentTokenVersion() {
        return jdbcTemplate.queryForObject(
                "SELECT token_version FROM t_user_auth WHERE id = ?",
                Integer.class,
                USER_ID);
    }
}
