package com.tyb.myblog.v2.stats.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 公开打点 OpenAPI 文档模型。
 */
@Schema(name = "PageViewRecordRequest")
public record PageViewRecordOpenApiRequest(
        @Schema(description = "文章 ID；非文章页面省略") Long articleId,
        @Schema(description = "站点语言", allowableValues = {
                "zh", "ja", "en"}) String lang) {
}
