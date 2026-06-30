package com.tyb.myblog.v2.content.integration;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 分类标签公开读取、后台管理、引用保护和审计完整集成测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CategoryTagIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM t_article_tag");
        jdbcTemplate.update("DELETE FROM t_article");
        jdbcTemplate.update("DELETE FROM t_category");
        jdbcTemplate.update("DELETE FROM t_tag");
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void managesCompleteCategoryAndTagLifecycle() throws Exception {
        String admin = token(1001L, "admin", 1, "ADMIN");
        String demo = token(1002L, "demo", 2, "DEMO");

        JsonNode category = response(post("/api/admin/categories")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryRequest(
                        "后端", null, "Backend", "backend", 20)));
        long categoryId = category.at("/data/id").asLong();
        JsonNode secondCategory = response(post("/api/admin/categories")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(categoryRequest(
                        "前端", null, null, "frontend", 10)));
        long secondCategoryId =
                secondCategory.at("/data/id").asLong();
        JsonNode tag = response(post("/api/admin/tags")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tagRequest(
                        "Java", null, null, "java")));
        long tagId = tag.at("/data/id").asLong();

        mockMvc.perform(get("/api/admin/categories")
                        .header("Authorization", bearer(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", bearer(demo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tagRequest(
                                "Denied", null, null, "denied")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/public/categories")
                        .queryParam("lang", "ja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name")
                        .value("前端"))
                .andExpect(jsonPath("$.data[1].name")
                        .value("后端"));
        mockMvc.perform(get("/api/public/categories")
                        .queryParam("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name")
                        .value("前端"))
                .andExpect(jsonPath("$.data[1].name")
                        .value("Backend"));
        mockMvc.perform(get("/api/public/tags")
                        .queryParam("lang", "ja"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name")
                        .value("Java"));

        mockMvc.perform(put(
                                "/api/admin/categories/{id}",
                                categoryId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryRequest(
                                "服务端",
                                null,
                                null,
                                "server",
                                30)))
                .andExpect(status().isConflict());

        mockMvc.perform(put(
                                "/api/admin/categories/{id}",
                                categoryId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryRequest(
                                "服务端",
                                null,
                                null,
                                "backend",
                                30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nameJa")
                        .doesNotExist())
                .andExpect(jsonPath("$.data.nameEn")
                        .doesNotExist());
        mockMvc.perform(put("/api/admin/categories/sort-orders")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[
                                  {"id":%d,"sortOrder":0},
                                  {"id":%d,"sortOrder":10}
                                ]}
                                """.formatted(
                                categoryId,
                                secondCategoryId)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/categories")
                        .queryParam("lang", "zh"))
                .andExpect(jsonPath("$.data[0].id")
                        .value(categoryId));

        insertArticle(3001L, categoryId);
        jdbcTemplate.update("""
                INSERT INTO t_article_tag (article_id, tag_id)
                VALUES (?, ?)
                """, 3001L, tagId);
        mockMvc.perform(delete(
                                "/api/admin/categories/{id}",
                                categoryId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/admin/tags/{id}", tagId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());

        jdbcTemplate.update("""
                UPDATE t_article
                SET deleted = 1,
                    deleted_at = CURRENT_TIMESTAMP,
                    deleted_by = 1001
                WHERE id = 3001
                """);
        mockMvc.perform(delete(
                                "/api/admin/categories/{id}",
                                categoryId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/admin/tags/{id}", tagId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForMap("""
                SELECT deleted, deleted_by, updated_by,
                       deleted_at, updated_at
                FROM t_category
                WHERE id = ?
                """, categoryId))
                .containsEntry("DELETED", 1)
                .containsEntry("DELETED_BY", 1001L)
                .containsEntry("UPDATED_BY", 1001L);
        mockMvc.perform(get(
                                "/api/admin/categories/{id}",
                                categoryId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/public/tags")
                        .queryParam("lang", "zh"))
                .andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryRequest(
                                "复用", null, null, "backend", 0)))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/tags")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tagRequest(
                                "复用", null, null, "java")))
                .andExpect(status().isConflict());
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

    private String categoryRequest(
            String nameZh,
            String nameJa,
            String nameEn,
            String slug,
            int sortOrder) {
        return """
                {
                  "nameZh":%s,
                  "nameJa":%s,
                  "nameEn":%s,
                  "slug":%s,
                  "sortOrder":%d
                }
                """.formatted(
                json(nameZh),
                json(nameJa),
                json(nameEn),
                json(slug),
                sortOrder);
    }

    private String tagRequest(
            String nameZh,
            String nameJa,
            String nameEn,
            String slug) {
        return """
                {
                  "nameZh":%s,
                  "nameJa":%s,
                  "nameEn":%s,
                  "slug":%s
                }
                """.formatted(
                json(nameZh),
                json(nameJa),
                json(nameEn),
                json(slug));
    }

    private String json(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void insertArticle(long id, long categoryId) {
        jdbcTemplate.update("""
                INSERT INTO t_article (
                    id, title_zh, category_id, author_id,
                    slug, status, comment_count,
                    created_at, created_by, updated_at, updated_by,
                    deleted
                ) VALUES (
                    ?, '文章', ?, 1001,
                    ?, 2, 0,
                    CURRENT_TIMESTAMP, 1001,
                    CURRENT_TIMESTAMP, 1001,
                    0
                )
                """,
                id,
                categoryId,
                "article-" + id);
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

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
