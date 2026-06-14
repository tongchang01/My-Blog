package com.tyb.myblog.v2.system.application.friendlink;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLink;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkPage;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkRepository;
import com.tyb.myblog.v2.system.domain.friendlink.FriendLinkStatus;
import com.tyb.myblog.v2.system.domain.friendlink.NewFriendLink;
import com.tyb.myblog.v2.system.infrastructure.persistence.repository.MyBatisFriendLinkRepository;
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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 友链更新、排序与软删除的事务行锁测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@Import(FriendLinkConcurrencyTest.ConcurrencyConfiguration.class)
class FriendLinkConcurrencyTest {

    @Autowired
    private FriendLinkUpdateService updateService;

    @Autowired
    private FriendLinkSortService sortService;

    @Autowired
    private FriendLinkDeleteService deleteService;

    @Autowired
    private CoordinatedFriendLinkRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        repository.reset();
        jdbcTemplate.update("DELETE FROM t_friend_link");
        insert(10L, 10);
        insert(20L, 20);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        repository.release();
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void updateCommitsBeforeWaitingDeleteSoftDeletesRow()
            throws Exception {
        assertThat(AopUtils.isAopProxy(updateService)).isTrue();
        assertThat(AopUtils.isAopProxy(deleteService)).isTrue();
        repository.blockFirstSingleLock();

        Future<FriendLinkResult> update = executor.submit(() ->
                updateService.update(
                        principal(),
                        10L,
                        new UpdateFriendLinkCommand(
                                "Updated",
                                "https://updated.example.com",
                                null,
                                null,
                                30,
                                FriendLinkStatus.VISIBLE)));
        assertThat(repository.awaitFirstLock()).isTrue();

        Future<?> delete = executor.submit(() -> {
            deleteService.delete(principal(), 10L);
            return null;
        });
        assertThat(repository.awaitSecondAttempt()).isTrue();
        assertThat(repository.awaitSecondReturnBeforeRelease())
                .isFalse();

        repository.release();
        update.get(10, TimeUnit.SECONDS);
        delete.get(10, TimeUnit.SECONDS);

        assertThat(deleted(10L)).isEqualTo(1);
    }

    @Test
    void updateReturnsNotFoundAfterDeleteCompleted() {
        deleteService.delete(principal(), 10L);

        assertThatThrownBy(() -> updateService.update(
                principal(),
                10L,
                new UpdateFriendLinkCommand(
                        "Updated",
                        "https://updated.example.com",
                        null,
                        null,
                        30,
                        FriendLinkStatus.VISIBLE)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        exception -> assertThat(exception.code())
                                .isEqualTo(ApiErrorCode.NOT_FOUND));
    }

    @Test
    void sortCommitsBeforeWaitingDeleteSoftDeletesTarget()
            throws Exception {
        assertThat(AopUtils.isAopProxy(sortService)).isTrue();
        repository.blockFirstBatchLock();

        Future<List<FriendLinkResult>> sort = executor.submit(() ->
                sortService.update(
                        principal(),
                        new UpdateFriendLinkSortOrdersCommand(List.of(
                                new FriendLinkSortItem(20L, 1),
                                new FriendLinkSortItem(10L, 2)))));
        assertThat(repository.awaitFirstLock()).isTrue();

        Future<?> delete = executor.submit(() -> {
            deleteService.delete(principal(), 20L);
            return null;
        });
        assertThat(repository.awaitSecondAttempt()).isTrue();
        assertThat(repository.awaitSecondReturnBeforeRelease())
                .isFalse();

        repository.release();
        sort.get(10, TimeUnit.SECONDS);
        delete.get(10, TimeUnit.SECONDS);

        assertThat(deleted(20L)).isEqualTo(1);
        assertThat(sortOrder(10L)).isEqualTo(2);
    }

    private AuthenticatedPrincipal principal() {
        return new AuthenticatedPrincipal(
                "1001", "admin", List.of("ADMIN"));
    }

    private void insert(long id, int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO t_friend_link (
                    id, name, url, sort_order, status,
                    created_at, created_by, updated_at, updated_by,
                    deleted
                ) VALUES (?, ?, ?, ?, 1,
                    CURRENT_TIMESTAMP, 1001,
                    CURRENT_TIMESTAMP, 1001, 0)
                """,
                id,
                "Link " + id,
                "https://example.com/" + id,
                sortOrder);
    }

    private int deleted(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT deleted FROM t_friend_link WHERE id = ?",
                Integer.class,
                id);
    }

    private int sortOrder(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT sort_order FROM t_friend_link WHERE id = ?",
                Integer.class,
                id);
    }

    /**
     * 在真实仓储取得数据库锁后暂停第一个事务，以观察第二个事务是否等待。
     */
    static final class CoordinatedFriendLinkRepository
            implements FriendLinkRepository {

        private enum Mode {
            NONE,
            SINGLE,
            BATCH
        }

        private final FriendLinkRepository delegate;
        private volatile Mode mode;
        private volatile CountDownLatch firstLock;
        private volatile CountDownLatch secondAttempt;
        private volatile CountDownLatch secondReturn;
        private volatile CountDownLatch release;

        private CoordinatedFriendLinkRepository(
                FriendLinkRepository delegate) {
            this.delegate = delegate;
            reset();
        }

        void reset() {
            mode = Mode.NONE;
            firstLock = new CountDownLatch(1);
            secondAttempt = new CountDownLatch(1);
            secondReturn = new CountDownLatch(1);
            release = new CountDownLatch(1);
        }

        void blockFirstSingleLock() {
            mode = Mode.SINGLE;
        }

        void blockFirstBatchLock() {
            mode = Mode.BATCH;
        }

        boolean awaitFirstLock() throws InterruptedException {
            return firstLock.await(5, TimeUnit.SECONDS);
        }

        boolean awaitSecondAttempt() throws InterruptedException {
            return secondAttempt.await(5, TimeUnit.SECONDS);
        }

        boolean awaitSecondReturnBeforeRelease()
                throws InterruptedException {
            return secondReturn.await(500, TimeUnit.MILLISECONDS);
        }

        void release() {
            release.countDown();
        }

        @Override
        public List<FriendLink> findPublicVisible() {
            return delegate.findPublicVisible();
        }

        @Override
        public FriendLinkPage findActivePage(int page, int size) {
            return delegate.findActivePage(page, size);
        }

        @Override
        public Optional<FriendLink> findActiveById(long id) {
            return delegate.findActiveById(id);
        }

        @Override
        public Optional<FriendLink> findActiveByIdForUpdate(long id) {
            if (mode != Mode.NONE && firstLock.getCount() == 0) {
                secondAttempt.countDown();
            }
            Optional<FriendLink> result =
                    delegate.findActiveByIdForUpdate(id);
            if (mode == Mode.SINGLE && firstLock.getCount() > 0) {
                firstLock.countDown();
                awaitRelease();
            } else if (mode != Mode.NONE) {
                secondReturn.countDown();
            }
            return result;
        }

        @Override
        public List<FriendLink> findActiveByIdsForUpdate(
                List<Long> ids) {
            List<FriendLink> result =
                    delegate.findActiveByIdsForUpdate(ids);
            if (mode == Mode.BATCH && firstLock.getCount() > 0) {
                firstLock.countDown();
                awaitRelease();
            }
            return result;
        }

        @Override
        public FriendLink insert(NewFriendLink friendLink) {
            return delegate.insert(friendLink);
        }

        @Override
        public boolean update(
                FriendLink friendLink,
                LocalDateTime updatedAt,
                long updatedBy) {
            return delegate.update(friendLink, updatedAt, updatedBy);
        }

        @Override
        public boolean updateStatus(
                long id,
                FriendLinkStatus status,
                LocalDateTime updatedAt,
                long updatedBy) {
            return delegate.updateStatus(
                    id, status, updatedAt, updatedBy);
        }

        @Override
        public boolean updateSortOrder(
                long id,
                int sortOrder,
                LocalDateTime updatedAt,
                long updatedBy) {
            return delegate.updateSortOrder(
                    id, sortOrder, updatedAt, updatedBy);
        }

        @Override
        public boolean softDelete(
                long id,
                LocalDateTime deletedAt,
                long deletedBy) {
            return delegate.softDelete(id, deletedAt, deletedBy);
        }

        private void awaitRelease() {
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "等待释放友链写事务超时");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "等待释放友链写事务被中断",
                        exception);
            }
        }
    }

    @TestConfiguration
    static class ConcurrencyConfiguration {

        @Bean
        @Primary
        CoordinatedFriendLinkRepository coordinatedFriendLinkRepository(
                MyBatisFriendLinkRepository delegate) {
            return new CoordinatedFriendLinkRepository(delegate);
        }
    }
}
