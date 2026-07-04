package com.tyb.myblog.v2.content.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArchivePageVO(
        String yearMonth,
        int year,
        int month,
        List<Item> articles) {

    @Schema(name = "PublicArchiveArticleVO")
    public record Item(
            @Schema(type = "string", format = "int64")
            String id,
            String title,
            String slug,
            LocalDateTime publishedAt,
            String summary) {
    }
}
