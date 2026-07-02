package com.tyb.myblog.v2.content.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 公开标签响应。
 */
public record PublicTagVO(
        @Schema(format = "int64") String id,
        String name,
        String slug,
        long articleCount) {
}
