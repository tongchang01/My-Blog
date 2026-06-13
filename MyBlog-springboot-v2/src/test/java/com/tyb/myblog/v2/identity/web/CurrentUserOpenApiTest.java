package com.tyb.myblog.v2.identity.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 当前用户资料 OpenAPI 契约测试。
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CurrentUserOpenApiTest {

    private static final List<String> PROFILE_FIELDS = List.of(
            "nickname",
            "avatarUrl",
            "bioZh",
            "bioJa",
            "bioEn",
            "location",
            "website",
            "emailPublic",
            "githubUrl",
            "twitterUrl",
            "linkedinUrl",
            "zhihuUrl",
            "qiitaUrl",
            "juejinUrl");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void documentsPatchFieldsAsNullableStringsInsteadOfPatchValues()
            throws Exception {
        String content = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(content);
        String requestSchemaRef = root.at(
                        "/paths/~1api~1auth~1me~1profile/patch/requestBody"
                                + "/content/application~1json/schema/$ref")
                .asText();
        assertThat(requestSchemaRef).startsWith("#/components/schemas/");
        String requestSchemaName = requestSchemaRef.substring(
                requestSchemaRef.lastIndexOf('/') + 1);
        JsonNode requestSchema = root.path("components")
                .path("schemas")
                .path(requestSchemaName);

        assertThat(requestSchema.isMissingNode()).isFalse();
        for (String field : PROFILE_FIELDS) {
            JsonNode property = requestSchema.path("properties").path(field);
            assertThat(property.path("type").toString())
                    .as("%s should be documented as string or null", field)
                    .isEqualTo("[\"string\",\"null\"]");
            assertThat(property.has("$ref"))
                    .as("%s should not reference PatchValue", field)
                    .isFalse();
        }

        assertThat(root.at("/components/schemas/PatchValueString")
                .isMissingNode()).isTrue();
    }
}
