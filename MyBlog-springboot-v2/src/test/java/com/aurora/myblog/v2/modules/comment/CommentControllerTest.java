package com.aurora.myblog.v2.modules.comment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
}
