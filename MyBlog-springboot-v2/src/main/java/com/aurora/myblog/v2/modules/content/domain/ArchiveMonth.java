package com.aurora.myblog.v2.modules.content.domain;

import java.util.List;

public record ArchiveMonth(String month, List<ArticleSummary> articles) {

    public ArchiveMonth {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }
}
