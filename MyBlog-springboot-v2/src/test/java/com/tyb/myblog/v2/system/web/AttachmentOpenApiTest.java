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
 * 附件接口 OpenAPI 契约测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AttachmentOpenApiTest {

    private static final Set<String> ATTACHMENT_FIELDS = Set.of(
            "id",
            "storageType",
            "bucket",
            "objectKey",
            "publicUrl",
            "contentType",
            "fileSize",
            "width",
            "height",
            "originalFilename",
            "hashSha256",
            "createdAt",
            "createdBy");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsExactMethodsAndMultipartBinaryFile() throws Exception {
        JsonNode root = apiDocument();
        JsonNode collection =
                root.at("/paths/~1api~1admin~1attachments");
        JsonNode detail =
                root.at("/paths/~1api~1admin~1attachments~1{id}");

        assertThat(collection.has("get")).isTrue();
        assertThat(collection.has("post")).isTrue();
        assertThat(collection.has("put")).isFalse();
        assertThat(collection.has("delete")).isFalse();
        assertThat(detail.has("get")).isTrue();
        assertThat(detail.size()).isEqualTo(1);

        JsonNode multipartSchema = resolveSchema(
                root,
                collection.path("post")
                        .at("/requestBody/content/multipart~1form-data/schema"));
        JsonNode file = multipartSchema.path("properties").path("file");
        assertThat(file.path("type").asText()).isEqualTo("string");
        assertThat(file.path("format").asText()).isEqualTo("binary");
    }

    @Test
    void documentsStablePageAndAttachmentFields() throws Exception {
        JsonNode root = apiDocument();
        JsonNode attachment = root.path("components")
                .path("schemas")
                .path("AttachmentVO");
        JsonNode page = root.path("components")
                .path("schemas")
                .path("PageResponseAttachmentVO");

        assertThat(attachment.path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrderElementsOf(ATTACHMENT_FIELDS);
        assertThat(page.path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder(
                        "records", "total", "page", "size");
        assertThat(attachment.toString() + page)
                .contains("\"records\"", "\"total\"", "\"page\"", "\"size\"")
                .doesNotContain(
                        "\"deleted\"",
                        "\"updatedAt\"",
                        "java.nio.file.Path",
                        "software.amazon.awssdk");
    }

    private JsonNode apiDocument() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private JsonNode resolveSchema(JsonNode root, JsonNode schema) {
        if (!schema.has("$ref")) {
            return schema;
        }
        String ref = schema.path("$ref").asText();
        assertThat(ref).startsWith("#/components/schemas/");
        return root.path("components")
                .path("schemas")
                .path(ref.substring(ref.lastIndexOf('/') + 1));
    }
}
