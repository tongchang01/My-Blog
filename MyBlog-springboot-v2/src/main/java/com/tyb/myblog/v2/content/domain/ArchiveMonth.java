package com.tyb.myblog.v2.content.domain;

import java.util.List;

public record ArchiveMonth(String month, List<ArticleSummary> articles) {

    public ArchiveMonth {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }
}
