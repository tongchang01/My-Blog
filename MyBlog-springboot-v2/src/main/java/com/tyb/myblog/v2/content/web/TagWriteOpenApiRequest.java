package com.tyb.myblog.v2.content.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 标签完整写请求的 OpenAPI 模型。
 */
public record TagWriteOpenApiRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String nameZh,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String nameJa,
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                types = {"string", "null"})
        String nameEn,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String slug) {
}
