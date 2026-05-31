package com.aurora.myblog.v2.modules.comment;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void returnsArticleCommentsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/comments?type=1&topicId=1&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].content").value("第一条文章评论"))
                .andExpect(jsonPath("$.data.records[0].replies[0].id").value(2));
    }

    @Test
    void returnsMessageCommentsWithoutTopicId() throws Exception {
        mockMvc.perform(get("/api/comments?type=2&page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(5));
    }

    @Test
    void returnsRepliesByCommentIdWithoutToken() throws Exception {
        mockMvc.perform(get("/api/comments/1/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].replyUser.nickname").value("普通用户"));
    }

    @Test
    void returnsTopCommentsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/comments/top"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(5))
                .andExpect(jsonPath("$.data[1].id").value(1));
    }

    @Test
    void rejectsUnsupportedCommentType() throws Exception {
        mockMvc.perform(get("/api/comments?type=99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void saveCommentRequiresLogin() throws Exception {
        mockMvc.perform(post("/api/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":1,"topicId":1,"content":"新的文章评论"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void loggedInUserCanSaveArticleComment() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"user@163.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(response, "$.data.accessToken");

        mockMvc.perform(post("/api/comments")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Forwarded-For", "203.0.113.77")
                        .header("User-Agent", "JUnit Browser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":1,"topicId":1,"content":"新的文章评论"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.review").value(false));

        String createIp = jdbcTemplate.queryForObject("""
                select create_ip
                from t_comment
                where comment_content = '新的文章评论'
                """, String.class);
        String userAgent = jdbcTemplate.queryForObject("""
                select user_agent
                from t_comment
                where comment_content = '新的文章评论'
                """, String.class);

        assertThat(createIp).isEqualTo("203.0.113.77");
        assertThat(userAgent).isEqualTo("JUnit Browser");
    }
}
