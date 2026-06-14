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
 * 站点配置 OpenAPI 契约测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SiteConfigOpenApiTest {

    private static final Set<String> PUBLIC_FIELDS = Set.of(
            "siteTitle",
            "siteSubtitle",
            "aboutMd",
            "logoUrl",
            "faviconUrl",
            "icpNo",
            "spotifyPlaylistId");
    private static final Set<String> BUSINESS_FIELDS = Set.of(
            "siteTitleZh",
            "siteTitleJa",
            "siteTitleEn",
            "siteSubtitleZh",
            "siteSubtitleJa",
            "siteSubtitleEn",
            "aboutMdZh",
            "aboutMdJa",
            "aboutMdEn",
            "logoUrl",
            "faviconUrl",
            "icpNo",
            "spotifyPlaylistId");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsExactSiteConfigMethodsAndResponseFields()
            throws Exception {
        JsonNode root = apiDocument();
        JsonNode publicPath =
                root.at("/paths/~1api~1public~1site-config");
        JsonNode adminPath =
                root.at("/paths/~1api~1admin~1site-config");

        assertThat(publicPath.has("get")).isTrue();
        assertThat(publicPath.has("put")).isFalse();
        assertThat(publicPath.has("post")).isFalse();
        assertThat(adminPath.has("get")).isTrue();
        assertThat(adminPath.has("put")).isTrue();
        assertThat(adminPath.has("post")).isFalse();
        assertThat(adminPath.has("patch")).isFalse();

        JsonNode publicData = responseDataSchema(root, publicPath.path("get"));
        assertThat(publicData.path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrderElementsOf(PUBLIC_FIELDS);

        JsonNode adminData = responseDataSchema(root, adminPath.path("get"));
        assertThat(adminData.path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrderElementsOf(
                        java.util.stream.Stream.concat(
                                        BUSINESS_FIELDS.stream(),
                                        java.util.stream.Stream.of(
                                                "updatedAt",
                                                "updatedBy"))
                                .toList());
        assertThat(adminData.toString())
                .doesNotContain(
                        "deleted",
                        "createdBy",
                        "SubmittedField");
    }

    @Test
    void documentsCompletePutAsRequiredStringOrNullFields()
            throws Exception {
        JsonNode root = apiDocument();
        JsonNode put = root.at(
                "/paths/~1api~1admin~1site-config/put");
        JsonNode requestSchema = resolveRef(
                root,
                put.at(
                        "/requestBody/content/application~1json/schema/$ref")
                        .asText());

        assertThat(requestSchema.path("required"))
                .extracting(JsonNode::asText)
                .containsExactlyInAnyOrderElementsOf(BUSINESS_FIELDS);
        assertThat(requestSchema.path("properties").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrderElementsOf(BUSINESS_FIELDS);
        assertThat(requestSchema.path("properties")
                        .path("siteTitleZh")
                        .path("type")
                        .asText())
                .isEqualTo("string");
        for (String field : BUSINESS_FIELDS) {
            if ("siteTitleZh".equals(field)) {
                continue;
            }
            assertThat(requestSchema.path("properties")
                            .path(field)
                            .path("type")
                            .toString())
                    .as(field)
                    .isEqualTo("[\"string\",\"null\"]");
        }
        assertThat(requestSchema.toString())
                .doesNotContain(
                        "SubmittedField",
                        "deleted",
                        "createdBy");
    }

    private JsonNode apiDocument() throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content);
    }

    private JsonNode responseDataSchema(
            JsonNode root,
            JsonNode operation) {
        JsonNode content = operation.path("responses")
                .path("200")
                .path("content");
        String mediaType = content.fieldNames().next();
        String wrapperRef = content.path(mediaType)
                .path("schema")
                .path("$ref")
                .asText();
        JsonNode wrapper = resolveRef(root, wrapperRef);
        return resolveRef(
                root,
                wrapper.path("properties").path("data").path("$ref").asText());
    }

    private JsonNode resolveRef(JsonNode root, String ref) {
        assertThat(ref).startsWith("#/components/schemas/");
        return root.path("components")
                .path("schemas")
                .path(ref.substring(ref.lastIndexOf('/') + 1));
    }
}
