package com.tyb.myblog.v2.stats.domain;

/** 文章在日期区间内的访问聚合。 */
public record TopArticleTraffic(long articleId, long pv, long dailyUvSum) {
}
