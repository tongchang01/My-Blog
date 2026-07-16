package com.tyb.myblog.v2.content.infrastructure.persistence;

import com.tyb.myblog.v2.content.domain.article.ArticleRepository;
import com.tyb.myblog.v2.content.domain.article.HomepageSlot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MySqlHomepageSlotConcurrencyTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("myblog_v2_homepage_slot_test")
                    .withUsername("myblog")
                    .withPassword("myblog-test-password");

    @Autowired
    private ArticleRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void serializesConcurrentPinnedClaims() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            jdbcTemplate.update("DELETE FROM t_article");
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<?> first = claim(executor, ready, start, 9101L);
                Future<?> second = claim(executor, ready, start, 9102L);
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                first.get(10, TimeUnit.SECONDS);
                second.get(10, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }
            assertThat(repository.countActiveHomepageSlot(
                    HomepageSlot.PINNED, null)).isEqualTo(1);
        }
    }

    private Future<?> claim(
            ExecutorService executor,
            CountDownLatch ready,
            CountDownLatch start,
            long articleId) {
        return executor.submit(() -> {
            ready.countDown();
            start.await();
            new TransactionTemplate(transactionManager).executeWithoutResult(
                    ignored -> {
                        repository.lockHomepageSlot(HomepageSlot.PINNED);
                        if (repository.countActiveHomepageSlot(
                                HomepageSlot.PINNED, null) == 0) {
                            insertPinnedArticle(articleId);
                        }
                    });
            return null;
        });
    }

    private void insertPinnedArticle(long id) {
        jdbcTemplate.update("""
                INSERT INTO t_article (
                    id, title_zh, body, author_id, slug, status,
                    publish_at, homepage_slot, created_at, updated_at, deleted
                ) VALUES (?, ?, 'body', 1001, ?, 2,
                    '2026-07-16 10:00:00', 'PINNED',
                    '2026-07-16 10:00:00', '2026-07-16 10:00:00', 0)
                """, id, "article-" + id, "article-" + id);
    }
}
