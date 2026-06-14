package com.tyb.myblog.v2.system.infrastructure.persistence;

import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfig;
import com.tyb.myblog.v2.system.domain.siteconfig.SiteConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 站点配置数据库仓储集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class DatabaseSiteConfigRepositoryTest {

    private static final LocalDateTime UPDATED_AT =
            LocalDateTime.of(2026, 1, 1, 0, 0);

    @Autowired
    private SiteConfigRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetSiteConfig() {
        jdbcTemplate.update("DELETE FROM t_site_config");
        jdbcTemplate.update("""
                INSERT INTO t_site_config (
                    id, site_title_zh, site_title_ja, site_title_en,
                    site_subtitle_zh, site_subtitle_ja, site_subtitle_en,
                    about_md_zh, about_md_ja, about_md_en,
                    logo_url, favicon_url, icp_no, spotify_playlist_id,
                    updated_at, updated_by, deleted
                ) VALUES (
                    1, 'MyBlog', 'マイブログ', 'My Blog',
                    '中文副标题', NULL, 'English subtitle',
                    '# 关于我', NULL, '# About',
                    'https://example.com/logo.png',
                    'https://example.com/favicon.ico',
                    'ICP-123', 'playlist_123',
                    '2026-01-01 00:00:00', 1001, 0
                )
                """);
    }

    @Test
    void readsActiveFixedConfiguration() {
        assertThat(repository.findActive()).contains(expected());
    }

    @Test
    @Transactional
    void readsActiveFixedConfigurationForUpdate() {
        assertThat(repository.findActiveForUpdate()).contains(expected());
    }

    @Test
    void ignoresDeletedConfiguration() {
        jdbcTemplate.update(
                "UPDATE t_site_config SET deleted = 1 WHERE id = 1");

        Optional<SiteConfig> normal = repository.findActive();

        assertThat(normal).isEmpty();
    }

    @Test
    @Transactional
    void ignoresDeletedConfigurationForUpdate() {
        jdbcTemplate.update(
                "UPDATE t_site_config SET deleted = 1 WHERE id = 1");

        Optional<SiteConfig> locked = repository.findActiveForUpdate();

        assertThat(locked).isEmpty();
    }

    @Test
    void fullyUpdatesBusinessAndAuditFieldsIncludingSqlNull() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 14, 12, 0);
        SiteConfig updated = SiteConfig.create(
                1L,
                "新标题",
                null,
                "New title",
                "新副标题",
                null,
                null,
                "# 新的关于我",
                null,
                null,
                "https://example.com/new-logo.png",
                null,
                null,
                "new_playlist",
                UPDATED_AT,
                1001L);

        assertThat(repository.update(updated, updatedAt, 2002L)).isTrue();

        assertThat(repository.findActive()).contains(
                SiteConfig.create(
                        1L,
                        "新标题",
                        null,
                        "New title",
                        "新副标题",
                        null,
                        null,
                        "# 新的关于我",
                        null,
                        null,
                        "https://example.com/new-logo.png",
                        null,
                        null,
                        "new_playlist",
                        updatedAt,
                        2002L));
    }

    @Test
    void refusesToUpdateDeletedConfiguration() {
        jdbcTemplate.update(
                "UPDATE t_site_config SET deleted = 1 WHERE id = 1");

        assertThat(repository.update(
                expected(),
                LocalDateTime.of(2026, 6, 14, 12, 0),
                2002L)).isFalse();
    }

    private SiteConfig expected() {
        return SiteConfig.create(
                1L,
                "MyBlog",
                "マイブログ",
                "My Blog",
                "中文副标题",
                null,
                "English subtitle",
                "# 关于我",
                null,
                "# About",
                "https://example.com/logo.png",
                "https://example.com/favicon.ico",
                "ICP-123",
                "playlist_123",
                UPDATED_AT,
                1001L);
    }
}
