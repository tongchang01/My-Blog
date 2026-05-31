package com.tyb.myblog.v2.content;

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
class ContentCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsCategoriesWithoutToken() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Java"))
                .andExpect(jsonPath("$.data[0].articleCount").value(1))
                .andExpect(jsonPath("$.data[1].name").value("生活"))
                .andExpect(jsonPath("$.data[1].articleCount").value(1));
    }

    @Test
    void returnsTagsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Spring"))
                .andExpect(jsonPath("$.data[0].articleCount").value(1))
                .andExpect(jsonPath("$.data[2].name").value("重构"))
                .andExpect(jsonPath("$.data[2].articleCount").value(2));
    }

    @Test
    void returnsLimitedTopTagsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/tags/top?limit=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("重构"))
                .andExpect(jsonPath("$.data[0].articleCount").value(2));
    }

    @Test
    void clampsOversizedTopTagLimit() throws Exception {
        mockMvc.perform(get("/api/tags/top?limit=999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3));
    }
}
