package com.tyb.myblog.v2.stats.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 单文章、语言和 JST 日期的访问聚合。
 */
public record DailyPageView(
        long articleId,
        StatsLanguage language,
        LocalDate statDate,
        int pv,
        int uv) {

    public DailyPageView {
        if (articleId < 0) {
            throw new IllegalArgumentException("聚合文章 ID 不能为负数");
        }
        Objects.requireNonNull(language, "聚合语言不能为空");
        Objects.requireNonNull(statDate, "统计日期不能为空");
        if (pv < 0 || uv < 0) {
            throw new IllegalArgumentException("PV 和 UV 不能为负数");
        }
        if (uv > pv) {
            throw new IllegalArgumentException("UV 不能大于 PV");
        }
    }
}
