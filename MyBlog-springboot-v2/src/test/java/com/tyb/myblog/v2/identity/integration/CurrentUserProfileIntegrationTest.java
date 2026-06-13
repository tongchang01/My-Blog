package com.tyb.myblog.v2.identity.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.identity.domain.account.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 当前用户资料从登录、鉴权到数据库更新的完整 HTTP 验收测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CurrentUserProfileIntegrationTest {

    private static final String PASSWORD = "correct-password";
    private static final LocalDateTime OLD_UPDATED_AT =
            LocalDateTime.of(2026, 1, 1, 0, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void adminLogsInAndReadsCompleteCurrentProfile() throws Exception {
        insertAccountAndProfile(1001L, "admin", AccountType.ADMIN);

        String accessToken = login("admin");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.type").value("ADMIN"))
                .andExpect(jsonPath("$.data.profile.nickname")
                        .value("admin nickname"))
                .andExpect(jsonPath("$.data.profile.avatarUrl")
                        .value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.data.profile.bioZh").value("中文简介"))
                .andExpect(jsonPath("$.data.profile.bioJa").value("日本語"))
                .andExpect(jsonPath("$.data.profile.bioEn").value("English"))
                .andExpect(jsonPath("$.data.profile.location").value("Tokyo"))
                .andExpect(jsonPath("$.data.profile.website")
                        .value("https://example.com"))
                .andExpect(jsonPath("$.data.profile.emailPublic")
                        .value("public@example.com"))
                .andExpect(jsonPath("$.data.profile.githubUrl")
                        .value("https://github.com/example"))
                .andExpect(jsonPath("$.data.profile.twitterUrl")
                        .value("https://example.com/twitter"))
                .andExpect(jsonPath("$.data.profile.linkedinUrl")
                        .value("https://linkedin.com/in/example"))
                .andExpect(jsonPath("$.data.profile.zhihuUrl")
                        .value("https://www.zhihu.com/people/example"))
                .andExpect(jsonPath("$.data.profile.qiitaUrl")
                        .value("https://qiita.com/example"))
                .andExpect(jsonPath("$.data.profile.juejinUrl")
                        .value("https://juejin.cn/user/example"));
    }

    @Test
    void adminPartiallyUpdatesProfileAndWritesAuditFields()
            throws Exception {
        insertAccountAndProfile(1001L, "admin", AccountType.ADMIN);
        String accessToken = login("admin");

        mockMvc.perform(patch("/api/auth/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname":"New Nickname",
                                  "twitterUrl":null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andExpect(jsonPath("$.data.nickname").value("New Nickname"))
                .andExpect(jsonPath("$.data.twitterUrl").isEmpty())
                .andExpect(jsonPath("$.data.website")
                        .value("https://example.com"));

        ProfileState profile = readProfile(1001L);
        assertThat(profile.nickname()).isEqualTo("New Nickname");
        assertThat(profile.twitterUrl()).isNull();
        assertThat(profile.website()).isEqualTo("https://example.com");
        assertThat(profile.updatedAt()).isAfter(OLD_UPDATED_AT);
        assertThat(profile.updatedBy()).isEqualTo(1001L);
    }

    @Test
    void demoReadsProfileButCannotUpdateIt() throws Exception {
        insertAccountAndProfile(2002L, "demo", AccountType.DEMO);
        String accessToken = login("demo");
        ProfileState before = readProfile(2002L);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("DEMO"))
                .andExpect(jsonPath("$.data.profile.nickname")
                        .value("demo nickname"));

        mockMvc.perform(patch("/api/auth/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nickname":"Changed"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));

        assertThat(readProfile(2002L)).isEqualTo(before);
    }

    @Test
    void returnsInternalErrorWhenCurrentProfileIsMissing()
            throws Exception {
        insertAccount(3003L, "missing-profile", AccountType.ADMIN);
        String accessToken = login("missing-profile");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("99999"));
    }

    @Test
    void rejectsAnonymousCurrentProfileRequest() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("10002"));
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn();
        JsonNode data = objectMapper.readTree(
                        result.getResponse().getContentAsString())
                .path("data");
        return data.path("accessToken").asText();
    }

    private void insertAccountAndProfile(
            long id,
            String username,
            AccountType type
    ) {
        insertAccount(id, username, type);
        jdbcTemplate.update("""
                        INSERT INTO t_user_info (
                            user_id, nickname, avatar_url,
                            bio_zh, bio_ja, bio_en, location,
                            website, email_public, github_url,
                            twitter_url, linkedin_url, zhihu_url,
                            qiita_url, juejin_url,
                            created_at, created_by,
                            updated_at, updated_by, deleted
                        ) VALUES (
                            ?, ?, ?,
                            ?, ?, ?, ?,
                            ?, ?, ?,
                            ?, ?, ?,
                            ?, ?,
                            ?, ?,
                            ?, ?, 0
                        )
                        """,
                id,
                username + " nickname",
                "https://example.com/avatar.png",
                "中文简介",
                "日本語",
                "English",
                "Tokyo",
                "https://example.com",
                "public@example.com",
                "https://github.com/example",
                "https://example.com/twitter",
                "https://linkedin.com/in/example",
                "https://www.zhihu.com/people/example",
                "https://qiita.com/example",
                "https://juejin.cn/user/example",
                OLD_UPDATED_AT,
                id,
                OLD_UPDATED_AT,
                id);
    }

    private void insertAccount(
            long id,
            String username,
            AccountType type
    ) {
        jdbcTemplate.update("""
                        INSERT INTO t_user_auth (
                            id, username, password_hash, type,
                            token_version, deleted
                        ) VALUES (?, ?, ?, ?, 0, 0)
                        """,
                id,
                username,
                passwordEncoder.encode(PASSWORD),
                type.databaseValue());
    }

    private ProfileState readProfile(long userId) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT nickname, twitter_url, website,
                               updated_at, updated_by
                        FROM t_user_info
                        WHERE user_id = ?
                        """,
                (resultSet, rowNumber) -> new ProfileState(
                        resultSet.getString("nickname"),
                        resultSet.getString("twitter_url"),
                        resultSet.getString("website"),
                        resultSet.getObject(
                                "updated_at",
                                LocalDateTime.class),
                        resultSet.getObject(
                                "updated_by",
                                Long.class)),
                userId);
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private record ProfileState(
            String nickname,
            String twitterUrl,
            String website,
            LocalDateTime updatedAt,
            Long updatedBy
    ) {
    }
}
