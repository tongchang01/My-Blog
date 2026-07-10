package com.tyb.myblog.v2.identity.application.profile;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.identity.domain.profile.UserProfile;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import com.tyb.myblog.v2.identity.infrastructure.persistence.repository.MyBatisUserProfileRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
 * 当前用户资料部分更新的事务与行锁并发集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@Import(CurrentUserProfileUpdateConcurrencyTest.ConcurrencyConfiguration.class)
class CurrentUserProfileUpdateConcurrencyTest {

    private static final long USER_ID = 7101L;

    @Autowired
    private CurrentUserProfileUpdateService service;

    @Autowired
    private CoordinatedUserProfileRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void prepareProfile() {
        executor = Executors.newFixedThreadPool(2);
        repository.reset();
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type, deleted
                        ) VALUES (?, 'concurrent-profile', 'hash', 1, 0)
                        """,
                USER_ID);
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_info (
                            user_id, nickname, location, deleted
                        ) VALUES (?, 'Original Name', 'Original Location', 0)
                        """,
                USER_ID);
    }

    @AfterEach
    void shutdownExecutor() throws InterruptedException {
        repository.releaseFirstUpdate();
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSerializeConcurrentPatchesAndPreserveBothFieldChanges() throws Exception {
        assertThat(AopUtils.isAopProxy(service)).isTrue();

        Future<UserProfileResult> nicknameUpdate = executor.submit(() -> {
            authenticate();
            try {
                return service.update(
                        principal(),
                        command(PatchValue.of("Updated Name"), PatchValue.absent()));
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
        assertThat(repository.awaitFirstLock()).isTrue();

        Future<UserProfileResult> locationUpdate = executor.submit(() -> {
            authenticate();
            try {
                return service.update(
                        principal(),
                        command(PatchValue.absent(), PatchValue.of("Updated Location")));
            } finally {
                SecurityContextHolder.clearContext();
            }
        });
        assertThat(repository.awaitSecondAttempt()).isTrue();

        boolean secondReadReturnedBeforeFirstTransactionReleased =
                repository.awaitSecondReadBeforeRelease();
        repository.releaseFirstUpdate();

        nicknameUpdate.get(10, TimeUnit.SECONDS);
        locationUpdate.get(10, TimeUnit.SECONDS);

        assertThat(secondReadReturnedBeforeFirstTransactionReleased).isFalse();
        assertThat(jdbcTemplate.queryForMap(
                """
                        SELECT nickname, location
                        FROM t_user_info
                        WHERE user_id = ?
                        """,
                USER_ID))
                .containsEntry("NICKNAME", "Updated Name")
                .containsEntry("LOCATION", "Updated Location");
    }

    private UpdateCurrentUserProfileCommand command(
            PatchValue<String> nickname,
            PatchValue<String> location) {
        PatchValue<String> absent = PatchValue.absent();
        return new UpdateCurrentUserProfileCommand(
                nickname, absent, absent, absent, absent, location, absent,
                absent, absent, absent, absent, absent, absent, absent);
    }

    private AuthenticatedPrincipal principal() {
        return new AuthenticatedPrincipal(
                Long.toString(USER_ID),
                "concurrent-profile",
                List.of("ADMIN"));
    }

    private void authenticate() {
        AuthenticatedPrincipal principal = principal();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of()));
    }

    /**
     * 为测试协调真实仓储调用，不替换 MyBatis 查询和更新。
     */
    static final class CoordinatedUserProfileRepository
            implements UserProfileRepository {

        private final UserProfileRepository delegate;
        private final AtomicInteger lockQueries = new AtomicInteger();
        private volatile CountDownLatch firstLockAcquired;
        private volatile CountDownLatch secondAttempted;
        private volatile CountDownLatch secondReadReturned;
        private volatile CountDownLatch allowFirstUpdate;

        private CoordinatedUserProfileRepository(
                UserProfileRepository delegate) {
            this.delegate = delegate;
            reset();
        }

        void reset() {
            lockQueries.set(0);
            firstLockAcquired = new CountDownLatch(1);
            secondAttempted = new CountDownLatch(1);
            secondReadReturned = new CountDownLatch(1);
            allowFirstUpdate = new CountDownLatch(1);
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

        void releaseFirstUpdate() {
            allowFirstUpdate.countDown();
        }

        @Override
        public Optional<UserProfile> findActiveByUserId(long userId) {
            return delegate.findActiveByUserId(userId);
        }

        @Override
        public Optional<UserProfile> findPrimaryPublicAuthor(LocalDateTime now) {
            return delegate.findPrimaryPublicAuthor(now);
        }

        @Override
        public Optional<UserProfile> findActiveByUserIdForUpdate(long userId) {
            int invocation = lockQueries.incrementAndGet();
            if (invocation == 2) {
                secondAttempted.countDown();
            }

            Optional<UserProfile> profile =
                    delegate.findActiveByUserIdForUpdate(userId);
            if (invocation == 1) {
                firstLockAcquired.countDown();
                awaitFirstRelease();
            } else if (invocation == 2) {
                secondReadReturned.countDown();
            }
            return profile;
        }

        @Override
        public void insert(UserProfile profile) {
            delegate.insert(profile);
        }

        @Override
        public boolean update(UserProfile profile) {
            return delegate.update(profile);
        }

        private void awaitFirstRelease() {
            try {
                if (!allowFirstUpdate.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("等待释放第一笔资料更新超时");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待释放第一笔资料更新被中断", exception);
            }
        }
    }

    @TestConfiguration
    static class ConcurrencyConfiguration {

        @Bean
        @Primary
        CoordinatedUserProfileRepository coordinatedUserProfileRepository(
                MyBatisUserProfileRepository delegate) {
            return new CoordinatedUserProfileRepository(delegate);
        }
    }
}
