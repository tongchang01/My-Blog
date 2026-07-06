package com.tyb.myblog.v2.system.integration;

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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 站点配置从公开读取、后台鉴权到数据库更新的完整 HTTP 验收测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SiteConfigIntegrationTest {

    private static final String PASSWORD = "correct-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepareDatabase() {
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        jdbcTemplate.update("DELETE FROM t_site_config");
        jdbcTemplate.update("""
                INSERT INTO t_site_config (
                    id, site_title_zh, site_title_ja,
                    site_subtitle_zh, about_md_zh, started_date, deleted
                ) VALUES (
                    1, '初始标题', '初期タイトル',
                    '初始副标题', '# 初始关于我', '2023-12-31', 0
                )
                """);
        insertAccount(1001L, "admin", AccountType.ADMIN);
        insertAccount(1002L, "demo", AccountType.DEMO);
    }

    @Test
    void completesPublicDemoAndAdminConfigurationFlow() throws Exception {
        mockMvc.perform(get("/api/public/site-config").param("lang", "ja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteTitle").value("初期タイトル"))
                .andExpect(jsonPath("$.data.siteSubtitle").value("初始副标题"))
                .andExpect(jsonPath("$.data.aboutMd").value("# 初始关于我"))
                .andExpect(jsonPath("$.data.startedDate").value("2023-12-31"));

        String demoToken = login("demo");
        mockMvc.perform(get("/api/admin/site-config")
                        .header(HttpHeaders.AUTHORIZATION, bearer(demoToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteTitleZh").value("初始标题"));

        Map<String, Object> update = validUpdate();
        mockMvc.perform(put("/api/admin/site-config")
                        .header(HttpHeaders.AUTHORIZATION, bearer(demoToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("10003"));
        assertThat(readTitle()).isEqualTo("初始标题");

        String adminToken = login("admin");
        mockMvc.perform(put("/api/admin/site-config")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteTitleZh").value("新标题"))
                .andExpect(jsonPath("$.data.siteTitleJa").isEmpty())
                .andExpect(jsonPath("$.data.startedDate").value("2024-01-02"))
                .andExpect(jsonPath("$.data.aboutMdZh")
                        .value("# 新的关于我\n\n正文"))
                .andExpect(jsonPath("$.data.updatedBy").value(1001));

        mockMvc.perform(get("/api/public/site-config").param("lang", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteTitle").value("新标题"))
                .andExpect(jsonPath("$.data.siteSubtitle").value("新副标题"))
                .andExpect(jsonPath("$.data.startedDate").value("2024-01-02"));
        mockMvc.perform(get("/api/public/site-config").param("lang", "ja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteTitle").value("新标题"))
                .andExpect(jsonPath("$.data.siteSubtitle").value("日本語副題"))
                .andExpect(jsonPath("$.data.aboutMd")
                        .value("# 新的关于我\n\n正文"));
        mockMvc.perform(get("/api/public/site-config").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteTitle").value("English title"))
                .andExpect(jsonPath("$.data.siteSubtitle").value("新副标题"))
                .andExpect(jsonPath("$.data.aboutMd").value("# About"));
    }

    @Test
    void rejectsInvalidUpdatesAndReportsMissingFixedRow() throws Exception {
        String adminToken = login("admin");

        Map<String, Object> missing = validUpdate();
        missing.remove("spotifyPlaylistId");
        assertInvalidUpdate(adminToken, missing);

        Map<String, Object> invalidUrl = validUpdate();
        invalidUrl.put("logoUrl", "javascript:alert(1)");
        assertInvalidUpdate(adminToken, invalidUrl);

        Map<String, Object> invalidMarkdown = validUpdate();
        invalidMarkdown.put("aboutMdZh", "x".repeat(50_001));
        assertInvalidUpdate(adminToken, invalidMarkdown);

        Map<String, Object> invalidSpotify = validUpdate();
        invalidSpotify.put("spotifyPlaylistId", "invalid id");
        assertInvalidUpdate(adminToken, invalidSpotify);
        assertThat(readTitle()).isEqualTo("初始标题");

        jdbcTemplate.update("DELETE FROM t_site_config WHERE id = 1");

        mockMvc.perform(get("/api/public/site-config").param("lang", "zh"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("99999"));
        mockMvc.perform(get("/api/admin/site-config")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("99999"));
        mockMvc.perform(put("/api/admin/site-config")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdate())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("99999"));
    }

    private void assertInvalidUpdate(
            String accessToken,
            Map<String, Object> request) throws Exception {
        mockMvc.perform(put("/api/admin/site-config")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
        assertThat(readTitle()).isEqualTo("初始标题");
    }

    private Map<String, Object> validUpdate() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("siteTitleZh", " 新标题 ");
        request.put("siteTitleJa", null);
        request.put("siteTitleEn", "English title");
        request.put("siteSubtitleZh", " 新副标题 ");
        request.put("siteSubtitleJa", "日本語副題");
        request.put("siteSubtitleEn", null);
        request.put("aboutMdZh", "# 新的关于我\n\n正文");
        request.put("aboutMdJa", null);
        request.put("aboutMdEn", "# About");
        request.put("logoUrl", " https://example.com/logo.png ");
        request.put("faviconUrl", null);
        request.put("icpNo", null);
        request.put("spotifyPlaylistId", "playlist_123");
        request.put("startedDate", "2024-01-02");
        return request;
    }

    private String login(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(
                        result.getResponse().getContentAsString())
                .path("data");
        return data.path("accessToken").asText();
    }

    private void insertAccount(
            long id,
            String username,
            AccountType type) {
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

    private String readTitle() {
        return jdbcTemplate.queryForObject(
                "SELECT site_title_zh FROM t_site_config WHERE id = 1",
                String.class);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
