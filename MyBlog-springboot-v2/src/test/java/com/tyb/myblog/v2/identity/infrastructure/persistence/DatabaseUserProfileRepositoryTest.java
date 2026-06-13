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

    private void authenticate(long userId) {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(Long.toString(userId), "admin", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));
    }
}
