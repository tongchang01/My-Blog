package com.tyb.myblog.v2.stats.application;

import java.time.LocalDate;

/** 后台访问统计总览日期查询。 */
public record StatsDashboardQuery(LocalDate from, LocalDate to) {
}
