package com.tyb.myblog.v2.content.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyb.myblog.v2.common.security.auth.JwtTokenService;
import com.tyb.myblog.v2.content.application.article.ArticleSchedulePublishService;
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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ArticleIntegrationTest {

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

    @Autowired
    private ArticleSchedulePublishService publishService;

    @BeforeEach
    void resetState() {
        jdbcTemplate.update("DELETE FROM t_article_access_token");
        jdbcTemplate.update("DELETE FROM t_article_tag");
        jdbcTemplate.update("DELETE FROM t_article");
        jdbcTemplate.update("DELETE FROM t_attachment");
        jdbcTemplate.update("DELETE FROM t_category");
        jdbcTemplate.update("DELETE FROM t_tag");
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void managesArticleLifecycleAcrossAdminAndPublicApis()
            throws Exception {
        String admin = token(1001L, "admin", 1, "ADMIN");
        String demo = token(1002L, "demo", 2, "DEMO");
        insertCategory(10L);
        insertTag(20L);
        insertAttachment(300L);

        long publishedId = response(post("/api/admin/articles")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeBody(
                        "已发布",
                        "Published",
                        "PUBLISHED",
                        null,
                        null))).at("/data/id").asLong();
        long passwordId = response(post("/api/admin/articles")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeBody(
                        "密码文",
                        null,
                        "PASSWORD",
                        "open-sesame",
                        null))).at("/data/id").asLong();
        long scheduledId = response(post("/api/admin/articles")
                .header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(writeBody(
                        "定时文",
                        null,
                        "SCHEDULED",
                        null,
                        LocalDateTime.now()
                                .plusDays(1)
                                .withNano(0)
                                .toString()))).at("/data/id").asLong();

        mockMvc.perform(get("/api/admin/articles/{id}", publishedId)
                        .header("Authorization", bearer(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("正文"));
        mockMvc.perform(get("/api/admin/articles/{id}", passwordId)
                        .header("Authorization", bearer(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value((Object) null));
        mockMvc.perform(get("/api/admin/articles/{id}", scheduledId)
                        .header("Authorization", bearer(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value((Object) null));
        mockMvc.perform(get("/api/admin/articles/{id}", passwordId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("正文"));

        assertThat(hash(passwordId)).startsWith("$2");
        mockMvc.perform(get("/api/admin/articles")
                        .header("Authorization", bearer(demo)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/articles")
                        .header("Authorization", bearer(demo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody(
                                "拒绝",
                                null,
                                "PUBLISHED",
                                null,
                                null)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/public/articles")
                        .queryParam("lang", "en")
                        .queryParam("categoryId", "10")
                        .queryParam("tagId", "20")
                        .queryParam("keyword", "Published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].title")
                        .value("Published"))
                .andExpect(jsonPath("$.data.records[0].body")
                        .doesNotExist());
        mockMvc.perform(get("/api/public/articles/{id}", publishedId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("正文"));
        mockMvc.perform(get("/api/public/articles/{id}", passwordId))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/public/articles/{id}/unlock", passwordId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isForbidden());
        String articleAccessToken = response(post("/api/public/articles/{id}/unlock", passwordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"open-sesame\"}"))
                .at("/data/token").asText();
        mockMvc.perform(get("/api/public/articles/{id}", passwordId)
                        .header("X-Article-Access-Token", articleAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("正文"));

        String retainedHash = hash(passwordId);
        mockMvc.perform(put("/api/admin/articles/{id}", passwordId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody(
                                "密码文",
                                null,
                                "PASSWORD",
                                null,
                                null)))
                .andExpect(status().isOk());
        assertThat(hash(passwordId)).isEqualTo(retainedHash);
        mockMvc.perform(get("/api/public/articles/{id}", passwordId)
                        .header("X-Article-Access-Token", articleAccessToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/articles/{id}", passwordId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody(
                                "密码文",
                                null,
                                "PASSWORD",
                                "new-secret",
                                null)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/articles/{id}", passwordId)
                        .header("X-Article-Access-Token", articleAccessToken))
                .andExpect(status().isForbidden());
        String renewedArticleAccessToken = response(post(
                "/api/public/articles/{id}/unlock", passwordId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"new-secret\"}"))
                .at("/data/token").asText();
        mockMvc.perform(get("/api/public/articles/{id}", passwordId)
                        .header("X-Article-Access-Token", renewedArticleAccessToken))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/articles/{id}", passwordId)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(writeBody(
                                "公开文",
                                null,
                                "PUBLISHED",
                                null,
                                null)))
                .andExpect(status().isOk());
        assertThat(hash(passwordId)).isNull();

        mockMvc.perform(delete("/api/admin/articles/{id}", publishedId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/articles/{id}", publishedId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/articles/recycle-bin")
                        .header("Authorization", bearer(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id")
                        .value(Long.toString(publishedId)));

        jdbcTemplate.update("UPDATE t_tag SET deleted = 1 WHERE id = 20");
        mockMvc.perform(post("/api/admin/articles/{id}/restore", publishedId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
        jdbcTemplate.update("UPDATE t_tag SET deleted = 0 WHERE id = 20");
        mockMvc.perform(post("/api/admin/articles/{id}/restore", publishedId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        jdbcTemplate.update("""
                UPDATE t_article
                SET publish_at = '2026-06-15 00:00:00'
                WHERE id = ?
                """, scheduledId);
        assertThat(publishService.publishDue()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT status
                FROM t_article
                WHERE id = ?
                """, Integer.class, scheduledId)).isEqualTo(2);
    }

    @Test
    void publicKeywordSearchMatchesTitleAndSummaryButNotBody()
            throws Exception {
        insertCategory(10L);
        insertTag(20L);
        insertAttachment(300L);
        insertPublishedArticle(100L, "Spring 标题", "普通摘要", "正文不重要");
        insertPublishedArticle(101L, "普通标题", "摘要包含 Spring", "正文不重要");
        insertPublishedArticle(102L, "普通标题", "普通摘要", "正文包含 Spring");

        mockMvc.perform(get("/api/public/articles")
                        .queryParam("lang", "zh")
                        .queryParam("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[*].id")
                        .value(containsInAnyOrder("100", "101")))
                .andExpect(jsonPath("$.data.records[*].body")
                        .doesNotExist());
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

    private String writeBody(
            String titleZh,
            String titleEn,
            String status,
            String password,
            String publishAt) throws Exception {
        return """
                {
                  "titleZh":%s,
                  "titleJa":null,
                  "titleEn":%s,
                  "summaryZh":"摘要",
                  "summaryJa":null,
                  "summaryEn":null,
                  "body":"正文",
                  "categoryId":10,
                  "tagIds":[20],
                  "slug":null,
                  "status":%s,
                  "homepageSlot":"NONE",
                  "password":%s,
                  "publishAt":%s,
                  "coverAttachmentId":300
                }
                """.formatted(
                json(titleZh),
                json(titleEn),
                json(status),
                json(password),
                json(publishAt));
    }

    private String json(String value) throws Exception {
        return objectMapper.writeValueAsString(value);
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

    private void insertCategory(long id) {
        jdbcTemplate.update("""
                INSERT INTO t_category (
                    id, name_zh, name_en, slug, sort_order,
                    created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (?, '分类', 'Category', ?, 0,
                    CURRENT_TIMESTAMP, 1001,
                    CURRENT_TIMESTAMP, 1001, 0)
                """, id, "category-" + id);
    }

    private void insertTag(long id) {
        jdbcTemplate.update("""
                INSERT INTO t_tag (
                    id, name_zh, name_en, slug,
                    created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (?, '标签', 'Tag', ?,
                    CURRENT_TIMESTAMP, 1001,
                    CURRENT_TIMESTAMP, 1001, 0)
                """, id, "tag-" + id);
    }

    private void insertAttachment(long id) {
        jdbcTemplate.update("""
                INSERT INTO t_attachment (
                    id, storage_type, bucket, object_key, public_url,
                    content_type, file_size, width, height,
                    original_filename, hash_sha256,
                    created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (?, 'LOCAL', 'local', ?, ?, 'image/png',
                    128, 2, 3, 'cover.png', ?,
                    CURRENT_TIMESTAMP, 1001,
                    CURRENT_TIMESTAMP, 1001, 0)
                """,
                id,
                "articles/" + id + ".png",
                "http://localhost/media/" + id + ".png",
                "a".repeat(64));
    }

    private void insertPublishedArticle(
            long id,
            String titleZh,
            String summaryZh,
            String body) {
        jdbcTemplate.update("""
                INSERT INTO t_article (
                    id, title_zh, title_ja, title_en,
                    summary_zh, summary_ja, summary_en,
                    body, category_id, author_id, slug, status, publish_at,
                    cover_attachment_id, homepage_slot,
                    created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (?, ?, null, null, ?, null, null, ?, 10, 1001, ?,
                    2, CURRENT_TIMESTAMP, 300, 'NONE',
                    CURRENT_TIMESTAMP, 1001, CURRENT_TIMESTAMP, 1001, 0)
                """,
                id,
                titleZh,
                summaryZh,
                body,
                "article-" + id);
    }

    private String hash(long articleId) {
        return jdbcTemplate.queryForObject("""
                SELECT access_password
                FROM t_article
                WHERE id = ?
                """, String.class, articleId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
