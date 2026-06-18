package com.tyb.myblog.v2.stats.domain;

import java.time.LocalDate;

/** 单日访问趋势点。 */
public record DailyTrafficPoint(LocalDate date, long pv, long uv) {
}
