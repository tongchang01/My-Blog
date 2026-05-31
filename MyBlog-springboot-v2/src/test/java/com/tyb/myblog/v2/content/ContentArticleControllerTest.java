package com.tyb.myblog.v2.content;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void returnsFeaturedArticlesWithoutToken() throws Exception {
        mockMvc.perform(get("/api/articles/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.topArticle.id").value(1))
                .andExpect(jsonPath("$.data.featuredArticles.length()").value(1))
                .andExpect(jsonPath("$.data.featuredArticles[0].id").value(2));
    }

    @Test
    void returnsArticleArchivesWithoutToken() throws Exception {
        mockMvc.perform(get("/api/articles/archives?page=1&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].month").value("2026-05"))
                .andExpect(jsonPath("$.data.records[0].articles[0].id").value(1))
                .andExpect(jsonPath("$.data.records[1].month").value("2026-04"))
                .andExpect(jsonPath("$.data.records[1].articles[0].id").value(2));
    }

    @Test
    void returnsAccessTokenForCorrectProtectedArticlePassword() throws Exception {
        mockMvc.perform(post("/api/articles/3/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"open-sesame"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.articleId").value(3))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
    }

    @Test
    void rejectsProtectedArticleDetailWithoutAccessToken() throws Exception {
        mockMvc.perform(get("/api/articles/3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void returnsProtectedArticleDetailWithAccessToken() throws Exception {
        String response = mockMvc.perform(post("/api/articles/3/access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"open-sesame"}
                                """))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String accessToken = JsonPath.read(response, "$.data.accessToken");

        mockMvc.perform(get("/api/articles/3").header("X-Article-Access-Token", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.content").value("密码正文"));
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
        mockMvc.perform(get("/api/articles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
