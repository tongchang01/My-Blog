package com.tyb.myblog.v2.stats.infrastructure.persistence.projection;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 单日访问趋势 SQL 投影。 */
@Getter
@Setter
public class DailyTrafficRow {
    private LocalDate statDate;
    private Long pv;
    private Long uv;
}
