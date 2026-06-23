package com.tyb.myblog.v2.content.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ArticleOpenApiTest {

    private static final Set<String> ARTICLE_WRITE_FIELDS = Set.of(
            "titleZh", "titleJa", "titleEn",
            "summaryZh", "summaryJa", "summaryEn",
            "body", "categoryId", "tagIds", "slug",
            "status", "password", "publishAt", "coverAttachmentId");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsArticlePathsAndMethods() throws Exception {
        JsonNode root = apiDocument();

        assertMethods(root, "/paths/~1api~1public~1articles", "get");
        assertMethods(root, "/paths/~1api~1public~1articles~1{id}", "get");
        assertMethods(root, "/paths/~1api~1admin~1articles", "get", "post");
        assertMethods(root, "/paths/~1api~1admin~1articles~1{id}",
                "get", "put", "delete");
        assertMethods(root, "/paths/~1api~1admin~1articles~1recycle-bin",
                "get");
        assertMethods(root, "/paths/~1api~1admin~1articles~1{id}~1restore",
                "post");
    }

    @Test
    void documentsStableArticleSchemasWithoutInternalTypes()
            throws Exception {
        JsonNode root = apiDocument();

        assertRequired(
                root,
                "ArticleWriteOpenApiRequest",
                ARTICLE_WRITE_FIELDS);
        assertThat(root.toString()).doesNotContain(
                "ArticleEntity",
                "ArticleMapper",
                "SubmittedField",
                "accessPassword",
                "\"passwordHash\"");
        assertThat(root.at(
                        "/components/schemas/PublicArticlePageItemVO/properties")
                .fieldNames()).toIterable()
                .doesNotContain("status", "coverAttachmentId");
        assertThat(root.at(
                        "/components/schemas/PublicArticleDetailVO/properties")
                .fieldNames()).toIterable()
                .contains("updatedAt")
                .doesNotContain("status", "coverAttachmentId");
        assertNoFields(root, "AdminArticlePageItemVO", "deleted");
        assertNoFields(root, "AdminArticleDetailVO", "deleted");
        assertNoFields(root, "PublicArticlePageItemVO", "deleted");
        assertNoFields(root, "PublicArticleDetailVO", "deleted");
        assertStringId(root, "AdminArticlePageItemVO", "id");
        assertStringId(root, "AdminArticlePageItemVO", "categoryId");
        assertStringId(root, "AdminArticlePageItemVO", "coverAttachmentId");
        assertStringId(root, "AdminArticlePageItemVO", "createdBy");
        assertStringId(root, "AdminArticlePageItemVO", "updatedBy");
        assertStringId(root, "AdminArticleDetailVO", "id");
        assertStringId(root, "AdminArticleDetailVO", "categoryId");
        assertStringId(root, "AdminArticleDetailVO", "authorId");
        assertStringId(root, "AdminArticleDetailVO", "coverAttachmentId");
        assertStringId(root, "AdminArticleDetailVO", "createdBy");
        assertStringId(root, "AdminArticleDetailVO", "updatedBy");
        assertStringId(root, "DeletedArticlePageItemVO", "id");
        assertStringId(root, "DeletedArticlePageItemVO", "categoryId");
        assertStringId(root, "DeletedArticlePageItemVO", "deletedBy");
        assertThat(root.at(
                        "/components/schemas/AdminArticlePageItemVO/properties/tagIds/items/type")
                .asText()).isEqualTo("string");
        assertThat(root.at(
                        "/components/schemas/AdminArticleDetailVO/properties/tagIds/items/type")
                .asText()).isEqualTo("string");
    }

    private void assertMethods(
            JsonNode root,
            String pointer,
            String... methods) {
        assertThat(root.at(pointer).fieldNames()).toIterable()
                .containsExactlyInAnyOrder(methods);
    }

    private void assertRequired(
            JsonNode root,
            String schema,
            Set<String> fields) {
        assertThat(root.at(
                        "/components/schemas/" + schema
                                + "/required"))
                .extracting(JsonNode::asText)
                .containsExactlyInAnyOrderElementsOf(fields);
    }

    private void assertStringId(
            JsonNode root,
            String schema,
            String field) {
        JsonNode property = root.at(
                "/components/schemas/" + schema
                        + "/properties/" + field);
        assertThat(property.path("type").asText()).isEqualTo("string");
        assertThat(property.path("format").asText()).isEqualTo("int64");
    }

    private void assertNoFields(
            JsonNode root,
            String schema,
            String... fields) {
        assertThat(root.at(
                        "/components/schemas/" + schema
                                + "/properties")
                        .fieldNames())
                .toIterable()
                .doesNotContain(fields);
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
