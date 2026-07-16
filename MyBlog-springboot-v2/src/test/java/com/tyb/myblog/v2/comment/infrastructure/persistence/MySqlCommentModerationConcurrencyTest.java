package com.tyb.myblog.v2.comment.infrastructure.persistence;

import com.tyb.myblog.v2.comment.application.AdminCommentCommandService;
import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.common.error.ApiErrorCode;
import com.tyb.myblog.v2.common.error.ApiException;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MySqlCommentModerationConcurrencyTest {

    private static final long ARTICLE_ID = 8401L;
    private static final long COMMENT_ID = 8402L;

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("myblog_v2_comment_test")
                    .withUsername("myblog")
                    .withPassword("myblog-test-password");

    @Autowired
    private AdminCommentCommandService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void keepsCountConsistentDuringConcurrentApproveAndHide()
            throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            prepareComment(2, 0);
            runConcurrently(
                    () -> service.approve(admin(), COMMENT_ID),
                    () -> service.hide(admin(), COMMENT_ID),
                    false);
            assertCountMatchesVisibility();
        }
    }

    @Test
    void keepsCountConsistentDuringConcurrentHideAndDelete()
            throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            prepareComment(1, 1);
            runConcurrently(
                    () -> service.hide(admin(), COMMENT_ID),
                    () -> service.delete(admin(), COMMENT_ID),
                    true);
            assertCountMatchesVisibility();
        }
    }

    private void prepareComment(int auditStatus, int commentCount) {
        jdbcTemplate.update("DELETE FROM t_comment");
        jdbcTemplate.update("DELETE FROM t_article WHERE id = ?", ARTICLE_ID);
        jdbcTemplate.update("""
                INSERT INTO t_article (
                    id, author_id, status, publish_at, comment_count,
                    created_at, updated_at, deleted
                ) VALUES (?, 1001, 2, '2026-07-16 10:00:00', ?,
                    '2026-07-16 10:00:00', '2026-07-16 10:00:00', 0)
                """, ARTICLE_ID, commentCount);
        jdbcTemplate.update("""
                INSERT INTO t_comment (
                    id, target_type, target_id, author_nickname,
                    author_email, content_md, content_html, audit_status,
                    created_at, updated_at, deleted
                ) VALUES (?, 1, ?, 'TYB', 'tyb@example.com',
                    'hello', '<p>hello</p>', ?,
                    '2026-07-16 10:00:00', '2026-07-16 10:00:00', 0)
                """, COMMENT_ID, ARTICLE_ID, auditStatus);
    }

    private void runConcurrently(
            Runnable left,
            Runnable right,
            boolean allowNotFound) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> leftFuture = submit(executor, ready, start, left);
            Future<?> rightFuture = submit(executor, ready, start, right);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            await(leftFuture, allowNotFound);
            await(rightFuture, allowNotFound);
        } finally {
            executor.shutdownNow();
        }
    }

    private Future<?> submit(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            Runnable action) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            action.run();
            return null;
        });
    }

    private void await(Future<?> future, boolean allowNotFound)
            throws Exception {
        try {
            future.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            if (!allowNotFound
                    || !(exception.getCause() instanceof ApiException apiException)
                    || apiException.code() != ApiErrorCode.NOT_FOUND) {
                throw exception;
            }
        }
    }

    private void assertCountMatchesVisibility() {
        Map<String, Object> state = jdbcTemplate.queryForMap("""
                SELECT c.audit_status, c.deleted, a.comment_count
                FROM t_comment c
                INNER JOIN t_article a ON a.id = c.target_id
                WHERE c.id = ?
                """, COMMENT_ID);
        boolean visible = ((Number) state.get("audit_status")).intValue() == 1
                && ((Number) state.get("deleted")).intValue() == 0;
        assertThat(((Number) state.get("comment_count")).intValue())
                .isEqualTo(visible ? 1 : 0);
    }

    private AuthenticatedPrincipal admin() {
        return new AuthenticatedPrincipal(
                "1001", "admin", List.of("ADMIN"));
    }
}
