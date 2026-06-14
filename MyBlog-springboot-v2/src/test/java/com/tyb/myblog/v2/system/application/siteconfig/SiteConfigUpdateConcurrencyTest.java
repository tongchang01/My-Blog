package com.tyb.myblog.v2.system.application.siteconfig;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfigRepository;
import com.tyb.myblog.v2.system.infrastructure.persistence.repository.MyBatisSiteConfigRepository;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 站点配置全量更新的事务与行锁并发集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@Import(SiteConfigUpdateConcurrencyTest.ConcurrencyConfiguration.class)
class SiteConfigUpdateConcurrencyTest {

    @Autowired
    private SiteConfigUpdateService service;

    @Autowired
    private CoordinatedSiteConfigRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ExecutorService executor;

    @BeforeEach
    void prepareConfiguration() {
        executor = Executors.newFixedThreadPool(2);
        repository.reset();
        jdbcTemplate.update("DELETE FROM t_site_config");
        jdbcTemplate.update("""
                INSERT INTO t_site_config (
                    id, site_title_zh, about_md_zh, deleted
                ) VALUES (1, 'Original', '# Original', 0)
                """);
    }

    @AfterEach
    void shutdownExecutor() throws InterruptedException {
        repository.releaseFirstUpdate();
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void serializesConcurrentFullUpdatesWithoutMixedFields()
            throws Exception {
        assertThat(AopUtils.isAopProxy(service)).isTrue();

        Future<AdminSiteConfigResult> first = executor.submit(() ->
                service.update(principal(1001L), command("First")));
        assertThat(repository.awaitFirstLock()).isTrue();

        Future<AdminSiteConfigResult> second = executor.submit(() ->
                service.update(principal(1002L), command("Second")));
        assertThat(repository.awaitSecondAttempt()).isTrue();

        boolean secondReadBeforeFirstReleased =
                repository.awaitSecondReadBeforeRelease();
        repository.releaseFirstUpdate();

        first.get(10, TimeUnit.SECONDS);
        second.get(10, TimeUnit.SECONDS);

        assertThat(secondReadBeforeFirstReleased).isFalse();
        assertThat(repository.findActive()).contains(
                SiteConfig.create(
                        1L,
                        "Second title",
                        "Second ja title",
                        "Second en title",
                        "Second subtitle",
                        "Second ja subtitle",
                        "Second en subtitle",
                        "# Second zh",
                        "# Second ja",
                        "# Second en",
                        "https://example.com/Second-logo.png",
                        "https://example.com/Second-favicon.ico",
                        "Second-icp",
                        "Second_playlist",
                        repository.findActive().orElseThrow().updatedAt(),
                        1002L));
    }

    private UpdateSiteConfigCommand command(String prefix) {
        return new UpdateSiteConfigCommand(
                prefix + " title",
                prefix + " ja title",
                prefix + " en title",
                prefix + " subtitle",
                prefix + " ja subtitle",
                prefix + " en subtitle",
                "# " + prefix + " zh",
                "# " + prefix + " ja",
                "# " + prefix + " en",
                "https://example.com/" + prefix + "-logo.png",
                "https://example.com/" + prefix + "-favicon.ico",
                prefix + "-icp",
                prefix + "_playlist");
    }

    private AuthenticatedPrincipal principal(long id) {
        return new AuthenticatedPrincipal(
                Long.toString(id),
                "admin-" + id,
                List.of("ADMIN"));
    }

    /**
     * 协调真实仓储调用，以观察第二个事务是否被行锁阻塞。
     */
    static final class CoordinatedSiteConfigRepository
            implements SiteConfigRepository {

        private final SiteConfigRepository delegate;
        private final AtomicInteger lockQueries = new AtomicInteger();
        private volatile CountDownLatch firstLockAcquired;
        private volatile CountDownLatch secondAttempted;
        private volatile CountDownLatch secondReadReturned;
        private volatile CountDownLatch allowFirstUpdate;

        private CoordinatedSiteConfigRepository(SiteConfigRepository delegate) {
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
        public Optional<SiteConfig> findActive() {
            return delegate.findActive();
        }

        @Override
        public Optional<SiteConfig> findActiveForUpdate() {
            int invocation = lockQueries.incrementAndGet();
            if (invocation == 2) {
                secondAttempted.countDown();
            }

            Optional<SiteConfig> config = delegate.findActiveForUpdate();
            if (invocation == 1) {
                firstLockAcquired.countDown();
                awaitFirstRelease();
            } else if (invocation == 2) {
                secondReadReturned.countDown();
            }
            return config;
        }

        @Override
        public boolean update(
                SiteConfig config,
                LocalDateTime updatedAt,
                Long updatedBy) {
            return delegate.update(config, updatedAt, updatedBy);
        }

        private void awaitFirstRelease() {
            try {
                if (!allowFirstUpdate.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("等待释放第一笔站点配置更新超时");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "等待释放第一笔站点配置更新被中断",
                        exception);
            }
        }
    }

    @TestConfiguration
    static class ConcurrencyConfiguration {

        @Bean
        @Primary
        CoordinatedSiteConfigRepository coordinatedSiteConfigRepository(
                MyBatisSiteConfigRepository delegate) {
            return new CoordinatedSiteConfigRepository(delegate);
        }
    }
}
