package com.tyb.myblog.v2.identity.infrastructure.persistence;

import com.tyb.myblog.v2.common.auth.AuthenticatedPrincipal;
import com.tyb.myblog.v2.identity.domain.profile.UserProfile;
import com.tyb.myblog.v2.identity.domain.profile.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户资料数据库仓储集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
class DatabaseUserProfileRepositoryTest {

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        SecurityContextHolder.clearContext();
        jdbcTemplate.update("DELETE FROM t_article");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReadActiveProfile() {
        insertAccount(1001L, "admin");
        insertProfile(1001L, 0);

        Optional<UserProfile> result = repository.findActiveByUserId(1001L);

        assertThat(result).contains(UserProfile.create(
                1001L,
                "TYB",
                "https://example.com/avatar.png",
                "中文简介",
                "日本語紹介",
                "English bio",
                "Tokyo",
                "https://example.com",
                "public@example.com",
                "https://github.com/tyb",
                "https://x.com/tyb",
                "https://linkedin.com/in/tyb",
                "https://zhihu.com/people/tyb",
                "https://qiita.com/tyb",
                "https://juejin.cn/user/tyb"));
    }

    @Test
    void shouldIgnoreDeletedProfile() {
        insertAccount(1002L, "deleted-profile");
        insertProfile(1002L, 1);

        assertThat(repository.findActiveByUserId(1002L)).isEmpty();
    }

    @Test
    void shouldReadPrimaryPublicAuthorFromPublishedArticles() {
        insertAccount(1007L, "minor-author");
        insertProfile(1007L, 0);
        insertAccount(1008L, "primary-author");
        insertProfile(1008L, 0);
        insertArticle(2001L, 1007L, 2, LocalDateTime.of(2026, 7, 1, 10, 0), 0);
        insertArticle(2002L, 1008L, 2, LocalDateTime.of(2026, 7, 2, 10, 0), 0);
        insertArticle(2003L, 1008L, 2, LocalDateTime.of(2026, 7, 3, 10, 0), 0);
        insertArticle(2004L, 1008L, 5, LocalDateTime.of(2026, 7, 20, 10, 0), 0);
        insertArticle(2005L, 1008L, 2, LocalDateTime.of(2026, 7, 4, 10, 0), 1);

        Optional<UserProfile> result =
                repository.findPrimaryPublicAuthor(LocalDateTime.of(2026, 7, 10, 0, 0));

        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(1008L);
        assertThat(result.get().githubUrl()).isEqualTo("https://github.com/tyb");
        assertThat(result.get().zhihuUrl()).isEqualTo("https://zhihu.com/people/tyb");
        assertThat(result.get().juejinUrl()).isEqualTo("https://juejin.cn/user/tyb");
    }

    @Test
    void shouldInsertProfileWithAuditColumns() {
        insertAccount(1003L, "new-profile");
        authenticate(1003L);
        UserProfile profile = UserProfile.create(
                1003L,
                " New Profile ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        repository.insert(profile);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT user_id, nickname, created_at, created_by,
                               updated_at, updated_by, deleted
                        FROM t_user_info
                        WHERE user_id = ?
                        """,
                1003L);
        assertThat(row.get("USER_ID")).isEqualTo(1003L);
        assertThat(row.get("NICKNAME")).isEqualTo("New Profile");
        assertThat(row.get("CREATED_AT")).isNotNull();
        assertThat(row.get("UPDATED_AT")).isNotNull();
        assertThat(row.get("CREATED_BY")).isEqualTo(1003L);
        assertThat(row.get("UPDATED_BY")).isEqualTo(1003L);
        assertThat(row.get("DELETED")).isEqualTo(0);
    }

    @Test
    @Transactional
    void shouldReadActiveProfileForUpdate() {
        insertAccount(1004L, "locked-profile");
        insertProfile(1004L, 0);

        assertThat(repository.findActiveByUserIdForUpdate(1004L))
                .contains(UserProfile.create(
                        1004L,
                        "TYB",
                        "https://example.com/avatar.png",
                        "中文简介",
                        "日本語紹介",
                        "English bio",
                        "Tokyo",
                        "https://example.com",
                        "public@example.com",
                        "https://github.com/tyb",
                        "https://x.com/tyb",
                        "https://linkedin.com/in/tyb",
                        "https://zhihu.com/people/tyb",
                        "https://qiita.com/tyb",
                        "https://juejin.cn/user/tyb"));
    }

    @Test
    void shouldUpdateAllProfileFieldsAndAuditColumns() {
        insertAccount(1005L, "updated-profile");
        insertProfile(1005L, 0);
        authenticate(1005L);
        LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
        UserProfile updated = UserProfile.create(
                1005L,
                "New Name",
                null,
                "新中文简介",
                null,
                "New English bio",
                null,
                "https://new.example.com",
                null,
                "https://github.com/new",
                null,
                "https://linkedin.com/in/new",
                null,
                "https://qiita.com/new",
                null);

        assertThat(repository.update(updated)).isTrue();

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        SELECT nickname, avatar_url, bio_zh, bio_ja, bio_en,
                               location, website, email_public, github_url,
                               twitter_url, linkedin_url, zhihu_url, qiita_url,
                               juejin_url, updated_at, updated_by
                        FROM t_user_info
                        WHERE user_id = ?
                        """,
                1005L);
        assertThat(row.get("NICKNAME")).isEqualTo("New Name");
        assertThat(row.get("AVATAR_URL")).isNull();
        assertThat(row.get("BIO_ZH")).isEqualTo("新中文简介");
        assertThat(row.get("BIO_JA")).isNull();
        assertThat(row.get("BIO_EN")).isEqualTo("New English bio");
        assertThat(row.get("LOCATION")).isNull();
        assertThat(row.get("WEBSITE")).isEqualTo("https://new.example.com");
        assertThat(row.get("EMAIL_PUBLIC")).isNull();
        assertThat(row.get("GITHUB_URL")).isEqualTo("https://github.com/new");
        assertThat(row.get("TWITTER_URL")).isNull();
        assertThat(row.get("LINKEDIN_URL")).isEqualTo("https://linkedin.com/in/new");
        assertThat(row.get("ZHIHU_URL")).isNull();
        assertThat(row.get("QIITA_URL")).isEqualTo("https://qiita.com/new");
        assertThat(row.get("JUEJIN_URL")).isNull();
        assertThat(((Timestamp) row.get("UPDATED_AT")).toLocalDateTime())
                .isAfter(beforeUpdate);
        assertThat(row.get("UPDATED_BY")).isEqualTo(1005L);
    }

    @Test
    void shouldNotUpdateDeletedProfile() {
        insertAccount(1006L, "deleted-update");
        insertProfile(1006L, 1);
        authenticate(1006L);

        boolean updated = repository.update(UserProfile.create(
                1006L, "New Name", null, null, null, null, null,
                null, null, null, null, null, null, null, null));

        assertThat(updated).isFalse();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT nickname FROM t_user_info WHERE user_id = ?",
                String.class,
                1006L)).isEqualTo("TYB");
    }

    private void insertAccount(long id, String username) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type, deleted
                        ) VALUES (?, ?, ?, 1, 0)
                        """,
                id,
                username,
                "$2a$10$test-password-hash");
    }

    private void insertProfile(long userId, int deleted) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_user_info (
                            user_id, nickname, avatar_url,
                            bio_zh, bio_ja, bio_en,
                            location, website, email_public,
                            github_url, twitter_url, linkedin_url,
                            zhihu_url, qiita_url, juejin_url,
                            deleted
                        ) VALUES (
                            ?, 'TYB', 'https://example.com/avatar.png',
                            '中文简介', '日本語紹介', 'English bio',
                            'Tokyo', 'https://example.com', 'public@example.com',
                            'https://github.com/tyb', 'https://x.com/tyb',
                            'https://linkedin.com/in/tyb',
                            'https://zhihu.com/people/tyb', 'https://qiita.com/tyb',
                            'https://juejin.cn/user/tyb', ?
                        )
                        """,
                userId,
                deleted);
    }

    private void insertArticle(
            long id,
            long authorId,
            int status,
            LocalDateTime publishAt,
            int deleted) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_article (
                            id, title_zh, body, author_id, slug, status,
                            publish_at, deleted
                        ) VALUES (?, ?, 'body', ?, ?, ?, ?, ?)
                        """,
                id,
                "Article " + id,
                authorId,
                "article-" + id,
                status,
                publishAt,
                deleted);
    }

    private void authenticate(long userId) {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(Long.toString(userId), "admin", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }
}
