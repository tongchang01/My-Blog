package com.aurora.myblog.v2.modules.comment;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(statements = "update t_comment set is_review = case when id in (1, 2, 5, 6) then 1 when id = 3 then 0 else is_review end, is_delete = case when id = 4 then 1 when id in (1, 2, 3, 5, 6) then 0 else is_delete end where id in (1, 2, 3, 4, 5, 6)")
class AdminCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsAnonymousAdminCommentList() throws Exception {
        mockMvc.perform(get("/api/admin/comments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void rejectsNonAdminUser() throws Exception {
        String token = loginAndToken("user@163.com");

        mockMvc.perform(get("/api/admin/comments").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listsCommentsForAdmin() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(get("/api/admin/comments")
                        .param("reviewed", "false")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(3))
                .andExpect(jsonPath("$.data.records[0].reviewed").value(false));
    }

    @Test
    void getsCommentDetailForAdmin() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(get("/api/admin/comments/{id}", 4)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.content").value("已删除评论"))
                .andExpect(jsonPath("$.data.deleted").value(true));
    }

    @Test
    void listsDeletedCommentsForAdmin() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(get("/api/admin/comments")
                        .param("deleted", "true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(4))
                .andExpect(jsonPath("$.data.records[0].deleted").value(true));
    }

    @Test
    void reviewsCommentsForAdmin() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(put("/api/admin/comments/review")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":[3],"reviewed":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affected").value(1));

        Integer reviewedBy = jdbcTemplate.queryForObject("select reviewed_by from t_comment where id = 3", Integer.class);
        Object reviewTime = jdbcTemplate.queryForObject("select review_time from t_comment where id = 3", Object.class);
        assertThat(reviewedBy).isEqualTo(1);
        assertThat(reviewTime).isNotNull();
    }

    @Test
    void deletesCommentsForAdmin() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(delete("/api/admin/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":[5]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affected").value(1));

        Integer deletedBy = jdbcTemplate.queryForObject("select deleted_by from t_comment where id = 5", Integer.class);
        Object deleteTime = jdbcTemplate.queryForObject("select delete_time from t_comment where id = 5", Object.class);
        assertThat(deletedBy).isEqualTo(1);
        assertThat(deleteTime).isNotNull();
    }

    @Test
    void restoresCommentsForAdmin() throws Exception {
        String token = loginAndToken("admin@163.com");

        mockMvc.perform(put("/api/admin/comments/restore")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":[4]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affected").value(1));

        Integer restoredBy = jdbcTemplate.queryForObject("select restored_by from t_comment where id = 4", Integer.class);
        Object restoreTime = jdbcTemplate.queryForObject("select restore_time from t_comment where id = 4", Object.class);
        assertThat(restoredBy).isEqualTo(1);
        assertThat(restoreTime).isNotNull();
    }

    private String loginAndToken(String username) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"password123"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(response, "$.data.accessToken");
    }
}
