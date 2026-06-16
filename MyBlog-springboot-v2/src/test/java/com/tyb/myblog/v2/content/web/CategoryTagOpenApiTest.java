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

/**
 * 分类标签接口 OpenAPI 契约测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CategoryTagOpenApiTest {

    private static final Set<String> PUBLIC_FIELDS =
            Set.of("id", "name", "slug");
    private static final Set<String> CATEGORY_ADMIN_FIELDS =
            Set.of(
                    "id", "nameZh", "nameJa", "nameEn", "slug",
                    "sortOrder", "createdAt", "createdBy",
                    "updatedAt", "updatedBy");
    private static final Set<String> TAG_ADMIN_FIELDS =
            Set.of(
                    "id", "nameZh", "nameJa", "nameEn", "slug",
                    "createdAt", "createdBy",
                    "updatedAt", "updatedBy");
    private static final Set<String> CATEGORY_WRITE_FIELDS =
            Set.of(
                    "nameZh", "nameJa", "nameEn",
                    "slug", "sortOrder");
    private static final Set<String> TAG_WRITE_FIELDS =
            Set.of("nameZh", "nameJa", "nameEn", "slug");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsExactCategoryAndTagMethods() throws Exception {
        JsonNode root = apiDocument();

        assertMethods(root,
                "/paths/~1api~1public~1categories",
                "get");
        assertMethods(root,
                "/paths/~1api~1public~1tags",
                "get");
        assertMethods(root,
                "/paths/~1api~1admin~1categories",
                "get", "post");
        assertMethods(root,
                "/paths/~1api~1admin~1categories~1{id}",
                "get", "put", "delete");
        assertMethods(root,
                "/paths/~1api~1admin~1categories~1sort-orders",
                "put");
        assertMethods(root,
                "/paths/~1api~1admin~1tags",
                "get", "post");
        assertMethods(root,
                "/paths/~1api~1admin~1tags~1{id}",
                "get", "put", "delete");
        assertThat(root.at(
                "/paths/~1api~1admin~1tags~1sort-orders")
                .isMissingNode()).isTrue();
    }

    @Test
    void documentsStableSchemasWithoutInternalTypes()
            throws Exception {
        JsonNode root = apiDocument();
        assertFields(root, "PublicCategoryVO", PUBLIC_FIELDS);
        assertFields(root, "PublicTagVO", PUBLIC_FIELDS);
        assertFields(
                root, "AdminCategoryVO", CATEGORY_ADMIN_FIELDS);
        assertFields(root, "AdminTagVO", TAG_ADMIN_FIELDS);
        assertFields(
                root,
                "CategoryWriteOpenApiRequest",
                CATEGORY_WRITE_FIELDS);
        assertFields(
                root,
                "TagWriteOpenApiRequest",
                TAG_WRITE_FIELDS);

        assertRequired(
                root,
                "CategoryWriteOpenApiRequest",
                CATEGORY_WRITE_FIELDS);
        assertRequired(
                root,
                "TagWriteOpenApiRequest",
                TAG_WRITE_FIELDS);
        String document = root.toString();
        assertThat(document).doesNotContain(
                "CategoryEntity",
                "TagEntity",
                "CategoryMapper",
                "TagMapper",
                "ContentName",
                "ContentSlug",
                "SubmittedField",
                "\"deleted\"");
    }

    private void assertMethods(
            JsonNode root,
            String pointer,
            String... methods) {
        assertThat(root.at(pointer).fieldNames()).toIterable()
                .containsExactlyInAnyOrder(methods);
    }

    private void assertFields(
            JsonNode root,
            String schema,
            Set<String> fields) {
        assertThat(root.at(
                        "/components/schemas/" + schema
                                + "/properties")
                        .fieldNames())
                .toIterable()
                .containsExactlyInAnyOrderElementsOf(fields);
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

    private JsonNode apiDocument() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }
}
