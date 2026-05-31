package com.tyb.myblog.v2.content.web;

import com.tyb.myblog.v2.content.domain.ArchiveMonth;

import java.util.List;

/**
 * 文章归档月份响应。
 *
 * @param month    归档月份
 * @param articles 当月文章列表
 */
public record ArchiveMonthResponse(String month, List<ArticleSummaryResponse> articles) {

    /**
     * 从领域归档对象转换为接口响应。
     */
    static ArchiveMonthResponse from(ArchiveMonth archiveMonth) {
        return new ArchiveMonthResponse(
                archiveMonth.month(),
                archiveMonth.articles().stream().map(ArticleSummaryResponse::from).toList());
    }
}
