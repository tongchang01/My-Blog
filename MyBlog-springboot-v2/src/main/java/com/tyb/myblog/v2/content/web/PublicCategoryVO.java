package com.tyb.myblog.v2.content.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 公开分类响应。
 */
public record PublicCategoryVO(
        @Schema(format = "int64") String id,
        String name,
        String slug,
        long articleCount) {
}
