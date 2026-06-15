package com.tyb.myblog.v2.content.application;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
import com.tyb.myblog.v2.content.application.category.CategoryCreateService;
import com.tyb.myblog.v2.content.application.category.CategoryDeleteService;
import com.tyb.myblog.v2.content.application.category.CreateCategoryCommand;
import com.tyb.myblog.v2.content.domain.category.Category;
import com.tyb.myblog.v2.content.domain.category.CategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class CategoryTagConcurrencyTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryCreateService createService;

    @Autowired
    private CategoryDeleteService deleteService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM t_article_tag");
        jdbcTemplate.update("DELETE FROM t_article");
        jdbcTemplate.update("DELETE FROM t_category");
        jdbcTemplate.update("DELETE FROM t_tag");
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @Timeout(10)
    void serializesCategoryEditAndDeleteWithDeleteAsFinalState()
            throws Exception {
        insertCategory(101L, "Backend", "backend", 10);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> edit = executor.submit(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    Category current = categoryRepository
                            .findActiveByIdForUpdate(101L)
                            .orElseThrow();
                    locked.countDown();
                    await(release);
                    categoryRepository.update(
                            current.replace(
                                    "服务端",
                                    null,
                                    null,
                                    "server",
                                    20),
                            LocalDateTime.of(
                                    2026, 6, 15, 12, 0),
                            1001L);
                }));
        assertThat(locked.await(3, TimeUnit.SECONDS)).isTrue();
        Future<?> delete = executor.submit(() ->
                deleteService.delete(admin(), 101L));
        release.countDown();

        edit.get(5, TimeUnit.SECONDS);
        delete.get(5, TimeUnit.SECONDS);
        assertThat(jdbcTemplate.queryForMap("""
                SELECT name_zh, slug, sort_order, deleted
                FROM t_category
                WHERE id = 101
                """))
                .containsEntry("NAME_ZH", "服务端")
                .containsEntry("SLUG", "server")
                .containsEntry("SORT_ORDER", 20)
                .containsEntry("DELETED", 1);
    }

    @Test
    @Timeout(10)
    void serializesCategorySortAndDeleteWithoutPartialSort()
            throws Exception {
        insertCategory(101L, "Backend", "backend", 10);
        insertCategory(102L, "Frontend", "frontend", 20);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> sort = executor.submit(() ->
                transactionTemplate.executeWithoutResult(status -> {
                    categoryRepository.findActiveByIdsForUpdate(
                            List.of(101L, 102L));
                    locked.countDown();
                    await(release);
                    LocalDateTime now = LocalDateTime.of(
                            2026, 6, 15, 12, 0);
                    categoryRepository.updateSortOrder(
                            101L, 30, now, 1001L);
                    categoryRepository.updateSortOrder(
                            102L, 40, now, 1001L);
                }));
        assertThat(locked.await(3, TimeUnit.SECONDS)).isTrue();
        Future<?> delete = executor.submit(() ->
                deleteService.delete(admin(), 101L));
        release.countDown();

        sort.get(5, TimeUnit.SECONDS);
        delete.get(5, TimeUnit.SECONDS);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT sort_order
                FROM t_category
                WHERE id = 101
                """, Integer.class)).isEqualTo(30);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT sort_order
                FROM t_category
                WHERE id = 102
                """, Integer.class)).isEqualTo(40);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT deleted
                FROM t_category
                WHERE id = 101
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    @Timeout(10)
    void allowsAtMostOneConcurrentCreateForSameSlug()
            throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        Future<Object> first = concurrentCreate(barrier, "分类甲");
        Future<Object> second = concurrentCreate(barrier, "分类乙");

        List<Object> outcomes = List.of(
                first.get(5, TimeUnit.SECONDS),
                second.get(5, TimeUnit.SECONDS));
        assertThat(outcomes.stream()
                .filter(outcome -> outcome instanceof Long)
                .count()).isEqualTo(1);
        assertThat(outcomes.stream()
                .filter(outcome -> outcome == ApiErrorCode.CONFLICT)
                .count()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM t_category
                WHERE slug = 'same-slug'
                """, Integer.class)).isEqualTo(1);
    }

    private Future<Object> concurrentCreate(
            CyclicBarrier barrier,
            String name) {
        return executor.submit(() -> {
            barrier.await(3, TimeUnit.SECONDS);
            try {
                return createService.create(
                        admin(),
                        new CreateCategoryCommand(
                                name,
                                null,
                                null,
                                "same-slug",
                                10))
                        .id();
            } catch (ApiException exception) {
                return exception.code();
            }
        });
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("并发测试等待超时");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "并发测试被中断",
                    exception);
        }
    }

    private AuthenticatedPrincipal admin() {
        return new AuthenticatedPrincipal(
                "1001", "admin", List.of("ADMIN"));
    }

    private void insertCategory(
            long id,
            String nameZh,
            String slug,
            int sortOrder) {
        jdbcTemplate.update("""
                INSERT INTO t_category (
                    id, name_zh, name_ja, name_en, slug, sort_order,
                    created_at, created_by, updated_at, updated_by,
                    deleted, deleted_at, deleted_by
                ) VALUES (?, ?, NULL, NULL, ?, ?,
                    '2026-06-15 10:00:00', 1001,
                    '2026-06-15 10:00:00', 1001,
                    0, NULL, NULL)
                """,
                id,
                nameZh,
                slug,
                sortOrder);
    }
}
