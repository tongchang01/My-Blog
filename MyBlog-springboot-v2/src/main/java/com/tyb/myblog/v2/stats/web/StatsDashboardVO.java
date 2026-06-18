package com.tyb.myblog.v2.stats.web;

import com.tyb.myblog.v2.stats.application.StatsDashboardResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 后台访问统计总览响应。 */
public record StatsDashboardVO(
        @Schema(description = "查询区间内总 PV") long periodPv,
        @Schema(description = "JST 今天 PV") long todayPv,
        @Schema(description = "JST 今天日 UV") long todayUv,
        @Schema(description = "按完整区间天数计算的平均日 UV")
        BigDecimal averageDailyUv,
        List<TrendPoint> trend,
        List<TopArticle> topArticles,
        List<LanguageDistribution> languageDistribution) {

    /** 把应用结果转换为稳定的 HTTP 响应模型。 */
    public static StatsDashboardVO from(StatsDashboardResult result) {
        return new StatsDashboardVO(
                result.periodPv(),
                result.todayPv(),
                result.todayUv(),
                result.averageDailyUv(),
                result.trend().stream()
                        .map(point -> new TrendPoint(
                                point.date(), point.pv(), point.uv()))
                        .toList(),
                result.topArticles().stream()
                        .map(article -> new TopArticle(
                                article.articleId(),
                                article.title(),
                                article.pv(),
                                article.dailyUvSum()))
                        .toList(),
                result.languageDistribution().stream()
                        .map(language -> new LanguageDistribution(
                                language.language(),
                                language.pv(),
                                language.ratio()))
                        .toList());
    }

    /** 连续自然日趋势。 */
    public record TrendPoint(LocalDate date, long pv, long uv) {
    }

    /** TOP 文章访问数据；标题不存在时保留统计行并返回 null。 */
    public record TopArticle(
            long articleId,
            String title,
            long pv,
            @Schema(description = "区间内各日 UV 之和，不代表跨日独立访客")
            long dailyUvSum) {
    }

    /** 语言访问数量及其占区间 PV 的比例。 */
    public record LanguageDistribution(
            String language,
            long pv,
            BigDecimal ratio) {
    }
}
