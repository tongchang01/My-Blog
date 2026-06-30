package com.tyb.myblog.v2.content.web;

import io.swagger.v3.oas.annotations.media.Schema;

public record PublicArticleTagVO(
        @Schema(format = "int64") String id,
        String name,
        String slug) {
}
