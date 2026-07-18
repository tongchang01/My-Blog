package com.tyb.myblog.v2.comment.integration;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CommentIntegrationTest {

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
        jdbcTemplate.update("DELETE FROM t_mail_log");
        jdbcTemplate.update("DELETE FROM t_comment");
        jdbcTemplate.update("DELETE FROM t_article_access_token");
        jdbcTemplate.update("DELETE FROM t_article_tag");
        jdbcTemplate.update("DELETE FROM t_article");
        jdbcTemplate.update("DELETE FROM t_refresh_token");
        jdbcTemplate.update("DELETE FROM t_user_info");
        jdbcTemplate.update("DELETE FROM t_user_auth");
    }

    @Test
    void managesPublicSubmissionAndAdminModerationFlow()
            throws Exception {
        String admin = token(1001L, "admin", 1, "ADMIN");
        String demo = token(1002L, "demo", 2, "DEMO");
        insertArticle(100L, 2);
        insertArticle(101L, 4);

        long passedId = response(post("/api/public/articles/100/comments")
                .header("User-Agent", "JUnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentBody("Reader", "reader@example.com", "hello", null)))
                .at("/data/id").asLong();
        assertThat(commentCount(100L)).isEqualTo(1);

        mockMvc.perform(get("/api/public/articles/100/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].contentHtml").exists())
                .andExpect(jsonPath("$.data.records[0].contentMd").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].authorEmail").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].authorIp").doesNotExist())
                .andExpect(jsonPath("$.data.records[0].authorUserAgent").doesNotExist());
        mockMvc.perform(get("/api/public/articles/101/comments"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/public/articles/101/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentBody(
                                "Reader",
                                "reader2@example.com",
                                "password article",
                                null)))
                .andExpect(status().isForbidden());

        long guestbookId = response(post("/api/public/guestbook/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentBody("Guest", "guest@example.com", "guestbook", null)))
                .at("/data/id").asLong();
        response(post("/api/public/guestbook/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentBody(
                        "Guest2",
                        "guest2@example.com",
                        "reply",
                        guestbookId)));
        mockMvc.perform(get("/api/public/guestbook/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].replies[0].replyToCommentId")
                        .value(Long.toString(guestbookId)));

        long pendingId = response(post("/api/public/articles/100/comments")
                .header("User-Agent", "JUnit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentBody("Spam", "spam@example.com", "spam text", null)))
                .at("/data/id").asLong();
        assertThat(commentCount(100L)).isEqualTo(1);

        mockMvc.perform(get("/api/admin/comments")
                        .header("Authorization", bearer(demo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].authorEmail")
                        .value((Object) null))
                .andExpect(jsonPath("$.data.records[0].authorIp")
                        .value((Object) null))
                .andExpect(jsonPath("$.data.records[0].authorUserAgent")
                        .value((Object) null))
                .andExpect(jsonPath("$.data.records[0].authorNickname")
                        .exists())
                .andExpect(jsonPath("$.data.records[0].contentMd")
                        .exists());
        mockMvc.perform(get("/api/admin/comments")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].authorEmail")
                        .exists())
                .andExpect(jsonPath("$.data.records[0].authorIp")
                        .exists())
                .andExpect(jsonPath("$.data.records[0].authorUserAgent")
                        .exists());
        mockMvc.perform(post("/api/admin/comments/{id}/approve", pendingId)
                        .header("Authorization", bearer(demo)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/comments/{id}/approve", pendingId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(commentCount(100L)).isEqualTo(2);

        mockMvc.perform(post("/api/admin/comments/{id}/hide", pendingId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(commentCount(100L)).isEqualTo(1);
        mockMvc.perform(delete("/api/admin/comments/{id}", passedId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(commentCount(100L)).isEqualTo(0);
        mockMvc.perform(post("/api/admin/comments/{id}/restore", passedId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(commentCount(100L)).isEqualTo(1);
    }

    @Test
    void passwordArticleAccessTokenAllowsCommentReadAndSubmission()
            throws Exception {
        insertArticle(101L, 4);
        jdbcTemplate.update("""
                UPDATE t_article
                SET access_password = ?
                WHERE id = ?
                """, passwordEncoder.encode("open-sesame"), 101L);

        String articleAccessToken = response(post("/api/public/articles/101/unlock")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"open-sesame\"}"))
                .at("/data/token")
                .asText();

        mockMvc.perform(get("/api/public/articles/101/comments")
                        .header("X-Article-Access-Token", articleAccessToken))
                .andExpect(status().isOk());
        response(post("/api/public/articles/101/comments")
                .header("X-Article-Access-Token", articleAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentBody(
                        "Reader",
                        "reader@example.com",
                        "password article",
                        null)));
        assertThat(commentCount(101L)).isEqualTo(1);
    }

    @Test
    void rootVisibilityControlsRepliesAndArticleCount()
            throws Exception {
        String admin = token(1001L, "admin", 1, "ADMIN");
        insertArticle(100L, 2);
        long rootId = response(post("/api/public/articles/100/comments")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.2");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentBody(
                        "Root", "root@example.com", "root", null)))
                .at("/data/id").asLong();
        response(post("/api/public/articles/100/comments")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.2");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentBody(
                        "Reply", "reply@example.com", "reply", rootId)));
        assertThat(commentCount(100L)).isEqualTo(2);

        mockMvc.perform(post("/api/admin/comments/{id}/hide", rootId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(commentCount(100L)).isZero();
        mockMvc.perform(get("/api/public/articles/100/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(post("/api/admin/comments/{id}/approve", rootId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(commentCount(100L)).isEqualTo(2);
        mockMvc.perform(get("/api/public/articles/100/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].replies").isNotEmpty());

        mockMvc.perform(delete("/api/admin/comments/{id}", rootId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(commentCount(100L)).isZero();
        mockMvc.perform(post("/api/admin/comments/{id}/restore", rootId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        assertThat(commentCount(100L)).isEqualTo(2);
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

    private String commentBody(
            String nickname,
            String email,
            String contentMd,
            Long replyToCommentId) throws Exception {
        return """
                {
                  "nickname":%s,
                  "email":%s,
                  "site":null,
                  "contentMd":%s,
                  "replyToCommentId":%s
                }
                """.formatted(
                json(nickname),
                json(email),
                json(contentMd),
                replyToCommentId == null ? "null" : replyToCommentId);
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

    private void insertArticle(long id, int status) {
        jdbcTemplate.update("""
                INSERT INTO t_article (
                    id, title_zh, summary_zh, body, category_id,
                    author_id, status, publish_at, comment_count,
                    created_at, created_by, updated_at, updated_by, deleted
                ) VALUES (?, ?, '摘要', '正文', NULL,
                    1001, ?, CURRENT_TIMESTAMP, 0,
                    CURRENT_TIMESTAMP, 1001,
                    CURRENT_TIMESTAMP, 1001, 0)
                """,
                id,
                "文章" + id,
                status);
    }

    private int commentCount(long articleId) {
        return jdbcTemplate.queryForObject("""
                SELECT comment_count
                FROM t_article
                WHERE id = ?
                """, Integer.class, articleId);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
