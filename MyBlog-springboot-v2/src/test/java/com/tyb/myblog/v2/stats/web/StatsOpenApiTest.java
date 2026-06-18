package com.tyb.myblog.v2.stats.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class StatsOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsOnlyTheSupportedStatsMethods() throws Exception {
        JsonNode root = apiDocument();

        assertMethods(root,
                "/paths/~1api~1public~1stats~1page-views", "post");
        assertMethods(root,
                "/paths/~1api~1admin~1stats~1dashboard", "get");
    }

    @Test
    void exposesStableStatsSchemasWithoutInternalData() throws Exception {
        JsonNode root = apiDocument();

        assertThat(root.at(
                        "/components/schemas/PageViewRecordRequest/properties")
                .fieldNames()).toIterable()
                .containsExactlyInAnyOrder("articleId", "lang");
        assertThat(root.at(
                        "/components/schemas/TopArticle/properties")
                .fieldNames()).toIterable()
                .contains("articleId", "title", "pv", "dailyUvSum");
        assertThat(root.toString()).doesNotContain(
                "PageViewEntity",
                "PageViewMapper",
                "PageViewRepository",
                "PageViewRecordCommand",
                "visitorHash",
                "clientIp",
                "userAgent",
                "hashSecret",
                "uvTotal");
    }

    private void assertMethods(
            JsonNode root,
            String pointer,
            String... methods) {
        assertThat(root.at(pointer).fieldNames()).toIterable()
                .containsExactlyInAnyOrder(methods);
    }

    private JsonNode apiDocument() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }
}
