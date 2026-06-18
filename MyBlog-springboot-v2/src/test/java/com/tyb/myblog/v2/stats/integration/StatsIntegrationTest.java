package com.tyb.myblog.v2.stats.integration;

import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
import com.tyb.myblog.v2.stats.application.PageViewDailyRebuildService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class StatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PageViewDailyRebuildService rebuildService;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM t_page_view_daily");
        jdbcTemplate.update("DELETE FROM t_page_view");
        jdbcTemplate.update("DELETE FROM t_article_tag");
        jdbcTemplate.update("DELETE FROM t_article");
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void recordsPublicTrafficAndBuildsPrivateDashboard() throws Exception {
        insertArticle(100L, "公开文章", 2, false);
        insertArticle(101L, "密码文章", 4, false);
        insertArticle(102L, "私密文章", 3, false);
        insertArticle(103L, "草稿文章", 1, false);
        insertArticle(104L, "定时文章", 5, false);
        insertArticle(105L, "已删除文章", 2, true);

        record("{\"articleId\":100,\"lang\":\"zh\"}")
                .andExpect(status().isOk());
        record("{\"articleId\":100,\"lang\":\"zh\"}")
                .andExpect(status().isOk());
        record("{\"articleId\":101,\"lang\":\"ja\"}")
                .andExpect(status().isOk());
        record("{\"lang\":\"en\"}")
                .andExpect(status().isOk());

        int acceptedRows = rawCount();
        for (long id : List.of(102L, 103L, 104L, 105L, 999L)) {
            record("{\"articleId\":" + id + ",\"lang\":\"zh\"}")
                    .andExpect(status().isNotFound());
        }
        assertThat(rawCount()).isEqualTo(acceptedRows);

        LocalDate date = LocalDate.of(2026, 6, 18);
        rebuildService.rebuild(date);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT pv FROM t_page_view_daily "
                        + "WHERE article_id=100 AND lang='zh' "
                        + "AND stat_date=?",
                Integer.class,
                date)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT uv FROM t_page_view_daily "
                        + "WHERE article_id=100 AND lang='zh' "
                        + "AND stat_date=?",
                Integer.class,
                date)).isEqualTo(1);

        String demoToken = token(1002L, "demo", 2, "DEMO");
        mockMvc.perform(get("/api/admin/stats/dashboard")
                        .header("Authorization", "Bearer " + demoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodPv").value(4))
                .andExpect(jsonPath("$.data.todayPv").value(4))
                .andExpect(jsonPath("$.data.todayUv").value(3))
                .andExpect(jsonPath("$.data.trend.length()").value(30))
                .andExpect(jsonPath("$.data.topArticles[0].articleId")
                        .value(100))
                .andExpect(jsonPath("$.data.topArticles[0].title")
                        .value("公开文章"))
                .andExpect(jsonPath(
                        "$.data.topArticles[0].dailyUvSum").value(1))
                .andExpect(jsonPath("$.data.languageDistribution.length()")
                        .value(3))
                .andExpect(jsonPath("$.data.uvTotal").doesNotExist());
        mockMvc.perform(get("/api/admin/stats/dashboard"))
                .andExpect(status().isUnauthorized());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_page_view "
                        + "WHERE visitor_hash LIKE '%203.0.113.1%'",
                Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_page_view "
                        + "WHERE visitor_hash LIKE '%JUnit-Agent%'",
                Integer.class)).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions record(
            String body) throws Exception {
        return mockMvc.perform(post("/api/public/stats/page-views")
                .with(request -> {
                    request.setRemoteAddr("203.0.113.1");
                    return request;
                })
                .header("User-Agent", "JUnit-Agent")
                .header("Referer", "https://example.com/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void insertArticle(
            long id,
            String title,
            int status,
            boolean deleted) {
        jdbcTemplate.update("""
                INSERT INTO t_article (
                    id, title_zh, summary_zh, body, category_id,
                    author_id, status, publish_at, comment_count,
                    created_at, created_by, updated_at, updated_by,
                    deleted, deleted_at, deleted_by
                ) VALUES (?, ?, '摘要', '正文', NULL,
                    1001, ?, CURRENT_TIMESTAMP, 0,
                    CURRENT_TIMESTAMP, 1001,
                    CURRENT_TIMESTAMP, 1001, ?,
                    CASE WHEN ? = 1 THEN CURRENT_TIMESTAMP ELSE NULL END,
                    CASE WHEN ? = 1 THEN 1001 ELSE NULL END)
                """,
                id, title, status,
                deleted ? 1 : 0,
                deleted ? 1 : 0,
                deleted ? 1 : 0);
    }

    private String token(
            long id,
            String username,
            int type,
            String role) {
        jdbcTemplate.update("""
                INSERT INTO t_user_auth (
                    id, username, password_hash, type,
                    token_version, deleted
                ) VALUES (?, ?, ?, ?, 0, 0)
                """, id, username,
                passwordEncoder.encode("old-password"), type);
        jdbcTemplate.update("""
                INSERT INTO t_user_info (
                    user_id, nickname, deleted
                ) VALUES (?, ?, 0)
                """, id, username);
        return tokenService.issueAccessToken(
                Long.toString(id), username, List.of(role), 0)
                .accessToken();
    }

    private int rawCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_page_view", Integer.class);
    }
}
