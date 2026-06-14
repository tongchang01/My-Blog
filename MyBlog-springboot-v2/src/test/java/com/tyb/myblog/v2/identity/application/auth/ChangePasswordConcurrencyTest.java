package com.tyb.myblog.v2.identity.application.auth;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.identity.domain.account.ChangeablePasswordAccount;
import com.tyb.myblog.v2.identity.domain.account.PasswordAccountRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.repository.MyBatisPasswordAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 修改密码事务与账号行锁的 H2 并发集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@Import(ChangePasswordConcurrencyTest.ConcurrencyConfiguration.class)
class ChangePasswordConcurrencyTest {

    private static final long USER_ID = 8201L;
    private static final String OLD_PASSWORD = "old-password";
    private static final String FIRST_NEW_PASSWORD = "first-password";
    private static final String SECOND_NEW_PASSWORD = "second-password";

    @Autowired
    private ChangePasswordApplicationService service;

    @Autowired
    private CoordinatedPasswordAccountRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void prepareAccount() {
        executor = Executors.newFixedThreadPool(2);
        repository.reset();
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type,
                            token_version, deleted
                        ) VALUES (?, 'concurrent-password', ?, 1, 3, 0)
                        """,
                USER_ID,
                passwordEncoder.encode(OLD_PASSWORD));
    }

    @AfterEach
    void shutdownExecutor() throws InterruptedException {
        repository.releaseFirstTransaction();
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void serializesConcurrentChangesUsingTheSameOldPassword()
            throws Exception {
        assertThat(AopUtils.isAopProxy(service)).isTrue();

        Future<ApiErrorCode> first = executor.submit(
                () -> change(FIRST_NEW_PASSWORD));
        assertThat(repository.awaitFirstLock()).isTrue();

        Future<ApiErrorCode> second = executor.submit(
                () -> change(SECOND_NEW_PASSWORD));
        assertThat(repository.awaitSecondAttempt()).isTrue();

        boolean secondReadReturnedBeforeFirstTransactionReleased =
                repository.awaitSecondReadBeforeRelease();
        repository.releaseFirstTransaction();

        assertThat(first.get(10, TimeUnit.SECONDS)).isNull();
        assertThat(second.get(10, TimeUnit.SECONDS))
                .isEqualTo(ApiErrorCode.BAD_CREDENTIALS);
        assertThat(secondReadReturnedBeforeFirstTransactionReleased)
                .isFalse();
        assertThat(currentTokenVersion()).isEqualTo(4);
        assertThat(passwordEncoder.matches(
                FIRST_NEW_PASSWORD,
                currentPasswordHash())).isTrue();
    }

    private ApiErrorCode change(String newPassword) {
        try {
            service.change(
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
                "concurrent-password",
                List.of("ADMIN"));
    }

    private int currentTokenVersion() {
        return jdbcTemplate.queryForObject(
                "SELECT token_version FROM t_user_auth WHERE id = ?",
                Integer.class,
                USER_ID);
    }

    private String currentPasswordHash() {
        return jdbcTemplate.queryForObject(
                "SELECT password_hash FROM t_user_auth WHERE id = ?",
                String.class,
                USER_ID);
    }

    /**
     * 协调真实加锁查询的测试仓储，不替换 MyBatis SQL。
     */
    static final class CoordinatedPasswordAccountRepository
            implements PasswordAccountRepository {

        private final PasswordAccountRepository delegate;
        private final AtomicInteger lockQueries = new AtomicInteger();
        private volatile CountDownLatch firstLockAcquired;
        private volatile CountDownLatch secondAttempted;
        private volatile CountDownLatch secondReadReturned;
        private volatile CountDownLatch allowFirstTransaction;

        private CoordinatedPasswordAccountRepository(
                PasswordAccountRepository delegate) {
            this.delegate = delegate;
            reset();
        }

        void reset() {
            lockQueries.set(0);
            firstLockAcquired = new CountDownLatch(1);
            secondAttempted = new CountDownLatch(1);
            secondReadReturned = new CountDownLatch(1);
            allowFirstTransaction = new CountDownLatch(1);
        }

        boolean awaitFirstLock() throws InterruptedException {
            return firstLockAcquired.await(5, TimeUnit.SECONDS);
        }

        boolean awaitSecondAttempt() throws InterruptedException {
            return secondAttempted.await(5, TimeUnit.SECONDS);
        }

        boolean awaitSecondReadBeforeRelease() throws InterruptedException {
            return secondReadReturned.await(500, TimeUnit.MILLISECONDS);
        }

        void releaseFirstTransaction() {
            allowFirstTransaction.countDown();
        }

        @Override
        public Optional<ChangeablePasswordAccount> findActiveByIdForUpdate(
                long userId) {
            int invocation = lockQueries.incrementAndGet();
            if (invocation == 2) {
                secondAttempted.countDown();
            }

            Optional<ChangeablePasswordAccount> account =
                    delegate.findActiveByIdForUpdate(userId);
            if (invocation == 1) {
                firstLockAcquired.countDown();
                awaitRelease();
            } else if (invocation == 2) {
                secondReadReturned.countDown();
            }
            return account;
        }

        @Override
        public boolean updatePasswordAndIncrementTokenVersion(
                long userId,
                String passwordHash,
                LocalDateTime updatedAt,
                Long updatedBy) {
            return delegate.updatePasswordAndIncrementTokenVersion(
                    userId,
                    passwordHash,
                    updatedAt,
                    updatedBy);
        }

        private void awaitRelease() {
            try {
                if (!allowFirstTransaction.await(
                        10,
                        TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "等待释放第一笔改密事务超时");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "等待释放第一笔改密事务被中断",
                        exception);
            }
        }
    }

    @TestConfiguration
    static class ConcurrencyConfiguration {

        @Bean
        @Primary
        CoordinatedPasswordAccountRepository
                coordinatedPasswordAccountRepository(
                MyBatisPasswordAccountRepository delegate) {
            return new CoordinatedPasswordAccountRepository(delegate);
        }
    }
}
