package com.aurora.myblog.v2.modules.content;

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
class ContentArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsPublishedArticlePageWithoutToken() throws Exception {
        mockMvc.perform(get("/api/articles?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.records[0].id").value(2))
                .andExpect(jsonPath("$.data.records[0].title").value("生活记录第一篇"))
                .andExpect(jsonPath("$.data.records[0].category.name").value("生活"))
                .andExpect(jsonPath("$.data.records[0].tags[0].name").value("Vue"))
                .andExpect(jsonPath("$.data.records[1].id").value(1))
                .andExpect(jsonPath("$.data.records[1].tags.length()").value(2));
    }

    @Test
    void normalizesInvalidPageParameters() throws Exception {
        mockMvc.perform(get("/api/articles?page=0&size=999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(50));
    }

    @Test
    void returnsPublishedArticlePageByCategoryWithoutToken() throws Exception {
        mockMvc.perform(get("/api/categories/1/articles?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].category.name").value("Java"));
    }

    @Test
    void returnsPublishedArticlePageByTagWithoutToken() throws Exception {
        mockMvc.perform(get("/api/tags/3/articles?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].id").value(2))
                .andExpect(jsonPath("$.data.records[1].id").value(1));
    }

    @Test
    void returnsPublishedArticleDetailWithoutToken() throws Exception {
        mockMvc.perform(get("/api/articles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andExpect(jsonPath("$.data.category.name").value("Java"))
                .andExpect(jsonPath("$.data.tags.length()").value(2));
    }

    @Test
    void returnsNotFoundForNonPublicArticleDetail() throws Exception {
        mockMvc.perform(get("/api/articles/3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/articles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
