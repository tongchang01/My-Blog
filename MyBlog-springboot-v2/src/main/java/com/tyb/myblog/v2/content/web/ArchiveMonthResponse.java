package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.ArchiveMonth;

import java.util.List;

public record ArchiveMonthResponse(String month, List<ArticleSummaryResponse> articles) {

    static ArchiveMonthResponse from(ArchiveMonth archiveMonth) {
        return new ArchiveMonthResponse(
                archiveMonth.month(),
                archiveMonth.articles().stream().map(ArticleSummaryResponse::from).toList());
    }
}
