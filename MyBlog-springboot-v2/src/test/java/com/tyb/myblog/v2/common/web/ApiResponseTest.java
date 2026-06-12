package com.tyb.myblog.v2.common.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统一 API 响应契约测试。
 */
class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesSuccessWithFrozenFieldsAndCode() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(ApiResponse.ok("payload")));

        assertThat(json.size()).isEqualTo(3);
        assertThat(json.has("code")).isTrue();
        assertThat(json.has("msg")).isTrue();
        assertThat(json.has("data")).isTrue();
        assertThat(json.path("code").asText()).isEqualTo("00000");
        assertThat(json.path("msg").asText()).isEqualTo("success");
        assertThat(json.path("data").asText()).isEqualTo("payload");
        assertThat(json.has("success")).isFalse();
        assertThat(json.has("message")).isFalse();
    }

    @Test
    void serializesFailureWithNullData() throws Exception {
        JsonNode json = objectMapper.readTree(
                objectMapper.writeValueAsString(
                        ApiResponse.fail("10001", "用户名或密码错误")));

        assertThat(json.path("code").asText()).isEqualTo("10001");
        assertThat(json.path("msg").asText()).isEqualTo("用户名或密码错误");
        assertThat(json.path("data").isNull()).isTrue();
    }
}
