package com.tyb.myblog.v2.system.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 友链公开读取、后台管理、权限和审计完整集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class FriendLinkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM t_friend_link");
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_user_auth");
        insertLink(10L, "Visible", 1, 20, false);
        insertLink(20L, "Hidden", 2, 10, false);
        insertLink(30L, "Deleted", 1, 0, true);
    }

    @Test
    void managesCompleteLifecycleWithRoleBoundaries() throws Exception {
        String admin = token(1001L, "admin", 1, "ADMIN");
        String demo = token(1002L, "demo", 2, "DEMO");

        mockMvc.perform(get("/api/public/friend-links"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(10));
        mockMvc.perform(get("/api/admin/friend-links")
                        .header("Authorization", bearer(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2));
        mockMvc.perform(post("/api/admin/friend-links")
                        .header("Authorization", bearer(demo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeRequest(
                                "Denied", "https://denied.example.com",
                                "VISIBLE", 0)))
                .andExpect(status().isForbidden());

        JsonNode created = response(post("/api/admin/friend-links")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeRequest(
                        "Created", "https://example.com/10",
                        "VISIBLE", 5)));
        long createdId = created.at("/data/id").asLong();
        assertThat(createdId).isPositive();
        assertThat(jdbcTemplate.queryForMap("""
                SELECT created_by, updated_by
                FROM t_friend_link WHERE id = ?
                """, createdId))
                .containsEntry("CREATED_BY", 1001L)
                .containsEntry("UPDATED_BY", 1001L);

        mockMvc.perform(put("/api/admin/friend-links/{id}", createdId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeRequest(
                                "Updated",
                                "https://updated.example.com",
                                "VISIBLE",
                                30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.data.description").doesNotExist());

        mockMvc.perform(patch(
                                "/api/admin/friend-links/{id}/status",
                                createdId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/friend-links"))
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(patch(
                                "/api/admin/friend-links/{id}/status",
                                createdId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VISIBLE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/friend-links/sort-orders")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[
                                  {"id":%d,"sortOrder":0},
                                  {"id":10,"sortOrder":50}
                                ]}
                                """.formatted(createdId)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/friend-links"))
                .andExpect(jsonPath("$.data[0].id").value(createdId))
                .andExpect(jsonPath("$.data[1].id").value(10));

        mockMvc.perform(delete("/api/admin/friend-links/{id}", createdId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForMap("""
                SELECT deleted, deleted_by, updated_by,
                       deleted_at, updated_at
                FROM t_friend_link WHERE id = ?
                """, createdId))
                .containsEntry("DELETED", 1)
                .containsEntry("DELETED_BY", 1001L)
                .containsEntry("UPDATED_BY", 1001L);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT CASE WHEN deleted_at = updated_at
                       THEN 1 ELSE 0 END
                FROM t_friend_link WHERE id = ?
                """,
                Integer.class,
                createdId)).isEqualTo(1);
        mockMvc.perform(get("/api/admin/friend-links/{id}", createdId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidWritesAndRollsBackMissingSortTarget()
            throws Exception {
        String admin = token(1001L, "admin", 1, "ADMIN");

        assertBadRequest(admin, """
                {
                  "name":"Missing",
                  "url":"https://example.com",
                  "avatarUrl":null,
                  "description":null,
                  "sortOrder":0
                }
                """);
        assertBadRequest(admin, writeRequest(
                "Bad", "ftp://example.com", "VISIBLE", 0));
        assertBadRequest(admin, writeRequest(
                "Unknown", "https://example.com", "VISIBLE", 0)
                .replace("\"status\":\"VISIBLE\"",
                        "\"status\":\"VISIBLE\",\"unknown\":true"));

        mockMvc.perform(put("/api/admin/friend-links/sort-orders")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[
                                  {"id":10,"sortOrder":1},
                                  {"id":10,"sortOrder":2}
                                ]}
                                """))
                .andExpect(status().isBadRequest());

        int before = sortOrder(10L);
        mockMvc.perform(put("/api/admin/friend-links/sort-orders")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[
                                  {"id":10,"sortOrder":1},
                                  {"id":999999,"sortOrder":2}
                                ]}
                                """))
                .andExpect(status().isNotFound());
        assertThat(sortOrder(10L)).isEqualTo(before);
    }

    private void assertBadRequest(String token, String request)
            throws Exception {
        mockMvc.perform(post("/api/admin/friend-links")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("90001"));
    }

    private JsonNode response(
            org.springframework.test.web.servlet.request
                    .MockHttpServletRequestBuilder request)
            throws Exception {
        String content = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("00000"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private String writeRequest(
            String name,
            String url,
            String status,
            int sortOrder) {
        return """
                {
                  "name":"%s",
                  "url":"%s",
                  "avatarUrl":null,
                  "description":null,
                  "sortOrder":%d,
                  "status":"%s"
                }
                """.formatted(name, url, sortOrder, status);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private int sortOrder(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT sort_order FROM t_friend_link WHERE id = ?",
                Integer.class,
                id);
    }

    private void insertLink(
            long id,
            String name,
            int status,
            int sortOrder,
            boolean deleted) {
        LocalDateTime now =
                LocalDateTime.of(2026, 6, 14, 12, 0);
        jdbcTemplate.update("""
                INSERT INTO t_friend_link (
                    id, name, url, sort_order, status,
                    created_at, created_by, updated_at, updated_by,
                    deleted, deleted_at, deleted_by
                ) VALUES (?, ?, ?, ?, ?, ?, 900, ?, 900, ?, ?, ?)
                """,
                id,
                name,
                "https://example.com/" + id,
                sortOrder,
                status,
                now,
                now,
                deleted ? 1 : 0,
                deleted ? now : null,
                deleted ? 900L : null);
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
                """,
                id,
                username,
                passwordEncoder.encode("password"),
                type);
        jdbcTemplate.update("""
                INSERT INTO t_user_info (
                    user_id, nickname, deleted
                ) VALUES (?, ?, 0)
                """,
                id,
                username);
        return tokenService.issueAccessToken(
                Long.toString(id),
                username,
                List.of(role),
                0).accessToken();
    }
}
