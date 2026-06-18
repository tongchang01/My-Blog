package com.tyb.myblog.v2.stats.domain;

/** 语言维度的访问聚合。 */
public record LanguageTraffic(StatsLanguage language, long pv) {
}
