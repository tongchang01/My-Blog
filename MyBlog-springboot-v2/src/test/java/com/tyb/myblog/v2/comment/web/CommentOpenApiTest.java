package com.tyb.myblog.v2.comment.web;

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
class CommentOpenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsCommentPathsAndMethods() throws Exception {
        JsonNode root = apiDocument();

        assertMethods(root,
                "/paths/~1api~1public~1articles~1{articleId}~1comments",
                "get",
                "post");
        assertMethods(root,
                "/paths/~1api~1public~1guestbook~1comments",
                "get",
                "post");
        assertMethods(root,
                "/paths/~1api~1admin~1comments",
                "get");
        assertMethods(root,
                "/paths/~1api~1admin~1comments~1{id}~1approve",
                "post");
        assertMethods(root,
                "/paths/~1api~1admin~1comments~1{id}~1hide",
                "post");
        assertMethods(root,
                "/paths/~1api~1admin~1comments~1{id}~1restore",
                "post");
        assertMethods(root,
                "/paths/~1api~1admin~1comments~1{id}",
                "delete");
    }

    @Test
    void publicSchemaDoesNotExposeSensitiveOrInternalTypes()
            throws Exception {
        JsonNode root = apiDocument();
        String document = root.toString();
        String publicCommentSchema =
                root.at("/components/schemas/PublicCommentVO").toString();

        assertThat(document).contains("PublicCommentVO");
        assertThat(document).doesNotContain(
                "CommentEntity",
                "CommentMapper",
                "CommentRepository",
                "CommentCreateCommand");
        assertThat(publicCommentSchema).doesNotContain(
                "contentMd",
                "authorEmail",
                "authorIp",
                "authorUserAgent");
        assertStringId(root, "PublicCommentVO", "id");
        assertStringId(root, "PublicCommentVO", "parentId");
        assertStringId(root, "PublicCommentVO", "replyToCommentId");
        assertStringId(root, "PublicCommentCreateVO", "id");
    }

    private void assertMethods(
            JsonNode root,
            String pointer,
            String... methods) {
        assertThat(root.at(pointer).fieldNames()).toIterable()
                .containsExactlyInAnyOrder(methods);
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

    private JsonNode apiDocument() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }
}
