package com.tyb.myblog.v2.system.web;

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
 * 友链接口 OpenAPI 契约测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class FriendLinkOpenApiTest {

    private static final Set<String> PUBLIC_FIELDS = Set.of(
            "id", "name", "url", "avatarUrl", "description");
    private static final Set<String> ADMIN_FIELDS = Set.of(
            "id", "name", "url", "avatarUrl", "description",
            "sortOrder", "status", "createdAt", "createdBy",
            "updatedAt", "updatedBy");
    private static final Set<String> WRITE_FIELDS = Set.of(
            "name", "url", "avatarUrl", "description",
            "sortOrder", "status");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsExactFriendLinkMethods() throws Exception {
        JsonNode root = apiDocument();
        JsonNode publicCollection = root.at(
                "/paths/~1api~1public~1friend-links");
        JsonNode adminCollection = root.at(
                "/paths/~1api~1admin~1friend-links");
        JsonNode detail = root.at(
                "/paths/~1api~1admin~1friend-links~1{id}");
        JsonNode statusPath = root.at(
                "/paths/~1api~1admin~1friend-links~1{id}~1status");
        JsonNode sortPath = root.at(
                "/paths/~1api~1admin~1friend-links~1sort-orders");

        assertThat(publicCollection.fieldNames()).toIterable()
                .containsExactly("get");
        assertThat(adminCollection.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("get", "post");
        assertThat(detail.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("get", "put", "delete");
        assertThat(statusPath.fieldNames()).toIterable()
                .containsExactly("patch");
        assertThat(sortPath.fieldNames()).toIterable()
                .containsExactly("put");
    }

    @Test
    void documentsStableFieldsAndCompleteWriteSchema()
            throws Exception {
        JsonNode root = apiDocument();
        JsonNode publicSchema = root.at(
                "/components/schemas/PublicFriendLinkVO");
        JsonNode adminSchema = root.at(
                "/components/schemas/AdminFriendLinkVO");
        JsonNode writeSchema = root.at(
                "/components/schemas/FriendLinkWriteOpenApiRequest");

        assertThat(publicSchema.path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrderElementsOf(PUBLIC_FIELDS);
        assertThat(adminSchema.path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrderElementsOf(ADMIN_FIELDS);
        assertThat(writeSchema.path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrderElementsOf(WRITE_FIELDS);
        assertThat(writeSchema.path("required"))
                .extracting(JsonNode::asText)
                .containsExactlyInAnyOrderElementsOf(WRITE_FIELDS);
        assertThat(writeSchema.path("properties")
                        .path("avatarUrl")
                        .path("type")
                        .toString())
                .isEqualTo("[\"string\",\"null\"]");
        assertThat(writeSchema.path("properties")
                        .path("description")
                        .path("type")
                        .toString())
                .isEqualTo("[\"string\",\"null\"]");
        assertThat(adminSchema.toString() + writeSchema)
                .doesNotContain(
                        "deleted",
                        "deletedAt",
                        "deletedBy",
                        "SubmittedField",
                        "FriendLinkEntity");
        assertThat(adminSchema.path("properties")
                        .path("status")
                        .path("enum"))
                .extracting(JsonNode::asText)
                .containsExactly("VISIBLE", "HIDDEN");
    }

    @Test
    void documentsStablePageShape() throws Exception {
        JsonNode root = apiDocument();
        JsonNode page = root.at(
                "/components/schemas/PageResponseAdminFriendLinkVO");

        assertThat(page.path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder(
                        "records", "total", "page", "size");
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
