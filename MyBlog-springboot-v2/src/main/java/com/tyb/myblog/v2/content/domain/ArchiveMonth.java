package com.tyb.myblog.v2.content.domain;

import java.util.List;

/**
 * 某个月份的文章归档。
 *
 * @param month    归档月份，格式由持久层统一生成
 * @param articles 该月份下的文章摘要
 */
public record ArchiveMonth(String month, List<ArticleSummary> articles) {

    public ArchiveMonth {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }
}
