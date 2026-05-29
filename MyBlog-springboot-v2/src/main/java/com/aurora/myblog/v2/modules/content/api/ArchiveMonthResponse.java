package com.aurora.myblog.v2.modules.content.api;

import com.aurora.myblog.v2.modules.content.domain.ArchiveMonth;

import java.util.List;

public record ArchiveMonthResponse(String month, List<ArticleSummaryResponse> articles) {

    static ArchiveMonthResponse from(ArchiveMonth archiveMonth) {
        return new ArchiveMonthResponse(
                archiveMonth.month(),
                archiveMonth.articles().stream().map(ArticleSummaryResponse::from).toList());
    }
}
