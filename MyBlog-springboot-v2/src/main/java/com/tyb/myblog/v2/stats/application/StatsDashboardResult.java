package com.tyb.myblog.v2.stats.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 后台访问统计总览结果。 */
public record StatsDashboardResult(
        long periodPv,
        long todayPv,
        long todayUv,
        BigDecimal averageDailyUv,
        List<TrendPoint> trend,
        List<TopArticle> topArticles,
        List<LanguageDistribution> languageDistribution) {

    /** 连续自然日趋势。 */
    public record TrendPoint(LocalDate date, long pv, long uv) {
    }

    /** TOP 文章访问数据。 */
    public record TopArticle(
            long articleId,
            String title,
            long pv,
            long dailyUvSum) {
    }

    /** 语言访问占比。 */
    public record LanguageDistribution(
            String language,
            long pv,
            BigDecimal ratio) {
    }
}
