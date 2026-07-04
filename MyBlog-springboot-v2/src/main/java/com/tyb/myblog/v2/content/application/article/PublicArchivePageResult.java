package com.tyb.myblog.v2.content.application.article;

import java.time.LocalDateTime;
import java.util.List;

public record PublicArchivePageResult(
        List<Group> records,
        long total,
        int page,
        int size) {

    public record Group(
            String yearMonth,
            int year,
            int month,
            List<Item> articles) {
    }

    public record Item(
            long id,
            String title,
            String slug,
            LocalDateTime publishedAt,
            String summary) {
    }
}
