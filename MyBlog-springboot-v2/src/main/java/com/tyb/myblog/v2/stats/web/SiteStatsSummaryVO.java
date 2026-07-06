package com.tyb.myblog.v2.stats.web;

import com.tyb.myblog.v2.stats.application.SiteStatsSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

/** 公开站点统计摘要响应。 */
public record SiteStatsSummaryVO(
        @Schema(description = "JST 今天全站 UV") long todayUv,
        @Schema(description = "全站累计 PV") long totalPv
) {

    public static SiteStatsSummaryVO from(SiteStatsSummaryResult result) {
        return new SiteStatsSummaryVO(result.todayUv(), result.totalPv());
    }
}
